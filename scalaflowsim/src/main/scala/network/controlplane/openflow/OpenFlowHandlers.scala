package scalasim.network.controlplane.openflow

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.openflow.protocol._
import scala.collection.JavaConversions._
import scalasim.network.component.Router
import scalasim.simengine.SimulationEngine
import scalasim.simengine.utils.Logging
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.openflow.protocol.statistics._
import java.util
import scalasim.XmlParser
import org.openflow.protocol.factory.BasicFactory

class OpenFlowMsgEncoder extends OneToOneEncoder {

  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
    if (!msg.isInstanceOf[java.util.ArrayList[_]]) return msg
    val msglist = msg.asInstanceOf[java.util.ArrayList[OFMessage]]
    var size: Int = 0
    for (ofm <- msglist) {
      size += ofm.getLength
    }
    val buf: ChannelBuffer = ChannelBuffers.buffer(size)
    for (ofm <- msglist) {
      ofm.writeTo(buf)
    }
    return buf
  }
}

class OpenFlowMsgDecoder extends FrameDecoder {
  val factory = new BasicFactory
  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer)  : AnyRef = {
    //parse the channelbuffer into the list of ofmessage
    if (!channel.isConnected) return None
    factory.parseMessage(buffer)
  }
}

class OpenFlowChannelHandler (private val openflowPlane : OpenFlowModule)
  extends SimpleChannelHandler  with Logging  {

  private val factory =  new BasicFactory

  private def processFlowMod(offlowmod : OFFlowMod) {
    offlowmod.getCommand match {
      case OFFlowMod.OFPFC_DELETE => {
        if (offlowmod.getMatch.getWildcards == OFMatch.OFPFW_ALL) {
          //clear to initialize flow tables;
          for (table <- openflowPlane.flowtables) table.clear()
        }
      }
      case _ => throw new Exception("unrecognized OFFlowMod command type:" + offlowmod.getCommand)
    }
  }

  private def processStatRequest(ofstatreq : OFStatisticsRequest, outlist : java.util.ArrayList[OFMessage]) {
    ofstatreq.getStatisticType match {
      case OFStatisticsType.DESC => {
        val statdescreply = factory.getStatistics(OFType.STATS_REPLY,OFStatisticsType.DESC)
          .asInstanceOf[OFDescriptionStatistics]
        //TODO: descriptions are not complete
        statdescreply.setDatapathDescription(openflowPlane.getSwitchDescription)
        statdescreply.setHardwareDescription("simulated router hardware")
        statdescreply.setManufacturerDescription("simulated router")
        statdescreply.setSoftwareDescription("simulated router software")
        statdescreply.setSerialNumber("1")
        val statList = new util.ArrayList[OFStatistics]
        statList += statdescreply
        val statreply = factory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
        statreply.setStatisticType(OFStatisticsType.DESC)
        statreply.setStatistics(statList)
        statreply.setStatisticsFactory(factory)
        statreply.setLength((statdescreply.getLength+ statreply.getLength).toShort)
        statreply.setXid(ofstatreq.getXid)
        outlist += statreply
      }
      case OFStatisticsType.FLOW =>{
        val statflowreqmsg = ofstatreq.asInstanceOf[OFStatisticsRequest]
        val statflowreq = statflowreqmsg.getStatistics.get(0).asInstanceOf[OFFlowStatisticsRequest]
        val statflowreplymsg = factory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
        val statlist = new util.ArrayList[OFStatistics]
        if (statflowreq.getTableId == -1) {
          //read from all tables
          logTrace("collect flow information from all tables")
          for (i <- 0 until openflowPlane.flowtables.length) {
            logTrace("collect flow information on table " + i + " with match wildcard " +
              statflowreq.getMatch.getWildcards + " and outputport " +  + statflowreq.getOutPort)
            val qualifiedflows = openflowPlane.flowtables(i).getFlowsByMatchAndPort(statflowreq.getMatch,
              statflowreq.getOutPort)
            logTrace("qualified flow number: " + qualifiedflows.length)
            for (flowentry <- qualifiedflows) {
              val statflowreply = factory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.FLOW)
                .asInstanceOf[OFFlowStatisticsReply]
              statflowreply.setLength(88)
              statflowreply.setMatch(statflowreq.getMatch)
              statflowreply.setTableId(i.toByte)
              statflowreply.setDurationNanoseconds(flowentry.counter.durationNanoSeconds)
              statflowreply.setDurationSeconds(flowentry.counter.durationSeconds)
              statflowreply.setPriority(0)
              statflowreply.setIdleTimeout((openflowPlane.flowtables(i).flowIdleDuration -
                SimulationEngine.currentTime + flowentry.lastAccessPoint).toShort)
              statflowreply.setHardTimeout((flowentry.expireMoments - SimulationEngine.currentTime).toShort)
              statflowreply.setCookie(0)
              statflowreply.setPacketCount(flowentry.counter.receivedpacket)
              statflowreply.setByteCount(flowentry.counter.receivedbytes)
              logTrace("add flow reply: " + statflowreply)
              statlist += statflowreply
            }
          }
        }
        else {
          val referredtable = openflowPlane.flowtables(statflowreq.getTableId)
          val qualifiedflows = referredtable.getFlowsByMatchAndPort(statflowreq.getMatch,
            statflowreq.getOutPort)
          for (flowentry <- qualifiedflows) {
            val statflowreply = factory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.FLOW)
              .asInstanceOf[OFFlowStatisticsReply]
            statflowreply.setLength(88)
            statflowreply.setMatch(statflowreq.getMatch)
            statflowreply.setTableId(statflowreq.getTableId)
            statflowreply.setDurationNanoseconds(flowentry.counter.durationNanoSeconds)
            statflowreply.setDurationSeconds(flowentry.counter.durationSeconds)
            statflowreply.setPriority(0)
            statflowreply.setIdleTimeout((referredtable.flowIdleDuration - SimulationEngine.currentTime +
              flowentry.lastAccessPoint).toShort)
            statflowreply.setHardTimeout((flowentry.expireMoments - SimulationEngine.currentTime).toShort)
            statflowreply.setCookie(0)
            statflowreply.setPacketCount(flowentry.counter.receivedpacket)
            statflowreply.setByteCount(flowentry.counter.receivedbytes)
            statflowreply.setActions(flowentry.actions)
            statlist += statflowreply
          }
        }
        statflowreplymsg.setLength((statflowreplymsg.getLength + statlist.length * 88).toShort)
        statflowreplymsg.setStatisticType(OFStatisticsType.FLOW)
        statflowreplymsg.setStatistics(statlist)
        statflowreplymsg.setStatisticsFactory(factory)
        statflowreplymsg.setXid(ofstatreq.getXid)
        outlist += statflowreplymsg
      }
      case OFStatisticsType.PORT => {
        val statportreqmsg = ofstatreq.asInstanceOf[OFStatisticsRequest]
        val statportreq = statportreqmsg.getStatistics.get(0).asInstanceOf[OFPortStatisticsRequest]
        val statportreply = factory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.PORT)
          .asInstanceOf[OFPortStatisticsReply]
        val statreply = factory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
        val statList = new util.ArrayList[OFStatistics]
        val port_num = statportreq.getPortNumber
        if (port_num == -1) {
          val counters = openflowPlane.topoModule.portcounters
          for (counter_pair <- counters) {
            val counter = counter_pair._2
            statportreply.setPortNumber(counter_pair._1)
            statportreply.setreceivePackets(counter.receivedpacket)
            statportreply.setTransmitPackets(counter.transmittedpacket)
            statportreply.setReceiveBytes(counter.receivedbytes)
            statportreply.setTransmitBytes(counter.transmittedbytes)
            statportreply.setReceiveDropped(counter.receivedrops)
            statportreply.setTransmitDropped(counter.transmitdrops)
            statportreply.setreceiveErrors(counter.receiveerror)
            statportreply.setTransmitErrors(counter.transmiterror)
            statportreply.setReceiveFrameErrors(counter.receiveframe_align_error)
            statportreply.setReceiveOverrunErrors(counter.receive_overrun_error)
            statportreply.setReceiveCRCErrors(counter.receive_crc_error)
            statportreply.setCollisions(counter.collisions)
            statList += statportreply
          }
        }
        else {
          val counter = openflowPlane.topoModule.portcounters(port_num)
          statportreply.setPortNumber(port_num)
          statportreply.setreceivePackets(counter.receivedpacket)
          statportreply.setTransmitPackets(counter.transmittedpacket)
          statportreply.setReceiveBytes(counter.receivedbytes)
          statportreply.setTransmitBytes(counter.transmittedbytes)
          statportreply.setReceiveDropped(counter.receivedrops)
          statportreply.setTransmitDropped(counter.transmitdrops)
          statportreply.setreceiveErrors(counter.receiveerror)
          statportreply.setTransmitErrors(counter.transmiterror)
          statportreply.setReceiveFrameErrors(counter.receiveframe_align_error)
          statportreply.setReceiveOverrunErrors(counter.receive_overrun_error)
          statportreply.setReceiveCRCErrors(counter.receive_crc_error)
          statportreply.setCollisions(counter.collisions)
          statList += statportreply
        }
        statreply.setStatisticType(OFStatisticsType.PORT)
        statreply.setStatistics(statList)
        statreply.setStatisticsFactory(factory)
        statreply.setLength((statreply.getLength + statportreply.getLength * statList.size).toShort)
        statreply.setXid(ofstatreq.getXid)
        outlist += statreply
      }

      case OFStatisticsType.AGGREGATE => {
        val stataggreqmsg = ofstatreq.asInstanceOf[OFStatisticsRequest]
        val stataggreq = stataggreqmsg.getStatistics.get(0).asInstanceOf[OFAggregateStatisticsRequest]
        val stataggreply = factory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.AGGREGATE)
          .asInstanceOf[OFAggregateStatisticsReply]
        val statreply = factory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
        val table_id = stataggreq.getTableId
        if (table_id >= 0 && table_id < openflowPlane.flowtables.length) {
          val referred_table = openflowPlane.flowtables(table_id)
          stataggreply.setFlowCount(referred_table.counters.referencecount)
          stataggreply.setPacketCount(referred_table.counters.packetlookup +
            referred_table.counters.packetmatches)
          stataggreply.setByteCount(referred_table.counters.flowbytes)
        }
        else {
          for (i <- 0 until openflowPlane.flowtables.length) {
            val referred_table = openflowPlane.flowtables(i)
            stataggreply.setFlowCount(referred_table.counters.referencecount)
            stataggreply.setPacketCount(referred_table.counters.packetlookup +
              referred_table.counters.packetmatches)
            stataggreply.setByteCount(referred_table.counters.flowbytes)
          }
        }
        val statList = new util.ArrayList[OFStatistics]
        statList += stataggreply
        statreply.setStatisticType(OFStatisticsType.AGGREGATE)
        statreply.setStatistics(statList)
        statreply.setStatisticsFactory(factory)
        statreply.setLength((stataggreply.getLength + statreply.getLength).toShort)
        statreply.setXid(ofstatreq.getXid)
        outlist += statreply
      }
      case _ => throw new Exception("unrecognized OFStatisticRequest: " + ofstatreq.getStatisticType)
    }
  }

  private def processMessage(ofm : OFMessage, outlist : java.util.ArrayList[OFMessage]) = ofm.getType match {
    case OFType.HELLO => {
      logTrace("receive a hello message from controller")
      val helloreply = factory.getMessage(OFType.HELLO).asInstanceOf[OFHello]
      helloreply.setLength(8)
      helloreply.setType(OFType.HELLO)
      helloreply.setXid(ofm.getXid)
      helloreply.setVersion(ofm.getVersion)
      outlist += helloreply
    }
    case OFType.FEATURES_REQUEST => {
      logTrace("receive a feature request message from controller")
      val featurereply = factory.getMessage(OFType.FEATURES_REPLY).asInstanceOf[OFFeaturesReply]
      val featurelist = openflowPlane.getSwitchFeature
      featurereply.setDatapathId(featurelist._1)
      featurereply.setBuffers(featurelist._2)
      featurereply.setTables(featurelist._3.toByte)
      featurereply.setCapabilities(featurelist._4)
      featurereply.setPorts(featurelist._5)
      featurereply.setLength((32 + featurereply.getPorts.length * 48).toShort)
      featurereply.setXid(ofm.getXid)
      outlist += featurereply
    }
    case OFType.SET_CONFIG => {
      val m = ofm.asInstanceOf[OFSetConfig]
      logTrace("receive a set config message from controller, miss_send_length:" + m.getMissSendLength +
        " , flags:" + m.getFlags)
      openflowPlane.setSwitchParameters(ofm.asInstanceOf[OFSetConfig])
    }
    case OFType.GET_CONFIG_REQUEST => {
      logTrace("receive a get config request from controller")
      val getconfigreply = factory.getMessage(OFType.GET_CONFIG_REPLY).asInstanceOf[OFGetConfigReply]
      val config = openflowPlane.getSwitchParameters
      getconfigreply.setFlags(config._1)
      getconfigreply.setMissSendLength(config._2)
      getconfigreply.setXid(ofm.getXid)
      outlist += getconfigreply
    }
    case OFType.STATS_REQUEST => {
      logTrace("receive a stat request message from controller")
      processStatRequest(ofm.asInstanceOf[OFStatisticsRequest], outlist)
      if (openflowPlane.status == OpenFlowSwitchHandShaking)
        openflowPlane.status = OpenFlowSwitchRunning
    }
    case OFType.FLOW_MOD => {
      logTrace("receive a flow_mod message from controller")
      processFlowMod(ofm.asInstanceOf[OFFlowMod])
    }
    case OFType.ECHO_REQUEST => {
      logTrace("receive a echo_request message from controller")
      val echoreq = ofm.asInstanceOf[OFEchoRequest]
      val echoreply = factory.getMessage(OFType.ECHO_REPLY).asInstanceOf[OFEchoReply]
      echoreply.setXid(echoreq.getXid)
      echoreply.setPayload(echoreq.getPayload)
      outlist += echoreply
    }
    case OFType.PACKET_OUT => {
      logTrace("receive a packet_out message from controller")
      val packetoutmsg = ofm.asInstanceOf[OFPacketOut]
      if (packetoutmsg.getBufferId == -1) {
        //the packet data is included in the packoutmsg
        val outData = packetoutmsg.getPacketData
        logTrace("receive LLDP packet from the controller with the size " + outData.length)
        val topoManager = openflowPlane.topoModule
        //send out through all ports
        val allports = topoManager.outlink.values.toList ::: topoManager.inlinks.values.toList
        for (port <- allports) {
          //send back PACKET_IN to controller
          val packet_in_msg = factory.getMessage(OFType.PACKET_IN).asInstanceOf[OFPacketIn]
          packet_in_msg.setBufferId(-1)
            .setInPort(topoManager.physicalports(port).getPortNumber)
            .setPacketData(outData)
            .setReason(OFPacketIn.OFPacketInReason.ACTION)
            .setTotalLength(outData.length.toShort)
          outlist += packet_in_msg
          //make Neightbour send PACKET_IN with the same data
          val neighbour = topoManager.getNeighbour(port)
          if (neighbour.isInstanceOf[Router]) {
            val neighbour_router = neighbour.asInstanceOf[Router]
            //TODO: this line may not work when the neighbour code is not a openflow switch
            neighbour_router.controlPlane.asInstanceOf[OpenFlowModule].sendLLDPtoController(port, outData)
          }
        }
      }
      else {
        //TODO:packet_out specifies a certain buffer or entry
        if (XmlParser.getString("scalasim.simengine.simlevel", "flow") == "flow") {

        }
        else {

        }
      }
    }
    case _ => throw new Exception("unrecognized message type:" + ofm)
  }

  override def channelConnected(ctx : ChannelHandlerContext, e : ChannelStateEvent) {
    //save the channel for sending packet
    openflowPlane.toControllerChannel = e.getChannel
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    if (!e.getMessage.isInstanceOf[java.util.ArrayList[_]]) return
    val msglist = e.getMessage.asInstanceOf[java.util.ArrayList[OFMessage]]
    val outmsglist = new java.util.ArrayList[OFMessage]
    for (ofm: OFMessage <- msglist) {
      try {
        processMessage(ofm, outmsglist)
      }
      catch {
        case ex: Exception => Channels.fireExceptionCaught(ctx.getChannel, ex)
      }
    }
    //send out all messages
    e.getChannel.write(outmsglist)
    outmsglist.clear
  }

  override def exceptionCaught (ctx : ChannelHandlerContext, e : ExceptionEvent) {
    e.getCause.printStackTrace()
  }
}