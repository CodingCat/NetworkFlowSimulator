package scalasim

import network.data.Flow
import scalasim.application.ApplicationRunner
import scalasim.simengine.SimulationEngine


object SimulationRunner {

  def reset() {
    Flow.reset()
    SimulationEngine.reset()
    ApplicationRunner.reset()
    XmlParser.reset()
  }

  def main(args:Array[String]) = {
   /* if (args.length != 1) {
      System.out.println("Usage: program confPath")
      System.exit(1)
    }
    XmlParser.loadConf(args(0))
    System.out.println(XmlParser.getString("lendingclub.dataset.trainingsetpath", "steak"))*/

  }
}


