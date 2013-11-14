package network.forwarding.controlplane.openflow

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.openflow.protocol._
import scala.collection.JavaConversions._
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.openflow.protocol.factory.BasicFactory
import scala.collection.mutable.{ListBuffer, ArrayBuffer}
import java.util

class OpenFlowMsgEncoder extends OneToOneEncoder {

  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
    //if (!msg.isInstanceOf[ArrayBuffer]) return msg
    val msglist = msg.asInstanceOf[ArrayBuffer[OFMessage]]
    var size: Int = 0
    msglist.foreach(ofm => size += ofm.getLength)
    val buf = ChannelBuffers.buffer(size)
    try {
      msglist.foreach(ofm => {
        println("buffer size:" + buf.capacity() + " write index:" + buf.writerIndex())
        if (buf == null) println("NULL BUFFER")
        else if (ofm == null) println("NULL OFM")
        if (ofm != null) {
          ofm.writeTo(buf)
        }
      })
    } catch {
      case e: ArrayIndexOutOfBoundsException => {
        println()
      }
    }
    buf
  }
}

class OpenFlowMsgDecoder extends FrameDecoder {
  val factory = new BasicFactory
  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): java.util.List[OFMessage] = {
    //parse the channelbuffer into the list of ofmessage
    if (!channel.isConnected) return null
    factory.parseMessage(buffer)
  }
}

class OpenFlowMessageDispatcher (private val ofcontrolplane : OpenFlowControlPlane)
  extends SimpleChannelHandler {

  private var barrier_set: Boolean = false
  private val barriedmsglist: util.ArrayList[OFMessage] = new util.ArrayList[OFMessage]

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
    if (!e.getMessage.isInstanceOf[java.util.List[_]]) return
    val msglist = e.getMessage.asInstanceOf[java.util.List[OFMessage]]
    for (ofm: OFMessage <- msglist) {
      //if (ofm.getType == OFType.BARRIER_REQUEST) barrier_set = true
      msglistenerList.foreach(handler =>
        /*if (!barrier_set)
          handler.handleMessage(ofm)
        else
          barriedmsglist.add(ofm)*/
        handler.handleMessage(ofm)
      )
    }
    //note: we process the messages in sequence, so the barrier does not make any sense
   /* barrier_set = false
    if (!barrier_set) {
      for (ofm <- barriedmsglist) {
        msglistenerList.foreach(handler => handler.handleMessage(ofm))
      }
      barriedmsglist.clear()
    } */
    //send out all messages
    if (e.getChannel != null && e.getChannel.isConnected)
      ofcontrolplane.ofmsgsender.flushBuffer(e.getChannel)
  }

  override def exceptionCaught (ctx : ChannelHandlerContext, e : ExceptionEvent) {
    e.getCause.printStackTrace()
  }

  registerMessageListener
}