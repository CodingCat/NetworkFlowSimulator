package simulator.entity.application;

import simulator.entity.NFSHost;
import desmoj.core.simulator.Model;

public class NFSParAgrApplication extends NFSApplication {
	
	public NFSParAgrApplication(Model model, String entityName,
			boolean showInTrace, double dr, NFSHost machine) {
		super(model, entityName, showInTrace, dr, machine);
	}

	@Override
	public void start() {
		NFSParAgrMaster master = new NFSParAgrMaster(getModel(), 
				"master-" + hostmachine.ipaddress,
				true, 
				hostmachine);
		master.run();
	}

	@Override
	public void close() {
		//leave it empty
	}
}
