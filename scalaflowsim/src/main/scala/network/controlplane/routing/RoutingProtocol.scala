package network.controlplane.routing

import network.topo.{Host, Link, Node}
import network.data.Flow
import scala.collection.mutable.HashMap
import scala.collection.mutable
import simengine.utils.Logging


abstract private [controlplane] class RoutingProtocol (val node : Node) extends Logging {
  protected val flowPathMap = new HashMap[Flow, Link]

  def selectNextLink(flow : Flow) : Link

  def insertFlowPath (flow : Flow, link : Link) {
    logTrace(node + " insert entry " + node + "->" + flow.DstIP)
    flowPathMap += (flow -> link)
    if (node.ip_addr(0) == flow.SrcIP) {
      RoutingProtocol.globalFlowStarterMap += flow.SrcIP -> node.asInstanceOf[Host]
    }
  }

  def getLink(flow : Flow) : Link = flowPathMap(flow)
}

object RoutingProtocol {
  protected val globalFlowStarterMap = new HashMap[String, Host]

  def getFlowStarter (ip : String) = globalFlowStarterMap(ip)
}
