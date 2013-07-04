package network.topo

import scalasim.network.{RouterContainer, Link, HostContainer, Router}
import scalasim.XmlParser


object LanBuilder {
  def buildLan(router: Router, hosts : HostContainer, startIdx : Int, endIdx : Int) {
    try {
      if (router.ip_addr.length == 0) throw new RuntimeException("Engress Router hasn't got ipaddress")
      for (i <- startIdx to endIdx) {
        if (hosts(i).ip_addr.length == 0)  {
          throw new RuntimeException("Hosts haven't got ipaddress, the idx is " + i)
        }
        val newlink = new Link(hosts(i), router, XmlParser.getDouble("scalasim.topology.locallinkrate", 100.0))
        hosts(i).addLink(newlink)
        router.registerIncomeLink(newlink)
      }
    }
    catch {
      case ex : RuntimeException => throw ex
    }
  }

  def buildLan(aggRouter: Router, routers : RouterContainer, startIdx : Int, endIdx : Int) {
    try {
      if (aggRouter.ip_addr.length == 0) throw new RuntimeException("AggRouter hasn't got ipaddress")
      for (i <- startIdx to endIdx) {
        if (routers(i).ip_addr.length == 0) {
          throw new RuntimeException("ToRRouters haven't got ipaddress, the idx is " + i)
        }
        val newlink = new Link(routers(i), aggRouter,
          XmlParser.getDouble("scalasim.topology.crossrouterlinkrate", 1000.0))
        routers(i).addLink(newlink)
        aggRouter.registerIncomeLink(newlink)
      }
    }
    catch {
      case ex : RuntimeException => throw ex
    }
  }
}