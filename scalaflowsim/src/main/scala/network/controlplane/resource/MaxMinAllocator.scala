package network.controlplane.resource

import network.data.Flow
import network.topo.{Node, Link}

private [controlplane] class MaxMinAllocator (node : Node) extends ResourceAllocator (node) {
  def allocate(flow: Flow, link: Link): Double = {
    return 0.0
  }
}
