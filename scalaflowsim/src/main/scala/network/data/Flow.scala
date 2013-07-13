package network.data

import scala.collection.mutable.ListBuffer
import scalasim.simengine.SimulationEngine
import simengine.utils.Logging
import network.events.CompleteFlowEvent


/**
 *
 * @param srcIP
 * @param dstIP
 * @param demand
 */
class Flow (
  private val srcIP : String,
  private val dstIP : String,
  private var demand : Double//in MB
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
    bindedCompleteEvent = ce
  }

  def changeRate (model : Char, r : Double) {
    demand -= rate * (SimulationEngine.currentTime - lastChangePoint)
    lastChangePoint = SimulationEngine.currentTime
    if (model == '+') rate += r
    if (model == '-') rate -= r
    if (status == RunningFlow) rescheduleBindedEvent
  }

  def changeTempRate(model : Char, tr : Double) = model match {
    case '+' => tempRate += tr
    case '-' => tempRate -= tr
  }

  //TODO: shall I move this method to the control plane or simulationEngine?
  private def rescheduleBindedEvent {
    if (bindedCompleteEvent == null) {
      throw new Exception("bindedCompleteEvent is null")
    }
    SimulationEngine.reschedule(bindedCompleteEvent,
      SimulationEngine.currentTime + demand / rate)
  }

  def setTempRate(tr : Double) = {tempRate = tr}

  def Demand = demand

  def getTempRate = tempRate

  def Rate = rate

  def sync() {
    logDebug("determine " + this + " rate to " + tempRate)
    rate = tempRate
    status = RunningFlow
  }

  def increaseHop() {
    hop += 1
  }
  def Hop() = hop

  override def toString() : String = ("Flow-" + srcIP + "-" + dstIP)
}


object Flow {
  val finishedFlows = new ListBuffer[Flow]
  def apply(srcIP : String, dstIP : String, size : Double) : Flow = new Flow(srcIP, dstIP, size)
  def reset() {finishedFlows.clear()}
}