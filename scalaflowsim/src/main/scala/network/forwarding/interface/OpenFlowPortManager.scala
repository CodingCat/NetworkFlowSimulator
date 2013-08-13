package network.forwarding.interface

import network.device.{HostType, GlobalDeviceManager, Link, Node}
import scala.collection.mutable.HashMap
import org.openflow.protocol.OFPhysicalPort
import scalasim.network.controlplane.openflow.flowtable.counters.OFPortCount
import org.openflow.util.HexString


class OpenFlowPortManager(node: Node) extends DefaultInterfacesManager(node) {

  private [controlplane] val linkphysicalportsMap = new HashMap[Link, OFPhysicalPort]
  private [controlplane] val physicalportsMap = new HashMap[Short, OFPhysicalPort]//port number -> port

  private [controlplane] val portcounters = new HashMap[Short, OFPortCount]


  def getPhysicalPort(portNum : Short) = physicalportsMap.getOrElse(portNum, null)

  def getPortByLink (l : Link) = {
    assert(linkphysicalportsMap.contains(l))
    linkphysicalportsMap(l)
  }

  /**
   * choose the link via port number
   * @param portNum the port number
   * @return the link
   */
  def reverseSelection (portNum : Short) : Link = {
    linkphysicalportsMap.find(link_port_pair => link_port_pair._2 == portNum) match {
      case Some(lppair) => lppair._1
      case None => null
    }
  }

  private def addOFPhysicalPort(l : Link, portID : Short) {
    val port = new OFPhysicalPort
    GlobalDeviceManager.globaldevicecounter += 1
    //port number
    port.setPortNumber(portID)
    //port hardware address
    val hwaddrhexstr = HexString.toHexString(GlobalDeviceManager.globaldevicecounter, 6)
    port.setHardwareAddress(HexString.fromHexString(hwaddrhexstr))
    //port name
    port.setName(hwaddrhexstr.replaceAll(":", ""))
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

  override def registerOutgoingLink(l : Link) {
    super.registerOutgoingLink(l)
    addOFPhysicalPort(l, (outlink.size + inlinks.size).toShort)
  }

  override def registerIncomeLink(l : Link) {
    val otherEnd = l.end_from
    if (otherEnd.nodetype == HostType) {
      inlinks += otherEnd.ip_addr(0) -> l
    } else {
      inlinks += otherEnd.ip_addr(0) -> l
      addOFPhysicalPort(l, (outlink.size + inlinks.size).toShort)
    }
  }
}
