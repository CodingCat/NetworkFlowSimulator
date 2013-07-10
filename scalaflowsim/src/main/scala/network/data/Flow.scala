package network.data

import scala.collection.mutable.ListBuffer


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
  ) {

  var status : FlowStatus = NewStartFlow

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
    rate = tempRate
    status = RunningFlow
  }

  override def toString() : String = ("Flow-" + srcIP + "-" + dstIP)
}


object Flow {
  val finishedFlows = new ListBuffer[Flow]
  def apply(srcIP : String, dstIP : String, size : Double) : Flow = new Flow(srcIP, dstIP, size)
  def reset() {finishedFlows.clear()}
}