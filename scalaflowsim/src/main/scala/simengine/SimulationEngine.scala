package scalasim.simengine

import scala.collection.mutable.ListBuffer
import scalasim.simengine.utils.Logging


object SimulationEngine extends Logging {

  var currentTime : Double = 0.0

  //TODO:any more performant implementation?
  private var eventqueue = new ListBuffer[Event]()

  private var numPassedEvents = 0

  def run() {
    while (!eventqueue.isEmpty) {
      val event = eventqueue.head
      if (event.getTimeStamp < currentTime) {
        throw new Exception("cannot execute an event happened before, event timestamp: " +
          event.getTimeStamp + ", currentTime:" + currentTime)
      }
      currentTime = event.getTimeStamp
      event.process
      numPassedEvents += 1
      eventqueue -= event
      logTrace("dequeue " + event.toString)
    }
  }

  def Events() = eventqueue

  def numFinishedEvents = numPassedEvents

  def addEvent(e : Event) {
    eventqueue += e
    eventqueue = eventqueue.sortWith(_.getTimeStamp < _.getTimeStamp)
  }

  def contains(e : Event) = eventqueue.contains(e)

  private def cancelEvent(e : Event) {
    if (eventqueue.contains(e)) {
      eventqueue -= e
    }
    else {
      throw new Exception("no such an event to cancel")
    }
  }

  def reschedule(e : Event, time : Double) {
    if (time < currentTime) throw new Exception("cannot reschedule a event to the before")
    cancelEvent(e)
    e.setTimeStamp(time)
    addEvent(e)
  }

  def reset () {
    currentTime = 0.0
    numPassedEvents = 0
    eventqueue.clear()
  }
}



