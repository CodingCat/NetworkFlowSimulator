package simulator.entity;


import java.util.ArrayList;
import java.util.HashMap;

import simulator.entity.application.NFSFlow;
import simulator.entity.topology.NFSLink;



import desmoj.core.simulator.Model;

public class NFSRouter extends NFSNode {
	
	private HashMap<String, ArrayList<NFSLink> > routetable = null;//dst ip range -> links
	private HashMap<String, NFSLink> lanLinks = null;//the connected host ip address -> links
	private ArrayList<String> IPs;
	static public enum RouterType { 
		Aggererate,
		Distribution,
		Core;
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
	
	public void SetRouterType(RouterType type) {
		routertype = type;
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
	
	public void registerIncomingLink(NFSNode node, double rate) {
		if (node.getClass().equals(NFSRouter.class)) {
			registerIncomingLink((NFSRouter)node, rate);
		}
		if (node.getClass().equals(NFSHost.class)) {
			registerIncomingLink((NFSHost)node, rate);
		}
	}
	
	private void registerIncomingLink(NFSHost node, double rate) {
		NFSLink link = new NFSLink(getModel(), "incoming link from " + node, true, rate, node, this);
		lanLinks.put(node.toString(), link);
	}
	
	private void registerIncomingLink(NFSRouter router, double rate) {
		NFSLink link = new NFSLink(getModel(), "incoming link from " + router, true, rate, router, this);
		String iprangekey = router.ipaddress.substring(0, router.ipaddress.lastIndexOf(".")) + ".0";
		lanLinks.put(iprangekey, link);
	}
	
	@Override
	public void AssignIPAddress(String ip) {
		if (IPs.size() == 0) {
			System.out.println(this.toString() + ":" + ip);
			super.AssignIPAddress(ip);
			IPs.add(ipaddress);
		}
		else {
			//System.out.println(this.toString() + ":" + ip);
			IPs.add(ip);
		}
	}
	
	
	public void ReceiveFlow(NFSFlow flow) {
		try {
			if (routertype == null) throw new Exception("unindicated router type");
			
			NFSNode nexthopNode = null;
			String dstcrange = flow.dstipString.substring(0, flow.dstipString.lastIndexOf(".")) + ".0";
			String localcrange = this.ipaddress.substring(0, flow.dstipString.lastIndexOf(".")) + ".0";
			//get the building tag
			//get the later 3 segment
			String dstlater3seg = flow.dstipString.substring(flow.dstipString.indexOf(".") + 1, 
					flow.dstipString.length());
			String dstbuildingTag = dstlater3seg.substring(0, dstlater3seg.indexOf("."));
			if (routertype.equals(RouterType.Aggererate)) {
				if (dstcrange.equals(localcrange)) {
					//in the same lan
					if (lanLinks.containsKey(flow.dstipString)) nexthopNode = lanLinks.get(flow.dstipString).src;
				}
				else{
					//send through arbitrary outlinks
					nexthopNode = ChooseECMPLink(flow.dstipString, (NFSLink[]) outLinks.values().toArray()).dst;
				}
			}
			else {
				if (routertype.equals(RouterType.Distribution)) {
					String locallater3seg = this.ipaddress.substring(this.ipaddress.indexOf(".") + 1, 
							this.ipaddress.length());
					String localbuildingTag = locallater3seg.substring(0, locallater3seg.indexOf("."));
					if (dstbuildingTag.equals(localbuildingTag)) {
						//local query
						if (lanLinks.containsKey(dstcrange)) nexthopNode = lanLinks.get(dstcrange).src;
					}
					else {
						//send through arbitrary link
						nexthopNode = ChooseECMPLink(flow.dstipString, (NFSLink[]) outLinks.values().toArray()).dst;
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
						int selectedIdx = flow.dstipString.hashCode() % outLinks.size();
						nexthopNode = potentialLinks.get(selectedIdx).src;
					}
					else {
						//send out
						nexthopNode = (NFSNode) outLinks.values().toArray()[0];
					}
				}
			}
			if (nexthopNode == null) {
				throw new Exception("could not find ip: " + flow.dstipString);
			}
			//TODO:schedule receiveflowevent with the target host
		}
		catch (Exception e) {
			e.printStackTrace();
		}
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
