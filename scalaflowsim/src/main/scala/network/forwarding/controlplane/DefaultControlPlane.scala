package network.forwarding.controlplane

import network.controlplane.openflow.flowtable.OFMatchField
import org.openflow.protocol.OFMatch
import network.device._
import simengine.utils.Logging
import netsimulator.utils.IPAddressConvertor
import network.traffic.Flow

/**
 * the class representing the default process to get/calculate the
 * routing path of packets/flow, that is depended on the symmetric topology
 * without support of any functions like VLAN, etc.
 */
class DefaultControlPlane (node : Node) extends RoutingProtocol with Logging {

  private var dstRange : String = null
  private var localRange : String = null
  private var dstCellID : String = null

  private def getCellID(IP : String) : String = {
    val t : String = IP.substring(IP.indexOf('.') + 1, IP.size)
    t.substring(0, t.indexOf('.'))
  }

  private def selectRandomOutlink(matchfield : OFMatch) : Link = {
    val dstip = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination)
    val selectidx = Math.max(dstip.hashCode(), dstip.hashCode() * -1) %
      node.interfacesManager.outlink.size
    node.interfacesManager.outlink.values.toList(selectidx)
  }

  private def getDstParameters(matchfield : OFMatch) {
    val dstIP = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination)
    dstRange = dstIP.substring(0, dstIP.lastIndexOf('.') + 1) + "1"
    localRange = node.ip_addr(0).substring(0, node.ip_addr(0).lastIndexOf('.') + 1) + "1"
    dstCellID = getCellID(dstIP)
  }

  def selectNextHop(flow : Flow, matchfield : OFMatchField, inlink : Link): Link = {
    if (RIBOut.contains(matchfield)) return RIBOut(matchfield)
    var olink : Link = null
    if (node.nodetype != HostType) {
      getDstParameters(matchfield)
      val dstIP = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination)
      val routerCP = node.asInstanceOf[Router].controlplane
      node.nodetype match {
        case ToRRouterType => {
          if (dstRange == localRange) {
            assert(node.interfacesManager.inlinks.contains(dstIP))
            olink = node.interfacesManager.inlinks(dstIP)
          } else {
            olink = selectRandomOutlink(matchfield)
          }
        }
        case AggregateRouterType => {
          if (getCellID(node.ip_addr(0)) == dstCellID) {
            //in the same cell
            assert(node.interfacesManager.inlinks.contains(dstRange))
            olink = node.interfacesManager.inlinks(dstRange)
          } else {
            olink = selectRandomOutlink(matchfield)
          }
        }
        case CoreRouterType => {
          val routepaths =
            node.interfacesManager.inlinks.keySet.filter(podip => (getCellID(podip) == dstCellID)).toList
          assert(routepaths.size > 0)
          val selectIdx = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination).hashCode %
              routepaths.size
          olink = node.interfacesManager.inlinks(routepaths(Math.max(selectIdx, selectIdx * -1)))
        }
      }
    } else {
      //it's a host
      if (RIBOut.contains(matchfield)) return RIBOut(matchfield)
      olink = selectRandomOutlink(matchfield)
    }
    insertOutPath(matchfield, olink)
    olink
  }



  override def toString = node.ip_addr(0)
}
