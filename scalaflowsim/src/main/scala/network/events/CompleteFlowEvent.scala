package network.events

import org.openflow.protocol.OFMatch
import network.traffic.Flow
import simengine.{EventOfSingleEntity, SimulationEngine}
import network.device.GlobalDeviceManager
import simengine.utils.Logging
import network.forwarding.controlplane.openflow.flowtable.OFFlowTable

/**
 *
 * @param flow finished matchfield
 * @param t timestamp of the event
 */
final class CompleteFlowEvent (flow : Flow, t : Double)
  extends EventOfSingleEntity[Flow] (flow, t) with Logging {

  def process {
    logInfo("flow " + flow + " completed at " + SimulationEngine.currentTime)
    flow.close()
    //ends at the flow destination
    val matchfield = OFFlowTable.createMatchField(flow = flow, wcard =
      (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK))
    GlobalDeviceManager.getHost(flow.dstIP).dataplane.finishFlow(
      GlobalDeviceManager.getHost(flow.dstIP), flow, matchfield)
  }
}
