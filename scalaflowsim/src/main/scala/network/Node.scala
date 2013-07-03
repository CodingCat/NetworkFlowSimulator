package scalasim.network

import scala.collection.mutable.HashMap;
import scala.collection.mutable.ListBuffer;

class Node () {
  protected val ip_addr : Array[String] = null
  protected val outlink = new HashMap[String, Link]() // key -> destination ip
  def assignIP(ip : String) = ip_addr(Math.max(0, ip_addr.length - 1)) = ip
}

trait NodeContainer {
  val nodecontainer = new ListBuffer[Node]()
  var idx: Int = 0;

  def create(nodeN : Int, ipPrefix : String, startIP : Int)

  def create(nodeN : Int)

  def size() : Int = nodecontainer.size

  def apply(idx : Int) = nodecontainer.apply(idx)
}

class Host () extends Node ()

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

class Router () extends Node ()

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

