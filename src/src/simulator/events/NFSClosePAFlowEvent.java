package simulator.events;

import simulator.entity.flow.NFSPAFlow;
import desmoj.core.simulator.Event;
import desmoj.core.simulator.Model;

public class NFSClosePAFlowEvent extends Event<NFSPAFlow> {

	public NFSClosePAFlowEvent(Model model, String eventName, boolean showInTrace) {
		super(model, eventName, showInTrace);
	}

	@Override
	public void eventRoutine(NFSPAFlow finishedflow) {
		finishedflow.finish();
	}

}
