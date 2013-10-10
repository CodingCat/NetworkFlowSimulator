package scalasim.network.component.builder

import simengine.utils.XmlParser
import network.topology._


object LanBuilder {

  /**
   * build a small lan within a rack one router and multiples hosts
   * @param router the tor router
   * @param hosts the servers set
   * @param startIdx the index of the first server to be connected
   * @param endIdx the index of the last server to be connected
   */
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


  private def connectRouters (highLevelRouters : Router, lowlevelRouters : RouterContainer, startIdx : Int, endIdx : Int) {
    try {
      var crossrouterlinkBandwidth = XmlParser.getDouble("scalasim.topology.crossrouterlinkrate", 1000.0)
      if (highLevelRouters.ip_addr.length == 0) throw new RuntimeException("AggRouter hasn't got ipaddress")
      for (i <- startIdx to endIdx) {
        if (lowlevelRouters(i).ip_addr.length == 0) {
          throw new RuntimeException("ToRRouters haven't got ipaddress, the idx is " + i)
        }
        val newlink = new Link(lowlevelRouters(i), highLevelRouters, crossrouterlinkBandwidth)
        lowlevelRouters(i).interfacesManager.registerOutgoingLink(newlink)
        highLevelRouters.interfacesManager.registerIncomeLink(newlink)
        GlobalDeviceManager.addNewNode(lowlevelRouters(i).ip_addr(0), lowlevelRouters(i))
      }
      GlobalDeviceManager.addNewNode(highLevelRouters.ip_addr(0), highLevelRouters)
    } catch {
      case ex : RuntimeException => throw ex
    }
  }

  def buildPod(aggRouter: Router, routers : RouterContainer, startIdx : Int, endIdx : Int) {
    connectRouters(aggRouter, routers, startIdx, endIdx)
  }

  def buildCore(coreRouter : Router, pods : Pod *)  {
    try {
      if (coreRouter.ip_addr.length == 0) {
        throw new RuntimeException("CoreRouters haven't got ipaddress")
      }
      val routerContainer = new RouterContainer()
      pods.foreach(pod => {
        val aggrouternum = pod.numAggRouters
        for (i <- 0 to aggrouternum) {
          routerContainer.addNode(pod.getAggregatRouter(i))
        }
      })
      connectRouters(coreRouter, routerContainer, 0, routerContainer.size())
    } catch {
      case ex: RuntimeException => throw ex
    }
  }
}