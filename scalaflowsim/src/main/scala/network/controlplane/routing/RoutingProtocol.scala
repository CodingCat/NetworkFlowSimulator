package scalasim.network.controlplane.routing

import scalasim.network.component.{HostType, Node, Host, Link}
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

  protected val RIBIn = new HashMap[OFMatch, Link] with mutable.SynchronizedMap[OFMatch, Link]
  protected val RIBOut = new HashMap[OFMatch, Link] with mutable.SynchronizedMap[OFMatch, Link]

  def selectNextLink(flow : Flow, matchfield : OFMatch, inPort : Link) : Link

  def getfloodLinks(flow: Flow, inport: Link): List[Link] = {
    assert(flow.floodflag == true)
    val returnList = new mutable.MutableList[Link]
    val alllink = {
      if (node.nodetype != HostType)
        node.controlPlane.topoModule.inlinks.values.toList :::
          node.controlPlane.topoModule.outlink.values.toList
      else node.controlPlane.topoModule.outlink.values.toList
    }
    alllink.foreach(l => if (l != inport) returnList += l)
    //add to flow's flood trace
    returnList.foreach(l => flow.addTrace(l, inport))
    returnList.toList
  }

  def floodoutFlow(flow : Flow, matchfield : OFMatch, inlink : Link) {
    val nextlinks = getfloodLinks(flow, inlink)
    //TODO : openflow flood handling in which nextlinks can be null?
    nextlinks.foreach(l => Link.otherEnd(l, node).controlPlane.routing(flow, matchfield, l))
  }

  def fetchInRoutingEntry(matchfield : OFMatch) : Link = RIBIn(matchfield)
  def fetchOutRoutingEntry(matchfield : OFMatch) : Link = RIBOut(matchfield)

  def insertOutPath (matchfield : OFMatch, link : Link) {
    logTrace(controlPlane + " insert outRIB entry " +
      IPAddressConvertor.IntToDecimalString(matchfield.getNetworkSource) + "->" +
      IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination) +
      " with the link " + link.toString)
    RIBOut += (matchfield -> link)
    if (IPAddressConvertor.DecimalStringToInt(controlPlane.IP) == matchfield.getNetworkSource) {
      RoutingProtocol.globalFlowStarterMap += controlPlane.IP -> controlPlane.node.asInstanceOf[Host]
    }
  }

  def insertInPath (matchfield : OFMatch, link : Link) {
    logTrace(controlPlane + " insert inRIB entry " +
      IPAddressConvertor.IntToDecimalString(matchfield.getNetworkSource) + "->" +
      IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination) +
      " with the link " + link.toString)
    RIBIn += (matchfield -> link)
    if (IPAddressConvertor.DecimalStringToInt(controlPlane.IP) == matchfield.getNetworkDestination) {
      RoutingProtocol.globalFlowStarterMap += controlPlane.IP -> controlPlane.node.asInstanceOf[Host]
    }
  }

  def deleteInEntry(matchfield : OFMatch) {RIBIn -= matchfield}

  def deleteOutEntry(matchfield : OFMatch) {RIBOut -= matchfield}

  def deleteEntry(matchfield : OFMatch) {
    deleteInEntry(matchfield)
    deleteOutEntry(matchfield)
  }
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
