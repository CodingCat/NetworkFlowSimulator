package scalasim.simengine.openflow

import scalasim.network.component.{Pod, Router, Node}
import scalasim.XmlParser
import org.jboss.netty.channel._
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import java.net.InetSocketAddress
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import scala.collection.mutable.ListBuffer
import org.openflow.protocol._
import org.openflow.protocol.factory.BasicFactory

object OpenFlowMsgDispatcher {
  private val host = XmlParser.getString("scalasim.simengine.openflow.controller.host", "127.0.0.1")
  private val port = XmlParser.getInt("scalasim.simengine.openflow.controller.port", 6633)
  private var connector : Channel = null
  private val handshakingRouters = new ListBuffer[Router]

  private val factory = new BasicFactory

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

  def addPod(pod : Pod) {
    for (i <- 0 until pod.numAggRouters) handshakingRouters += pod.getAggregatRouter(i)
    for (i <- 0 until pod.numRacks) handshakingRouters += pod.getToRRouter(i)
  }

  def addSwitch(sw : Router) {
    handshakingRouters += sw
  }

  def getFeatureOfCurrentNode() = {
    (handshakingRouters(0).getDPID, 1000, 2, 7, new java.util.ArrayList[OFPhysicalPort])
  }

  def setSwitchParameters(configpacket : OFSetConfig) {
    handshakingRouters(0).setParameter(configpacket.getFlags, configpacket.getMissSendLength)
  }

  def getSwitchParameters() : (Short, Short) = {
    (handshakingRouters(0).getFlag, handshakingRouters(0).get_miss_send_len)
  }

  def getSwitchDescription = handshakingRouters(0).ip_addr(0)

  def removeHandshakingHead { handshakingRouters.remove(0) }
}
