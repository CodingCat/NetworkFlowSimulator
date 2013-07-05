package network.topo

import scala.collection.mutable.ListBuffer


class Host () extends Node

class HostContainer extends NodeContainer {
  //private val apps = ListBuffer[Application]

  def create(nodeN : Int) {
    for (i <- 0 until nodeN) nodecontainer += new Host()
  }

  def addHost(servers : HostContainer) {
    for (i <- 0 until servers.size) nodecontainer += servers(i)
  }
}
