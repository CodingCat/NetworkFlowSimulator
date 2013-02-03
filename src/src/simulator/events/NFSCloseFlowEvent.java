package simulator.events;

import simulator.entity.NFSHost;
import simulator.entity.flow.NFSFlow;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;

public class NFSCloseFlowEvent extends EventOf2Entities<NFSHost, NFSFlow> {

	public NFSCloseFlowEvent(Model model, String eventName, boolean showInReport) {
		super(model, eventName, showInReport);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void eventRoutine(NFSHost dsthost, NFSFlow flow) {
	/*	NFSLink [] passLinks = flow.getPassLinks();
		for (NFSLink link : passLinks) {
			NFSFlowRateChangeEvent rateChangeEvt = new NFSFlowRateChangeEvent(
					getModel(),
					flow + " change-propogate-event",
					true);
			rateChangeEvt.schedule(link, flow, new TimeInstant(0));
		}*/
	}		
}
