package simulator.events;

import simulator.NetworkFlowSimulator;
import simulator.entity.flow.NFSFlowSchedulingAlgorithm;
import simulator.entity.flow.NFSOFController;
import simulator.entity.flow.NFSTaskBindedFlow;
//import simulator.entity.flow.NFSOFController;

import desmoj.core.simulator.Event;
import desmoj.core.simulator.Model;

public class NFSCloseMapReduceFlowEvent extends
		Event<NFSTaskBindedFlow> {
	
	public NFSCloseMapReduceFlowEvent(Model model, String eventName, boolean showInTrace) {
		super(model, eventName, showInTrace);
	}

	@Override
	public void eventRoutine(NFSTaskBindedFlow finishedflow) {
		sendTraceNote(finishedflow.getName() + "-" + finishedflow.datarate + " finishes");
		System.out.println("closing " + finishedflow.getName());
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
