package root

import _root_.network.device.{Pod, GlobalDeviceManager}
import _root_.network.traffic.Flow
import _root_.simengine.SimulationEngine
import _root_.simengine.utils.XmlParser
import application.ApplicationRunner
import network.events.StartNewFlowEvent


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
    Thread.sleep(1000 * 20)
    val flow1 = Flow(pod.getHost(0, 1).toString, pod.getHost(1, 1).toString,
      pod.getHost(0, 1).mac_addr(0), pod.getHost(1, 1).mac_addr(0), size = 1)
    val flow2 = Flow(pod.getHost(3, 1).toString, pod.getHost(2, 1).toString,
      pod.getHost(3, 1).mac_addr(0), pod.getHost(2, 1).mac_addr(0), size = 1)
    SimulationEngine.addEvent(new StartNewFlowEvent(flow1, pod.getHost(0, 1), 0))
    SimulationEngine.addEvent(new StartNewFlowEvent(flow2, pod.getHost(3, 1), 0))
    SimulationEngine.run
  }
}


