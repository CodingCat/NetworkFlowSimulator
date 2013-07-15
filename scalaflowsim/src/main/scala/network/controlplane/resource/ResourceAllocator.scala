package network.controlplane.resource

import network.traffic.Flow
import network.component.{Node, Link}
import scala.collection.mutable.{ListBuffer, HashMap, HashSet}
import network.controlplane.ControlPlane

abstract private [controlplane] class ResourceAllocator (controlPlane : ControlPlane) {

  protected val linkFlowMap = new HashMap[Link, ListBuffer[Flow]]

  def allocate(link : Link)

  def reallocate(link : Link)

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

  def deleteFlow(flow : Flow) {
    for (linkflowpair <- linkFlowMap) {
      if (linkflowpair._2.contains(flow)) linkflowpair._2 -= flow
    }
  }

  def apply(link : Link) : ListBuffer[Flow] = linkFlowMap(link)
}
