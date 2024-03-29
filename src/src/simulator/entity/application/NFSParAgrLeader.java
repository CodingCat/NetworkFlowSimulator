package simulator.entity.application;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.NFSRouter;
import simulator.entity.flow.NFSPAFlow;
import simulator.entity.flow.NFSFlow.NFSFlowStatus;
import simulator.entity.topology.NFSLink;
import simulator.events.NFSOpenFlowSubscribeEvent;
import simulator.events.NFSReceiveFlowEvent;
import simulator.model.NFSModel;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSParAgrLeader extends Entity {
	
	private double countinsize = 0.0;
	private int outfactor = 0;
	private NFSPAFlow [] flows = null;
	private NFSHost tasktracker = null;
	private boolean openflowonoff = false;
	
	public NFSParAgrLeader(Model model, String entityName, boolean showInTrace, NFSHost tt) {
		super(model, entityName, showInTrace);
		tasktracker = tt;
		init();
	}
	
	private void init() {
		outfactor = NetworkFlowSimulator.parser.getInt(
				"fluidsim.application.pa.leader.outfactor", 43);
		flows = new NFSPAFlow[outfactor];
		openflowonoff = NetworkFlowSimulator.parser.getBoolean(
				"fluidsim.openflow.onoff", false);
	}
	
	public void run() {
		String [] receiverIPs = NFSModel.trafficcontroller.getRandomTargets(outfactor);
		for (int i = 0; i < outfactor; i++) {
			if (receiverIPs[i].equals(tasktracker.ipaddress)) continue;
			flows[i] = new NFSPAFlow(getModel(),
					"flows-" + tasktracker.ipaddress + "-" + receiverIPs[i],
					true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.pa.rate", 0.2),
					this);
			flows[i].srcipString = tasktracker.ipaddress;
			flows[i].dstipString = receiverIPs[i];
			flows[i].expectedrate = flows[i].demandrate;
			flows[i].setStatus(NFSFlowStatus.NEWSTARTED);
			if (openflowonoff == false) {
				NFSLink passLink = tasktracker.startNewFlow(flows[i]);
				//scheduler receive event
				NFSReceiveFlowEvent receiveEvent = new NFSReceiveFlowEvent(
						getModel(),
						"receiveflow-" + flows[i].srcipString + "-" + flows[i].dstipString, 
						true);
				receiveEvent.setSchedulingPriority(1);
				receiveEvent.schedule(tasktracker, (NFSRouter) passLink.dst, flows[i], presentTime());
			}
			else {
				NFSOpenFlowSubscribeEvent subevent = 
						new NFSOpenFlowSubscribeEvent(getModel(), tasktracker.getName() + "subEvent", true);
				subevent.schedule(tasktracker, flows[i], presentTime());
			}
		}
	}
	
	public void finish(NFSPAFlow finishflow) {
		if (finishflow.isInTime()) countinsize += finishflow.getDemandSize();
	}
	
	public double getDemandSize() {
		double sum = 0;
		for (NFSPAFlow flow : flows) {
			if (flow != null) sum += flow.getDemandSize();
		}
		return sum;
	}
	
	public double getCountSize() {
		return countinsize;
	}
}
