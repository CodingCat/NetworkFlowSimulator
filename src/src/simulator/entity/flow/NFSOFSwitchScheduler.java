package simulator.entity.flow;

import java.util.ArrayList;
import java.util.HashMap;
//import java.util.HashMap;

import simulator.entity.NFSNode;
import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.Model;

public class NFSOFSwitchScheduler extends NFSFlowScheduler {
	
	private HashMap<String, NFSLink> flowtable = null;//flow name -> out link
	private NFSOFController controller = null;
	
	
	public NFSOFSwitchScheduler(Model model, String entityName,
			boolean showInReport, ArrayList<NFSLink> links, NFSNode node) {
		super(model, entityName, showInReport, links, node);
		flowtable = new HashMap<String, NFSLink>();
		controller = NFSOFController._Instance(model);
		init();
	}
	
	private void init() {
		controller.registerSwitch(getNode(), this);
	}
	
	public void insert(String flowname, NFSLink link) {
		flowtable.put(flowname, link);
	}
	
	public void remove(String flowname) {
		flowtable.remove(flowname);
	}
	
	@Override
	public NFSLink schedule(NFSFlow flow) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reallocateBandwidth(NFSLink link, NFSFlow changedflow) {
		// TODO Auto-generated method stub

	}

}
