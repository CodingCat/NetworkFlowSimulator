package network.events

import network.traffic.Flow
import simengine.EventOfSingleEntity


class FlowOffEvent (flow : Flow, timestamp : Double)
  extends EventOfSingleEntity[Flow] (flow, timestamp) {

  def process {
    flow.setRate(0)
    if (flow.Demand > 0) {
      //SCHEDULE next ON event
    }
  }
}
