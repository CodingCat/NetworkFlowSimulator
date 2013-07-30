package scalasim.network.controlplane.resource

import scalasim.network.traffic._
import scalasim.network.controlplane.TCPControlPlane
import scalasim.network.controlplane.routing.RoutingProtocol
import scalasim.network.component.Link
import scalasim.simengine.utils.Logging

private [controlplane] class MaxMinAllocator (controlPlane : TCPControlPlane)
  extends ResourceAllocator (controlPlane) with Logging {

  override def allocate (link: Link) {
    if (linkFlowMap(link).size == 0) return
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
        if (currentflow.status != RunningFlow) currentflow.getTempRate else currentflow.Rate
      }
      logDebug("demand of flow " + currentflow + " is " + demand)
      if (demand <= avrRate) {
        remainingBandwidth -= demand
      } else {
        if (currentflow.status == RunningFlow) {
          currentflow.setRate(avrRate)
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
    if (linkFlowMap(link).size == 0) return
    var demandingflows = linkFlowMap(link).clone()
    var remainingBandwidth = link.bandwidth
    var avrRate = link.bandwidth / linkFlowMap(link).size
    logDebug("avrRate on " + link + " is " + avrRate)
    demandingflows.map(f => {f.status = ChangingRateFlow; f.setTempRate(avrRate)})
    demandingflows = demandingflows.sorted(FlowRateOrdering)
    while (demandingflows.size != 0 && remainingBandwidth != 0) {
      val currentflow = demandingflows.head
      RoutingProtocol.getFlowStarter(currentflow.SrcIP).controlPlane.allocate(currentflow)
      demandingflows.remove(0)
      if (demandingflows.size != 0) avrRate = remainingBandwidth / demandingflows.size
    }
  }
}
