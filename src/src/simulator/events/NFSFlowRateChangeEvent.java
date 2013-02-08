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
		node.changeResourceAllocation(link, changedflow);
		NFSLink nextlink = changedflow.getNextLink(link);
		schedule(node, nextlink, changedflow, presentTime());
	}//end of eventRoutine
}
