package network.controlplane

import scalasim.XmlParser
import network.topo.{Link, Node, Router}
import network.controlplane.routing.RoutingProtocolFactory
import network.data.Flow
import network.controlplane.resource.ResourceAllocatorFactory


class ControlPlane(node : Node) {
  protected val routingModule = RoutingProtocolFactory.getRoutingProtocol(
    XmlParser.getString("scalasim.router.routing", "SimpleSymmetricRouting"), node)
  protected val resourceModule = ResourceAllocatorFactory.getResourceAllocator(
    XmlParser.getString("scalasim.router.resource", "MaxMin"), node)


  def nextNode(flow : Flow) : Node = routingModule.nextNode(flow)

  def allocate(flow : Flow, link : Link) = resourceModule.allocate(flow, link)

  def decide(flow : Flow) : Unit = {
    val node = nextNode(flow)
    if (node.ip_addr(0) != flow.DstIP) {
      node.controlPlane.decide(flow)
    }

  }
}
