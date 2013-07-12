package network.data

import scala.collection.mutable.ListBuffer
import simengine.utils.Logging


/**
 *
 * @param srcIP
 * @param dstIP
 * @param demand
 */
class Flow (
  private val srcIP : String,
  private val dstIP : String,
  private val demand : Double//in MB
  ) extends Logging {

  var status : FlowStatus = NewStartFlow
  private var hop : Int = 0

  def DstIP = dstIP
  def SrcIP = srcIP

  private var rate : Double = 0.0
  private var tempRate : Double = Double.MaxValue

  def changeRate (model : Char, r : Double) = model match {
    case '+' => rate += r
    case '-' => rate -= r
  }

  def changeTempRate(model : Char, tr : Double) = model match {
    case '+' => tempRate += tr
    case '-' => tempRate -= tr
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