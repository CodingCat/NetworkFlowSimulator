package simulator.entity;

import java.util.HashMap;
import java.util.LinkedList;


import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSNode extends Entity{
	
	protected double totalBandWidth = 0.0;
	protected double availableBandWidth = 0.0;
	protected LinkedList<NFSFlow> runningFlows = null;
	protected HashMap<String, NFSLink> outLinks = null;//nexthop address -> link
	protected HashMap<NFSFlow, Double> flowAllocationTable = null;//flow->allocation
	
	protected Model ownermodel;
	
	public NFSNode(Model model, String entityName, boolean showInLog, double bandWidth) {
		super(model, entityName, showInLog);
		this.ownermodel = model;
		this.totalBandWidth = bandWidth;
		this.availableBandWidth = this.totalBandWidth;
		runningFlows = new LinkedList<NFSFlow>();
		outLinks = new HashMap<String, NFSLink>();
		flowAllocationTable = new HashMap<NFSFlow, Double>();
	}

	public int AddNewFlow(NFSFlow flow) {
		// max-min fair
		//TODO: need to be checked
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
	
}
