package network.events

import network.traffic.Flow
import simengine.{SimulationEngine, EventOfSingleEntity}
import application.OnOffApp
import scala.util.Random
import network.device.GlobalDeviceManager
import network.forwarding.controlplane.openflow.flowtable.OFFlowTable
import org.openflow.protocol.OFMatch

class FlowOffEvent (flow : Flow, timestamp : Double)
  extends EventOfSingleEntity[Flow] (flow, timestamp) {

  def process {
    println(flow + " is off")
    flow.setRate(0)
    if (flow.Demand > 0) {
      //SCHEDULE next ON event
      val nextOnMoment = Random.nextInt(OnOffApp.onLength)
      SimulationEngine.addEvent(new FlowOnEvent(flow,
        SimulationEngine.currentTime + nextOnMoment))
      //reallocate resources
      GlobalDeviceManager.getHost(flow.srcIP).dataplane.reallocate(
        GlobalDeviceManager.getHost(flow.dstIP), //destination host
        flow,//offflow
        OFFlowTable.createMatchField(flow = flow,
          wcard = (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK))) //matchfield
    } else {
      GlobalDeviceManager.getHost(flow.srcIP).dataplane.finishFlow(
        GlobalDeviceManager.getHost(flow.dstIP), //destination host
        flow,//offflow
        OFFlowTable.createMatchField(flow = flow,
          wcard = (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK))) //matchfield
    }
  }
}
