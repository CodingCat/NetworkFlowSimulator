package network.protocol

import network.topo.{Router, Node}
import network.data.Flow


abstract class RoutingProtocol (val router : Router) {
  def nextNode(flow : Flow) : Node
}
