package simulator.events;

import simulator.entity.NFSHost;
import simulator.entity.NFSNode;
import simulator.entity.NFSRouter;
import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.EventOf3Entities;
import desmoj.core.simulator.Model;

public class NFSReceiveFlowEvent extends EventOf3Entities<NFSNode, NFSRouter, NFSFlow> {

	public NFSReceiveFlowEvent(Model model, String evtName, boolean showInTrace) {
		super(model, evtName, showInTrace);
	}
	
	private NFSLink getnextLink(NFSNode sendNode, NFSRouter recvNode) {
		String key = null;
		NFSRouter linkowner = null;
		if (sendNode.getClass().equals(NFSRouter.class) && recvNode.getClass().equals(NFSRouter.class)) {
			//if the flow is send from router to router
			int stype = ((NFSRouter) sendNode).getRouterType().getType();
			int rtype = ((NFSRouter) recvNode).getRouterType().getType();
			if (stype > rtype) {
				key = recvNode.ipaddress.substring(0, recvNode.ipaddress.lastIndexOf(".")) + ".0";
				linkowner = (NFSRouter) sendNode;
			}
			else {
				key = sendNode.ipaddress.substring(0, sendNode.ipaddress.lastIndexOf(".")) + ".0";
				linkowner = (NFSRouter) recvNode;
			}
		}
		else {
			if (sendNode.getClass().equals(NFSHost.class)) key = sendNode.ipaddress;
			else {
				key = sendNode.ipaddress.substring(0, sendNode.ipaddress.lastIndexOf(".")) + ".0";
			}
			linkowner = (NFSRouter) recvNode;
		}
		return linkowner.getLanLink(key);
	}
	
	@Override
	public void eventRoutine(NFSNode sendNode, NFSRouter recvNode, NFSFlow flow) {
		try {
			NFSLink ingresslink = getnextLink(sendNode, recvNode);
			if (ingresslink != null) {
				ingresslink.addRunningFlow(flow);
				NFSNode nexthop = recvNode.receiveFlow(flow);
				if (nexthop.ipaddress.equals(flow.dstipString)) {
					//last hop
					//schedule rate change
					NFSFlowRateChangeEvent ratechangeevent = new NFSFlowRateChangeEvent(
							getModel(), 
							"RateChangeEventTriggeredByStartNewFlow",
							true);
					ratechangeevent.setSchedulingPriority(1);
					ratechangeevent.schedule(recvNode, flow.getFirstLink(), flow, presentTime());
				}
				else {
					//keeping priority to be 1 
					schedule(recvNode, (NFSRouter)nexthop, flow, presentTime());
				}
			}
			else {
				throw new Exception("invalid link in receveFlowEvent");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
