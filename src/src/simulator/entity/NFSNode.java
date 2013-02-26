package simulator.entity;

import java.util.ArrayList;
import java.util.HashMap;

import simulator.NetworkFlowSimulator;
import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSFlowScheduler;
import simulator.entity.topology.NFSLink;

import desmoj.core.simulator.Entity;
import desmoj.core.simulator.Model;

public class NFSNode extends Entity{
	
	protected ArrayList<NFSLink> outLinks = null;//nexthop address -> link
	protected HashMap<NFSFlow, Double> flowAllocationTable = null;//flow->allocation
	protected NFSFlowScheduler flowscheduler = null;
	public String ipaddress = null;
	
	
	public NFSNode(Model model, String entityName, boolean showInLog, double bandWidth, String ip) {
		super(model, entityName, showInLog);
		outLinks = new ArrayList<NFSLink>();
		flowAllocationTable = new HashMap<NFSFlow, Double>();
		ipaddress = ip;
		buildFlowScheduler();
	}
	
	public double getFlowAllocation(NFSFlow flow) {
		return flowAllocationTable.get(flow);
	}
	
	/**
	 * add a new link between this node and the destination node
	 * @param dst, destination node
	 * @param rate, the bandwidth of the link
	 * @return new created link
	 */
	public NFSLink addNewLink(NFSNode dst, double rate) {
		NFSLink link = new NFSLink(getModel(), "link-" + this + "-" + dst, true, rate, this, dst);
		outLinks.add(link);
		return link;
	}
	
	public void assignIPAddress(String ip) {
		ipaddress = ip;
	}
	
	public boolean HasAllocatedIP() { 
		return ipaddress != null;
	}
	
	public ArrayList<NFSLink> getOutLinks() {
		return outLinks;
	}
	
	public void changeResourceAllocation(NFSLink link, NFSFlow changedflow) {
		flowscheduler.reallocateBandwidth(link, changedflow);
	}
	
	/**
	 * create flow scheduler with reflection
	 */
	private void buildFlowScheduler() {
		try {
			Class<?> flowSchedulerClass = Class.forName(
					NetworkFlowSimulator.parser.getString("fluidsim.flow.scheduler", 
							"simulator.entity.flow.NFSFlowFairScheduler"));
			Class<?> [] parameterTypes = {Model.class, String.class, boolean.class, 
					ArrayList.class, NFSNode.class};
			java.lang.reflect.Constructor<?> constructor = 
					flowSchedulerClass.getConstructor(parameterTypes);
			Object [] parameterList = {getModel(), "flowschedulerOn" + this.getName(), true, 
					outLinks, this};
			flowscheduler = (NFSFlowScheduler) constructor.newInstance(parameterList); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public NFSFlowScheduler getScheduler() {
		return flowscheduler;
	}
	
	public void PrintLinks() {
		for (NFSLink link : outLinks) {
			System.out.println(link);
		}
	}
	 
	@Override
	public String toString() {
		return this.ipaddress;
	}

}
