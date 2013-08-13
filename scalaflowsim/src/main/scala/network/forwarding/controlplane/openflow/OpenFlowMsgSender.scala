package network.forwarding.controlplane.openflow

import scala.collection.mutable.ArrayBuffer
import org.openflow.protocol.OFMessage
import scala.collection.mutable
import org.jboss.netty.channel.Channel


class OpenFlowMsgSender (private val channel : Channel) {

  //used to batch IO
  private [openflow] val ioBatchBuffer = new ArrayBuffer[OFMessage] with
    mutable.SynchronizedBuffer[OFMessage]
  //used to store those messages pended for the unconnected messages
  private [openflow] val msgPendingBuffer = new ArrayBuffer[OFMessage] with
    mutable.SynchronizedBuffer[OFMessage]

  def pushInToBuffer (msg : OFMessage) {
    ioBatchBuffer += msg
  }

  def sendMessageToController(message : OFMessage) {
    msgPendingBuffer += message
    if (channel != null && channel.isConnected) {
      channel.write(msgPendingBuffer)
      msgPendingBuffer.clear()
    }
  }

  def flushBuffer() {
    if (channel.isConnected) channel.write(ioBatchBuffer)
  }
}
