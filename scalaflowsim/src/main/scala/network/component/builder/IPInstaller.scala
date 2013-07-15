package network.component.builder

import network.component.{NodeContainer, Node}

object IPInstaller {

  /**
   * assign IP to a single node
   * @param node the node to be assigned
   * @param ip ip address
   */
  def assignIPAddress(node : Node, ip : String) {
    node.assignIP(ip)
  }

  /**
   * assign ip address to a set of ndoes
   * @param ipbase specifying the C range of the ip address
   * @param startAddress, the startaddress of the addresses to be assigned
   * @param nodes, the node container which containing the address
   * @param startIdx,start idx of hte node in the container
   * @param endIdx, the end of hte node in the container
   */
  def assignIPAddress (ipbase : String,
                       startAddress : Int,
                       nodes : NodeContainer,
                       startIdx : Int,
                       endIdx : Int) {
    val ip_prefix : String =  ipbase.substring(0, ipbase.lastIndexOf('.') + 1)
    for (i <- startIdx to endIdx)
      nodes(i).assignIP(ip_prefix + (startAddress + i - startIdx).toString)
  }
}