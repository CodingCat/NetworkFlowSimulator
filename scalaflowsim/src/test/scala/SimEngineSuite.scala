import org.scalatest.FunSuite
import scalasim.simengine.{Event, SimulationEngine, EventOfSingleEntity}


class SimEngineSuite extends FunSuite{
  class DummySingleEntity (t : Double) extends EventOfSingleEntity[String] (t) {
    def process(entity: String) {

    }

  }


  test ("Events should be ordered with their timestamp") {
    val e1 = new DummySingleEntity(10)
    val e2 = new DummySingleEntity(5)
    val e3 = new DummySingleEntity(20)
    SimulationEngine.addEvent(e1)
    SimulationEngine.addEvent(e2)
    SimulationEngine.addEvent(e3)
    var r = -1.0
    for (e <- SimulationEngine.eventqueue) {
      assert((r < e.asInstanceOf[DummySingleEntity].getTimeStamp) === true)
      r = e.asInstanceOf[DummySingleEntity].getTimeStamp
    }
  }

}
