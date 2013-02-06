package simulator.entity.topology;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import simulator.entity.NFSHost;
import simulator.entity.NFSHostsContainer;

public class NFSTopologyController extends Entity {

	NFSHostsContainer allhosts = null;
	
	public NFSTopologyController(Model model, String entityName, boolean showInTrace) {
		super(model, entityName, showInTrace);
		allhosts = new NFSHostsContainer(model, "allhosts", true);
	}
	
	public void registerHosts(NFSHostsContainer newhosts) {
		for (NFSHost host : newhosts) allhosts.addHost(host);
	}
	
	public NFSHostsContainer allHosts() {
		return allhosts;
	}
}
