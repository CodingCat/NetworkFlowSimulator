package scalasim.network.controlplane

import scalasim.network.component.{Link, Node}
import scalasim.network.controlplane.resource.ResourceAllocator
import scalasim.network.controlplane.routing.RoutingProtocol
import scalasim.network.controlplane.topology.TopologyManager
import scalasim.network.traffic.Flow

abstract class ControlPlane (private [controlplane] val node : Node,
                             private [controlplane] val routingModule : RoutingProtocol,
                             private [controlplane] val resourceModule : ResourceAllocator,
                             private [controlplane] val topoModule : TopologyManager) {

  lazy val IP : String = node.toString

  override def toString = "controlplane-" + node.toString

  /**
   * allowcate resource to the flow
   * @param flow
   */
  def allocate(flow : Flow) : Flow

  /**
   * cleanup job when a flow is deleted
   * @param flow
   */
  def finishFlow(flow : Flow)

  /**
   * routing the flow
   * @param flow
   */
  def routing (flow : Flow, inlink : Link)

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
