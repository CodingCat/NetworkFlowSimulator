package simulator.entity.application;

import java.util.Random;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.NFSRouter;
import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;
import simulator.events.NFSCloseFlowEvent;
import simulator.events.NFSReceiveFlowEvent;
import simulator.events.NFSStartNewFlowEvent;
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
		NFSFlow newflow = new NFSFlow(getModel(), "flow", true, 
				NetworkFlowSimulator.parser.getDouble("fluidsim.application.onoff.rate", 0.5));
		//set the source address
		newflow.srcipString = hostmachine.ipaddress;
		//set the destination address
		newflow.dstipString = NFSModel.trafficcontroller.getTarget(newflow.srcipString);
		NFSLink passLink = hostmachine.startNewFlow(newflow);
		//schedule receive flow event
		NFSReceiveFlowEvent receiveflowevent = new NFSReceiveFlowEvent(
				getModel(), "receiveflow", true);
		receiveflowevent.setSchedulingPriority(1);
		receiveflowevent.schedule((NFSRouter) passLink.dst, newflow, new TimeInstant(0));

		Random rand = new Random(System.currentTimeMillis());
		//schedule close event 
		NFSCloseFlowEvent closeevent = new NFSCloseFlowEvent(getModel(), "closeflow", true);
		closeevent.schedule((NFSHost)passLink.src, 
				newflow, 
				new TimeInstant(rand.nextDouble() % onDurationUpbound));
	}

	@Override
	public void close() {
		NFSStartNewFlowEvent newflowevent = new NFSStartNewFlowEvent(
				getModel(),
				"startflow",
				true);
		Random rand = new Random(System.currentTimeMillis());
		newflowevent.schedule(this.hostmachine, 
				new TimeInstant(rand.nextDouble() % offDurationUpbound));	
	}
}
