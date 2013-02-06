package simulator.entity;

import simulator.NetworkFlowSimulator;
import simulator.entity.application.NFSApplication;
import simulator.entity.application.NFSOnOffApplication;
import desmoj.core.simulator.Model;

public class NFSHost extends NFSNode{
	
	NFSApplication app = null;
	
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
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.onoff.maxoffduration", 5), 
					this,
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.onoff.datarate", 0.5),
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.onoff.maxonduration", 5));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		app.send();
	}	
	
	public void close() {
		app.close();
	}
}
