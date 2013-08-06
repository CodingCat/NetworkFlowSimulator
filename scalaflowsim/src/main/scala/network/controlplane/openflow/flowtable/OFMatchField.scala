package network.controlplane.openflow.flowtable

import org.openflow.protocol.OFMatch
import java.util
import java.util.Arrays

class OFMatchField extends OFMatch {

  override def hashCode = {
    val prime: Int = 131
    var result: Int = 1
    if ((wildcards & OFMatch.OFPFW_DL_DST) == 0)
      result = prime * result + Arrays.hashCode(dataLayerDestination)
    if ((wildcards & OFMatch.OFPFW_DL_SRC) == 0)
      result = prime * result + Arrays.hashCode(dataLayerSource)
    if ((wildcards & OFMatch.OFPFW_DL_TYPE) == 0)
      result = prime * result + dataLayerType
    if ((wildcards & OFMatch.OFPFW_DL_VLAN) == 0)
      result = prime * result + dataLayerVirtualLan
    if ((wildcards & OFMatch.OFPFW_DL_VLAN_PCP) == 0)
      result = prime * result + dataLayerVirtualLanPriorityCodePoint
    if ((wildcards & OFMatch.OFPFW_IN_PORT) == 0)
      result = prime * result + inputPort
    result = prime * result +
      cidrToString(networkDestination, getNetworkDestinationMaskLen).hashCode
    result = prime * result +
      cidrToString(networkSource, getNetworkSourceMaskLen).hashCode
    if ((wildcards & OFMatch.OFPFW_NW_PROTO) == 0)
      result = prime * result + networkProtocol
    if ((wildcards & OFMatch.OFPFW_NW_TOS) == 0)
      result = prime * result + networkTypeOfService
    if ((wildcards & OFMatch.OFPFW_TP_DST) == 0)
      result = prime * result + transportDestination
    if ((wildcards & OFMatch.OFPFW_TP_SRC) == 0)
      result = prime * result + transportSource
    result
  }

  def matching(toCompare : OFMatch) : Boolean = {
    if ((wildcards & OFMatch.OFPFW_IN_PORT) == 0 &&
      this.inputPort != toCompare.getInputPort)
      return false
    if ((wildcards & OFMatch.OFPFW_DL_DST) == 0 &&
      !util.Arrays.equals(this.dataLayerDestination, toCompare.getDataLayerDestination))
      return false
    if ((wildcards & OFMatch.OFPFW_DL_SRC) == 0 &&
      !util.Arrays.equals(this.dataLayerSource, toCompare.getDataLayerSource))
      return false
    if ((wildcards & OFMatch.OFPFW_DL_TYPE) == 0
      && this.dataLayerType != toCompare.getDataLayerType)
      return false
    if ((wildcards & OFMatch.OFPFW_DL_VLAN) == 0 &&
      this.dataLayerVirtualLan != toCompare.getDataLayerVirtualLan)
      return false
    if ((wildcards & OFMatch.OFPFW_DL_VLAN_PCP) == 0 &&
      this.dataLayerVirtualLanPriorityCodePoint != toCompare.getDataLayerVirtualLanPriorityCodePoint)
      return false
    if ((wildcards & OFMatch.OFPFW_NW_PROTO) == 0 &&
      this.networkProtocol != toCompare.getNetworkProtocol)
      return false
    if ((wildcards & OFMatch.OFPFW_NW_TOS) == 0 &&
      this.networkTypeOfService != toCompare.getNetworkTypeOfService)
      return false
    //compare network layer src/dst
    val dstmasklen = getNetworkDestinationMaskLen
    val srcmasklen = getNetworkSourceMaskLen
    println("dstmasklen:" + dstmasklen + ", srcmasklen:" + srcmasklen)
    if (dstmasklen >= 32 && networkDestination != toCompare.getNetworkDestination)
      return false
    if (srcmasklen >= 32 && networkSource != toCompare.getNetworkSource)
      return false
    val dstmask = ~((1 << (32 - dstmasklen)) - 1)
    val srcmask = ~((1 << (32 - srcmasklen)) - 1)
    if (dstmasklen < 32 &&
      (networkDestination & dstmask) != (toCompare.getNetworkDestination & dstmask))
      return false
    if (srcmasklen < 32 &&
      (networkSource & srcmask) != (toCompare.getNetworkSource & srcmask))
      return false
    //layer - 4
    if ((wildcards & OFMatch.OFPFW_TP_DST) == 0 &&
      this.transportDestination != toCompare.getTransportDestination)
      return false
    if ((wildcards & OFMatch.OFPFW_TP_SRC) == 0 &&
      this.transportSource != toCompare.getTransportSource)
      return false
    true
  }

  private def cidrToString(ip : Int, prefix : Int) : String = {
    var str : String = null
    if (prefix >= 32) {
      str = OFMatch.ipToString(ip)
    } else {
      // use the negation of mask to fake endian magic
      val mask = ~((1 << (32 - prefix)) - 1)
      str = OFMatch.ipToString(ip & mask) + "/" + prefix
    }

    str
  }

  override def getNetworkDestinationMaskLen() : Int = {
    return Math
      .max(32 - ((wildcards & OFMatch.OFPFW_NW_DST_MASK) >> OFMatch.OFPFW_NW_DST_SHIFT),
      0)
  }

  override def getNetworkSourceMaskLen() : Int = {
    println((wildcards & OFMatch.OFPFW_NW_SRC_MASK) >> OFMatch.OFPFW_NW_SRC_SHIFT)
    return Math
      .max(32 - ((wildcards & OFMatch.OFPFW_NW_SRC_MASK) >> OFMatch.OFPFW_NW_SRC_SHIFT),
      0)
  }
}
