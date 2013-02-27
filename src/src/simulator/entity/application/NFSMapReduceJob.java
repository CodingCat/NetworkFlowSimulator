package simulator.entity.application;

import java.util.Random;

import simulator.NetworkFlowSimulator;
import simulator.model.NFSModel;
import simulator.utils.NFSDoubleCalculator;
import simulator.utils.NFSRandomArrayGenerator;
import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;

public class NFSMapReduceJob extends Entity {
	
	private double inputSize = 0.0;
	private double shuffleSize = 0.0;
	private int mapnum = 0;
	private int reducenum = 0;
	private int priority = 1;
	private NFSMapTask [] mappers = null;
	private NFSReduceTask [] reducers = null;
	private int finishedmapnum = 0;
	private int finishedreducenum = 0;
	
	TimeInstant startTime = null;
	TimeInstant finishTime = null;
	
	public NFSMapReduceJob(Model model, String entityName, boolean showInTrace, double insize) {
		super(model, entityName, showInTrace);
		inputSize = insize;
		initialize();
	} 

	protected void initialize() {
		shuffleSize = inputSize * NetworkFlowSimulator.parser.getDouble(
				"fluidsim.application.mapreduce.expansionfactor", 1.0);
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
		int totalmachineNum = NFSModel.trafficcontroller.topocontroller.getHostN();
		Random rand = new Random(System.currentTimeMillis());
		//start map tasks
		for (int i = 0; i < mapnum; i++) { 
			mappers[i] = new NFSMapTask(getModel(), 
					getName() + "-m-" + i, 
					true, 
					i, //task id
					NFSDoubleCalculator.div(shuffleSize, (double) mapnum), // data to be transferred to be through the networks from this mapper 
					NFSModel.trafficcontroller.topocontroller.getHost(rand.nextInt(totalmachineNum)),//the tasktracker
					this);
		}
		//start reduce tasks
		for (int i = 0; i < reducenum; i++) {
			reducers[i] = new NFSReduceTask(getModel(),
					getName() + "-r-" + i,
					true, 
					NFSModel.trafficcontroller.topocontroller.getHost(rand.nextInt(totalmachineNum)),
					this);
		}
	}
	
	public int reduceNum() {
		return reducenum;
	}
	
	public int mapNum() {
		return mapnum;
	}
	
	public NFSReduceTask getReducer(int i) {
		return reducers[i];
	}
	
	public int getPriority() {
		return priority;
	}
	
	private void checkJobFinish() {
		if (finishedmapnum == mapnum && finishedreducenum == reducenum) {
			//TODO: report to the controller 
		}
	}
	
	public void finishMap() {
		finishedmapnum++;
		checkJobFinish();
	}
	
	public void finishReduce() {
		finishedreducenum++;
		checkJobFinish();
	}
	
	public void run() {
		distribute();
		Random rand = new Random(System.currentTimeMillis());
		for (int i = 0; i < mappers.length; i++) mappers[i].run(rand);
		System.out.println("start a new job, map num:" + mappers.length + "  reduce num:" + 
				reducenum);
		
	}
}
