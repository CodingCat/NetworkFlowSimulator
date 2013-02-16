package simulator.events;


import simulator.entity.NFSNode;
import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;

import desmoj.core.simulator.EventOf3Entities;
import desmoj.core.simulator.Model;

public class NFSFlowRateChangeEvent extends EventOf3Entities<NFSNode, NFSLink, NFSFlow> {

	public NFSFlowRateChangeEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
	}
	
	@Override
	public void eventRoutine(NFSNode node, NFSLink link, NFSFlow changedflow) {
		sendTraceNote("a flow on " + link + " changes causing rate change");
		node.changeResourceAllocation(link, changedflow);
		NFSLink nextlink = changedflow.getNextLink(link);
		if (nextlink != null) {
			schedule(nextlink.dst, nextlink, changedflow, presentTime());
		}
		else {
			//if we have determined expected rate of the flow
			if (changedflow.getStatus().equals(NFSFlow.NFSFlowStatus.NEWSTARTED)) {
				//if this flow is a new started flow
				changedflow.start();
			}
			else {
				if (changedflow.getStatus().equals(NFSFlow.NFSFlowStatus.CLOSED)) {
					//if this flow is just closed
					changedflow.close();
				}
			}
		}
	}//end of eventRoutine
}
