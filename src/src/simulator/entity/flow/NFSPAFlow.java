package simulator.entity.flow;

import simulator.NetworkFlowSimulator;
import simulator.entity.application.NFSParAgrLeader;
import simulator.events.NFSClosePAFlowEvent;
import simulator.utils.NFSDoubleCalculator;
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
		super(model, entityname, showinreport, demand, NFSFlowType.QUERY);
		flowsource = source;
		init();
	}
	
	private void init() {
		demandSize = NetworkFlowSimulator.parser.getDouble(
				"fluidsim.application.pa.demandflowsize", 0.032);
		deadline = NFSDoubleCalculator.div(demandSize, this.demandrate);
		closeevent = new NFSClosePAFlowEvent(getModel(), "closeEvent-" + getName(),
				true);
		closeevent.setSchedulingPriority(1);
	}
	
	public double getDemandSize() {
		return demandSize;
	}
	
	@Override
	public void start() {
		super.start();
		startTime = presentTime();
		closeevent.schedule(this, TimeOperations.add(presentTime(), 
				new TimeSpan(deadline)));
	}
	
	@Override
	public void update(char model, double newdata) {
		super.update(model, newdata);
		//reschedule the closeevent
		try {
			if (demandSize <= sendoutSize) {
				if (!closeevent.isScheduled()) closeevent.schedule(this, presentTime());
			} else {
				if (closeevent.isScheduled()) {
					closeevent.cancel();
					TimeInstant newfinishTime = TimeOperations.add(presentTime(),
							new TimeSpan((demandSize - sendoutSize) / datarate));
					closeevent.schedule(this, newfinishTime);
				}
			}
		}
		catch (Exception e) {
			System.out.println(datarate + ":" + demandSize + ":" + sendoutSize);
			e.printStackTrace();
		}
	}
	
	public NFSParAgrLeader getSender() {
		return this.flowsource;
	}
	
	public void finish() {
		setStatus(NFSFlowStatus.CLOSED);
		finishTime = presentTime();
		flowsource.finish(this);
	}
	
	public boolean isInTime() {
		return (TimeOperations.diff(finishTime, startTime).getTimeAsDouble()) <= deadline;
	}
}
