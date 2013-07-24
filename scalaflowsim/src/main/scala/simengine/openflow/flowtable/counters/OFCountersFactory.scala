package simengine.openflow.counters

object OFCountersFactory {
  def getCounter(countertype : OFCounterType.Value) : OFCounter = {
    countertype match {
      case OFCounterType.OFPacketLookups => new OFPacketLookupCount("packet_lookup_counter")
      case OFCounterType.OFPacketMatches => new OFPacketMatchesCount("packet_match_counter")
      case OFCounterType.OFReferenceCount => new OFReferenceCount("packet_reference_counter")
    }
  }
}
