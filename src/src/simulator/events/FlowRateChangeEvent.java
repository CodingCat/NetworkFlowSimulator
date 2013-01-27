package simulator.events;

import simulator.entity.NFSNode;
import simulator.entity.application.NFSFlow;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;

public class FlowRateChangeEvent extends EventOf2Entities<NFSNode, NFSFlow> {
	
	Model ownermodel = null;
	
	public FlowRateChangeEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
		ownermodel = model;
	}

	@Override
	public void eventRoutine(NFSNode dst, NFSFlow flow) {
		dst.AddNewFlow(flow);
	}
}
