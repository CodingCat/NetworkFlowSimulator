package simulator.entity.flow;

import simulator.entity.application.NFSMapReduceApplication;
import simulator.events.NFSCloseTaskBindedFlowEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSTaskBindedFlow extends NFSFlow {

	NFSMapReduceApplication.MapTask bindedtask = null;
	double demandSize = 0;//in MB
	TimeInstant startTime = null;
	TimeInstant finishTime = null;
	NFSCloseTaskBindedFlowEvent closeevent = null;
	
	
	public NFSTaskBindedFlow(Model model, String entityname,
			boolean showinreport, double demand, double outSize, NFSMapReduceApplication.MapTask task) {
		super(model, entityname, showinreport, demand);
		demandSize = outSize;
		bindedtask = task;
		startTime = presentTime();
	}
	
	public void close() {
		finishTime = presentTime();
	}
	
	public void schedule() {
		closeevent = new NFSCloseTaskBindedFlowEvent(getModel(), "closeEvent-" + this.getName(),
				true);
		closeevent.schedule(bindedtask, this, TimeOperations.add(presentTime(),
				new TimeSpan(demandSize / datarate)));
	}
	
	public void update(char model, double newdata) {
		super.update(model, newdata);
		//reschedule the closeevent
		if (demandSize <= sendoutSize) {
			closeevent.schedule(bindedtask, this, presentTime());
		} else {
			closeevent.cancel();
			TimeInstant newfinishTime = TimeOperations.add(presentTime(),
					new TimeSpan((demandSize - sendoutSize) / datarate));
			closeevent.schedule(bindedtask, this, newfinishTime);
		}
	}
	
	public double getResponseTime() { 
		return TimeOperations.diff(finishTime, startTime).getTimeAsDouble();
	}
	
}
