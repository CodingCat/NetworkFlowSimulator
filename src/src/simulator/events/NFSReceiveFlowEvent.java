package simulator.events;

import simulator.entity.NFSNode;
import simulator.entity.NFSRouter;
import simulator.entity.flow.NFSFlow;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;

public class NFSReceiveFlowEvent extends EventOf2Entities<NFSRouter, NFSFlow> {

	public NFSReceiveFlowEvent(Model model, String evtName, boolean showInTrace) {
		super(model, evtName, showInTrace);
	}

	@Override
	public void eventRoutine(NFSRouter router, NFSFlow flow) {
		NFSNode nexthop = router.receiveFlow(flow);
		if (nexthop.ipaddress.equals(flow.dstipString)) {
			//last hop
			//schedule rate change
			NFSFlowRateChangeEvent ratechangeevent = new NFSFlowRateChangeEvent(
					getModel(), 
					"RateChangeEventTriggeredByStartNewFlow",
					true);
			ratechangeevent.setSchedulingPriority(1);
			ratechangeevent.schedule(router, flow.getFirstLink(), flow, presentTime());
		}
		else {
			//keeping priority to be 1 
			schedule((NFSRouter)nexthop, flow, presentTime());
		}
	}

}
