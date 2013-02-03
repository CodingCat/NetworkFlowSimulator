package simulator.entity;

import java.util.HashMap;

import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSNode extends Entity{
	
//	protected HashMap<NFSLink, LinkedList<NFSFlow>> runningFlows = null;
	protected HashMap<String, NFSLink> outLinks = null;//nexthop address -> link
	protected HashMap<NFSFlow, Double> flowAllocationTable = null;//flow->allocation
	public String ipaddress = null;
	
	
	public NFSNode(Model model, String entityName, boolean showInLog, double bandWidth, String ip) {
		super(model, entityName, showInLog);
	//	runningFlows = new HashMap<NFSLink, LinkedList<NFSFlow>>();
		outLinks = new HashMap<String, NFSLink>();
		flowAllocationTable = new HashMap<NFSFlow, Double>();
		ipaddress = ip;
	}
	
	protected NFSLink ChooseECMPLink(String dstString, NFSLink [] links) {
		int index = dstString.hashCode() % links.length;
		return links[index];
	}
	
	public NFSLink AddNewFlow(NFSFlow flow) {
		NFSLink link = ChooseECMPLink(
				(flow.srtipString + flow.dstipString), (NFSLink[]) outLinks.values().toArray());
		link.addRunningFlow(flow);
		flow.addBypassingLink(link);
		return link;
	}
	
	
	/*private void changeSendingRate(NFSLink link) {
		LinkedList<NFSFlow> allflowsonLink = runningFlows.get(link);
		double avrrate = link.GetTotalBandwidth() / link.GetRunningFlowsN();
		for (NFSFlow flow : allflowsonLink) {
			if (flow.datarate >= avrrate) {
				flow.expectedrate = avrrate;
				NFSFlowRateChangeEvent ratechangeevt = new NFSFlowRateChangeEvent(
						getModel(),
						flow.toString() + " Rate Change",
						true);
				ratechangeevt.schedule(this, link, new TimeInstant(0));
			}
		}
	}*/
	
	public double getFlowAllocation(NFSFlow flow) {
		return flowAllocationTable.get(flow);
	}
	
	public void AddNewLink(NFSNode dst, double rate) {
		NFSLink link = new NFSLink(getModel(), "link-" + this + "-" + dst, true, rate, this, dst);
		outLinks.put(dst.toString(), link);
	}
	
	public void AssignIPAddress(String ip) {
		ipaddress = ip;
	}
	
	public boolean HasAllocatedIP() { 
		return ipaddress != null;
	}
	 
	@Override
	public String toString() {
		return this.ipaddress;
	}
	
/*	public NFSFlow[] getRunningFlows(NFSLink link) {
		return (NFSFlow[]) runningFlows.get(link).toArray();
	}
	*/
	public void PrintLinks() {
		for (NFSLink link : outLinks.values()) {
			System.out.println(link);
		}
	}
}
