package network.forwarding.interface

import network.device.{Link, HostType, Node}
import simengine.utils.XmlParser
import scala.collection.mutable.HashMap
import scala.collection.mutable


trait InterfacesManager {
  private [controlplane] val outlink = new HashMap[String, Link] // key -> destination ip

  //if the other end is a host, then the key is the ip of the host,
  //if the other end is a router, then the key is the IP range of that host
  private [controlplane] val inlinks = new HashMap[String, Link]

  def registerOutgoingLink(l : Link) {
    outlink += (l.end_to.ip_addr(0) -> l)
  }

  def registerIncomeLink(l : Link) {
    val otherEnd = l.end_from
    if (otherEnd.nodetype == HostType) {
      inlinks += otherEnd.ip_addr(0) -> l
    } else {
      inlinks += otherEnd.ip_addr(0) -> l
    }
  }

  def getNeighbour(localnode : Node, l : Link) : Node = {
    assert(l.end_from == localnode || l.end_to == localnode)
    if (l.end_to == localnode) l.end_from
    else l.end_to
  }

  def getfloodLinks(localnode: Node, inport: Link): List[Link] = {
    val alllink = {
      if (localnode.nodetype != HostType)
        inlinks.values.toList ::: outlink.values.toList
      else outlink.values.toList
    }
    alllink.filterNot(l => l == inport)
  }
}

object InterfacesManager {

  private val runningModel = XmlParser.getString("scalasim.simengine.model", "default")

  def apply(node : Node) = {
    runningModel match {
      case "default" => new DefaultInterfacesManager(node)
      case "openflow" => node.nodetype match {
        case HostType => new DefaultInterfacesManager(node)
        case _ => new OpenFlowPortManager(node)
      }
      case _ => null
    }
  }
}
