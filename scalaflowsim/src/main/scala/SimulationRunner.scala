package scalasim

import scalasim.application.ApplicationRunner
import scalasim.network.events.StartNewFlowEvent
import scalasim.network.traffic.Flow
import scalasim.simengine.SimulationEngine
import scalasim.network.component.Pod


object SimulationRunner {

  def reset {
    SimulationEngine.reset
    ApplicationRunner.reset
    XmlParser.reset
  }

  def main(args:Array[String]) = {
    XmlParser.addProperties("scalasim.simengine.model", "openflow")
    XmlParser.loadConf("config.xml")
    val pod = new Pod(1, 1, 2, 1)
    val flow1 = Flow(pod.getHost(0, 0).ip_addr(0), pod.getHost(0, 1).ip_addr(0), pod.getHost(0, 0).mac_addr(0),
      pod.getHost(1, 0).mac_addr(0), size = 1)
    val flow2 = Flow(pod.getHost(0, 1).ip_addr(0), pod.getHost(0, 0).ip_addr(0), pod.getHost(1, 0).mac_addr(0),
      pod.getHost(0, 0).mac_addr(0), size = 1)
    SimulationEngine.addEvent(new StartNewFlowEvent(flow1, pod.getHost(0, 0), 0))
    SimulationEngine.addEvent(new StartNewFlowEvent(flow2, pod.getHost(1, 0), 0))
    SimulationEngine.run()
  }
}


