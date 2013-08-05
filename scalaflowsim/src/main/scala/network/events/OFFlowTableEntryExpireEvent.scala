package network.events

import scalasim.network.controlplane.routing.OpenFlowRouting
import scalasim.simengine.EventOfTwoEntities
import org.openflow.protocol.OFMatch

final class OFFlowTableEntryExpireEvent (routingModule : OpenFlowRouting, matchfield : OFMatch, t : Double)
  extends EventOfTwoEntities[OpenFlowRouting, OFMatch] (routingModule, matchfield, t) {

  def process {
    routingModule.removeFlowEntry(matchfield)
  }
}
