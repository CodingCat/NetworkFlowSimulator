package scalasim.network.component.builder

import simengine.utils.XmlParser
import network.device._


object LanBuilder {

  /**
   * build a small lan within a rack one router and multiples hosts
   * @param router the tor router
   * @param hosts the servers set
   * @param startIdx the index of the first server to be connected
   * @param endIdx the index of the last server to be connected
   */
  def buildLan(router: Router, hosts : HostContainer, startIdx : Int, endIdx : Int) {
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

  def buildLan(aggRouter: Router, routers : RouterContainer, startIdx : Int, endIdx : Int) {
    try {
      var crossrouterlinkBandwidth = XmlParser.getDouble("scalasim.topology.crossrouterlinkrate", 1000.0)
      if (aggRouter.ip_addr.length == 0) throw new RuntimeException("AggRouter hasn't got ipaddress")
      for (i <- startIdx to endIdx) {
        if (routers(i).ip_addr.length == 0) {
          throw new RuntimeException("ToRRouters haven't got ipaddress, the idx is " + i)
        }
        val newlink = new Link(routers(i), aggRouter, crossrouterlinkBandwidth)
        routers(i).interfacesManager.registerOutgoingLink(newlink)
        aggRouter.interfacesManager.registerIncomeLink(newlink)
        GlobalDeviceManager.addNewNode(routers(i).ip_addr(0), routers(i))
      }
      GlobalDeviceManager.addNewNode(aggRouter.ip_addr(0), aggRouter)
    }
    catch {
      case ex : RuntimeException => throw ex
    }
  }
}