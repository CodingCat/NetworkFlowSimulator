package simulator.entity;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import simulator.events.StartFlowEvent;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;

public class NFSRouter extends NFSNode {
	
	private HashMap<String, ArrayList<String> > routetable = null;
	
	public NFSRouter(Model model, String entityName, boolean showInLog,
			double bandWidth) {
		super(model, entityName, showInLog, bandWidth);
		routetable = new HashMap<String, ArrayList<String> >();
	}
	
	//statically build the route table
	public void addRoute(String dst, String nexthop){
		if (routetable.containsKey(dst) == false) {
			routetable.put(dst, new ArrayList<String>());
		}
		routetable.get(dst).add(nexthop);
	}
	
	public void Forward(NFSFlow flow) {
		try {
			//get the destination ip range
			String dstiprange = flow.dst.substring(0, flow.dst.lastIndexOf(".")) + ".0";
			//search the route table
			if (routetable.containsKey(dstiprange) == false) {
				throw new Exception("dead path");
			}
			else {
				int availablePathNum = routetable.get(dstiprange).size();
				int selectedIndex = (new Random(System.currentTimeMillis())).nextInt() % availablePathNum;
				String nextHopAddr = (String) outLinks.keySet().toArray()[selectedIndex];
				StartFlowEvent evt = new StartFlowEvent(ownermodel, 
						"Start New flow from " + this.toString() + " to " + nextHopAddr, true);
				evt.schedule((NFSNode)this, (NFSNode)outLinks.get(nextHopAddr).dst, flow, new TimeInstant(0));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
