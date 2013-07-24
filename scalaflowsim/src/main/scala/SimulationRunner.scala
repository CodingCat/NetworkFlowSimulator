package scalasim

import scalasim.application.ApplicationRunner
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
    val pod = new Pod(1)
  }
}


