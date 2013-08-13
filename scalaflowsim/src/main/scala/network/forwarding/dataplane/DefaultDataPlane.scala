package network.forwarding.dataplane

import scala.collection.mutable.{ListBuffer, HashMap}
import network.device._
import network.traffic.{RunningFlow, ChangingRateFlow, FlowRateOrdering, Flow}
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import simengine.utils.Logging
import org.openflow.protocol.OFMatch


/**
 * the class representing the default functionalities to forward
 * packets/flows as well as the congestion control, etc.
 * that is that maxmin allocation
 */
class DefaultDataPlane extends ResourceAllocator with Logging {

  /**
   * perform max min allocation on the link
   * for now, if the flow's rate is down, it does not trigger the allocation
   * in the path of other flows
   * @param link the input link
   */
  override def allocate (link: Link) {
    if (linkFlowMap(link).size == 0) return
    var demandingflows = linkFlowMap(link).clone()
    var remainingBandwidth = link.bandwidth
    var avrRate = link.bandwidth / linkFlowMap(link).size
    logDebug("avrRate on " + link + " is " + avrRate)
    demandingflows = demandingflows.sorted(FlowRateOrdering)
    while (demandingflows.size != 0 && remainingBandwidth != 0) {
      val currentflow = demandingflows.head
      //initialize for the new flow
      if (currentflow.getTempRate == Double.MaxValue) currentflow.setTempRate(link.bandwidth)
      var demand = {
        if (currentflow.status != RunningFlow) currentflow.getTempRate
        else currentflow.Rate
      }
      logDebug("demand of flow " + currentflow + " is " + demand)
      if (demand <= avrRate) {
        remainingBandwidth -= demand
      } else {
        if (currentflow.status == RunningFlow) {
          //TODO: if avrRate < currentflow.rate trigger the change on its path
          currentflow.setRate(avrRate)
          logDebug("change flow " + currentflow + " rate to " + currentflow.Rate)
        } else {
          currentflow.setTempRate(avrRate) //set to avrRate
          logDebug("change flow " + currentflow + " temprate to " + currentflow.getTempRate)
        }
        remainingBandwidth -= avrRate
      }
      demandingflows.remove(0)
      if (demandingflows.size != 0) avrRate = remainingBandwidth / demandingflows.size
    }
  }

  /**
   * reallocate the flows' rate on the link, always called when a flow
   * is finished
   * @param link on the involved link
   */
  override def reallocate(link: Link) {
    if (linkFlowMap(link).size == 0) return
    var demandingflows = linkFlowMap(link).clone()
    var remainingBandwidth = link.bandwidth
    var avrRate = link.bandwidth / linkFlowMap(link).size
    logDebug("avrRate on " + link + " is " + avrRate)
    demandingflows.map(f => {f.status = ChangingRateFlow; f.setTempRate(avrRate)})
    demandingflows = demandingflows.sorted(FlowRateOrdering)
    while (demandingflows.size != 0 && remainingBandwidth != 0) {
      //the flow with the minimum rate
      val currentflow = demandingflows.head
      val flowdest = GlobalDeviceManager.getHost(currentflow.dstIP)
      val matchfield = OFFlowTable.createMatchField(flow = currentflow,
        wcard = (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK))
      //try to acquire the max-min rate starting from the dest of this flow
      flowdest.dataplane.allocate(currentflow, flowdest.controlplane.fetchInRoutingEntry(matchfield))
      demandingflows.remove(0)
      if (demandingflows.size != 0) avrRate = remainingBandwidth / demandingflows.size
    }
  }
}


