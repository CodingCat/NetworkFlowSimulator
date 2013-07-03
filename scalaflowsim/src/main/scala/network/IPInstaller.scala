package network

import scalasim.network.NodeContainer
import scalasim.network.Node

object IPInstaller {

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
    for (i <- startIdx to endIdx; j <- startAddress to startAddress + (endIdx - startIdx))
      nodes(i).assignIP(ip_prefix + j.toString)
  }
}
