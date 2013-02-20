package simulator.entity.application;

import java.util.Random;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.events.NFSParAgrMasterCompleteEvent;
import simulator.model.NFSModel;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSParAgrMaster extends Entity {

	private TimeInstant startTime = null;
	private TimeInstant finishTime = null;
	private double demandSize = 0.0;
	private double deadline = 0.0;
	private double throughput = 0.0;
	private int outfactor = 0;
	
	private NFSParAgrLeader [] leaders = null;
	private NFSParAgrMasterCompleteEvent bindedevent = null;
	private NFSHost tracker = null;
	
	public NFSParAgrMaster(Model model, String entityName, boolean showInTrace, 
			NFSHost host) {
		super(model, entityName, showInTrace);
		bindedevent = new NFSParAgrMasterCompleteEvent(model, entityName + "CompleteEvent", 
				true);
		deadline = NetworkFlowSimulator.parser.getDouble(
				"fluidsim.application.pa.master.deadline", 0.3);
		outfactor = NetworkFlowSimulator.parser.getInt(
				"fluidsim.application.pa.master.outfactor", 5);
		leaders = new NFSParAgrLeader[outfactor];
		tracker = host;
	}
	
	
	public void run() {
		Random rand = new Random(System.currentTimeMillis());
		startTime = presentTime();
		bindedevent.schedule(this, TimeOperations.add(presentTime(), 
				new TimeSpan(deadline)));
		int totalMachines = NFSModel.trafficcontroller.topocontroller.getHostN();
		for (int i = 0; i < outfactor; i++) {
			NFSHost leaderhost = NFSModel.trafficcontroller.topocontroller.getHost(
					rand.nextInt(totalMachines));
			if (tracker.ipaddress.equals(leaderhost.ipaddress)) continue;
			leaders[i] = new NFSParAgrLeader(getModel(),
					"NFSPALeader-" + getName() + "-" + leaderhost.ipaddress,
					true, 
					leaderhost);
			leaders[i].run();
		}
		bindedevent.schedule(this, TimeOperations.add(presentTime(), 
				new TimeSpan(deadline)));
	}
	
	public void finish() {
		for (NFSParAgrLeader leader : leaders) demandSize += leader.getSize();
		finishTime = presentTime();
		throughput = demandSize / (TimeOperations.diff(finishTime, startTime).getTimeAsDouble());
	}
	
	public double getThroughput() {
		return throughput;
	}
}
