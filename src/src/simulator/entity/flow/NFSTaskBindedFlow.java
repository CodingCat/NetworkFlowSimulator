package simulator.entity.flow;

import simulator.entity.application.NFSMapTask;
import simulator.events.NFSCloseTaskBindedFlowEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSTaskBindedFlow extends NFSFlow {
	
	private NFSMapTask bindedtask = null;
	private double demandSize = 0;//in MB
	private TimeInstant startTime = null;
	private TimeInstant finishTime = null;
	private NFSCloseTaskBindedFlowEvent closeevent = null;
	
	
	public NFSTaskBindedFlow(Model model, String entityname,
			boolean showinreport, double demand, double outSize, NFSMapTask task) {
		super(model, entityname, showinreport, demand);
		demandSize = outSize;
		bindedtask = task;
		startTime = presentTime();
	}
	
	public void close() {
		finishTime = presentTime();
		sendTraceNote(getName()  + " closed");
	}
	
	/**
	 * get the name of the sender of this flow
	 * @return the name
	 */
	public String getSenderName() {
		return bindedtask.getName();
	}
	
	/**
	 * start the flow
	 */
	@Override
	public void start() {
		super.start();
		closeevent = new NFSCloseTaskBindedFlowEvent(getModel(), "closeEvent-" + this.getName(),
				true);
		sendTraceNote("start a new flow, with demand:" + demandSize + " MB and rate:" + datarate);
		closeevent.schedule(bindedtask, this, TimeOperations.add(presentTime(),
				new TimeSpan(demandSize / datarate)));
	}
	
	@Override
	public void update(char model, double newdata) {
		super.update(model, newdata);
		//reschedule the closeevent
		try {
			if (demandSize <= sendoutSize) {
				closeevent.schedule(bindedtask, this, presentTime());
			} else {
				closeevent.cancel();
				TimeInstant newfinishTime = TimeOperations.add(presentTime(),
						new TimeSpan((demandSize - sendoutSize) / datarate));
				closeevent.schedule(bindedtask, this, newfinishTime);
			}
		}
		catch (Exception e) {
			System.out.println(datarate + ":" + demandSize + ":" + sendoutSize);
			e.printStackTrace();
		}
	}
	
	public double getResponseTime() { 
		return TimeOperations.diff(finishTime, startTime).getTimeAsDouble();
	}
	
}
