package network.controlplane.topology

import scala.collection.mutable.HashMap
import network.component.{Router, Host, HostType, Link}
import network.controlplane.ControlPlane

class TopologyManager (private val cp : ControlPlane) {

  private [controlplane] val outlink = new HashMap[String, Link] // key -> destination ip
  private [controlplane] val inlinks = {
    //if the other end is a host, then the key is the ip of the host,
    //if the other end is a router, then the key is the IP range of that host
    if (cp.node.nodetype != HostType) new HashMap[String, Link]
    else null
  }

  def registerOutgoingLink(l : Link) {
    outlink += (l.end_to.ip_addr(0) -> l)
  }

  def registerIncomeLink(l : Link) {
    val otherEnd = l.end_from
    if (otherEnd.isInstanceOf[Host]) inlinks += otherEnd.ip_addr(0) -> l
    if (otherEnd.isInstanceOf[Router]) inlinks += otherEnd.ip_addr(0) -> l
  }
}
