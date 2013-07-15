package network.controlplane.routing

import network.component.{Host, Link, Node}
import network.traffic.Flow
import scala.collection.mutable.HashMap
import scala.collection.mutable
import simengine.utils.Logging
import network.controlplane.ControlPlane


abstract private [controlplane] class RoutingProtocol (protected val controlPlane : ControlPlane)
  extends Logging {
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

  def apply (name : String, controlPlane : ControlPlane) : RoutingProtocol = name match {
    case "SimpleSymmetricRouting" => new SimpleSymmetricRouting(controlPlane)
    case _ => throw new Exception("unrecognizable routing protocol type")
  }
}
