package simulator;

import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeInstant;
import simulator.model.NFSModel;
import simulator.utils.NFSXmlParser;

public class NetworkFlowSimulator {
	
	static public NFSXmlParser parser = null;
	
	
	/**
	 * @param args
	 * args[0] - the path of config.xml
	 */
	public static void main(String[] args) {
		parser = NFSXmlParser.Instance(args[0]);
		
		NFSModel mainModel = new NFSModel(null, "NetworksFlowModel", true, true);
		Experiment exp = new Experiment("NetworksFlowExperiments");
		
		int traceperiod = parser.getInt("fluidsim.system.traceperiod", 100);
		int debugperiod = parser.getInt("fluidsim.system.debugperiod", 100);
		int runperiod = parser.getInt("fluidsim.system.runlength", 100);
		
		mainModel.connectToExperiment(exp);
		
		exp.tracePeriod(new TimeInstant(0), new TimeInstant(traceperiod));
		exp.debugPeriod(new TimeInstant(0), new TimeInstant(debugperiod));
		exp.stop(new TimeInstant(runperiod)); 
		exp.start();
		exp.report();
		exp.finish();
		System.out.println("Done");
	}
}
