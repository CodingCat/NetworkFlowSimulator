package scalasim.network.events

import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.routing.RoutingProtocol
import scalasim.simengine.{EventOfSingleEntity, EventOfTwoEntities, SimulationEngine}
import scalasim.network.traffic.Flow

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
    RoutingProtocol.getFlowStarter(flow.dstIP).controlPlane.finishFlow(flow, OFFlowTable.createMatchField(flow))
  }
}
