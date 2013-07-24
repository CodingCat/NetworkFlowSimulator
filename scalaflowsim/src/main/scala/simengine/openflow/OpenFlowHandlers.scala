package scalasim.simengine.openflow

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.openflow.protocol._
import scala.collection.JavaConversions._
import scalasim.simengine.utils.Logging
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.openflow.protocol.statistics.{OFStatistics, OFDescriptionStatistics, OFStatisticsType}
import java.util
import simengine.openflow.OpenFlowMsgFactory

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
    OpenFlowMsgFactory.parseMessage(buffer)
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
        val statdescreply = OpenFlowMsgFactory.getStatistics(OFType.STATS_REPLY,OFStatisticsType.DESC)
          .asInstanceOf[OFDescriptionStatistics]
        //TODO: descriptions are not complete
        statdescreply.setDatapathDescription(connector.getSwitchDescription)
        statdescreply.setHardwareDescription("simulated router hardware")
        statdescreply.setManufacturerDescription("simulated router")
        statdescreply.setSoftwareDescription("simulated router software")
        statdescreply.setSerialNumber("1")
        val statList = new util.ArrayList[OFStatistics]
        statList += statdescreply
        val statreply = OpenFlowMsgFactory.getMessage(OFType.STATS_REPLY).asInstanceOf[OFStatisticsReply]
        statreply.setStatisticType(OFStatisticsType.DESC)
        statreply.setStatistics(statList)
        statreply.setStatisticsFactory(OpenFlowMsgFactory)
        statreply.setLength((statdescreply.getLength+ statreply.getLength).toShort)
        statreply.setXid(ofstatreq.getXid)
        outlist += statreply
      }
      case _ => throw new Exception("unrecognized OFStatisticRequest: " + ofstatreq.getStatisticType)
    }
  }

  private def processMessage(ofm : OFMessage, outlist : java.util.ArrayList[OFMessage]) = ofm.getType match {
    case OFType.HELLO => {
      logTrace("receive a hello message from controller")
      outlist += OpenFlowMsgFactory.getMessage(OFType.HELLO).asInstanceOf[OFHello]
    }
    case OFType.FEATURES_REQUEST => {
      logTrace("receive a hello message from controller")
      val featurereply = OpenFlowMsgFactory.getMessage(OFType.FEATURES_REPLY).asInstanceOf[OFFeaturesReply]
      val featurelist = connector.getSwitchFeature
      featurereply.setXid(ofm.getXid)
      featurereply.setDatapathId(featurelist._1)
      featurereply.setBuffers(featurelist._2)
      featurereply.setTables(featurelist._3.toByte)
      featurereply.setCapabilities(featurelist._4)
      featurereply.setPorts(featurelist._5)
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
      val getconfigreply = OpenFlowMsgFactory.getMessage(OFType.GET_CONFIG_REPLY).asInstanceOf[OFGetConfigReply]
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