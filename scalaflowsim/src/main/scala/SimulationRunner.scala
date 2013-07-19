package scalasim

import _root_.simengine.openflow.OpenFlowMsgDispatcher
import scalasim.application.ApplicationRunner
import scalasim.simengine.SimulationEngine


object SimulationRunner {

  def reset() {
    SimulationEngine.reset()
    ApplicationRunner.reset()
    XmlParser.reset()
  }

  def main(args:Array[String]) = {
    OpenFlowMsgDispatcher.initChannel()
  }
}


