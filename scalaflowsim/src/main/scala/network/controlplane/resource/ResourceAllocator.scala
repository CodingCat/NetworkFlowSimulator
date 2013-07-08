package network.controlplane.resource

import network.data.Flow
import network.topo.{Node, Link}
import scala.collection.mutable.{HashMap, HashSet}


abstract private [controlplane] class ResourceAllocator (node : Node) {

  protected val linkFlowMap = new HashMap[Link, HashSet[Flow]]

  def init() {
    for (link <- node.outlink.values) linkFlowMap += (link -> new HashSet[Flow])
  }

  def allocate(flow : Flow, link : Link) : Double

  def insertNewLinkFlowPair(link : Link, flow : Flow) = linkFlowMap(link) += flow

  init()
}
