package simulator.entity.flow;

import java.util.ArrayList;

import simulator.entity.topology.NFSLink;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSFlow extends Entity {
	
	public double demandrate = -1.0;//in MBps
	public double datarate = -1.0; //in MBps
	public double expectedrate = -1.0;// in MBps 
	
	public String dstipString = null;
	public String srcipString = null;
	
	private TimeSpan lastingTime;
	private TimeInstant lastStartPoint;
	double sendoutSize = 0.0;
	double throughput = 0.0;
	
	
	private ArrayList<NFSLink> path = null;
	private int bottleneckIdx = -1;
	
	public enum NFSFlowStatus {
		NEWSTARTED,
		RUNNING,
		CLOSED
	}
	
	NFSFlowStatus status;
	
	public NFSFlow(Model model, String entityname, boolean showinreport, 
			double demand) {
		super(model, entityname, showinreport);
		demandrate = demand;
		expectedrate = demandrate;
		lastingTime = new TimeSpan(0);
		lastStartPoint = new TimeInstant(0);
		path = new ArrayList<NFSLink>();
	}
	
	public void setStatus(NFSFlowStatus s) {
		status = s;
	}
	
	public NFSFlowStatus getStatus() {
		return status;
	}
	
	
	public void addPath(NFSLink link) {
		try {
			if (dstipString == null) throw new Exception("invalid destination ip address");
			this.path.add(link);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
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
	
	public void rateChange() {
		sendoutSize += (TimeOperations.diff(presentTime(), lastStartPoint).getTimeAsDouble() * datarate);
	}
	
	public int getLinkIdx(NFSLink link) {
		return path.indexOf(link);
	}
	
	public boolean isFullyMeet() {
		double t = (expectedrate == 0) ? datarate : expectedrate; 
		return demandrate >= t;
	}
	
	public void setBottleneckIdx(int idx) {
		try {
			if (idx >= path.size()) throw new Exception("Invalid bottleneck idx");
			bottleneckIdx = idx;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public int getBottleneckIdx() {
		return this.bottleneckIdx;
	}
	
	public NFSLink getFirstLink() {
		return path.get(0);
	}
	
	public NFSLink getNextLink(NFSLink currentLink) {
		int nextlinkidx = path.indexOf(currentLink) + 1;
		if (nextlinkidx < path.size()) return path.get(nextlinkidx);
		return null;
	}
	
	public void consumeBandwidth() {
		for (NFSLink link : path) {
			if (link.getAvailableBandwidth() >= datarate) {
				link.setAvailableBandwidth('-', datarate);
			}
			else {
				//adjust datarate of other flows
				link.adjustFlowRates(this);
				link.setAvailableBandwidth('-', link.getAvailableBandwidth());
			}
		}
	}
}
