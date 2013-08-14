package network.device

import scala.collection.mutable.HashMap

object GlobalDeviceManager {
  var globaldevicecounter = 0

  private val globalHostMap = new HashMap[String, Node]

  def getHost (ip: String) = {
    assert(globalHostMap.contains(ip))
    globalHostMap(ip)
  }

  def insertNewNode(ip: String, node: Node) {
    globalHostMap += ip -> node
  }
}
