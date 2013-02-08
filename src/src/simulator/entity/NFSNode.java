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
	
	public void addNewLink(NFSNode dst, double rate) {
		NFSLink link = new NFSLink(getModel(), "link-" + this + "-" + dst, true, rate, this, dst);
		outLinks.add(link);
	}
	
	public void assignIPAddress(String ip) {
		ipaddress = ip;
	}
	
	public boolean HasAllocatedIP() { 
		return ipaddress != null;
	}
	 
	@Override
	public String toString() {
		return this.ipaddress;
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
							"simulator.entity.flow.NFSFlowScheduler"));
			java.lang.reflect.Constructor<?> constructor = 
					flowSchedulerClass.getConstructor();
			Object [] parameterList = {outLinks};
			flowscheduler = (NFSFlowScheduler) constructor.newInstance(parameterList); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void PrintLinks() {
		for (NFSLink link : outLinks) {
			System.out.println(link);
		}
	}
}
