package simulator.entity.application;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;
import simulator.model.NFSModel;

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
	public void send() {
		//TODO: start the new flow
		NFSFlow newflow = new NFSFlow(getModel(), "flow", true, 
				NetworkFlowSimulator.parser.getDouble("fluidsim.application.onoff.rate", 0.5));
		NFSLink passLink = hostmachine.AddNewFlow(newflow);
		//schedule receive flow event
	}
}
