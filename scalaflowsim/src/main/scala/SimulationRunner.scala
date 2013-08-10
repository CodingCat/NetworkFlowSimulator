package scalasim

import _root_.network.device.GlobalDeviceManager
import _root_.simengine.ofconnector.FloodlightConnector
import scalasim.application.ApplicationRunner
import scalasim.network.events.StartNewFlowEvent
import scalasim.network.traffic.Flow
import scalasim.simengine.SimulationEngine
import scalasim.network.component.Pod


object SimulationRunner {

  def reset {
    GlobalDeviceManager.globaldevicecounter = 0
    SimulationEngine.reset
    ApplicationRunner.reset
    XmlParser.reset
  }

  def main(args:Array[String]) = {
    XmlParser.addProperties("scalasim.simengine.model", "openflow")
    val pod = new Pod(1, 2, 4, 15)
    /*FloodlightConnector.readUrl(
    "http://localhost:8080/wm/topology/switchclusters/json",
    "{\"00:01:00:00:00:00:00:3c\":[\"00:01:00:00:00:00:00:3c\"," +
    "\"00:01:00:00:00:00:00:3d\",\"00:01:00:00:00:00:00:3e\"," +
    "\"01:01:00:00:00:00:00:40\",\"01:01:00:00:00:00:00:41\"," +
    "\"00:01:00:00:00:00:00:3f\"]}"
    ) */
    Thread.sleep(1000 * 20)
    val flow1 = Flow(pod.getHost(0, 1).toString, pod.getHost(1, 1).toString,
      pod.getHost(0, 1).mac_addr(0), pod.getHost(1, 1).mac_addr(0), size = 1)
    val flow2 = Flow(pod.getHost(3, 1).toString, pod.getHost(2, 1).toString,
      pod.getHost(3, 1).mac_addr(0), pod.getHost(2, 1).mac_addr(0), size = 1)
    SimulationEngine.addEvent(new StartNewFlowEvent(flow1, pod.getHost(0, 1), 0))
    SimulationEngine.addEvent(new StartNewFlowEvent(flow2, pod.getHost(3, 1), 0))
    SimulationEngine.run
    //assert(flow1.status === CompletedFlow)
    //assert(flow2.status === CompletedFlow)
    //pod.shutDownOpenFlowNetwork()
  }
}


