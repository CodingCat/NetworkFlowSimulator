package simulator.entity.topology;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.NFSHostsContainer;
import simulator.entity.NFSRouter;
import simulator.entity.NFSRoutersContainer;


public class NFSSwitchBasedLAN {
	
	/**
	 * build a local lan with the network gate router
	 * @param router. the network gate
	 * @param hosts. hosts in this lan
	 * @param startIdx. indicating the first host in the nfsnodecontainer which is going to be added to the lan
	 * @param endIdx. indicating teh last host in the nfsndoecontainer which is going to be added to the lan
	 */
	public void buildLan(NFSRouter router, NFSHostsContainer hosts, int startIdx, int endIdx) {
		try {
			if (router.HasAllocatedIP() == false) {
				throw new Exception("Router " + router + " hasn't got ipaddress");
			}
			//connect the host to link
			for (int i = startIdx; i < endIdx; i++) {
				NFSHost host = hosts.Get(i);
				if (host.HasAllocatedIP() == false) {
					throw new Exception(host + " hasn't got ipaddress");
				}
				host.addNewLink(router, 
						NetworkFlowSimulator.parser.getDouble("fluidsim.topology.locallinkrate", 10.0));
				router.registerIncomingLink(host, 
						NetworkFlowSimulator.parser.getDouble("fluidsim.topology.locallinkrate", 10.0));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * build a local lan with the network gate router
	 * @param router. the network gate
	 * @param hosts. hosts in this lan
	 * @param startIdx. indicating the first host in the nfsnodecontainer which is going to be added to the lan
	 * @param endIdx. indicating teh last host in the nfsndoecontainer which is going to be added to the lan
	 */
	public void buildLan(NFSRouter highlayerRouter, NFSRoutersContainer routers, int startIdx, int endIdx) {
		try {
			if (highlayerRouter.HasAllocatedIP() == false) {
				throw new Exception("Router " + highlayerRouter + " hasn't got ipaddress");
			}
			for (int i = startIdx; i < endIdx; i++) {
				NFSRouter lowLevelRouter = routers.Get(i);
				if (lowLevelRouter.HasAllocatedIP() == false) {
					throw new Exception(lowLevelRouter + " hasn't got ipaddress");
				}
				lowLevelRouter.addNewLink(highlayerRouter, 
						NetworkFlowSimulator.parser.getDouble("fluidsim.topology.crossrouterlinkrate", 1000.0));
				highlayerRouter.registerIncomingLink(lowLevelRouter, 
						NetworkFlowSimulator.parser.getDouble("fluidsim.topology.crossrouterlinkrate", 1000.0));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
