package simulator.events;

import simulator.entity.NFSHost;
import simulator.entity.flow.NFSFlow;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;

public class NFSOpenFlowSubscribeEvent extends EventOf2Entities<NFSHost, NFSFlow> {

	public NFSOpenFlowSubscribeEvent(Model model, String eventName, boolean showInTrace) {
		super(model, eventName, showInTrace);
	}

	@Override
	public void eventRoutine(NFSHost host, NFSFlow flow) {
		host.subscribe(flow);
	}

}
