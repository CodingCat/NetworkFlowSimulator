package simulator.entity.flow;

import java.util.ArrayList;
import java.util.Collections;

import simulator.NetworkFlowSimulator;
import simulator.entity.topology.NFSLink;
import simulator.model.NFSModel;
import simulator.utils.NFSDoubleCalculator;
import desmoj.core.report.Reporter;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.Reportable;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSFlow extends Entity {
	
	class NFSFlowInfo extends Reportable {

		private double lastingtimeInReport;
		private double sendsizeInReport;
		private double throughputInReport;
		
		public NFSFlowInfo(Model model, String name, boolean showInReport, boolean showInTrace) {
			super(model, name, showInReport, showInTrace);
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
			return "flowID:" + getName() + 
					" lastingTime:" + lastingtimeInReport + 
					" sendsize:" + sendsizeInReport + 
					" throughput:" + throughputInReport;
		}
	}
	
	class NFSFlowReporter extends Reporter {

		public NFSFlowReporter(Reportable infoSource) {
			super(infoSource);
			numColumns = 4;
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
			if (source instanceof NFSFlowInfo) {
				entries[0] = ((NFSFlowInfo) source).getName();
				entries[1] = Double.toString(((NFSFlowInfo) source).getlastingtime());
				entries[2] = Double.toString(((NFSFlowInfo) source).getsendsize());
				entries[3] = Double.toString(((NFSFlowInfo) source).getthroughput());
			}
			return entries;
		}
	}
	
	public enum NFSFlowStatus {
		NEWSTARTED,
		RUNNING,
		CLOSED,
		ADJUSTING
	}
	
	protected enum NFSFlowType {
		QUERY,
		MESSAGE,
		BACKGROUND
	};
	
	
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
	NFSFlowInfo flowinform = null;
	NFSFlowReporter flowreporter = null;
	
	protected ArrayList<NFSLink> path = null;
	private NFSLink bottlenecklink = null;
	NFSFlowType flowtype = null;
	
	public NFSFlow(Model model, String entityname, boolean showinreport, 
			double demand, NFSFlowType type) {
		super(model, entityname, showinreport);
		demandrate = demand;
		expectedrate = demandrate;
		lastingTime = new TimeSpan(0);
		lastCheckingPoint = new TimeInstant(0);
		path = new ArrayList<NFSLink>();
		flowinform = new NFSFlowInfo(model, entityname, NFSModel.showNFSFlow, true);
		flowreporter = new NFSFlowReporter(flowinform);
		flowtype = type;
	}
	
	public double getExpectedRate() {
		return expectedrate;
	}
	
	public void setExpectedRate(double r) {
		expectedrate = r;
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
		//path.clear();
		update();
	}
	
	/**
	 * start the new flow, be called after we have determined the datarate
	 * of the flow
	 */
	public void start() {
		//lastlink, we have determine the datarate of the new flow
		sendTraceNote(getName() + " is starting");
		boolean openflowonoff = NetworkFlowSimulator.parser.getBoolean(
				"fluidsim.openflow.onoff", false);
		lastCheckingPoint = presentTime();
		if (!openflowonoff) consumeBandwidth();
		setStatus(NFSFlowStatus.RUNNING);
	}
		
	/**
	 * update the data members of the flow
	 */
	protected void update() {
		lastingTime = TimeOperations.add(lastingTime, TimeOperations.diff(presentTime(), lastCheckingPoint));
		sendoutSize = NFSDoubleCalculator.sum(sendoutSize, 
				TimeOperations.diff(presentTime(), lastCheckingPoint).getTimeAsDouble() * datarate);
		lastCheckingPoint = presentTime();
		throughput = lastingTime.getTimeAsDouble() != 0 ?
				NFSDoubleCalculator.div(sendoutSize, lastingTime.getTimeAsDouble()) : 0;
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
		if (model == '-') datarate = NFSDoubleCalculator.sub(datarate, newdata);
		if (model == '+') datarate = NFSDoubleCalculator.sum(datarate, newdata);
		sendTraceNote(tracerecord + " to " + datarate + " in update(char model, double newdata)");
	}
	
	public int getLinkIdx(NFSLink link) {
		return path.indexOf(link);
	}
	
	public final ArrayList<NFSLink> getPaths() {
		return path;
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
	
	@SuppressWarnings("unchecked")
	public boolean iswithsamepath(NFSFlow flow) {
		ArrayList<NFSLink> path1 = (ArrayList<NFSLink>) flow.getPaths().clone();
		ArrayList<NFSLink> path2 = (ArrayList<NFSLink>) path.clone();
		Collections.sort(path1, NFSLink.linkcomparator);
		Collections.sort(path2, NFSLink.linkcomparator);
		if (path1.size() != path2.size()) return false;
		for (int i = 0; i < path1.size(); i++) {
			if (!path1.get(i).equals(path2.get(i))) return false;
		}
		return true;
	}
	
	public NFSLink getFirstLink() {
		return path.get(0);
	}
	
	public void clearLinks() {
		path.clear();
	}
	
	public boolean isLatencySensitive() {
		return !(flowtype.equals(NFSFlowType.BACKGROUND));
	}
	
	public NFSLink getNextLink(NFSLink currentLink) {
		int nextlinkidx = path.indexOf(currentLink) + 1;
		if (nextlinkidx < path.size()) return path.get(nextlinkidx);
		return null;
	}
	
	public void adjust(double rate) {
		setStatus(NFSFlowStatus.ADJUSTING);
		expectedrate = rate;
		consumeBandwidth();
		setStatus(NFSFlowStatus.RUNNING);
	}
	
	/**
	 * consume the bandwidth on the links along with the path
	 * only be called when the flow is new started
	 */
	private void consumeBandwidth() {
		try {
			if (!status.equals(NFSFlowStatus.NEWSTARTED) && 
					!status.equals(NFSFlowStatus.ADJUSTING)) {
				throw new Exception("flow must be NEWSTARTED || ADJUSTING, but " + status.toString() + " detected");
			}
			for (NFSLink link : path) {
				sendTraceNote(getName() + " proposing rate:" + expectedrate);
				NFSFlowSchedulingAlgorithm.allocate(link, this);
				String flowratesStr = "Flow Rates on Link " + link.getName();
				for (NFSFlow flow : link.getRunningFlows()) {
					flowratesStr += (flow.getName() + ":" + flow.datarate + " ");
				}
				sendTraceNote(flowratesStr);
			}
			datarate = expectedrate;
			if (datarate < 0) System.out.println("NEGATIVE!!!!!");
			sendTraceNote("determine " + getName() + " datarate as " + datarate);
			sendTraceNote("existing from consumeBandwidth()");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
