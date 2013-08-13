package network.events

import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import org.openflow.protocol.OFMatch
import network.traffic.Flow
import scalasim.simengine.EventOfSingleEntity
import simengine.SimulationEngine
import network.forwarding.controlplane.RoutingProtocol
import network.device.GlobalDeviceManager
import simengine.utils.Logging

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
    val destinationControlPlane = GlobalDeviceManager.getHost(flow.dstIP).controlplane
    destinationControlPlane.finishFlow(flow, matchfield)
  }
}
