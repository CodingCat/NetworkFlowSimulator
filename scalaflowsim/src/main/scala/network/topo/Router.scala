package network.topo

import scala.collection.mutable.HashMap
import network.protocol.RoutingProtocolFactory
import scalasim.XmlParser
import network.data.Flow

abstract class RouterType

case class AggregateRouterType() extends RouterType
case class ToRRouterType() extends  RouterType
case class CoreRouterType() extends RouterType

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

  def receiveFlow(flow : Flow) : Node = controlPlane.nextNode(flow)
}

class RouterContainer () extends NodeContainer {
  def create(nodeN : Int, rtype : RouterType) {
    for (i <- 0 until nodeN) nodecontainer += new Router(rtype)
  }

  def create(nodeN : Int) {
    throw new RuntimeException("you have to specify the router type")
  }
}

