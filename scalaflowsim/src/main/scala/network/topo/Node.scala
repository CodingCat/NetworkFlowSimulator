package network.topo

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import network.controlplane.ControlPlane
import network.data.Flow


abstract class NodeType

case class AggregateRouterType() extends NodeType
case class ToRRouterType() extends  NodeType
case class CoreRouterType() extends NodeType
case class HostType() extends NodeType

class Node (val nodetype : NodeType) {
  val ip_addr : ListBuffer[String] = new ListBuffer[String]
  val outlink = new HashMap[String, Link]() // key -> destination ip
  val controlPlane = new ControlPlane(this)
  def assignIP(ip : String) = ip_addr += ip
  def addLink(l : Link) = outlink += (l.end_to.ip_addr(0) -> l)
}

trait NodeContainer {
  val nodecontainer = new ListBuffer[Node]()
  var idx: Int = 0;

  def create(nodeN : Int)

  def size() : Int = nodecontainer.size

  def apply(idx : Int) = nodecontainer.apply(idx)

}




