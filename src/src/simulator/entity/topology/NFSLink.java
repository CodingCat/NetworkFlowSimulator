package simulator.entity.topology;

import simulator.entity.NFSNode;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSLink extends Entity{
	
	double availableBandwidth = 0.0f;
	double totalBandwidth = 0.0f;
	int runningflowsnum = 0;
	
	public NFSNode src = null;
	public NFSNode dst = null;
	
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
	
	public void UpdateBandWidth(double newvalue) {
		availableBandwidth = newvalue;
	}
	
	public double GetBandWidth() {
		return availableBandwidth;
	}
	
	public double GetTotalBandwidth() {
		return totalBandwidth;
	}
	
	public int GetRunningFlowsN() {
		return runningflowsnum;
	}
	
	public void IncRunningFlowN() {
		this.runningflowsnum++;
	}
}
