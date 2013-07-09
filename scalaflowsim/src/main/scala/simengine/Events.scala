package scalasim.simengine

abstract class Event (protected var timestamp : Double) {
  def setTimeStamp(t : Double) {
    timestamp = t
  }
  def getTimeStamp() = timestamp
  def process
}

abstract class EventOfSingleEntity[EntityType] (entity : EntityType, ts : Double) extends Event(ts) {

}

abstract class EventOfTwoEntities[ET1, ET2] (e1 : ET1, e2 : ET2 , ts : Double) extends Event(ts) {

}
