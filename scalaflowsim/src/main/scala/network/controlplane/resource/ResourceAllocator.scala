package scalasim.network.controlplane.resource

import scalasim.network.traffic.Flow
import scala.collection.mutable.{ListBuffer, HashMap}
import scalasim.network.controlplane.ControlPlane
import scalasim.network.component.Link

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

object ResourceAllocator {
  def apply (name : String, controlPlane : ControlPlane) : ResourceAllocator = name match {
    case "MaxMin" => new MaxMinAllocator(controlPlane)
    case _ => throw new Exception("unrecognizable ResourceAllocator type")
  }
}