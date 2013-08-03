package scalasim.network.controlplane.routing

import scalasim.network.component.{Node, Host, Link}
import scalasim.network.traffic.Flow
import scala.collection.mutable.HashMap
import scalasim.simengine.utils.Logging
import scalasim.network.controlplane.ControlPlane
import org.openflow.protocol.OFMatch
import simengine.utils.IPAddressConvertor
import scala.collection.mutable


abstract private [controlplane] class RoutingProtocol (private val node : Node)
  extends Logging {

  protected lazy val controlPlane : ControlPlane = node.controlPlane

  protected val flowPathMap = new HashMap[OFMatch, Link] with mutable.SynchronizedMap[OFMatch, Link]

  def selectNextLink(flow : Flow, matchfield : OFMatch, inPort : Link) : Link

  def getfloodLinks(flow : Flow, inport : Link) : List[Link]

  def fetchRoutingEntry(matchfield : OFMatch) : Link = flowPathMap(matchfield)

  def insertFlowPath (matchfield : OFMatch, link : Link) {
    logTrace(controlPlane + " insert entry " +
      IPAddressConvertor.IntToDecimalString(matchfield.getNetworkSource) + "->" +
      IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination) +
      " with the link " + link.toString)
    flowPathMap += (matchfield -> link)
    if (IPAddressConvertor.DecimalStringToInt(controlPlane.IP) == matchfield.getNetworkSource) {
      RoutingProtocol.globalFlowStarterMap += controlPlane.IP -> controlPlane.node.asInstanceOf[Host]
    }
  }

  def deleteEntry(matchfield : OFMatch) {flowPathMap -= matchfield}
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
