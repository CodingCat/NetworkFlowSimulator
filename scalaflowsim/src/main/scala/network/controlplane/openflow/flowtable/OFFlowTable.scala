package scalasim.network.controlplane.openflow.flowtable

import scalasim.network.controlplane.openflow.flowtable.counters._
import scala.collection.mutable.{ListBuffer, HashMap}
import org.openflow.protocol.{OFFlowMod, OFMessage, OFMatch}
import scala.collection.mutable
import org.openflow.protocol.action.{OFActionOutput, OFAction}
import scalasim.simengine.SimulationEngine
import scalasim.simengine.utils.Logging
import network.events.OFFlowTableEntryExpireEvent
import scala.collection.JavaConversions._
import java.util

class OFFlowTable extends Logging {
  class OFFlowTableEntryAttaches (table : OFFlowTable) {
    private [openflow] var matchfield : OFMatch = null
    private [openflow] val counter : OFFlowCount = new OFFlowCount
    private [openflow] val actions : ListBuffer[OFAction] = new ListBuffer[OFAction]
    private var lastAccessPoint : Int = SimulationEngine.currentTime.toInt
    private [openflow] var flowHardExpireMoment : Int = 0
    private [openflow] var flowIdleDuration : Int = 0
    private [openflow] var priority : Short = 0
    private [openflow] var expireEvent : OFFlowTableEntryExpireEvent = null

    def getLastAccessPoint = lastAccessPoint

    def refreshlastAccessPoint() {
      lastAccessPoint = SimulationEngine.currentTime.toInt
      determineEntryExpireMoment()
    }

    private def determineEntryExpireMoment () {
      var expireMoment = 0
      val idleDuration = flowIdleDuration
      val idleexpireMoment = lastAccessPoint + flowIdleDuration
      val hardexpireMoment = flowHardExpireMoment
      if (
        (hardexpireMoment != 0 && idleDuration == 0 && expireEvent == null) &&
          (hardexpireMoment != 0 && idleexpireMoment != 0 && hardexpireMoment < idleexpireMoment)) {
        expireMoment = hardexpireMoment
      }
      else {
        if (
          (hardexpireMoment == 0 && idleDuration != 0) &&
          (hardexpireMoment != 0 && idleexpireMoment != 0 && idleexpireMoment < hardexpireMoment)) {
          expireMoment = idleexpireMoment
        }
      }
      if (expireMoment != 0) {
        expireEvent = new OFFlowTableEntryExpireEvent(table, matchfield, expireMoment)
        SimulationEngine.addEvent(expireEvent)
      }
    }
  }

  private [openflow] val entries : HashMap[OFMatch, OFFlowTableEntryAttaches] =
    new HashMap[OFMatch, OFFlowTableEntryAttaches]
  private [openflow] val counters : OFTableCount = new OFTableCount

  def clear() {
    entries.clear()
  }

  def getFlowsByMatch(match_field : OFMatch) : List[OFFlowTableEntryAttaches] = {
    if (match_field.getWildcards == -1) {
      logDebug("return all flows: " + entries.values.toList.length)
      entries.values.toList
    }
    else {
      val entrieslist = new mutable.LinkedList[OFFlowTableEntryAttaches]{entries(match_field)}
      entrieslist.toList
    }
  }

  def getFlowsByMatchAndPort(match_field : OFMatch, port_num : Short) : List[OFFlowTableEntryAttaches] = {

    def containsOutputAction (p : OFFlowTableEntryAttaches) : OFActionOutput = {
      for (action <- p.actions) {
        if (action.isInstanceOf[OFActionOutput]) return action.asInstanceOf[OFActionOutput]
      }
      return null
    }

    def outputToCertainPort (outaction : OFActionOutput, port_num : Short) : Boolean = {
      if (outaction == null) return false
      if (port_num == -1) return true
      port_num == outaction.getPort
    }

    val filteredByMatch = getFlowsByMatch(match_field)
    logTrace("filteredByMatchLength: " + filteredByMatch.length)
    if (port_num == -1) {
      filteredByMatch
    }
    else {
      filteredByMatch.filter(p => outputToCertainPort(containsOutputAction(p), port_num)).toList
    }
  }

  def removeEntry (matchfield : OFMatch) {
    entries -= matchfield
  }

  def matchFlow(matchfield : OFMatch) {
    if (entries.contains(matchfield)) {
      entries(matchfield).refreshlastAccessPoint()
    }
  }

  def addFlowTableEntry (flow_mod : OFFlowMod) {
    //openflow 1.0 hasn't support pipeline processing yet
    //so add the entry to the first table by default
    if (flow_mod.getCommand != OFFlowMod.OFPFC_ADD)
      throw new Exception("the flow must be a OFPFC_ADD Flow_Mod when you add flowtable entry")
    val entryAttach = new OFFlowTableEntryAttaches(this)
    entryAttach.matchfield = flow_mod.getMatch
    entryAttach.priority = flow_mod.getPriority
    flow_mod.getActions.toList.foreach(f => entryAttach.actions += f)
    //schedule flow entry clean event
    entryAttach.flowHardExpireMoment = (SimulationEngine.currentTime + flow_mod.getHardTimeout).toInt
    entryAttach.flowIdleDuration = flow_mod.getIdleTimeout
    entryAttach.refreshlastAccessPoint
    entries += (entryAttach.matchfield -> entryAttach)
  }
}
