package network.device

import scala.collection.mutable.HashMap

object GlobalDeviceManager {
  var globaldevicecounter = 0

  private val globalNodeMap = new HashMap[String, Node]

  def getNode (ip: String) = {
    assert(globalNodeMap.contains(ip))
    globalNodeMap(ip)
  }

  def addNewNode(ip: String, node: Node) {
    globalNodeMap += ip -> node
  }
}
