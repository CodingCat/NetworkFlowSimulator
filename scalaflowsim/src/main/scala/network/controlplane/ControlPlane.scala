package scalasim.network.controlplane

import scalasim.network.component.{HostType, Link, Node}
import scalasim.network.controlplane.resource.ResourceAllocator
import scalasim.network.controlplane.routing.{OpenFlowRouting, RoutingProtocol}
import scalasim.network.controlplane.topology.TopologyManager
import scalasim.network.events.CompleteFlowEvent
import scalasim.network.traffic.{NewStartFlow, Flow}
import scalasim.simengine.SimulationEngine
import scalasim.simengine.utils.Logging
import org.openflow.protocol.OFMatch
import network.controlplane.openflow.flowtable.OFMatchField
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import simengine.utils.XmlParser

abstract class ControlPlane (private [controlplane] val node : Node,
                             private [controlplane] val routingModule : RoutingProtocol,
                             private [controlplane] val resourceModule : ResourceAllocator,
                             private [controlplane] val topoModule : TopologyManager) extends Logging {

  lazy val IP : String = node.toString

  override def toString = "controlplane-" + node.toString

  private val floodlist = new ArrayBuffer[Flow] with mutable.SynchronizedBuffer[Flow]

  /**
   *
   * @param flow
   * @param matchfield
   * @param inlink
   */
  def floodoutFlow(flow : Flow, matchfield : OFMatchField, inlink : Link) {
    if (!floodlist.contains(flow)) {
      val nextlinks = routingModule.getfloodLinks(inlink)
      //TODO : openflow flood handling in which nextlinks can be null?
      nextlinks.foreach(l => {
        flow.addTrace(l, inlink)
        Link.otherEnd(l, node).controlPlane.routing(flow, matchfield, l)
      })
      floodlist += flow
    }
  }

  private def startFlow (flow : Flow) {
    if (flow.status == NewStartFlow) {
      val completeEvent = new CompleteFlowEvent(
        flow,
        SimulationEngine.currentTime + flow.Demand / flow.getTempRate)
      logTrace("schedule complete event " + completeEvent + " for " + flow + " at " +
        (SimulationEngine.currentTime + flow.Demand / flow.getTempRate))
      flow.bindEvent(completeEvent)
      SimulationEngine.addEvent(completeEvent)
      //has found the destination, change the property
      if (flow.floodflag) flow.floodflag = false
    }
    flow.run
    SimulationEngine.atomicLock.release()
    logDebug("release lock at ControlPlane")
  }

  /**
   * allocate for the flows on the link
   * @param changingflow the new flow
   * @param link the involved link
   */
  private def allocateOnCurrentHop(changingflow : Flow, link : Link) {
    val nextnode = Link.otherEnd(link, node)
    val rpccontrolplane = {
      if (link.end_from == node) node.controlPlane
      else nextnode.controlPlane
    }
    //if it's a new flow, first insert it into the newlinkflowpair
    if (changingflow.status == NewStartFlow) {
      rpccontrolplane.resourceModule.insertNewLinkFlowPair(link, changingflow)
    }
    //determine all flows' rate in the link
    rpccontrolplane.resourceModule.allocate(link)
  }

  /**
   * allocate bandwidth to the flow along the path
   * @param flow the flow to be allocated
   * @param referenceLink  the last hop or null (when the node is just the destination of the flow)
   */
  def allocate (flow : Flow, referenceLink : Link = null) {
    if (node.ip_addr(0) == flow.srcIP) {
      startFlow(flow)
    } else {
      val laststep = {
        if (node.ip_addr(0) == flow.dstIP) referenceLink
        else flow.getLastHop(referenceLink)
      }
      val nextnode = Link.otherEnd(laststep, node)
      logTrace("allocate for flow " + flow.toString() + " on " + laststep + " at node " + node)
      allocateOnCurrentHop(flow, laststep)
      //continue the allocate process on the last hop
      nextnode.controlPlane.allocate(flow, laststep)
    }
  }

  def finishFlow(flow : Flow, matchfield : OFMatch, referencelink : Link = null) {
    if (node.ip_addr(0) != flow.srcIP) {
      logTrace("flow ended at " + node.ip_addr(0))
      val nextlink = {
        if (referencelink == null) routingModule.fetchInRoutingEntry(matchfield)
        else flow.getLastHop(referencelink)
      }
      val nextnode = Link.otherEnd(nextlink, node)
      val rpccontrolplane = {
        if (nextlink.end_from == node) node.controlPlane
        else nextnode.controlPlane
      }
      rpccontrolplane.resourceModule.deleteFlow(flow)
      nextnode.controlPlane.finishFlow(flow, matchfield, nextlink)
      //reallocate resource to other flows
      logTrace("reallocate resource on " + node.toString)
      rpccontrolplane.resourceModule.reallocate(nextlink)
      routingModule.deleteEntry(matchfield)
    }
  }

  protected def passbyFlow(flow : Flow) : Boolean = {
    if (node.ip_addr(0) !=  flow.srcIP && node.ip_addr(0) != flow.dstIP && node.nodetype == HostType) {
      logTrace("Discard flow " + flow + " on node " + node.toString)
      return true
    }
    false
  }

  protected def inFlowRegistration(matchfield : OFMatchField, inlink : Link) {
    routingModule.insertInPath(matchfield, inlink)

  }

  /**
   *
   * @param flow
   * @param inlink it can be null (for the first hop)
   */
  def routing (flow : Flow, matchfield : OFMatchField, inlink : Link) {
    //discard the flood packets
    if (passbyFlow(flow)) return
    logTrace("arrive at " + node.ip_addr(0) + ", routing (flow : Flow, matchfield : OFMatch, inlink : Link)" +
      " flow:" + flow + ", inlink:" + inlink)
    if (inlink != null) inFlowRegistration(matchfield, inlink)
    if (node.ip_addr(0) == flow.dstIP) {
      //start resource allocation process
      allocate(flow, inlink)
    } else {
      if (!flow.floodflag) {
        val nextlink = routingModule.selectNextLink(flow, matchfield, inlink)
        if (nextlink != null) {
          forward(nextlink, inlink, flow, matchfield)
        }
      } else {
        //it's a flood flow
        logTrace("flow " + flow + " is flooded out at " + node)
        floodoutFlow(flow, matchfield, inlink)
      }
    }
  }

  def forward (olink : Link, inlink : Link, flow : Flow, matchfield : OFMatchField) {
    val nextnode = Link.otherEnd(olink, node)
    logDebug("send through " + olink)
    flow.addTrace(olink, inlink)
    nextnode.controlPlane.routing(flow, matchfield, olink)
  }

  def getLinkAvailableBandwidth(l : Link) : Double = resourceModule.getLinkAvailableBandwidth(l)

  def registerIncomeLink(link : Link)  {
    topoModule.registerIncomeLink(link)
  }

  def registerOutgoingLink(link : Link) {
    topoModule.registerOutgoingLink(link)
  }

  //TODO: expose topo for test, need to be improved
  def outlinks = topoModule.outlink
  def inlinks = topoModule.inlinks
}
