package simulator.entity.flow;

import simulator.entity.application.NFSMapTask;
import simulator.entity.application.NFSReduceTask;
import simulator.events.NFSCloseTaskBindedFlowEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSTaskBindedFlow extends NFSFlow {
	
	private NFSMapTask mapper = null;
	private NFSReduceTask reducer = null;
	private double demandSize = 0;//in MB
	private TimeInstant startTime = null;
	private TimeInstant finishTime = null;
	private NFSCloseTaskBindedFlowEvent closeevent = null;
	
	
	public NFSTaskBindedFlow(Model model, String entityname, boolean showinreport, 
			double demand, double outSize, NFSMapTask mtask, NFSReduceTask rtask) {
		super(model, entityname, showinreport, demand);
		demandSize = outSize;
		mapper = mtask;
		reducer = rtask;
	}
	
	/**
	 * get the name of the sender of this flow
	 * @return the name
	 */
	public String getSenderName() {
		return mapper.getName();
	}
	
	public NFSReduceTask getReceiver() {
		return reducer;
	}
	
	/**
	 * start the flow
	 */
	@Override
	public void start() {
		super.start();
		startTime = presentTime();
		closeevent = new NFSCloseTaskBindedFlowEvent(getModel(), "closeEvent-" + this.getName(),
				true);
		sendTraceNote("start a new flow, with demand:" + demandSize + " MB and rate:" + datarate);
		closeevent.schedule(this, TimeOperations.add(presentTime(),
				new TimeSpan(demandSize / datarate)));
	}
	
	public void finish() {
		finishTime = presentTime();
		mapper.finishflow();
		reducer.finishflow(this);
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
	
	public double getResponseTime() { 
		return TimeOperations.diff(finishTime, startTime).getTimeAsDouble();
	}
	
	public double getDemandSize() {
		return demandSize;
	}
	
}
