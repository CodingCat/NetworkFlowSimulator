package scalasim.network.controlplane.routing

import scalasim.network.component.{HostType, Node, Host, Link}
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.traffic.Flow
import scala.collection.mutable.HashMap
import scalasim.simengine.utils.Logging
import scalasim.network.controlplane.ControlPlane
import org.openflow.protocol.OFMatch
import simengine.utils.IPAddressConvertor
import scala.collection.mutable
import network.controlplane.openflow.flowtable.OFMatchField


abstract private [controlplane] class RoutingProtocol (private val node : Node)
  extends Logging {

  protected lazy val controlPlane : ControlPlane = node.controlPlane

  protected val RIBIn = new HashMap[OFMatchField, Link] with mutable.SynchronizedMap[OFMatchField, Link]
  protected val RIBOut = new HashMap[OFMatchField, Link] with mutable.SynchronizedMap[OFMatchField, Link]

  protected var wildcard = OFMatch.OFPFW_ALL &
    ~OFMatch.OFPFW_NW_DST_MASK &
    ~OFMatch.OFPFW_NW_SRC_MASK

  def selectNextLink(flow : Flow, matchfield : OFMatchField, inPort : Link) : Link

  def getfloodLinks(inport: Link): List[Link] = {
    val returnList = new mutable.MutableList[Link]
    val alllink = {
      if (node.nodetype != HostType)
        node.controlPlane.topoModule.inlinks.values.toList :::
          node.controlPlane.topoModule.outlink.values.toList
      else node.controlPlane.topoModule.outlink.values.toList
    }
    alllink.foreach(l => if (l != inport) returnList += l)
    returnList.toList
  }


  def fetchInRoutingEntry(ofmatch : OFMatch) : Link = {
    val matchfield = OFFlowTable.createMatchField(ofmatch, wildcard)
    logDebug("quering matchfield: " + matchfield + "(" + matchfield.hashCode + ")" +
      " node:" + controlPlane.node)
    RIBIn.foreach(matchlinkpair => println(matchlinkpair._1 + "(" + matchlinkpair._1.hashCode +
      "):" + matchlinkpair._2))
    assert(RIBIn.contains(matchfield) == true)
    RIBIn(matchfield)
  }

  def fetchOutRoutingEntry(ofmatch : OFMatch) : Link = {
    val matchfield = OFFlowTable.createMatchField(ofmatch, wildcard)
    assert(RIBOut.contains(matchfield) == true)
    RIBOut(matchfield)
  }

  def insertOutPath (ofmatch : OFMatch, link : Link)  {
    logTrace(controlPlane + " insert outRIB entry " +
      IPAddressConvertor.IntToDecimalString(ofmatch.getNetworkSource) + "->" +
      IPAddressConvertor.IntToDecimalString(ofmatch.getNetworkDestination) +
      " with the link " + link.toString)
    val matchfield = OFFlowTable.createMatchField(ofmatch = ofmatch, wcard = wildcard)
    RIBOut += (matchfield -> link)
    if (IPAddressConvertor.DecimalStringToInt(controlPlane.IP) == matchfield.getNetworkSource) {
      RoutingProtocol.globalFlowStarterMap += controlPlane.IP -> controlPlane.node.asInstanceOf[Host]
    }
  }

  def insertInPath (ofmatch : OFMatch, link : Link) {
    val matchfield = OFFlowTable.createMatchField(ofmatch = ofmatch, wcard = wildcard)
    logTrace(controlPlane + " insert inRIB entry " + matchfield + "(" + matchfield.hashCode
      + ") -> " + link)
    RIBIn += (matchfield -> link)
    if (IPAddressConvertor.DecimalStringToInt(controlPlane.IP) == matchfield.getNetworkDestination) {
      RoutingProtocol.globalFlowStarterMap += controlPlane.IP -> controlPlane.node.asInstanceOf[Host]
    }
  }

  def deleteEntry(ofmatch : OFMatch) {
    val matchfield = OFFlowTable.createMatchField(ofmatch, wildcard)
    logTrace("delete entry:" + matchfield + " at node:" + controlPlane.IP)
    RIBIn -= matchfield
    RIBOut -= matchfield
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
