package application

import network.topology.{Host, HostContainer}
import scala.util.Random
import network.events.{FlowOffEvent, StartNewFlowEvent}
import network.traffic.Flow
import simengine.SimulationEngine
import scala.collection.immutable.HashMap


class OnOffApp (servers : HostContainer) extends ServerApp(servers) {
  private var selectedPair = new HashMap[Host, Host]

  private var selectedHost = List[Int]()

  private def selectMachinePairs() {
    for (i <- 0 until servers.size) {
      var proposedIdx = Random.nextInt(servers.size())
      while (selectedHost.contains(proposedIdx) ||
        proposedIdx == i) {
        proposedIdx = Random.nextInt(servers.size())
      }
      selectedHost = i :: selectedHost
      selectedPair += servers(i) -> servers(proposedIdx)
    }
  }


  def run() {
    if (selectedPair.size == 0) selectMachinePairs()
    for (srcdstPair <- selectedPair; dst <- srcdstPair._2) {
      val flow = Flow(srcdstPair._1.ip_addr(0), srcdstPair._2.ip_addr(0),
        srcdstPair._1.mac_addr(0), srcdstPair._2.mac_addr(0),
        appDataSize = 100)
      val newflowevent = new StartNewFlowEvent(
        flow,
        srcdstPair._1,
        SimulationEngine.currentTime)
      SimulationEngine.addEvent(newflowevent)
      //start a off
      SimulationEngine.addEvent(new FlowOffEvent(flow,
        SimulationEngine.currentTime + Random.nextInt(OnOffApp.offLength)))
    }
  }

  def reset() {
    selectedPair = new HashMap[Host, Host]
  }
}

object OnOffApp {
  val onLength = 10
  val offLength = 5
}
