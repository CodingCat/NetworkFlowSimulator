package application

import scalasim.application.ServerApp
import network.topo.{Host, HostContainer}
import scala.collection.mutable
import scala.util.Random


//build a permulate matrix between all machines,
//each machine should be selected for only once
//and do not allow to send to itself
class PermuMatrixApp (servers : HostContainer) extends ServerApp (servers) {
  private val selectedPair = new mutable.HashMap[String, String]//destination ip -> src ip
  private val ipHostMap = new mutable.HashMap[String, Host]//ip -> host

  private def init() {
    for (i <- 0 until servers.size) {
      ipHostMap += servers(i).ip_addr(0) -> servers(i).asInstanceOf[Host]
    }
  }

  private def selectMachinePairs() {
    for (i <- 0 until servers.size) {
      var proposedIdx = Random.nextInt(servers.size)
      while (selectedPair.contains(servers(proposedIdx).ip_addr(0)) ||
        proposedIdx == i) {
        proposedIdx = Random.nextInt(servers.size)
      }
      selectedPair += servers(proposedIdx).ip_addr(0) -> servers(i).ip_addr(0)
    }
  }

  def selectedPairSize = selectedPair.size

  def run() {
    selectMachinePairs()

  }

  init()
}


