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
    XmlParser.loadConf(args(0))
    val cellnet = new Pod(1)
    println("Warming up...")
    Thread.sleep(20 * 1000)
    ApplicationRunner.setResource(cellnet.getAllHostsInPod)
    ApplicationRunner.installApplication()
    ApplicationRunner.run()
    SimulationEngine.startTime = 0.0
    SimulationEngine.endTime = 1000.0
    SimulationEngine.run
    SimulationEngine.summary()
    /*val pod = new Pod(0, 1, 1, 20)
    Thread.sleep(1000 * 20)
    val flow1 = Flow(pod.getHost(0, 0).toString, pod.getHost(0, 1).toString,
      pod.getHost(0, 0).mac_addr(0), pod.getHost(0, 1).mac_addr(0), demand = 1)
    val flow2 = Flow(pod.getHost(0, 1).toString, pod.getHost(0, 0).toString,
      pod.getHost(0, 1).mac_addr(0), pod.getHost(0, 0).mac_addr(0), demand = 1)
    SimulationEngine.addEvent(new StartNewFlowEvent(flow1, pod.getHost(0, 0), 0))
    SimulationEngine.addEvent(new StartNewFlowEvent(flow2, pod.getHost(0, 1), 0))
    SimulationEngine.run*/
    //pod.shutDownOpenFlowNetwork()
  }
}


