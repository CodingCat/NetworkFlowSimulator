package simulator.events;

import simulator.entity.flow.NFSTaskBindedFlow;
import desmoj.core.simulator.Event;
import desmoj.core.simulator.Model;

public class NFSCloseMapReduceFlowEvent extends
		Event<NFSTaskBindedFlow> {

	public NFSCloseMapReduceFlowEvent(Model model, String eventName, boolean showInTrace) {
		super(model, eventName, showInTrace);
	}

	@Override
	public void eventRoutine(NFSTaskBindedFlow flow) {
		sendTraceNote(flow.getName() + " finishes");
		flow.finish();
	}

}
