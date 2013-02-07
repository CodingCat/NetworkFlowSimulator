package simulator.entity.application;

import java.util.Random;

import simulator.entity.NFSHost;
import simulator.entity.topology.NFSTopologyController;

public class NFSPermuMatrixTraffic extends NFSTrafficGenerator {

	public NFSPermuMatrixTraffic(NFSTopologyController topocontroller) {
		super(topocontroller);
	}
	
	private void buildflowmap() {
		Random rand = new Random(System.currentTimeMillis());
		int hostN = topocontroller.getHostN();
		System.out.println("hostN:" + hostN);
		int dstHostIdx = 0;
		for (NFSHost host : topocontroller.allHosts()) {
			dstHostIdx = rand.nextInt(hostN);
			String dstip = topocontroller.getHostIP(dstHostIdx);
			while (trafficMapping.containsValue(dstip) || host.ipaddress.equals(dstip)) {
				dstHostIdx = rand.nextInt(hostN);
				dstip = topocontroller.getHostIP(dstHostIdx);
			}
			trafficMapping.put(host.ipaddress, dstip);
		}
	}
	
	@Override
	protected void init() {
		super.init();
		buildflowmap();
	}
}
