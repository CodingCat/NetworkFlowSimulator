package scalasim.test

import org.scalatest.FunSuite
import network.topo.{Pod, HostContainer, ToRRouterType, Router}
import network.topo.builder.{LanBuilder, IPInstaller}
import scalasim.application.ApplicationRunner
import scalasim.simengine.SimulationEngine
import network.data._
import scala.collection.mutable.ListBuffer
import network.topo.ToRRouterType
import scala.util.Sorting
import scalasim.SimulationRunner

class ControlPlaneSuite extends FunSuite {

  test("flow can be routed within a rack") {
    SimulationRunner.reset
val torrouter = new Router(new ToRRouterType)
    val rackservers = new HostContainer()
    rackservers.create(40)
    IPInstaller.assignIPAddress(torrouter, "10.0.0.1")
    IPInstaller.assignIPAddress(torrouter.ip_addr(0), 2, rackservers, 0, rackservers.size - 1)
    LanBuilder.buildLan(torrouter, rackservers, 0, rackservers.size - 1)
    ApplicationRunner.setResource(rackservers)
    ApplicationRunner.installApplication()
    ApplicationRunner("PermuMatrixApp").run()
    SimulationEngine.run()
    assert(Flow.finishedFlows.size === rackservers.size)
  }

  test("flow can be routed across racks") {
    SimulationRunner.reset
    val pod = new Pod(1, 2, 4, 20)
    ApplicationRunner.setResource(pod.getAllHostsInPod)
    ApplicationRunner.installApplication()
    ApplicationRunner("PermuMatrixApp").run()
    SimulationEngine.run()
    assert(Flow.finishedFlows.size === pod.numMachinesPerRack * pod.numRacks)
  }

  test ("flows in a collection can be ordered according to their rate or temprate") {
    var flowset = new ListBuffer[Flow]
    flowset += new Flow("10.0.0.1", "10.0.0.2", 1000)
    flowset(0).status = RunningFlow
    flowset(0).changeRate('+', 10)
    flowset += new Flow("10.0.0.2", "10.0.0.3", 1000)
    flowset(1).status = NewStartFlow
    flowset(1).changeTempRate('-', Double.MaxValue)
    flowset(1).changeTempRate('+', 5)
    flowset += new Flow("10.0.0.3", "10.0.0.4", 1000)
    flowset(2).status = RunningFlow
    flowset(2).changeRate('+', 20)
    flowset = flowset.sorted(FlowRateOrdering)
    assert(flowset(0).SrcIP === "10.0.0.2")
    assert(flowset(1).SrcIP === "10.0.0.1")
    assert(flowset(2).SrcIP === "10.0.0.3")
  }

  test("flow can be allocated with correct bandwidth (within the same rack)") {
    val pod = new Pod(1, 0, 1, 2)
    SimulationRunner.reset
    ApplicationRunner.setResource(pod.getAllHostsInPod)
    ApplicationRunner.installApplication()
    ApplicationRunner("PermuMatrixApp").insertTrafficPair("10.1.0.2", "10.1.0.3")
    ApplicationRunner("PermuMatrixApp").insertTrafficPair("10.1.0.3", "10.1.0.2")
    ApplicationRunner("PermuMatrixApp").run()
    SimulationEngine.run()
    for (flow <- Flow.finishedFlows) assert(flow.Rate === 50)
  }
}
