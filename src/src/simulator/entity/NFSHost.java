package simulator.entity;

import simulator.NetworkFlowSimulator;
import simulator.entity.application.NFSApplication;
import simulator.entity.application.NFSOnOffApplication;
import simulator.entity.flow.NFSFlow;
import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.Model;

public class NFSHost extends NFSNode{
	
	private NFSApplication app = null;
	
	public NFSHost(Model model, String entityName, boolean showInLog, double bandWidth, String ip, int multihominglevel) {
		super(model, entityName, showInLog, bandWidth, ip);
		installApp();
	}
	
	private void installApp() {
		try {
			//TODO: change to factory pattern to generate different types of applications
			//build applications via reflection
			this.app = new NFSOnOffApplication(
					getModel(),
					"OnOffApp-" + this.toString(),
					true,
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.onoff.datarate", 0.5),
					this,
					NetworkFlowSimulator.parser.getInt("fluidsim.application.onoff.maxonduration", 5),
					NetworkFlowSimulator.parser.getInt("fluidsim.application.onoff.maxoffduration", 5));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * start a new flow from this host
	 * @param flow, the new flow
	 * @return the link selected to pass the flow data
	 */
	public NFSLink startNewFlow(NFSFlow flow) {
		//select a path with ECMP link, with ECMP style
		//supporting multihoming
		NFSLink link = chooseECMPLink(flow);
		//add the flow to the link
		link.addRunningFlow(flow);
		//add the current link to the flow path
		flow.addPath(link);
		return link;
	}
	
	public void run() {
		app.send();
	}	
	
	public void close() {
		app.close();
	}
}
