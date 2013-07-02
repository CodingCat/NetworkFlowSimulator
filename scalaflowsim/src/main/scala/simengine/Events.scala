package scalasim.simengine

abstract  class Event {}

abstract class EventOfSingleEntity[EntityType] (entity : EntityType) extends Event {
   def process(entity: EntityType);
}

abstract class EventOfTwoEntities[ET1, ET2] (entity_a : ET1, entity_b : ET2) extends Event {
   def process(entity_a: ET1, entity_b : ET2);
}
