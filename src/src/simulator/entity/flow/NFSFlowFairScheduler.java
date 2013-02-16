package simulator.entity.flow;

import java.util.ArrayList;
import java.util.Collections;

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
				//this flow has been closed
				//triggered by the close flow event
				//rate of others might be improved
				if (link != null) {
					link.removeRunningFlow(changedflow);
					
					NFSFlow [] involvedFlows = link.getRunningFlows();
					ArrayList<NFSFlow> flowsCanBeImproved = new ArrayList<NFSFlow>();
					NFSFlow [] improvedFlowsArray = null;
					double improvedRate = changedflow.datarate;
					
					link.setAvailableBandwidth('+', changedflow.datarate);
					System.out.println(involvedFlows.length);
					for (int i = 0; i < involvedFlows.length; i++) {
						if ((involvedFlows[i].getBottleneckLink() == null || !involvedFlows[i].getBottleneckLink().equals(link))
							&& (involvedFlows[i].isFullyMeet() == false)) 
							flowsCanBeImproved.add(involvedFlows[i]);
					}
					sendTraceNote("flowsCanBeImproved:" + flowsCanBeImproved.size());
					improvedFlowsArray = new NFSFlow[flowsCanBeImproved.size()];
					Collections.sort(flowsCanBeImproved, new NFSFlowComparator());
					flowsCanBeImproved.toArray(improvedFlowsArray);
					improvedRate = link.getAvailableBandwidth() / improvedFlowsArray.length;
					for (int i = 0; i < improvedFlowsArray.length; i++) {
						if (improvedFlowsArray[i].datarate + improvedRate 
								<= improvedFlowsArray[i].demandrate) {
							improvedFlowsArray[i].update('+', improvedRate);
							link.setAvailableBandwidth('-', improvedRate);
						} else {
							link.setAvailableBandwidth('-', 
									improvedFlowsArray[i].demandrate - improvedFlowsArray[i].datarate);
							improvedFlowsArray[i].update('+', 
									improvedFlowsArray[i].demandrate - improvedFlowsArray[i].datarate);
							improvedRate = link.getAvailableBandwidth() / (improvedFlowsArray.length - 1 - i);
						}
						sendTraceNote("set " + improvedFlowsArray[i] + 
								" datarate to " + improvedFlowsArray[i].datarate);
					}
				}
			}//end of if this flow is closed
			else {
				//TODO:adjust the rate of running flows
			}
		}
		else {
			// this is a new flow
			// the share of others on this link might be reduced
			if (link.getAvailableBandwidth() > changedflow.expectedrate) {
				// flow.expectedrate = Math.min(flow.expectedrate,
				// flow.demandrate);
				// do nothing
			} else {
				NFSFlow[] involvedFlows = link.getRunningFlows();
				double avrRate = link.getAvrRate();
				double availablebisecBandwidth = 0.0;
				double amortizedBenefit = 0.0;
				int flowsUnderShareN = 0;
				int flowsDeservingMoreShareN = 0;
				for (NFSFlow maychangeflow : involvedFlows) {
					if (maychangeflow.equals(changedflow)) continue;
					if (maychangeflow.datarate < avrRate) {
						// these flows may be bottlenecked in other links
						flowsUnderShareN++;
						availablebisecBandwidth += (avrRate - maychangeflow.datarate);
					}
				}
				flowsDeservingMoreShareN = involvedFlows.length
						- flowsUnderShareN;
				//sendTraceNote("involved flows length:" + involvedFlows.length +  
					//	" flowsDeservingMoreShareN:" + flowsDeservingMoreShareN);
				amortizedBenefit = availablebisecBandwidth
						/ flowsDeservingMoreShareN;
				double allocatedrateOnthisLink = Math.min((avrRate + amortizedBenefit), changedflow.demandrate);
				if (changedflow.expectedrate == -1.0 || (changedflow.expectedrate > allocatedrateOnthisLink)) {
					changedflow.expectedrate = allocatedrateOnthisLink;
					if (changedflow.expectedrate != -1.0) {
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
