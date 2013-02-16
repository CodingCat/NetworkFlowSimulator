package simulator.entity.application;

import java.util.Random;

import desmoj.core.dist.DiscreteDistPoisson;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.topology.NFSTopologyController;
import simulator.events.NFSStartNewFlowEvent;

public class NFSMapReduceTraffic extends NFSTrafficGenerator {
	
	private DiscreteDistPoisson arrivaldist = null;
	
	public NFSMapReduceTraffic(Model model, String entityName, boolean showInReport,
			NFSTopologyController tpctrl) {
		super(model, entityName, showInReport, tpctrl);
		arrivaldist = new DiscreteDistPoisson(NetworkFlowSimulator.mainModel, 
				"job arrival dist", 
				NetworkFlowSimulator.parser.getDouble("fluidsim.system.runperiod", 100) / 2, 
				true, true);
	}

	@Override
	@Deprecated
	public String getOneToOneTarget(String srcip) {
		return null;
	}
	
	@Override
	public String[] getOneToManyTarget(int targetsnum) {
		Random rand = new Random(System.currentTimeMillis());
		String [] dstips = new String[targetsnum];
		int hostN = topocontroller.getHostN();
		for (int i = 0; i < targetsnum; i++) {
			dstips[i] = topocontroller.getHostIP(rand.nextInt(hostN));
		}
		return dstips;
	}

	@Override
	/**
	 * start mapreduce jobs
	 */
	public void run() {
		int jobnum = NetworkFlowSimulator.parser.getInt("fluidsim.application.mapreduce.jobnum", 
				100);
		Random rand = new Random(System.currentTimeMillis());
		for (int i = 0; i < jobnum; i++) {
			NFSHost host = topocontroller.getHost(rand.nextInt(jobnum));
			NFSStartNewFlowEvent startevent = new NFSStartNewFlowEvent(
					NetworkFlowSimulator.mainModel,
					"startMapRJobOn" + host.ipaddress,
					true);
			startevent.schedule(host, 
					TimeOperations.add(presentTime(), new TimeSpan(arrivaldist.sample())));
		}
	}
}
