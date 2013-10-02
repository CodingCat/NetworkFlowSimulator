package network.events

import simengine.{SimulationEngine, EventOfSingleEntity}
import network.traffic.Flow
import scala.util.Random
import application.OnOffApp
import network.device.GlobalDeviceManager
import network.forwarding.controlplane.openflow.flowtable.OFFlowTable
import org.openflow.protocol.OFMatch
import simengine.utils.Logging


class FlowOnEvent (flow : Flow, timestamp : Double)
  extends EventOfSingleEntity[Flow] (flow, timestamp) with Logging {

  def process {
    println(flow + " is on")
    //scheduler off event
    val nextOffMoment = Random.nextInt(OnOffApp.offLength)
    SimulationEngine.addEvent(new FlowOffEvent(flow,
      SimulationEngine.currentTime + nextOffMoment))
    flow.demand = Double.MaxValue
    //reallocate resources
    GlobalDeviceManager.getHost(flow.srcIP).dataplane.reallocate(
      GlobalDeviceManager.getHost(flow.dstIP), //destination host
      flow,//offflow
      OFFlowTable.createMatchField(flow = flow,
        wcard = (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK))) //matchfield
  }
}
