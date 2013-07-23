package scalasim.network.component

import org.openflow.util.HexString


class Router (nodetype : NodeType) extends Node(nodetype) {
  private var rid : Int = 0
  //openflow setup
  private var flags : Short = 0
  private var miss_send_len : Short = 0

  def setParameter(f : Short, m : Short) {
    flags = f
    miss_send_len = miss_send_len
  }

  def getFlag = flags

  def get_miss_send_len = miss_send_len

  def setrid (r : Int) { rid = r }

  def getDPID() : Long = {
    val impl_dependent = nodetype match {
      case ToRRouterType => "00"
      case AggregateRouterType => "01"
      case CoreRouterType => "02"
    }
    val t = ip_addr(0).substring(ip_addr(0).indexOf('.') + 1, ip_addr(0).size)
    //TODO: implicitly limit the maximum number of pods, improve?
    val podid = HexString.toHexString(Integer.parseInt(t.substring(0, t.indexOf('.'))), 1)
    val order = HexString.toHexString(rid, 6)
    HexString.toLong(impl_dependent + ":" + podid + ":" + order)
  }
}

class RouterContainer () extends NodeContainer {
  def create(nodeN : Int, rtype : NodeType) {
    for (i <- 0 until nodeN) {
      nodecontainer += new Router(rtype)
      this(i).setrid(i)
    }
  }

  def create(nodeN : Int) {
    throw new RuntimeException("you have to specify the router type")
  }

  override def apply(i : Int) = nodecontainer(i).asInstanceOf[Router]
}

