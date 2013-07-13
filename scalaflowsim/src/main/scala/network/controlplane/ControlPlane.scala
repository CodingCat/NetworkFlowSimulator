package network.controlplane

import scalasim.simengine.SimulationEngine
import scalasim.XmlParser
import network.topo.{Link, Node}
import network.controlplane.routing.{RoutingProtocol, RoutingProtocolFactory}
import network.data.Flow
import network.controlplane.resource.ResourceAllocatorFactory
import network.events.CompleteFlowEvent
import simengine.utils.Logging


class ControlPlane(node : Node) extends Logging {
  private [controlplane] val routingModule = RoutingProtocolFactory.getRoutingProtocol(
    XmlParser.getString("scalasim.router.routing", "SimpleSymmetricRouting"), node)
  private [controlplane] val resourceModule = ResourceAllocatorFactory.getResourceAllocator(
    XmlParser.getString("scalasim.router.resource", "MaxMin"), node)

  private def nextNode(link : Link) : Node = {
    if (link.end_to == node) link.end_from
    else link.end_to
  }

  def allocateForNewFlow (flow : Flow) {
    if (node.ip_addr(0) == flow.DstIP) {
      flow.sync
      val completeEvent = new CompleteFlowEvent(flow, SimulationEngine.currentTime + flow.Demand / flow.Rate)
      logTrace("schedule complete event " + completeEvent +  " for " + flow + " at " +
        (SimulationEngine.currentTime + flow.Demand / flow.Rate))
      flow.bindEvent(completeEvent)
      SimulationEngine.addEvent(completeEvent)
    }
    else {
      val nextlink = routingModule.selectNextLink(flow)
      val nextnode = nextNode(nextlink)
      var rpcorlocal = node
      if (nextlink.end_from != node) rpcorlocal = nextnode
      rpcorlocal.controlPlane.resourceModule.insertNewLinkFlowPair(nextlink, flow)
      rpcorlocal.controlPlane.resourceModule.allocateForNewFlow(flow, nextlink)
      nextnode.controlPlane.allocateForNewFlow(flow)
    }
  }

  def getLinkAvailableBandwidth(l : Link) : Double = resourceModule.getLinkAvailableBandwidth(l)

  def routing (flow : Flow) : Unit = {
    logInfo("arrive at " + node.ip_addr(0))
    if (node.ip_addr(0) != flow.DstIP) {
      val nextlink = routingModule.selectNextLink(flow)
      val nextnode = nextNode(nextlink)
      logDebug("send through " + nextlink)
      routingModule.insertFlowPath(flow, nextlink)
      nextnode.controlPlane.routing(flow)
    }
    else {
      //arrive the destination
      //start resource allocation process
      RoutingProtocol.getFlowStarter(flow.SrcIP).controlPlane.allocateForNewFlow(flow)
    }
  }
}
