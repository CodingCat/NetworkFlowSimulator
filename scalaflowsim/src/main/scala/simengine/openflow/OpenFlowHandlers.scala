package simengine.openflow

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.openflow.protocol.factory.BasicFactory
import org.openflow.protocol.{OFHello, OFType, OFMessage}
import scala.collection.JavaConversions._

class OpenFlowMsgDecoder extends FrameDecoder {

  private val factory = new BasicFactory()

  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer)  : AnyRef = {
    //parse the channelbuffer into the list of ofmessage
    if (!channel.isConnected) return None
    factory.parseMessage(buffer)
  }
}

class OpenFlowChannelHandler extends SimpleChannelHandler {
  private val factory = new BasicFactory()

  override def messageReceived(ctx : ChannelHandlerContext, e : MessageEvent) {
    val msglist = e.getMessage.asInstanceOf[java.util.ArrayList[OFMessage]]
    for (ofm: OFMessage <- msglist) {
      try {
        println(ofm.toString)
        if (ofm.getType == OFType.HELLO) {
          val hellomsg = factory.getMessage(OFType.HELLO).asInstanceOf[OFHello]
          val buffer = ChannelBuffers.buffer(hellomsg.getLengthU)
          hellomsg.writeTo(buffer)
          e.getChannel.write(buffer)
        }
      }
      catch {
        // We are the last handler in the stream, so run the
        // exception through the channel again by passing in
        // ctx.getChannel().
        case ex: Exception => Channels.fireExceptionCaught(ctx.getChannel, ex)
      }
    }
  }

  override def exceptionCaught (ctx : ChannelHandlerContext, e : ExceptionEvent) {
    e.getCause.printStackTrace()
  }
}