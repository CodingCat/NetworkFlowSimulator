package scalasim.network.controlplane.openflow.flowtable

import scalasim.network.controlplane.openflow.flowtable.counters._
import scala.collection.mutable.HashMap
import org.openflow.protocol.OFMatch
import scala.collection.mutable
import org.openflow.protocol.action.{OFActionOutput, OFAction}
import scalasim.simengine.utils.Logging


class OFFlowTable (private [openflow] val flowExpireDuration : Int ,
                    private [openflow] val flowIdleDuration : Int) extends Logging {
  class OFFlowTableEntryAttaches (private [openflow] val expireMoments : Int) {
    private [openflow] val counter : OFFlowCount = new OFFlowCount
    private [openflow] val actions : mutable.LinkedList[OFAction] = new mutable.LinkedList[OFAction]
    private [openflow] var lastAccessPoint : Int = 0
  }

  /*def testcode {
    val match_field_1 = new OFMatch
    val match_field_2 = new OFMatch
    match_field_2.setDataLayerDestination("00:00:00:00:00:01")
    val entry_1 = new OFFlowTableEntryAttaches(100)
    val entry_2 = new OFFlowTableEntryAttaches(100)
    entries += (match_field_1 -> entry_1)
    entries += (match_field_2 -> entry_2)
    logDebug("entries length: " + entries.size)
  } */

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
      for (action <- p.actions) if (action.isInstanceOf[OFActionOutput]) return action.asInstanceOf[OFActionOutput]
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

 // testcode
}
