package simulator.entity.application;

import java.util.HashMap;

import simulator.entity.topology.NFSTopologyController;

public abstract class NFSTrafficGenerator {

	protected NFSTopologyController topocontroller = null;
	
	protected HashMap<String, String> trafficMapping = null;//src ip -> dst ip
	
	public NFSTrafficGenerator(NFSTopologyController tpctrl) {
		topocontroller = tpctrl;
		init();
	}
	
	protected void init() {
		trafficMapping = new HashMap<String, String>();
	}
	
	public String getTarget(String srcip) {
		return trafficMapping.get(srcip);
	}
}
