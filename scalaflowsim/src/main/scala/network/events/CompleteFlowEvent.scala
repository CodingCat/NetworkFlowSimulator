package network.events

import scalasim.simengine.{EventOfTwoEntities, SimulationEngine, EventOfSingleEntity}
import network.data.{CompletedFlow, Flow}
import scalasim.XmlParser
import network.component.Node


class CompleteFlowEvent (private val flow : Flow, node : Node, t : Double)
  extends EventOfTwoEntities[Flow, Node] (flow, node, t) {

  def process {
    logInfo("flow " + flow + " completed at " + SimulationEngine.currentTime)
    flow.status = CompletedFlow
    Flow.finishedFlows += flow
    flow.setRate(0.0)
    node.controlPlane.finishFlow(flow)
  }
}
