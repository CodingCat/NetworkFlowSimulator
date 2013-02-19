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
		String [] receiverIPs = new String[receiverNum];
		flows = new NFSTaskBindedFlow[receiverNum];
		outputdist = new double[receiverNum];
		NFSRandomArrayGenerator.getDoubleArray(outputdist);
		for (int i = 0; i < receiverIPs.length; i++) {
			String ip = parentJob.getReducerLocation(rand.nextInt(reducenum));
			while (selectedIPs.contains(ip)) {
				ip = parentJob.getReducerLocation(rand.nextInt(reducenum));
			}
			receiverIPs[i] = ip;
			selectedIPs.add(ip);
		}
		for (int i = 0; i < receiverNum; i++) {
			if (receiverIPs[i].equals(tasktracker.ipaddress)) continue;//map and reducer are in the same host
			flows[i] = new NFSTaskBindedFlow(getModel(), 
					"flows-" + tasktracker.ipaddress + "-" + receiverIPs[i],
					true,
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.mapreduce.rate", 10),
					outputdist[i] * resultsize,
					this);
			flows[i].srcipString = tasktracker.ipaddress;
			flows[i].dstipString = receiverIPs[i];
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
		//	finish(this);
		}
	}
	
	public double getResponseTime() {
		return TimeOperations.diff(finishTime, startTime).getTimeAsDouble();
	}
}
