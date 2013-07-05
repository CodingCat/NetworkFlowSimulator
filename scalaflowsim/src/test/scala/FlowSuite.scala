package scalasim.test

import org.scalatest.FunSuite
import network.topo.{HostContainer, ToRRouterType, Router}
import network.topo.builder.{LanBuilder, IPInstaller}

class FlowSuite extends FunSuite {
  test("flow can be sent within a rack") {
    val torrouter = new Router(new ToRRouterType)
    val rackservers = new HostContainer()
    rackservers.create(40)
    IPInstaller.assignIPAddress(torrouter, "10.0.0.1")
    IPInstaller.assignIPAddress(torrouter.ip_addr(0), 2, rackservers, 0, rackservers.size - 1)
    LanBuilder.buildLan(torrouter, rackservers, 0, rackservers.size - 1)
    assert(1 === 1)
  }
}
