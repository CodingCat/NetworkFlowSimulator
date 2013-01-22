package simulator.model;

import simulator.NetworkFlowSimulator;
import desmoj.core.simulator.Model;

public class NFSModel extends Model{
	
	
	public NFSModel(Model model, String modelName, boolean showInReport, boolean showInTrace) {
		super(model, modelName, showInReport, showInTrace);
	}

	@Override
	public String description() {
		return "flow-based networks simulator";
	}

	@Override
	public void doInitialSchedules() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init() {
		//TODO: build the topology
		int buildingNum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.buildingnum", 2);
		int l3switchnum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.l3switchnum", 2);
		int l2switchnum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.l2switchnum", 4);
		int hostsperl2sw = NetworkFlowSimulator.parser.getInt("fluidsim.topology.hostsperl2sw", 100);
		
		
	}
}
