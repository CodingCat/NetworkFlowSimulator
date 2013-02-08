package simulator.entity.topology;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import simulator.entity.NFSHostsContainer;
import simulator.entity.NFSRoutersContainer;
import simulator.entity.NFSRouter.RouterType;

public class NFSBuilding extends Entity{
	
	private int buildingID = -1;
	private int l3num = 0;
	private int l2num = 0;
	private int hostsperl2 = 0;
	
	NFSHostsContainer hosts = null;
	NFSRoutersContainer l2switches = null;
	NFSRoutersContainer l3switches = null;
	
	public NFSBuilding(Model model, String entityname, boolean debugmodel, int bid, int l3sw, int l2sw, int hostperl2) {
		super(model, entityname, debugmodel);
		this.buildingID = bid;
		this.l3num = l3sw;
		this.l2num = l2sw;
		this.hostsperl2= hostperl2;
		hosts = new NFSHostsContainer(model, "hosts in " + entityname, debugmodel);
		l2switches = new NFSRoutersContainer(model, "l2 switches in " + entityname, debugmodel);
		l3switches = new NFSRoutersContainer(model, "l3 switches in " + entityname, debugmodel);
		hosts.create(hostsperl2 * l2num);
		l2switches.create(l2num);
		l2switches.SetRouterType(RouterType.Aggererate);
		l3switches.create(l3num);
		l3switches.SetRouterType(RouterType.Distribution);
		build();
	}
	
	private void build() {
		NFSSwitchBasedLAN  lanbuilder = new NFSSwitchBasedLAN();
		NFSIpv4Installer ipv4Installer = new NFSIpv4Installer();
		//Assign ip address to hosts and l2 switch
		for (int i = 0; i < l2num; i++) {
			String ip = "10." + buildingID + "." + (i + 1) + ".1";
			l2switches.Get(i).assignIPAddress(ip);
			ipv4Installer.assignIPAddress(ip, 2, hosts, i * hostsperl2, (i + 1) * hostsperl2);
		}
		
		//assign ip address to l3 switch and l2 switch
		for (int i = 0; i < l3num; i++) {
			String ip = "10." + buildingID + "." + (l2num + i + 1) + ".1";
			l3switches.Get(i).assignIPAddress(ip);
			ipv4Installer.assignIPAddress(ip, 2, l2switches, 0, l2switches.GetN());
		}
		//build link from access to distribution switch
		for (int i = 0; i < l3num; i++) {
			lanbuilder.buildLan(l3switches.Get(i), l2switches, 0, l2switches.GetN());
		}
		
		for (int i = 0; i < l2num; i++) {
			//connect the hosts to the l2 switch
			lanbuilder.buildLan(l2switches.Get(i), hosts, i * hostsperl2, (i + 1) * hostsperl2);
		}
		
	}
	
	public NFSHostsContainer getHosts() {
		return hosts;
	}
	
	/**
	 * for debug
	 */
	public void dumpTopology() {
		System.out.println("Building " + buildingID);
		System.out.println("=====================================================");
		System.out.println("Statistical Numbers");
		System.out.println("Hosts Number:" + hosts.GetN());
		System.out.println("L2 Switches Number:" + l2switches.GetN());
		System.out.println("L3 Switches Number:" + l3switches.GetN());
		System.out.println("=====================================================");
		System.out.println("L3 Switches");
		for (int i = 0; i < l3switches.GetN(); i++){
			System.out.println("L3 Switch:" + l3switches.Get(i));
			l3switches.Get(i).PrintLinks();
		}
		System.out.println("=====================================================");
		System.out.println("L2 Switches");
		for (int i = 0; i < l2switches.GetN(); i++){
			System.out.println("L2 Switch:" + l2switches.Get(i));
			l2switches.Get(i).PrintLinks();
		}
		System.out.println("=====================================================");
		System.out.println("Hosts");
		for (int i = 0; i < hosts.GetN(); i++){
			System.out.println("Hosts:" + hosts.Get(i));
			hosts.Get(i).PrintLinks();
		}
		System.out.println("=====================================================");
	}
}
