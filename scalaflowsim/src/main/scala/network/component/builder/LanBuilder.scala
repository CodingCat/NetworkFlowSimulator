package network.component.builder

import scalasim.XmlParser
import network.component.{RouterContainer, Router, Link, HostContainer}


object LanBuilder {

  def buildLan(router: Router, hosts : HostContainer, startIdx : Int, endIdx : Int) {
    try {
      var locallinkBandwidth = XmlParser.getDouble("scalasim.topology.locallinkrate", 100.0)
      if (router.ip_addr.length == 0) throw new RuntimeException("Engress Router hasn't got ipaddress")
      for (i <- startIdx to endIdx) {
        if (hosts(i).ip_addr.length == 0)  {
          throw new RuntimeException("Hosts haven't got ipaddress, the idx is " + i)
        }
        val newlink = new Link(hosts(i), router, locallinkBandwidth)
        hosts(i).controlPlane.registerOutgoingLink(newlink)
        router.controlPlane.registerIncomeLink(newlink)
      }
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
        routers(i).controlPlane.registerOutgoingLink(newlink)
        aggRouter.controlPlane.registerIncomeLink(newlink)
      }
    }
    catch {
      case ex : RuntimeException => throw ex
    }
  }
}