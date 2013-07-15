package network.controlplane.routing

import network.component._
import network.traffic.Flow
import network.component.ToRRouterType
import scala.collection.mutable.ListBuffer
import network.controlplane.ControlPlane


private [controlplane] class SimpleSymmetricRouting (controlPlane : ControlPlane)
  extends RoutingProtocol (controlPlane) {

  private var dstRange : String = null
  private var localRange : String = null
  private var dstCellID : String = null

  private def getCellID(IP : String) : String = {
    val t : String = IP.substring(IP.indexOf('.') + 1, IP.size)
    t.substring(0, t.indexOf('.'))
  }

  private def selectRandomOutlink(flow : Flow) : Link = {
    val selectidx = Math.max(flow.DstIP.hashCode(), flow.DstIP.hashCode() * -1) %
      controlPlane.topoModule.outlink.size
    var i = 0
    def selectLink() : Link = {
      for (link <- controlPlane.topoModule.outlink.values) {
        if (i == selectidx) {
          return link
        }
        i = i + 1
      }
      return null
    }
    selectLink()
  }

  private def getDstParameters(flow : Flow) {
    dstRange = flow.DstIP.substring(0, flow.DstIP.lastIndexOf('.') + 1) + "1"
    localRange = controlPlane.IP.substring(0, controlPlane.IP.lastIndexOf('.') + 1) + "1"
    dstCellID = getCellID(flow.DstIP)
  }

  def selectNextLink(flow: Flow): Link = {
    if (flowPathMap.contains(flow)) {
      return flowPathMap(flow)
    }
    flow.increaseHop()
    if (controlPlane.node.nodetype != HostType) {
      getDstParameters(flow)
      val router = controlPlane.node.asInstanceOf[Router]
      val routerCP = router.controlPlane
      controlPlane.node.nodetype match {
        case ToRRouterType => {
          if (dstRange == localRange) {
            if (routerCP.topoModule.inlinks.contains(flow.DstIP)) routerCP.topoModule.inlinks(flow.DstIP)
            else {
              throw new Exception("topology error, tor router cannot find a host, " +
                "dstIP:" + flow.DstIP + "\tsrcIP:" + flow.SrcIP + "\tlocalIP:" + localRange)
            }
          }
          else selectRandomOutlink(flow)
        }
        case AggregateRouterType => {
          if (getCellID(router.ip_addr(0)) == dstCellID) {
            //in the same cell
            if (routerCP.topoModule.inlinks.contains(dstRange)) routerCP.topoModule.inlinks(dstRange)
            else {
              throw new Exception("topology error, agg router cannot find a router, " +
                "dstIP:" + flow.DstIP + "\tsrcIP:" + flow.SrcIP + "\tlocalIP:" + localRange)
            }
          }
          else selectRandomOutlink(flow)
        }
        case CoreRouterType => {
          val routepaths = new ListBuffer[Link]
          for (cellIP <- routerCP.topoModule.inlinks.keySet) {
            if (getCellID(cellIP) == dstCellID) routepaths += routerCP.topoModule.inlinks(cellIP)
          }
          if (routepaths.size > 0) {
            val selectIdx = flow.DstIP.hashCode % routepaths.size
            routepaths(Math.max(selectIdx, selectIdx * -1))
          }
          else {
            throw new Exception("topology error, core router cannot find a router, " +
              "dstIP:" + flow.DstIP + "\tsrcIP:" + flow.SrcIP + "\tlocalIP:" + localRange)
          }
        }
      }
    }
    else {
      if (flowPathMap.contains(flow)) return flowPathMap(flow)
      //it's a host
      val l = selectRandomOutlink(flow)
      if (l == null) throw new Exception("failed on the first hop")
      l
    }
  }
}
