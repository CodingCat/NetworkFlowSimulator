package scalasim.network.controlplane.openflow

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.openflow.protocol._
import scala.collection.JavaConversions._
import scalasim.simengine.SimulationEngine
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.openflow.protocol.statistics._
import java.util
import org.openflow.protocol.factory.BasicFactory
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer

class OpenFlowMsgEncoder extends OneToOneEncoder {

  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
    if (!msg.isInstanceOf[ArrayBuffer[_]]) return msg
    val msglist = msg.asInstanceOf[ArrayBuffer[OFMessage]]
    var size: Int = 0
    msglist.foreach(ofm => size += ofm.getLength)
    val buf = ChannelBuffers.buffer(size)
    msglist.foreach(ofmessage => ofmessage.writeTo(buf))
    msglist.clear()
    buf
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
  extends SimpleChannelHandler {

  private val factory =  new BasicFactory

  private val logger = LoggerFactory.getLogger("OpenFlowHandler")

  private def processFlowMod(offlowmod: OFFlowMod) {
    offlowmod.getCommand match {
      case OFFlowMod.OFPFC_DELETE => {
        if (offlowmod.getMatch.getWildcards == OFMatch.OFPFW_ALL) {
          //clear to initialize matchfield tables;
          for (table <- openflowPlane.ofroutingModule.flowtables) table.clear
        }
      }
      case OFFlowMod.OFPFC_ADD => {
        logger.trace("receive OFPFC_ADD:" + offlowmod.toString)
        openflowPlane.ofroutingModule.addNewFlowEntry(offlowmod)
      }
      case _ => throw new Exception("unrecognized OFFlowMod command type:" + offlowmod.getCommand)
    }
  }

  private def processStatRequest(ofstatreq : OFStatisticsRequest) {
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
        openflowPlane.ioBatchBuffer += statreply
      }
      case OFStatisticsType.FLOW =>{
        val statflowreqmsg = ofstatreq.asInstanceOf[OFStatisticsRequest]
        val statflowreq = statflowreqmsg.getStatistics.get(0).asInstanceOf[OFFlowStatisticsRequest]
        val statflowreplymsg = factory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
        val statlist = new util.ArrayList[OFStatistics]
        var statreplieslength = 0
        if (statflowreq.getTableId == -1) {
          //read from all tables
          logger.trace("collect matchfield information from all tables")
          for (i <- 0 until openflowPlane.ofroutingModule.flowtables.length) {
            logger.trace("collect matchfield information on table " + i + " with match wildcard " +
              statflowreq.getMatch.getWildcards + " and outputport " +  + statflowreq.getOutPort)
            val qualifiedflows = openflowPlane.ofroutingModule.flowtables(i).
              queryTableByMatchAndOutport(statflowreq.getMatch, statflowreq.getOutPort)
            logger.trace("qualified matchfield number: " + qualifiedflows.length)
            for (flowentry <- qualifiedflows) {
              val statflowreply = factory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.FLOW)
                .asInstanceOf[OFFlowStatisticsReply]
              var actionlistlength = 0
              flowentry.actions.foreach(action => actionlistlength += action.getLength)
              statflowreply.setMatch(statflowreq.getMatch)
              statflowreply.setTableId(i.toByte)
              statflowreply.setDurationNanoseconds(flowentry.counter.durationNanoSeconds)
              statflowreply.setDurationSeconds(flowentry.counter.durationSeconds)
              statflowreply.setPriority(0)
              statflowreply.setIdleTimeout((flowentry.flowIdleDuration -
                (SimulationEngine.currentTime - flowentry.getLastAccessPoint)).toShort)
              statflowreply.setHardTimeout((flowentry.flowHardExpireMoment - SimulationEngine.currentTime).toShort)
              statflowreply.setCookie(0)
              statflowreply.setPacketCount(flowentry.counter.receivedpacket)
              statflowreply.setByteCount(flowentry.counter.receivedbytes)
              statflowreply.setActions(flowentry.actions)
              statflowreply.setLength((88 + actionlistlength).toShort)
              statreplieslength += statflowreply.getLength
              logger.trace("add matchfield reply: " + statflowreply)
              statlist += statflowreply
            }
          }
        } else {
          val referredtable = openflowPlane.ofroutingModule.flowtables(statflowreq.getTableId)
          val qualifiedflows = referredtable.queryTableByMatchAndOutport(statflowreq.getMatch,
            statflowreq.getOutPort)
          for (flowentry <- qualifiedflows) {
            val statflowreply = factory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.FLOW)
              .asInstanceOf[OFFlowStatisticsReply]
            var actionlistlength = 0
            flowentry.actions.foreach(action => actionlistlength += action.getLength)
            statflowreply.setLength((88 + actionlistlength).toShort)
            statflowreply.setMatch(statflowreq.getMatch)
            statflowreply.setTableId(statflowreq.getTableId)
            statflowreply.setDurationNanoseconds(flowentry.counter.durationNanoSeconds)
            statflowreply.setDurationSeconds(flowentry.counter.durationSeconds)
            statflowreply.setPriority(0)
            statflowreply.setIdleTimeout((flowentry.flowIdleDuration -
              (SimulationEngine.currentTime - flowentry.getLastAccessPoint)).toShort)
            statflowreply.setHardTimeout((flowentry.flowHardExpireMoment - SimulationEngine.currentTime).toShort)
            statflowreply.setCookie(0)
            statflowreply.setPacketCount(flowentry.counter.receivedpacket)
            statflowreply.setByteCount(flowentry.counter.receivedbytes)
            statflowreply.setActions(flowentry.actions)
            statreplieslength += statflowreply.getLength
            statlist += statflowreply
          }
        }
        statflowreplymsg.setLength((statflowreplymsg.getLength + statreplieslength).toShort)
        statflowreplymsg.setStatisticType(OFStatisticsType.FLOW)
        statflowreplymsg.setStatistics(statlist)
        statflowreplymsg.setStatisticsFactory(factory)
        statflowreplymsg.setXid(ofstatreq.getXid)
        openflowPlane.ioBatchBuffer += statflowreplymsg
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
            statportreply.setCollisions(counter.collisions)
            statportreply.setPortNumber(counter_pair._1)
            statportreply.setReceiveBytes(counter.receivedbytes)
            statportreply.setreceivePackets(counter.receivedpacket)
            statportreply.setReceiveDropped(counter.receivedrops)
            statportreply.setreceiveErrors(counter.receiveerror)
            statportreply.setReceiveFrameErrors(counter.receiveframe_align_error)
            statportreply.setReceiveOverrunErrors(counter.receive_overrun_error)
            statportreply.setReceiveCRCErrors(counter.receive_crc_error)
            statportreply.setTransmitPackets(counter.transmittedpacket)
            statportreply.setTransmitBytes(counter.transmittedbytes)
            statportreply.setTransmitDropped(counter.transmitdrops)
            statportreply.setTransmitErrors(counter.transmiterror)
            statList += statportreply
          }
        }
        else {
          val counter = openflowPlane.topoModule.portcounters(port_num)
          statportreply.setCollisions(counter.collisions)
          statportreply.setPortNumber(port_num)
          statportreply.setReceiveBytes(counter.receivedbytes)
          statportreply.setreceivePackets(counter.receivedpacket)
          statportreply.setReceiveDropped(counter.receivedrops)
          statportreply.setreceiveErrors(counter.receiveerror)
          statportreply.setReceiveFrameErrors(counter.receiveframe_align_error)
          statportreply.setReceiveOverrunErrors(counter.receive_overrun_error)
          statportreply.setReceiveCRCErrors(counter.receive_crc_error)
          statportreply.setTransmitPackets(counter.transmittedpacket)
          statportreply.setTransmitBytes(counter.transmittedbytes)
          statportreply.setTransmitDropped(counter.transmitdrops)
          statportreply.setTransmitErrors(counter.transmiterror)
          statList += statportreply
        }
        statreply.setStatisticType(OFStatisticsType.PORT)
        statreply.setStatistics(statList)
        statreply.setStatisticsFactory(factory)
        statreply.setLength((12 + statportreply.getLength * statList.size).toShort)
        statreply.setXid(ofstatreq.getXid)
        openflowPlane.ioBatchBuffer += statreply
      }

      case OFStatisticsType.AGGREGATE => {
        val stataggreqmsg = ofstatreq.asInstanceOf[OFStatisticsRequest]
        val stataggreq = stataggreqmsg.getStatistics.get(0).asInstanceOf[OFAggregateStatisticsRequest]
        val stataggreply = factory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.AGGREGATE)
          .asInstanceOf[OFAggregateStatisticsReply]
        val statreply = factory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
        val table_id = stataggreq.getTableId
        val statList = new util.ArrayList[OFStatistics]
        if (table_id >= 0 && table_id < openflowPlane.ofroutingModule.flowtables.length) {
          val referred_table = openflowPlane.ofroutingModule.flowtables(table_id)
          stataggreply.setFlowCount(referred_table.counters.referencecount)
          stataggreply.setPacketCount(referred_table.counters.packetlookup +
            referred_table.counters.packetmatches)
          stataggreply.setByteCount(referred_table.counters.flowbytes)
        }
        else {
          for (i <- 0 until openflowPlane.ofroutingModule.flowtables.length) {
            val referred_table = openflowPlane.ofroutingModule.flowtables(i)
            stataggreply.setFlowCount(stataggreply.getFlowCount + referred_table.counters.referencecount)
            stataggreply.setPacketCount(stataggreply.getPacketCount + referred_table.counters.packetlookup +
              referred_table.counters.packetmatches)
            stataggreply.setByteCount(stataggreply.getByteCount + referred_table.counters.flowbytes)
          }
        }
        statList += stataggreply
        statreply.setStatisticType(OFStatisticsType.AGGREGATE)
        statreply.setStatistics(statList)
        statreply.setStatisticsFactory(factory)
        statreply.setLength((stataggreply.getLength + statreply.getLength).toShort)
        statreply.setXid(ofstatreq.getXid)
        openflowPlane.ioBatchBuffer += statreply
      }
      case _ => throw new Exception("unrecognized OFStatisticRequest: " + ofstatreq.getStatisticType)
    }
  }

  private def processMessage(ofm : OFMessage) = ofm.getType match {
    case OFType.HELLO => {
      val helloreply = factory.getMessage(OFType.HELLO).asInstanceOf[OFHello]
      helloreply.setLength(8)
      helloreply.setType(OFType.HELLO)
      helloreply.setXid(ofm.getXid)
      helloreply.setVersion(ofm.getVersion)
      openflowPlane.ioBatchBuffer += helloreply
    }
    case OFType.FEATURES_REQUEST => {
      val featurereply = factory.getMessage(OFType.FEATURES_REPLY).asInstanceOf[OFFeaturesReply]
      val featurelist = openflowPlane.getSwitchFeature
      featurereply.setDatapathId(featurelist._1)
      featurereply.setBuffers(featurelist._2)
      featurereply.setTables(featurelist._3.toByte)
      featurereply.setCapabilities(featurelist._4)
      featurereply.setPorts(featurelist._5)
      featurereply.setLength((32 + featurereply.getPorts.length * OFPhysicalPort.MINIMUM_LENGTH).toShort)
      //TODO: only support output action for now
      featurereply.setActions(1) //only support output action for now
      featurereply.setXid(ofm.getXid)
      openflowPlane.ioBatchBuffer += featurereply
    }
    case OFType.SET_CONFIG => {
      val ofsetconfig = ofm.asInstanceOf[OFSetConfig]
      openflowPlane.setSwitchParameters(ofsetconfig.getFlags, ofsetconfig.getMissSendLength)
    }
    case OFType.GET_CONFIG_REQUEST => {
      val getconfigreply = factory.getMessage(OFType.GET_CONFIG_REPLY).asInstanceOf[OFGetConfigReply]
      val config = openflowPlane.getSwitchParameters
      getconfigreply.setFlags(config._1)
      getconfigreply.setMissSendLength(config._2)
      getconfigreply.setXid(ofm.getXid)
      getconfigreply.setLength(20)
      openflowPlane.ioBatchBuffer += getconfigreply
    }
    case OFType.STATS_REQUEST => {
      processStatRequest(ofm.asInstanceOf[OFStatisticsRequest])
    }
    case OFType.FLOW_MOD => {
      processFlowMod(ofm.asInstanceOf[OFFlowMod])
    }
    case OFType.ECHO_REQUEST => {
      val echoreq = ofm.asInstanceOf[OFEchoRequest]
      val echoreply = factory.getMessage(OFType.ECHO_REPLY).asInstanceOf[OFEchoReply]
      val payloadlength = {
        if (echoreq.getPayload != null) echoreq.getPayload.length
        else 0
      }
      echoreply.setXid(echoreq.getXid)
      echoreply.setPayload(echoreq.getPayload)
      echoreply.setLength((OFMessage.MINIMUM_LENGTH + payloadlength).toShort)
      openflowPlane.ioBatchBuffer += echoreply
    }
    case OFType.PACKET_OUT => {
      val pkgoutmsg = ofm.asInstanceOf[OFPacketOut]
      openflowPlane.routing(pkgoutmsg)
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
    for (ofm: OFMessage <- msglist) {
      try {
        processMessage(ofm)
      }
      catch {
        case ex: Exception => Channels.fireExceptionCaught(ctx.getChannel, ex)
      }
    }
    //send out all messages
    if (e.getChannel != null && e.getChannel.isConnected)
      e.getChannel.write(openflowPlane.ioBatchBuffer)
  }

  override def exceptionCaught (ctx : ChannelHandlerContext, e : ExceptionEvent) {
    e.getCause.printStackTrace()
  }
}