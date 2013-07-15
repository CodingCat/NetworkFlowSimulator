package network.events

import scalasim.simengine.{EventOfTwoEntities, SimulationEngine}
import network.traffic.Flow
import network.component.Node

/**
 *
 * @param flow finished flow
 * @param node invoker of the flow
 * @param t timestamp of the event
 */
class CompleteFlowEvent (flow : Flow, node : Node, t : Double)
  extends EventOfTwoEntities[Flow, Node] (flow, node, t) {

  def process {
    logInfo("flow " + flow + " completed at " + SimulationEngine.currentTime)
    flow.close()
    node.controlPlane.finishFlow(flow)
  }
}
