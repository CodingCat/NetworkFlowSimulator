package simulator.entity.topology;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import simulator.entity.NFSHost;
import simulator.entity.NFSHostsContainer;

public class NFSTopologyController extends Entity {

	NFSHostsContainer allhosts = null;
	
	public NFSTopologyController(Model model, String entityName, boolean showInTrace) {
		super(model, entityName, showInTrace);
		allhosts = new NFSHostsContainer(model, "allhosts", true, "allhosts");
	}
	
	/**
	 * add the host to the topology controller so that the topocontroller can generate traffic mapping
	 * @param newhosts. the hosts to be added in 
	 */
	public void registerHosts(NFSHostsContainer newhosts) {
		for (NFSHost host : newhosts) {
			allhosts.addHost(host);
		}
	}
	
	/**
	 * 
	 * @return return all hosts in the topology 
	 */
	public NFSHostsContainer allHosts() {
		return allhosts;
	}
	
	/**
	 * 
	 * @return number of hosts in the topology
	 */
	public int getHostN() {
		return allhosts.GetN();
	}
	
	/**
	 * get host 
	 * @param i, the index of host
	 * @return the host instance
	 */
	public NFSHost getHost(int i) {
		return allhosts.Get(i);
	}
	
	/**
	 * get particular host ip
	 * @param index, index of the host in the system
	 * @return particular host ip
	 */
	public String getHostIP(int index) {
		return allhosts.Get(index).ipaddress;
	}
}
