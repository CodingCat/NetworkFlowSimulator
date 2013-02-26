package simulator.entity.flow;

import simulator.entity.topology.NFSLink;

public class NFSOpenFlowMessage {
	
	private double allocatedrate = 0.0;
	private NFSLink outlink = null;
	
	public NFSOpenFlowMessage(NFSLink link, double rate) {
		allocatedrate = rate;
		outlink = link;
	}
	
	public double getRate() {
		return allocatedrate;
	}
	
	public NFSLink getAllocatedLink() {
		return outlink;
	}
}
