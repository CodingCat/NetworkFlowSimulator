package network.controlplane

import scalasim.simengine.SimulationEngine
import scalasim.XmlParser
import network.topo.{Link, Node, Router}
import network.controlplane.routing.RoutingProtocolFactory
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

  def allocate(flow : Flow, link : Link) : Double = resourceModule.allocate(flow, link)

  def getLinkAvailableBandwidth(l : Link) : Double = {
    resourceModule.getLinkAvailableBandwidth(l)
  }

  def decide(flow : Flow) : Unit = {
    val nextlink = routingModule.selectNextLink(flow)
    val nextnode = nextNode(nextlink)
    routingModule.insertFlowPath(flow, nextlink)
    if (nextlink.end_from == node) {
      resourceModule.allocate(flow, nextlink)
    }
    else {
      nextnode.controlPlane.allocate(flow, nextlink)
    }
    if (nextnode.ip_addr(0) != flow.DstIP) {
      nextnode.controlPlane.decide(flow)
    }
    else {
      //determine the final flow
      flow.sync()
      println("scheduling completeEvent")
      val completeEvent = new CompleteFlowEvent(flow, SimulationEngine.currentTime + flow.Demand / flow.Rate)
      SimulationEngine.addEvent(completeEvent)
    }
  }
}
