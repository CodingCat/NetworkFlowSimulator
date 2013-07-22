package simengine.openflow

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.openflow.protocol.factory.BasicFactory
import org.openflow.protocol.{OFHello, OFType, OFMessage}
import scala.collection.JavaConversions._
import simengine.utils.Logging
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import scala.collection.mutable.ListBuffer

class OpenFlowMsgEncoder extends OneToOneEncoder {

  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
    if (!(msg.isInstanceOf[java.util.ArrayList[_]])) return msg
    val msglist = msg.asInstanceOf[java.util.ArrayList[OFMessage]]
    var size: Int = 0
    for (ofm <- msglist) {
      size += ofm.getLengthU
    }
    val buf: ChannelBuffer = ChannelBuffers.buffer(size)
    for (ofm <- msglist) {
      ofm.writeTo(buf)
    }
    return buf
  }
}

class OpenFlowMsgDecoder extends FrameDecoder {

  private val factory = new BasicFactory()

  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer)  : AnyRef = {
    //parse the channelbuffer into the list of ofmessage
    if (!channel.isConnected) return None
    factory.parseMessage(buffer)
  }
}

class OpenFlowChannelHandler extends SimpleChannelHandler  with Logging  {
  private val factory = new BasicFactory()

  private def processMessage(ofm : OFMessage) = ofm.getType match {
    case OFType.HELLO => {
      logTrace("receive a hello message from controller")
      factory.getMessage(OFType.HELLO).asInstanceOf[OFHello]
    }
    case _ => throw new Exception("unrecognized message type:" + ofm)
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val msglist = e.getMessage.asInstanceOf[java.util.ArrayList[OFMessage]]
    val outmsglist = new java.util.ArrayList[OFMessage]
    for (ofm: OFMessage <- msglist) {
      try {
        outmsglist += processMessage(ofm)
      }
      catch {
        case ex: Exception => Channels.fireExceptionCaught(ctx.getChannel, ex)
      }
    }
    //send out all messages
    e.getChannel.write(outmsglist)
  }

  override def exceptionCaught (ctx : ChannelHandlerContext, e : ExceptionEvent) {
    e.getCause.printStackTrace()
  }
}