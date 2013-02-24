package simulator.entity.flow;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import simulator.NetworkFlowSimulator;
import simulator.entity.flow.NFSFlow.NFSFlowStatus;
import simulator.entity.topology.NFSLink;
import simulator.utils.NFSDoubleCalculator;

public class NFSFlowSchedulingAlgorithm {

	void AppAwareAllocate(NFSLink link, NFSFlow changingflow) { 
		if (changingflow.getStatus().equals(NFSFlowStatus.NEWSTARTED)) {
			if (link.getAvailableBandwidth() >= changingflow.expectedrate) {
				link.setAvailableBandwidth('-', changingflow.expectedrate);
			}
			else {
				ArrayList<NFSFlow> flowsToBeReduced = new ArrayList<NFSFlow>();
				NFSFlow[] runningflows = link.getRunningFlows();
				double sumRates = 0.0;
				for (NFSFlow flow : runningflows) {
					if (flow == changingflow || flow.isLatencySensitive()) continue;
					flowsToBeReduced.add(flow);
					sumRates += flow.datarate;
				}
				if (sumRates != 0.0 && sumRates >= changingflow.expectedrate) {
					if (flowsToBeReduced.size() != 0) {
						double totalCost = changingflow.expectedrate - link.getAvailableBandwidth();
						Collections.sort(flowsToBeReduced, Collections
								.reverseOrder(NFSFlowFairScheduler.ratecomparator));
						for (NFSFlow flow : flowsToBeReduced) {
							flow.update('-', totalCost * (flow.datarate / sumRates));
							flow.setBottleneckLink(link);
						}
					}
				}
				else {
				
				}
			}
			//finally determine the datarate of the new flow
			changingflow.datarate = changingflow.expectedrate;
		}//end of it's a new flow
		
	}
	
	void MaxMinAllocate(NFSLink link, NFSFlow changingflow) {
		if (changingflow.getStatus().equals(NFSFlowStatus.NEWSTARTED)) {
			if (link.getAvailableBandwidth() >= changingflow.expectedrate) {
				link.setAvailableBandwidth('-', changingflow.expectedrate);
			}
			else {
				ArrayList<NFSFlow> flowsToBeReduced = new ArrayList<NFSFlow>();
				NFSFlow[] runningflows = link.getRunningFlows();
				double sumRates = 0.0;
				for (NFSFlow flow : runningflows) {
					if (flow == changingflow) continue;
					flowsToBeReduced.add(flow);
					sumRates = NFSDoubleCalculator.sum(flow.datarate, sumRates);
				}
				if (changingflow.expectedrate > sumRates) System.out.println("No enough space");
				if (flowsToBeReduced.size() != 0) {
					double totalCost = NFSDoubleCalculator.sub(changingflow.expectedrate, 
							link.getAvailableBandwidth());
					if (totalCost < 0) System.out.println("totalCost < 0");
					Collections.sort(flowsToBeReduced, 
							Collections.reverseOrder(NFSFlowFairScheduler.ratecomparator));
					for (NFSFlow flow : flowsToBeReduced) {
						flow.update('-', totalCost * (flow.datarate / sumRates));
						flow.setBottleneckLink(link);
					}
					link.setAvailableBandwidth(0.0);
				}
			}
			//finally determine the datarate of the new flow
		//	changingflow.datarate = changingflow.expectedrate;
		}//end of it's a new flow
		if (changingflow.getStatus().equals(NFSFlowStatus.CLOSED)) {
			//this flow has been closed
			//triggered by the close flow event
			//rate of others might be improved
			if (link != null) {
				link.removeRunningFlow(changingflow);
				
				NFSFlow [] involvedFlows = link.getRunningFlows();
				ArrayList<NFSFlow> flowsCanBeImproved = new ArrayList<NFSFlow>();
				NFSFlow [] improvedFlowsArray = null;
				double improvedRate = changingflow.datarate;
				link.setAvailableBandwidth('+', changingflow.datarate);
				for (int i = 0; i < involvedFlows.length; i++) {
					if (involvedFlows[i].getBottleneckLink() == null) {
						System.out.println("haven't set bottle link," + involvedFlows[i].getStatus());
					}
					if (involvedFlows[i].getBottleneckLink().equals(link) && !involvedFlows[i].isFullyMeet()) 
						flowsCanBeImproved.add(involvedFlows[i]);
				}
				improvedFlowsArray = new NFSFlow[flowsCanBeImproved.size()];
				Collections.sort(flowsCanBeImproved, NFSFlowScheduler.demandcomparator);
				flowsCanBeImproved.toArray(improvedFlowsArray);
				if (improvedFlowsArray.length != 0) {
					improvedRate = NFSDoubleCalculator.div(link.getAvailableBandwidth(), (double)improvedFlowsArray.length);
					if (improvedRate < 0) {
						System.out.println("negative improvedRate: " + improvedRate);
					}
					for (int i = 0; i < improvedFlowsArray.length; i++) {
						if (NFSDoubleCalculator.sum(improvedFlowsArray[i].datarate, improvedRate) 
								<= improvedFlowsArray[i].demandrate) {
							improvedFlowsArray[i].update('+', improvedRate);
							//link.setAvailableBandwidth('-', improvedRate);
						} else {
							improvedFlowsArray[i].update('+', 
									improvedFlowsArray[i].demandrate - improvedFlowsArray[i].datarate);
							if (improvedFlowsArray.length - 1 - i != 0)
								improvedRate = NFSDoubleCalculator.div(link.getAvailableBandwidth(),
										(double) improvedFlowsArray.length - 1 - i);
						}
					}
				}
				else {
					if (link.getAvailableBandwidth() < 0) {
						System.out.println("FUCK " + link.getAvailableBandwidth());
					}
				}
			}
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
