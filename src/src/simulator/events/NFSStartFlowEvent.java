package simulator.events;


import simulator.entity.NFSNode;
import simulator.entity.application.NFSFlow;
import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.EventOf3Entities;
import desmoj.core.simulator.Model;

public class NFSStartFlowEvent extends EventOf3Entities<NFSNode, NFSLink, NFSFlow> {

	public NFSStartFlowEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
	}

	@Override
	public void eventRoutine(NFSNode srcNode, NFSLink link, NFSFlow flow) {
		//1. update the link's bandwidth value
		double averageBandwidth = link.GetTotalBandwidth() / (link.GetRunningFlowsN() + 1);
		
		//2. Schedule rate change event
		
		//3. schedule receive flow event
	}
}
