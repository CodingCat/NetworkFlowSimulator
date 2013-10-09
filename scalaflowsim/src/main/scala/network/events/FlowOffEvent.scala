package network.events

import network.traffic.{CompletedFlow, Flow}
import simengine.{SimulationEngine, EventOfSingleEntity}
import application.OnOffApp
import scala.util.Random
import network.device.GlobalDeviceManager
import network.forwarding.controlplane.openflow.flowtable.OFFlowTable
import org.openflow.protocol.OFMatch
import simengine.utils.Logging

class FlowOffEvent (flow : Flow, timestamp : Double)
  extends EventOfSingleEntity[Flow] (flow, timestamp) with Logging {

  def process {
    logTrace(flow + " is off")
    if (flow.status != CompletedFlow) {
      flow.changeRate(0)
      if (flow.AppDataSize > 0) {
        //SCHEDULE next ON event
        val nextOnMoment = Random.nextInt(OnOffApp.onLength)
        SimulationEngine.addEvent(new FlowOnEvent(flow,
          SimulationEngine.currentTime + nextOnMoment))
        //reallocate resources
        GlobalDeviceManager.getNode(flow.srcIP).dataplane.reallocate(
          GlobalDeviceManager.getNode(flow.dstIP), //destination host
          flow, //offflow
          OFFlowTable.createMatchField(flow = flow,
            wcard = (OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_NW_DST_MASK & ~OFMatch.OFPFW_NW_SRC_MASK))) //matchfield
      }
    }
  }
}
