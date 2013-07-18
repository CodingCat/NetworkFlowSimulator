package simengine

import scalasim.XmlParser
import org.jboss.netty.channel._
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import java.net.InetSocketAddress
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory


class OpenFlowMsgHandler extends SimpleChannelHandler {

  override def messageReceived(ctx : ChannelHandlerContext, e : MessageEvent) {

  }


  override def writeRequested(ctx : ChannelHandlerContext, e : MessageEvent) {

  }

  override def exceptionCaught (ctx : ChannelHandlerContext, e : ExceptionEvent) {

  }
}

class OpenFlowMsgChannelPipelineFactory extends ChannelPipelineFactory {
  def getPipeline: ChannelPipeline = Channels.pipeline(new OpenFlowMsgHandler)
}

object OpenFlowMsgDispatcher {
  private val host = XmlParser.getString("scalasim.simengine.openflow.controller.host", "127.0.0.1")
  private val port = XmlParser.getInt("scalasim.simengine.openflow.controller.port", 6633)

  //build channel to the controller
  private def initChannel() {
    try {
      val clientfactory = new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool())
      val clientbootstrap = new ClientBootstrap(clientfactory)

      clientbootstrap.setPipelineFactory(new OpenFlowMsgChannelPipelineFactory)

      clientbootstrap.setOption("tcpNoDelay", true)
      clientbootstrap.setOption("keepAlive", true)

      clientbootstrap.connect(new InetSocketAddress(host, port))
    }
    catch {
      case e : Exception => e.printStackTrace;
    }
  }

  initChannel


}
