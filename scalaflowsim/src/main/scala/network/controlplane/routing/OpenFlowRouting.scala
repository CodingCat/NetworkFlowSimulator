package scalasim.network.controlplane.routing

import scalasim.network.component.{Node, Link}
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.openflow.OpenFlowModule
import scalasim.network.traffic.Flow
import packets._
import scala.collection.mutable
import org.openflow.protocol._
import network.controlplane.openflow.flowtable.OFMatchField
import org.openflow.protocol.action.OFActionOutput
import scala.collection.JavaConversions._
import scala.concurrent.Lock
import simengine.utils.XmlParser

class OpenFlowRouting (node : Node) extends RoutingProtocol (node) {

  private lazy val ofcontrolplane = controlPlane.asInstanceOf[OpenFlowModule]

  private [controlplane] val flowtables = new Array[OFFlowTable](
    XmlParser.getInt("scalasim.openflow.flowtablenum", 1))

  private [controlplane] val pendingFlows = new mutable.HashMap[Int, Flow]
    with mutable.SynchronizedMap[Int, Flow]
  private [controlplane] val bufferLock = new Lock

  def init() {
    for (i <- 0 until flowtables.length)
      flowtables(i) = new OFFlowTable(this)
  }

  override def selectNextLink(flow : Flow, matchfield : OFMatchField, inLink : Link): Link = {
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
      ofcontrolplane.sendPacketInToController(flow , inLink, serializedData)
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

  def addNewFlowEntry (flowmod : OFFlowMod) {
    //for openflow 1.0
    flowtables(0).addFlowTableEntry(flowmod)
    var opnum = 0
    flowmod.getActions.foreach(action => {
      if (action.isInstanceOf[OFActionOutput])
        opnum = action.asInstanceOf[OFActionOutput].getPort
    } )
    val link = node.controlPlane.topoModule.reverseSelection(opnum.toShort)
    insertOutPath(flowmod.getMatch, link)
  }

  def removeFlowEntry (matchfield : OFMatchField) {
    //for openflow 1.0
    flowtables(0).removeEntry(matchfield)
    RIBOut -= matchfield
  }

  init

}
