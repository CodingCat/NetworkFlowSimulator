package simengine

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import scala.concurrent.Lock
import simengine.utils.Logging


object SimulationEngine extends Logging {

  val queueReadingLock = new Lock
  var currentTime : Double = 0.0

  //TODO:any more performant implementation?
  private var eventqueue : ArrayBuffer[Event] = new ArrayBuffer[Event] with mutable.SynchronizedBuffer[Event]
  private var numPassedEvents = 0

  def run {
    while (!eventqueue.isEmpty) {
      queueReadingLock.acquire()
      logDebug("acquire lock at SimulationEngine")
      val event = eventqueue.head
      queueReadingLock.release()
      logDebug("release lock at SimulationEngine")
      if (event.getTimeStamp < currentTime) {
        throw new Exception("cannot execute an event happened before, event timestamp: " +
          event.getTimeStamp + ", currentTime:" + currentTime)
      }
      currentTime = event.getTimeStamp
      //every event should be atomic
      event.process
      numPassedEvents += 1
      eventqueue -= event
    }
  }

  def Events() = eventqueue

  def numFinishedEvents = numPassedEvents

  def addEvent(e : Event) {
    eventqueue += e
    eventqueue = eventqueue.sortWith(_.getTimeStamp < _.getTimeStamp)
  }

  def contains(e : Event) = eventqueue.contains(e)

  def cancelEvent(e : Event) {
    if (eventqueue.contains(e)) {
      eventqueue -= e
    }
    //remove the following statements because the event may be
    //canceled in advance when we use onOffAPp
    /*
    else {
      throw new Exception("no such an event to cancel")
    }  */
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



