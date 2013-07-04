package network.topo

import scala.collection.mutable.HashMap
import network.protocol.RoutingProtocolFactory
import scalasim.XmlParser
import network.topo.{NodeContainer, Node, Link, Host}

abstract class RouterType

case class AggregateRouter() extends RouterType
case class ToRRouter() extends  RouterType

class Router (val routertype : RouterType) extends Node {
  val inLinks = new HashMap[String, Link]() //if the other end is a host, then the key is the ip of the host,
  //if the other end is a router, then the key is the IP range of that host

  private val controlPlane = RoutingProtocolFactory.getRoutingProtocol(
    XmlParser.getString("scalasim.network.routingprotocol", "SimpleSymmetricRouting"), this)

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

