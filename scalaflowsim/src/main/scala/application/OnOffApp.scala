package application

import network.device.{Host, HostContainer}
import scala.collection.mutable.{HashSet, MultiMap, Set, HashMap}
import scala.util.Random
import network.events.StartNewFlowEvent
import network.traffic.Flow
import simengine.SimulationEngine


class OnOffApp (servers : HostContainer, onBound : Double, offBound : Double) extends ServerApp(servers) {
  private val selectedPair = new HashMap[Host, Set[Host]] with MultiMap[Host, Host]//src ip -> dst ip
  private val ipHostMap = new HashMap[String, Host]//ip -> host

  def init() {
    for (i <- 0 until servers.size) {
      ipHostMap += servers(i).ip_addr(0) -> servers(i)
    }
  }

  def insertTrafficPair(src : Host, dst : Host) {
    if (!selectedPair.contains(src)) {
      selectedPair += (src -> new HashSet[Host])
    }
    selectedPair(src) += dst
  }

  private def selectMachinePairs() {
    for (i <- 0 until servers.size) {
      var proposedIdx = Random.nextInt(servers.size)
      //currently, we allow a node to be selected for multiple times
      while (proposedIdx == i) {
        proposedIdx = Random.nextInt(servers.size)
      }
      insertTrafficPair(servers(i), servers(proposedIdx))
    }
  }

  def selectedPairSize = selectedPair.size

  def run() {
    if (selectedPair.size == 0) selectMachinePairs()
    for (srcdstPair <- selectedPair; dst <- srcdstPair._2) {
      val newflowevent = new StartNewFlowEvent(
        Flow(srcdstPair._1.ip_addr(0), dst.ip_addr(0), srcdstPair._1.mac_addr(0), dst.mac_addr(0), size = 1),
        ipHostMap(srcdstPair._1.ip_addr(0)),
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