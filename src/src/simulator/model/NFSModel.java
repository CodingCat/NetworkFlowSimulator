package simulator.model;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.topology.NFSBuilding;
import simulator.entity.topology.NFSNetworksBackbone;
import simulator.entity.topology.NFSTopologyController;
import desmoj.core.simulator.Model;

public class NFSModel extends Model{

	NFSTopologyController topocontroller = null;

	public NFSModel(Model model, String modelName, boolean showInReport, boolean showInTrace) {
		super(model, modelName, showInReport, showInTrace);
		topocontroller = new NFSTopologyController(
				getModel(),
				"topo-controller",
				true);
	}
	
	@Override
	public String description() {
		return "flow-based networks simulator";
	}

	@Override
	public void doInitialSchedules() {
		for (NFSHost host : topocontroller.allHosts()) host.run();
	}

	@Override
	public void init() {
		int buildingNum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.buildingnum", 2);
		int l3switchnum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.l3switchnum", 2);
		int l2switchnum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.l2switchnum", 4);
		int hostsperl2sw = NetworkFlowSimulator.parser.getInt("fluidsim.topology.hostsperl2sw", 100);
		int coreNum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.corenum", 2);
	
		NFSBuilding [] buildings = new NFSBuilding[buildingNum];
		NFSNetworksBackbone backbone = new NFSNetworksBackbone(getModel(), "networks back bone", true, coreNum);
		
		
		for (int i = 0 ; i < buildingNum; i++) {
			NFSBuilding building = new NFSBuilding(getModel(), 
					"building " + i, 
					true, i + 1, 
					l3switchnum, 
					l2switchnum, 
					hostsperl2sw);
			buildings[i] = building;
			topocontroller.registerHosts(buildings[i].getHosts());
		}
		backbone.connect(buildings);
	}
}
