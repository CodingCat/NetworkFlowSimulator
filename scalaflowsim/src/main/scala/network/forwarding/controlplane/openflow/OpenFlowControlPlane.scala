package network.forwarding.controlplane.openflow

import network.device._
import network.forwarding.controlplane.DefaultControlPlane
import org.openflow.protocol._
import simengine.utils.XmlParser
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import java.net.InetSocketAddress
import org.jboss.netty.channel.Channel
import scala.collection.mutable
import org.slf4j.LoggerFactory
import org.openflow.protocol.factory.BasicFactory
import org.openflow.protocol.action.{OFActionType, OFAction, OFActionOutput}
import network.traffic.Flow
import network.forwarding.interface.OpenFlowPortManager
import scala.concurrent.Lock
import org.openflow.util.HexString
import scala.collection.JavaConverters._
import network.forwarding.controlplane.openflow.flowtable.OFFlowTable
import org.openflow.protocol.statistics._
import scala.collection.mutable.ListBuffer
import java.util
import packets.{Data, TCP, IPv4, Ethernet}

/**
 * this class implement the functions for routers to contact with
 * controller
 */
class OpenFlowControlPlane (node : Node) extends DefaultControlPlane(node) with MessageListener {

  private val controllerIP = XmlParser.getString("scalasim.network.controlplane.openflow.controller.host", "127.0.0.1")
  private val controllerPort = XmlParser.getInt("scalasim.network.controlplane.openflow.controller.port", 6633)

  private var lldpcnt = 0

  private [openflow] var toControllerChannel : Channel = null

  private [openflow] lazy val ofinterfacemanager = node.interfacesManager.asInstanceOf[OpenFlowPortManager]
  private [forwarding] val ofmsgsender = new OpenFlowMsgSender

  private var config_flags : Short = 0
  private var miss_send_len : Short = 1000

  private [controlplane] val flowtables = new Array[OFFlowTable](
    XmlParser.getInt("scalasim.openflow.flowtablenum", 1))

  //the flows waiting for PACKET_OUT
  private val pendingFlowLock = new Lock
  private [controlplane] val pendingFlows = new mutable.HashMap[Int, Flow]
    with mutable.SynchronizedMap[Int, Flow]


  private val logger = LoggerFactory.getLogger("OpenFlowModule")
  private val factory = new BasicFactory

  def getSwitchDescription = node.ip_addr(0)

  private def openflowInit() {
    for (i <- 0 until flowtables.length) flowtables(i) = new OFFlowTable(i.toShort, this)
  }

  private def generatePacketIn(bufferid : Int, inPortnum : Short, payload: Array[Byte],
                               reason: OFPacketIn.OFPacketInReason): OFPacketIn = {

    val packet_in_msg = factory.getMessage(OFType.PACKET_IN).asInstanceOf[OFPacketIn]
    packet_in_msg.setBufferId(bufferid)
      .setInPort(inPortnum)
      .setPacketData(payload)
      .setReason(OFPacketIn.OFPacketInReason.NO_MATCH)
      .setTotalLength(payload.length.toShort)
      .setLength((payload.length + 18).toShort)
    packet_in_msg
  }

  def sendPacketInToController(flow : Flow, inlink: Link, ethernetFramedata: Array[Byte]) {
    assert(ofinterfacemanager.linkphysicalportsMap.contains(inlink))
    val port = ofinterfacemanager.linkphysicalportsMap(inlink)
    pendingFlowLock.acquire()
    val bufferid = pendingFlows.size
    logger.trace("send PACKET_IN to controller for table missing at node " + node)
    logger.debug("buffering flow " + flow + " at buffer " + bufferid +
      " at node " + node)
    pendingFlows += (bufferid -> flow)
    pendingFlowLock.release()
    ofmsgsender.sendMessageToController(toControllerChannel,
      generatePacketIn(bufferid, port.getPortNumber, ethernetFramedata,
        OFPacketIn.OFPacketInReason.NO_MATCH))
  }

  def sendLLDPtoController (l : Link, lldpData : Array[Byte]) {
    val port = ofinterfacemanager.linkphysicalportsMap(l)
    //send out packet_in
    lldpcnt += 1
    ofmsgsender.sendMessageToController(toControllerChannel,
      generatePacketIn(-1, port.getPortNumber, lldpData, OFPacketIn.OFPacketInReason.ACTION))
  }

  private def replyLLDP (pktoutMsg : OFPacketOut) {
    //send out through all ports
    val outport = pktoutMsg.getActions.get(0).asInstanceOf[OFActionOutput].getPort
    val outlink = ofinterfacemanager.reverseSelection(outport)
    val neighbor = Link.otherEnd(outlink, node)
    val lldpdata = pktoutMsg.getPacketData
    //TODO: only support the situation that all routers are openflow-enabled
    if (neighbor.nodetype != HostType)
      neighbor.controlplane.asInstanceOf[OpenFlowControlPlane].sendLLDPtoController(outlink, lldpdata)
  }

  def topologyReady() : Boolean = {
    val expectedNumber = {
      val alllinks = ofinterfacemanager.outlinks.values.toList :::
        ofinterfacemanager.inlinks.values.toList
      alllinks.filter(l => Link.otherEnd(l, node).nodetype != HostType).length
    }
    lldpcnt >= expectedNumber
  }

  protected def inFlowRegistration(matchfield : OFMatchField, inlink : Link) {
    insertInPath(matchfield, inlink)
    //only valid in openflow model
    if (node.nodetype != HostType) {
      matchfield.setInputPort(ofinterfacemanager.getPortByLink(inlink).getPortNumber)
    }
  }

  //build channel to the controller
  def connectToController() {
    try {
      val clientfactory = new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool())
      val clientbootstrap = new ClientBootstrap(clientfactory)
      clientbootstrap.setPipelineFactory(new OpenFlowMsgPipelineFactory(this))
      clientbootstrap.connect(new InetSocketAddress(controllerIP, controllerPort))
    }
    catch {
      case e : Exception => e.printStackTrace
    }
  }

  def disconnectFormController() {
    if (toControllerChannel != null && toControllerChannel.isConnected)
      toControllerChannel.disconnect()
  }

  private def setParameter(f : Short, m : Short) {
    config_flags = f
    miss_send_len = m
  }

  lazy val DPID = {
    val impl_dependent = node.nodetype match {
      case ToRRouterType => "00"
      case AggregateRouterType => "01"
      case CoreRouterType => "02"
    }
    val t = node.ip_addr(0).substring(node.ip_addr(0).indexOf('.') + 1, node.ip_addr(0).size)
    val podid = HexString.toHexString(Integer.parseInt(t.substring(0, t.indexOf('.'))), 1)
    HexString.toLong(impl_dependent + ":" + podid + ":" + node.mac_addr(0))
  }

  private def getSwitchFeature (xid : Short) : OFFeaturesReply = {
    //TODO: specify the switch features
    //(dpid, buffer, n_tables, capabilities, physical port
    val featurereply = factory.getMessage(OFType.FEATURES_REPLY).asInstanceOf[OFFeaturesReply]
    val featurelist = (DPID, 1000, flowtables.length, 0xff, ofinterfacemanager.linkphysicalportsMap.values.toList)
    featurereply.setDatapathId(featurelist._1)
    featurereply.setBuffers(featurelist._2)
    featurereply.setTables(featurelist._3.toByte)
    featurereply.setCapabilities(featurelist._4)
    featurereply.setPorts(featurelist._5.asJava)
    featurereply.setLength((32 + featurereply.getPorts.size * OFPhysicalPort.MINIMUM_LENGTH).toShort)
    //TODO: only support output action for now
    featurereply.setActions(1) //only support output action for now
    featurereply.setXid(xid)
    featurereply
  }

  private def generateSwitchConfigReply(xid : Short, version : Byte): OFGetConfigReply = {
    val getconfigreply = factory.getMessage(OFType.GET_CONFIG_REPLY).asInstanceOf[OFGetConfigReply]
    getconfigreply.setFlags(config_flags)
    getconfigreply.setMissSendLength(miss_send_len)
    getconfigreply.setXid(xid)
    getconfigreply.setVersion(version)
    getconfigreply.setLength(12)
    getconfigreply
  }

  private def generateEchoReply(echoreq: OFEchoRequest): OFEchoReply = {
    val echoreply = factory.getMessage(OFType.ECHO_REPLY).asInstanceOf[OFEchoReply]
    val payloadlength = {
      if (echoreq.getPayload != null) echoreq.getPayload.length
      else 0
    }
    echoreply.setXid(echoreq.getXid)
    echoreply.setPayload(echoreq.getPayload)
    echoreply.setLength((OFMessage.MINIMUM_LENGTH + payloadlength).toShort)
    echoreply
  }

  private def processFlowMod(offlowmod: OFFlowMod) {
    offlowmod.getCommand match {
      case OFFlowMod.OFPFC_DELETE => {
        if (offlowmod.getMatch.getWildcards == OFMatch.OFPFW_ALL) {
          //clear to initialize matchfield tables;
          flowtables.foreach(table => table.clear)
        }
      }
      case OFFlowMod.OFPFC_ADD => {
        logger.trace("receive OFPFC_ADD:" + offlowmod.toString)
        //table(0) for openflow 1.0
        flowtables(0).addFlowTableEntry(offlowmod)
      }
      case _ => throw new Exception("unrecognized OFFlowMod command type:" + offlowmod.getCommand)
    }
  }

  /**
   * called by the openflowhandler
   * routing according to the pktoutmsg
   * @param pktoutmsg
   */
  def processOFPacketOut (pktoutmsg : OFPacketOut) {
    //-1 is reserved for lldp packets
    if (pktoutmsg.getBufferId == -1) {
      //the packet data is included in the packoutmsg
      replyLLDP(pktoutmsg)
    }
    else {
      //TODO: is there any difference if we are on packet-level simulation?
      log.trace("receive a packet_out to certain buffer:" + pktoutmsg.toString + " at " + node)
      val pendingflow = pendingFlows(pktoutmsg.getBufferId)
      for (action : OFAction <- pktoutmsg.getActions.asScala) {
        action.getType match {
          //only support output for now
          case OFActionType.OUTPUT => {
            val outaction = action.asInstanceOf[OFActionOutput]
            val wildcard = (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK)
            val matchfield = OFFlowTable.createMatchField(flow = pendingflow, wcard = wildcard)
            val ilink = ofinterfacemanager.reverseSelection(pktoutmsg.getInPort)
            val olink = ofinterfacemanager.reverseSelection(outaction.getPort)
            log.trace("removing flow " + pendingflow + " from pending buffer " + pktoutmsg.getBufferId +
              " at node " + node)
            pendingFlowLock.acquire()
            pendingFlows -= (pktoutmsg.getBufferId)
            pendingFlowLock.release()
            if (outaction.getPort == OFPort.OFPP_FLOOD.getValue) {
              //flood the flow since the controller does not know the location of the destination
              logTrace("flood the flow " + pendingflow + " at " + node)
              pendingflow.floodflag = false
              floodoutFlow(node, pendingflow, matchfield, ilink)
            } else {
              logTrace("forward the flow " + pendingflow + " through " + olink + " at node " + node)
              if (ilink != olink) {
                pendingflow.floodflag = false
                forward(node, olink, ilink, pendingflow, matchfield)
              } else {
                deleteEntry(matchfield)
              }
            }
          }
          case _ => throw new Exception("unrecognizable action")
        }
      }
    }
  }

  private def generateDataplaneDesc(ofstatreq : OFStatisticsRequest) = {
    val statdescreply = factory.getStatistics(OFType.STATS_REPLY,OFStatisticsType.DESC)
      .asInstanceOf[OFDescriptionStatistics]
    val statreply = factory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
    //TODO: descriptions are not complete
    val statlist = new ListBuffer[OFStatistics]
    statdescreply.setDatapathDescription(getSwitchDescription)
    statdescreply.setHardwareDescription(node.mac_addr(0))
    statdescreply.setManufacturerDescription("simulated router")
    statdescreply.setSoftwareDescription("simulated router software")
    statdescreply.setSerialNumber("1")
    statlist += statdescreply
    statreply.setStatisticType(OFStatisticsType.DESC)
    statreply.setStatistics(statlist.toList.asJava)
    statreply.setStatisticsFactory(factory)
    statreply.setLength((statdescreply.getLength+ statreply.getLength).toShort)
    statreply.setXid(ofstatreq.getXid)
    statreply
  }

  private def queryAllTablesByFlowStatRequest(flowstatreq : OFFlowStatisticsRequest) = {
    val statreplylist = new ListBuffer[OFStatistics]
    //query all tables
    for (flowtable <- flowtables) {
      flowtable.queryByFlowStatRequest(flowstatreq).foreach(
        flowstatreply => statreplylist += flowstatreply)
    }
    statreplylist
  }

  private def processFlowStatisticsQuery (ofstatrequest: OFStatisticsRequest) = {
    val ofstatreply = factory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
    val offlowstatreq = ofstatrequest.getStatistics.asScala
    val statreplylist = new ListBuffer[OFStatistics]
    for (statreq <- offlowstatreq) {
      val flowstatreq = statreq.asInstanceOf[OFFlowStatisticsRequest]
      if (flowstatreq.getTableId == -1) {
        queryAllTablesByFlowStatRequest(flowstatreq).foreach(flowstatreply => statreplylist += flowstatreply)
      } else {
        //query a single table
        flowtables(flowstatreq.getTableId).queryByFlowStatRequest(flowstatreq).foreach(
          flowstatreply => statreplylist += flowstatreply)
      }
    }
    //resemble the ofstatreply
    ofstatreply.setStatistics(statreplylist.toList.asJava)
    ofstatreply.setStatisticType(OFStatisticsType.FLOW)
    ofstatreply.setStatisticsFactory(factory)
    //calculate the message length
    var l = 0
    statreplylist.foreach(statreply => l += statreply.getLength)
    ofstatreply.setLength((l + ofstatreply.getLength).toShort)
    ofstatreply.setXid(ofstatrequest.getXid)
    ofmsgsender.pushInToBuffer(ofstatreply)
  }

  private def processAggregateStatisticsQuery (ofstatrequest: OFStatisticsRequest) = {
    val ofstatreply = factory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
    val offlowstatreq = ofstatrequest.getStatistics.asScala
    val statreplylist = new ListBuffer[OFStatistics]
    val stataggreply = factory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.AGGREGATE)
      .asInstanceOf[OFAggregateStatisticsReply]
    for (statreq <- offlowstatreq) {
      val flowstatreq = statreq.asInstanceOf[OFAggregateStatisticsRequest]
      if (flowstatreq.getTableId == -1) {
        for (i <- 0 until flowtables.length) {
          val referred_table = flowtables(i)
          stataggreply.setFlowCount(stataggreply.getFlowCount + referred_table.counters.referencecount)
          stataggreply.setPacketCount(stataggreply.getPacketCount + referred_table.counters.packetlookup +
            referred_table.counters.packetmatches)
          stataggreply.setByteCount(stataggreply.getByteCount + referred_table.counters.flowbytes)
        }
      } else {
        val referred_table = flowtables(flowstatreq.getTableId)
        stataggreply.setFlowCount(referred_table.counters.referencecount)
        stataggreply.setPacketCount(referred_table.counters.packetlookup +
          referred_table.counters.packetmatches)
        stataggreply.setByteCount(referred_table.counters.flowbytes)
      }
    }
    //resemble the ofstatreply
    statreplylist += stataggreply
    ofstatreply.setStatistics(statreplylist.toList.asJava)
    ofstatreply.setStatisticType(OFStatisticsType.AGGREGATE)
    ofstatreply.setStatisticsFactory(factory)
    //calculate the message length
    ofstatreply.setLength((stataggreply.getLength + ofstatreply.getLength).toShort)
    ofstatreply.setXid(ofstatrequest.getXid)
    ofmsgsender.pushInToBuffer(ofstatreply)
  }

  private def generateHelloMsg(hellomsg : OFHello) = {
    val helloreply = factory.getMessage(OFType.HELLO).asInstanceOf[OFHello]
    helloreply.setLength(8)
    helloreply.setType(OFType.HELLO)
    helloreply.setXid(hellomsg.getXid)
    helloreply.setVersion(hellomsg.getVersion)
    helloreply
  }

  override def handleMessage(msg: OFMessage) {
    msg.getType match {
      case OFType.SET_CONFIG => {
        logger.trace(node + " received a set_config message")
        val setconfigmsg = msg.asInstanceOf[OFSetConfig]
        setParameter(setconfigmsg.getFlags, setconfigmsg.getMissSendLength)
      }
      case OFType.HELLO => {
        logger.trace(node + " received a hello message")
        ofmsgsender.pushInToBuffer(generateHelloMsg(msg.asInstanceOf[OFHello]))
      }
      case OFType.FEATURES_REQUEST => {
        val featurereply = getSwitchFeature(msg.getXid.toShort)
        ofmsgsender.pushInToBuffer(featurereply)
      }
      case OFType.GET_CONFIG_REQUEST => {
        ofmsgsender.pushInToBuffer(
          generateSwitchConfigReply(msg.getXid.toShort, msg.getVersion))
      }
      case OFType.FLOW_MOD => processFlowMod(msg.asInstanceOf[OFFlowMod])
      case OFType.PACKET_OUT => processOFPacketOut(msg.asInstanceOf[OFPacketOut])
      case OFType.ECHO_REQUEST => ofmsgsender.pushInToBuffer(
        generateEchoReply(msg.asInstanceOf[OFEchoRequest]))
      case OFType.STATS_REQUEST => {
        val ofstatrequest = msg.asInstanceOf[OFStatisticsRequest]
        ofstatrequest.getStatisticType match {
          case OFStatisticsType.DESC => {
            logger.trace(node +  " received a desc stat request")
            ofmsgsender.pushInToBuffer(generateDataplaneDesc(ofstatrequest))
          }
          case OFStatisticsType.FLOW => processFlowStatisticsQuery(ofstatrequest)
          case OFStatisticsType.AGGREGATE => processAggregateStatisticsQuery(ofstatrequest)
          case _ => {}
        }
      }
      case _ => {}
    }
  }

  //abstract methods
  override def selectNextHop(flow: Flow, matchfield: OFMatchField, inPort: Link): Link = {
    if (!RIBOut.contains(matchfield)) {
      //send packet_in to controller
      logDebug("miss the matchfield:" + matchfield.toString)
      val dummypayload = new Array[Byte](1)
      dummypayload(0) = (0).toByte
      val ethernetFrame = new Ethernet
      ethernetFrame.setEtherType(Ethernet.TYPE_IPv4)
        .setSourceMACAddress(matchfield.getDataLayerSource)
        .setDestinationMACAddress(matchfield.getDataLayerDestination)
        .setPriorityCode(0)
        .setPad(true)
        .setVlanID(0)
        .setPayload(new IPv4()
        .setSourceAddress(matchfield.getNetworkSource)
        .setDestinationAddress(matchfield.getNetworkDestination)
        .setVersion(4)
        .setPayload(new TCP()
        .setSourcePort(matchfield.getTransportSource)
        .setDestinationPort(matchfield.getTransportDestination)
        .setPayload(new Data(dummypayload))))
      val serializedData = ethernetFrame.serialize
      sendPacketInToController(flow , inPort, serializedData)
      null
    } else {
      //openflow 1.0
      logDebug("hit RIBOut with " + matchfield.toString)
      //assume return only one result
      for (entryattach <- flowtables(0).matchFlow(matchfield)) {
        //TODO: support other actions
        entryattach.actions.foreach(action =>
          if (action.isInstanceOf[OFActionOutput]) return RIBOut(matchfield))
      }
      null
    }
  }

  openflowInit()
}