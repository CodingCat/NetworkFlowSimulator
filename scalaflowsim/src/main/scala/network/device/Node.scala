package network.device

import scala.collection.mutable.ListBuffer
import network.forwarding.controlplane.{RoutingProtocol, DefaultControlPlane}
import simengine.utils.XmlParser
import network.forwarding.dataplane.ResourceAllocator
import network.device.interface.InterfacesManager
import network.forwarding.interface.InterfacesManager

abstract class NodeType

case object AggregateRouterType extends NodeType
case object ToRRouterType extends  NodeType
case object CoreRouterType extends NodeType
case object HostType extends NodeType

class Node (val nodetype : NodeType,
  val globalDeviceId : Int) {

  val mac_addr : ListBuffer[String] = new ListBuffer[String]
  val ip_addr : ListBuffer[String] = new ListBuffer[String]

  val controlplane = RoutingProtocol(this)

  val dataplane =  ResourceAllocator(this)

  val interfacesManager = InterfacesManager(this)

  def assignIP(ip : String) {
    ip_addr += ip
  }

  def assignMac(mac : String) {
    mac_addr += mac
  }

  override def toString = {
    if (ip_addr.length > 0) ip_addr(0)
    else "new initialized node"
  }

  def getLink(destinationIP : String) {}
}

trait NodeContainer {
  val nodecontainer = new ListBuffer[Node]()
  var idx: Int = 0;

  def create(nodeN : Int)

  def size() : Int = nodecontainer.size

  def apply(idx : Int) = nodecontainer.apply(idx)

}




