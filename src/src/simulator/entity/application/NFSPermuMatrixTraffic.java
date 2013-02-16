package simulator.entity.application;

import java.util.Random;

import desmoj.core.simulator.Model;

import simulator.entity.NFSHost;
import simulator.entity.topology.NFSTopologyController;

public class NFSPermuMatrixTraffic extends NFSTrafficGenerator {

	public NFSPermuMatrixTraffic(Model model, String entityName, boolean showInReport, 
			NFSTopologyController topocontroller) {
		super(model, entityName, showInReport, topocontroller);
	}
	
	@Override
	protected void init() {
		super.init();
		buildflowmap();
	}
	
	private void buildflowmap() {
		Random rand = new Random(System.currentTimeMillis());
		int hostN = topocontroller.getHostN();
		int dstHostIdx = 0;
		for (NFSHost host : topocontroller.allHosts()) {
			dstHostIdx = rand.nextInt(hostN);
			String dstip = topocontroller.getHostIP(dstHostIdx);
			while (oneToOneTrafficMap.containsValue(dstip) || host.ipaddress.equals(dstip)) {
				dstHostIdx = rand.nextInt(hostN);
				dstip = topocontroller.getHostIP(dstHostIdx);
			}
			oneToOneTrafficMap.put(host.ipaddress, dstip);
		}
	}

	@Override
	@Deprecated
	public String[] getOneToManyTarget(int targetsnum) {
		return null;
	}

	@Override
	public void run() {
		for (NFSHost host : topocontroller.allHosts()) host.run();
	}
}
