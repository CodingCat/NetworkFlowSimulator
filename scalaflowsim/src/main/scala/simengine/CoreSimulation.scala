package simengine

import scala.collection.mutable.Queue

class SimulationEngine {
  val eventqueue = new Queue[Event]();

  def run() = {
    for (event <- eventqueue) {

    }
  }
}

object SimulationEngine {

}



