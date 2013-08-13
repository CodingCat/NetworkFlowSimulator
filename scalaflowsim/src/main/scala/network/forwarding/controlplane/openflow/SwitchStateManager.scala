package network.forwarding.controlplane.openflow

import org.openflow.protocol.OFMessage


class SwitchStateManager extends MessageListener {

  def handleMessage (msg: OFMessage) {
    msg.getType match {
    }
  }
}
