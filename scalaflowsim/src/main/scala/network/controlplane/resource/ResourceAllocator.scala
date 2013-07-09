package network.controlplane.resource

import network.data.Flow
import network.topo.{Node, Link}
import scala.collection.mutable.{HashMap, HashSet}


abstract private [controlplane] class ResourceAllocator (node : Node) {

  protected val linkFlowMap = new HashMap[Link, HashSet[Flow]]

  def allocate(flow : Flow, link : Link) : Double

  def insertNewLinkFlowPair(link : Link, flow : Flow) {
    if (linkFlowMap.contains(link) == false) {
      linkFlowMap += (link -> new HashSet[Flow])
    }
    linkFlowMap(link) += flow
  }

  def getLinkAvailableBandwidth(l : Link) : Double = {
    var usedBandwidth = 0.0
    for (f <- linkFlowMap(l)) usedBandwidth += f.Rate
    l.bandwidth - usedBandwidth
  }

}
