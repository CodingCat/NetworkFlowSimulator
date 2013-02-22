package simulator.entity.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSRouter;
import simulator.entity.NFSRouter.RouterType;
import simulator.entity.topology.NFSLink;
import simulator.model.NFSModel;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSOFController extends Entity {
	
	
	private class NFSLinkComparator implements Comparator<NFSLink> {
		
		public double demandsize = 0;
		
		public NFSLinkComparator(double demand) {
			demandsize = demand;
		}
		
		@Override
		public int compare(NFSLink l1, NFSLink l2) {
			int ret = 0;
			double sparecap1 = l1.getAvailableBandwidth() - demandsize;
			double sparecap2 = l2.getAvailableBandwidth() - demandsize;
			ret = sparecap1 > sparecap2 ? 1 : (sparecap1 == sparecap2 ? 0 : -1);
			if (ret == 0 || Math.max(sparecap1, sparecap2) < 0) {
				//compare number of flows
				ret = l1.getRunningFlows().length - l2.getRunningFlows().length;
			}
			return ret;
		}
		
	}
	
	
	private static NFSOFController _instance = null;
	
	private HashMap<NFSRouter, ArrayList<NFSLink>> globalmap = null;
	private double latencyPercentage = 0.0;
	private double throughputPercentage = 0.0;
	
	private NFSOFController(Model model, String entityName, boolean showInTrace) {
		super(model, entityName, showInTrace);
		globalmap = new HashMap<NFSRouter, ArrayList<NFSLink>>();
		latencyPercentage = NetworkFlowSimulator.parser.getDouble(
				"fluidsim.openflow.latencypercentage", 0.5);
		throughputPercentage = 1 - latencyPercentage;
	}
	
	public double allocaterate(NFSLink link, NFSFlow flow) {
		double allocatedrate = 0.0;
		if (link.getAvailableBandwidth() > flow.expectedrate) {
			double allocation = Math.min(flow.expectedrate, link.getAvailableBandwidth());
			if (flow.expectedrate > allocation) {
				flow.expectedrate = allocation;
				flow.setBottleneckLink(link);
			}
		}
		else {
			//spare capacity cannot meet the flow's expected rate
			if (flow.isLatencySensitive()) {
				// TODO:
				// 1. check if sensitive service has achieved their
				// threshold
				// 2. if yes, drop the flow (return 0.0)
				// 3. if no
				// 3.1 if (total bandwidth of throughput service minus
				// throughput
				// percentage * total bandwidth ) is smaller than
				// (the demand of flow - link.availableBW), drop the flow
				// (return 0.0)
				// 3.2 totalCost = (the demand of flow - link.availableBW)
				// reduce rate of throughput flows,
				// reduce amount = total cost * flow.datarate / (throughput
				// percentage * total bandwidth)
				// assign the allocation
			} else {
				// TODO:
				// if this flow is a best-effort flow
				// available bandwidth(ab) = totalbandwidth - all sensitiveflow datarate
				// job's allocation (ja) = ab * (job's priority / sum of priorities)
				// flow's allocation(fa) = ja * (flow's size / total input size)
				// if (fa < flow.expectedrate) flow.expectedrate = fa;
			}
		}
		return allocatedrate;
	}
	
	private NFSOpenFlowMessage decide(ArrayList<NFSLink> outlinks, NFSFlow flow) {
		NFSLink selectedlink = null;
		double rate = 0.0;
		//select the least congested link 
		Collections.sort(outlinks, new NFSLinkComparator(flow.expectedrate));
		for (NFSLink link : outlinks) {
			selectedlink = link;
			rate = allocaterate(selectedlink, flow);
			if (rate != 0) break;
			flow.addPath(link);
		}
		if (rate == 0) return null;
		return new NFSOpenFlowMessage(selectedlink, rate);
	}
	
	private NFSOpenFlowMessage decide(NFSLink link, NFSFlow flow) {
		double rate = allocaterate(link, flow);
		if (rate == 0) return null;
		return new NFSOpenFlowMessage(link, 0);
	}
	
	/**
	 * called by the host when initializing the flow, negotiate
	 * @param firstlink
	 * @param flow
	 * @return
	 */
	public NFSOpenFlowMessage schedule(NFSLink firstlink, NFSFlow flow) {
		//select the least congested link
		NFSLink selectedlink = firstlink;
		NFSOpenFlowMessage resultmsg = null;
		firstlink.addRunningFlow(flow);
		NFSRouter router = (NFSRouter) firstlink.dst;
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
					resultmsg = decide(selectedlink, flow);
				}
				else{
					//send through the least congested link to distribution layer
					resultmsg = decide(globalmap.get(router), flow);
					if (resultmsg == null) break;
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
						resultmsg = decide(selectedlink, flow);
					}
					else {
						//send through the least congested link to the core
						resultmsg = decide(globalmap.get(router), flow);
						if (resultmsg == null) break;
						router = (NFSRouter) resultmsg.getAllocatedLink().dst;
					}
				}
				else {
					//Must be core
					//local query
					ArrayList<NFSLink> potentialLinks = new ArrayList<NFSLink>();
					for (String link: router.getPods()) {
						String linklater3seg = link.substring(link.indexOf(".") + 1, link.length());
						String linkbuildingTag = linklater3seg.substring(0, linklater3seg.indexOf("."));
						if (linkbuildingTag.equals(dstbuildingTag)) {
							potentialLinks.add(router.getLanLink(link));
						}
					}
					if (potentialLinks.size() != 0) {
						resultmsg = decide(potentialLinks, flow);
						if (resultmsg == null) break;
						router = (NFSRouter) resultmsg.getAllocatedLink().src;
					}
				}
			}
			if (resultmsg.getAllocatedLink().src.ipaddress.equals(flow.dstipString)) break;
		}//end of loop
		return resultmsg;
	}
	
	public static NFSOFController _Instance(Model model) {
		if (_instance == null) {
			_instance = new NFSOFController(model, "OpenFlowController", true); 
		}
		return _instance;
	}

}
