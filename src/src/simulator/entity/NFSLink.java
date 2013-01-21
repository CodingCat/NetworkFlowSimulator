package simulator.entity;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSLink extends Entity{
	
	double datarate = 0.0f;
	NFSNode src = null;
	NFSNode dst = null;
	
	public NFSLink(Model model, String entityName, boolean showInLog, double rate) {
		super(model, entityName, showInLog);
		datarate = rate;
	}

}
