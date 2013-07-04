package network.data


class Flow (
  private val srcIP : String,
  private val dstIP : String,
  private val size : Double//in bytes
  ) {

  def DstIP = dstIP
  def SrcIP = srcIP

}
