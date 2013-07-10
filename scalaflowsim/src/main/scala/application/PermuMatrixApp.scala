package application

import scalasim.application.ServerApp
import network.topo.{Host, HostContainer}
import scala.collection.mutable.{ListMap, MultiMap, HashMap, Set}
import scala.util.Random
import scalasim.simengine.SimulationEngine
import network.events.StartNewFlowEvent
import network.data.Flow


//build a permulate matrix between all machines,
//each machine should be selected for only once
//and do not allow to send to itself
class PermuMatrixApp (servers : HostContainer) extends ServerApp (servers) {
  private val selectedPair = new HashMap[String, String] //src ip -> dst ip
  private val ipHostMap = new HashMap[String, Host]//ip -> host

  def init() {
    for (i <- 0 until servers.size) {
      ipHostMap += servers(i).ip_addr(0) -> servers(i).asInstanceOf[Host]
    }
  }

  private def selectMachinePairs() {
    for (i <- 0 until servers.size) {
      var proposedIdx = Random.nextInt(servers.size)
      //currently, we allow a node to be selected for multiple times
      while (proposedIdx == i) {
        proposedIdx = Random.nextInt(servers.size)
      }
      selectedPair += (servers(i).ip_addr(0) -> servers(proposedIdx).ip_addr(0))
    }
  }

  def selectedPairSize = selectedPair.size

  def run() {
    selectMachinePairs()
    for (srcdstPair <- selectedPair) {
      val newflowevent = new StartNewFlowEvent(Flow(srcdstPair._1, srcdstPair._2, 100000), ipHostMap(srcdstPair._1),
        SimulationEngine.currentTime)
      SimulationEngine.addEvent(newflowevent)
    }
  }

  def reset() {
    selectedPair.clear()
    ipHostMap.clear()
  }

  init()
}


