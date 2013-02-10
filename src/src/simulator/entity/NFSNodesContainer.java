package simulator.entity;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public abstract class NFSNodesContainer extends Entity {
	
	protected String mainID = null;
	
	public NFSNodesContainer(Model arg0, String arg1, boolean arg2, String mainid) {
		super(arg0, arg1, arg2);
		mainID = mainid;
	}
	
	abstract public void create(int n);
	abstract public int GetN();
	abstract public NFSNode Get(int i);
}
