package simulator.entity.topology;

import java.util.ArrayList;
import java.util.Collections;

import simulator.entity.NFSNode;
import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSFlowFairScheduler;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSLink extends Entity{
	
	double availableBandwidth = 0.0;
	double totalBandwidth = 0.0;
	
	public NFSNode src = null;
	public NFSNode dst = null;
	
	private ArrayList<NFSFlow> runningflows = null;
	
	public NFSLink(Model model, String entityName, boolean showInLog, double bw, NFSNode s, NFSNode d) {
		super(model, entityName, showInLog);
		availableBandwidth = bw;
		totalBandwidth = bw;
		src = s;
		dst = d;
		runningflows = new ArrayList<NFSFlow>();
	}
	
	public void setAvailableBandwidth(char op, double v) {
		if (op == '+') {
			availableBandwidth += v;
		}
		else {
			if (op == '-') availableBandwidth -= v;
		}
	}
	
	public double getAvailableBandwidth() {
		return availableBandwidth;
	}
	
	public double getTotalBandwidth() {
		return totalBandwidth;
	}
		
	public void addRunningFlow(NFSFlow flow) {
		if (!runningflows.contains(flow)) runningflows.add(flow);
	}
	
	public void removeRunningFlow(NFSFlow flow) {
		runningflows.remove(flow);
	}
	
	public NFSFlow[] getRunningFlows() {
		NFSFlow [] runningflowArray = new NFSFlow[runningflows.size()];
		return runningflows.toArray(runningflowArray);
	}
	
	public double getAvrRate() {
		return this.totalBandwidth / runningflows.size();
	}
	
	/**
	 * adjust the rates of other flows, due to the appearance of newflow
	 * @param newflow, the new flow
	 */
	public void adjustFlowRates(NFSFlow newflow) {
		ArrayList<NFSFlow> flowsToBeReduced = new ArrayList<NFSFlow>();
		double sumRates = 0.0;
		for (NFSFlow flow : runningflows) {
			if (flow == newflow) continue;
			flowsToBeReduced.add(flow);
			sumRates += flow.datarate;
		}
		this.availableBandwidth = totalBandwidth - sumRates;
		if (flowsToBeReduced.size() != 0) { 
			double totalCost = newflow.datarate - availableBandwidth;
			Collections.sort(flowsToBeReduced, 
					Collections.reverseOrder(NFSFlowFairScheduler.ratecomparator));
			for (NFSFlow flow : flowsToBeReduced) {
				flow.update('-', totalCost * (flow.datarate / sumRates));
				flow.setBottleneckLink(this);
			}
		}
	}
	
	@Override
	public String toString() {
		return "link-" + src.toString() + 
				"-" + dst.toString() + 
				"-" +  totalBandwidth +
				"-" + availableBandwidth + 
				"Mbps";
	}
}
