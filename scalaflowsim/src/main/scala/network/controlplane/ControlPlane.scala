package scalasim.network.controlplane

import scalasim.network.component.{HostType, Link, Node}
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.resource.ResourceAllocator
import scalasim.network.controlplane.routing.{OpenFlowRouting, RoutingProtocol}
import scalasim.network.controlplane.topology.TopologyManager
import scalasim.network.events.CompleteFlowEvent
import scalasim.network.traffic.{NewStartFlow, CompletedFlow, RunningFlow, Flow}
import scalasim.simengine.SimulationEngine
import scalasim.simengine.utils.Logging
import org.openflow.protocol.OFMatch
import scalasim.XmlParser
import simengine.utils.IPAddressConvertor
import network.controlplane.openflow.flowtable.OFMatchField

abstract class ControlPlane (private [controlplane] val node : Node,
                             private [controlplane] val routingModule : RoutingProtocol,
                             private [controlplane] val resourceModule : ResourceAllocator,
                             private [controlplane] val topoModule : TopologyManager) extends Logging {

  lazy val IP : String = node.toString

  override def toString = "controlplane-" + node.toString

  private def startFlow (flow : Flow) {
    if (flow.status == NewStartFlow) {
      val completeEvent = new CompleteFlowEvent(
        flow,
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

  private def allocateOnCurrentHop(flow : Flow, link : Link) {
    val nextnode = Link.otherEnd(link, node)
    val rpccontrolplane = {
      if (link.end_from == node) node.controlPlane
      else nextnode.controlPlane
    }
    if (flow.status == NewStartFlow) {
      rpccontrolplane.resourceModule.insertNewLinkFlowPair(link, flow)
    }
    else {
      println("status:" == flow.status.toString)
    }
    rpccontrolplane.resourceModule.allocate(link)
  }

  def allocate (flow : Flow, referenceLink : Link = null) {
    if (node.ip_addr(0) == flow.srcIP) {
      startFlow(flow)
    } else {
      val laststep = {
        if (node.ip_addr(0) == flow.dstIP) referenceLink
        else flow.getLastHop(referenceLink)
      }
      val nextnode = Link.otherEnd(laststep, node)
      logTrace("allocate for flow " + flow.toString() + " on " + laststep + " at node " + node)
      allocateOnCurrentHop(flow, laststep)
      nextnode.controlPlane.allocate(flow, laststep)
    }
  }

  def finishFlow(flow : Flow, matchfield : OFMatch, referencelink : Link = null) {
    if (node.ip_addr(0) != flow.srcIP) {
      logTrace("flow ended at " + node.ip_addr(0))
      val nextlink = {
        if (referencelink == null) routingModule.fetchInRoutingEntry(matchfield)
        else flow.getLastHop(referencelink)
      }
      val nextnode = Link.otherEnd(nextlink, node)
      val rpccontrolplane = {
        if (nextlink.end_from == node) node.controlPlane
        else nextnode.controlPlane
      }
      rpccontrolplane.resourceModule.deleteFlow(flow)
      nextnode.controlPlane.finishFlow(flow, matchfield, nextlink)
      //reallocate resource to other flows
      logTrace("reallocate resource on " + node.toString)
      rpccontrolplane.resourceModule.reallocate(nextlink)
      logTrace("delete route table entry:" + flow + " at " + node.ip_addr(0))
      routingModule.deleteInEntry(matchfield)
    }
  }

  /**
   *
   * @param flow
   * @param inlink it can be null (for the first hop)
   */
  def routing (flow : Flow, matchfield : OFMatchField, inlink : Link) {
    //discard the flood packets
    if (node.ip_addr(0) !=  flow.srcIP && node.ip_addr(0) != flow.dstIP && node.nodetype == HostType) {
      logTrace("Discard flow " + flow + " on node " + node.toString)
      return
    }
    logTrace("arrive at " + node.ip_addr(0) + ", routing (flow : Flow, matchfield : OFMatch, inlink : Link)" +
      " flow:" + flow + ", inlink:" + inlink)
    if (inlink != null) {
      routingModule.insertInPath(flow, inlink)
      //only valid in openflow model
      if (XmlParser.getString("scalasim.simengine.model", "tcp") == "openflow" &&
        node.nodetype != HostType)
        flow.inport = topoModule.getPortByLink(inlink).getPortNumber
    }
    if (node.ip_addr(0) == flow.dstIP) {
      //arrive the destination
      //start resource allocation process
      node.controlPlane.allocate(flow, inlink)
    }
    else {
      if (!flow.floodflag) {
        val nextlink = routingModule.selectNextLink(matchfield, inlink)
        if (nextlink != null) {
          val nextnode = Link.otherEnd(nextlink, node)
          logDebug("send through " + nextlink)
          routingModule.insertOutPath(flow, nextlink)
          flow.addTrace(nextlink, inlink)
          nextnode.controlPlane.routing(flow, matchfield, nextlink)
        } else {
          val ofrouting = routingModule.asInstanceOf[OpenFlowRouting]
          ofrouting.pendingFlows += (ofrouting.pendingFlows.size -> flow)
        }
      } else {
        //it's a flood flow
        logTrace("flow " + flow + " is broadcasted, and is flooded out")
        routingModule.floodoutFlow(flow, matchfield, inlink)
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
