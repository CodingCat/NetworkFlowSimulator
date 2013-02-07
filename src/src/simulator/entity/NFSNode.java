package simulator.entity;

import java.util.ArrayList;
import java.util.HashMap;

import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSNode extends Entity{
	
	protected ArrayList<NFSLink> outLinks = null;//nexthop address -> link
	protected HashMap<NFSFlow, Double> flowAllocationTable = null;//flow->allocation
	public String ipaddress = null;
	
	
	public NFSNode(Model model, String entityName, boolean showInLog, double bandWidth, String ip) {
		super(model, entityName, showInLog);
		outLinks = new ArrayList<NFSLink>();
		flowAllocationTable = new HashMap<NFSFlow, Double>();
		ipaddress = ip;
	}
	
	protected NFSLink chooseECMPLink(NFSFlow flow) {
		int index = (flow.srcipString + flow.dstipString).hashCode() % outLinks.size();
		return outLinks.get(index);
	}
	
	public double getFlowAllocation(NFSFlow flow) {
		return flowAllocationTable.get(flow);
	}
	
	public void AddNewLink(NFSNode dst, double rate) {
		NFSLink link = new NFSLink(getModel(), "link-" + this + "-" + dst, true, rate, this, dst);
		outLinks.add(link);
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
		for (NFSLink link : outLinks) {
			System.out.println(link);
		}
	}
}
