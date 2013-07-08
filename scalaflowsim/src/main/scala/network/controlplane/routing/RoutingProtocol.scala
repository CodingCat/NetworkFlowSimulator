package network.controlplane.routing

import network.topo.{Link, Node}
import network.data.Flow
import scala.collection.mutable.HashMap


abstract private [controlplane] class RoutingProtocol (val node : Node) {
  protected val flowPathMap = new HashMap[Flow, Link]

  def nextNode(flow : Flow) : Node

  def insertFlowPath (flow : Flow, link : Link) {
    flowPathMap += (flow -> link)
  }

  def getLink(flow : Flow) : Link = flowPathMap(flow)
}
