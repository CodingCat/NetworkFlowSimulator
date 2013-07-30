package scalasim.network.controlplane.routing

import scalasim.network.component.{Node, Link}
import scalasim.network.traffic.Flow

class OpenFlowRouting (node : Node) extends RoutingProtocol (node) {

  def selectNextLink(flow: Flow): Link = ???

}
