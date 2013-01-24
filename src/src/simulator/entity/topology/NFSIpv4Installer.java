package simulator.entity.topology;

import simulator.entity.NFSNode;
import simulator.entity.NFSNodesContainer;

public class NFSIpv4Installer {
	
	public void assignIPAddress(String ipaddress, NFSNode node) {
		node.AssignIPAddress(ipaddress);
	}
	
	/**
	 * assign ip address to hostscontainer
	 * @param ipbase， the ip base address, extract A, B, C segment from the provided parameter
	 * @param startAddress, the first address of the ip range to be allocated
	 * @param nodes, the hostscontainer which 
	 * @param startIdx. the first host in the container to be processed
	 * @param endIdx, the last host in the container to be processed
	 */
	public void assignIPAddress(String ipbase, int startAddress, NFSNodesContainer nodes, int startIdx, int endIdx) {
		try {
			for (int i = startIdx, j = startAddress; i < endIdx; i++, j++) {
				String ipaddr = ipbase.substring(0, ipbase.lastIndexOf(".")) + "." + j;
				nodes.Get(i).AssignIPAddress(ipaddr);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
