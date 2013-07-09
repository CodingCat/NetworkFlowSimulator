package scalasim.simengine

import scala.collection.immutable.TreeMap
import scala.collection.mutable.HashSet


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

  var eventqueue = new TreeMap[Double,  HashSet[Event]]()(KeyOrder)//timestamp -> event

  def run() {
    for (eventsAtMoment <- eventqueue.values; event <- eventsAtMoment) {
      event.process
      currentTime = event.getTimeStamp
    }
  }

  def addEvent(e : Event) {
    if (eventqueue.contains(e.getTimeStamp) == false)
      eventqueue += e.getTimeStamp -> new HashSet[Event]
    eventqueue(e.getTimeStamp) += e
  }

  def cancelEvent(e : Event) {
    eventqueue = eventqueue - e.getTimeStamp
  }

  def clear() {
    eventqueue = eventqueue.empty
  }
}



