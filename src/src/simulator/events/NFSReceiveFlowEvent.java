package simulator.events;

import simulator.entity.NFSNode;
import simulator.entity.NFSRouter;
import simulator.entity.flow.NFSFlow;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;

public class NFSReceiveFlowEvent extends EventOf2Entities<NFSRouter, NFSFlow> {

	public NFSReceiveFlowEvent(Model model, String evtName, boolean showInTrace) {
		super(model, evtName, showInTrace);
	}

	@Override
	public void eventRoutine(NFSRouter currentNode, NFSFlow flow) {
		NFSNode nexthop = currentNode.ReceiveFlow(flow);
		if (nexthop.ipaddress.equals(flow.dstipString)) {
			//last hop
			//schedule rate change
			
		}
		else {
			schedule((NFSRouter)nexthop, flow, new TimeInstant(0));
		}
	}

}
