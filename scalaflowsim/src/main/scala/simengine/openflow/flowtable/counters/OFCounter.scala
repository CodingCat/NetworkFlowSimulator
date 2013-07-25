package scalasim.simengine.openflow.counters

class OFCounter (private [openflow] val name : String) {
  protected [openflow] var value : Long = 0
}

//Per table counters
class OFReferenceCount extends OFCounter("referencecount")

class OFPacketLookupCount extends OFCounter("packetlookupcount")

class OFPacketMatchesCount extends OFCounter("packetmatchedcount")

//in openflow specification there is not such a counter
class OFFlowBytesCount extends OFCounter("flowbytescount")

