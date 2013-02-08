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
		return outlinks.get(index > 0 ? index : -index);
	}
	
	private boolean isConstrainedOnLink(NFSFlow flow, NFSLink candidatelink) {
		return flow.getBottleneckIdx() == flow.getLinkIdx(candidatelink);
	}
	
	@Override
	public void reallocateBandwidth(NFSLink link, NFSFlow changedflow) {
		if (changedflow.getStatus() != NFSFlowStatus.NEWSTARTED) {
			if (changedflow.getStatus() == NFSFlowStatus.CLOSED) {
				//this flow has been closed
				//triggered by the close flow event
				//rate of others might be improved
				if (link != null) {
					NFSFlow [] involvedFlows = link.getRunningFlows();
					ArrayList<NFSFlow> flowsCanBeImproved = new ArrayList<NFSFlow>();
					NFSFlow [] improvedFlowsArray = null;
					double improvedRate = changedflow.datarate;
					
					link.setAvailableBandwidth('+', changedflow.datarate);
					for (int i = 0; i < involvedFlows.length; i++) {
						if (isConstrainedOnLink(involvedFlows[i], link) && 
								(involvedFlows[i].isFullyMeet() == false)) 
							flowsCanBeImproved.add(involvedFlows[i]);
					}
					improvedFlowsArray = new NFSFlow[flowsCanBeImproved.size()];
					flowsCanBeImproved.toArray(improvedFlowsArray);
					improvedRate = link.getAvailableBandwidth() / improvedFlowsArray.length;
					for (int i = 0; i < improvedFlowsArray.length; i++) {
						if (improvedFlowsArray[i].datarate + improvedRate 
								<= improvedFlowsArray[i].demandrate) {
							improvedFlowsArray[i].datarate += improvedRate;
							link.setAvailableBandwidth('-', improvedRate);
						} else {
							link.setAvailableBandwidth('-', 
									improvedFlowsArray[i].demandrate - improvedFlowsArray[i].datarate);
							improvedFlowsArray[i].datarate = improvedFlowsArray[i].demandrate;
							improvedRate = link.getAvailableBandwidth() / (improvedFlowsArray.length - 1 - i);
						}
					}
				}
			}//end of if this flow is closed
			else {
				//TODO:adjust the rate of running flows
			}
		}
		else {
			if (link != null) {
				//this is a new flow
				//the share of others on this link might be reduced
				if (link.getAvailableBandwidth() > changedflow.expectedrate) {
					//flow.expectedrate = Math.min(flow.expectedrate, flow.demandrate);
					//do nothing
				}
				else {
					NFSFlow [] involvedFlows = link.getRunningFlows();
					double avrRate = link.getAvrRate();
					double availablebisecBandwidth = 0.0;
					double amortizedBenefit = 0.0;
					int flowsUnderShareN = 0;
					int flowsDeservingMoreShareN = 0;
					for (NFSFlow maychangeflow : involvedFlows) {
						if (maychangeflow.datarate < avrRate) {
							//these flows may be bottlenecked in other links
							flowsUnderShareN++;
							availablebisecBandwidth += (avrRate - maychangeflow.datarate);
						}
					}
					flowsDeservingMoreShareN = involvedFlows.length - flowsUnderShareN;
					amortizedBenefit = availablebisecBandwidth / flowsDeservingMoreShareN;
					changedflow.expectedrate = Math.min((avrRate + amortizedBenefit), 
							changedflow.demandrate);
				}
			}//end of if link != null
			else {
				//lastlink, we have determine the datarate of the new flow
				changedflow.datarate = changedflow.expectedrate;
				changedflow.consumeBandwidth();
				changedflow.setStatus(NFSFlowStatus.RUNNING);
			}
		}//end of if this flow is new
	}
}
