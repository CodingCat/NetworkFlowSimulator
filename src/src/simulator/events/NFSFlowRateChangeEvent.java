package simulator.events;

import simulator.entity.NFSNode;
import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;

public class NFSFlowRateChangeEvent extends EventOf2Entities<NFSNode, NFSLink> {

	public NFSFlowRateChangeEvent(Model arg0, String arg1, boolean arg2) {
		super(arg0, arg1, arg2);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void eventRoutine(NFSNode node, NFSLink link) {
		// TODO Auto-generated method stub
		
	}

}
