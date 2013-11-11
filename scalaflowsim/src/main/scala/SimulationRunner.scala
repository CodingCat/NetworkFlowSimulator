package root

import network.topology.{Core, Pod, GlobalDeviceManager}
import simengine.{PeriodicalEventManager, SimulationEngine}
import _root_.simengine.utils.XmlParser
import application.ApplicationRunner
import network.utils.FlowReporter
import network.events.UpdateFlowPropertyEvent
import network.topology.builder.FatTreeNetworkBuilder


object SimulationRunner {

  def reset() {
    GlobalDeviceManager.globaldevicecounter = 0
    SimulationEngine.reset()
    ApplicationRunner.reset()
    XmlParser.reset()
  }

  def main(args:Array[String]) = {
    XmlParser.loadConf(args(0))

    FatTreeNetworkBuilder.k = 4
    FatTreeNetworkBuilder.initNetwork()
    FatTreeNetworkBuilder.buildFatTreeNetwork(1.0)
    FatTreeNetworkBuilder.initOFNetwork()
    println("Warming up...")
    Thread.sleep(20 * 1000)

    ApplicationRunner.setResource(FatTreeNetworkBuilder.getAllHosts)
    ApplicationRunner.installApplication()
    ApplicationRunner.run()
    PeriodicalEventManager.event = new UpdateFlowPropertyEvent(0)
    SimulationEngine.startTime = 0.0
    SimulationEngine.endTime = 10000.0
    SimulationEngine.reporter = FlowReporter
    SimulationEngine.run()
    SimulationEngine.summary()
  }
}


