package floodlight

import scalasim.network.component.Pod
import scalasim.application.ApplicationRunner
import scalasim.simengine.SimulationEngine
import org.scalatest.FunSuite
import simengine.utils.XmlParser

class AppSuite extends FunSuite {

  test ("PermuMatrixApp can build the matrix correctly") {
    XmlParser.loadConf("config.xml")
    val cellnet = new Pod(1)
    SimulationEngine.reset()
    ApplicationRunner.reset()
    ApplicationRunner.setResource(cellnet.getAllHostsInPod)
    ApplicationRunner.installApplication()
    ApplicationRunner.run()
    assert(ApplicationRunner("PermuMatrixApp").selectedPairSize ===
      cellnet.numMachinesPerRack * cellnet.numRacks)
  }
}
