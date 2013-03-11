package simulator.entity;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;



import desmoj.core.simulator.Model;

public class NFSRouter extends NFSNode {
	
	private HashMap<String, ArrayList<NFSLink> > routetable = null;//dst ip range -> links
	private HashMap<String, NFSLink> lanLinks = null;//the connected host ip address -> links
	private ArrayList<String> IPs;
	static public enum RouterType { 
		Edge(0), Distribution(1), Core(2);
		
		private final int type;

		private RouterType(int value) {
			this.type = value;
		}

		public int getType() {
			return type;
		}
	};
	private RouterType routertype = null;
	
	public NFSRouter(Model model, String entityName, boolean showInLog,
			double bandWidth, String ipaddr) {
		super(model, entityName, showInLog, bandWidth, ipaddr);
		routetable = new HashMap<String, ArrayList<NFSLink> >();
		lanLinks = new HashMap<String, NFSLink>();
		IPs = new ArrayList<String>();
	}
	
	public NFSRouter(Model model, String entityName, boolean showInLog,
			double bandWidth, String ipaddr, RouterType type) {
		super(model, entityName, showInLog, bandWidth, ipaddr);
		routetable = new HashMap<String, ArrayList<NFSLink> >();
		lanLinks = new HashMap<String, NFSLink>();
		IPs = new ArrayList<String>();
		routertype = type;
	}
	
	public void setRouterType(RouterType type) {
		routertype = type;
	}
	
	public RouterType getRouterType() {
		return routertype;
	}
	
	/**
	 * add the routet table entry
	 * NOTICE, this table only stores the outgoing path, if the destination is in the local lan, the router
	 * will search the port directly, see Forward(NFSFlow flow)
	 * @param dst. the destination iprange, (format 192.168.1.0) if the destination is in the local subset
	 * then dst is the concrete ip address 
	 * @param nexthop. the ipaddress of the next hop point 
	 */
	public void addRoute(String dst, String nexthop){
		if (routetable.containsKey(dst) == false) {
			routetable.put(dst, new ArrayList<NFSLink>());
		}
	}
	
	/**
	 * public api for register incoming link 
	 * @param node, the node connecting this node
	 * @param rate, the bandwidth of the link
	 */
	public void registerIncomingLink(NFSLink link, double rate) {
		NFSNode node = link.src;
		if (node.getClass().equals(NFSRouter.class)) {
			String iprangekey = node.ipaddress.substring(0, node.ipaddress.lastIndexOf(".")) + ".0";
			lanLinks.put(iprangekey, link);
		}
		if (node.getClass().equals(NFSHost.class)) {
		//	System.out.println("register incoming link:" + node.toString());
			lanLinks.put(node.toString(), link);
		}
	}
	
	@Override
	public void assignIPAddress(String ip) {
		if (IPs.size() == 0) {
		//	System.out.println(this.toString() + ":" + ip);
			super.assignIPAddress(ip);
			IPs.add(ipaddress);
		}
		else {
			//System.out.println(this.toString() + ":" + ip);
			IPs.add(ip);
		}
	}
	
	
	public NFSNode receiveFlow(NFSFlow flow) {
		try {
			if (routertype == null) throw new Exception("unindicated router type");
			
			NFSNode nexthopNode = null;
			NFSLink outgoingPath = null;
			String dstCrange = flow.dstipString.substring(0, flow.dstipString.lastIndexOf(".")) + ".0";
			String localCrange = this.ipaddress.substring(0, this.ipaddress.lastIndexOf(".")) + ".0";
			//get the building tag
			//get the later 3 segment
			String dstlater3seg = flow.dstipString.substring(flow.dstipString.indexOf(".") + 1, 
					flow.dstipString.length());
			String dstbuildingTag = dstlater3seg.substring(0, dstlater3seg.indexOf("."));
			if (routertype.equals(RouterType.Edge)) {
				if (dstCrange.equals(localCrange)) 
				{	
					//in the same lan
					if (lanLinks.containsKey(flow.dstipString)) {
						outgoingPath = lanLinks.get(flow.dstipString);
						nexthopNode = outgoingPath.src;
					}
				}
				else{
					//send through arbitrary outlinks to distribution layer
					outgoingPath = flowscheduler.schedule(flow);
					nexthopNode = outgoingPath.dst;
				}
			}
			else {
				if (routertype.equals(RouterType.Distribution)) {
					String locallater3seg = this.ipaddress.substring(this.ipaddress.indexOf(".") + 1, 
							this.ipaddress.length());
					String localbuildingTag = locallater3seg.substring(0, locallater3seg.indexOf("."));
					if (dstbuildingTag.equals(localbuildingTag)) {
						//local query
						if (lanLinks.containsKey(dstCrange)) {
							outgoingPath = lanLinks.get(dstCrange);
							nexthopNode = outgoingPath.src;
						}
					}
					else {
						//send through arbitrary link to the core
						outgoingPath = flowscheduler.schedule(flow);
						nexthopNode = outgoingPath.dst;
					}
				}
				else {
					//Must be core
					//local query
					ArrayList<NFSLink> potentialLinks = new ArrayList<NFSLink>();
					for (String link: lanLinks.keySet()) {
						String linklater3seg = link.substring(link.indexOf(".") + 1, link.length());
						String linkbuildingTag = linklater3seg.substring(0, linklater3seg.indexOf("."));
						if (linkbuildingTag.equals(dstbuildingTag)) {
							potentialLinks.add(lanLinks.get(link));
						}
					}
					if (potentialLinks.size() != 0) {
						int selectedIdx = flow.dstipString.hashCode() % potentialLinks.size();
						selectedIdx = selectedIdx > 0 ? selectedIdx : -selectedIdx;
						outgoingPath = potentialLinks.get(selectedIdx);
						nexthopNode = outgoingPath.src;
					}
					else {
						//send out
						outgoingPath = (NFSLink) outLinks.get(0);
						nexthopNode = outgoingPath.dst;
					}
				}
			}
			if (nexthopNode == null) {
				String pathStr = "";
				for (int i = 0; i < flow.getPaths().size(); i++) {
					pathStr += (flow.getPaths().get(i).getName() + " ");
				}
				throw new Exception("could not find ip: " + flow.dstipString + 
						" Router Type:" + this.routertype.toString() + " Router IP:" +
						this.ipaddress + " " + pathStr);
			}
			//update involved the objects
			outgoingPath.addRunningFlow(flow);
			flow.addPath(outgoingPath);
			return nexthopNode;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
		
	/**
	 * get the lan link by the link source ip
	 * @param srcip, the source ip
	 * @return, return the link object
	 */
	public NFSLink getLanLink(String srcip) {
		return lanLinks.get(srcip);
	}
	
	public Set<String> getPods() {
		return lanLinks.keySet();
	}
	
	@Override
	public void PrintLinks() {
		System.out.println("Allocated IPs");
		System.out.println("====================================================");
		for (int i = 0; i < IPs.size(); i++) {
			System.out.print(IPs.get(i) + "	");
		}
		System.out.println();
		System.out.println("Outgoing Links:");
		super.PrintLinks();
		System.out.println("====================================================");
		System.out.println("Incoming Links:");
		for (int i = 0 ; i < this.lanLinks.values().size(); i++) {
			System.out.println(this.lanLinks.values().toArray()[i]);
		}
		System.out.println("====================================================");
	}
}
