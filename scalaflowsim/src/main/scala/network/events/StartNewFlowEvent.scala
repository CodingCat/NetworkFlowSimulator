package network.events

import scalasim.simengine.EventOfSingleEntity
import network.topo.Host


class StartNewFlowEvent (host : Host, timestamp : Double) extends EventOfSingleEntity[Host] (timestamp) {

  def process(entity: Host) {

  }
}
