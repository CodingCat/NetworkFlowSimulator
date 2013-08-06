package scalasim.network.controlplane.topology

import scala.collection.mutable.HashMap
import scalasim.network.component._
import scalasim.network.controlplane.openflow.flowtable.counters.OFPortCount
import scalasim.XmlParser
import org.openflow.protocol.OFPhysicalPort
import org.openflow.util.HexString

class TopologyManager (private [controlplane] val  node : Node) {

  private [controlplane] val outlink = new HashMap[String, Link] // key -> destination ip
  private [controlplane] val inlinks = {
    //if the other end is a host, then the key is the ip of the host,
    //if the other end is a router, then the key is the IP range of that host
    if (! node.isInstanceOf[Host]) new HashMap[String, Link]
    else null
  }

  private val runningmodel = XmlParser.getString("scalasim.simengine.model", "tcp")

  private [controlplane] val linkphysicalportsMap = {
    if (runningmodel == "openflow") {
      new HashMap[Link, OFPhysicalPort]
    }
    else null
  }

  private [controlplane] val physicalportsMap = {
    if (runningmodel == "openflow") {
      new HashMap[Short, OFPhysicalPort]
    } else {
      null
    }
  }

  private [controlplane] val portcounters = {
    if (runningmodel == "openflow") {
      new HashMap[Short, OFPortCount]
    }
    else null
  }

  def getPhysicalPort(portNum : Short) = physicalportsMap.getOrElse(portNum, null)

  def getPortByLink (l : Link) = {
    assert(linkphysicalportsMap.contains(l) == true)
    linkphysicalportsMap(l)
  }

  def reverseSelection (portNum : Short) : Link = {
    if (linkphysicalportsMap == null) throw new Exception("you're not running on openflow model")
    for (link_port_pair <- linkphysicalportsMap) {
      if (link_port_pair._2.getPortNumber == portNum) return link_port_pair._1
    }
    null
  }

  private def addOFPhysicalPort(l : Link, portID : Short) {
    val port = new OFPhysicalPort
    //port number
    port.setPortNumber(portID)
    //port hardware address
    val t = node.ip_addr(0).substring(node.ip_addr(0).indexOf('.') + 1, node.ip_addr(0).size)
    val podid = HexString.toHexString(Integer.parseInt(t.substring(0, t.indexOf('.'))), 1)
    val order = HexString.toHexString(node.asInstanceOf[Router].getrid, 4)
    val portnumhex = HexString.toHexString(port.getPortNumber, 1)
    port.setHardwareAddress(HexString.fromHexString(podid + ":" + order + ":" + portnumhex))
    //port name
    //TODO:convert name into 16 bytes array
    val portname = Link.otherEnd(l, node).toString
    port.setName(portname)
    //port features
    //TODO: limited support feature?
    var feature = 0
    if (l.bandwidth == 100) {
      feature = (1 << 3)
    }
    else {
      if (l.bandwidth == 1000) feature = (1 << 5)
    }
    port.setAdvertisedFeatures(feature)
    port.setCurrentFeatures(feature)
    port.setPeerFeatures(feature)
    port.setSupportedFeatures(feature)
    port.setConfig(0)
    port.setState(0)
    linkphysicalportsMap += l -> port
    physicalportsMap += (port.getPortNumber -> port)
    portcounters += (port.getPortNumber -> new OFPortCount(port.getPortNumber))
  }

  def registerOutgoingLink(l : Link) {
    outlink += (l.end_to.ip_addr(0) -> l)
    if (! node.isInstanceOf[Host] && runningmodel == "openflow")
      addOFPhysicalPort(l, (outlink.size + inlinks.size).toShort)
  }

  def registerIncomeLink(l : Link) {
    val otherEnd = l.end_from
    if (otherEnd.isInstanceOf[Host]) inlinks += otherEnd.ip_addr(0) -> l
    if (otherEnd.isInstanceOf[Router]) inlinks += otherEnd.ip_addr(0) -> l
    if (! node.isInstanceOf[Host] && runningmodel == "openflow") addOFPhysicalPort(l,
      (outlink.size + inlinks.size).toShort)
  }

  def getNeighbour(l : Link) : Node = {
    if (l.end_from != node && l.end_to != node) {
      throw new Exception("getting neighbour error, this link :" + l.toString + " doesn't belong to this node (" +
      node.toString)
    }
    if (l.end_to == node) l.end_from
    else l.end_to
  }
}
