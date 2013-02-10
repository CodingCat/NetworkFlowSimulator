package simulator.entity.application;

import java.util.Random;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;
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

	int onDurationUpbound = 0;
	int offDurationUpbound = 0;
	int onDurationLowbound = 0;
	int offDurationLowbound = 0;
	
	private NFSFlow bindingflow = null;
	
	public NFSOnOffApplication(Model model, String entityName,
			boolean showInTrace, double dr, NFSHost machine, 
			int ondurationmax, 
			int offdurationmax,
			int ondurationmin,
			int offdurationmin) {
		super(model, entityName, showInTrace, dr, machine);
		onDurationUpbound = ondurationmax;
		offDurationUpbound = offdurationmax;
		onDurationLowbound = ondurationmin;
		offDurationLowbound = offdurationmin;
	}
	
	@Override
	public void send() {
		if (bindingflow == null) {
			bindingflow = new NFSFlow(
					getModel(), 
					"flow-" + hostmachine.ipaddress + "-" + NFSModel.trafficcontroller.getTarget(hostmachine.ipaddress), 
					true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.onoff.rate", 0.5));
			//set the source address
			bindingflow.srcipString = hostmachine.ipaddress;
			//set the destination address
			bindingflow.dstipString = NFSModel.trafficcontroller.getTarget(bindingflow.srcipString);
		}
		bindingflow.setStatus(NFSFlow.NFSFlowStatus.NEWSTARTED);
		NFSLink passLink = hostmachine.startNewFlow(bindingflow);
		//schedule receive flow event
		NFSReceiveFlowEvent receiveflowevent = new NFSReceiveFlowEvent(
				getModel(), 
				"receiveflow-" + bindingflow.srcipString + "-" + bindingflow.dstipString, true);
		receiveflowevent.setSchedulingPriority(1);
		receiveflowevent.schedule((NFSRouter) passLink.dst, bindingflow, presentTime());

		Random rand = new Random(System.currentTimeMillis());
		//schedule close event 
		NFSCloseFlowEvent closeevent = new NFSCloseFlowEvent(getModel(), 
				"closeflow-" + bindingflow.srcipString + "-" + bindingflow.dstipString, true);
		closeevent.schedule(hostmachine, bindingflow, 
				TimeOperations.add(presentTime(), 
						new TimeSpan(rand.nextInt(onDurationLowbound + 1) + 
								onDurationUpbound - onDurationLowbound)));
	}

	@Override
	public void close() {
		Random rand = new Random(System.currentTimeMillis());
		NFSStartNewFlowEvent newflowevent = new NFSStartNewFlowEvent(
				getModel(),
				"startflow-" + bindingflow.srcipString + "-" + bindingflow.dstipString,
				true);
		newflowevent.schedule(this.hostmachine, 
				TimeOperations.add(presentTime(), 
						new TimeSpan(rand.nextInt(offDurationLowbound + 1) + offDurationUpbound - offDurationLowbound)));
	}
}
