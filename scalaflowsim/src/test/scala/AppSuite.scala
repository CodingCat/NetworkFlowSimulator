package scalasim.test

import network.topo.Pod
import scalasim.application.ApplicationRunner
import scalasim.simengine.SimulationEngine
import scalasim.XmlParser
import application.PermuMatrixApp
import org.scalatest.FunSuite

class AppSuite extends FunSuite {

  test ("PermuMatrixApp can build the matrix correctly") {
    XmlParser.loadConf("config.xml")
    val cellnet = new Pod(1)
    SimulationEngine.reset()
    ApplicationRunner.reset()
    ApplicationRunner.setResource(cellnet.getAllHostsInPod)
    ApplicationRunner.installApplication()
    ApplicationRunner.run()
    assert(ApplicationRunner("PermuMatrixApp").asInstanceOf[PermuMatrixApp].selectedPairSize ===
      cellnet.numMachinesPerRack * cellnet.numRacks)
  }
}
