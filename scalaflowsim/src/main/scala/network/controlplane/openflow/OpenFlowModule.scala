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
import org.openflow.protocol.factory.BasicFactory
import org.jboss.netty.channel.Channel
import org.slf4j.LoggerFactory
import org.openflow.protocol.action.{OFActionOutput, OFActionType, OFAction}
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import network.controlplane.openflow.flowtable.OFMatchField

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

  private var lldpcnt = 0

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

  def disconnectFormController() {
    if (toControllerChannel != null && toControllerChannel.isConnected)
      toControllerChannel.disconnect()
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
    val podid = HexString.toHexString(Integer.parseInt(t.substring(0, t.indexOf('.'))), 1)
    HexString.toLong(impl_dependent + ":" + podid + ":" + node.mac_addr(0))
  }


  def getSwitchFeature = {
    //TODO: specify the switch features
    //(dpid, buffer, n_tables, capabilities, physical port
    (getDPID, 1000, ofroutingModule.flowtables.length, 0xff,
      router.controlPlane.topoModule.linkphysicalportsMap.values.toList)
  }

  def setSwitchParameters(config_flag : Short, miss_send_length : Short) {
    config_flags = config_flag
    miss_send_len = miss_send_length
  }

  def getSwitchParameters() : (Short, Short) = {
    (config_flags, miss_send_len)
  }

  def getSwitchDescription = router.ip_addr(0)

  private def sendMessageToController(message : OFMessage) {
    msgPendingBuffer += message
    if (toControllerChannel != null && toControllerChannel.isConnected) {
      toControllerChannel.write(msgPendingBuffer)
      msgPendingBuffer.clear()
    }
  }

  def sendPacketInToController(flow : Flow, inlink: Link, ethernetFramedata: Array[Byte]) {
    val port = topoModule.linkphysicalportsMap(inlink)
    val packet_in_msg = factory.getMessage(OFType.PACKET_IN).asInstanceOf[OFPacketIn]
    var bufferid = 0
    ofroutingModule.bufferLock.acquire()
    bufferid = ofroutingModule.pendingFlows.size
    logger.trace("send PACKET_IN to controller for table missing at node " + node)
    logger.debug("buffering flow " + flow + " at buffer " + bufferid +
        " at node " + node)
    ofroutingModule.pendingFlows += (bufferid -> flow)
    ofroutingModule.bufferLock.release()
    packet_in_msg.setBufferId(bufferid)
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
    lldpcnt = lldpcnt + 1
    packet_in_msg.setBufferId(-1)
      .setInPort(port.getPortNumber)
      .setPacketData(lldpData)
      .setReason(OFPacketIn.OFPacketInReason.ACTION)
      .setTotalLength(lldpData.length.toShort)
      .setLength((lldpData.length + 18).toShort)
    sendMessageToController(packet_in_msg)
  }

  def topologyHasbeenRecognized() : Boolean = {
    def getLLDPNeighbourNumber : Int = {
      var ret = 0
      val alllink = {
        if (node.nodetype != HostType)
          node.controlPlane.topoModule.inlinks.values.toList :::
            node.controlPlane.topoModule.outlink.values.toList
        else node.controlPlane.topoModule.outlink.values.toList
      }
      alllink.foreach(l => if (Link.otherEnd(l, node).nodetype != HostType) ret += 1)
      ret
    }
    lldpcnt >= getLLDPNeighbourNumber
  }

  private def replyLLDP (pktoutMsg : OFPacketOut) {
    //send out through all ports
    val outport = pktoutMsg.getActions.get(0).asInstanceOf[OFActionOutput].getPort
    val outlink = topoModule.reverseSelection(outport)
    val neighbor = Link.otherEnd(outlink, node)
    val lldpdata = pktoutMsg.getPacketData
    if (neighbor.nodetype != HostType)
      neighbor.controlPlane.asInstanceOf[OpenFlowModule].sendLLDPtoController(outlink, lldpdata)
  }

  override protected def inFlowRegistration(matchfield : OFMatchField, inlink : Link) {
    routingModule.insertInPath(matchfield, inlink)
    //only valid in openflow model
    if (node.nodetype != HostType) {
      matchfield.setInputPort(topoModule.getPortByLink(inlink).getPortNumber)
    }
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
      log.trace("receive a packet_out to certain buffer:" + pktoutmsg.toString + " at " + node)
      val pendingflow = ofroutingModule.pendingFlows(pktoutmsg.getBufferId)
      for (action : OFAction <- pktoutmsg.getActions.asScala) {
        action.getType match {
          //only support output for now
          case OFActionType.OUTPUT => {
            val outaction = action.asInstanceOf[OFActionOutput]
            val wildcard = (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK)
            val matchfield = OFFlowTable.createMatchField(flow = pendingflow, wcard = wildcard)
            val ilink = topoModule.reverseSelection(pktoutmsg.getInPort)
            val olink = topoModule.reverseSelection(outaction.getPort)
            log.trace("removing flow " + pendingflow + " from pending buffer " + pktoutmsg.getBufferId +
              " at node " + node)
            ofroutingModule.bufferLock.acquire()
            ofroutingModule.pendingFlows -= (pktoutmsg.getBufferId)
            ofroutingModule.bufferLock.release()
            if (outaction.getPort == OFPort.OFPP_FLOOD.getValue) {
              //flood the flow since the controller does not know the location of the destination
              logTrace("flood the flow " + pendingflow + " at " + node)
              pendingflow.floodflag = false
              floodoutFlow(pendingflow, matchfield, ilink)
            } else {
              logTrace("forward the flow " + pendingflow + " through " + olink + " at node " + node)
              if (ilink != olink) {
                pendingflow.floodflag = false
                forward(olink, ilink, pendingflow, matchfield)
              } else {
                routingModule.deleteEntry(matchfield)
              }
            }
          }
          case _ => throw new Exception("unrecognizable action")
        }
      }
    }
  }
}
