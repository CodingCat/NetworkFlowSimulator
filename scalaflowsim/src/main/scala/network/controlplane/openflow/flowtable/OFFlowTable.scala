package scalasim.network.controlplane.openflow.flowtable

import scalasim.network.controlplane.openflow.flowtable.matchfield.OFMatchField
import scalasim.network.controlplane.openflow.flowtable.counters._
import scala.collection.mutable.{ListBuffer, HashMap}
import scalasim.network.controlplane.openflow.flowtable.instructions.OFInstruction


class OFFlowTable {
  class OFFlowTableEntry {
    private val counters : HashMap[String, OFCounter] = new HashMap[String, OFCounter]
    private val instructions : Array[OFInstruction] = new Array[OFInstruction](5)
  }

  private[openflow] val entries : HashMap[OFMatchField, OFFlowTableEntry] = new HashMap[OFMatchField, OFFlowTableEntry]
  private[openflow] val tablecounters : HashMap[String, OFCounter] = new HashMap[String, OFCounter]

  def init() {
    tablecounters += ("referencecount" -> new OFReferenceCount)
    tablecounters += ("packetlookupcount" -> new OFPacketLookupCount)
    tablecounters += ("packetmatchescount" -> new OFPacketMatchesCount)
    tablecounters += ("flowbytescount" -> new OFPacketMatchesCount)
  }

  def addEntry(matchfield : OFMatchField, instructionSet : ListBuffer[OFInstruction]) {
    //TODO:to be implemented
  }

  def clear() {
    entries.clear()
  }

  init()
}
