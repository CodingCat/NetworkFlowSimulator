package simengine.openflow

import scalasim.XmlParser
import org.jboss.netty.channel._
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import java.net.InetSocketAddress
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory

object OpenFlowMsgDispatcher {
  private val host = XmlParser.getString("scalasim.simengine.openflow.controller.host", "127.0.0.1")
  private val port = XmlParser.getInt("scalasim.simengine.openflow.controller.port", 6633)
  private var connector : Channel = null

  //build channel to the controller
  def initChannel() {
    try {
      val clientfactory = new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool())
      val clientbootstrap = new ClientBootstrap(clientfactory)

      clientbootstrap.setPipelineFactory(new OpenFlowMsgPipelineFactory)
      val ch = clientbootstrap.connect(new InetSocketAddress(host, port))
      connector = ch.getChannel
    }
    catch {
      case e : Exception => e.printStackTrace
    }
  }
}
