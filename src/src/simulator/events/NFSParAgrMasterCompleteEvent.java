package simulator.events;

import simulator.entity.application.NFSParAgrMaster;
import desmoj.core.simulator.Event;
import desmoj.core.simulator.Model;

public class NFSParAgrMasterCompleteEvent extends Event<NFSParAgrMaster> {

	public NFSParAgrMasterCompleteEvent(Model model, String eventName, boolean showInTrace) {
		super(model, eventName, showInTrace);
	}

	@Override
	public void eventRoutine(NFSParAgrMaster finishedmaster) {
		finishedmaster.finish();
	}
}
