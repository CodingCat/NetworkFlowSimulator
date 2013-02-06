package simulator.entity;

import java.util.HashMap;

import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSNode extends Entity{
	
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
	
	public NFSLink getOutLink(String dstip) {
		return outLinks.get(dstip);
	}
	
	public NFSLink AddNewFlow(NFSFlow flow) {
		NFSLink link = ChooseECMPLink(
				(flow.srtipString + flow.dstipString), (NFSLink[]) outLinks.values().toArray());
		link.addRunningFlow(flow);
		flow.addBypassingLink(link);
		return link;
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
