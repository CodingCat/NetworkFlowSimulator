package simulator.entity.flow;

import simulator.NetworkFlowSimulator;
import simulator.entity.application.NFSParAgrLeader;
import simulator.events.NFSClosePAFlowEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSPAFlow extends NFSFlow {
	
	private double demandSize;
	private double deadline;
	
	private TimeInstant startTime = null;
	private TimeInstant finishTime = null;
	
	private NFSClosePAFlowEvent closeevent = null;
	private NFSParAgrLeader flowsource = null;
	
	public NFSPAFlow(Model model, String entityname, boolean showinreport,
			double demand, NFSParAgrLeader source) {
		super(model, entityname, showinreport, demand);
		flowsource = source;
		init();
	}
	
	private void init() {
		demandSize = NetworkFlowSimulator.parser.getDouble(
				"fluidsim.application.pa.flowsize", 0.5);
		deadline = NetworkFlowSimulator.parser.getDouble(
				"fluidsim.application.pa.deadline", 0.2);
	}
	
	public double getSize() {
		return demandSize;
	}
	
	@Override
	public void start() {
		super.start();
		startTime = presentTime();
		closeevent = new NFSClosePAFlowEvent(getModel(), "closeEvent-" + getName(),
				true);
		closeevent.schedule(this, TimeOperations.add(presentTime(), 
				new TimeSpan(deadline)));
	}
	
	@Override
	public void update(char model, double newdata) {
		super.update(model, newdata);
		//reschedule the closeevent
		try {
			if (demandSize <= sendoutSize) {
				closeevent.schedule(this, presentTime());
			} else {
				//System.out.println("rescheduling close event");
				closeevent.cancel();
				TimeInstant newfinishTime = TimeOperations.add(presentTime(),
						new TimeSpan((demandSize - sendoutSize) / datarate));
				closeevent.schedule(this, newfinishTime);
			}
		}
		catch (Exception e) {
			System.out.println(datarate + ":" + demandSize + ":" + sendoutSize);
			e.printStackTrace();
		}
	}
	
	public void finish() {
		finishTime = presentTime();
		flowsource.finish(this);
	}
	
	public boolean isInTime() {
		return (TimeOperations.diff(finishTime, startTime).getTimeAsDouble()) <= deadline;
	}
}
