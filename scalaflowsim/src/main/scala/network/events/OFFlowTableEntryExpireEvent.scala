package network.events

import scalasim.network.controlplane.routing.OpenFlowRouting
import scalasim.simengine.EventOfTwoEntities
import org.openflow.protocol.OFMatch
import network.controlplane.openflow.flowtable.OFMatchField
import scalasim.simengine.utils.Logging

final class OFFlowTableEntryExpireEvent (routingModule : OpenFlowRouting, matchfield : OFMatchField, t : Double)
  extends EventOfTwoEntities[OpenFlowRouting, OFMatch] (routingModule, matchfield, t) with Logging {

  def process {
    logInfo("entry for " + matchfield.toString + " expires at " + t)
    routingModule.removeFlowEntry(matchfield)
  }
}
