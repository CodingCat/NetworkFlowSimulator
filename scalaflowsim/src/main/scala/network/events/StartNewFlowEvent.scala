package scalasim.network.events

import scalasim.network.component.Host
import scalasim.simengine.{SimulationEngine, EventOfTwoEntities}
import scalasim.network.traffic.Flow
import scalasim.XmlParser

/**
 *
 * @param flow the new start flow
 * @param host the invoker of the flow
 * @param timestamp the timestamp of the event
 */
class StartNewFlowEvent (flow : Flow, host : Host, timestamp : Double)
  extends EventOfTwoEntities [Flow, Host] (flow, host, timestamp) {

  def process() {
    logTrace("start the flow " + flow + " at " + SimulationEngine.currentTime)
    host.controlPlane.routing(flow)
  }
}
