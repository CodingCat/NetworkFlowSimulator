package simulator.entity;

import simulator.NetworkFlowSimulator;
import simulator.entity.application.NFSApplication;
import simulator.entity.application.NFSMapReduceApplication;
import simulator.entity.application.NFSOnOffApplication;
import simulator.entity.application.NFSParAgrApplication;
import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.Model;

public class NFSHost extends NFSNode{
	
	private NFSApplication [] apps = null;
	
	public NFSHost(Model model, String entityName, boolean showInLog, double bandWidth, String ip, int multihominglevel) {
		super(model, entityName, showInLog, bandWidth, ip);
		installApp();
	}
	
	private void installApp() {
		apps = new NFSApplication[3];
		// onoff application
		apps[0] = new NFSOnOffApplication(getModel(), "OnOffAppOn" + getName(),
				true, NetworkFlowSimulator.parser.getDouble(
						"fluidsim.application.onoff.rate", 0.5), this);
		// mapreduce application
		apps[1] = new NFSMapReduceApplication(getModel(),
				"MRAppOn" + getName(), true,
				NetworkFlowSimulator.parser.getDouble(
						"fluidsim.application.mapreduce.rate", 10), this);
		// partition aggregator application
		apps[2] = new NFSParAgrApplication(getModel(), "PAAppOn" + getName(),
				true, NetworkFlowSimulator.parser.getDouble(
						"fluidsim.application.pa.rate", 0.05), this);
	}
	
	/**
	 * start a new flow from this host
	 * @param flow, the new flow
	 * @return the link selected to pass the flow data
	 */
	public NFSLink startNewFlow(NFSFlow flow) {
		//select a path with ECMP link, with ECMP style
		//supporting multihoming
		NFSLink link = flowscheduler.schedule(flow);
		//add the current link to the flow path
		flow.addPath(link);
		return link;
	}
	
	public void run(int appIdx) {
		apps[appIdx].start();
	}	
	
	public void close(int appIdx) {
		apps[appIdx].close();
	}
}
