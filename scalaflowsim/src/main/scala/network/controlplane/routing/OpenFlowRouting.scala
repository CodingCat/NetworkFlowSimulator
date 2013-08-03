package scalasim.network.controlplane.routing

import scalasim.network.component.{Node, Link}
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.openflow.OpenFlowModule
import scalasim.network.traffic.Flow
import packets._
import scalasim.XmlParser
import scala.collection.mutable
import org.openflow.protocol.{OFMatch, OFFlowMod}

class OpenFlowRouting (node : Node) extends RoutingProtocol (node) {

  private [controlplane] val flowtables = new Array[OFFlowTable](
    XmlParser.getInt("scalasim.openflow.flowtablenum", 1))
  private [controlplane] val pendingFlows = new mutable.HashMap[Int, Flow] with mutable.SynchronizedMap[Int, Flow]

  def init() {
    for (i <- 0 until flowtables.length)
      flowtables(i) = new OFFlowTable(this)
  }

  override def selectNextLink(flow : Flow, matchfield : OFMatch, inLink : Link): Link = {
    //TODO: matching by flowtable
    if (!flowPathMap.contains(matchfield)) {
      //send packet_in to controller
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
            .setSourcePort((50000).toShort)
            .setDestinationPort((50000).toShort)
            .setPayload(new Data(dummypayload))))
      val serializedData = ethernetFrame.serialize
      pendingFlows += (pendingFlows.size -> flow)
      node.controlPlane.asInstanceOf[OpenFlowModule].sendPacketInToController(inLink, serializedData)
    }
    else {
      //apply the actions on the matchfield/packets and return the link
      flowPathMap(matchfield)
    }
    null
  }

  def addNewFlowEntry (flow : Flow, flowmod : OFFlowMod) {
    //for openflow 1.0
    val outportnum = flowtables(0).addFlowTableEntry(flowmod)
    flowPathMap += (flowmod.getMatch -> node.controlPlane.topoModule.reverseSelection(outportnum))
  }

  def removeFlowEntry (matchfield : OFMatch) {
    flowPathMap -= matchfield
    flowtables(0).removeEntry(matchfield)
  }

  init

  def getfloodLinks(flow: Flow, inport: Link): List[Link] = {
    null
  }
}
