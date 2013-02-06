package simulator.events;


import simulator.entity.NFSHost;
import simulator.entity.flow.NFSFlow;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;

public class NFSCloseFlowEvent extends EventOf2Entities<NFSHost, NFSFlow> {

	public NFSCloseFlowEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
	}

	@Override
	public void eventRoutine(NFSHost dsthost, NFSFlow flow) {
		NFSFlowRateChangeEvent flowrateevent = new NFSFlowRateChangeEvent(
				getModel(), 
				"ratechange",
				true);
		flow.setStatus(NFSFlow.NFSFlowStatus.CLOSED);
		flowrateevent.schedule(
				dsthost.getOutLink(flow.dstipString), 
				flow, 
				new TimeInstant(0));
		dsthost.close();
	}		
}
