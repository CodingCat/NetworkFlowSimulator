package simulator.entity.topology;

import java.util.ArrayList;
import java.util.Comparator;

import simulator.entity.NFSNode;
import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSFlow.NFSFlowStatus;
import simulator.utils.NFSDoubleCalculator;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSLink extends Entity{
	
	public static class NFSLinkComparator implements Comparator<NFSLink> {
		
		private static NFSLinkComparator _instance = null;
		
		public static NFSLinkComparator _Instance() {
			if (_instance == null) _instance = new NFSLinkComparator();
			return _instance;
		}
		
		@Override
		public int compare(NFSLink link1, NFSLink link2) {
			return link1.getName().compareTo(link2.getName());
		}
		
	}
	
	double availableBandwidth = 0.0;
	double totalBandwidth = 0.0;
	
	public NFSNode src = null;
	public NFSNode dst = null;
	public static NFSLinkComparator linkcomparator = null;
	
	private ArrayList<NFSFlow> runningflows = null;
	
	public NFSLink(Model model, String entityName, boolean showInLog, double bw, NFSNode s, NFSNode d) {
		super(model, entityName, showInLog);
		availableBandwidth = bw;
		totalBandwidth = bw;
		src = s;
		dst = d;
		runningflows = new ArrayList<NFSFlow>();
		linkcomparator = NFSLinkComparator._Instance();
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
				if (flow.getStatus().equals(NFSFlowStatus.NEWSTARTED)) continue;
				sum = NFSDoubleCalculator.sum(sum, flow.datarate);
			}
			if (sum > totalBandwidth + 0.001) {
				String str = "";
				for (int i = 0; i < runningflows.size(); i++) {
					str += (runningflows.get(i).getName() + "-" + runningflows.get(i).datarate + "\n");
				}
				throw new Exception(str + " sum:" + sum + " on link:" + this.getName());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			//System.exit(-1);
		}
		return Math.max(0, NFSDoubleCalculator.sub(totalBandwidth, sum));
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
