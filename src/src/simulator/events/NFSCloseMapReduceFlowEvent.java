package simulator.events;

import simulator.NetworkFlowSimulator;
import simulator.entity.flow.NFSOFController;
import simulator.entity.flow.NFSTaskBindedFlow;
//import simulator.entity.flow.NFSOFController;

import desmoj.core.simulator.Event;
import desmoj.core.simulator.Model;

public class NFSCloseMapReduceFlowEvent extends
		Event<NFSTaskBindedFlow> {
	
//	private NFSOFController controller = null;

	public NFSCloseMapReduceFlowEvent(Model model, String eventName, boolean showInTrace) {
		super(model, eventName, showInTrace);
	//	controller = NFSOFController._Instance(model);
	}

	@Override
	public void eventRoutine(NFSTaskBindedFlow finishedflow) {
		sendTraceNote(finishedflow.getName() + "-" + finishedflow.datarate + " finishes");
		boolean openflowonoff = NetworkFlowSimulator.parser.getBoolean(
				"fluidsim.openflow.onoff", false);
		if (!openflowonoff) {
			finishedflow.finish();
			NFSFlowRateChangeEvent flowrateevent = new NFSFlowRateChangeEvent(
					getModel(),
					"RateChangeEventTriggeredByCloseFlow",
					true);
			flowrateevent.schedule(finishedflow.getSender().getTaskTracker(), 
					finishedflow.getFirstLink(), finishedflow, presentTime());
		}
		else {
			NFSOFController._Instance(getModel()).finishflow(finishedflow);
		}
	}
}
