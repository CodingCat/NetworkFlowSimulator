package simulator.entity;

import java.util.HashMap;
import java.util.LinkedList;

import simulator.entity.application.NFSFlow;
import simulator.entity.topology.NFSLink;



import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSNode extends Entity{
	
	protected double totalBandWidth = 0.0;
	protected LinkedList<NFSFlow> runningFlows = null;
	protected HashMap<String, NFSLink> outLinks = null;//nexthop address -> link
	protected HashMap<NFSFlow, Double> flowAllocationTable = null;//flow->allocation
	protected String ipaddress = null;
	
	public NFSNode(Model model, String entityName, boolean showInLog, double bandWidth, String ip) {
		super(model, entityName, showInLog);
		this.totalBandWidth = bandWidth;
		runningFlows = new LinkedList<NFSFlow>();
		outLinks = new HashMap<String, NFSLink>();
		flowAllocationTable = new HashMap<NFSFlow, Double>();
		ipaddress = ip;
	}

	public int AddNewFlow(NFSFlow flow) {
		// max-min fair
		runningFlows.add(flow);
		double avr = totalBandWidth / (runningFlows.size());
		int cnt = 1;
		for (NFSFlow runningflow : runningFlows) {
			if (runningflow.demand < avr) {
				avr = (totalBandWidth - runningflow.demand) / (runningFlows.size() - cnt); 
			}
			runningflow.datarate = runningflow.demand <= avr ? runningflow.demand : avr;
			flowAllocationTable.put(runningflow, runningflow.datarate);
			cnt++;
		}
		return runningFlows.size();
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
