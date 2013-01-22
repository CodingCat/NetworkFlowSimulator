package simulator.entity;

import simulator.entity.application.NFSApplication;
import desmoj.core.simulator.Model;

public class NFSHost extends NFSNode{
	
	NFSApplication app = null;
	
	public NFSHost(Model model, String entityName, boolean showInLog, double bandWidth, String ip) {
		super(model, entityName, showInLog, bandWidth, ip);
	}
	
	public void send(){
		//TODO: send the flows
	}
}
