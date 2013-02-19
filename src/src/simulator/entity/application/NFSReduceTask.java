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
	public void addReceiver(String maptaskName) {
		if (!senders.contains(maptaskName)) senders.contains(maptaskName);
	}
	
	/**
	 * finish a flow
	 * @param finishedflow, just finished flow
	 */
	public void finishflow(NFSTaskBindedFlow finishedflow) {
		try {
			String senderName = finishedflow.getSenderName();
			if (!senders.contains(senderName)) throw new Exception("wrong partition");
			finishedmappers.add(senderName);
			if (finishedmappers.size() == senders.size()) finish(finishedflow);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * called in finishflow, 
	 * @param finishflow, new finished flow
	 */
	private void finish(NFSTaskBindedFlow finishflow) {
		endtime = presentTime();
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
