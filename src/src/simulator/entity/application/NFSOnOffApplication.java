package simulator.entity.application;

import desmoj.core.simulator.Model;
import simulator.entity.NFSHost;

public class NFSOnOffApplication extends NFSApplication {

	double onDurationUpbound = 0.0;
	double offDurationUpbound = 0.0;
	
	public NFSOnOffApplication(Model model, String entityName,
			boolean showInTrace, double dr, NFSHost machine, double onduration, double offduration) {
		super(model, entityName, showInTrace, dr, machine);
		onDurationUpbound = onduration;
		offDurationUpbound = offduration;
	}
	
	@Override
	public void Send() {
		//TODO: start the new flow
		NFSFlow newflow = new NFSFlow(getModel(), "flow", true);
		hostmachine.AddNewFlow(newflow);
	}
}
