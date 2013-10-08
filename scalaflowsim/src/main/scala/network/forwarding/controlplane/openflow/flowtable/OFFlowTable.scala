package network.forwarding.controlplane.openflow.flowtable

import scala.collection.mutable.{ListBuffer, HashMap}
import org.openflow.protocol._
import org.openflow.protocol.action.{OFActionOutput, OFAction}
import network.events.OFFlowTableEntryExpireEvent
import scala.collection.JavaConversions._
import org.openflow.util.U32
import scala.collection.mutable
import simengine.utils.Logging
import network.forwarding.controlplane.openflow._
import simengine.SimulationEngine
import network.traffic.Flow
import org.openflow.protocol.statistics.{OFFlowStatisticsReply, OFFlowStatisticsRequest, OFStatisticsType}
import org.openflow.protocol.factory.BasicFactory
import utils.IPAddressConvertor

class OFFlowTable (tableid : Short, ofcontrolplane : OpenFlowControlPlane) extends Logging {

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

    def Counters = counter

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
        expireEvent = new OFFlowTableEntryExpireEvent(table,
          OFFlowTable.createMatchField(ofmatch, ofmatch.getWildcards),
          expireMoment)
        SimulationEngine.addEvent(expireEvent)
      }
    }
  }

  private [openflow] val entries : HashMap[OFMatchField, OFFlowTableEntryAttaches] =
    new HashMap[OFMatchField, OFFlowTableEntryAttaches] with
      mutable.SynchronizedMap[OFMatchField, OFFlowTableEntryAttaches]
  private [openflow] val tableCounter : OFTableCount = new OFTableCount

  private val messageFactory = new BasicFactory

  def TableCounter = tableCounter

  def clear() {
    entries.clear()
  }

  def matchFlow(flowmatch : OFMatch, topk : Int = 1) : List[OFFlowTableEntryAttaches] = {
    assert(topk > 0)
    val ret = new ListBuffer[OFFlowTableEntryAttaches]
    val matchfield = OFFlowTable.createMatchField(flowmatch, flowmatch.getWildcards)
    entries.foreach(entry => {
    //  logDebug(entry._1.toCompleteString() + "\t" + matchfield.toCompleteString())
      if (entry._1.matching(matchfield)) ret += entry._2
    })
    ret.toList.sortWith(_.priority > _.priority).slice(0, topk)
  }

  private def queryTable(matchrule : OFMatchField, topk : Int = 1) : List[OFFlowTableEntryAttaches] = {
    assert(topk > 0)
    val ret = new ListBuffer[OFFlowTableEntryAttaches]
    entries.foreach(entry => {if (matchrule.matching(entry._1)) ret += entry._2})
    ret.toList.sortWith(_.priority > _.priority).slice(0, topk)
  }

  def queryTableByMatch(ofmatch : OFMatch) : List[OFFlowTableEntryAttaches] = {
    if (ofmatch.getWildcards == -1) {
      logDebug("return all flows: " + entries.values.toList.length)
      entries.values.toList
    } else {
      queryTable(OFFlowTable.createMatchField(ofmatch, ofmatch.getWildcards))
    }
  }

  def queryTableByMatchAndOutport (match_field : OFMatch, outport_num : Short,
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

    val filteredByMatch = queryTableByMatch(match_field)
    logTrace("filteredByMatchLength: " + filteredByMatch.length)
    if (outport_num == -1) return filteredByMatch
    filteredByMatch.filter(p => filterByOutputPort(containsOutputAction(p), outport_num)).toList.
      sortWith(_.priority > _.priority).slice(0, topk)
  }

  def removeEntry (matchfield : OFMatchField) {
    entries -= matchfield
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
    entryAttach.refreshlastAccessPoint()
    entries += (OFFlowTable.createMatchField(entryAttach.ofmatch, entryAttach.ofmatch.getWildcards)
      -> entryAttach)
    entries
  }

  def queryByFlowStatRequest(offlowstatreq: OFFlowStatisticsRequest): List[OFFlowStatisticsReply] = {
    val replylist = new ListBuffer[OFFlowStatisticsReply]
    val qualifiedflows = queryTableByMatchAndOutport(offlowstatreq.getMatch,
      offlowstatreq.getOutPort)
    logTrace("qualified matchfield number: " + qualifiedflows.length)

    val localdataplane = ofcontrolplane.node.dataplane
    //get flow
    localdataplane.

    qualifiedflows.foreach(flowentry => {
      val offlowstatreply = messageFactory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.FLOW)
        .asInstanceOf[OFFlowStatisticsReply]
      var actionlistlength = 0

      flowentry.actions.foreach(action => actionlistlength += action.getLength)
      offlowstatreply.setMatch(offlowstatreq.getMatch)
      offlowstatreply.setTableId(offlowstatreq.getTableId)
      offlowstatreply.setDurationNanoseconds(flowentry.counter.durationNanoSeconds)
      offlowstatreply.setDurationSeconds(flowentry.counter.durationSeconds)
      offlowstatreply.setPriority(0)
      offlowstatreply.setIdleTimeout((flowentry.flowIdleDuration -
        (SimulationEngine.currentTime - flowentry.getLastAccessPoint)).toShort)
      offlowstatreply.setHardTimeout((flowentry.flowHardExpireMoment - SimulationEngine.currentTime).toShort)
      offlowstatreply.setCookie(0)
      offlowstatreply.setPacketCount(flowentry.counter.receivedpacket)
      offlowstatreply.setByteCount(flowentry.counter.receivedbytes)
      offlowstatreply.setActions(flowentry.actions)
      offlowstatreply.setLength((88 + actionlistlength).toShort)
      replylist += offlowstatreply
    })
    replylist.toList
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
    val matchfield = new OFMatchField()
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
   * generate the OFMatchField from a OFMatch
   * @param ofmatch
   * @param wcard
   * @return
   */
  def createMatchField(ofmatch : OFMatch, wcard : Int) : OFMatchField = {
    val matchfield = new OFMatchField
    matchfield.setWildcards(OFMatch.OFPFW_ALL & wcard)
    matchfield.setInputPort(ofmatch.getInputPort)
    matchfield.setDataLayerDestination(ofmatch.getDataLayerDestination)
    matchfield.setDataLayerSource(ofmatch.getDataLayerSource)
    matchfield.setDataLayerVirtualLan(ofmatch.getDataLayerVirtualLan)
    matchfield.setNetworkDestination(ofmatch.getNetworkDestination)
    matchfield.setNetworkSource(ofmatch.getNetworkSource)
    matchfield
  }
}