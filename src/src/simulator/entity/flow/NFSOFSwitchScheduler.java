package simulator.entity.flow;

import java.util.ArrayList;
//import java.util.HashMap;

import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.Model;

public class NFSOFSwitchScheduler extends NFSFlowScheduler {
	
	//private HashMap<NFSFlow.NFSFlowType, Double> staticAppTable = null;
	//private NFSOFController controller = null;
	
	public NFSOFSwitchScheduler(Model model, String entityName,
			boolean showInReport, ArrayList<NFSLink> links) {
		super(model, entityName, showInReport, links);
	
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
