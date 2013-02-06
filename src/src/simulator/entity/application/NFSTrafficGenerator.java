package simulator.entity.application;

import java.util.HashMap;

import simulator.entity.topology.NFSTopologyController;

public abstract class NFSTrafficGenerator {

	protected NFSTopologyController topocontroller = null;
	
	protected HashMap<String, String> trafficMapping = new HashMap<String, String>();
	
	protected abstract String getTarget(String srcip);
}
