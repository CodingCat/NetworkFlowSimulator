package network.controlplane.resource

import network.data.Flow
import network.topo.{Node, Link}
import scala.collection.mutable.{ListBuffer, HashMap, HashSet}

abstract private [controlplane] class ResourceAllocator (node : Node) {

  protected val linkFlowMap = new HashMap[Link, ListBuffer[Flow]]

  def allocateForNewFlow(flow : Flow, link : Link)

  def insertNewLinkFlowPair(link : Link, flow : Flow) {
    if (linkFlowMap.contains(link) == false) {
      linkFlowMap += (link -> new ListBuffer[Flow])
    }
    linkFlowMap(link) += flow
  }

  def getLinkAvailableBandwidth(l : Link) : Double = {
    var usedBandwidth = 0.0
    for (f <- linkFlowMap(l)) usedBandwidth += f.Rate
    //for double precision problem
    Math.max(l.bandwidth - usedBandwidth, 0)
  }
}
