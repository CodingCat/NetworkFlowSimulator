package scalasim.network.controlplane.routing

import scalasim.network.component.{Node, Link}
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.openflow.OpenFlowModule
import scalasim.network.traffic.Flow
import packets._
import scalasim.XmlParser
import scala.collection.mutable
import org.openflow.protocol._

class OpenFlowRouting (node : Node) extends RoutingProtocol (node) {

  private lazy val ofcontrolplane = controlPlane.asInstanceOf[OpenFlowModule]

  private [controlplane] val flowtables = new Array[OFFlowTable](
    XmlParser.getInt("scalasim.openflow.flowtablenum", 1))
  private [controlplane] val pendingFlows = new mutable.HashMap[Int, Flow] with mutable.SynchronizedMap[Int, Flow]

  def init() {
    for (i <- 0 until flowtables.length)
      flowtables(i) = new OFFlowTable(this)
  }

  /*override def floodoutFlow(flow : Flow, matchfield : OFMatch, inlink : Link) {
    val nextlinks = getfloodLinks(flow, inlink)
    flow.floodflag = false
    //TODO : openflow flood handling in which nextlinks can be null?
    nextlinks.foreach(l => Link.otherEnd(l, node).controlPlane.routing(flow, matchfield, l))
  } */

  override def selectNextLink(flow : Flow, matchfield : OFMatch, inLink : Link): Link = {
    //TODO: matching by flowtable
    if (!RIBIn.contains(matchfield)) {
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
            .setSourcePort(flow.srcPort)
            .setDestinationPort(flow.dstPort)
            .setPayload(new Data(dummypayload))))
      val serializedData = ethernetFrame.serialize
      pendingFlows += (pendingFlows.size -> flow)
      ofcontrolplane.sendPacketInToController(inLink, serializedData)
      null
    }
    else {
      //apply the actions on the matchfield/packets and return the link
      RIBIn(matchfield)
    }
  }

  def addNewFlowEntry (flow : Flow, flowmod : OFFlowMod) {
    //for openflow 1.0
    val outportnum = flowtables(0).addFlowTableEntry(flowmod)
    RIBOut += (flowmod.getMatch -> node.controlPlane.topoModule.reverseSelection(outportnum))
  }

  def removeFlowEntry (matchfield : OFMatch) {
    //for openflow 1.0
    flowtables(0).removeEntry(matchfield)
    RIBOut -= matchfield
  }

  init

}
