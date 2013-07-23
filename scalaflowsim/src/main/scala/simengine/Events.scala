package scalasim.simengine

import scalasim.simengine.utils.Logging

abstract class Event (protected var timestamp : Double) extends Logging {
  def setTimeStamp(t : Double) {
    timestamp = t
  }
  def getTimeStamp() = timestamp
  def process
}

abstract class EventOfSingleEntity[EntityType] (val entity : EntityType, ts : Double) extends Event(ts) {

}

abstract class EventOfTwoEntities[ET1, ET2] (val e1 : ET1, val e2 : ET2 , ts : Double) extends Event(ts) {
  /*override def equals(e : Any) : Boolean = {
    try {
      val econverted = e.asInstanceOf[EventOfTwoEntities[ET1, ET2]]
      sametype(e) && econverted.e1 == this.e1 && econverted.e2 == this.e2 && econverted.timestamp == this.timestamp
    }
    catch {
      case e : Exception => false
    }
  } */
}
