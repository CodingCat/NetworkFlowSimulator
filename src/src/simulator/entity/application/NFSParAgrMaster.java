package simulator.entity.application;

import java.util.Random;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.events.NFSParAgrMasterCompleteEvent;
import simulator.model.NFSModel;
import desmoj.core.report.Reporter;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.Reportable;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSParAgrMaster extends Entity {

	class NFSParAgrMasterInfo extends Reportable {

		private double idealthroughput = 0;
		private double actualthroughput = 0;

		public NFSParAgrMasterInfo(Model model, String name,
				boolean showInReport, boolean showInTrace) {
			super(model, name, showInReport, showInTrace);
		}
		
		public NFSParAgrMasterReporter createReporter() {
			return new NFSParAgrMasterReporter(this);
		}
	}

	class NFSParAgrMasterReporter extends Reporter {

		public NFSParAgrMasterReporter(Reportable infosource) {
			super(infosource);
			numColumns = 3;
			columns = new String[numColumns];
			columns[0] = "TaskID";
			columns[1] = "IdealThroughput";
			columns[2] = "ActualThroughput";
			groupHeading = "Partition Aggeregate";
			groupID = 891029;
			entries = new String[numColumns];
		}

		@Override
		public String[] getEntries() {
			if (source instanceof NFSParAgrMasterInfo) {
				entries[0] = ((NFSParAgrMasterInfo) source).getName();
				entries[1] = String.valueOf(((NFSParAgrMasterInfo) source).idealthroughput);
				entries[2] = String.valueOf(((NFSParAgrMasterInfo) source).actualthroughput);
			}
			return entries;
		}
	}
	
	
	private TimeInstant startTime = null;
	private TimeInstant finishTime = null;
	private double countinSize = 0.0;
	private double demandSize = 0.0;
	private double deadline = 0.0;
	private double throughput = 0.0;
	private int outfactor = 0;
	
	private NFSParAgrLeader [] leaders = null;
	private NFSParAgrMasterCompleteEvent bindedevent = null;
	private NFSHost tracker = null;
	
	NFSParAgrMasterInfo masterinfo = null;
	NFSParAgrMasterReporter masterreporter = null;
	
	public NFSParAgrMaster(Model model, String entityName, boolean showInTrace, 
			NFSHost host) {
		super(model, entityName, showInTrace);
		bindedevent = new NFSParAgrMasterCompleteEvent(model, entityName + "CompleteEvent", 
				true);
		deadline = NetworkFlowSimulator.parser.getDouble(
				"fluidsim.application.pa.master.deadline", 0.06);
		outfactor = NetworkFlowSimulator.parser.getInt(
				"fluidsim.application.pa.master.outfactor", 5);
		leaders = new NFSParAgrLeader[outfactor];
		tracker = host;
		masterinfo = new NFSParAgrMasterInfo(model, entityName, NFSModel.showPATask, true);
		masterreporter = new NFSParAgrMasterReporter(masterinfo);
	}
	
	
	public void run() {
		Random rand = new Random(System.currentTimeMillis());
		startTime = presentTime();
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
		for (NFSParAgrLeader leader : leaders) {
			if (leader == null) continue;
			demandSize += leader.getDemandSize();
			countinSize += leader.getCountSize();
		}
		masterinfo.actualthroughput = countinSize;
		masterinfo.idealthroughput = demandSize;
		finishTime = presentTime();
		throughput = countinSize / (TimeOperations.diff(finishTime, startTime).getTimeAsDouble());
	}
	
	public double getThroughput() {
		return throughput;
	}
}
