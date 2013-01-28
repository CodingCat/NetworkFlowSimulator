package simulator.entity;

import java.util.ArrayList;
import java.util.HashMap;

import simulator.entity.application.NFSApplication;
import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.Model;

public class NFSHost extends NFSNode{
	
	NFSApplication app = null;
	
	private HashMap<String, ArrayList<NFSLink> > localroutetable = null;
	
	public NFSHost(Model model, String entityName, boolean showInLog, double bandWidth, String ip, int multihominglevel) {
		super(model, entityName, showInLog, bandWidth, ip);
		localroutetable = new HashMap<String, ArrayList<NFSLink> >();
	}
	
	@Override
	public void AddNewLink(NFSNode dst, double datarate) {
		super.AddNewLink(dst, datarate);
		//only support multihoming table
		if (localroutetable.containsKey("default") == false) {
			localroutetable.put("default", new ArrayList<NFSLink>());
		}
		localroutetable.get("default").add(outLinks.get(dst));
	}
	
	public void send(){
		//TODO: send the flows
		
	}
}
