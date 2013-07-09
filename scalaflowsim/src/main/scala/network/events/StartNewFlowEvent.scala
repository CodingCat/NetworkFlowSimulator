package network.events

import scalasim.simengine.{EventOfTwoEntities}
import network.data.Flow
import network.topo.Host

class StartNewFlowEvent (flow : Flow, host : Host, timestamp : Double)
  extends EventOfTwoEntities [Flow, Host] (flow, host, timestamp) {

  def process() {
    System.out.println("start flow:" + flow.SrcIP + "->" + flow.DstIP)
    host.controlPlane.decide(flow)
  }
}
