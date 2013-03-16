package simulator.entity.application;

import simulator.NetworkFlowSimulator;
import desmoj.core.dist.ContDistNormal;
import desmoj.core.dist.DiscreteDistUniform;
import desmoj.core.dist.NumericalDist;

public class NFSLoadArrivalPattern {
	
	private NumericalDist<?> arrivaldist = null;
	private String loadpattern = null;
	
	public NFSLoadArrivalPattern(String pattern) {
		loadpattern = pattern;
		if (loadpattern.equals("normal")) {
			arrivaldist = new ContDistNormal(NetworkFlowSimulator.mainModel, 
					"mr arrival dist", 
					NetworkFlowSimulator.parser.getDouble("fluidsim.system.runlength", 100) / 2,
					NetworkFlowSimulator.parser.getDouble("fluidsim.application.mapreduce.arrival.stdev", 
							NetworkFlowSimulator.parser.getDouble("fluidsim.system.runlength", 100) / 2),
					true, true);
		}
		else {
			if (loadpattern.equals("uniform")) {
				arrivaldist = new DiscreteDistUniform(NetworkFlowSimulator.mainModel,
						"mr arrival dist",
						0,
						(int) NetworkFlowSimulator.parser.getDouble("fluidsim.system.runlength", 100) / 2,
						true, true);
			}
		}
	}
	
	public double sample() {
		if (loadpattern.equals("normal")) {
			return ((ContDistNormal) arrivaldist).sample();
		}
		else {
			if (loadpattern.equals("uniform")) {
				return ((DiscreteDistUniform) arrivaldist).sample();
			}
		}
		return -1.0;
	}

}
