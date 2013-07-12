package network.events

import scalasim.simengine.{SimulationEngine, EventOfSingleEntity}
import network.data.{CompletedFlow, Flow}


class CompleteFlowEvent (private val flow : Flow, t : Double) extends EventOfSingleEntity[Flow] (flow, t) {

  def process {
    logTrace("flow " + flow + " completed at " + SimulationEngine.currentTime)
    flow.status = CompletedFlow
    Flow.finishedFlows += flow
  }
}
