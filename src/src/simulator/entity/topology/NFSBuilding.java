package simulator.entity.topology;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import simulator.entity.NFSNodeContainer;

public class NFSBuilding extends Entity{

	private int l3num = 0;
	private int l2num = 0;
	private int hostsperl2 = 0;
	
	NFSNodeContainer hosts = null;
	NFSNodeContainer l2switches = null;
	NFSNodeContainer l3switches = null;
	
	public NFSBuilding(Model model, String entityname, boolean debugmodel, int l3sw, int l2sw, int hostperl2) {
		super(model, entityname, debugmodel);
		this.l3num = l3sw;
		this.l2num = l2sw;
		this.hostsperl2= hostperl2;
		hosts = new NFSNodeContainer(model, "hosts in " + entityname, debugmodel);
		l2switches = new NFSNodeContainer(model, "l2 switches in " + entityname, debugmodel);
		l3switches = new NFSNodeContainer(model, "l3 switches in " + entityname, debugmodel);
		
	}
	
	
	private void build() {
		for (int i = 0; i < l2num; i++) {
			//connect the hosts to the l2 switch
			
		}
	}
}
