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
import simulator.events.NFSCloseOnOffFlowEvent;
import simulator.events.NFSReceiveFlowEvent;
import simulator.events.NFSStartNewFlowEvent;
import simulator.model.NFSModel;
import simulator.utils.NFSIntegerEntity;

public class NFSOnOffApplication extends NFSApplication {

	int onDurationUpbound = 0;
	int offDurationUpbound = 0;
	int onDurationLowbound = 0;
	int offDurationLowbound = 0;
	
	private NFSFlow bindingflow = null;
	
	public NFSOnOffApplication(Model model, String entityName,
			boolean showInTrace, 
			double dr, 
			NFSHost machine) {
		super(model, entityName, showInTrace, dr, machine);
		initialize();
	}
	
	/**
	 * initialize the data members in this class 
	 */
	protected void initialize() {
		onDurationUpbound = NetworkFlowSimulator.parser.getInt("fluidsim.application.onoff.maxonduration", 40);
		offDurationUpbound = NetworkFlowSimulator.parser.getInt("fluidsim.application.onoff.maxoffduration", 40);
		onDurationLowbound = NetworkFlowSimulator.parser.getInt("fluidsim.application.onoff.minonduration", 20);
		offDurationLowbound = NetworkFlowSimulator.parser.getInt("fluidsim.application.onoff.minoffduration", 20);

	}
	
	
	@Override
	public void start() {
		if (bindingflow == null) {
			bindingflow = new NFSFlow(
					getModel(), 
					"flow-" + hostmachine.ipaddress + "-" + NFSModel.trafficcontroller.getPermuMatrixTarget(hostmachine.ipaddress), 
					true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.onoff.rate", 0.5));
			//set the source address
			bindingflow.srcipString = hostmachine.ipaddress;
			//set the destination address
			bindingflow.dstipString = NFSModel.trafficcontroller.getPermuMatrixTarget(bindingflow.srcipString);
		}
		bindingflow.expectedrate = bindingflow.demandrate;
		bindingflow.setStatus(NFSFlow.NFSFlowStatus.NEWSTARTED);
		NFSLink passLink = hostmachine.startNewFlow(bindingflow);
		//schedule receive flow event
		NFSReceiveFlowEvent receiveflowevent = new NFSReceiveFlowEvent(
				getModel(), 
				"receiveflow-" + bindingflow.srcipString + "-" + bindingflow.dstipString, true);
		receiveflowevent.setSchedulingPriority(1);
		receiveflowevent.schedule(hostmachine, (NFSRouter) passLink.dst, bindingflow, presentTime());

		Random rand = new Random(System.currentTimeMillis());
		//schedule close event 
		NFSCloseOnOffFlowEvent closeevent = new NFSCloseOnOffFlowEvent(getModel(), 
				"closeflow-" + bindingflow.srcipString + "-" + bindingflow.dstipString, true);
		closeevent.schedule(hostmachine, bindingflow, 
				TimeOperations.add(presentTime(), 
						new TimeSpan(rand.nextInt(onDurationLowbound + 1) + 
								onDurationUpbound - onDurationLowbound)));
	}

	@Override
	public void close() {
		Random rand = new Random(System.currentTimeMillis());
		bindingflow.setStatus(NFSFlow.NFSFlowStatus.CLOSED);
		NFSStartNewFlowEvent newflowevent = new NFSStartNewFlowEvent(
				getModel(),
				"startflow-" + bindingflow.srcipString + "-" + bindingflow.dstipString,
				true);
		newflowevent.schedule(this.hostmachine, 
				new NFSIntegerEntity(getModel(), "onoffintentity", false, 0),
				TimeOperations.add(presentTime(), 
						new TimeSpan(rand.nextInt(offDurationLowbound + 1) + offDurationUpbound - offDurationLowbound)));
	}
}
