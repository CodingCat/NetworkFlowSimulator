import application.PermuMatrixApp
import network.topo.Pod
import org.scalatest.FunSuite
import scalasim.application.ApplicationRunner

class AppSuite extends FunSuite {

  test ("PermuMatrixApp can build the matrix correctly") {
    val cellnet = new Pod(1)
    ApplicationRunner.setResource(cellnet.getAllHostsInPod)
    ApplicationRunner.installApplication()
    ApplicationRunner.run()
    assert(ApplicationRunner("PermuMatrixApp").asInstanceOf[PermuMatrixApp].selectedPairSize ===
      cellnet.numMachinesPerRack * cellnet.numRacks)
  }
}