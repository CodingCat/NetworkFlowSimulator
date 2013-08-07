package scalasim.network.events

import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.routing.RoutingProtocol
import scalasim.simengine.{EventOfSingleEntity, SimulationEngine}
import scalasim.network.traffic.Flow
import org.openflow.protocol.OFMatch

/**
 *
 * @param flow finished matchfield
 * @param t timestamp of the event
 */
final class CompleteFlowEvent (flow : Flow, t : Double)
  extends EventOfSingleEntity[Flow] (flow, t) {

  def process {
    logInfo("flow " + flow + " completed at " + SimulationEngine.currentTime)
    flow.close()
    //ends at the flow destination
    val matchfield = OFFlowTable.createMatchField(flow = flow, wcard =
      (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK))
    val destinationControlPlane = RoutingProtocol.getFlowStarter(flow.dstIP).controlPlane
    destinationControlPlane.finishFlow(flow, matchfield)
  }
}
