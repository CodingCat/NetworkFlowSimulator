package scalasim.network.controlplane.routing

import scalasim.network.component._
import scalasim.network.traffic.Flow
import scalasim.network.component.ToRRouterType
import scala.collection.mutable.ListBuffer
import org.openflow.protocol.OFMatch
import simengine.utils.IPAddressConvertor
import scala.collection.mutable


private [controlplane] class SimpleSymmetricRouting (node : Node)
  extends RoutingProtocol (node) {

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
      controlPlane.topoModule.outlink.size
    var i = 0
    def selectLink() : Link = {
      for (link <- controlPlane.topoModule.outlink.values) {
        if (i == selectidx) {
          return link
        }
        i = i + 1
      }
      null
    }
    selectLink()
  }

  private def getDstParameters(matchfield : OFMatch) {
    val dstIP = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination)
    dstRange = dstIP.substring(0, dstIP.lastIndexOf('.') + 1) + "1"
    localRange = controlPlane.IP.substring(0, controlPlane.IP.lastIndexOf('.') + 1) + "1"
    dstCellID = getCellID(dstIP)
  }

  override def selectNextLink(flow : Flow, matchfield : OFMatch, inlink : Link): Link = {
    if (flow.floodflag) throw new Exception("you cannot call this method for a flood flow")
    if (flowPathMap.contains(matchfield)) return flowPathMap(matchfield)
    if (controlPlane.node.nodetype != HostType) {
      getDstParameters(matchfield)
      val dstIP = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination)
      val router = controlPlane.node.asInstanceOf[Router]
      val routerCP = router.controlPlane
      controlPlane.node.nodetype match {
        case ToRRouterType => {
          if (dstRange == localRange) {
            if (routerCP.topoModule.inlinks.contains(dstIP)) routerCP.topoModule.inlinks(dstIP)
            else {
              throw new Exception("topology error, tor router cannot find a host, " +
                "dstIP:" + dstIP + "\tsrcIP:" + dstIP + "\tlocalIP:" + localRange)
            }
          } else {
            selectRandomOutlink(matchfield)
          }
        }
        case AggregateRouterType => {
          if (getCellID(router.ip_addr(0)) == dstCellID) {
            //in the same cell
            if (routerCP.topoModule.inlinks.contains(dstRange)) routerCP.topoModule.inlinks(dstRange)
            else {
              throw new Exception("topology error, agg router cannot find a router, " +
                "dstIP:" + IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination) +
                "\tsrcIP:" + IPAddressConvertor.IntToDecimalString(matchfield.getNetworkSource) +
                "\tlocalIP:" + localRange)
            }
          } else selectRandomOutlink(matchfield)
        }
        case CoreRouterType => {
          val routepaths = new ListBuffer[Link]
          for (cellIP <- routerCP.topoModule.inlinks.keySet) {
            if (getCellID(cellIP) == dstCellID) routepaths += routerCP.topoModule.inlinks(cellIP)
          }
          if (routepaths.size > 0) {
            val selectIdx = IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination).hashCode %
              routepaths.size
            routepaths(Math.max(selectIdx, selectIdx * -1))
          } else {
            throw new Exception("topology error, core router cannot find a router, " +
              "dstIP:" + IPAddressConvertor.IntToDecimalString(matchfield.getNetworkDestination) +
              "\tsrcIP:" + IPAddressConvertor.IntToDecimalString(matchfield.getNetworkSource) +
              "\tlocalIP:" + localRange)
          }
        }
      }
    } else {
      if (flowPathMap.contains(matchfield)) return flowPathMap(matchfield)
      //it's a host
      val l = selectRandomOutlink(matchfield)
      if (l == null) throw new Exception("failed on the first hop")
      l
    }
  }

  def getfloodLinks(flow: Flow, inport: Link): List[Link] = {
    if (!flow.floodflag) throw new Exception("you cannot call this method on a non-flood flow")
    val returnList = new mutable.MutableList[Link]
    val alllink = {
      if (node.nodetype != HostType)
        node.controlPlane.topoModule.inlinks.values.toList :::
          node.controlPlane.topoModule.outlink.values.toList
      else node.controlPlane.topoModule.outlink.values.toList
    }
    alllink.foreach(l => if (l != inport) returnList += l)
    //add to flow's flood trace
    returnList.foreach(l => flow.addFloodTrace(l, inport))
    returnList.toList
  }
}
