package network.topology.builder

import network.topology.{GlobalDeviceManager, Link, HostContainer, RouterContainer}


class FatTreeBuilder (private val podnum: Int = 4,
                      private val linkspeed: Double = 1.0) {

  val pods = 0 until podnum
  val core_sws_idx = 1 until (podnum / 2  + 1)
  val agg_sws_idx = podnum / 2 until podnum
  val edge_sws_idx = 0 until (podnum / 2)
  val hosts_idx = 2 until (podnum / 2 + 2)

  /*
  def buildRack(router: Router, hosts : HostContainer, startIdx : Int, endIdx : Int) {
    try {
      var locallinkBandwidth = XmlParser.getDouble("scalasim.topology.locallinkrate", 100.0)
      if (router.ip_addr.length == 0) throw new RuntimeException("Engress Router hasn't got ipaddress")
      for (i <- startIdx to endIdx) {
        if (hosts(i).ip_addr.length == 0)  {
          throw new RuntimeException("Hosts haven't got ipaddress, the idx is " + i)
        }
        val newlink = new Link(hosts(i), router, locallinkBandwidth)
        hosts(i).interfacesManager.registerOutgoingLink(newlink)
        router.interfacesManager.registerIncomeLink(newlink)
        GlobalDeviceManager.addNewNode(hosts(i).ip_addr(0), hosts(i))
      }
      GlobalDeviceManager.addNewNode(router.ip_addr(0), router)
    }
    catch {
      case ex : RuntimeException => throw ex
    }
  }

   */

  def buildRack(edgeRouters : RouterContainer, hosts: HostContainer) {
    for (edge_idx <- edge_sws_idx) {
      for (host_idx <- hosts_idx) {
        val newlink = new Link(hosts(host_idx - 2), edgeRouters(edge_idx), linkspeed)
        hosts(host_idx - 2).interfacesManager.registerOutgoingLink(newlink)
        edgeRouters(edge_idx).interfacesManager.registerIncomeLink(newlink)
        GlobalDeviceManager.addNewNode(hosts(host_idx - 2).ip_addr(0), hosts(host_idx - 2))
      }
      GlobalDeviceManager.addNewNode(edgeRouters(edge_idx).ip_addr(0), edgeRouters(edge_idx))
    }
  }
}
