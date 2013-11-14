package network.forwarding.controlplane.openflow

import org.openflow.protocol.OFMessage
import org.jboss.netty.channel.Channel
import scala.collection.immutable.ListSet


class OpenFlowMsgSender () {

  //used to batch IO
  private [openflow] var ioBatchBuffer = new ListSet[OFMessage]

  //used to store those messages pended for the unconnected messages
  private [openflow] var msgPendingBuffer = new ListSet[OFMessage]

  def pushInToBuffer (msg : OFMessage) {
    ioBatchBuffer +=  msg
  }

  def sendMessageToController(channel : Channel, message : OFMessage) {
    msgPendingBuffer += message
    if (channel != null && channel.isConnected) {
      channel.write(msgPendingBuffer)
      msgPendingBuffer = new ListSet[OFMessage]
    }
  }

  def flushBuffer(channel : Channel) {
    if (channel != null && channel.isConnected) {
      channel.write(ioBatchBuffer)
      ioBatchBuffer = new ListSet[OFMessage]
    }
  }
}
