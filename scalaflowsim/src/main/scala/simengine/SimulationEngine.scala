package scalasim.simengine

import scala.collection.immutable.TreeMap


object SimulationEngine {

  implicit object KeyOrder extends Ordering[Double] {
    def compare(x : Double, y : Double) = x > y match {
      case true => 1
      case false => {
        x == y match {
          case true => 0
          case false => -1
        }
      }
    }
  }

  var currentTime : Double = 0.0

  var eventqueue = new TreeMap[Double, Event]()(KeyOrder)

  def run() {
    for (event <- eventqueue.values) {
      event.process
      currentTime = event.getTimeStamp
    }
  }

  def addEvent(e : Event) {
    eventqueue = eventqueue + (e.getTimeStamp -> e)
  }

  def cancelEvent(e : Event) {
    eventqueue = eventqueue - e.getTimeStamp
  }
}



