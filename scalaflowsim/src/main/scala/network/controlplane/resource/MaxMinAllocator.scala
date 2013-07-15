package network.controlplane.resource

import network.data._
import network.component.{Node, Link}
import simengine.utils.Logging
import network.controlplane.ControlPlane

private [controlplane] class MaxMinAllocator (controlPlane : ControlPlane)
  extends ResourceAllocator (controlPlane) with Logging {

  override def allocateForNewFlow (flow: Flow, link: Link) = {
    var demandingflows = linkFlowMap(link).clone()
    var remainingBandwidth = link.bandwidth
    var avrRate = link.bandwidth / linkFlowMap(link).size
    logDebug("avrRate on " + link + " is " + avrRate)
    demandingflows = demandingflows.sorted(FlowRateOrdering)
    while (demandingflows.size != 0 && remainingBandwidth != 0) {
      val currentflow = demandingflows.head
      //initialize for the new flow
      if (currentflow.getTempRate == Double.MaxValue) currentflow.setTempRate(link.bandwidth)
      var demand = {
        if (currentflow.status != NewStartFlow) currentflow.Rate else currentflow.getTempRate
      }
      logDebug("demand of flow " + currentflow + " is " + demand)
      if (demand < avrRate) {
        remainingBandwidth -= demand
      } else {
        if (currentflow.status != NewStartFlow) {
          currentflow.changeRate('-', currentflow.Rate - avrRate)
          logDebug("change flow " + currentflow + " rate to " + currentflow.Rate)
        } else {
          currentflow.setTempRate(avrRate) //set to avrRate
          logDebug("change flow " + currentflow + " temprate to " + currentflow.getTempRate)
        }
        remainingBandwidth -= avrRate
      }
      demandingflows.remove(0)
      if (demandingflows.size != 0) avrRate = remainingBandwidth / demandingflows.size
    }
  }

  override def reallocate(link: Link) {
    var demandingflows = linkFlowMap(link).clone()
    var remainingBandwidth = link.bandwidth
    var avrRate = link.bandwidth / linkFlowMap(link).size
    logDebug("avrRate on " + link + " is " + avrRate)
    demandingflows.map(f => {f.status = ChangingRateFlow; f.setTempRate(avrRate)})
    demandingflows = demandingflows.sorted(FlowRateOrdering)
    while (demandingflows.size != 0 && remainingBandwidth != 0) {
      val currentflow = demandingflows.head
      if (currentflow.status != RunningFlow)
        throw new Exception("status error, reallocate bandwidth can only be available to running flows, " +
          "the flow status is: " + currentflow.status)
      controlPlane.allocateForNewFlow(currentflow)
      demandingflows.remove(0)
      if (demandingflows.size != 0) avrRate = remainingBandwidth / demandingflows.size
    }
  }
}
