package scalasim.network.controlplane

import scalasim.network.component.{HostType, Link, Node}
import scalasim.network.controlplane.resource.ResourceAllocator
import scalasim.network.controlplane.routing.RoutingProtocol
import scalasim.network.controlplane.topology.TopologyManager
import scalasim.network.events.CompleteFlowEvent
import scalasim.network.traffic.{NewStartFlow, CompletedFlow, RunningFlow, Flow}
import scalasim.simengine.SimulationEngine
import scalasim.simengine.utils.Logging
import org.openflow.protocol.OFMatch
import simengine.utils.IPAddressConvertor

abstract class ControlPlane (private [controlplane] val node : Node,
                             private [controlplane] val routingModule : RoutingProtocol,
                             private [controlplane] val resourceModule : ResourceAllocator,
                             private [controlplane] val topoModule : TopologyManager) extends Logging {

  lazy val IP : String = node.toString

  override def toString = "controlplane-" + node.toString

  protected def otherEnd (link : Link) : Node = {
    if (link.end_to == node) link.end_from
    else link.end_to
  }

  protected def startFlow (flow : Flow) {
    if (flow.status == NewStartFlow) {
      val completeEvent = new CompleteFlowEvent(flow, RoutingProtocol.getFlowStarter(flow.SrcIP),
        SimulationEngine.currentTime + flow.Demand / flow.getTempRate)
      logTrace("schedule complete event " + completeEvent + " for " + flow + " at " +
        (SimulationEngine.currentTime + flow.Demand / flow.getTempRate))
      flow.bindEvent(completeEvent)
      SimulationEngine.addEvent(completeEvent)
      //has found the destination, change the property
      if (flow.floodflag) flow.floodflag = false
    }
    flow.run
  }

  protected def allocateOnCurrentHop(flow : Flow, link : Link) {
    val nextnode = otherEnd(link)
    val rpccontrolplane = {
      if (link.end_from == node) node.controlPlane
      else nextnode.controlPlane
    }
    if (flow.status == NewStartFlow)
      rpccontrolplane.resourceModule.insertNewLinkFlowPair(link, flow)
    rpccontrolplane.resourceModule.allocate(link)
  }

  def allocate (flow : Flow, matchfield : OFMatch, referenceLink : Link = null) {
    if (!flow.floodflag) {
      if (node.ip_addr(0) == flow.DstIP) startFlow(flow)
      else {
        val nextlink = routingModule.fetchRoutingEntry(matchfield)
        val nextnode = otherEnd(nextlink)
        allocateOnCurrentHop(flow, nextlink)
        nextnode.controlPlane.allocate(flow, matchfield, nextlink)
      }
    } else {
      //it's a flood flow
      if (node.ip_addr(0) == flow.srcIP) startFlow(flow)
      else {
        val laststep = {
          if (node.ip_addr(0) == flow.dstIP) referenceLink
          else flow.getLastHop(referenceLink)
        }
        val nextnode = otherEnd(laststep)
        logTrace("allocate for flood flow " + flow.toString() + " on " + laststep + " at node " + node)
        allocateOnCurrentHop(flow, laststep)
        //adding flow to the routing table in reverse order
        //may be conflicted with the future support for multi/broadcast
        //or, we should discard the support for multi/broadcast in flow-level simulation?
        nextnode.controlPlane.routingModule.insertFlowPath(matchfield, laststep)
        nextnode.controlPlane.allocate(flow, matchfield, laststep)
      }
    }
  }


  def finishFlow(flow : Flow, matchfield : OFMatch) {
    if (node.ip_addr(0) != flow.DstIP) {
      logTrace("flow ended at " + node.ip_addr(0))
      val nextlink = routingModule.fetchRoutingEntry(matchfield)
      val nextnode = otherEnd(nextlink)
      val rpccontrolplane = {
        if (nextlink.end_from == node) node.controlPlane
        else nextnode.controlPlane
      }
      rpccontrolplane.resourceModule.deleteFlow(flow)
      nextnode.controlPlane.finishFlow(flow, matchfield)
      //reallocate resource to other flows
      logTrace("reallocate resource on " + node.toString)
      rpccontrolplane.resourceModule.reallocate(nextlink)
      logTrace("delete route table entry:" + flow + " at " + node.ip_addr(0))
      routingModule.deleteEntry(matchfield)
    }
  }

  /**
   *
   * @param flow
   * @param matchfield
   * @param inlink it can be null (for the first hop)
   */
  def routing (flow : Flow, matchfield : OFMatch, inlink : Link) {
    //discard the flood packets
    val srcIP = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkSource)
    val dstIP = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination)
    if (node.ip_addr(0) !=  srcIP && node.ip_addr(0) != dstIP && node.nodetype == HostType) {
      logTrace("Discard flow " + flow + " on node " + node.toString)
      return
    }

    logTrace("arrive at " + node.ip_addr(0))
    if (node.ip_addr(0) == dstIP) {
      //arrive the destination
      //start resource allocation process
      val allocateStartNode = {
        if (!flow.floodflag) RoutingProtocol.getFlowStarter(srcIP)
        else node//start from the destination for flood flow
      }
      allocateStartNode.controlPlane.allocate(flow, matchfield, inlink)
    }
    else {
      //build matchfield match
      if (!flow.floodflag) {
        val nextlink = routingModule.selectNextLink(flow, matchfield, inlink)
        if (nextlink != null) {
          val nextnode = otherEnd(nextlink)
          logDebug("send through " + nextlink)
          routingModule.insertFlowPath(matchfield, nextlink)
          nextnode.controlPlane.routing(flow, matchfield, nextlink)
        } else {
          //the nextlink is null, which means that the routing hasn't been decided, it is
          //asynchronous, e.g. openflow, the next link will be handled in
          //OpenFlowHandler
          //do nothing,
        }
      } else {
        //it's a flood flow
        val nextlinks = routingModule.getfloodLinks(flow, inlink)
        //TODO : openflow flood handling in which nextlinks can be null?
        nextlinks.foreach(l => otherEnd(l).controlPlane.routing(flow, matchfield, l))
      }
    }
  }

  def getLinkAvailableBandwidth(l : Link) : Double = resourceModule.getLinkAvailableBandwidth(l)

  def registerIncomeLink(link : Link)  {
    topoModule.registerIncomeLink(link)
  }

  def registerOutgoingLink(link : Link) {
    topoModule.registerOutgoingLink(link)
  }

  //TODO: expose topo for test, need to be improved
  def outlinks = topoModule.outlink
  def inlinks = topoModule.inlinks
}
