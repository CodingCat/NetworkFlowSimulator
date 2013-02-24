package simulator.entity.flow;

import java.util.ArrayList;

import desmoj.core.simulator.Model;

import simulator.entity.flow.NFSFlow.NFSFlowStatus;
import simulator.entity.topology.NFSLink;
import simulator.utils.NFSDoubleCalculator;

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
			//changedflow.expectedrate = Math.min(changedflow.expectedrate, link.getAvrRate());
			if (link.getAvailableBandwidth() < changedflow.expectedrate) {
				NFSFlow[] involvedFlows = link.getRunningFlows();
				double avrRate = link.getAvrRate();
				double unusedbandwidth = 0.0;//link.getAvailableBandwidth();
				String involvedflowsstr = "";
				int goodguysnum = 0;
				for (NFSFlow maychangeflow : involvedFlows) {
					involvedflowsstr += (maychangeflow.toString() + "-" + maychangeflow.datarate);
					if (maychangeflow.equals(changedflow)) continue;
					if (maychangeflow.datarate < avrRate) {
						// these flows may be bottlenecked in other links
						double a = NFSDoubleCalculator.sub(avrRate, maychangeflow.datarate);
						unusedbandwidth =  NFSDoubleCalculator.sum(unusedbandwidth, a);
						goodguysnum++;
					}
				}
				sendTraceNote("involved flows: " + involvedflowsstr);
				int greedyflowsnum = involvedFlows.length - goodguysnum;  
				double allocatedrateOnthisLink = Math.min(
						NFSDoubleCalculator.sum(avrRate, 
								NFSDoubleCalculator.div(unusedbandwidth, (double)(greedyflowsnum))), 
						changedflow.demandrate);
				sendTraceNote("local allocation:" + allocatedrateOnthisLink);
				if (changedflow.expectedrate > allocatedrateOnthisLink) {
					changedflow.expectedrate = allocatedrateOnthisLink;
					changedflow.setBottleneckLink(link);
				}
				sendTraceNote("set " + changedflow + 
						" expected rate to " + changedflow.expectedrate + 
						" avr rate:" + avrRate);
			}
			else {
				sendTraceNote("link avai bandwidth:" + link.getAvailableBandwidth() 
						+ " flow expected rate:" + changedflow.expectedrate);
				changedflow.setBottleneckLink(link);
			}
		}//end of if this flow is new
	}
}
