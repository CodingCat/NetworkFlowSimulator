package network.events

import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.routing.OpenFlowRouting
import scalasim.simengine.EventOfTwoEntities
import org.openflow.protocol.OFMatch

class OFFlowTableEntryExpireEvent (routingModule : OpenFlowRouting, matchfield : OFMatch, t : Double)
  extends EventOfTwoEntities[OpenFlowRouting, OFMatch] (routingModule, matchfield, t) {

  def process {
    routingModule.removeFlowEntry(matchfield)
  }
}
