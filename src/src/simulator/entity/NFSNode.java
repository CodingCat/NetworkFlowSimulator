package simulator.entity;

import java.util.HashMap;
import java.util.LinkedList;


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
	
	public void AddNewLink(String dstip, double rate) {
		NFSLink link = new NFSLink(getModel(), "link-" + this + "-" + dstip, true, rate);
		outLinks.put(dstip, link);
	}
	
	@Override
	public String toString() {
		return this.ipaddress;
	}
}
