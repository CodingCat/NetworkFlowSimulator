package network.events

import simengine.Event
import network.traffic.GlobalFlowStore

class UpdateFlowPropertyEvent (timestamp : Double) extends Event(timestamp) {

  override def repeatInFuture(future : Double) : Event =
    new UpdateFlowPropertyEvent(timestamp + future)

  def process() {GlobalFlowStore.getFlows.foreach(f => f.updateTransferredData())}
}
