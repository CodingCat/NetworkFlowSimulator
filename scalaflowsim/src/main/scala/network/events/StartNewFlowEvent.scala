package network.events

import scalasim.simengine.{SimulationEngine, EventOfTwoEntities}
import network.data.Flow
import network.topo.Host
import simengine.utils.Logging

class StartNewFlowEvent (flow : Flow, host : Host, timestamp : Double)
  extends EventOfTwoEntities [Flow, Host] (flow, host, timestamp) {

  def process() {
    logTrace("start the flow " + flow + " at " + SimulationEngine.currentTime)
    host.controlPlane.routing(flow)
  }
}
