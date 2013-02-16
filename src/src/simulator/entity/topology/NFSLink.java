package simulator.entity.topology;

import java.util.ArrayList;

import simulator.entity.NFSNode;
import simulator.entity.flow.NFSFlow;
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
		runningflows.add(flow);
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
		double avrrate = totalBandwidth / runningflows.size();
		ArrayList<NFSFlow> flowsToBeReduced = new ArrayList<NFSFlow>();
		double amortizedCost = 0.0;
		for (NFSFlow flow : runningflows) {
			if (flow == newflow) continue;
			if (flow.datarate > avrrate) flowsToBeReduced.add(flow);
		}
		amortizedCost = (newflow.datarate - availableBandwidth) / flowsToBeReduced.size();
		for (NFSFlow flow : flowsToBeReduced) {
			if (flow == newflow) continue;
			if (amortizedCost == 0.0) System.out.println("fuck");
			flow.update('-', amortizedCost);
			sendTraceNote("change " + flow.toString() + " datarate to " + flow.datarate);
			flow.setBottleneckLink(this);
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
