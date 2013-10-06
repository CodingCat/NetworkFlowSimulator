package simengine

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import scala.concurrent.Lock
import simengine.utils.{XmlParser, Logging}


object SimulationEngine extends Logging {

  val queueReadingLock = new Lock
  var currentTime : Double = 0.0

  //TODO:any more performant implementation?
  private var eventqueue : ArrayBuffer[Event] = new ArrayBuffer[Event] with mutable.SynchronizedBuffer[Event]
  private var numPassedEvents = 0

  private val startTime = XmlParser.getInt("scalasim.simengine.starttime", 0)
  private val endTime = XmlParser.getInt("scalasim.simengine.endtime", 1000)
  val reporter: Report = null

  def run {
    //run insert the periodical Events
    PeriodicalEventManager.run(startTime, endTime)
    while (!eventqueue.isEmpty) {
      queueReadingLock.acquire()
      logDebug("acquire lock at SimulationEngine")
      val event = eventqueue.head
      if (event.getTimeStamp() > endTime) return
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
    if (reporter != null)
      reporter.report()
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



