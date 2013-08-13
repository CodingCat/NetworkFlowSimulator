package scalasim.network.controlplane.openflow


import scalasim.network.component._
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.resource.ResourceAllocator
import scalasim.network.controlplane.routing.{OpenFlowRouting, RoutingProtocol}
import scalasim.network.controlplane.topology.TopologyManager
import scalasim.network.controlplane.ControlPlane
import scalasim.network.traffic.Flow
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
import simengine.utils.XmlParser

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
