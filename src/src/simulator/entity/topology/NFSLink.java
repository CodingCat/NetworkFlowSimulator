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
	}
	
	@Override
	public String toString() {
		return "src:	" + src.toString() + "	dst:	" + dst.toString() + "	bandwidth:" + availableBandwidth;
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
	
	public NFSFlow[] getRunningFlows() {
		NFSFlow [] runningflowArray = new NFSFlow[runningflows.size()];
		return runningflows.toArray(runningflowArray);
	}
	
	public double getAvrRate() {
		return this.totalBandwidth / runningflows.size();
	}
	
	public void adjustFlowRates(double newflowrate) {
		double avrrate = totalBandwidth / runningflows.size();
		ArrayList<NFSFlow> flowsToBeReduced = new ArrayList<NFSFlow>();
		double amortizedCost = 0.0;
		for (NFSFlow flow : runningflows) {
			if (flow.datarate > avrrate) flowsToBeReduced.add(flow);
		}
		amortizedCost = (newflowrate - availableBandwidth) / flowsToBeReduced.size();
		for (NFSFlow flow : flowsToBeReduced) {
			flow.datarate -= amortizedCost;
			flow.setBottleneckIdx(flow.getLinkIdx(this));
		}
	}
}
