package simulator.entity.application;

import java.util.ArrayList;

import simulator.entity.NFSHost;
import simulator.entity.flow.NFSTaskBindedFlow;
import simulator.model.NFSModel;

import desmoj.core.report.Reporter;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.Reportable;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;

public class NFSReduceTask extends Entity {
	
	class NFSReduceTaskInfo extends Reportable {
		private double responseTime = 0;
		private long receiveFlowNum = 0;
		private double shuffleSize = 0;
		
		public NFSReduceTaskInfo(Model model, String name, boolean showInReport,
				boolean showInTrace) {
			super(model, name, showInReport, showInTrace);
		}
		
		public Reporter createReporter() {
			return new NFSReduceTaskReporter(this);
		}
		
		public double getResponseTime() {
			return responseTime;
		}
		
		public void setResponseTime(double res) {
			responseTime = res;
		}
		
		public long getReceiveFlowNum() {
			return receiveFlowNum;
		}
		
		public void setReceiveFlowNum(long flownum) {
			receiveFlowNum = flownum;
		}
		
		public double getShuffleSize() {
			return shuffleSize;
		}
		
		public void setShuffleSize(double ssize) {
			shuffleSize = ssize;
		}
	}
	
	class NFSReduceTaskReporter extends Reporter {

		public NFSReduceTaskReporter(Reportable infosource) {
			super(infosource);
			numColumns = 4;
			columns = new String[numColumns];
			columns[0] = "TaskID";
			columns[1] = "ResponseTime";
			columns[2] = "ReceiveFlowsNum";
			columns[3] = "ShuffleSize";
			groupHeading = "ReduceTasks";
			groupID = 881029;
			entries = new String[numColumns];
		}

		@Override
		public String[] getEntries() {
			if (source instanceof NFSReduceTaskInfo) {
				entries[0] = ((NFSReduceTaskInfo) source).getName();
				entries[1] = Double.toString(((NFSReduceTaskInfo) source).getResponseTime());
				entries[2] = Double.toString(((NFSReduceTaskInfo) source).getReceiveFlowNum());
				entries[3] = Double.toString(((NFSReduceTaskInfo) source).getShuffleSize());
			}
			return entries;
		}
	}
	
	
	private ArrayList<String> senders = null;
	private ArrayList<String> finishedmappers = null;
	private TimeInstant starttime = null;
	private TimeInstant endtime = null;
	private NFSHost tasktracker = null;
	private double shufflesize = 0.0;
	
	private NFSReduceTaskInfo taskinfo = null;
	NFSReduceTaskReporter taskreporter = null;
	
	public NFSReduceTask(Model model, String taskName, boolean showInReport, NFSHost host) {
		super(model, taskName, showInReport);
		senders = new ArrayList<String>();
		finishedmappers = new ArrayList<String>();
		starttime = presentTime();
		tasktracker = host;
		taskinfo = new NFSReduceTaskInfo(model, taskName, NFSModel.showReduceTask, true);
		taskreporter = new NFSReduceTaskReporter(taskinfo);
	}
	
	/**
	 * register the map task's name as receiver
	 * @param maptaskName, the name of the map task
	 */
	public void addSender(String maptaskName) {
		if (!senders.contains(maptaskName)) senders.add(maptaskName);
	}
	
	/**
	 * get the tasktracker's ip
	 * @return , ipaddress
	 */
	public String getTaskTrackerIP() {
		return tasktracker.ipaddress;
	}
	
	/**
	 * finish a flow
	 * @param finishedflow, just finished flow
	 */
	public void finishflow(NFSTaskBindedFlow finishedflow) {
		try {
			String senderName = finishedflow.getSenderName();
			if (!senders.contains(senderName)) {
				throw new Exception("wrong partition, tasktracker ip:" + tasktracker.ipaddress + 
						" flow target:" + finishedflow.dstipString);
			}
			finishedmappers.add(senderName);
			shufflesize += finishedflow.getDemandSize();
			if (finishedmappers.size() == senders.size()) {
				endtime = presentTime();
				taskinfo.setReceiveFlowNum(senders.size());
				taskinfo.setResponseTime(TimeOperations.diff(endtime, starttime).getTimeAsDouble());
				taskinfo.setShuffleSize(shufflesize);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * get the response time of this reducer
	 * @return
	 */
	public double getResponseTime() {
		return TimeOperations.diff(endtime, starttime).getTimeAsDouble();
	}
	
	public String getTaskTrackerLocation() {
		return tasktracker.ipaddress;
	}
}
