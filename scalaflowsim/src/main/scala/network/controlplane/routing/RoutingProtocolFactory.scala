package network.controlplane.routing

import network.component.Node
import network.controlplane.ControlPlane


private [controlplane] object RoutingProtocolFactory {
  def getRoutingProtocol (name : String, controlPlane : ControlPlane) : RoutingProtocol = name match {
    case "SimpleSymmetricRouting" => new SimpleSymmetricRouting(controlPlane)
    case _ => throw new Exception("unrecognizable routing protocol type")
  }
}
