package root

import _root_.network.device.{Pod, GlobalDeviceManager}
import simengine.{PeriodicalEventManager, SimulationEngine}
import _root_.simengine.utils.XmlParser
import application.ApplicationRunner
import network.utils.FlowReporter
import network.events.UpdateFlowPropertyEvent


object SimulationRunner {

  def reset() {
    GlobalDeviceManager.globaldevicecounter = 0
    SimulationEngine.reset()
    ApplicationRunner.reset()
    XmlParser.reset()
  }

  def main(args:Array[String]) = {
    XmlParser.loadConf(args(0))
    val cellnet = new Pod(1, 4, 8, 20)
    println("Warming up...")
    Thread.sleep(20 * 1000)
    ApplicationRunner.setResource(cellnet.getAllHostsInPod)
    ApplicationRunner.installApplication()
    ApplicationRunner.run()
    PeriodicalEventManager.event = new UpdateFlowPropertyEvent(0)
    SimulationEngine.startTime = 0.0
    SimulationEngine.endTime = 10000.0
    SimulationEngine.reporter = FlowReporter
    SimulationEngine.run()
    SimulationEngine.summary()
    //cellnet.shutDownOpenFlowNetwork()
   /* val pod = new Pod(0, 1, 1, 20)
    Thread.sleep(1000 * 20)
    val flow1 = Flow(pod.getHost(0, 0).toString, pod.getHost(0, 1).toString,
      pod.getHost(0, 0).mac_addr(0), pod.getHost(0, 1).mac_addr(0), appDataSize = 1)
    val flow2 = Flow(pod.getHost(0, 1).toString, pod.getHost(0, 0).toString,
      pod.getHost(0, 1).mac_addr(0), pod.getHost(0, 0).mac_addr(0), appDataSize = 1)
    SimulationEngine.addEvent(new StartNewFlowEvent(flow1, pod.getHost(0, 0), 0))
    SimulationEngine.addEvent(new StartNewFlowEvent(flow2, pod.getHost(0, 1), 0))
    SimulationEngine.reporter = FlowReporter
    SimulationEngine.run
    SimulationEngine.summary()  */
    //pod.shutDownOpenFlowNetwork()
   /* XmlParser.addProperties("scalasim.simengine.model", "openflow")
    val pod = new Pod(1, 2, 4, 20)
    val flow1 = Flow(pod.getHost(0, 1).toString, pod.getHost(1, 1).toString,
      pod.getHost(0, 1).mac_addr(0), pod.getHost(1, 1).mac_addr(0), appDataSize = 1)
    Thread.sleep(1000 * 20)
    val flow2 = Flow(pod.getHost(3, 1).toString, pod.getHost(2, 1).toString,
      pod.getHost(3, 1).mac_addr(0), pod.getHost(2, 1).mac_addr(0), appDataSize = 1)
    SimulationEngine.addEvent(new StartNewFlowEvent(flow1, pod.getHost(0, 1), 0))
    SimulationEngine.addEvent(new StartNewFlowEvent(flow2, pod.getHost(3, 1), 0))
    SimulationEngine.reporter = FlowReporter
    SimulationEngine.run
    SimulationEngine.summary()*/

  }
}


