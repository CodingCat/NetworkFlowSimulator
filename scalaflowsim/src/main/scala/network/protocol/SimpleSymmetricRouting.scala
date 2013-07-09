package network.protocol

import network.topo._
import network.data.Flow
import network.topo.ToRRouterType
import scala.collection.mutable.ListBuffer


class SimpleSymmetricRouting (router : Router) extends RoutingProtocol (router) {

  private var dstRange : String = null
  private var localRange : String = null
  private var dstCellID : String = null

  private def getCellID(IP : String) : String = {
    val t : String = IP.substring(IP.indexOf('.') + 1, IP.size)
    t.substring(0, t.indexOf('.'))
  }

  private def selectRandomOutlink(flow : Flow) : Link = {
    val selectidx = Math.max(flow.DstIP.hashCode(), flow.DstIP.hashCode() * -1) % router.outlink.size
    var i = 0
    var l : Link = null
    def selectLink() {
      for (l <- router.outlink) {
        if (i == selectidx) return
        i = i + 1
      }
    }
    selectLink()
    l
  }

  private def getDstParameters(flow : Flow) {
    dstRange = flow.DstIP.substring(0, flow.DstIP.lastIndexOf('.') + 1) + ".1"
    localRange = router.ip_addr(0).substring(0, router.ip_addr(0).lastIndexOf('.') + 1) + ".1"
    dstCellID = getCellID(flow.DstIP)
  }

  def nextNode(flow: Flow): Node = router.nodetype match {
    case ToRRouterType() => {
      getDstParameters(flow)
      if (dstRange == localRange) {
        if (router.inLinks.contains(flow.DstIP)) {
          //in the same lan
          return router.inLinks(flow.DstIP).end_from
        }
        else {
          //send through arbitrary outlinks to aggregate layer
          return selectRandomOutlink(flow).end_to
        }
      }
      return null
    }
    case AggregateRouterType() => {
      getDstParameters(flow)
      if (getCellID(router.ip_addr(0)) == dstCellID) {
        //in the same cell
        if (router.inLinks.contains(dstRange)) {
          return router.inLinks(dstRange).end_from
        }
        else {
          //send through arbitrary link to the code
          return selectRandomOutlink(flow).end_to
        }
      }
      return null
    }
    case CoreRouterType() => {
      getDstParameters(flow)
      val routepaths = new ListBuffer[Link]
      for (cellIP <- router.inLinks.keySet) {
        if (getCellID(cellIP) == dstCellID) routepaths += router.inLinks(cellIP)
      }
      if (routepaths.size > 0) {
        val selectIdx = flow.DstIP.hashCode % routepaths.size
        return routepaths(Math.max(selectIdx, selectIdx * -1)).end_from
      }
      return null
    }
    case _ => null
  }
}
