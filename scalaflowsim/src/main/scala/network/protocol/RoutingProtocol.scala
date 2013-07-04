package network.protocol

import network.topo.{Router, Node}
import network.data.Flow


abstract class RoutingProtocol (val router : Router) {
  abstract def nextNode(flow : Flow) : Node
}
