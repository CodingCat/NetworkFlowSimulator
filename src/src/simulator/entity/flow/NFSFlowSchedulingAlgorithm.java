package simulator.entity.flow;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSNode;
import simulator.entity.NFSRouter;
import simulator.entity.NFSRouter.RouterType;
import simulator.entity.flow.NFSFlow.NFSFlowStatus;
import simulator.entity.topology.NFSLink;
import simulator.utils.NFSDoubleCalculator;

public class NFSFlowSchedulingAlgorithm {
	
	public static void rateAllocation(NFSNode node, NFSLink firstlink, NFSFlow changingflow) {
		NFSNode dummynode = node; 
		dummynode.changeResourceAllocation(firstlink, changingflow);
		NFSLink nextlink = changingflow.getNextLink(firstlink);
		while (nextlink != null) {
			dummynode = nextlink.dst;
			dummynode.changeResourceAllocation(nextlink, changingflow);
			nextlink = changingflow.getNextLink(nextlink);
		}
		if (changingflow.getStatus().equals(NFSFlow.NFSFlowStatus.NEWSTARTED)) {
			//if this flow is a new started flow
			changingflow.start();
		}
		else {
			if (changingflow.getStatus().equals(NFSFlow.NFSFlowStatus.CLOSED)) {
				//if this flow is just closed
				changingflow.close();
			}
		}
	}
	
	public static void ecmpPathSelection(NFSLink firstlink, NFSFlow flow) {
		
		NFSLink selectedlink = firstlink;
		flow.addPath(selectedlink);
		selectedlink.addRunningFlow(flow);
		NFSRouter router = (NFSRouter) selectedlink.dst;
		String dstCrange = flow.dstipString.substring(0, flow.dstipString.lastIndexOf(".")) + ".0";
		//get the building tag
		//get the later 3 segment
		String dstlater3seg = flow.dstipString.substring(flow.dstipString.indexOf(".") + 1, 
				flow.dstipString.length());
		String dstbuildingTag = dstlater3seg.substring(0, dstlater3seg.indexOf("."));
		while (true) {
			String localCrange = router.ipaddress.substring(0, router.ipaddress.lastIndexOf(".")) + ".0";
			if (router.getRouterType().equals(RouterType.Edge)) {
				if (dstCrange.equals(localCrange)) {	
					//in the same lan
					selectedlink = router.getLanLink(flow.dstipString);
				}
				else{
					//send through the ECMP link to distribution layer
					selectedlink = router.getScheduler().schedule(flow);
					router = (NFSRouter) selectedlink.dst;
				}
			}
			else {
				if (router.getRouterType().equals(RouterType.Distribution)) {
					String locallater3seg = router.ipaddress.substring(router.ipaddress.indexOf(".") + 1, 
							router.ipaddress.length());
					String localbuildingTag = locallater3seg.substring(0, locallater3seg.indexOf("."));
					if (dstbuildingTag.equals(localbuildingTag)) {
						//it's in the same building, so, just send back to edge layer
						selectedlink = router.getLanLink(dstCrange);
						router = (NFSRouter) selectedlink.src;
					}
					else {
						//send through ECMP link to the core
						selectedlink = router.getScheduler().schedule(flow);
						router = (NFSRouter) selectedlink.dst;
					}
				}
				else {
					//Must be core
					//local query
					ArrayList<NFSLink> potentialLinks = new ArrayList<NFSLink>();
					for (String link: router.getPods()) {
						String linklater3seg = link.substring(link.indexOf(".") + 1, link.length());
						String linkbuildingTag = linklater3seg.substring(0, linklater3seg.indexOf("."));
						if (linkbuildingTag.equals(dstbuildingTag)) potentialLinks.add(router.getLanLink(link));
					}
					if (potentialLinks.size() != 0) {
						int selectedIdx = (flow.srcipString + flow.dstipString).hashCode() % potentialLinks.size();
						selectedIdx = Math.max(selectedIdx, -selectedIdx);
						selectedlink = potentialLinks.get(selectedIdx);
						router = (NFSRouter) selectedlink.src;
					}
				}
			}
			flow.addPath(selectedlink);//add path
			selectedlink.addRunningFlow(flow);
			if (selectedlink.src.ipaddress.equals(flow.dstipString)) break;
		}//end of loop
	}
	
	void MaxMinAllocate(NFSLink link, NFSFlow changingflow) {
		if (changingflow.getStatus().equals(NFSFlowStatus.NEWSTARTED) || 
				changingflow.getStatus().equals(NFSFlowStatus.ADJUSTING)) {
			@SuppressWarnings("unchecked")
			ArrayList<NFSFlow> demandingflows = (ArrayList<NFSFlow>) link
					.getRunningFlows().clone();
			double remainingBandwidth = link.getTotalBandwidth();
			double avrRate = link.getAvrRate();
			Collections.sort(demandingflows, NFSFlowScheduler.ratecomparator);
			while (demandingflows.size() != 0 && remainingBandwidth != 0) {
				double demand = 0.0;
				NFSFlow flow = demandingflows.get(0);
				demand = !flow.status.equals(NFSFlowStatus.RUNNING) ? flow.expectedrate : flow.datarate;
				if (demand < avrRate) {
					remainingBandwidth = NFSDoubleCalculator.sub(remainingBandwidth, demand);
				} else {
					flow.update('-', NFSDoubleCalculator.sub(flow.datarate, avrRate));
					flow.setBottleneckLink(link);
					remainingBandwidth = NFSDoubleCalculator.sub(remainingBandwidth, avrRate);
				}
				demandingflows.remove(0);
				if (demandingflows.size() != 0)
					avrRate = NFSDoubleCalculator.div(remainingBandwidth, demandingflows.size());
			}
		}//end of it's a new flow
		if (changingflow.getStatus() == (NFSFlowStatus.CLOSED)) {
			//this flow has been closed
			//triggered by the close flow event
			//rate of others might be improved
			link.removeRunningFlow(changingflow);
			ArrayList<NFSFlow> improvingflows = new ArrayList<NFSFlow>();
			double totalfreedbw = changingflow.datarate;
			for (NFSFlow eleflow : link.getRunningFlows()) {
				if (eleflow.getBottleneckLink().equals(link) && !eleflow.isFullyMeet()) {
					improvingflows.add(eleflow);
				}
			}
			while (improvingflows.size() > 0 && totalfreedbw > 0) {
				double oldrate = improvingflows.get(0).datarate;
				double amortizedbw = NFSDoubleCalculator.div(
						totalfreedbw, (double) improvingflows.size());
				improvingflows.get(0).adjust(
						NFSDoubleCalculator.sum(improvingflows.get(0).datarate, amortizedbw));
				double newrate = improvingflows.get(0).datarate;
				totalfreedbw = NFSDoubleCalculator.sub(
						totalfreedbw, NFSDoubleCalculator.sub(newrate, oldrate));
				improvingflows.remove(0);
			}
		}//end of it's a closing flow
	}
	
	public static void allocate(NFSLink link, NFSFlow changingflow) {
		try {
			NFSFlowSchedulingAlgorithm algoobj = new NFSFlowSchedulingAlgorithm();
			Class<?> algoclass = algoobj.getClass();
			String allocatemethodname = NetworkFlowSimulator.parser.getString(
					"fluidsim.flow.algorithm", "MaxMinAllocate");
			Method allocatealgo = algoclass.getDeclaredMethod(
					allocatemethodname, NFSLink.class, NFSFlow.class);
			allocatealgo.invoke(algoobj, link, changingflow);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
