package simulator.entity.application;

import java.util.ArrayList;

import simulator.entity.NFSHost;
import simulator.entity.flow.NFSTaskBindedFlow;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;

public class NFSReduceTask extends Entity {

	private ArrayList<String> senders = null;
	private ArrayList<String> finishedmappers = null;
	private TimeInstant starttime = null;
	private TimeInstant endtime = null;
	private NFSHost tasktracker = null;
	
	public NFSReduceTask(Model model, String taskName, boolean showInReport, NFSHost host) {
		super(model, taskName, showInReport);
		senders = new ArrayList<String>();
		finishedmappers = new ArrayList<String>();
		starttime = presentTime();
		tasktracker = host;
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
			if (finishedmappers.size() == senders.size()) endtime = presentTime();
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
