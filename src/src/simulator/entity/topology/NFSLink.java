package simulator.entity.topology;

import java.util.ArrayList;

import simulator.entity.NFSNode;
import simulator.entity.flow.NFSFlow;
import simulator.utils.NFSDoubleCalculator;
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
	
	public void setAvailableBandwidth(double v) {
		availableBandwidth = v;
	}
	
	public double getAvailableBandwidth() {
		double sum = 0.0;
		try {
			//NFSFlow newflow = null;
			for (NFSFlow flow : runningflows) {
				if (flow.datarate == -1) continue;
				sum = NFSDoubleCalculator.sum(sum, flow.datarate);
			}
			if (sum > totalBandwidth) {
				String str = "";
				for (int i = 0; i < runningflows.size(); i++) {
					str += (runningflows.get(i).getName() + "-" + runningflows.get(i).datarate + "\n");
				}
				throw new Exception(str);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			//System.exit(-1);
		}
		return NFSDoubleCalculator.sub(totalBandwidth, sum);
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
	
	public ArrayList<NFSFlow> getRunningFlows() {
		return runningflows;
	}
	
	public double getAvrRate() {
		return NFSDoubleCalculator.div(this.totalBandwidth, (double)runningflows.size());
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
