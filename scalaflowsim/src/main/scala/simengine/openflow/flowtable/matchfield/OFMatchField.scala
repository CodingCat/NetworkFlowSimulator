package simengine.openflow.flowtable.matchfield

import org.openflow.protocol.OFMatch

class OFMatchField {
  //TODO: temporary implementation, what is the information in metadata?
  private val metadata : Array[Byte] = new Array[Byte](8)
  private val basicmatch : OFMatch = new OFMatch
}
