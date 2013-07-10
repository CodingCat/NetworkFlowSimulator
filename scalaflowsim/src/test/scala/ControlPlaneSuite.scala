package scalasim.test

import org.scalatest.FunSuite
import network.topo.{Pod, HostContainer, ToRRouterType, Router}
import network.topo.builder.{LanBuilder, IPInstaller}
import scalasim.application.ApplicationRunner
import scalasim.simengine.SimulationEngine
import network.data.Flow

class ControlPlaneSuite extends FunSuite {
  test("flow can be routed within a rack") {
    Flow.reset()
    val torrouter = new Router(new ToRRouterType)
    val rackservers = new HostContainer()
    rackservers.create(40)
    IPInstaller.assignIPAddress(torrouter, "10.0.0.1")
    IPInstaller.assignIPAddress(torrouter.ip_addr(0), 2, rackservers, 0, rackservers.size - 1)
    LanBuilder.buildLan(torrouter, rackservers, 0, rackservers.size - 1)
    SimulationEngine.reset()
    ApplicationRunner.reset()
    ApplicationRunner.setResource(rackservers)
    ApplicationRunner.installApplication()
    ApplicationRunner("PermuMatrixApp").run()
    SimulationEngine.run()
    assert(Flow.finishedFlows.size === rackservers.size)
  }

  test("flow can be routed across racks") {
    Flow.reset()
    val pod = new Pod(1, 2, 4, 20)
    SimulationEngine.reset()
    ApplicationRunner.reset()
    ApplicationRunner.setResource(pod.getAllHostsInPod)
    ApplicationRunner.installApplication()
    ApplicationRunner("PermuMatrixApp").run()
    SimulationEngine.run()
    assert(Flow.finishedFlows.size === pod.numMachinesPerRack * pod.numRacks)
  }
}
