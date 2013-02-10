package simulator.entity.application;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import simulator.entity.NFSHost;

public abstract class NFSApplication extends Entity{

	protected double datarate = 0;// in kbps
	protected NFSHost hostmachine = null;
	
	public NFSApplication(Model model, String entityName, boolean showInTrace, 
			double dr, NFSHost machine) {
		super(model, entityName, showInTrace);
		datarate = dr;
		hostmachine = machine;
	}

	public abstract void send();
	
	public abstract void close();
}
