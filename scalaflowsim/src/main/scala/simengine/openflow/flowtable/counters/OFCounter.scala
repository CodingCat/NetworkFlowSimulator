package simengine.openflow.counters

class OFCounter (private val name : String)

//Per table counters
class OFReferenceCount(name : String) extends OFCounter(name) {
  private [openflow] var value : Int = 0
}

class OFPacketLookupCount(name : String) extends OFCounter(name) {
  private [openflow] var value : Long = 0
}

class OFPacketMatchesCount(name : String) extends OFCounter(name) {
  private [openflow] var value : Long = 0
}


