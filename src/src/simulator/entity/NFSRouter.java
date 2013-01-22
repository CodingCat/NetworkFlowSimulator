package simulator.entity;


import java.util.ArrayList;
import java.util.HashMap;

import desmoj.core.simulator.Model;

public class NFSRouter extends NFSNode {
	
	private HashMap<String, ArrayList<NFSLink> > routetable = null;//dst ip range -> links
	private HashMap<String, NFSLink> lanLinks = null;//the connected host ip address -> links
	
	public NFSRouter(Model model, String entityName, boolean showInLog,
			double bandWidth, String ipaddr) {
		super(model, entityName, showInLog, bandWidth, ipaddr);
		routetable = new HashMap<String, ArrayList<NFSLink> >();
		lanLinks = new HashMap<String, NFSLink>();
	}
	
	/**
	 * add the routet table entry
	 * @param dst. the destination iprange, (format 192.168.1.0) if the destination is in the local subset
	 * then dst is the concrete ip address 
	 * @param nexthop. the ipaddress of the next hop point 
	 */
	public void addRoute(String dst, String nexthop){
		if (routetable.containsKey(dst) == false) {
			routetable.put(dst, new ArrayList<NFSLink>());
		}
		//TODO: need to be checked
		NFSLink link = new NFSLink(getModel(), "link", true, 100);
		routetable.get(dst).add(link);
	}
	
	public void registerLocalHosts(String hostip, double rate) {
		NFSLink link = new NFSLink(getModel(), "incoming link from " + hostip, true, rate);
		lanLinks.put(hostip, link);
	}
	
	public void Forward(NFSFlow flow) {
		try {
			//get the destination ip range
			String dstiprange = flow.dst.substring(0, flow.dst.lastIndexOf(".")) + ".0";
			//lookup the lan links to check if it's in the local subset
			if (lanLinks.containsKey(flow.dst)) {
				//TODO:send out the flow
				return ;
			}
			
			//search the route table
			if (routetable.containsKey(dstiprange) == false) {
				throw new Exception("dead path");
			}
			else {
				int availablePathNum = routetable.get(dstiprange).size();
				int selectedIndex = flow.dst.hashCode() % availablePathNum;
				String nextHopAddr = (String) outLinks.keySet().toArray()[selectedIndex];
				/*
				StartFlowEvent evt = new StartFlowEvent(ownermodel, 
						"Start New flow from " + this.toString() + " to " + nextHopAddr, true);
				evt.schedule((NFSNode)this, (NFSNode)outLinks.get(nextHopAddr).dst, flow, new TimeInstant(0));
				*/
				//TODO:send out the flows
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
