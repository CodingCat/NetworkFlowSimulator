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
				"fluidsim.openflow.pauseEvent", 0.06);
		subscribebound = NetworkFlowSimulator.parser.getInt(
				"fluidsim.openflow.subscribebound", 3);
	
	}
	
	@Override
	public void eventRoutine(NFSHost host, NFSFlow flow) {
		//in openflow model
		NFSOpenFlowMessage msg = controller.schedule(host.getOutlink(), flow);
		if (msg != null) {
			//start the flow
			flow.start();
		}
		else {
			if ((subscribecnt++) < subscribebound) {
				System.out.println("------------rejected for " + subscribecnt + " times-------------");
				schedule(host, flow, TimeOperations.add(presentTime(), new TimeSpan(pauseDuration)));
			}
		}
	}

}
