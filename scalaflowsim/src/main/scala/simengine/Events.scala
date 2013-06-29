package simengine

abstract  class Event {}

abstract class EventOfSingleEntity[EntityType] (entity : EntityType) extends Events {
  abstract def process(entity: EntityType);
}

abstract class EventOfTwoEntities[ET1, ET2] (entity_a : ET1, entity_b : ET2) extends Events {
  abstract def process(entity_a: ET1, entity_b : ET2);
}
