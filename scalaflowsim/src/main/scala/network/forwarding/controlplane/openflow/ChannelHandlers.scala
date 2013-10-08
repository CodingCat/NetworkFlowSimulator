package network.forwarding.controlplane.openflow

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.openflow.protocol._
import scala.collection.JavaConversions._
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.openflow.protocol.factory.BasicFactory
import scala.collection.mutable.{ListBuffer, ArrayBuffer}

class OpenFlowMsgEncoder extends OneToOneEncoder {

  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
    if (!msg.isInstanceOf[ArrayBuffer[_]]) return msg
    val msglist = msg.asInstanceOf[ArrayBuffer[OFMessage]]
    var size: Int = 0
    msglist.foreach(ofm => size += ofm.getLength)
    val buf = ChannelBuffers.buffer(size)
    msglist.foreach(ofm =>
      if (ofm != null) ofm.writeTo(buf))
    buf
  }
}

class OpenFlowMsgDecoder extends FrameDecoder {
  val factory = new BasicFactory
  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer)  : AnyRef = {
    //parse the channelbuffer into the list of ofmessage
    if (!channel.isConnected) return None
    factory.parseMessage(buffer)
  }
}

class OpenFlowMessageDispatcher (private val ofcontrolplane : OpenFlowControlPlane)
  extends SimpleChannelHandler {

  private val msglistenerList = new ListBuffer[MessageListener]
  private def registerMessageListener {
    msglistenerList += ofcontrolplane
    msglistenerList += ofcontrolplane.ofinterfacemanager
  }

  override def channelConnected(ctx : ChannelHandlerContext, e : ChannelStateEvent) {
    //save the channel for sending packet
    ofcontrolplane.toControllerChannel = e.getChannel
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    if (!e.getMessage.isInstanceOf[java.util.ArrayList[_]]) return
    val msglist = e.getMessage.asInstanceOf[java.util.ArrayList[OFMessage]]
    for (ofm: OFMessage <- msglist) {
      msglistenerList.foreach(handler => handler.handleMessage(ofm))
    }
    //send out all messages
    if (e.getChannel != null && e.getChannel.isConnected)
      ofcontrolplane.ofmsgsender.flushBuffer(e.getChannel)
  }

  override def exceptionCaught (ctx : ChannelHandlerContext, e : ExceptionEvent) {
    e.getCause.printStackTrace()
  }

  registerMessageListener
}