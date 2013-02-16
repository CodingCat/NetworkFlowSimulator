package simulator.entity.application;

import java.util.Random;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.NFSRouter;
import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSTaskBindedFlow;
import simulator.entity.topology.NFSLink;
import simulator.events.NFSReceiveFlowEvent;
import simulator.model.NFSModel;
import desmoj.core.dist.ContDistNormal;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;

public class NFSMapReduceApplication extends NFSApplication {
	
	
	private static class RandomArrayGenerator{
		public static void getDoubleArray(double [] array) {
			Random rand = new Random(System.currentTimeMillis());
			double sum = 0.0;
			for (int i = 0; i < array.length; i++) {
				if (i == array.length - 1) array[i] = 1 - sum;
				else array[i] = rand.nextDouble() % (1 - sum);
				sum += array[i];
			}
		}
	}
	
	public class MapTask extends Entity{
		
		TimeInstant startTime = null;
		TimeInstant finishTime = null;
		
		public MapTask(Model model, String taskName, boolean showInTrace, int tid, int of, double outSize) {
			super(model, taskName, showInTrace);
			taskID = tid;
			outfactor = of;
			flows = new NFSTaskBindedFlow[outfactor];
			double [] partitions = new double [outfactor];
			RandomArrayGenerator.getDoubleArray(partitions);
			for (int i = 0; i < outfactor; i++) flows[i] = new NFSTaskBindedFlow(
					getModel(), 
					getName(), 
					true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.mapreduce.rate", 10),
					partitions[i],
					this);//
			startTime = presentTime();

		}

		int taskID = 0;
		int outfactor = 0;
		int closedflowN = 0;
		NFSTaskBindedFlow [] flows = null;
		
		/**
		 * send out the data
		 */
		public void run() {
			String [] targets = NFSModel.trafficcontroller.getOneToManyTarget(outfactor);
			for (int i = 0; i < flows.length; i++) {
				flows[i].srcipString = hostmachine.ipaddress;
				flows[i].dstipString = targets[i];
				flows[i].expectedrate = flows[i].demandrate;
				flows[i].setStatus(NFSFlow.NFSFlowStatus.NEWSTARTED);
				NFSLink passLink = hostmachine.startNewFlow(flows[i]);
				//schedule receive flow event
				NFSReceiveFlowEvent receiveflowevent = new NFSReceiveFlowEvent(
						getModel(), 
						"receiveflow-" + flows[i].srcipString + "-" + flows[i].dstipString, true);
				receiveflowevent.setSchedulingPriority(1);
				receiveflowevent.schedule((NFSRouter) passLink.dst, flows[i], presentTime());
			}
		}
		
		public void close(NFSTaskBindedFlow flow) {
			flow.close();
			closedflowN++;
			if (closedflowN == flows.length) {
				finishTime = presentTime();
				finish(this);
			}
		}
		
		public double getResponseTime() {
			return TimeOperations.diff(finishTime, startTime).getTimeAsDouble();
		}
	}
	
	double inputSize = 0.0;
	double shuffleSize = 0.0;
	int mapnum = 0;
	int reducenum = 0;
	int finishtasks = 0;
	MapTask [] mappers = null;
	static double expansionFactor = 0;
	static ContDistNormal inputdist = null;

	TimeInstant startTime = null;
	TimeInstant finishTime = null;
	
	public NFSMapReduceApplication(Model model, String entityName,
			boolean showInTrace, double dr, NFSHost machine) {
		super(model, entityName, showInTrace, dr, machine);
	}
	
	@Override
	protected void init() {
		if (inputdist == null) {
			double meanInputSize = NetworkFlowSimulator.parser.getDouble(
					"fluidsim.application.mapreduce.inputsize.mean", 
					100);
			double stdevInputSize = NetworkFlowSimulator.parser.getDouble(
					"fluidsim.application.mapreduce.inputsize.stdev", 
					100);
			expansionFactor = NetworkFlowSimulator.parser.getDouble(
					"fluidsim.application.mapreduce.inputsize.stdev", 
					1.2);
			inputdist = new ContDistNormal(getModel(),
					"mapreduce-input-norm-dist",
					meanInputSize,
					stdevInputSize,
					true,
					true);
		}
		inputSize = inputdist.sample();
		shuffleSize = inputSize * expansionFactor;
		mapnum = (int) Math.ceil(inputSize / 64);
		reducenum = (int) (mapnum * 0.9);
		startTime = presentTime();
		mappers = new MapTask[mapnum];
		partition();
	}
	
	private void partition() {
		//generate the partition of key space
		double [] partitions = new double[mapnum];
		RandomArrayGenerator.getDoubleArray(partitions);
		Random rand = new Random(System.currentTimeMillis());
		for (int i = 0; i < mapnum; i++) 
			mappers[i] = new MapTask(getModel(), getName() + "-task-" + i, 
					true, i, rand.nextInt(reducenum), partitions[i] * shuffleSize);
	}
	
	public void finish(MapTask task) {
		if (++finishtasks == mappers.length) {
			finishTime = presentTime();
		}
	}

	public double getResponseTime() {
		return TimeOperations.diff(finishTime, startTime).getTimeAsDouble();
	}
	
	@Override
	public void start() {
		for (int i = 0; i < mappers.length; i++) mappers[i].run();
	}

	@Override
	public void close() {
	
	}

}
