package simulator;

import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeInstant;
import simulator.model.NFSModel;
import simulator.utils.NFSXmlParser;

public class NetworkFlowSimulator {
	
	static public NFSXmlParser parser = null;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		parser = NFSXmlParser.Instance(args[0]);
		
		NFSModel mainModel = new NFSModel(null, "NetworksFlowModel", true, true);
		Experiment exp = new Experiment("NetworksFlowExperiments");
		mainModel.connectToExperiment(exp);
		
		exp.tracePeriod(new TimeInstant(0), new TimeInstant(100));
		exp.debugPeriod(new TimeInstant(0), new TimeInstant(100));
		exp.stop(new TimeInstant(150)); 
		exp.start();
		exp.report();
		exp.finish();
		System.out.println("Done");
	}
}
