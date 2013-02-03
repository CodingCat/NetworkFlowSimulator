package simulator.events;


import simulator.entity.NFSNode;
import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.EventOf3Entities;
import desmoj.core.simulator.Model;

public class NFSStartFlowEvent extends EventOf3Entities<NFSNode, NFSLink, NFSFlow> {

	public NFSStartFlowEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
	}

	@Override
	public void eventRoutine(NFSNode srcNode, NFSLink link, NFSFlow flow) {

	}
}
