package scalasim.network.events

import scalasim.network.component.Host
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.simengine.{SimulationEngine, EventOfTwoEntities}
import scalasim.network.traffic.Flow
import org.openflow.protocol.OFMatch

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
    SimulationEngine.atomicLock.acquire()
    logDebug("acquire lock at StartEvent")
    host.controlPlane.routing(flow,
      OFFlowTable.createMatchField(flow = flow,
        wcard = OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK),
      null)
  }
}
