package simulator.entity.topology;

import java.util.ArrayList;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSRouter;

public class NFSNetworksBackbone extends Entity{
	
	NFSRouter [] coreswitches = null;
	ArrayList<NFSRouter> distributionSwitches = null;
	
	public NFSNetworksBackbone(Model model, 
			String entityname, 
			boolean debugmodel, 
			int n) {
		super(model, entityname, debugmodel);
		coreswitches = new NFSRouter[n];
		distributionSwitches = new ArrayList<NFSRouter>();
		for (int i = 0; i < n; i++) {
			coreswitches[i] = new NFSRouter(getModel(), "core " + i, true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.topology.corebandwidth", 1000), 
					null);
		}
	}
	

	
	public void connect(NFSBuilding [] buildinglist){
		//put all distribution switches to array list
		for (int i = 0; i < buildinglist.length; i++) {
			for (int j = 0; j < buildinglist[i].l3switches.GetN(); j++) {
				distributionSwitches.add(buildinglist[i].l3switches.Get(j));
			}
		}
		//TODO:assign ip address to core
		
	}
}
