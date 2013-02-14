package simulator.entity.application;

import java.util.Random;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;
import simulator.entity.flow.NFSFlow;
import desmoj.core.simulator.Model;

public class NFSMapReduceJobs extends NFSApplication {
	
	String JobID;
	double inputSize;
	double shuffleSize;
	int mapnum = 0;
	int reducenum = 0;
	MapTask [] mappers = null;
	
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
	
	private class MapTask {
		int taskID = 0;
		int outfactor = 0;
		NFSFlow [] flows = null;
		
		public MapTask(int tid, int of, double outSize) {
			taskID = tid;
			outfactor = of;
			flows = new NFSFlow[outfactor];
			double [] partitions = new double [outfactor];
			RandomArrayGenerator.getDoubleArray(partitions);
			for (int i = 0; i < outfactor; i++) flows[i] = new NFSFlow(
					getModel(), 
					JobID + "-m-" + taskID + "-flow", 
					true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.onoff.rate", 10),
					partitions[i]);//
		}
		
		/**
		 * send out the data
		 */
		public void run() {
			for (int i = 0; i < flows.length; i++) {
				
			}
		}
	}
	
	public NFSMapReduceJobs(Model model, String entityName,
			boolean showInTrace, double dr, NFSHost machine, String jid, double iSize, double sSize) {
		super(model, entityName, showInTrace, dr, machine);
		JobID = jid;
		inputSize = iSize;
		shuffleSize = sSize;
		mapnum = (int) Math.ceil(inputSize / 64);
		reducenum = (int) (mapnum * 0.9);
		mappers = new MapTask[mapnum];
		init();
	}
	
	private void init() {
		//generate the partition of key space
		double [] partitions = new double[mapnum];
		RandomArrayGenerator.getDoubleArray(partitions);
		for (int i = 0; i < mapnum; i++) mappers[i] = new MapTask(i, reducenum, partitions[i] * shuffleSize);
	}

	@Override
	public void send() {
		for (int i = 0; i < mappers.length; i++) mappers[i].run();
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
