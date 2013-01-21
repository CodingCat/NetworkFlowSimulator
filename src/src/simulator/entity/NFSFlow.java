package simulator.entity;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSFlow extends Entity {
	
	double demand = 0;
	double datarate = 0; //in kbps
	
	String dst = "";
	String srt = "";
	
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
