package scalasim.network.component

import scala.collection.mutable.ListBuffer
import scalasim.network.controlplane.ControlPlane


abstract class NodeType

case object AggregateRouterType extends NodeType
case object ToRRouterType extends  NodeType
case object CoreRouterType extends NodeType
case object HostType extends NodeType

class Node (val nodetype : NodeType) {
  val ip_addr : ListBuffer[String] = new ListBuffer[String]
  val controlPlane = new ControlPlane(this)
  def assignIP(ip : String) = ip_addr += ip
  override def toString = ip_addr(0)
  def getLink(destinationIP : String) {}
}

trait NodeContainer {
  val nodecontainer = new ListBuffer[Node]()
  var idx: Int = 0;

  def create(nodeN : Int)

  def size() : Int = nodecontainer.size

  def apply(idx : Int) = nodecontainer.apply(idx)

}




