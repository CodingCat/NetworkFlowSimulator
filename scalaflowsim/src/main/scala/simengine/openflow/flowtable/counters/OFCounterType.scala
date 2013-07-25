package scalasim.simengine.openflow.counters

object OFCounterType extends Enumeration {
  type OFCounterType = Value
  val OFReferenceCount, OFPacketLookups, OFPacketMatches = Value
}
