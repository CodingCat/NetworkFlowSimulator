package simulator.events;

import simulator.entity.application.NFSMapTask;
import simulator.entity.flow.NFSTaskBindedFlow;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;

public class NFSCloseTaskBindedFlowEvent extends
		EventOf2Entities<NFSMapTask, NFSTaskBindedFlow> {

	public NFSCloseTaskBindedFlowEvent(Model arg0, String arg1, boolean arg2) {
		super(arg0, arg1, arg2);
	}

	@Override
	public void eventRoutine(NFSMapTask task, NFSTaskBindedFlow flow) {
		task.close(flow);
	}

}
