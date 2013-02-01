package simulator.entity.application;

import simulator.entity.NFSNode;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSFlow extends Entity {
	
	public double demand = 0;//in MB
	public double datarate = 0; //in MBps
	
	public String dstipString = "";
	public String srtipString = "";
	
	private TimeSpan lastingTime;
	private TimeInstant lastStartPoint;
	double throughput = 0.0;
	
	public double activeTimeUpbound = 0;
	public double idleTimeDownbound = 0;
	
	public NFSFlow(Model model, String entityname, boolean showinreport) {
		super(model, entityname, showinreport);
		lastingTime = new TimeSpan(0);
		lastStartPoint = new TimeInstant(0);
	}
	
	public double Start(NFSNode src, NFSNode dst) {
		src.AddNewFlow(this);
		dst.AddNewFlow(this);
		datarate = Math.min(src.getFlowAllocation(this), dst.getFlowAllocation(this));
		lastStartPoint = presentTime();
		return datarate;
	}
	
	public void Free(){
		TimeSpan recentLastSpan = TimeOperations.diff(presentTime(), lastStartPoint);
		double oldtotaldataamount = throughput * lastingTime.getTimeAsDouble();
		double recentdataamount = datarate * recentLastSpan.getTimeAsDouble();
		//increase the lastineTime
		lastingTime  = TimeOperations.add(lastingTime, recentLastSpan);
		//Calculate throughput
		throughput = ((oldtotaldataamount + recentdataamount) / lastingTime.getTimeAsDouble());  
	}
	
	public void SetDatarate(double rate) {
		this.datarate = rate;
	}
	
	public double GetDataRate() {
		return this.datarate;
	}
}
