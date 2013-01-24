package simulator.entity;

import java.util.ArrayList;

import desmoj.core.simulator.Model;

import simulator.NetworkFlowSimulator;

public class NFSHostsContainer extends NFSNodesContainer{
	
	private ArrayList<NFSHost> hosts = null;
	public NFSHostsContainer(Model model, String entityName, boolean showInReport) {
		super(model, entityName, showInReport);
		hosts = new ArrayList<NFSHost>();
	}
	
	@Override
	public void create(int n){
		for (int i = 0; i < n; i++) {
			NFSHost node = new NFSHost(getModel(), "node " + i, 
					true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.node.default.bandwidth", 1000),
					null);
			hosts.add(node);
		}
	}
	
	public int GetN() {
		return hosts.size();
	}
	
	public NFSHost Get(int i) {
		return hosts.get(i);
	}
}
