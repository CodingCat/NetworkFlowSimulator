package scalasim.network.traffic

import scalasim.network.component.Link
import scalasim.simengine.SimulationEngine
import scalasim.network.events.CompleteFlowEvent
import scalasim.simengine.utils.Logging
import scala.collection.mutable.ListBuffer
import scala.collection.mutable

/**
 *
 * @param srcIP
 * @param dstIP
 * @param srcMac
 * @param dstMac
 * @param vlanID
 * @param prioritycode
 * @param demand
 */
class Flow private (
  private [network] val srcIP : String,
  private [network] val dstIP : String,
  private [network] val srcMac : String,
  private [network] val dstMac : String,
  private [network] val vlanID : Short = 0,
  private [network] val prioritycode: Byte = 0,
  private [network] val srcPort : Short = 1,
  private [network] val dstPort : Short = 1,
  private [network] var demand : Double,//in MB
  private [network] var floodflag : Boolean = false
  //to indicate this flow may be routed to the non-destination host
  ) extends Logging {

  var status : FlowStatus = NewStartFlow
  private var hop : Int = 0
  private var bindedCompleteEvent : CompleteFlowEvent = null
  private var lastChangePoint  = 0.0


  /**
   * it's used as the solution in flow-level simulation
   * when the flow is finally routed to the destination,
   * the resource allocation module can track the passed
   * links through this
   */
  private val floodtrace_laststeptrack = new mutable.ListBuffer[(Link, Int)]   //(link, lastlinkindex)
  private val floodtrace = new mutable.ListBuffer[Link]

  def DstIP = dstIP
  def SrcIP = srcIP

  private var rate : Double = 0.0
  private var tempRate : Double = Double.MaxValue

  def bindEvent(ce : CompleteFlowEvent) {
    logDebug("bind completeEvent for " + this)
    bindedCompleteEvent = ce
  }

  def setRate(r : Double) {
    logDebug("old rate : " + rate + " new rate : " + r)
    demand -= rate * (SimulationEngine.currentTime - lastChangePoint)
    lastChangePoint = SimulationEngine.currentTime
    rate = r
    //TODO: may causing some duplicated rescheduling in successive links
    if ((status == RunningFlow || status == ChangingRateFlow) && demand > 0) rescheduleBindedEvent
  }

  def setTempRate(tr : Double) {tempRate = tr}

  def Demand = demand

  def getTempRate = tempRate

  def Rate = rate

  def addFloodTrace(newlink : Link, lastlink : Link) {
    var lastlinkindex = -1
    if (lastlink != null) lastlinkindex = floodtrace.indexOf(lastlink)
    floodtrace_laststeptrack += Tuple2(newlink, lastlinkindex)
    floodtrace += newlink
  }

  def getLastHop(curlink : Link) = {
    val ret = floodtrace(floodtrace_laststeptrack(floodtrace.indexOf(curlink))._2)
    logDebug("get the link " + curlink + " last step is " + ret)
    ret
  }

  //TODO: shall I move this method to the control plane or simulationEngine?
  private def rescheduleBindedEvent {
    //TODO: in test ControlPlaneSuite "ordering" test, bindedCompleteEvent can be true
    //TODO: that test case need to be polished, but not that urgent
    if (bindedCompleteEvent != null) {
      logTrace("reschedule " + toString + " completeEvent to " + (SimulationEngine.currentTime + demand / rate) +
        " current time:" + SimulationEngine.currentTime)
      SimulationEngine.reschedule(bindedCompleteEvent,
        SimulationEngine.currentTime + demand / rate)
    }
  }

  def run () {
    logTrace("determine " + this + " rate to " + tempRate)
    setRate(tempRate)
    status = RunningFlow
  }

  def close() {
    logTrace(this + " finishes at " + SimulationEngine.currentTime)
    lastChangePoint = SimulationEngine.currentTime
    status = CompletedFlow
    setRate(0)
  }

  def LastCheckPoint : Double = lastChangePoint

  def increaseHop() {
    hop += 1
  }
  def Hop() = hop

  override def toString() : String = ("Flow-" + srcIP + "-" + dstIP)
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
   * @param size
   * @param fflag
   * @return
   */
  def apply(srcIP : String, dstIP : String, srcMac : String, dstMac : String,
            sPort : Short = 1, dPort : Short = 1, vlanID : Short = 0,
            prioritycode : Byte = 0, size : Double, fflag : Boolean = false) : Flow = {
    new Flow(srcIP, dstIP, srcMac, dstMac, vlanID, prioritycode, srcPort = sPort, dstPort = dPort,
      demand = size, floodflag = fflag)
  }
}