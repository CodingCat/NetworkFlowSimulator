package scalasim.network.events

import scalasim.network.component.Host
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.simengine.{SimulationEngine, EventOfTwoEntities}
import scalasim.network.traffic.Flow

/**
 *
 * @param flow the new start matchfield
 * @param host the invoker of the matchfield
 * @param timestamp the timestamp of the event
 */
final class StartNewFlowEvent (flow : Flow, host : Host, timestamp : Double)
  extends EventOfTwoEntities [Flow, Host] (flow, host, timestamp) {

  def process() {
    logTrace("start the flow " + flow + " at " + SimulationEngine.currentTime)
    //null in the last parameter means it's the first hop of the flow
    host.controlPlane.routing(flow, OFFlowTable.createMatchField(flow), null)
  }
}
