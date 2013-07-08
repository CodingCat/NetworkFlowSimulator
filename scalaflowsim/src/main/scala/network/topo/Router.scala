package network.topo

import scala.collection.mutable.HashMap
import scalasim.XmlParser
import network.data.Flow
import network.controlplane.routing.RoutingProtocolFactory
import network.controlplane.ControlPlane

class Router (nodetype : NodeType) extends Node(nodetype) {
  val inLinks = new HashMap[String, Link]() //if the other end is a host, then the key is the ip of the host,
  //if the other end is a router, then the key is the IP range of that host

  def registerIncomeLink(l : Link) {
    val otherEnd = l.end_from
    if (otherEnd.isInstanceOf[Host]) inLinks += otherEnd.ip_addr(0) -> l
    if (otherEnd.isInstanceOf[Router]) inLinks += otherEnd.ip_addr(0) -> l
  }

  def receiveFlow(flow : Flow) : Node = controlPlane.nextNode(flow)
}

class RouterContainer () extends NodeContainer {
  def create(nodeN : Int, rtype : NodeType) {
    for (i <- 0 until nodeN) nodecontainer += new Router(rtype)
  }

  def create(nodeN : Int) {
    throw new RuntimeException("you have to specify the router type")
  }
}

