package network.topo

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer


class Node () {
  val ip_addr : ListBuffer[String] = new ListBuffer[String]
  val outlink = new HashMap[String, Link]() // key -> destination ip
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




