import org.scalatest.FunSuite
import scalasim.simengine.{Event, SimulationEngine, EventOfSingleEntity}


class SimEngineSuite extends FunSuite{
  class DummySingleEntity (entity : String, t : Double) extends EventOfSingleEntity[String] (entity, t) {
    def process() {

    }
  }

  test ("Events should be ordered with their timestamp") {
    val e1 = new DummySingleEntity("e1", 10)
    val e2 = new DummySingleEntity("e2", 5)
    val e3 = new DummySingleEntity("e3", 20)
    SimulationEngine.clear()
    SimulationEngine.addEvent(e1)
    SimulationEngine.addEvent(e2)
    SimulationEngine.addEvent(e3)
    var r = -1.0
    for (eventsAtMoment <- SimulationEngine.Events; e <- eventsAtMoment) {
      assert((r <= e.asInstanceOf[DummySingleEntity].getTimeStamp) === true)
      r = e.asInstanceOf[DummySingleEntity].getTimeStamp
    }
  }

  test ("Events can be rescheduled") {
    val e1 = new DummySingleEntity("e1", 10)
    val e2 = new DummySingleEntity("e2", 5)
    val e3 = new DummySingleEntity("e3", 20)
    SimulationEngine.clear()
    SimulationEngine.addEvent(e1)
    SimulationEngine.addEvent(e2)
    SimulationEngine.addEvent(e3)
    SimulationEngine.cancelEvent(e3)
    e3.setTimeStamp(1)
    SimulationEngine.addEvent(e3)
    assert(SimulationEngine.Events.size === 3)
    assert(SimulationEngine.Events.head.head === e3)
  }
}
