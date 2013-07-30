package scalasim.network.controlplane.routing

import scalasim.network.component.{Node, Host, Link}
import scalasim.network.controlplane.openflow.OpenFlowModule
import scalasim.network.traffic.Flow
import scala.collection.mutable.HashMap
import scalasim.simengine.utils.Logging
import scalasim.network.controlplane.{ControlPlane, TCPControlPlane}


abstract private [controlplane] class RoutingProtocol (private val node : Node)
  extends Logging {

  protected lazy val controlPlane : ControlPlane = node.controlPlane

  protected val flowPathMap = new HashMap[Flow, Link]

  def selectNextLink(flow : Flow) : Link

  def fetchRoutingEntry(flow : Flow) : Link = flowPathMap(flow)

  def insertFlowPath (flow : Flow, link : Link) {
    logTrace(controlPlane + " insert entry " + controlPlane + "->" + flow.DstIP)
    flowPathMap += (flow -> link)
    if (controlPlane.IP == flow.SrcIP) {
      RoutingProtocol.globalFlowStarterMap += flow.SrcIP -> controlPlane.node.asInstanceOf[Host]
    }
  }

  def deleteEntry(flow : Flow) {flowPathMap -= flow}
}

private [network] object RoutingProtocol {
  protected val globalFlowStarterMap = new HashMap[String, Host]

  def getFlowStarter (ip : String) = globalFlowStarterMap(ip)

  def apply (name : String, node : Node) : RoutingProtocol = name match {
    case "SimpleSymmetric" => new SimpleSymmetricRouting(node)
    case "OpenFlow" => new OpenFlowRouting(node)
    case _ => throw new Exception("unrecognizable routing protocol type")
  }
}
