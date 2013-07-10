package network.events

import scalasim.simengine.EventOfSingleEntity
import network.data.Flow


class CompleteFlowEvent (private val flow : Flow, t : Double) extends EventOfSingleEntity[Flow] (flow, t) {

  def process {
    Flow.finishedFlows += flow
  }
}
