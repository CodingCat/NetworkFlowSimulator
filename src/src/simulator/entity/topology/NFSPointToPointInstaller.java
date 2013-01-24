package simulator.entity.topology;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSNode;
import simulator.entity.NFSRouter;

public class NFSPointToPointInstaller {
	public void assignIPAddress(String ipaddress, NFSNode node) {
		node.AssignIPAddress(ipaddress);
	}
	
	public void Link(String ipbase, int startAddress, NFSNode src, NFSRouter dst) {
		try {
			String ip1 = ipbase.substring(0, ipbase.lastIndexOf(".")) + "." + startAddress;
			String ip2 = ipbase.substring(0, ipbase.lastIndexOf(".")) + "." + (startAddress + 1);
			src.AssignIPAddress(ip1);
			dst.AssignIPAddress(ip2);
			src.AddNewLink(dst, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.topology.p2plinkrate", 1000.0));
			dst.registerIncomingLink(src, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.topology.p2plinkrate", 1000.0));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
