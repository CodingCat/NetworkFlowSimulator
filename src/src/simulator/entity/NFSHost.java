package simulator.entity;

import simulator.NetworkFlowSimulator;
import simulator.entity.application.NFSApplication;
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
			Class<?> appClass = Class.forName(
					NetworkFlowSimulator.parser.getString("fluidsim.host.applications", 
							"simulator.entity.application.NFSOnOffApplication"));
			Class<?> [] parameterTypes = {Model.class, String.class, boolean.class, 
					double.class, NFSHost.class};
			java.lang.reflect.Constructor<?> constructor = 
					appClass.getConstructor(parameterTypes);
			Object [] parameterList = {getModel(), "appOn" + this.getName(), true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.rate", 0.5)};
			app = (NFSApplication) constructor.newInstance(parameterList); 
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
		NFSLink link = flowscheduler.schedule(flow);
		//add the flow to the link
		link.addRunningFlow(flow);
		//add the current link to the flow path
		flow.addPath(link);
		return link;
	}
	
	public void run() {
		app.start();
	}	
	
	public void close() {
		app.close();
	}
}
