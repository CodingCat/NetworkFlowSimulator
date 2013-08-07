package scalasim.network.controlplane.openflow


import scalasim.network.component._
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.resource.ResourceAllocator
import scalasim.network.controlplane.routing.{OpenFlowRouting, RoutingProtocol}
import scalasim.network.controlplane.topology.TopologyManager
import scalasim.network.controlplane.ControlPlane
import scalasim.network.traffic.Flow
import scalasim.XmlParser
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import java.net.InetSocketAddress
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.openflow.protocol._
import org.openflow.util.HexString
import java.util
import org.openflow.protocol.factory.BasicFactory
import org.jboss.netty.channel.Channel
import org.slf4j.LoggerFactory
import org.openflow.protocol.action.{OFActionOutput, OFActionType, OFAction}
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

class OpenFlowModule (router : Router,
                      routingModule : RoutingProtocol,
                      resourceModule : ResourceAllocator,
                      topoModule : TopologyManager)
  extends ControlPlane (router, routingModule, resourceModule, topoModule) {

  private [openflow] val ofroutingModule = routingModule.asInstanceOf[OpenFlowRouting]

  private val host = XmlParser.getString("scalasim.network.controlplane.openflow.controller.host", "127.0.0.1")
  private val port = XmlParser.getInt("scalasim.network.controlplane.openflow.controller.port", 6633)

  private var config_flags : Short = 0
  private var miss_send_len : Short = 0

  private val logger = LoggerFactory.getLogger("OpenFlowModule")

  private var lldpcnt = 0;

  private val factory = new BasicFactory

  private [openflow] var toControllerChannel : Channel = null

  private [openflow] val ioBatchBuffer = new ArrayBuffer[OFMessage] with
    mutable.SynchronizedBuffer[OFMessage]
  private [openflow] val msgPendingBuffer = new ArrayBuffer[OFMessage] with
    mutable.SynchronizedBuffer[OFMessage]

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
    (getDPID, 1000, ofroutingModule.flowtables.length, 0x27,
      router.controlPlane.topoModule.linkphysicalportsMap.values.toList)
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
    msgPendingBuffer += message
    if (toControllerChannel.isConnected) {
      toControllerChannel.write(msgPendingBuffer)
      msgPendingBuffer.clear()
    }
    else
      throw new Exception("the openflow switch " + router.ip_addr(0) +
        " has been disconnected with the controller")
  }

  def sendPacketInToController(inlink: Link, ethernetFramedata: Array[Byte]) {
    val port = topoModule.linkphysicalportsMap(inlink)
    val packet_in_msg = factory.getMessage(OFType.PACKET_IN).asInstanceOf[OFPacketIn]
    logger.trace("send PACKET_IN to controller for table missing at node " + node)
    packet_in_msg.setBufferId(routingModule.asInstanceOf[OpenFlowRouting].pendingFlows.size)
      .setInPort(port.getPortNumber)
      .setPacketData(ethernetFramedata)
      .setReason(OFPacketIn.OFPacketInReason.NO_MATCH)
      .setTotalLength(ethernetFramedata.length.toShort)
      .setLength((ethernetFramedata.length + 18).toShort)
    sendMessageToController(packet_in_msg)
  }

  def sendLLDPtoController (l : Link, lldpData : Array[Byte]) {
    val port = topoModule.linkphysicalportsMap(l)
    //send out packet_in
    val packet_in_msg = factory.getMessage(OFType.PACKET_IN).asInstanceOf[OFPacketIn]
    packet_in_msg.setBufferId(-1)
      .setInPort(port.getPortNumber)
      .setPacketData(lldpData)
      .setReason(OFPacketIn.OFPacketInReason.ACTION)
      .setTotalLength(lldpData.length.toShort)
      .setLength((lldpData.length + 18).toShort)
    sendMessageToController(packet_in_msg)
  }

  def topologyHasbeenRecognized() = (lldpcnt >= (inlinks.size + outlinks.size))

  private def replyLLDP (pktoutMsg : OFPacketOut) {
    //send out through all ports
    log.trace("reply LLDP at " + node)
    lldpcnt = lldpcnt + 1
    val outport = pktoutMsg.getActions.get(0).asInstanceOf[OFActionOutput].getPort
    val outlink = topoModule.reverseSelection(outport)
    val neighbor = Link.otherEnd(outlink, node)
    val lldpdata = pktoutMsg.getPacketData
    if (neighbor.nodetype != HostType)
      neighbor.controlPlane.asInstanceOf[OpenFlowModule].sendLLDPtoController(outlink, lldpdata)
  }

  /**
   * called by the openflowhandler
   * routing according to the pktoutmsg
   * @param pktoutmsg
   */
  def routing(pktoutmsg : OFPacketOut) {
    //-1 is reserved for lldp packets
    if (pktoutmsg.getBufferId == -1) {
      //the packet data is included in the packoutmsg
      replyLLDP(pktoutmsg)
    }
    else {
      //TODO: is there any difference if we are on packet-level simulation?
      val pendingflow = ofroutingModule.pendingFlows(pktoutmsg.getBufferId)
      log.trace("receive a packet_out to certain buffer:" + pktoutmsg.toString)
      for (action : OFAction <- pktoutmsg.getActions.asScala) {
        action.getType match {
          //only support output for now
          case OFActionType.OUTPUT => {
            val outaction = action.asInstanceOf[OFActionOutput]
            val wildcard = (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK)
            val matchfield = OFFlowTable.createMatchField(flow = pendingflow, wcard = wildcard)
            val ilink = topoModule.reverseSelection(pktoutmsg.getInPort)
            val olink = topoModule.reverseSelection(outaction.getPort)
            if (outaction.getPort == OFPort.OFPP_FLOOD.getValue) {
              //flood the flow since the controller does not know the location of the destination
              logTrace("flood the flow " + pendingflow + " at " + node)
              pendingflow.floodflag = true
              routing(pendingflow, matchfield, ilink)
            } else {
              logTrace("forward the flow " + pendingflow + " through " + olink + " at node " + node)
              pendingflow.floodflag = false
              routing(pendingflow, matchfield, inlink = ilink)
            }
            log.trace("removing flow " + pendingflow + " from pending buffer " + " at node " + node)
            ofroutingModule.pendingFlows -= (pktoutmsg.getBufferId - 1)
          }
          case _ => throw new Exception("unrecognizable action")
        }
      }
    }
  }
}
