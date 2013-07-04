package scalasim.network

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

class Host () extends Node

class HostContainer extends NodeContainer {

  def create(nodeN : Int) = {
    for (i <- 0 until nodeN) nodecontainer += new Host()
  }
}

class Router () extends Node {
  val inLinks = new HashMap[String, Link]() //if the other end is a host, then the key is the ip of the host,
  //if the other end is a router, then the key is the IP range of that host

  def registerIncomeLink(l : Link) {
    val otherEnd = l.end_from
    if (otherEnd.isInstanceOf[Host]) inLinks += otherEnd.ip_addr(0) -> l
    if (otherEnd.isInstanceOf[Router]) inLinks += otherEnd.ip_addr(0) -> l
  }
}

class RouterContainer () extends NodeContainer {

  def create(nodeN : Int) {
    for (i <- 0 until nodeN) nodecontainer += new Router()
  }
}

