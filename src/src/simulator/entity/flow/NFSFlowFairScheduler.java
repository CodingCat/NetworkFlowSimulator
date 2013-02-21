package simulator.entity.flow;

import java.util.ArrayList;

import desmoj.core.simulator.Model;

import simulator.entity.flow.NFSFlow.NFSFlowStatus;
import simulator.entity.topology.NFSLink;

public class NFSFlowFairScheduler extends NFSFlowScheduler {

	public NFSFlowFairScheduler(Model model, String entityName,
			boolean showInReport, ArrayList<NFSLink> links) {
		super(model, entityName, showInReport, links);
	}

	@Override
	/**
	 * choose the link with ECMP, 
	 */
	public NFSLink schedule(NFSFlow flow) {
		int index = (flow.srcipString + flow.dstipString).hashCode() % outlinks.size();
		this.sendTraceNote("NFSFlowFairScheduler:" + outlinks.size() + " possible links;" + 
				" selected:" + index);
		return outlinks.get(index > 0 ? index : -index);
	}
	
	@Override
	public void reallocateBandwidth(NFSLink link, NFSFlow changedflow) {
		if (changedflow.getStatus() != NFSFlowStatus.NEWSTARTED) {
			if (changedflow.getStatus() == NFSFlowStatus.CLOSED) {
				NFSFlowSchedulingAlgorithm.allocate(link, changedflow);
			}//end of if this flow is closed
		}
		else {
			// this is a new flow
			// the share of others on this link might be reduced
			if (link.getAvailableBandwidth() < changedflow.expectedrate) {
				NFSFlow[] involvedFlows = link.getRunningFlows();
				double avrRate = link.getAvrRate();
				double availablebisecBandwidth = 0.0;
				String involvedflowsstr = "";
				for (NFSFlow maychangeflow : involvedFlows) {
					involvedflowsstr += (maychangeflow.toString() + " ");
					if (maychangeflow.equals(changedflow)) continue;
					if (maychangeflow.datarate < avrRate && maychangeflow.datarate != -1.0) {
						// these flows may be bottlenecked in other links
						availablebisecBandwidth += (avrRate - maychangeflow.datarate);
					}
				}
				sendTraceNote("involved flows: " + involvedflowsstr);
				double allocatedrateOnthisLink = Math.min((avrRate + availablebisecBandwidth), changedflow.demandrate);
				if (changedflow.expectedrate == -1.0) {
					//first link
					changedflow.expectedrate = allocatedrateOnthisLink;
				}
				else {
					if (changedflow.expectedrate > allocatedrateOnthisLink) {
						changedflow.expectedrate = allocatedrateOnthisLink;
						changedflow.setBottleneckLink(link);
					}
				}
				sendTraceNote("set " + changedflow + 
						" expected rate to " + changedflow.expectedrate + 
						" avr rate:" + avrRate);
			}
		}//end of if this flow is new
	}
}
