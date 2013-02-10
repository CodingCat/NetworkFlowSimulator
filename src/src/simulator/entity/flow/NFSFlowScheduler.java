package simulator.entity.flow;

import java.util.ArrayList;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

import simulator.entity.topology.NFSLink;

public abstract class NFSFlowScheduler extends Entity{
	
	protected ArrayList<NFSLink> outlinks = null;
	
	public NFSFlowScheduler(Model model, String entityName, boolean showInReport, 
			ArrayList<NFSLink> links) {
		super(model, entityName, showInReport);
		outlinks = links;
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
