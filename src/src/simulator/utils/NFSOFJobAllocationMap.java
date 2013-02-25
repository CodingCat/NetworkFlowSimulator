package simulator.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import simulator.entity.application.NFSMapReduceJob;
import simulator.entity.flow.NFSTaskBindedFlow;

/**
 * 
 * store the job loads in certain link
 * 
 */
public class NFSOFJobAllocationMap {
	
	//job name -> sum of remaining input of flows on the link
	private HashMap<String, HashSet<NFSTaskBindedFlow>> jobflowMap = null;
	//job list
	private HashSet<NFSMapReduceJob> joblist = null;
	//job name -> allocation (sum of all flows' left size belonging to this job)
	private HashMap<String, Double> joballocMap = null;
	
	private double sumflowloads = 0;
	private double sumjobweights = 0;
	
	public NFSOFJobAllocationMap() {
		jobflowMap = new HashMap<String, HashSet<NFSTaskBindedFlow>>();
		joblist = new HashSet<NFSMapReduceJob>();
		joballocMap = new HashMap<String, Double>();
	}
	
	public double getFlowWeight(NFSTaskBindedFlow flow) {
		String jobname = flow.getSender().getJob().getName();
		return NFSDoubleCalculator.div(flow.getleftsize(), joballocMap.get(jobname));
	}
	
	public double getsumrates() {
		return sumflowloads;
	}
	
	public double getPossibleJobAllocation(int p) {
		return (double) p / (double) (p + sumjobweights);
	}
	
	public double getPossibleFlowWeight(double input) {
		return input / (input + sumflowloads);
	}
	
	public void register(NFSTaskBindedFlow newflow) {
		registerNewJob(newflow.getSender().getJob());
		registerNewFlow(newflow);
		updateflowrate("newflow");
	}
	
	private void registerNewJob(NFSMapReduceJob newjob) {
		if (joblist.contains(newjob) == false) {
			joblist.add(newjob);
			sumjobweights += newjob.getPriority();
			jobflowMap.put(newjob.getName(), new HashSet<NFSTaskBindedFlow>());
			joballocMap.put(newjob.getName(), 0.0);
		}
	}
	
	private void registerNewFlow(NFSTaskBindedFlow flow) {
		String jobname = flow.getSender().getJob().getName();
		if (jobflowMap.containsKey(jobname)) {
			jobflowMap.get(jobname).add(flow);
			joballocMap.put(jobname, 
					NFSDoubleCalculator.sum(joballocMap.get(jobname), flow.getleftsize()));
			sumflowloads = NFSDoubleCalculator.sum(sumflowloads, flow.getleftsize());
		}
	}
	
	public void updateflowrate(String modelflag) {
		for (Entry<String, HashSet<NFSTaskBindedFlow>> entry : jobflowMap.entrySet()) {
			HashSet<NFSTaskBindedFlow> jobflows = entry.getValue();
			for (NFSTaskBindedFlow flow : jobflows) {
				NFSMapReduceJob job = flow.getSender().getJob();
				double jobweight = NFSDoubleCalculator.div(job.getPriority(),
						sumjobweights);
				double flowweight = NFSDoubleCalculator.div(flow.datarate,
						joballocMap.get(job.getName()));
				flow.update('+', NFSDoubleCalculator.mul(sumflowloads,
						NFSDoubleCalculator.mul(jobweight, flowweight)));
				if (modelflag.equals("closerate")) flow.expectedrate = flow.datarate;
			}
		}
	}
	
	public void adjustallocation(double v) {
		sumflowloads = NFSDoubleCalculator.sub(sumflowloads, v);
		updateflowrate("updateflow");
	}
	
	public void finishflow(NFSTaskBindedFlow finishedflow) {
		NFSMapReduceJob job = finishedflow.getSender().getJob();
		String jobname = job.getName();
		sumflowloads -= finishedflow.getleftsize();
		jobflowMap.get(jobname).remove(finishedflow);
		joballocMap.put(
				jobname, 
				NFSDoubleCalculator.sub(joballocMap.get(jobname), 
						finishedflow.getleftsize()));
		if (joballocMap.get(jobname) == 0.0) {
			clearJobInfo(job);
		}
		updateflowrate("closerate");
	}
	
	private void clearJobInfo(NFSMapReduceJob job) {
		//jobweigthMap
		sumjobweights -= job.getPriority();
		joblist.remove(job);
		//jobloadMap
		jobflowMap.remove(job.getName());
		joballocMap.remove(job.getName());
	}
}
