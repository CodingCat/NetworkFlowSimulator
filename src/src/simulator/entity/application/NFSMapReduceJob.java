package simulator.entity.application;

import java.util.Random;

import simulator.model.NFSModel;
import simulator.utils.NFSRandomArrayGenerator;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;

public class NFSMapReduceJob extends Entity {
	
	private double inputSize = 0.0;
	private double shuffleSize = 0.0;
	private int mapnum = 0;
	private int reducenum = 0;
	private int finishtasks = 0;
	private NFSMapTask [] mappers = null;
	private NFSReduceTask [] reducers = null;
	
	TimeInstant startTime = null;
	TimeInstant finishTime = null;
	
	public NFSMapReduceJob(Model model, String entityName, boolean showInTrace, double insize) {
		super(model, entityName, showInTrace);
		inputSize = insize;
		initialize();
	} 

	protected void initialize() {
		shuffleSize = inputSize;
		mapnum = (int) Math.ceil(inputSize / 64);
		reducenum = (int) Math.ceil (mapnum * 0.9);
		startTime = presentTime();
		mappers = new NFSMapTask[mapnum];
		reducers = new NFSReduceTask[reducenum];
	}
	
	/**
	 * deliver tasks to machines in datacenter
	 */
	private void distribute() {
		//generate the partition of key space
		double [] partitions = new double[mapnum];
		int totalmachineNum = NFSModel.trafficcontroller.topocontroller.getHostN();
		NFSRandomArrayGenerator.getDoubleArray(partitions);
		Random rand = new Random(System.currentTimeMillis());
		for (int i = 0; i < mapnum; i++) { 
			mappers[i] = new NFSMapTask(getModel(), 
					getName() + "-m-" + i, 
					true, 
					i, //task id
					partitions[i] * shuffleSize, // data to be transferred to be through the networks from this mapper 
					NFSModel.trafficcontroller.topocontroller.getHost(rand.nextInt(totalmachineNum)),//the tasktracker
					this);
		}
		
		for (int i = 0; i < reducenum; i++) {
			reducers[i] = new NFSReduceTask(getModel(),
					getName() + "-r-" + i,
					true, 
					NFSModel.trafficcontroller.topocontroller.getHost(rand.nextInt(totalmachineNum)));
		}
	}
	
	public int reduceNum() {
		return reducenum;
	}
	
	public int mapNum() {
		return mapnum;
	}
	
	public String getReducerLocation(int i) {
		return reducers[i].getTaskTrackerLocation();
	}
	
	public void run() {
		distribute();
		for (int i = 0; i < mappers.length; i++) mappers[i].run();
	}
	
	
	
	public void finish(NFSMapTask task) {
		if (++finishtasks == mappers.length) {
			finishTime = presentTime();
		}
	}

	public double getResponseTime() {
		return TimeOperations.diff(finishTime, startTime).getTimeAsDouble();
	}

}
