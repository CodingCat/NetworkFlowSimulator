package network.controlplane.resource

import network.data.{RunningFlow, FlowRateOrdering, NewStartFlow, Flow}
import network.topo.{Node, Link}
import simengine.utils.Logging

private [controlplane] class MaxMinAllocator (node : Node) extends ResourceAllocator (node) with Logging {

  override def allocateForNewFlow (flow: Flow, link: Link) = {
    if (flow.status == NewStartFlow) {
      var demandingflows = linkFlowMap(link).clone()
      var remainingBandwidth = link.bandwidth
      var avrRate = link.bandwidth / linkFlowMap(link).size
      logDebug("avrRate on " + link + " is " + avrRate)
      demandingflows = demandingflows.sorted(FlowRateOrdering)
      while (demandingflows.size != 0 && remainingBandwidth != 0) {
        val currentflow = demandingflows.head
        //initialize for the new flow
        if (currentflow.getTempRate == Double.MaxValue) currentflow.setTempRate(link.bandwidth)
        var demand = {if (currentflow.status == RunningFlow) currentflow.Rate else currentflow.getTempRate}
        logDebug("demand of flow " + currentflow + " is " + demand)
        if (demand < avrRate) {
          remainingBandwidth -= demand
        } else {
          if (currentflow.status == RunningFlow) {
            currentflow.changeRate('-', currentflow.Rate - avrRate)
            logDebug("change flow " + currentflow + " rate to " + currentflow.Rate)
          } else {
            currentflow.setTempRate(avrRate)//set to avrRate
            logDebug("change flow " + currentflow + " temprate to " + currentflow.getTempRate)
          }
          remainingBandwidth -= avrRate
        }
        demandingflows.remove(0)
        if (demandingflows.size == 0) avrRate = remainingBandwidth / demandingflows.size
      }
    }
  }
}
