package scalasim.network.controlplane.routing

import scalasim.network.component.{Node, Link}
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.openflow.OpenFlowModule
import scalasim.network.traffic.Flow
import packets.{Data, TCP, IPv4, Ethernet}
import scalasim.XmlParser

class OpenFlowRouting (node : Node) extends RoutingProtocol (node) {

  private [controlplane] val flowtables = new Array[OFFlowTable](
    XmlParser.getInt("scalasim.openflow.flowtablenum", 1))

  def init() {
    for (i <- 0 until flowtables.length)
      flowtables(i) = new OFFlowTable
  }

  def selectNextLink(flow: Flow, inLink : Link): Link = {
    //TODO: matching by flowtable
    if (!flowPathMap.contains(flow)) {
      //send packet_in to controller
      // def sendPacketInToController(flow : Flow, ethernetFramedata : Array[Byte])
      val dummypayload = new Array[Byte](1)
      dummypayload(0) = (0).toByte
      val ethernetFrame = new Ethernet
      ethernetFrame.setEtherType(Ethernet.TYPE_IPv4)
        .setSourceMACAddress(flow.srcMac)
        .setDestinationMACAddress(flow.dstMac)
        .setPayload(new IPv4()
          .setSourceAddress(flow.srcIP)
          .setDestinationAddress(flow.dstIP)
          .setPayload(new TCP()
            .setSourcePort((50000).toShort)
            .setDestinationPort((50000).toShort)
            .setPayload(new Data(dummypayload))))
      val serializedData = ethernetFrame.serialize
      node.controlPlane.asInstanceOf[OpenFlowModule].sendPacketInToController(inLink, serializedData)
    }
    if (flowPathMap.contains(flow)) flowPathMap(flow)
    else null
  }

  init
}
