package network.protocol

import network.topo.Router


object RoutingProtocolFactory {
  def getRoutingProtocol (name : String, router : Router) = name match {
    case "SimpleSymmetricRouting" => new SimpleSymmetricRouting(router)
  }
}
