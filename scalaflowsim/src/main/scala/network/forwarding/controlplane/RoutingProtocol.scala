package network.forwarding.controlplane

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.collection.mutable
import org.openflow.protocol.OFMatch
import network.device._
import network.traffic.Flow
import simengine.utils.{XmlParser, Logging}
import network.forwarding.controlplane.openflow.{OFMatchField, OpenFlowControlPlane}
import network.forwarding.controlplane.openflow.flowtable.OFFlowTable
import utils.IPAddressConvertor

/**
 *  the trait representing the functionalities to calculate
 *  the routing path locally or remotely
 */
trait RoutingProtocol extends Logging {


  //to avoid flood the same flow for multiple times
  //TODO: lacking a broadcast tree
  private val floodlist = new ArrayBuffer[Flow] with mutable.SynchronizedBuffer[Flow]


  protected val RIBIn = new mutable.HashMap[OFMatchField, Link]
    with mutable.SynchronizedMap[OFMatchField, Link]
  protected val RIBOut = new mutable.HashMap[OFMatchField, Link]
    with mutable.SynchronizedMap[OFMatchField, Link]

  protected var wildcard = OFMatch.OFPFW_ALL &
    ~OFMatch.OFPFW_NW_DST_MASK &
    ~OFMatch.OFPFW_NW_SRC_MASK

  def fetchInRoutingEntry(ofmatch : OFMatch) : Link = {
    val matchfield = OFFlowTable.createMatchField(ofmatch, wildcard)
    logDebug("quering matchfield: " + matchfield + "(" + matchfield.hashCode + ")" +
      " node:" + this)
    assert(RIBIn.contains(matchfield))
    RIBIn(matchfield)
  }

  def fetchOutRoutingEntry(ofmatch : OFMatch) : Link = {
    val matchfield = OFFlowTable.createMatchField(ofmatch, wildcard)
    assert(RIBOut.contains(matchfield))
    RIBOut(matchfield)
  }

  def insertOutPath (ofmatch : OFMatch, link : Link)  {
    logTrace(this + " insert outRIB entry " +
      IPAddressConvertor.IntToDecimalString(ofmatch.getNetworkSource) + "->" +
      IPAddressConvertor.IntToDecimalString(ofmatch.getNetworkDestination) +
      " with the link " + link.toString)
    val matchfield = OFFlowTable.createMatchField(ofmatch = ofmatch, wcard = wildcard)
    RIBOut += (matchfield -> link)
    //add source host to global device
    if (link.end_from.ip_addr(0) ==
      IPAddressConvertor.IntToDecimalString(matchfield.getNetworkSource)) {
    }
  }

  def insertInPath (ofmatch : OFMatch, link : Link) {
    val matchfield = OFFlowTable.createMatchField(ofmatch = ofmatch, wcard = wildcard)
    RIBIn += (matchfield -> link)
    logTrace(this + " insert inRIB entry " + matchfield + "(" + matchfield.hashCode
      + ") -> " + link + " RIBIn Length:" + RIBIn.size)
    //add destination host to global device
    if (link.end_from.ip_addr(0) ==
      IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination)) {
    }
  }

  def deleteEntry(ofmatch : OFMatch) {
    val matchfield = OFFlowTable.createMatchField(ofmatch, wildcard)
    logTrace("delete entry:" + matchfield + " at node:" + this)
    RIBIn -= matchfield
    RIBOut -= matchfield
  }

  /**
   * route the flow to the next node
   * @param flow the flow to be routed
   * @param inlink it can be null (for the first hop)
   */
  def routing (localnode : Node, flow : Flow, matchfield : OFMatchField, inlink : Link) {
    //discard the flood packets
    if (wrongDistination(localnode, flow)) return
    logTrace("arrive at " + localnode.ip_addr(0) +
      ", routing (flow : Flow, matchfield : OFMatch, inlink : Link)" +
      " flow:" + flow + ", inlink:" + inlink)
    if (inlink != null) insertInPath(matchfield, inlink)
    if (localnode.ip_addr(0) == flow.dstIP) {
      //start resource allocation process
      flow.setEgressLink(inlink)
      localnode.dataplane.allocate(localnode, flow, inlink)
    } else {
      if (!flow.floodflag) {
        val nextlink = selectNextHop(flow, matchfield, inlink)
        if (nextlink != null) {
          forward(localnode, nextlink, inlink, flow, matchfield)
        }
      } else {
        //it's a flood flow
        logTrace("flow " + flow + " is flooded out at " + localnode)
        floodoutFlow(localnode, flow, matchfield, inlink)
      }
    }
  }

  /**
   *
   * @param flow
   * @param matchfield
   * @param inlink
   */
  def floodoutFlow(localnode: Node, flow : Flow, matchfield : OFMatchField, inlink : Link) {
    if (!floodlist.contains(flow)) {
      val nextlinks = localnode.interfacesManager.getfloodLinks(localnode, inlink)
      if (nextlinks != null) System.out.println("fuck");
      //TODO : openflow flood handling in which nextlinks can be null?
      nextlinks.foreach(l => {
        flow.addTrace(l, inlink)
        Link.otherEnd(l, localnode).controlplane.routing(Link.otherEnd(l, localnode), flow, matchfield, l)
      })
      floodlist += flow
    }
  }

  def forward (localnode: Node, olink : Link, inlink : Link, flow : Flow, matchfield : OFMatchField) {
    val nextnode = Link.otherEnd(olink, localnode)
    logDebug("send through " + olink)
    flow.addTrace(olink, inlink)
    nextnode.controlplane.routing(nextnode, flow, matchfield, olink)
  }


  private def wrongDistination(localnode: Node, flow : Flow) : Boolean = {
    if (localnode.ip_addr(0) !=  flow.srcIP && localnode.ip_addr(0) != flow.dstIP && localnode.nodetype == HostType) {
      logTrace("Discard flow " + flow + " on node " + localnode.toString)
      return true
    }
    false
  }

  //abstract methods
  def selectNextHop(flow : Flow, matchfield : OFMatchField, inPort : Link) : Link
}

object RoutingProtocol {

  def apply(node : Node) : RoutingProtocol = {
    XmlParser.getString("scalasim.simengine.model", "default") match {
      case "default" => new DefaultControlPlane(node)
      case "openflow" => {
        node.nodetype match {
          case HostType => new DefaultControlPlane(node)
          case _ => new OpenFlowControlPlane(node)
        }
      }
      case _ => null
    }
  }
}

