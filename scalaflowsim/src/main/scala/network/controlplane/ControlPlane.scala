package network.controlplane

import scalasim.simengine.SimulationEngine
import scalasim.XmlParser
import network.topo.{Link, Node}
import network.controlplane.routing.{RoutingProtocol, RoutingProtocolFactory}
import network.data.Flow
import network.controlplane.resource.ResourceAllocatorFactory
import network.events.CompleteFlowEvent


class ControlPlane(node : Node) {
  protected val routingModule = RoutingProtocolFactory.getRoutingProtocol(
    XmlParser.getString("scalasim.router.routing", "SimpleSymmetricRouting"), node)
  protected val resourceModule = ResourceAllocatorFactory.getResourceAllocator(
    XmlParser.getString("scalasim.router.resource", "MaxMin"), node)



  def nextNode(link : Link) : Node = {
    if (link.end_to == node) link.end_from
    else link.end_to
  }

  def allocateForNewFlow (flow : Flow) {
    val nextlink = routingModule.selectNextLink(flow)
    val nextnode = nextNode(nextlink)
    resourceModule.insertNewLinkFlowPair(nextlink, flow)
    resourceModule.allocateForNewFlow(flow, nextlink)
    if (node.ip_addr(0) == flow.DstIP) {
      flow.sync
    }
    else {
      nextnode.controlPlane.allocateForNewFlow(flow)
    }
  }

  def getLinkAvailableBandwidth(l : Link) : Double = resourceModule.getLinkAvailableBandwidth(l)

  def routing (flow : Flow) : Unit = {
    if (node.ip_addr(0) != flow.DstIP) {
      val nextlink = routingModule.selectNextLink(flow)
      val nextnode = nextNode(nextlink)
      routingModule.insertFlowPath(flow, nextlink)
      nextnode.controlPlane.routing(flow)
    }
    else {
      //arrive the destination
      //start resource allocation process
      RoutingProtocol.getFlowStarter(flow.SrcIP).controlPlane.allocateForNewFlow(flow)
      val completeEvent = new CompleteFlowEvent(flow, SimulationEngine.currentTime + flow.Demand / flow.Rate)
      SimulationEngine.addEvent(completeEvent)
    }
  }
}
