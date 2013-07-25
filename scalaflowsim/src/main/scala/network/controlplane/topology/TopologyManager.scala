package scalasim.network.controlplane.topology

import scala.collection.mutable.HashMap
import scalasim.network.component.{Router, Host, HostType, Link}
import scalasim.network.controlplane.ControlPlane
import scalasim.network.util.AddressNameConvertor
import scalasim.XmlParser
import java.util
import org.openflow.protocol.OFPhysicalPort
import org.openflow.util.HexString

class TopologyManager (private val cp : ControlPlane) {

  private [controlplane] val outlink = new HashMap[String, Link] // key -> destination ip
  private [controlplane] val inlinks = {
    //if the other end is a host, then the key is the ip of the host,
    //if the other end is a router, then the key is the IP range of that host
    if (cp.node.nodetype != HostType) new HashMap[String, Link]
    else null
  }

  private val runningmodel = XmlParser.getString("scalasim.simengine.model", "tcp")

  private [controlplane] val physicalports = {
    if (runningmodel == "openflow") {
      new util.ArrayList[OFPhysicalPort]
    }
    else null
  }

  private def addOFPhysicalPort(l : Link, portID : Short) {
    val port = new OFPhysicalPort
    //port number
    port.setPortNumber(portID)
    //port hardware address
    val t = cp.node.ip_addr(0).substring(cp.node.ip_addr(0).indexOf('.') + 1, cp.node.ip_addr(0).size)
    val podid = HexString.toHexString(Integer.parseInt(t.substring(0, t.indexOf('.'))), 1)
    val order = HexString.toHexString(cp.node.asInstanceOf[Router].getrid, 4)
    val portnumhex = HexString.toHexString(port.getPortNumber, 1)
    port.setHardwareAddress(HexString.fromHexString(podid + ":" + order + ":" + portnumhex))
    //port name
    //TODO:convert name into 16 bytes array
    val portname = {
      if (l.end_from == cp.node) l.end_to.toString
      else l.end_from.toString
    }
    port.setName(portname)
    //port features
    //TODO: limited support feature?
    var feature = 0
    if (l.bandwidth == 100) {
      feature = (1 << 3) | (1 << 12) | (1 << 13)
    }
    else {
      if (l.bandwidth == 1000) feature = (1 << 5) | (1 << 12) | (1 << 13)
    }
    port.setAdvertisedFeatures(feature)
    port.setCurrentFeatures(feature)
    port.setPeerFeatures(feature)
    port.setSupportedFeatures(feature)
    physicalports.add(port)
  }

  def registerOutgoingLink(l : Link) {
    outlink += (l.end_from.ip_addr(0) -> l)
    if (cp.node.isInstanceOf[Router] && runningmodel == "openflow") addOFPhysicalPort(l, outlink.size.toShort)
  }

  def registerIncomeLink(l : Link) {
    val otherEnd = l.end_from
    if (otherEnd.isInstanceOf[Host]) inlinks += otherEnd.ip_addr(0) -> l
    if (otherEnd.isInstanceOf[Router]) inlinks += otherEnd.ip_addr(0) -> l
    if (cp.node.isInstanceOf[Router] && runningmodel == "openflow") addOFPhysicalPort(l, inlinks.size.toShort)
  }
}
