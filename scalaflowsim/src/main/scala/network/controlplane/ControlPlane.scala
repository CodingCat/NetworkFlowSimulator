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

  protected def nextNode(link : Link) : Node = {
    if (link.end_to == node) link.end_from
    else link.end_to
  }

  def allocate (flow : Flow, matchfield : OFMatch) : Flow = {
    if (node.ip_addr(0) == flow.DstIP) {
      if (flow.status != RunningFlow && flow.status != CompletedFlow){
        if (flow.status == NewStartFlow) {
          val completeEvent = new CompleteFlowEvent(flow, RoutingProtocol.getFlowStarter(flow.SrcIP),
            SimulationEngine.currentTime + flow.Demand / flow.getTempRate)
          logTrace("schedule complete event " + completeEvent + " for " + flow + " at " +
            (SimulationEngine.currentTime + flow.Demand / flow.getTempRate))
          flow.bindEvent(completeEvent)
          SimulationEngine.addEvent(completeEvent)
        }
        flow.run
      }
    }
    else {
      val nextlink = routingModule.fetchRoutingEntry(matchfield)
      val nextnode = nextNode(nextlink)
      val rpccontrolplane = {
        if (nextlink.end_from == node) node.controlPlane
        else nextnode.controlPlane
      }
      if (flow.status == NewStartFlow)
        rpccontrolplane.resourceModule.insertNewLinkFlowPair(nextlink, flow)
      rpccontrolplane.resourceModule.allocate(nextlink)
      nextnode.controlPlane.allocate(flow, matchfield)
    }
    flow
  }


  def finishFlow(flow : Flow, matchfield : OFMatch) {
    if (node.ip_addr(0) != flow.DstIP) {
      logTrace("matchfield ended at " + node.ip_addr(0))
      val nextlink = routingModule.fetchRoutingEntry(matchfield)
      val nextnode = nextNode(nextlink)
      val rpccontrolplane = {
        if (nextlink.end_from == node) node.controlPlane
        else nextnode.controlPlane
      }
      rpccontrolplane.resourceModule.deleteFlow(flow)
      nextnode.controlPlane.finishFlow(flow, matchfield)
      //reallocate resource to other flows
      rpccontrolplane.resourceModule.reallocate(nextlink)
      logTrace("delete route table entry:" + flow + " at " + node.ip_addr(0))
      routingModule.deleteEntry(matchfield)
    }
  }

  def routing (flow : Flow, matchfield : OFMatch, inlink : Link) {
    //discard the flood packets
    val srcIP = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkSource)
    val dstIP = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination)
    if (node.ip_addr(0) !=  srcIP && node.ip_addr(0) != dstIP && node.nodetype == HostType) return

    logTrace("arrive at " + node.ip_addr(0))
    if (node.ip_addr(0) == dstIP) {
      //arrive the destination
      //start resource allocation process
      RoutingProtocol.getFlowStarter(srcIP).controlPlane.allocate(flow, matchfield)
    }
    else {
      //build matchfield match
      val nextlink = routingModule.selectNextLink(flow, matchfield, inlink)
      if (nextlink != null) {
        val nextnode = nextNode(nextlink)
        logDebug("send through " + nextlink)
        routingModule.insertFlowPath(matchfield, nextlink)
        nextnode.controlPlane.routing(flow, matchfield, nextlink)
      } else {
        //the nextlink is null, which means that the routing hasn't been decided, it is
        //asynchronous, e.g. openflow, the next link will be handled in
        //OpenFlowHandler
        //do nothing,
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
