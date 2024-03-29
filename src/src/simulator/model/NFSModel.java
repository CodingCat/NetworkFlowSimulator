package simulator.model;

import simulator.NetworkFlowSimulator;
import simulator.entity.application.NFSTrafficGenerator;
import simulator.entity.topology.NFSBuilding;
import simulator.entity.topology.NFSNetworksBackbone;
import simulator.entity.topology.NFSTopologyController;
import desmoj.core.simulator.Model;

public class NFSModel extends Model{
	
	//control flags
	public static boolean showNFSFlow = false;
	public static boolean showTaskBindedFlow = false;
	public static boolean showMapTask = true;
	public static boolean showReduceTask = true;
	public static boolean showPATask = true;
	
	public NFSTopologyController topocontroller = null;
	public static NFSTrafficGenerator trafficcontroller = null;
	
	public NFSModel(Model model, String modelName, boolean showInReport, boolean showInTrace) {
		super(model, modelName, showInReport, showInTrace);
	}

	@Override
	public String description() {
		return "flow-based networks simulator";
	}

	@Override
	public void doInitialSchedules() {
		trafficcontroller.run();
	}

	@Override
	public void init() {
		int buildingNum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.buildingnum", 2);
		int l3switchnum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.l3switchnum", 2);
		int l2switchnum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.l2switchnum", 4);
		int hostsperl2sw = NetworkFlowSimulator.parser.getInt("fluidsim.topology.hostsperl2sw", 100);
		int coreNum = NetworkFlowSimulator.parser.getInt("fluidsim.topology.corenum", 2);
		
		topocontroller = new NFSTopologyController(this, "topo-controller", true);
		
		NFSBuilding [] buildings = new NFSBuilding[buildingNum];
		NFSNetworksBackbone backbone = new NFSNetworksBackbone(getModel(), "networks backbone", true, coreNum);
		
		for (int i = 0 ; i < buildingNum; i++) {
			NFSBuilding building = new NFSBuilding(getModel(), 
					"building " + i, 
					true, 
					i + 1, 
					l3switchnum, 
					l2switchnum, 
					hostsperl2sw);
			buildings[i] = building;
			topocontroller.registerHosts(buildings[i].getHosts());
		}
		backbone.connect(buildings);
	
		trafficcontroller = new NFSTrafficGenerator(getModel(), "trafficcontroller", true, topocontroller);
	}
}
