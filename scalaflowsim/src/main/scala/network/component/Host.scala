package network.component


class Host (hosttype : NodeType) extends Node (hosttype) {

}

class HostContainer extends NodeContainer {
  //private val apps = ListBuffer[Application]

  def create(nodeN : Int) {
    for (i <- 0 until nodeN) nodecontainer += new Host(HostType)
  }

  def addHost(servers : HostContainer) {
    for (i <- 0 until servers.size) nodecontainer += servers(i)
  }
}