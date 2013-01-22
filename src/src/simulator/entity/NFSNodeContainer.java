package simulator.entity;

import java.util.ArrayList;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

import simulator.NetworkFlowSimulator;

public class NFSNodeContainer extends Entity{
	
	private ArrayList<NFSNode> nodes = null;
	
	
	public NFSNodeContainer(Model model, String entityName, boolean showInReport) {
		super(model, entityName, showInReport);
		nodes = new ArrayList<NFSNode>();
	}
	
	//for hosts
	public void create(int n, int ip_a, int ip_b, int ip_c, int start, int end){
		String iprange = String.format("%s.%s.%s.", ip_a, ip_b, ip_c); 
		for (int i = start; i <= end; i++) {
			NFSHost node = new NFSHost(getModel(), 
					iprange + i, 
					true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.node.default.bandwidth", 1024.0),
					iprange + i);
			nodes.add(node);
		}
	}
	
	//for routers
	public void create(int n, int ip_a, int ip_b, int start, int end){
		String iprange = String.format("%s.%s.", ip_a, ip_b); 
		for (int i = start; i <= end; i++) {
			NFSHost node = new NFSHost(getModel(), 
					iprange + i + ".1", 
					true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.node.default.bandwidth", 1024.0),
					iprange + i + ".1");
			nodes.add(node);
		}
	}
}
