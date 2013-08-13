package network.device

import scala.collection.mutable.HashMap

object GlobalDeviceManager {
  var globaldevicecounter = 0

  private val globalHostMap = new HashMap[String, Host]

  def getHost (ip: String) = {
    assert(globalHostMap.contains(ip))
    globalHostMap(ip)
  }

  def insertNewHost(ip: String, host: Host) {
    globalHostMap += ip -> host
  }
}
