package simulator.events;


import simulator.entity.NFSHost;
import simulator.entity.flow.NFSFlow;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;

public class NFSCloseOnOffFlowEvent extends EventOf2Entities<NFSHost, NFSFlow> {

	public NFSCloseOnOffFlowEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
	}

	@Override
	public void eventRoutine(NFSHost srchost, NFSFlow flow) {
		//change the rate of flows along the path
		NFSFlowRateChangeEvent flowrateevent = new NFSFlowRateChangeEvent(
				getModel(), 
				"RateChangeEventTriggeredByCloseFlow",
				true);
		flowrateevent.schedule(srchost, flow.getFirstLink(), flow, presentTime());
		srchost.close(0);
	}		
}
