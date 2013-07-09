package network.events

import scalasim.simengine.EventOfSingleEntity
import network.data.Flow


class CompleteFlowEvent (private val flow : Flow, t : Double) extends EventOfSingleEntity[Flow] (flow, t) {

  def process {
    System.out.println("finish flow:" + flow.SrcIP + "->" + flow.DstIP)
    Flow.finishedFlows += flow
  }
}
