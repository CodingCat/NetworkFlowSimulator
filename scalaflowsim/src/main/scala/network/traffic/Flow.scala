package network.traffic

import network.device.{HostType, Node, Link}
import scala.collection.mutable
import scalasim.XmlParser
import simengine.utils.Logging
import network.events.CompleteFlowEvent
import simengine.SimulationEngine
import scala.collection.mutable.ListBuffer
import network.forwarding.controlplane.openflow.OpenFlowControlPlane
import network.forwarding.controlplane.openflow.flowtable.OFFlowTable
import org.openflow.protocol.OFMatch

/**
 *
 * @param srcIP
 * @param dstIP
 * @param srcMac
 * @param dstMac
 * @param vlanID
 * @param prioritycode
 * @param remainingAppData
 */
class Flow private (
  private [network] val srcIP : String,
  private [network] val dstIP : String,
  private [network] val srcMac : String,
  private [network] val dstMac : String,
  private [network] val vlanID : Short = 0,
  private [network] val prioritycode: Byte = 0,
  private [network] val srcPort : Short = 0,
  private [network] val dstPort : Short = 0,//set to 0 to wildcarding src/dst ports
  private var remainingAppData : Double,//in MB
  private [network] var floodflag : Boolean = false
  //to indicate this flow may be routed to the non-destination host
  ) extends Logging {

  var status : FlowStatus = NewStartFlow
  private var hop : Int = 0
  private var bindedCompleteEvent : CompleteFlowEvent = null
  private var lastChangePoint  = 0.0

  //this value is dynamic,
  //mainly used by the openflow protocol to match flowtable
  private [network] var inport : Short = 0

  /**
   * track the flow's hops,
   * used to allocate resource in reverse order
   */
  private val trace_laststeptrack = new mutable.ListBuffer[(Link, Int)]   //(link, lastlinkindex)
  private val trace = new mutable.ListBuffer[Link]

  def DstIP = dstIP
  def SrcIP = srcIP

  private var rate : Double = 0.0
  private var tempRate : Double = Double.MaxValue

  def bindEvent(ce : CompleteFlowEvent) {
    logDebug("bind completeEvent for " + this)
    bindedCompleteEvent = ce
  }

  private def updateCounters(additionalPacket : Long, additionalBytes : Long,
                                      additionalDuration : Int) {
    def updateCounter(node : Node) {
      if (node.nodetype != HostType) {
        val oftables = node.controlplane.asInstanceOf[OpenFlowControlPlane].FlowTables
        oftables.foreach(table => {
          val entries = table.matchFlow(OFFlowTable.createMatchField(
            this, wcard = (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK)))
          entries.foreach(entry => {
            entry.Counters.increaseReceivedBytes(additionalBytes)
            entry.Counters.increaseReceivedPacket(additionalPacket)
            entry.Counters.increaseDurationSeconds(additionalDuration)
            entry.Counters.increaseDurationNanoSeconds(additionalDuration * 1000000000)
          })
        })
      }
    }
    if (XmlParser.getString("scalasim.simengine.model", "tcp") == "openflow") {
      //TODO:to be finished
      val checkedNode = new ListBuffer[Node]
      trace.foreach(link => {
        if (!checkedNode.contains(link.end_from)) {
          updateCounter(link.end_from)
          checkedNode += link.end_from
        }
        if (!checkedNode.contains(link.end_to)) {
          updateCounter(link.end_to)
          checkedNode += link.end_to
        }
      })
    }
  }

  def changeRate(newRateValue : Double) {
    logDebug("old rate : " + rate + " new rate : " + newRateValue + ", lastChangePoint = " + lastChangePoint)
    remainingAppData -= rate * (SimulationEngine.currentTime - lastChangePoint)
    updateCounters(0,
      (rate * (SimulationEngine.currentTime - lastChangePoint)).asInstanceOf[Long],
      (SimulationEngine.currentTime - lastChangePoint).asInstanceOf[Int])
    logTrace("change " + this + "'s lastChangePoint to " + SimulationEngine.currentTime)
    lastChangePoint = SimulationEngine.currentTime
    rate = newRateValue
    if (rate == 0) cancelBindedEvent()
    else {
      //TODO: may causing some duplicated rescheduling in successive links
      if ((status == RunningFlow || status == ChangingRateFlow) && remainingAppData > 0)
        rescheduleBindedEvent()
    }
  }

  def setTempRate(tr : Double) {tempRate = tr}

  def AppDataSize = remainingAppData

  def getTempRate = tempRate

  def Rate = rate

  /**
   * add the link to the flow's trace
   * @param newlink, the link to be added
   * @param lastlink, we need to know this parameter to track the last link of newlink
   */
  def addTrace(newlink : Link, lastlink : Link) {
    var lastlinkindex = -1
    if (lastlink != null) lastlinkindex = trace.indexOf(lastlink)
    logDebug("add trace, currentlink:" + newlink + ", lastlink:" + lastlink +
      ", flow:" + this.toString)
    trace_laststeptrack += Tuple2(newlink, lastlinkindex)
    trace += newlink
  }

  def getLastHop(curlink : Link) : Link = {
    logDebug("get the link " + curlink + "'s last step, flow:" + this.toString)
    val laststepindex = trace_laststeptrack(trace.indexOf(curlink))._2
    if (laststepindex < 0) return null
    val ret = trace(laststepindex)
    ret
  }

  private def cancelBindedEvent() {
    if (bindedCompleteEvent != null) {
      logTrace("cancel " + toString + " completeEvent " +
        " current time:" + SimulationEngine.currentTime)
      SimulationEngine.cancelEvent(bindedCompleteEvent)
    }
  }

  //TODO: shall I move this method to the control plane or simulationEngine?
  private def rescheduleBindedEvent () {
    //TODO: in test ControlPlaneSuite "ordering" test, bindedCompleteEvent can be true
    //TODO: that test case need to be polished, but not that urgent
    if (bindedCompleteEvent != null) {
      logTrace("reschedule " + toString + " completeEvent to " + (SimulationEngine.currentTime + remainingAppData / rate) +
        " current time:" + SimulationEngine.currentTime + " rate :" + rate + " demand:" + remainingAppData)
      if (remainingAppData > 100) {
        logError("demand > 100")
      }
      SimulationEngine.reschedule(bindedCompleteEvent,
        SimulationEngine.currentTime + remainingAppData / rate)
    }
  }

  def run () {
    logTrace("determine " + this + " rate to " + tempRate)
    changeRate(tempRate)
    status = RunningFlow
  }

  def close() {
    logTrace(this + " finishes at " + SimulationEngine.currentTime)
    lastChangePoint = SimulationEngine.currentTime
    status = CompletedFlow
    changeRate(0)
  }

  def LastCheckPoint : Double = lastChangePoint

  def increaseHop() {
    hop += 1
  }
  def Hop() = hop

  override def toString() : String = ("Flow-" + srcIP + "-" + dstIP)

  def getEgressLink = trace(trace.size - 1)

  def getIngressLink = trace(0)
}

object Flow {
  /**
   *
   * @param srcIP
   * @param dstIP
   * @param srcMac
   * @param dstMac
   * @param sPort
   * @param dPort
   * @param vlanID
   * @param prioritycode
   * @param demand
   * @param fflag
   * @return
   */
  def apply(srcIP : String, dstIP : String, srcMac : String, dstMac : String,
            sPort : Short = 1, dPort : Short = 1, vlanID : Short = 0,
            prioritycode : Byte = 0, demand : Double, fflag : Boolean = false) : Flow = {
    new Flow(srcIP, dstIP, srcMac, dstMac, vlanID, prioritycode, srcPort = sPort, dstPort = dPort,
      remainingAppData = demand, floodflag = fflag)
  }
}