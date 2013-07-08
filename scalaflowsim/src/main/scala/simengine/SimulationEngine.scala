package scalasim.simengine

import scala.collection.mutable.PriorityQueue
import network.events.StartNewFlowEvent
import scala.collection.mutable
import network.topo.Link
import network.data.Flow


object SimulationEngine {

  implicit object EventOrder extends Ordering[Event] {
    def compare(x : Event, y : Event) = x.getTimeStamp > y.getTimeStamp match {
      case true => -1
      case false => {
        x.getTimeStamp == y.getTimeStamp match {
          case true => 0
          case false => 1
        }
      }
    }
  }

  val eventqueue = new PriorityQueue[Event]()(EventOrder)

  def run() = for (event <- eventqueue) event.process

  def addEvent(e : Event) = eventqueue += e

}



