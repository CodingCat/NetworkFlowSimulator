package network.events

import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.simengine.EventOfTwoEntities
import org.openflow.protocol.OFMatch

class OFFlowTableEntryExpireEvent (table : OFFlowTable, matchfield : OFMatch, t : Double)
  extends EventOfTwoEntities[OFFlowTable, OFMatch] (table, matchfield, t) {

  def process {
    table.removeEntry(matchfield)
  }
}
