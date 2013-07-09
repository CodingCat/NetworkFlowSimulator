package network.controlplane.resource

import network.data.Flow
import network.topo.{Node, Link}

private [controlplane] class MaxMinAllocator (node : Node) extends ResourceAllocator (node) {

  def allocate(flow: Flow, link: Link): Double = {
    insertNewLinkFlowPair(link, flow)
    val t = link.bandwidth / linkFlowMap(link).size
    if (t < flow.getTempRate) flow.setTempRate(t)
    return flow.getTempRate
  }
}
