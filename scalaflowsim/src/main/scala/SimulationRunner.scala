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
    ApplicationRunner.setResource(cellnet.getAllHostsInPod)
    ApplicationRunner.installApplication()
    ApplicationRunner.run()
    SimulationEngine.run
    SimulationEngine.summary()
  }
}


