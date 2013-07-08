package network.data

/**
 *
 * @param srcIP
 * @param dstIP
 * @param demand
 */
class Flow (
  private val srcIP : String,
  private val dstIP : String,
  private val demand : Double//in bytes
  ) {

  def DstIP = dstIP
  def SrcIP = srcIP

  private var rate : Double = 0.0

  def setRate(r : Double) = rate = r

  def Rate = rate
}


object Flow {
  def apply(srcIP : String, dstIP : String, size : Double) : Flow = new Flow(srcIP, dstIP, size)
}