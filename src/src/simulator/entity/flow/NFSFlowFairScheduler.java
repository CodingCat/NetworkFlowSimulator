package simulator.entity.flow;

import java.util.ArrayList;

import java.util.Collections;

import desmoj.core.simulator.Model;

import simulator.entity.flow.NFSFlow.NFSFlowStatus;
import simulator.entity.topology.NFSLink;
import simulator.utils.NFSDoubleCalculator;
import simulator.entity.NFSNode;

public class NFSFlowFairScheduler extends NFSFlowScheduler {

	public NFSFlowFairScheduler(Model model, String entityName,
			boolean showInReport, ArrayList<NFSLink> links, NFSNode node) {
		super(model, entityName, showInReport, links, node);
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
		if (changedflow.getStatus() == NFSFlowStatus.CLOSED) {
			NFSFlowSchedulingAlgorithm.allocate(link, changedflow);
		}
		else {
			// this is a new flow
			// the share of others on this link might be reduced
			//changedflow.expectedrate = Math.min(changedflow.expectedrate, link.getAvrRate());
			if (link.getAvailableBandwidth() < changedflow.expectedrate) {
				@SuppressWarnings("unchecked")
				ArrayList<NFSFlow> demandingflows = (ArrayList<NFSFlow>) link.getRunningFlows().clone();
				double remainingBandwidth = link.getTotalBandwidth();
				double avrRate = link.getAvrRate();
				Collections.sort(demandingflows, NFSFlowScheduler.ratecomparator);
				while (demandingflows.size() != 0 && remainingBandwidth != 0) {
					double demand = 0.0;
					NFSFlow flow = demandingflows.get(0);
					if (flow.status.equals(NFSFlowStatus.NEWSTARTED) || flow.status.equals(NFSFlowStatus.ADJUSTING)) {
						demand = flow.expectedrate;
					} else {
						demand = flow.datarate;
					}
					if (demand < avrRate) {
						remainingBandwidth = NFSDoubleCalculator.sub(remainingBandwidth, demand);
					}
					else {
						if (flow.status.equals(NFSFlowStatus.NEWSTARTED) || flow.status.equals(NFSFlowStatus.ADJUSTING)) {
							String outstr = "change " + flow.getName() + " rate from " + flow.expectedrate + " to ";
							flow.expectedrate = avrRate;
							flow.setBottleneckLink(link);
							sendTraceNote(outstr + flow.expectedrate);
						}
						remainingBandwidth = NFSDoubleCalculator.sub(remainingBandwidth, avrRate);
					}
					demandingflows.remove(0);
					if (demandingflows.size() != 0)
						avrRate = NFSDoubleCalculator.div(remainingBandwidth, demandingflows.size());
				}
			}
			else {
				sendTraceNote("link avai bandwidth:" + link.getAvailableBandwidth() 
						+ " flow expected rate:" + changedflow.expectedrate);
				changedflow.setBottleneckLink(link);
			}
		}//end of if this flow is new
	}
}
