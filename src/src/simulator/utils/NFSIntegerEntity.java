package simulator.utils;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSIntegerEntity extends Entity {
	
	private int value = 0;
	
	public NFSIntegerEntity(Model model, String entityName, boolean showInTrace, int v) {
		super(model, entityName, showInTrace);
		value = v;
	}
	
	public int getValue() {
		return value;
	}
}
