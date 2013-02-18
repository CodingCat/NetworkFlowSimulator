package simulator.entity.flow;

import java.util.ArrayList;
import java.util.Comparator;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

import simulator.entity.topology.NFSLink;

public abstract class NFSFlowScheduler extends Entity{
	
	public class NFSFlowDemandComparator implements Comparator<NFSFlow> {

		@Override
		public int compare(NFSFlow flow1, NFSFlow flow2) {
			double demandgap1 = flow1.demandrate - flow1.datarate;
			double demandgap2 = flow2.demandrate - flow2.datarate;
			return demandgap1 > demandgap2 ? 1 : (demandgap1 == demandgap2 ? 0 : -1);
		}

	}
	
	public class NFSFlowRateComparator implements Comparator<NFSFlow> {

		@Override
		public int compare(NFSFlow flow1, NFSFlow flow2) {
			double datarate1 = flow1.datarate;
			double datarate2 = flow2.datarate;
			return datarate1 > datarate2 ? 1 : (datarate1 == datarate2 ? 0 : -1);
		}
	}
	
	protected ArrayList<NFSLink> outlinks = null;
	public static NFSFlowDemandComparator demandcomparator = null;
	public static NFSFlowRateComparator ratecomparator = null;
	
	public NFSFlowScheduler(Model model, String entityName, boolean showInReport, 
			ArrayList<NFSLink> links) {
		super(model, entityName, showInReport);
		outlinks = links;
		demandcomparator = new NFSFlowDemandComparator();
		ratecomparator = new NFSFlowRateComparator();
	}
	
	/**
	 * determine which link the flow will be sent to
	 * @param flow, the flow
	 * @return, the selected flow
	 */
	public abstract NFSLink schedule(NFSFlow flow);
	
	/**
	 * allocate the bandwidth among the running flow on the link
	 * this method will only be executed by the event of a flow is closed or there is a new flow
	 * @param link, the link the changed flow is on
	 * @param changedflow, the changed flow, can be a new flow or flow just be closed 
	 */
	public abstract void reallocateBandwidth(NFSLink link, NFSFlow changedflow);
}
