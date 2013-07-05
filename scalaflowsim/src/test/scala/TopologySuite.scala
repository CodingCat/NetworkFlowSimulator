package scalasim.test

import network.topo._
import network.topo.builder.{LanBuilder, IPInstaller}
import org.scalatest.FunSuite

class TopologySuite extends FunSuite {

  test("IPInstaller can assign IPs to a host/router") {
     val host : Host = new Host()
     IPInstaller.assignIPAddress(host, "10.0.0.1")
     assert(host.ip_addr.length == 1 && host.ip_addr(0) == "10.0.0.1")
     IPInstaller.assignIPAddress(host, "10.0.0.2")
     assert(host.ip_addr.length == 2 && host.ip_addr(0) == "10.0.0.1" && host.ip_addr(1) == "10.0.0.2")
  }

  test("IPInstaller can assign IPs to host/router container") {
    val hostContainer : HostContainer = new HostContainer()
    val router : Router = new Router(new ToRRouterType)
    IPInstaller.assignIPAddress(router, "10.0.0.1")
    hostContainer.create(10)
    IPInstaller.assignIPAddress(router.ip_addr(router.ip_addr.length - 1), 2, hostContainer, 0, 9)
    for (i <- 0 until 10) {
      assert(hostContainer(i).ip_addr(0) === "10.0.0." + (i + 2))
    }
  }

  test("LanBuilder cannot build the lan for router-hosts before all involved elements are assigned with IP addresses") {
    val router : Router = new Router(new ToRRouterType)
    val hostContainer = new  HostContainer()
    hostContainer.create(10)
    var exception = intercept[RuntimeException] {
      LanBuilder.buildLan(router, hostContainer, 0, 9)
    }
    router.assignIP("10.0.0.1")
    exception = intercept[RuntimeException] {
      LanBuilder.buildLan(router, hostContainer, 0, 9)
    }
  }

  test("LanBuilder cannot build the lan for router-routers before all involved elements are assigned with IP addresses") {
    val router : Router = new Router(new AggregateRouterType)
    val routerContainer = new RouterContainer()
    routerContainer.create(10, new ToRRouterType)
    var exception = intercept[RuntimeException] {
      LanBuilder.buildLan(router, routerContainer, 0, 9)
    }
    router.assignIP("10.0.0.1")
    exception = intercept[RuntimeException] {
      LanBuilder.buildLan(router, routerContainer, 0, 9)
    }
  }

  test("LanBuilder should be able to create the local area network for router-hosts") {
    val router : Router = new Router(new ToRRouterType)
    val hosts : HostContainer = new HostContainer()
    IPInstaller.assignIPAddress(router, "10.0.0.1")
    hosts.create(10)
    IPInstaller.assignIPAddress(router.ip_addr(0), 2, hosts, 0, 9)
    LanBuilder.buildLan(router, hosts, 0, 9)
    for (i <- 0 to 9) {
      val hostOutLink = hosts(i).outlink.get("10.0.0.1")
      //check host outlink
      assert(router.ip_addr(0) === hostOutLink.get.end_to.ip_addr(0))
      //check router inlink
      assert(router.inLinks.get(hosts(i).ip_addr(0)).get.end_from === hosts(i))
    }
  }

  test("LanBuilder should be able to create the local area network for router-routers") {
    val aggRouter : Router = new Router(new AggregateRouterType)
    val routers : RouterContainer = new RouterContainer()
    IPInstaller.assignIPAddress(aggRouter, "10.0.0.1")
    routers.create(10, new ToRRouterType)
    IPInstaller.assignIPAddress(aggRouter.ip_addr(0), 2, routers, 0, 9)
    LanBuilder.buildLan(aggRouter, routers, 0, 9)
    for (i <- 0 to 9) {
      val routerOutlink = routers(i).outlink.get("10.0.0.1")
      //check tor routers outlink
      assert(aggRouter.ip_addr(0) === routerOutlink.get.end_to.ip_addr(0))
      //check aggregate router inlink
      assert(aggRouter.inLinks.get(routers(i).ip_addr(0)).get.end_from === routers(i))
    }
  }

  test("Pod network can be created correctly") {
    val cellnet = new Pod(1)
    //check aggregate routers's inlinks
    for (i <- 0 until cellnet.numAggRouters; j <- 0 until cellnet.numRacks) {
      val aggrouter = cellnet.getAggregatRouter(i)
      assert(aggrouter.inLinks.contains("10.1." + j + ".1") === true)
    }
    //check tor router's inlinks and outlinks
    for (i <- 0 until cellnet.numRacks; j <- 0 until cellnet.numMachinesPerRack) {
      val torrouter = cellnet.getToRRouter(i)
      //check inlinks
      assert(torrouter.inLinks.contains("10.1." + i + "." + (j + 2)) === true)
    }
    for (i <- 0 until cellnet.numRacks; j <- 0 until cellnet.numAggRouters) {
      val torrouter = cellnet.getToRRouter(i)
      //check outlink
      assert(torrouter.outlink.contains("10.1." + (cellnet.numRacks  + j).toString + ".1"))
    }
    //check hosts outlinks
    for (i <- 0 until cellnet.numRacks; j <- 0 until cellnet.numMachinesPerRack) {
      assert(cellnet.getHost(i, j).outlink.contains("10.1." + i + ".1") === true)
    }
  }
}
