package network.controlplane.routing

import network.topo.Node


private [controlplane] object RoutingProtocolFactory {
  def getRoutingProtocol (name : String, node : Node) : RoutingProtocol = name match {
    case "SimpleSymmetricRouting" => new SimpleSymmetricRouting(node)
  }
}
