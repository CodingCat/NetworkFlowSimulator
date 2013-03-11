package simulator.events;

import simulator.NetworkFlowSimulator;
import simulator.entity.flow.NFSFlowSchedulingAlgorithm;
import simulator.entity.flow.NFSOFController;
import simulator.entity.flow.NFSPAFlow;
import desmoj.core.simulator.Event;
import desmoj.core.simulator.Model;

public class NFSClosePAFlowEvent extends Event<NFSPAFlow> {

	public NFSClosePAFlowEvent(Model model, String eventName, boolean showInTrace) {
		super(model, eventName, showInTrace);
	}

	@Override
	public void eventRoutine(NFSPAFlow finishedflow) {
		sendTraceNote(finishedflow.getName() + "-" + finishedflow.datarate + " finishes");
		boolean openflowonoff = NetworkFlowSimulator.parser.getBoolean(
				"fluidsim.openflow.onoff", false);
		finishedflow.finish();
		if (!openflowonoff) {
			NFSFlowSchedulingAlgorithm.rateAllocation(
					finishedflow.getSender().getTaskTracker(), 
					finishedflow.getFirstLink(), finishedflow);
		}
		else {
			NFSOFController._Instance(getModel()).finishflow(finishedflow);
		}
	}

}
