package simulator.events;

import simulator.entity.NFSHost;

import desmoj.core.simulator.Event;
import desmoj.core.simulator.Model;

public class NFSStartNewFlowEvent extends Event<NFSHost> {

	public NFSStartNewFlowEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
	}

	@Override
	public void eventRoutine(NFSHost host) {
		host.run();
	}

}
