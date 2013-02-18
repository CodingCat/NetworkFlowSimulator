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
		/**
		 * generate a random array with double values
		 * @param array
		 */
		public static void getDoubleArray(double [] array) {
			Random rand = new Random(System.currentTimeMillis());
			for (int i = 0; i < array.length; i++) {
				double p = rand.nextDouble();
				while (p < 0.0001) p = rand.nextDouble();
				array[i] = p;
			}
		}
	}
	
	public class MapTask extends Entity{
		
		TimeInstant startTime = null;
		TimeInstant finishTime = null;
		NFSHost tasktracker = null;

		int taskID = 0;
		int outfactor = 0;
		int closedflowN = 0;
		NFSTaskBindedFlow [] flows = null;
		
		public MapTask(Model model, 
				String taskName, 
				boolean showInTrace, 
				int tid, 
				int of, 
				double outSize,
				NFSHost tt) {
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
			RandomArrayGenerator.getDoubleArray(partitions);
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
				finish(this);
			}
		}
		
		public double getResponseTime() {
			return TimeOperations.diff(finishTime, startTime).getTimeAsDouble();
		}
	}//end of maptask
	
	private double inputSize = 0.0;
	private double shuffleSize = 0.0;
	private int mapnum = 0;
	private int reducenum = 0;
	private int finishtasks = 0;
	private MapTask [] mappers = null;
	static ContDistNormal inputdist = null;

	TimeInstant startTime = null;
	TimeInstant finishTime = null;
	
	public NFSMapReduceApplication(Model model, String entityName,
			boolean showInTrace, double dr, NFSHost machine) {
		super(model, entityName, showInTrace, dr, machine);
		initialize();
	}
	
	protected void initialize() {
		if (inputdist == null) {
			double meanInputSize = NetworkFlowSimulator.parser.getDouble(
					"fluidsim.application.mapreduce.inputsize.mean", 
					256);
			double stdevInputSize = NetworkFlowSimulator.parser.getDouble(
					"fluidsim.application.mapreduce.inputsize.stdev", 
					10);
			inputdist = new ContDistNormal(getModel(),
					"mapreduce-input-norm-dist",
					meanInputSize,
					stdevInputSize,
					true,
					true);
		}
		inputSize = inputdist.sample();
		shuffleSize = inputSize;
		mapnum = (int) Math.ceil(inputSize / 64);
		reducenum = (int) Math.ceil (mapnum * 0.9);
		startTime = presentTime();
		mappers = new MapTask[mapnum];
	}
	
	/**
	 * deliver tasks to machines in datacenter
	 */
	private void distribute() {
		//generate the partition of key space
		double [] partitions = new double[mapnum];
		int totalmachineNum = NFSModel.trafficcontroller.topocontroller.getHostN();
		RandomArrayGenerator.getDoubleArray(partitions);
		Random rand = new Random(System.currentTimeMillis());
		for (int i = 0; i < mapnum; i++) { 
			mappers[i] = new MapTask(getModel(), 
					getName() + "-task-" + i, 
					true, 
					i, //task id
					reducenum, // how many reducer will accept this mapper's results 
					partitions[i] * shuffleSize, // data to be transferred to be through the networks from this mapper 
					NFSModel.trafficcontroller.topocontroller.getHost(rand.nextInt(totalmachineNum))//the tasktracker
					);
		}
		System.out.println("Map Number:" + mappers.length + " reduce number:" + reducenum);
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
		distribute();
		for (int i = 0; i < mappers.length; i++) mappers[i].run();
	}

	@Override
	public void close() {
	
	}
}
