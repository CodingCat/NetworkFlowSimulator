package scalasim.network.controlplane

import scalasim.network.component.Node
import scalasim.network.controlplane.routing.RoutingProtocol
import scalasim.simengine.utils.Logging
import scalasim.network.controlplane.topology.TopologyManager
import scalasim.network.controlplane.resource.ResourceAllocator


class TCPControlPlane(node : Node,
                      routingModule : RoutingProtocol,
                      resourceModule : ResourceAllocator,
                      topoModule : TopologyManager)
  extends ControlPlane (node, routingModule, resourceModule, topoModule) with Logging {


  override def toString = "TCPControlPlane-" + node

}
