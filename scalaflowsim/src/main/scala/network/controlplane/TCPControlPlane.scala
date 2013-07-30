package scalasim.network.controlplane

import scalasim.simengine.SimulationEngine
import scalasim.XmlParser
import scalasim.network.component.{Link, Node}
import scalasim.network.controlplane.routing.RoutingProtocol
import scalasim.network.traffic.{CompletedFlow, RunningFlow, NewStartFlow, Flow}
import scalasim.network.events.CompleteFlowEvent
import scalasim.simengine.utils.Logging
import scala.collection.mutable.HashMap
import scalasim.network.controlplane.topology.TopologyManager
import scalasim.network.controlplane.resource.ResourceAllocator


class TCPControlPlane(node : Node,
                      routingModule : RoutingProtocol,
                      resourceModule : ResourceAllocator,
                      topoModule : TopologyManager)
  extends ControlPlane (node, routingModule, resourceModule, topoModule) with Logging {

  private def nextNode(link : Link) : Node = {
    if (link.end_to == node) link.end_from
    else link.end_to
  }

  def allocate (flow : Flow) : Flow = {
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
    flow
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

  def routing (flow : Flow) {
    logTrace("arrive at " + node.ip_addr(0))
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

  override def toString = "TCPControlPlane-" + node

}
