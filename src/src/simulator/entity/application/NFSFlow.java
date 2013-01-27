package simulator.entity.application;

import simulator.entity.NFSNode;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSFlow extends Entity {
	
	public double demand = 0;
	public double datarate = 0; //in kbps
	
	public String dst = "";
	public String srt = "";
	
	public NFSFlow(Model arg0, String arg1, boolean arg2) {
		super(arg0, arg1, arg2);
		// TODO Auto-generated constructor stub
	}
	
	public double Start(NFSNode src, NFSNode dst) {
		src.AddNewFlow(this);
		dst.AddNewFlow(this);
		datarate = Math.min(src.getFlowAllocation(this), dst.getFlowAllocation(this));
		return datarate;
	}
}
