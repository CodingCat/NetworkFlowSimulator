package scalasim.network.controlplane.openflow.flowtable.counters


object OFCountersFactory {
  def getCounter(countertype : OFCounterType.Value) : OFCounter = {
    countertype match {
      case OFCounterType.OFPacketLookups => new OFPacketLookupCount
      case OFCounterType.OFPacketMatches => new OFPacketMatchesCount
      case OFCounterType.OFReferenceCount => new OFReferenceCount
    }
  }
}
