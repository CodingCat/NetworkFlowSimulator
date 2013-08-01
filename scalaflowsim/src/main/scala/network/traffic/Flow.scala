package scalasim.network.traffic

import scalasim.simengine.SimulationEngine
import scalasim.network.events.CompleteFlowEvent
import scalasim.simengine.utils.Logging

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
  private [network] var demand : Double//in MB
  ) extends Logging {

  var status : FlowStatus = NewStartFlow
  private var hop : Int = 0
  private var bindedCompleteEvent : CompleteFlowEvent = null
  private var lastChangePoint  = 0.0

  def DstIP = dstIP
  def SrcIP = srcIP

  private var rate : Double = 0.0
  private var tempRate : Double = Double.MaxValue

  def bindEvent(ce : CompleteFlowEvent) {
    logDebug("bind completeEvent for " + this)
    bindedCompleteEvent = ce
  }

  def setRate(r : Double) {
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
  def apply(srcIP : String, dstIP : String, srcMac : String, dstMac : String,
            vlanID : Short = 0, prioritycode : Byte = 0, size : Double) : Flow = {
    new Flow(srcIP, dstIP, srcMac, dstMac, vlanID, prioritycode, demand = size)
  }
}