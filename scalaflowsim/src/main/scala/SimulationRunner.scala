package scalasim

import scalasim.simengine.openflow.OpenFlowMsgDispatcher
import scalasim.application.ApplicationRunner
import scalasim.simengine.SimulationEngine
import scalasim.network.component.Pod


object SimulationRunner {

  def reset() {
    SimulationEngine.reset()
    ApplicationRunner.reset()
    XmlParser.reset()
  }

  def main(args:Array[String]) = {
    val pod = new Pod(1)
    OpenFlowMsgDispatcher.addPod(pod)
    OpenFlowMsgDispatcher.initChannel()
  }
}


