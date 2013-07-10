package scalasim.simengine

import scala.collection.mutable.{TreeSet, SynchronizedSet}
import scala.collection.mutable


object SimulationEngine {

  implicit object EventOrder extends Ordering[Event] {
    def compare(x : Event, y : Event) = x.getTimeStamp > y.getTimeStamp match {
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

  private var eventqueue = new TreeSet[Event]()(EventOrder)

  private var numPassedEvents = 0

  def run() {
    while (eventqueue.isEmpty == false) {
      val event = eventqueue.head
      if (event.getTimeStamp < currentTime) {
        throw new Exception("cannot execute an event happened before, event timestamp: " +
          event.getTimeStamp + ", currentTime:" + currentTime)
      }
      event.process
      numPassedEvents += 1
      currentTime = event.getTimeStamp
      eventqueue -= event
    }
  }

  def Events() = eventqueue

  def numFinishedEvents = numPassedEvents

  def addEvent(e : Event) = {
    eventqueue += e
  }

  def cancelEvent(e : Event) {
    if (eventqueue.contains(e)) {
      eventqueue -= e
    }
    else {
      throw new Exception("no such an event to cancel")
    }
  }

  def reset () {
    currentTime = 0.0
    numPassedEvents = 0
    eventqueue.clear()
  }
}



