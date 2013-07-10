package network.events

import scalasim.simengine.EventOfSingleEntity
import network.data.{CompletedFlow, Flow}


class CompleteFlowEvent (private val flow : Flow, t : Double) extends EventOfSingleEntity[Flow] (flow, t) {

  def process {
    flow.status = CompletedFlow
    Flow.finishedFlows += flow
  }
}
