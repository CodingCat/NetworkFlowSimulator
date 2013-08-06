package scalasim.network.controlplane.openflow.flowtable

import scalasim.network.controlplane.openflow.flowtable.counters._
import scala.collection.mutable.{ListBuffer, HashMap}
import org.openflow.protocol.{OFFlowMod, OFMatch}
import org.openflow.protocol.action.{OFActionOutput, OFAction}
import scalasim.network.controlplane.routing.OpenFlowRouting
import scalasim.network.traffic.Flow
import scalasim.simengine.SimulationEngine
import scalasim.simengine.utils.Logging
import network.events.OFFlowTableEntryExpireEvent
import scala.collection.JavaConversions._
import simengine.utils.IPAddressConvertor
import org.openflow.util.U32
import scala.collection.mutable
import network.controlplane.openflow.flowtable.OFMatchField

class OFFlowTable (ofroutingmodule : OpenFlowRouting) extends Logging {
  class OFFlowTableEntryAttaches (table : OFFlowTable) {
    private [openflow] var ofmatch : OFMatch = null
    private [openflow] val counter : OFFlowCount = new OFFlowCount
    private [controlplane] val actions : ListBuffer[OFAction] = new ListBuffer[OFAction]
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
        (hardexpireMoment != 0 && idleDuration == 0 && expireEvent == null) ||
          (hardexpireMoment != 0 && idleexpireMoment != 0 && hardexpireMoment < idleexpireMoment)) {
        expireMoment = hardexpireMoment
      }
      else {
        if (
          (hardexpireMoment == 0 && idleDuration != 0) ||
          (hardexpireMoment != 0 && idleexpireMoment != 0 && idleexpireMoment < hardexpireMoment)) {
          expireMoment = idleexpireMoment
        }
      }
      if (expireMoment != 0) {
        expireEvent = new OFFlowTableEntryExpireEvent(ofroutingmodule,
          OFFlowTable.createMatchField(ofmatch, ofmatch.getWildcards),
          expireMoment)
        SimulationEngine.addEvent(expireEvent)
      }
    }
  }

  private [openflow] val entries : HashMap[OFMatchField, OFFlowTableEntryAttaches] =
    new HashMap[OFMatchField, OFFlowTableEntryAttaches] with
      mutable.SynchronizedMap[OFMatchField, OFFlowTableEntryAttaches]
  private [openflow] val counters : OFTableCount = new OFTableCount

  def clear() {
    entries.clear()
  }

  private def queryTable(matchfield : OFMatch, topk : Int = 1) : List[OFFlowTableEntryAttaches] = {
    assert(topk > 0)
    val ret = new ListBuffer[OFFlowTableEntryAttaches]
    entries.foreach(entry => {if (entry._1.matching(matchfield)) ret += entry._2})
    ret.toList.sortWith(_.priority > _.priority).slice(0, topk)
  }

  def getFlowsByMatch(ofmatch : OFMatch) : List[OFFlowTableEntryAttaches] = {
    if (ofmatch.getWildcards == -1) {
      logDebug("return all flows: " + entries.values.toList.length)
      entries.values.toList
    } else {
      queryTable(ofmatch)
    }
  }

  def getFlowsByMatchAndOutPort(match_field : OFMatch, outport_num : Short,
                                 topk : Int = 1) : List[OFFlowTableEntryAttaches] = {

    def containsOutputAction (p : OFFlowTableEntryAttaches) : OFActionOutput = {
      for (action <- p.actions) {
        if (action.isInstanceOf[OFActionOutput]) return action.asInstanceOf[OFActionOutput]
      }
      null
    }

    def filterByOutputPort (outaction : OFActionOutput, port_num : Short) : Boolean = {
      if (outaction == null) return false
      if (port_num == -1) return true
      port_num == outaction.getPort
    }

    val filteredByMatch = getFlowsByMatch(match_field)
    logTrace("filteredByMatchLength: " + filteredByMatch.length)
    if (outport_num == -1) return filteredByMatch
    filteredByMatch.filter(p => filterByOutputPort(containsOutputAction(p), outport_num)).toList.
      sortWith(_.priority > _.priority).slice(0, topk)
  }

  def removeEntry (matchfield : OFMatchField) {
    entries -= matchfield
  }

  def matchFlow(matchfield : OFMatchField) {
    if (entries.contains(matchfield)) {
      entries(matchfield).refreshlastAccessPoint()
    }
  }

  /**
   *
   * @param flow_mod
   * @return the updated entries
   */
  def addFlowTableEntry (flow_mod : OFFlowMod) = {
    assert(flow_mod.getCommand == OFFlowMod.OFPFC_ADD)
    val entryAttach = new OFFlowTableEntryAttaches(this)
    entryAttach.ofmatch = flow_mod.getMatch
    entryAttach.priority = flow_mod.getPriority
    flow_mod.getActions.toList.foreach(f => entryAttach.actions += f)
    //schedule matchfield entry clean event
    entryAttach.flowHardExpireMoment = (SimulationEngine.currentTime + flow_mod.getHardTimeout).toInt
    entryAttach.flowIdleDuration = flow_mod.getIdleTimeout
    entryAttach.refreshlastAccessPoint
    entries += (OFFlowTable.createMatchField(entryAttach.ofmatch, entryAttach.ofmatch.getWildcards)
      -> entryAttach)
    entries
  }
}

object OFFlowTable {

  /**
   *
   * @param flow
   * @param wcard
   * @return
   */
  def createMatchField(flow : Flow, wcard : Int) : OFMatchField = {
    val matchfield = new OFMatchField
    matchfield.setWildcards(wcard)
    matchfield.setInputPort(flow.inport)
    matchfield.setDataLayerDestination(flow.dstMac)
    matchfield.setDataLayerSource(flow.srcMac)
    matchfield.setDataLayerVirtualLan(flow.vlanID)
    matchfield.setNetworkDestination(U32.t(IPAddressConvertor.DecimalStringToInt(flow.dstIP)))
    matchfield.setNetworkSource(U32.t(IPAddressConvertor.DecimalStringToInt(flow.srcIP)))
    matchfield
  }

  /**
   *
   * @param ofmatch
   * @param wcard
   * @return
   */
  def createMatchField(ofmatch : OFMatch, wcard : Int) : OFMatchField = {
    val matchfield = new OFMatchField
    val wildcard = OFMatch.OFPFW_ALL & wcard
    matchfield.setWildcards(wildcard)
    matchfield.setInputPort(ofmatch.getInputPort)
    matchfield.setDataLayerDestination(ofmatch.getDataLayerDestination)
    matchfield.setDataLayerSource(ofmatch.getDataLayerSource)
    matchfield.setDataLayerVirtualLan(ofmatch.getDataLayerVirtualLan)
    matchfield.setNetworkDestination(ofmatch.getNetworkDestination)
    matchfield.setNetworkSource(ofmatch.getNetworkSource)
    matchfield
  }
}