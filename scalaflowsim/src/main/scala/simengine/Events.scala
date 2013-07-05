package scalasim.simengine

abstract class Event (protected val timestamp : Double) {
  def getTimeStamp() = timestamp
}

abstract class EventOfSingleEntity[EntityType] (ts : Double) extends Event(ts) {
   def process(entity: EntityType)
}

abstract class EventOfTwoEntities[ET1, ET2] (ts : Double) extends Event(ts) {
   def process(entity_a: ET1, entity_b : ET2)
}
