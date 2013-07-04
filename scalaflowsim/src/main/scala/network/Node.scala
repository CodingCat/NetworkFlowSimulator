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

  def create(nodeN : Int, ipPrefix : String, startIP : Int)

  def create(nodeN : Int)

  def size() : Int = nodecontainer.size

  def apply(idx : Int) = nodecontainer.apply(idx)

}

class Host () extends Node

class HostContainer extends NodeContainer {

  /**
   *
   * @param nodeN number of hosts
   * @param ipPrefix 192.168.1
   * @param startIP
   */
  def create(nodeN: Int, ipPrefix: String, startIP: Int) {
    for (i <- startIP to nodeN + startIP) nodecontainer += new Host()
  }

  def create(nodeN : Int) = {
    for (i <- 0 to nodeN) nodecontainer += new Host()
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

  /**
   *
   * @param nodeN number of routers
   * @param ipPrefix 192.168.1
   * @param startIP
   */
  def create(nodeN : Int, ipPrefix : String, startIP : Int) {
    for (i <- startIP to nodeN + startIP) nodecontainer += new Router()
  }

  def create(nodeN : Int) {
    for (i <- 0 to nodeN) nodecontainer += new Router()
  }
}

