package simulator.events;

import java.util.ArrayList;

import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSFlow.NFSFlowStatus;
import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;

public class NFSFlowRateChangeEvent extends EventOf2Entities<NFSLink, NFSFlow> {

	public NFSFlowRateChangeEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
	}
	
	private boolean isConstrainedOnLink(NFSFlow flow, NFSLink candidatelink) {
		return flow.getBottleneckIdx() == flow.getLinkIdx(candidatelink);
	}

	@Override
	public void eventRoutine(NFSLink link, NFSFlow flow) {
		if (flow.getStatus() != NFSFlowStatus.NEWSTARTED) {
			if (flow.getStatus() == NFSFlowStatus.CLOSED) {
				//this flow has been closed
				//triggered by the close flow event
				//rate of others might be improved
				NFSFlow [] involvedFlows = link.getRunningFlows();
				ArrayList<NFSFlow> flowsCanBeImproved = new ArrayList<NFSFlow>();
				NFSFlow [] improvedFlowsArray = null;
				double improvedRate = flow.datarate;
				
				link.setAvailableBandwidth('+', flow.datarate);
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
				schedule(flow.getNextLink(link), flow, new TimeInstant(0));
			}//end of if this flow is closed
		}
		else {
			//this is a new flow
			//the share of others on this link might be reduced
			NFSLink nextlink = null;
			if (link.getAvailableBandwidth() > flow.expectedrate) {
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
				flow.expectedrate = Math.min((avrRate + amortizedBenefit), flow.demandrate);
			}
			nextlink = flow.getNextLink(link);
			if (nextlink != null) {
				//not the last link
				schedule(nextlink, flow, new TimeInstant(0));
			}
			else{
				//lastlink, we have determine the datarate of the new flow
				flow.datarate = flow.expectedrate;
				flow.consumeBandwidth();
				flow.setStatus(NFSFlowStatus.RUNNING);
			}
		}//end of if this flow is new
	}//end of eventRoutine
}
