package simulator.entity.application;

import java.util.HashSet;
import java.util.Random;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSFlowSchedulingAlgorithm;
import simulator.entity.flow.NFSTaskBindedFlow;
import simulator.events.NFSOpenFlowSubscribeEvent;
import simulator.model.NFSModel;
import simulator.utils.NFSDoubleCalculator;
import simulator.utils.NFSRandomArrayGenerator;
import desmoj.core.report.Reporter;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.Reportable;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;

public class NFSMapTask extends Entity {
	
	class NFSMapTaskInfo extends Reportable {
		
		private double responseTime = 0;
		private long sendingFlowNum = 0;
		private double shuffleSize = 0;
		
		public NFSMapTaskInfo(Model model, String name, boolean showInReport,
				boolean showInTrace) {
			super(model, name, showInReport, showInTrace);
		}

		public NFSMapTaskReporter createReporter() {
			return new NFSMapTaskReporter(this);
		}
	}
	
	class NFSMapTaskReporter extends Reporter {

		public NFSMapTaskReporter(Reportable infosource) {
			super(infosource);
			numColumns = 4;
			columns = new String[numColumns];
			columns[0] = "TaskID";
			columns[1] = "ResponseTime";
			columns[2] = "SendingFlowsNum";
			columns[3] = "ShuffleSize";
			groupHeading = "MapTasks";
			groupID = 871029;
			entries = new String[numColumns];
		}
		

		@Override
		public String[] getEntries() {
			if (source instanceof NFSMapTaskInfo) {
				entries[0] = ((NFSMapTaskInfo) source).getName();
				entries[1] = Double.toString(((NFSMapTaskInfo) source).responseTime);
				entries[2] = Double.toString(((NFSMapTaskInfo) source).sendingFlowNum);
				entries[3] = Double.toString(((NFSMapTaskInfo) source).shuffleSize);
			}
			return entries;
		}
	}
	
	protected boolean openflowonoff = false;
	
	TimeInstant startTime = null;
	TimeInstant finishTime = null;
	NFSHost tasktracker = null;

	int taskID = 0;
	int receiverNum = 0;
	int closedflowN = 0;
	private double [] outputdist = null;
	private double shufflesize = 0.0;
	NFSTaskBindedFlow [] flows = null;
	NFSMapReduceJob parentJob = null;
	private NFSReduceTask [] receivers = null;
	
	private NFSMapTaskInfo taskinfo = null;
	NFSMapTaskReporter taskreporter = null;
	
	public NFSMapTask(Model model, String taskName, boolean showInTrace,
			int tid, double size, NFSHost tt, NFSMapReduceJob pJob) {
		super(model, taskName, showInTrace);
		taskID = tid;
		tasktracker = tt;
		parentJob = pJob;
		shufflesize = size;
		taskinfo = new NFSMapTaskInfo(model, taskName, NFSModel.showMapTask, true);
		taskreporter = new NFSMapTaskReporter(taskinfo);
		taskinfo.shuffleSize = shufflesize;
		openflowonoff = NetworkFlowSimulator.parser.getBoolean(
				"fluidsim.openflow.onoff", false);
	}
	
	
	public NFSMapReduceJob getJob() {
		return parentJob;
	}
	
	
	/**
	 * generate flows to send out the data
	 */
	public void run(Random rand) {
		startTime = presentTime();
		int reducenum = parentJob.reduceNum();
		receiverNum = rand.nextInt(reducenum + 1);
		taskinfo.sendingFlowNum = receiverNum;
		HashSet<String> selectedReceivers = new HashSet<String>();
		flows = new NFSTaskBindedFlow[receiverNum];
		outputdist = new double[receiverNum];
		receivers = new NFSReduceTask[receiverNum];
		NFSRandomArrayGenerator.getDoubleArray(outputdist);
		NFSReduceTask recvcandidate = null;
		for (int i = 0; i < receivers.length; i++) {
			recvcandidate = parentJob.getReducer(rand.nextInt(reducenum));
			String name = recvcandidate.getName();
			while (selectedReceivers.contains(name)) {
				recvcandidate = parentJob.getReducer(rand.nextInt(reducenum));
				name = recvcandidate.getName();
			}
			receivers[i] = recvcandidate;
			receivers[i].addSender(getName());
			selectedReceivers.add(name);
		}
		for (int i = 0; i < receiverNum; i++) {
			if (receivers[i].getTaskTrackerIP().equals(tasktracker.ipaddress)) {
				System.out.println(getName() + " starts local tasks");
				continue;//map and reducer are in the same host
			}
			flows[i] = new NFSTaskBindedFlow(getModel(), 
					"flows-" + tasktracker.ipaddress + "-" + receivers[i].getTaskTrackerIP() + "-" + getName(),
					true,
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.mapreduce.rate", 10),
					NFSDoubleCalculator.mul(outputdist[i], shufflesize),
					this, 
					receivers[i]);
			flows[i].srcipString = tasktracker.ipaddress;
			flows[i].dstipString = receivers[i].getTaskTrackerIP();
			flows[i].expectedrate = flows[i].demandrate;
			flows[i].setStatus(NFSFlow.NFSFlowStatus.NEWSTARTED);
			if (openflowonoff == false) {
				//1. select path
				NFSFlowSchedulingAlgorithm.ecmpPathSelection(tasktracker.getOutlink(), flows[i]);
				//2. determine rate
				NFSFlowSchedulingAlgorithm.rateAllocation(tasktracker, tasktracker.getOutlink(), flows[i]);
			}
			else {
				NFSOpenFlowSubscribeEvent subevent = 
						new NFSOpenFlowSubscribeEvent(getModel(), 
								tasktracker.getName() + flows[i].getName() + "subEvent", 
								true);
				subevent.schedule(tasktracker, flows[i], presentTime());
			}
		}
	}
	
	
	
	public NFSHost getTaskTracker() {
		return tasktracker;
	}
	
	public void finishflow() {
		//System.out.println(getName() + " finishes a flow");
		closedflowN++;
		if (closedflowN == flows.length) {
			finishTime = presentTime();
			taskinfo.responseTime = TimeOperations.diff(finishTime, startTime).getTimeAsDouble();
			parentJob.finishMap();
		}
	}
}
