package network.events

import scalasim.simengine.{EventOfTwoEntities}

class StartNewFlowEvent[Flow, Host] (flow : Flow, host : Host, timestamp : Double)
  extends EventOfTwoEntities [Flow, Host] (flow, host, timestamp) {

  def process() {

  }
}
