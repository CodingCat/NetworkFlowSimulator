package simulator.events;


import simulator.entity.NFSNode;
import simulator.entity.application.NFSFlow;
import desmoj.core.simulator.EventOf3Entities;
import desmoj.core.simulator.Model;

public class StartFlowEvent extends EventOf3Entities<NFSNode, NFSNode, NFSFlow> {

	public StartFlowEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
	}

	@Override
	public void eventRoutine(NFSNode srcNode, NFSNode dstNode, NFSFlow flow) {
		flow.Start(srcNode, dstNode);
	}
}
