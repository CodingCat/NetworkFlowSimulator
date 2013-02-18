package simulator.entity.flow;

import java.util.ArrayList;

import simulator.entity.topology.NFSLink;
import desmoj.core.report.Reporter;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.Reportable;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSFlow extends Entity {
	
	class NFSFlowInform extends Reportable {

		private String flowIDInReport;
		private double lastingtimeInReport;
		private double sendsizeInReport;
		private double throughputInReport;
		
		
		public NFSFlowInform(Model model, String name, boolean showInReport, boolean showInTrace) {
			super(model, name, showInReport, showInTrace);
			flowIDInReport = name;
		}
		
		public Reporter createReporter() {
			return new NFSFlowReporter(this);
		}
		
		public double getlastingtime() {
			return this.lastingtimeInReport;
		}
		
		public void setlastingtime(double lt) {
			this.lastingtimeInReport = lt;
		}
		
		public double getsendsize() {
			return this.sendsizeInReport;
		}
		
		public void setsendsize(double ss) {
			this.sendsizeInReport = ss;
		}
		
		public double getthroughput() {
			return this.throughputInReport;
		}
		
		public void setthroughput(double tp) {
			this.throughputInReport = tp;
		}
		
		@Override
		public String toString() {
			return "flowID:" + flowIDInReport + 
					" lastingTime:" + lastingtimeInReport + 
					" sendsize:" + sendsizeInReport + 
					" throughput:" + throughputInReport;
		}
	}
	
	class NFSFlowReporter extends Reporter {

		public NFSFlowReporter(Reportable infoSource) {
			super(infoSource);
			numColumns = 5;
			columns = new String[numColumns];
			columns[0] = "FlowID";
			columns[1] = "LastingTime";
			columns[2] = "SendSize";
			columns[3] = "Throughput";
			groupHeading = "Flows";
			groupID = 861029;
			entries = new String[numColumns];
		}

		@Override
		public String[] getEntries() {
			if (source instanceof NFSFlowInform) {
				entries[0] = ((NFSFlowInform) source).getName();
				entries[1] = Double.toString(((NFSFlowInform) source).getlastingtime());
				entries[2] = Double.toString(((NFSFlowInform) source).getsendsize());
				entries[3] = Double.toString(((NFSFlowInform) source).getthroughput());
			}
			return entries;
		}
	}
	
	public double demandrate = -1.0;//in MBps
	public double inputSize = 0;//in MBps
	public double datarate = -1.0; //in MBps
	public double expectedrate = -1.0;// in MBps 
	
	public String dstipString = null;
	public String srcipString = null;
	
	protected TimeSpan lastingTime;
	protected TimeInstant lastCheckingPoint;
	double sendoutSize = 0.0;
	double throughput = 0.0;
	double outsize = 0;
	
	NFSFlowStatus status;
	NFSFlowInform flowinform = null;
	NFSFlowReporter flowreporter = null;
	
	private ArrayList<NFSLink> path = null;
	private NFSLink bottlenecklink = null;
	
	public enum NFSFlowStatus {
		NEWSTARTED,
		RUNNING,
		CLOSED
	}
	
	public NFSFlow(Model model, String entityname, boolean showinreport, 
			double demand) {
		super(model, entityname, showinreport);
		demandrate = demand;
		expectedrate = demandrate;
		lastingTime = new TimeSpan(0);
		lastCheckingPoint = new TimeInstant(0);
		path = new ArrayList<NFSLink>();
		flowinform = new NFSFlowInform(model, entityname, true, true);
		flowreporter = new NFSFlowReporter(flowinform);
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
	
	public void close() {
		expectedrate = 0;
		path.clear();
		update();
	}
	
	/**
	 * start the new flow, be called after we have determined the datarate
	 * of the flow
	 */
	public void start() {
		//lastlink, we have determine the datarate of the new flow
		sendTraceNote(getName() + " is starting");
		datarate = expectedrate;
		sendTraceNote("determine " + getName() + " datarate as " + datarate);
		lastCheckingPoint = presentTime();
		consumeBandwidth();
		setStatus(NFSFlowStatus.RUNNING);
	}
	
	/**
	 * update the data members of the flow
	 */
	private void update() {
		lastingTime = TimeOperations.add(lastingTime, TimeOperations.diff(presentTime(), lastCheckingPoint));
		sendoutSize += (TimeOperations.diff(presentTime(), lastCheckingPoint).getTimeAsDouble() * datarate);
		lastCheckingPoint = presentTime();
		throughput = sendoutSize / lastingTime.getTimeAsDouble();
		flowinform.setlastingtime(lastingTime.getTimeAsDouble());
		flowinform.setsendsize(sendoutSize);
		flowinform.setthroughput(throughput);
	}
	
	/**
	 * update the rate of the flow
	 * @param model, '+' or '-'
	 * @param newdata, value to be added or reduced with
	 */
	public void update(char model, double newdata) {
		update();
		String tracerecord = "change flow " + getName() + " rate from " + datarate;
		if (model == '-') datarate -= newdata;
		if (model == '+') datarate += newdata;
		sendTraceNote(tracerecord + " to " + datarate + " in update(char model, double newdata)");
	}
	
	public int getLinkIdx(NFSLink link) {
		return path.indexOf(link);
	}
	
	public boolean isFullyMeet() {
		double t = (status.equals(NFSFlowStatus.NEWSTARTED)) ? expectedrate : datarate; 
		return demandrate == t;
	}
	
	public void setBottleneckLink(NFSLink link) {
		bottlenecklink = link;
	}
	
	public NFSLink getBottleneckLink() {
		return bottlenecklink;
	}
	
	public NFSLink getFirstLink() {
		return path.get(0);
	}
	
	public NFSLink getNextLink(NFSLink currentLink) {
		int nextlinkidx = path.indexOf(currentLink) + 1;
		if (nextlinkidx < path.size()) return path.get(nextlinkidx);
		return null;
	}
	
	/**
	 * consume the bandwidth on the links along with the path
	 * only be called when the flow is new started
	 */
	private void consumeBandwidth() {
		try {
			if (!status.equals(NFSFlowStatus.NEWSTARTED)) {
				throw new Exception("flow must be NEWSTARTED, but " + status.toString() + " detected");
			}
			sendTraceNote("path length:" + path.size());
			for (NFSLink link : path) {
				if (link.getAvailableBandwidth() >= datarate) {
					link.setAvailableBandwidth('-', datarate);
				} else {
					// adjust datarate of other flows
					link.adjustFlowRates(this);
					link.setAvailableBandwidth('-', link.getAvailableBandwidth());
				}
				String flowratesStr = "Flow Rates on Link " + link.getName();
				for (NFSFlow flow : link.getRunningFlows()) {
					flowratesStr += (flow.getName() + ":" + flow.datarate + " ");
				}
				sendTraceNote(flowratesStr);
			}
			sendTraceNote("existing from consumeBandwidth()");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
