package simulator.events;

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
		finishedflow.finish();
		if (finishedflow.getPaths().size() == 0) {
			System.out.println(finishedflow.getName() + " status:" + 
					finishedflow.getStatus().toString());
		}
		NFSFlowRateChangeEvent flowrateevent = new NFSFlowRateChangeEvent(
				getModel(),
				"RateChangeEventTriggeredByCloseFlow",
				true);
		flowrateevent.schedule(finishedflow.getSender().getTaskTracker(), 
				finishedflow.getFirstLink(), finishedflow, presentTime());
	}
}
