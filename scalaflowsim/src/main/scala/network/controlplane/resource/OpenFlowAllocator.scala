package scalasim.network.controlplane.resource

import scalasim.network.component.Link
import scalasim.network.controlplane.openflow.OpenFlowModule

class OpenFlowAllocator (ofcp : OpenFlowModule) extends ResourceAllocator (ofcp) {

  def allocate(link: Link) {}

  def reallocate(link: Link) {}

}
