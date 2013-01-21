package simulator.model;

import desmoj.core.simulator.Model;

public class NFSModel extends Model{

	public NFSModel(Model model, String modelName, boolean showInReport, boolean showInTrace) {
		super(model, modelName, showInReport, showInTrace);
	}

	@Override
	public String description() {
		return "flow-based networks simulator";
	}

	@Override
	public void doInitialSchedules() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}
}
