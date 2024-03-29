package simulator.entity.topology;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSNode;

public class NFSPointToPointInstaller {
	public void assignIPAddress(String ipaddress, NFSNode node) {
		node.assignIPAddress(ipaddress);
	}
	
	public void Link(NFSNode src, NFSNode dst) {
		src.addNewLink(dst, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.topology.p2plinkrate", 1000.0));
		
	}
}
