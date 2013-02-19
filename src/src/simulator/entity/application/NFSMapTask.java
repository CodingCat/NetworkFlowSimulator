package simulator.entity.application;

import java.util.HashSet;
import java.util.Random;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.NFSRouter;
import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSTaskBindedFlow;
import simulator.entity.topology.NFSLink;
import simulator.events.NFSReceiveFlowEvent;
import simulator.utils.NFSRandomArrayGenerator;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;

public class NFSMapTask extends Entity {

	TimeInstant startTime = null;
	TimeInstant finishTime = null;
	NFSHost tasktracker = null;

	int taskID = 0;
	int receiverNum = 0;
	int closedflowN = 0;
	private double [] outputdist = null;
	private double resultsize = 0.0;
	NFSTaskBindedFlow [] flows = null;
	NFSMapReduceJob parentJob = null;
	private NFSReduceTask [] receivers = null;
	
	public NFSMapTask(Model model, String taskName, boolean showInTrace,
			int tid, double size, NFSHost tt, NFSMapReduceJob pJob) {
		super(model, taskName, showInTrace);
		taskID = tid;
		tasktracker = tt;
		parentJob = pJob;
		resultsize = size;
	}

	
	/**
	 * generate flows to send out the data
	 */
	public void run() {
		startTime = presentTime();
		Random rand = new Random(System.currentTimeMillis());
		int reducenum = parentJob.reduceNum();
		receiverNum = rand.nextInt(reducenum + 1);
		HashSet<String> selectedIPs = new HashSet<String>();
		flows = new NFSTaskBindedFlow[receiverNum];
		outputdist = new double[receiverNum];
		receivers = new NFSReduceTask[receiverNum];
		NFSRandomArrayGenerator.getDoubleArray(outputdist);
		NFSReduceTask recvcandidate = null;
		for (int i = 0; i < receivers.length; i++) {
			recvcandidate = parentJob.getReducer(rand.nextInt(reducenum));
			String ip = recvcandidate.getTaskTrackerIP();
			while (selectedIPs.contains(ip)) {
				recvcandidate = parentJob.getReducer(rand.nextInt(reducenum));
				ip = recvcandidate.getTaskTrackerIP();
			}
			receivers[i] = recvcandidate;
			selectedIPs.add(ip);
		}
		for (int i = 0; i < receiverNum; i++) {
			if (receivers[i].equals(tasktracker)) continue;//map and reducer are in the same host
			flows[i] = new NFSTaskBindedFlow(getModel(), 
					"flows-" + tasktracker.ipaddress + "-" + receivers[i].getTaskTrackerIP(),
					true,
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.mapreduce.rate", 10),
					outputdist[i] * resultsize,
					this, 
					receivers[i]);
			flows[i].srcipString = tasktracker.ipaddress;
			flows[i].dstipString = receivers[i].getTaskTrackerIP();
			flows[i].expectedrate = flows[i].demandrate;
			flows[i].setStatus(NFSFlow.NFSFlowStatus.NEWSTARTED);
			NFSLink passLink = tasktracker.startNewFlow(flows[i]);
			//schedule receive flow event
			NFSReceiveFlowEvent receiveflowevent = new NFSReceiveFlowEvent(
					getModel(), 
					"receiveflow-" + flows[i].srcipString + "-" + flows[i].dstipString, true);
			receiveflowevent.setSchedulingPriority(1);
			receiveflowevent.schedule(tasktracker, (NFSRouter) passLink.dst, flows[i], presentTime());
		}
	}
	
	public void close(NFSTaskBindedFlow flow) {
		flow.close();
		closedflowN++;
		if (closedflowN == flows.length) {
			finishTime = presentTime();
		}
	}
	
	public double getResponseTime() {
		return TimeOperations.diff(finishTime, startTime).getTimeAsDouble();
	}
}
