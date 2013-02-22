package simulator.events;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSOFController;
import simulator.entity.flow.NFSOpenFlowMessage;
import desmoj.core.simulator.EventOf2Entities;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSOpenFlowSubscribeEvent extends EventOf2Entities<NFSHost, NFSFlow> {

	private NFSOFController controller = null;
	protected double pauseDuration = 0.0;
	protected int subscribecnt = 0;
	protected int subscribebound = 0;
	
	public NFSOpenFlowSubscribeEvent(Model model, String eventName, boolean showInTrace) {
		super(model, eventName, showInTrace);
		controller = NFSOFController._Instance(model);
		pauseDuration = NetworkFlowSimulator.parser.getDouble(
				"fluidsim.openflow.pauseEvent", 0.00025);
		subscribebound = NetworkFlowSimulator.parser.getInt(
				"fluidsim.openflow.subscribebound", 3);
	
	}
	
	private NFSOpenFlowMessage subscribe(NFSHost tasktracker, NFSFlow flow) {
		NFSOpenFlowMessage msg = controller.schedule(tasktracker.getOutlink(), flow);
		if (msg == null) {
			if ((subscribecnt++) < subscribebound) {
				NFSOpenFlowSubscribeEvent subevent = new NFSOpenFlowSubscribeEvent(
						getModel(), getName() + "SubscribeEvent-" + flow.getName(), true);
				subevent.schedule(tasktracker, flow, 
						TimeOperations.add(presentTime(), new TimeSpan(pauseDuration)));
			}
		}
		return msg;
	}

	@Override
	public void eventRoutine(NFSHost host, NFSFlow flow) {
		//in openflow model
		NFSOpenFlowMessage msg = subscribe(host, flow);
		if (msg != null) {
			//start the flow
			flow.start();
		}
		else {
			//re-subscribe bandwidth from the controller
			if (subscribecnt++ < subscribebound) {
				schedule(host, flow, TimeOperations.add(presentTime(), new TimeSpan(pauseDuration)));
			}
		}
	}

}
