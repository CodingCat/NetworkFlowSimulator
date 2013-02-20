package simulator.events;

import simulator.entity.NFSHost;
import simulator.utils.NFSIntegerEntity;

import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;

public class NFSStartNewFlowEvent extends EventOf2Entities<NFSHost, NFSIntegerEntity> {

	public NFSStartNewFlowEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
	}

	@Override
	public void eventRoutine(NFSHost host, NFSIntegerEntity integer) {
		host.run(integer.getValue());
	}
}
