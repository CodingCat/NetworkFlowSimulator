package network

import scala.collection.mutable.HashMap
import scalasim.network.{NodeContainer, Node, Link}

abstract class RouterType

case class AggregateRouter() extends RouterType
case class ToRRouter() extends  RouterType

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

