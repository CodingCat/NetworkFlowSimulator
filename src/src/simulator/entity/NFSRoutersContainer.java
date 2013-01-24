package simulator.entity;

import java.util.ArrayList;

import simulator.NetworkFlowSimulator;
import desmoj.core.simulator.Model;

public class NFSRoutersContainer extends NFSNodesContainer{
	
	private ArrayList<NFSRouter> routers = null;
	
	public NFSRoutersContainer(Model model, String entityName, boolean showInReport) {
		super(model, entityName, showInReport);
		routers = new ArrayList<NFSRouter>();
	}
	
	@Override
	public void create(int n) {
		for (int i = 0; i < n; i++) {
			NFSRouter node = new NFSRouter(getModel(), "node " + i, 
					true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.node.default.bandwidth", 1024.0),
					null);
			routers.add(node);
		}
	}

	public int GetN() {
		return routers.size();
	}
	
	public NFSRouter Get(int i) {
		return routers.get(i);
	}
	
}
