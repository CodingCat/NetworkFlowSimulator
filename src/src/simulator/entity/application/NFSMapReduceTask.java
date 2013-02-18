package simulator.entity.application;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.NFSRouter;
import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSTaskBindedFlow;
import simulator.entity.topology.NFSLink;
import simulator.events.NFSReceiveFlowEvent;
import simulator.model.NFSModel;
import simulator.utils.NFSRandomArrayGenerator;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;

public class NFSMapReduceTask extends Entity {

	public NFSMapReduceTask(Model arg0, String arg1, boolean arg2) {
		super(arg0, arg1, arg2);
	}
	
	
	TimeInstant startTime = null;
	TimeInstant finishTime = null;
	NFSHost tasktracker = null;

	int taskID = 0;
	int outfactor = 0;
	int closedflowN = 0;
	NFSTaskBindedFlow [] flows = null;
	
	public NFSMapReduceTask(Model model, String taskName, boolean showInTrace, int tid, 
			int of, double outSize,NFSHost tt) {
		super(model, taskName, showInTrace);
		taskID = tid;
		outfactor = of;
		flows = new NFSTaskBindedFlow[outfactor];
		startTime = presentTime();
		tasktracker = tt;
	}

	
	/**
	 * generate flows to send out the data
	 */
	public void run() {
		double [] partitions = new double [outfactor];
		NFSRandomArrayGenerator.getDoubleArray(partitions);
		String [] targets = NFSModel.trafficcontroller.getOneToManyTarget(outfactor);
		for (int i = 0; i < flows.length; i++) {
			if (targets[i].equals(tasktracker.ipaddress)) continue;//map and reducer are in the same host
			flows[i] = new NFSTaskBindedFlow(getModel(), 
					"flows-" + tasktracker.ipaddress + "-" + targets[i],
					true,
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.mapreduce.rate", 10),
					partitions[i],
					this);
			flows[i].srcipString = tasktracker.ipaddress;
			flows[i].dstipString = targets[i];
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
