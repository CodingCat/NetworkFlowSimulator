package scalasim.network.component

import scala.collection.mutable.ListBuffer
import scalasim.network.controlplane.openflow.OpenFlowModule
import scalasim.network.controlplane.resource.ResourceAllocator
import scalasim.network.controlplane.routing.RoutingProtocol
import scalasim.network.controlplane.{ControlPlane, TCPControlPlane}
import scalasim.network.controlplane.topology.TopologyManager
import scalasim.XmlParser


abstract class NodeType

case object AggregateRouterType extends NodeType
case object ToRRouterType extends  NodeType
case object CoreRouterType extends NodeType
case object HostType extends NodeType

class Node (val nodetype : NodeType,
  val globalDeviceId : Int) {

  val mac_addr : ListBuffer[String] = new ListBuffer[String]
  val ip_addr : ListBuffer[String] = new ListBuffer[String]

  val controlPlane : ControlPlane = {
    if (XmlParser.getString("scalasim.simengine.model", "tcp") == "tcp" || nodetype == HostType)
      new TCPControlPlane(this,
        RoutingProtocol("SimpleSymmetric", this),
        ResourceAllocator("MaxMin", controlPlane),
        new TopologyManager(this))
    else {
      if (XmlParser.getString("scalasim.simengine.model", "tcp") == "openflow" && nodetype != HostType) {
        new OpenFlowModule(this.asInstanceOf[Router],
          RoutingProtocol("OpenFlow", this),
          ResourceAllocator("MaxMin", controlPlane),
          new TopologyManager(this))
      }
      else {
        null
      }
    }
  }

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




