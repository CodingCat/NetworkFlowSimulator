package network.forwarding.interface

import network.device.{Link, HostType, Node}
import simengine.utils.XmlParser
import scala.collection.mutable.HashMap


trait InterfacesManager {
  private [forwarding] val outlinks = new HashMap[String, Link] // key -> destination ip

  //if the other end is a host, then the key is the ip of the host,
  //if the other end is a router, then the key is the IP range of that host
  private [forwarding] val inlinks = new HashMap[String, Link]

  def getOutLinks (ip : String) = outlinks.get(ip)
  def getInLinks (ip : String) = inlinks.get(ip)

  def registerOutgoingLink(l : Link) {
    outlinks += (l.end_to.ip_addr(0) -> l)
  }

  def registerIncomeLink(l : Link) {
    val otherEnd = l.end_from
    inlinks += otherEnd.ip_addr(0) -> l
  }

  def getNeighbour(localnode : Node, l : Link) : Node = {
    assert(l.end_from == localnode || l.end_to == localnode)
    if (l.end_to == localnode) l.end_from
    else l.end_to
  }

  def getfloodLinks(localnode: Node, inport: Link): List[Link] = {
    val alllink = {
      if (localnode.nodetype != HostType)
        inlinks.values.toList ::: outlinks.values.toList
      else outlinks.values.toList
    }
    alllink.filterNot(l => l == inport)
  }
}

object InterfacesManager {


  def apply(node : Node) = {
    XmlParser.getString("scalasim.simengine.model", "default") match {
      case "default" => new DefaultInterfacesManager(node)
      case "openflow" => node.nodetype match {
        case HostType => new DefaultInterfacesManager(node)
        case _ => new OpenFlowPortManager(node)
      }
      case _ => null
    }
  }
}