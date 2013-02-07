package simulator.events;


import simulator.entity.NFSHost;
import simulator.entity.flow.NFSFlow;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;

public class NFSCloseFlowEvent extends EventOf2Entities<NFSHost, NFSFlow> {

	public NFSCloseFlowEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
	}

	@Override
	public void eventRoutine(NFSHost dsthost, NFSFlow flow) {
		//change the rate of flows along the path
		NFSFlowRateChangeEvent flowrateevent = new NFSFlowRateChangeEvent(
				getModel(), 
				"ratechange",
				true);
		flow.setStatus(NFSFlow.NFSFlowStatus.CLOSED);
		flowrateevent.schedule(flow.getFirstLink(), flow, presentTime());
		dsthost.close();
	}		
}
