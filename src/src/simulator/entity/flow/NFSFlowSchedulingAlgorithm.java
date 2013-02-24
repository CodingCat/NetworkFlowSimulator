package simulator.entity.flow;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import simulator.NetworkFlowSimulator;
import simulator.entity.flow.NFSFlow.NFSFlowStatus;
import simulator.entity.topology.NFSLink;
import simulator.utils.NFSDoubleCalculator;

public class NFSFlowSchedulingAlgorithm {

	void MaxMinAllocate(NFSLink link, NFSFlow changingflow) {
		if (changingflow.getStatus().equals(NFSFlowStatus.NEWSTARTED)) {
			@SuppressWarnings("unchecked")
			ArrayList<NFSFlow> demandingflows = (ArrayList<NFSFlow>) link
					.getRunningFlows().clone();
			double remainingBandwidth = link.getTotalBandwidth();
			double avrRate = link.getAvrRate();
			Collections.sort(demandingflows, NFSFlowScheduler.ratecomparator);
			while (demandingflows.size() != 0 && remainingBandwidth != 0) {
				double demand = 0.0;
				NFSFlow flow = demandingflows.get(0);
				demand = flow.status.equals(NFSFlowStatus.NEWSTARTED) ? flow.expectedrate : flow.datarate;
				if (demand < avrRate) {
					remainingBandwidth = NFSDoubleCalculator.sub(remainingBandwidth, demand);
				} else {
					flow.update('-', NFSDoubleCalculator.sub(flow.datarate, avrRate));
					remainingBandwidth = NFSDoubleCalculator.sub(remainingBandwidth, avrRate);
				}
				demandingflows.remove(0);
				if (demandingflows.size() != 0)
					avrRate = NFSDoubleCalculator.div(remainingBandwidth, demandingflows.size());
			}
		}//end of it's a new flow
		if (changingflow.getStatus() == (NFSFlowStatus.CLOSED)) {
			//this flow has been closed
			//triggered by the close flow event
			//rate of others might be improved
			System.out.println("Closing flow");
		}//end of it's a closing flow
	}
	
	public static void allocate(NFSLink link, NFSFlow changingflow) {
		try {
			NFSFlowSchedulingAlgorithm algoobj = new NFSFlowSchedulingAlgorithm();
			Class<?> algoclass = algoobj.getClass();
			String allocatemethodname = NetworkFlowSimulator.parser.getString(
					"fluidsim.flow.algorithm", "MaxMinAllocate");
			Method allocatealgo = algoclass.getDeclaredMethod(
					allocatemethodname, NFSLink.class, NFSFlow.class);
			allocatealgo.invoke(algoobj, link, changingflow);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
