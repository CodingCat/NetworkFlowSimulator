package simulator.entity.topology;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSNode;
import simulator.entity.NFSRouter;
import simulator.entity.NFSRoutersContainer;

public class NFSNetworksBackbone extends Entity{
	
	NFSRouter [] coreswitches = null;
	NFSRoutersContainer distributionSwitches = null;
	NFSIpv4Installer ipinstaller = null;
	NFSSwitchBasedLAN lanbuilder = null;
	NFSNode dummynode = null; //this node is the target of all traffic out from the enterprise networks
	
	public NFSNetworksBackbone(Model model, 
			String entityname, 
			boolean debugmodel, 
			int n) {
		super(model, entityname, debugmodel);
		coreswitches = new NFSRouter[n];
		distributionSwitches = new NFSRoutersContainer(model, "all distribution switches", true);
		for (int i = 0; i < n; i++) {
			coreswitches[i] = new NFSRouter(getModel(), "core " + i, true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.topology.corebandwidth", 1000), 
					null, NFSRouter.RouterType.Core);
		}
		ipinstaller = new NFSIpv4Installer();
		lanbuilder = new NFSSwitchBasedLAN();
	}
	

	
	public void connect(NFSBuilding [] buildinglist){
		//put all distribution switches to array list
		for (int i = 0; i < buildinglist.length; i++) {
			for (int j = 0; j < buildinglist[i].l3switches.GetN(); j++) {
				distributionSwitches.Add(buildinglist[i].l3switches.Get(j));
			}
		}
		
		//TODO:assign ip address to core and distributions
		for (int i = 0; i < coreswitches.length; i++) {
			String base = "10." + (i + 1 + buildinglist.length) + ".1.1";
			System.out.println("connecting distributions to " + base);
			ipinstaller.assignIPAddress(base, coreswitches[i]);
			//build lan
			ipinstaller.assignIPAddress(base, 2, distributionSwitches, 0, distributionSwitches.GetN());
			//connect the distribution switches to the cores
			lanbuilder.buildLan(coreswitches[i], distributionSwitches, 0, distributionSwitches.GetN());
		}
		
		//set a dummy node representing the networks out of current enterprise networks
		String dummy_ip = "10." + (coreswitches.length + 1 + buildinglist.length) + "1.1";
		ipinstaller.assignIPAddress(dummy_ip, dummynode);
		NFSPointToPointInstaller p2pinstaller = new NFSPointToPointInstaller();
		for (int i = 0; i < coreswitches.length; i++) {
			p2pinstaller.Link(coreswitches[i], dummynode);
		}
	}
}
