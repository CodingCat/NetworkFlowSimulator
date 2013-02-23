package simulator.entity.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSRouter;
import simulator.entity.NFSRouter.RouterType;
import simulator.entity.application.NFSMapReduceJob;
import simulator.entity.topology.NFSLink;
import simulator.utils.NFSDoubleCalculator;
import simulator.utils.NFSOFJobAllocationMap;
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
	private HashMap<NFSLink, NFSOFJobAllocationMap> linkappmap = null; 
	
	private double latencyPercentage = 0.0;
	private double throughputPercentage = 0.0;
	
	private NFSOFController(Model model, String entityName, boolean showInTrace) {
		super(model, entityName, showInTrace);
		globalmap = new HashMap<NFSRouter, ArrayList<NFSLink>>();
		linkappmap = new HashMap<NFSLink, NFSOFJobAllocationMap>();
		latencyPercentage = NetworkFlowSimulator.parser.getDouble(
				"fluidsim.openflow.latencypercentage", 0.5);
		throughputPercentage = 1 - latencyPercentage;
	}
	
	public double allocaterate(NFSLink link, NFSFlow newflow) {
		double allocatedrate = 0.0;
		if (link.getAvailableBandwidth() > newflow.expectedrate) {
			double allocation = Math.min(newflow.expectedrate, link.getAvailableBandwidth());
			if (newflow.expectedrate > allocation) {
				newflow.expectedrate = allocation;
				newflow.setBottleneckLink(link);
			}
		}
		else {
			//spare capacity cannot meet the flow's expected rate
			double sumRateExistingLatencyFlows = 0.0;
			double sumRateExistingThroughputFlows = 0.0;
			for (NFSFlow flow : link.getRunningFlows()) {
				if (flow.isLatencySensitive()) {
					sumRateExistingLatencyFlows = NFSDoubleCalculator.sum(sumRateExistingLatencyFlows, flow.datarate);
				}
				else {
					sumRateExistingThroughputFlows = NFSDoubleCalculator.sum(sumRateExistingThroughputFlows, flow.datarate);
				}
			}
			if (newflow.isLatencySensitive()) {
				// TODO:
				double sumRateLatencyFlows = NFSDoubleCalculator.sum(sumRateExistingLatencyFlows, newflow.expectedrate);
				// 1. check if sensitive service has achieved their
				// threshold
				if (NFSDoubleCalculator.div(sumRateLatencyFlows, link.getTotalBandwidth()) >= latencyPercentage) {
					// 2. if yes, drop the flow (return 0.0)
					return 0.0;
				}
				else {
					// 3. if no
					// 3.1 if (total bandwidth of throughput service minus
					// throughput percentage * total bandwidth ) is smaller than
					// (the demand of flow - link.availableBW), drop the flow
					// (return 0.0)
					if (NFSDoubleCalculator.sub(sumRateExistingThroughputFlows, throughputPercentage * link.getTotalBandwidth()) < 
							newflow.expectedrate - link.getAvailableBandwidth())  {
						return 0.0;
					}
					else {
						// 3.2 totalCost = (the demand of flow - link.availableBW)
						// reduce rate of throughput flows,
						// reduce amount = total cost * flow.datarate / (throughput percentage * total bandwidth)
						// assign the allocation
						//keep the expected rate, do nothing
					}
				}
			} else {
				// if this flow is a best-effort flow
				// available bandwidth(ab) = totalbandwidth - all sensitiveflow datarate
				// job's allocation (ja) = ab * (job's weight)
				// flow's allocation(fa) = ja * (flow's size / total input size)
				// if (fa < flow.expectedrate) flow.expectedrate = fa;
				NFSTaskBindedFlow taskflow = (NFSTaskBindedFlow) newflow;
				NFSMapReduceJob job = taskflow.getSender().getJob();
				double ab = NFSDoubleCalculator.sub(link.getTotalBandwidth(), sumRateExistingLatencyFlows);
				double jw = linkappmap.get(link).getPossibleJobAllocation(job.getPriority());
				double ja = NFSDoubleCalculator.mul(ab, jw);
				double fa = NFSDoubleCalculator.mul(ja, linkappmap.get(link).getPossibleFlowWeight(taskflow.inputSize - taskflow.sendoutSize));
				if (fa < newflow.expectedrate) {
					newflow.expectedrate = fa;
					newflow.setBottleneckLink(link);
				}
			}//end of throughput flow
		}
		return allocatedrate;
	}
	
	private NFSOpenFlowMessage decide(ArrayList<NFSLink> candidatelinks, NFSFlow flow) {
		NFSLink selectedlink = null;
		double rate = 0.0;
		//select the least congested link 
		Collections.sort(candidatelinks, new NFSLinkComparator(flow.expectedrate));
		for (NFSLink link : candidatelinks) {
			selectedlink = link;
			rate = allocaterate(selectedlink, flow);
			if (rate != 0) break;
		}
		if (rate == 0) return null;
		flow.addPath(selectedlink);
		return new NFSOpenFlowMessage(selectedlink, rate);
	}
	
	private NFSOpenFlowMessage decide(NFSLink link, NFSFlow flow) {
		double rate = allocaterate(link, flow);
		if (rate == 0) return null;
		return new NFSOpenFlowMessage(link, rate);
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
			flow.addPath(resultmsg.getAllocatedLink());//add path
			if (resultmsg.getAllocatedLink().src.ipaddress.equals(flow.dstipString)) break;
		}//end of loop
		if (resultmsg != null) {
			if (!flow.isLatencySensitive()) {
				NFSTaskBindedFlow taskbindedflow = (NFSTaskBindedFlow) flow;
				NFSMapReduceJob job = taskbindedflow.getSender().getJob();
				for (NFSLink link : flow.getPaths()) {
					linkappmap.get(link).registerNewJob(job);
					linkappmap.get(link).registerNewFlow(job.getName(), taskbindedflow);
				}
			}
		}
		else {
			flow.clearLinks();
		}
		return resultmsg;
	}
	
	public void registerNewJob(NFSLink link, NFSMapReduceJob newjob) {
		linkappmap.get(link).registerNewJob(newjob);
	}
	
	public void registerNewFlow(NFSLink link, String jobname, NFSTaskBindedFlow newflow) {
		linkappmap.get(link).registerNewFlow(jobname, newflow);
	}
	
	public static NFSOFController _Instance(Model model) {
		if (_instance == null) {
			_instance = new NFSOFController(model, "OpenFlowController", true); 
		}
		return _instance;
	}

}
