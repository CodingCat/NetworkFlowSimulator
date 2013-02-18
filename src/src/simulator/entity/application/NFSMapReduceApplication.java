package simulator.entity.application;

import java.util.ArrayList;

import simulator.NetworkFlowSimulator;
import simulator.entity.NFSHost;

import desmoj.core.dist.ContDistNormal;
import desmoj.core.simulator.Model;

public class NFSMapReduceApplication extends NFSApplication {
	
	private static ContDistNormal inputdist = null;
	
	private ArrayList<NFSMapReduceJob> jobs = null;
	
	public NFSMapReduceApplication(Model model, String entityName,
			boolean showInReport, double dr, NFSHost machine) {
		super(model, entityName, showInReport, dr, machine);
		if (inputdist == null) {
			double inputdistmean = NetworkFlowSimulator.parser.getDouble("fluidsim.application.mapreduce.inputsize.mean", 1024);
			double inputdiststdev = NetworkFlowSimulator.parser.getDouble("fluidsim.application.mapreduce.inputsize.stdev", 0);
			inputdist = new ContDistNormal(model, "mapreduce-input-dist", inputdistmean, inputdiststdev, true, true);
		}
		jobs = new ArrayList<NFSMapReduceJob>();
	}

	@Override
	public void start() {
		NFSMapReduceJob newjob = new NFSMapReduceJob(getModel(), 
				"Job" + jobs.size() + "On-" + this.hostmachine.ipaddress,
				true,
				inputdist.sample());
		jobs.add(newjob);
		newjob.run();
	}

	@Override
	public void close() {
		//leave it empty
	}
}
