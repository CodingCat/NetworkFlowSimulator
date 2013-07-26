package scalasim.network.controlplane.openflow

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.openflow.protocol._
import scala.collection.JavaConversions._
import scalasim.simengine.utils.Logging
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.openflow.protocol.statistics._
import java.util
import scalasim.XmlParser

class OpenFlowMsgEncoder extends OneToOneEncoder {

  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
    if (!(msg.isInstanceOf[java.util.ArrayList[_]])) return msg
    val msglist = msg.asInstanceOf[java.util.ArrayList[OFMessage]]
    var size: Int = 0
    for (ofm <- msglist) {
      size += ofm.getLengthU
    }
    val buf: ChannelBuffer = ChannelBuffers.buffer(size)
    for (ofm <- msglist) {
      ofm.writeTo(buf)
    }
    return buf
  }
}

class OpenFlowMsgDecoder extends FrameDecoder {

  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer)  : AnyRef = {
    //parse the channelbuffer into the list of ofmessage
    if (!channel.isConnected) return None
    OpenFlowFactory.parseMessage(buffer)
  }
}

class OpenFlowChannelHandler (private val connector : OpenFlowModule)
  extends SimpleChannelHandler  with Logging  {

  private def processFlowMod(offlowmod : OFFlowMod) {
    offlowmod.getCommand match {
      case OFFlowMod.OFPFC_DELETE => {
        if (offlowmod.getMatch.getWildcards == OFMatch.OFPFW_ALL) {
          //clear to initialize flow tables;
          for (table <- connector.flowtables) table.clear
        }
      }
      case _ => throw new Exception("unrecognized OFFlowMod command type:" + offlowmod.getCommand)
    }
  }

  private def processStatRequest(ofstatreq : OFStatisticsRequest, outlist : java.util.ArrayList[OFMessage]) {
    ofstatreq.getStatisticType match {
      case OFStatisticsType.DESC => {
        val statdescreply = OpenFlowFactory.getStatistics(OFType.STATS_REPLY,OFStatisticsType.DESC)
          .asInstanceOf[OFDescriptionStatistics]
        //TODO: descriptions are not complete
        statdescreply.setDatapathDescription(connector.getSwitchDescription)
        statdescreply.setHardwareDescription("simulated router hardware")
        statdescreply.setManufacturerDescription("simulated router")
        statdescreply.setSoftwareDescription("simulated router software")
        statdescreply.setSerialNumber("1")
        val statList = new util.ArrayList[OFStatistics]
        statList += statdescreply
        val statreply = OpenFlowFactory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
        statreply.setStatisticType(OFStatisticsType.DESC)
        statreply.setStatistics(statList)
        statreply.setStatisticsFactory(OpenFlowFactory)
        statreply.setLength((statdescreply.getLength+ statreply.getLength).toShort)
        statreply.setXid(ofstatreq.getXid)
        outlist += statreply
      }
      case OFStatisticsType.PORT => {
        val statportreqmsg = ofstatreq.asInstanceOf[OFStatisticsRequest]
        val statportreq = statportreqmsg.getStatistics.get(0).asInstanceOf[OFPortStatisticsRequest]
        val statportreply = OpenFlowFactory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.PORT)
          .asInstanceOf[OFPortStatisticsReply]
        val statreply = OpenFlowFactory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
        val port_num = statportreq.getPortNumber
        if (port_num == -1) {
          val counters = connector.router.controlPlane.topoModule.portcounters
          for (counter_pair <- counters) {
            val counter = counter_pair._2
            statportreply.setPortNumber(port_num)
            statportreply.setreceivePackets(statportreply.getreceivePackets + counter.receivedpacket)
            statportreply.setTransmitPackets(statportreply.getTransmitPackets + counter.transmittedpacket)
            statportreply.setReceiveBytes(statportreply.getReceiveBytes + counter.receivedbytes)
            statportreply.setTransmitBytes(statportreply.getTransmitBytes + counter.transmittedbytes)
            statportreply.setReceiveDropped(statportreply.getReceiveDropped + counter.receivedrops)
            statportreply.setTransmitDropped(statportreply.getTransmitDropped + counter.transmitdrops)
            statportreply.setreceiveErrors(statportreply.getreceiveErrors + counter.receiveerror)
            statportreply.setTransmitErrors(statportreply.getTransmitErrors + counter.transmiterror)
            statportreply.setReceiveFrameErrors(statportreply.getReceiveFrameErrors +
              counter.receiveframe_align_error)
            statportreply.setReceiveOverrunErrors(statportreply.getReceiveOverrunErrors +
              counter.receive_overrun_error)
            statportreply.setReceiveCRCErrors(statportreply.getReceiveCRCErrors + counter.receive_crc_error)
            statportreply.setCollisions(statportreply.getCollisions + counter.collisions)
          }
        }
        else {
          val counter = connector.router.controlPlane.topoModule.portcounters(port_num)
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
        }
        val statList = new util.ArrayList[OFStatistics]
        statList += statportreply
        statreply.setStatisticType(OFStatisticsType.PORT)
        statreply.setStatistics(statList)
        statreply.setStatisticsFactory(OpenFlowFactory)
        statreply.setLength((statportreply.getLength + statreply.getLength).toShort)
        statreply.setXid(ofstatreq.getXid)
        outlist += statreply
      }
      case OFStatisticsType.FLOW => {

      }
      case OFStatisticsType.AGGREGATE => {
        val stataggreqmsg = ofstatreq.asInstanceOf[OFStatisticsRequest]
        val stataggreq = stataggreqmsg.getStatistics.get(0).asInstanceOf[OFAggregateStatisticsRequest]
        val stataggreply = OpenFlowFactory.getStatistics(OFType.STATS_REPLY, OFStatisticsType.AGGREGATE)
          .asInstanceOf[OFAggregateStatisticsReply]
        val statreply = OpenFlowFactory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
        val table_id = stataggreq.getTableId
        if (table_id >= 0 && table_id < connector.flowtables.length) {
          val referred_table = connector.flowtables(table_id)
          stataggreply.setFlowCount(referred_table.counters.referencecount)
          stataggreply.setPacketCount(referred_table.counters.packetlookup +
            referred_table.counters.packetmatches)
          stataggreply.setByteCount(referred_table.counters.flowbytes)
        }
        else {
          for (i <- 0 until connector.flowtables.length) {
            val referred_table = connector.flowtables(i)
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
        statreply.setStatisticsFactory(OpenFlowFactory)
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
      outlist += OpenFlowFactory.getMessage(OFType.HELLO).asInstanceOf[OFHello]
    }
    case OFType.FEATURES_REQUEST => {
      logTrace("receive a feature request message from controller")
      val featurereply = OpenFlowFactory.getMessage(OFType.FEATURES_REPLY).asInstanceOf[OFFeaturesReply]
      val featurelist = connector.getSwitchFeature
      featurereply.setXid(ofm.getXid)
      featurereply.setDatapathId(featurelist._1)
      featurereply.setBuffers(featurelist._2)
      featurereply.setTables(featurelist._3.toByte)
      featurereply.setCapabilities(featurelist._4)
      featurereply.setPorts(featurelist._5)
      //featurereply.setLengthU(32 + featurereply.getPorts.length * 64)
      outlist += featurereply
    }
    case OFType.SET_CONFIG => {
      val m = ofm.asInstanceOf[OFSetConfig]
      logTrace("receive a set config message from controller, miss_send_length:" + m.getMissSendLength +
        " , flags:" + m.getFlags)
      connector.setSwitchParameters(ofm.asInstanceOf[OFSetConfig])
    }
    case OFType.GET_CONFIG_REQUEST => {
      logTrace("receive a get config request from controller")
      val getconfigreply = OpenFlowFactory.getMessage(OFType.GET_CONFIG_REPLY).asInstanceOf[OFGetConfigReply]
      val config = connector.getSwitchParameters
      getconfigreply.setFlags(config._1)
      getconfigreply.setMissSendLength(config._2)
      getconfigreply.setXid(ofm.getXid)
      outlist += getconfigreply
    }
    case OFType.STATS_REQUEST => {
      logTrace("receive a stat request message from controller")
      processStatRequest(ofm.asInstanceOf[OFStatisticsRequest], outlist)
      if (connector.status == OpenFlowSwitchHandShaking)
        connector.status = OpenFlowSwitchRunning
    }
    case OFType.FLOW_MOD => {
      logTrace("receive a flow_mod message from controller")
      processFlowMod(ofm.asInstanceOf[OFFlowMod])
    }
    case OFType.ECHO_REQUEST => {
      logTrace("receive a echo_request message from controller")
      val echoreq = ofm.asInstanceOf[OFEchoRequest]
      val echoreply = OpenFlowFactory.getMessage(OFType.ECHO_REPLY).asInstanceOf[OFEchoReply]
      echoreply.setXid(echoreq.getXid)
      echoreply.setPayload(echoreq.getPayload)
      outlist += echoreply
    }
    case OFType.PACKET_OUT => {
      logTrace("receive a packet_out message from controller")
      val packetoutmsg = ofm.asInstanceOf[OFPacketOut]
      if (packetoutmsg.getInPort == -1 && packetoutmsg.getBufferId == -1) {
        //TODO: forward the packet in flood
        //the packet must a BDDP
      }
      else {
        //send out the packet or forward the flow
        if (XmlParser.getString("scalasim.simengine.simlevel", "flow") == "flow") {

        }
        else {

        }
      }
    }
    case _ => throw new Exception("unrecognized message type:" + ofm)
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