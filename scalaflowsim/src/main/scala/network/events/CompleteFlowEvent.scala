package scalasim.network.events

import scalasim.network.component.Node
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.simengine.{EventOfTwoEntities, SimulationEngine}
import scalasim.network.traffic.Flow

/**
 *
 * @param flow finished matchfield
 * @param node invoker of the matchfield
 * @param t timestamp of the event
 */
class CompleteFlowEvent (flow : Flow, node : Node, t : Double)
  extends EventOfTwoEntities[Flow, Node] (flow, node, t) {

  def process {
    logInfo("matchfield " + flow + " completed at " + SimulationEngine.currentTime)
    flow.close()
    node.controlPlane.finishFlow(flow, OFFlowTable.createMatchField(flow))
  }
}
