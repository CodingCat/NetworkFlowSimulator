package simulator.entity;

import java.util.HashMap;
import java.util.LinkedList;

import simulator.entity.application.NFSFlow;
import simulator.entity.topology.NFSLink;



import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSNode extends Entity{
	
	protected HashMap<NFSLink, LinkedList<NFSFlow>> runningFlows = null;
	protected HashMap<String, NFSLink> outLinks = null;//nexthop address -> link
	protected HashMap<NFSFlow, Double> flowAllocationTable = null;//flow->allocation
	protected String ipaddress = null;
	
	
	public NFSNode(Model model, String entityName, boolean showInLog, double bandWidth, String ip) {
		super(model, entityName, showInLog);
		runningFlows = new HashMap<NFSLink, LinkedList<NFSFlow>>();
		outLinks = new HashMap<String, NFSLink>();
		flowAllocationTable = new HashMap<NFSFlow, Double>();
		ipaddress = ip;
	}
	
	protected NFSLink ChooseECMPLink(String dstString, NFSLink [] links) {
		int index = dstString.hashCode() % links.length;
		return links[index];
	}
	
	public NFSLink AddNewFlow(NFSFlow flow) {
		NFSLink link = ChooseECMPLink(flow.dstipString, (NFSLink[]) outLinks.values().toArray()); 
	/*	if (runningFlows.containsKey(link) == false) {
			runningFlows.put(link, new LinkedList<NFSFlow>());
		}
	//	runningFlows.get(link).add(flow);
	//	link.IncRunningFlowN();
		//set flow rate for the first time
		flow.SetDatarate(link.GetTotalBandwidth() / link.GetRunningFlowsN());
		//change the datarate of all other flows on this link*/
		changeSendingRate(link);
		return link;
	}
	
	private void changeSendingRate(NFSLink link) {
		LinkedList<NFSFlow> allflowsonLink = runningFlows.get(link);
		double avrrate = link.GetTotalBandwidth() / link.GetRunningFlowsN();
		for (NFSFlow flow : allflowsonLink) {
			if (flow.GetDataRate() >= avrrate) flow.SetDatarate(avrrate);
		}
	}
	
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
	
	public void PrintLinks() {
		for (NFSLink link : outLinks.values()) {
			System.out.println(link);
		}
	}
}
