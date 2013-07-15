package network.controlplane

import scalasim.simengine.SimulationEngine
import scalasim.XmlParser
import network.component.{Link, Node}
import network.controlplane.routing.{RoutingProtocol}
import network.traffic.{CompletedFlow, RunningFlow, NewStartFlow, Flow}
import network.events.CompleteFlowEvent
import simengine.utils.Logging
import scala.collection.mutable.HashMap
import network.controlplane.topology.TopologyManager
import network.controlplane.resource.ResourceAllocator


class ControlPlane(private [controlplane] val node : Node) extends Logging {
  private [controlplane] val routingModule = RoutingProtocol(
    XmlParser.getString("scalasim.router.routing", "SimpleSymmetricRouting"), this)
  private [controlplane] val resourceModule = ResourceAllocator(
    XmlParser.getString("scalasim.router.resource", "MaxMin"), this)
  private [controlplane] val topoModule = new TopologyManager(this)

  private def nextNode(link : Link) : Node = {
    if (link.end_to == node) link.end_from
    else link.end_to
  }

  def allocate (flow : Flow) {
    if (node.ip_addr(0) == flow.DstIP) {
      if (flow.status != RunningFlow && flow.status != CompletedFlow){
        if (flow.status == NewStartFlow) {
          val completeEvent = new CompleteFlowEvent(flow, RoutingProtocol.getFlowStarter(flow.SrcIP),
            SimulationEngine.currentTime + flow.Demand / flow.getTempRate)
          logTrace("schedule complete event " + completeEvent + " for " + flow + " at " +
            (SimulationEngine.currentTime + flow.Demand / flow.getTempRate))
          flow.bindEvent(completeEvent)
          SimulationEngine.addEvent(completeEvent)
        }
        flow.run
      }
    }
    else {
      val nextlink = routingModule.fetchRoutingEntry(flow)
      val nextnode = nextNode(nextlink)
      val rpccontrolplane = {
        if (nextlink.end_from == node) node.controlPlane
        else nextnode.controlPlane
      }
      if (flow.status == NewStartFlow)
        rpccontrolplane.resourceModule.insertNewLinkFlowPair(nextlink, flow)
      rpccontrolplane.resourceModule.allocate(nextlink)
      nextnode.controlPlane.allocate(flow)
    }
  }

  def finishFlow(flow : Flow) {
    if (node.ip_addr(0) != flow.DstIP) {
      logTrace("flow ended at " + node.ip_addr(0))
      val nextlink = routingModule.fetchRoutingEntry(flow)
      val nextnode = nextNode(nextlink)
      val rpccontrolplane = {
        if (nextlink.end_from == node) node.controlPlane
        else nextnode.controlPlane
      }
      rpccontrolplane.resourceModule.deleteFlow(flow)
      nextnode.controlPlane.finishFlow(flow)
      //reallocate resource to other flows
      rpccontrolplane.resourceModule.reallocate(nextlink)
      logTrace("delete route table entry:" + flow + " at " + node.ip_addr(0))
      routingModule.deleteEntry(flow)
    }
  }

  def getLinkAvailableBandwidth(l : Link) : Double = resourceModule.getLinkAvailableBandwidth(l)

  def registerIncomeLink(link : Link)  {
    topoModule.registerIncomeLink(link)
  }

  def registerOutgoingLink(link : Link) {
    topoModule.registerOutgoingLink(link)
  }


  def routing (flow : Flow) : Unit = {
    logInfo("arrive at " + node.ip_addr(0))
    if (node.ip_addr(0) != flow.DstIP) {
      val nextlink = routingModule.selectNextLink(flow)
      val nextnode = nextNode(nextlink)
      logDebug("send through " + nextlink)
      routingModule.insertFlowPath(flow, nextlink)
      nextnode.controlPlane.routing(flow)
    }
    else {
      //arrive the destination
      //start resource allocation process
      RoutingProtocol.getFlowStarter(flow.SrcIP).controlPlane.allocate(flow)
    }
  }

  override def toString = "ControlPlane-" + node

  def IP : String = node.toString

  def outlinks : HashMap[String, Link] = topoModule.outlink
  def inlinks : HashMap[String, Link] = topoModule.inlinks
}
