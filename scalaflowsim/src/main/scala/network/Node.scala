package scalasim.network

import scala.collection.mutable.HashMap;
import scala.collection.mutable.ListBuffer;

class Node (ip_address:String) {
  protected val outlink = new HashMap[String, Link](); // key -> destination ip
}

trait NodeContainer {
  val nodecontainer = new ListBuffer[Node]();

  def create(nodeN : Int, ipPrefix : String, startIP : Int);

  def create(nodeN : Int);
}

class Host (ip_address:String) extends Node (ip_address);

class HostContainer extends NodeContainer {

  /**
   *
   * @param nodeN number of hosts
   * @param ipPrefix 192.168.1
   * @param startIP
   */
  def create(nodeN: Int, ipPrefix: String, startIP: Int) {
    for (i <- startIP to nodeN + startIP) nodecontainer += new Host(ipPrefix + "." + i);
  }

  def create(nodeN : Int) = {
    for (i <- 0 to nodeN) nodecontainer += new Host(null);
  }
}

class Router (ip_address : String) extends Node (ip_address);

class RouterContainer () extends NodeContainer {
  /**
   *
   * @param nodeN number of routers
   * @param ipPrefix 192.168.1
   * @param startIP
   */
  def create(nodeN : Int, ipPrefix : String, startIP : Int) = {
    for (i <- startIP to nodeN + startIP) nodecontainer += new Router(ipPrefix + "." + i);
  }

  def create(nodeN : Int) = {
    for (i <- 0 to nodeN) nodecontainer += new Router(null);
  }
}

