package network.events

import simengine.EventOfSingleEntity
import network.traffic.Flow


class FlowOnEvent (flow : Flow, timestamp : Double)
  extends EventOfSingleEntity[Flow] (flow, timestamp) {

  def process {

  }
}
