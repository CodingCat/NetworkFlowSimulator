package simengine.openflow.flowtable

import simengine.openflow.flowtable.matchfield.OFMatchField
import simengine.openflow.counters.OFCounter
import scala.collection.mutable.{ListBuffer, HashMap, LinkedList}
import simengine.openflow.flowtable.instructions.OFInstruction


class OFFlowTable {
  class OFFlowTableEntry {
    private val counters : HashMap[String, OFCounter] = new HashMap[String, OFCounter]
    private val instructions : Array[OFInstruction] = new Array[OFInstruction](5)
  }

  private val entries : HashMap[OFMatchField, OFFlowTableEntry] = new HashMap[OFMatchField, OFFlowTableEntry]

  def addEntry(matchfield : OFMatchField, instructionSet : ListBuffer[OFInstruction]) {
    //TODO:to be implemented
  }

  def clear() {
    entries.clear()
  }
}
