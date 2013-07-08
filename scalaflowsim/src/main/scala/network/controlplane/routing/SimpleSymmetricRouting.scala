package network.controlplane.routing

import network.topo._
import network.data.Flow
import network.topo.ToRRouterType
import scala.collection.mutable.ListBuffer


private [controlplane] class SimpleSymmetricRouting (node : Node) extends RoutingProtocol (node) {

  private var dstRange : String = null
  private var localRange : String = null
  private var dstCellID : String = null

  private def getCellID(IP : String) : String = {
    val t : String = IP.substring(IP.indexOf('.') + 1, IP.size)
    t.substring(0, t.indexOf('.'))
  }

  private def selectRandomOutlink(flow : Flow) : Link = {
    val selectidx = Math.max(flow.DstIP.hashCode(), flow.DstIP.hashCode() * -1) % node.outlink.size
    var i = 0
    var l : Link = null
    def selectLink() {
      for (l <- node.outlink) {
        if (i == selectidx) return
        i = i + 1
      }
    }
    selectLink()
    l
  }

  private def getDstParameters(flow : Flow) {
    dstRange = flow.DstIP.substring(0, flow.DstIP.lastIndexOf('.') + 1) + ".1"
    localRange = node.ip_addr(0).substring(0, node.ip_addr(0).lastIndexOf('.') + 1) + ".1"
    dstCellID = getCellID(flow.DstIP)
  }

  def nextNode(flow: Flow): Node = node.nodetype match {
    case ToRRouterType() => {
      if (flowPathMap.contains(flow)) return flowPathMap(flow).end_to
      getDstParameters(flow)
      val router = node.asInstanceOf[Router]
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
      if (flowPathMap.contains(flow)) return flowPathMap(flow).end_to
      getDstParameters(flow)
      val router = node.asInstanceOf[Router]
      if (getCellID(node.ip_addr(0)) == dstCellID) {
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
      if (flowPathMap.contains(flow)) return flowPathMap(flow).end_to
      getDstParameters(flow)
      val router = node.asInstanceOf[Router]
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
    case _ => {
      if (flowPathMap.contains(flow)) return flowPathMap(flow).end_to
      //it's a host
      return selectRandomOutlink(flow).end_to
    }
  }
}
