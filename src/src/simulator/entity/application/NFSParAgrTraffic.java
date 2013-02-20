package simulator.entity.application;

import simulator.entity.topology.NFSTopologyController;
import desmoj.core.simulator.Model;

public class NFSParAgrTraffic extends NFSTrafficGenerator {

	public NFSParAgrTraffic(Model model, String entityName,
			boolean showInReport, NFSTopologyController tpctrl) {
		super(model, entityName, showInReport, tpctrl);
	}

	@Override
	public String[] getRandomTargets(int targetsnum) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
