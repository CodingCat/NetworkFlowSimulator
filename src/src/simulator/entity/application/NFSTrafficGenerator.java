package simulator.entity.application;

import java.util.HashMap;
import java.util.Random;

import desmoj.core.dist.DiscreteDistPoisson;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.topology.NFSTopologyController;
import simulator.events.NFSStartNewFlowEvent;
import simulator.utils.NFSIntegerEntity;

public class NFSTrafficGenerator extends Entity {

	protected NFSTopologyController topocontroller = null;
	
	protected HashMap<String, String> oneToOneTrafficMap = null;//src ip -> dst ip
	
	private DiscreteDistPoisson mrarrivaldist = null;
	private DiscreteDistPoisson paarrivaldist = null;
	Random rand = null;
	
	public NFSTrafficGenerator(Model model, String entityName, boolean showInReport,
			NFSTopologyController tpctrl) {
		super(model, entityName, showInReport);
		topocontroller = tpctrl;
		init();
	}
	
	protected void init() {
		oneToOneTrafficMap = new HashMap<String, String>();
		mrarrivaldist = new DiscreteDistPoisson(NetworkFlowSimulator.mainModel, 
				"mr arrival dist", 
				NetworkFlowSimulator.parser.getDouble("fluidsim.system.runlength", 100) / 2, 
				true, true);
		paarrivaldist = new DiscreteDistPoisson(NetworkFlowSimulator.mainModel, 
				"pa arrival dist", 
				NetworkFlowSimulator.parser.getDouble("fluidsim.system.runlength", 100) / 2, 
				true, true);
		rand = new Random(System.currentTimeMillis());
		buildflowmap();
	}
	
	private void buildflowmap() {
		Random rand = new Random(System.currentTimeMillis());
		int hostN = topocontroller.getHostN();
		int dstHostIdx = 0;
		for (NFSHost host : topocontroller.allHosts()) {
			dstHostIdx = rand.nextInt(hostN);
			String dstip = topocontroller.getHostIP(dstHostIdx);
			while (oneToOneTrafficMap.containsValue(dstip) || host.ipaddress.equals(dstip)) {
				dstHostIdx = rand.nextInt(hostN);
				dstip = topocontroller.getHostIP(dstHostIdx);
			}
			oneToOneTrafficMap.put(host.ipaddress, dstip);
		}
	}
	
	public String getPermuMatrixTarget(String srcip) {
		return oneToOneTrafficMap.get(srcip);
	}
	
	private void runPermuMatrix() {
		for (NFSHost host : topocontroller.allHosts()) host.run(0);
	}

	//mapreduce
	
	public String[] getRandomTargets(int targetsnum) {
		String [] dstips = new String[targetsnum];
		int hostN = topocontroller.getHostN();
		for (int i = 0; i < targetsnum; i++) {
			dstips[i] = topocontroller.getHostIP(rand.nextInt(hostN));
		}
		return dstips;
	}
	
	public void runMapReduce(int jobnum) {
		int totalmachines = topocontroller.getHostN();
		for (int i = 0; i < jobnum; i++) {
			NFSHost host = topocontroller.getHost(rand.nextInt(totalmachines));
			NFSStartNewFlowEvent startevent = new NFSStartNewFlowEvent(
					NetworkFlowSimulator.mainModel, "startMapRJobOn"
							+ host.ipaddress, true);
			startevent.schedule(
					host,
					new NFSIntegerEntity(getModel(), "map-intentity", false, 1),
					TimeOperations.add(presentTime(),
							new TimeSpan(mrarrivaldist.sample())));
		}
	}
	
	//partition aggregater
	public void runPartitionAggregate(int jobnum) {
		int totalmachines = topocontroller.getHostN();
		for (int i = 0; i < jobnum; i++) {
			NFSHost host = topocontroller.getHost(rand.nextInt(totalmachines));
			NFSStartNewFlowEvent startevent = new NFSStartNewFlowEvent(
					NetworkFlowSimulator.mainModel, "startMapRJobOn"
							+ host.ipaddress, true);
			startevent.schedule(
					host,
					new NFSIntegerEntity(getModel(), "mapintentity", false, 2),
					TimeOperations.add(presentTime(),
							new TimeSpan(paarrivaldist.sample())));
		}
	}
	
	public void run() {
		boolean permumatrixflag = NetworkFlowSimulator.parser.getBoolean(
				"fluidsim.application.onoff.controlflag", 
				false);
		int mrjobnum = NetworkFlowSimulator.parser.getInt(
				"fluidsim.application.mapreduce.jobnum", 10);
		int pajobnum = NetworkFlowSimulator.parser.getInt(
				"fluidsim.application.pa.jobnum", 10);
		if (permumatrixflag) runPermuMatrix();
		runMapReduce(mrjobnum);
		runPartitionAggregate(pajobnum);
	}
}
