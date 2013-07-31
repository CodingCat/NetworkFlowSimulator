package scalasim.network.controlplane.openflow


import scalasim.network.component._
import scalasim.network.controlplane.resource.ResourceAllocator
import scalasim.network.controlplane.routing.RoutingProtocol
import scalasim.network.controlplane.topology.TopologyManager
import scalasim.network.controlplane.{ControlPlane, TCPControlPlane}
import scalasim.network.traffic.Flow
import scalasim.simengine.utils.Logging
import scalasim.XmlParser
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import java.net.InetSocketAddress
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.openflow.protocol._
import org.openflow.util.HexString
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import java.util
import org.openflow.protocol.factory.BasicFactory
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.jboss.netty.channel.Channel
import org.slf4j.LoggerFactory

abstract class OpenFlowSwitchStatus

case object OpenFlowSwitchHandShaking extends OpenFlowSwitchStatus
case object OpenFlowSwitchRunning extends OpenFlowSwitchStatus
case object OpenFlowSwitchClosing extends OpenFlowSwitchStatus

class OpenFlowModule (router : Router,
                      routingModule : RoutingProtocol,
                      resourceModule : ResourceAllocator,
                      topoModule : TopologyManager)
  extends ControlPlane (router, routingModule, resourceModule, topoModule) {

  private val host = XmlParser.getString("scalasim.network.controlplane.openflow.controller.host", "127.0.0.1")
  private val port = XmlParser.getInt("scalasim.network.controlplane.openflow.controller.port", 6633)

  private var config_flags : Short = 0
  private var miss_send_len : Short = 0

  private [openflow] var status : OpenFlowSwitchStatus = OpenFlowSwitchHandShaking

  private [openflow] val flowtables : Array[OFFlowTable] = new Array[OFFlowTable](
    XmlParser.getInt("scalasim.network.controlplane.openflow.tablenum", 1)
  )

  private val factory = new BasicFactory

  private [openflow] var toControllerChannel : Channel = null

  def init() {
    //leave interface to implement pipeline
    for (i <- 0 until flowtables.length) flowtables(i) = new OFFlowTable(
      XmlParser.getInt("scalasim.network.controlplane.openflow.flowExpireDuration", 600),
      XmlParser.getInt("scalasim.network.controlplane.openflow.flowIdleDuration", 300))
  }

  //build channel to the controller
  def connectToController() {
    try {
      val clientfactory = new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool())
      val clientbootstrap = new ClientBootstrap(clientfactory)
      clientbootstrap.setPipelineFactory(new OpenFlowMsgPipelineFactory(this))
      clientbootstrap.connect(new InetSocketAddress(host, port))
    }
    catch {
      case e : Exception => e.printStackTrace
    }
  }

  def setParameter(f : Short, m : Short) {
    config_flags = f
    miss_send_len = miss_send_len
  }

  def getFlag = config_flags
  def get_miss_send_len = miss_send_len

  def getDPID = {
    val impl_dependent = router.nodetype match {
      case ToRRouterType => "00"
      case AggregateRouterType => "01"
      case CoreRouterType => "02"
    }
    val t = router.ip_addr(0).substring(router.ip_addr(0).indexOf('.') + 1, router.ip_addr(0).size)
    //TODO: implicitly limit the maximum number of pods, improve?
    val podid = HexString.toHexString(Integer.parseInt(t.substring(0, t.indexOf('.'))), 1)
    HexString.toLong(impl_dependent + ":" + podid + ":" + node.mac_addr(0))
  }


  def getSwitchFeature() = {
    //TODO: specify the switch features
    //(dpid, buffer, n_tables, capabilities, physical port
    (getDPID, 1000, flowtables.length, 7,
      router.controlPlane.topoModule.physicalports.values.toList)
  }

  def setSwitchParameters(configpacket : OFSetConfig) {
    config_flags = configpacket.getFlags
    miss_send_len = configpacket.getMissSendLength
  }

  def getSwitchParameters() : (Short, Short) = {
    (config_flags, miss_send_len)
  }

  def getSwitchDescription = router.ip_addr(0)

  private def sendMessageToController(message : OFMessage) {
    val buffer = ChannelBuffers.buffer(message.getLength)
    message.writeTo(buffer)
    if (toControllerChannel.isConnected)
      toControllerChannel.write(buffer)
    else
      throw new Exception("the openflow switch " + router.ip_addr(0) +
      " has been disconnected with the controller")
  }

  def sendLLDPtoController (l : Link, lldpData : Array[Byte]) {
    val port = topoModule.physicalports(l)
    //send out packet_in
    val packet_in_msg = factory.getMessage(OFType.PACKET_IN).asInstanceOf[OFPacketIn]
    packet_in_msg.setBufferId(-1)
      .setInPort(port.getPortNumber)
      .setPacketData(lldpData)
      .setReason(OFPacketIn.OFPacketInReason.ACTION)
      .setTotalLength(lldpData.length.toShort)
    sendMessageToController(packet_in_msg)
  }

  def sendPacketInToController(inlink : Link, ethernetFramedata : Array[Byte]) {
    val port = topoModule.physicalports(inlink)
    val packet_in_msg = factory.getMessage(OFType.PACKET_IN).asInstanceOf[OFPacketIn]
    packet_in_msg.setBufferId(0)
      .setInPort(port.getPortNumber)
      .setPacketData(ethernetFramedata)
      .setReason(OFPacketIn.OFPacketInReason.NO_MATCH)
      .setTotalLength(ethernetFramedata.length.toShort)
    sendMessageToController(packet_in_msg)
  }

  /**
   * allowcate resource to the flow
   * @param flow
   */
  def allocate(flow: Flow): Flow = flow

  /**
   * cleanup job when a flow is deleted
   * @param flow
   */
  def finishFlow(flow: Flow) {}

  /**
   * routing the flow
   * @param flow
   */
  def routing(flow: Flow, inlink : Link) {
    routingModule.selectNextLink(flow, inlink)
  }

  //init
  init
}
