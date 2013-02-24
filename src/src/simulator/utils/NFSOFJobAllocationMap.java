package simulator.utils;

import java.util.HashMap;
import java.util.Map.Entry;

import simulator.entity.application.NFSMapReduceJob;
import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSTaskBindedFlow;

/**
 * 
 * store the job loads in certain link
 * 
 */
public class NFSOFJobAllocationMap {
	
	//job name -> sum of remaining input of flows on the link
	private HashMap<String, HashMap<NFSFlow, Double>> jobloadMap = null;
	//job name -> sum of job fair share
	private HashMap<String, Double> jobweightMap = null;
	
	private double sumflowloads = 0;
	private double sumjobweights = 0;
	
	public NFSOFJobAllocationMap() {
		jobloadMap = new HashMap<String, HashMap<NFSFlow, Double>>();
		jobweightMap = new HashMap<String, Double>();
	}
	
	
	public double getJobWeight(String jobname) {
		return jobweightMap.get(jobname);
	}
	
	public double getFlowWeight(String jobname, NFSFlow newflow) {
		return jobloadMap.get(jobname).get(newflow);
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
	
	public void registerNewJob(NFSMapReduceJob newjob) {
		if (jobweightMap.containsKey(newjob)) {
			for (Entry<String, Double> entry : jobweightMap.entrySet()) {
				jobweightMap.put(entry.getKey(), 
						entry.getValue() * sumjobweights / (sumjobweights + newjob.getPriority()));
			}
			sumjobweights += newjob.getPriority();
			jobweightMap.put(newjob.getName(), (double) newjob.getPriority() / sumjobweights);
		}
	}
	
	public void registerNewFlow(String jobname, NFSTaskBindedFlow flow) {
		HashMap<NFSFlow, Double> jobflowload = jobloadMap.get(jobname);
		for (Entry<NFSFlow, Double> entry : jobflowload.entrySet()) {
			jobflowload.put(entry.getKey(), entry.getValue() * sumflowloads / 
					(sumflowloads + flow.getleftsize()));
		}
		sumflowloads += flow.getleftsize();
		jobflowload.put(flow, flow.getleftsize());
		jobloadMap.put(jobname, jobflowload);
	}
	
	public void finishflow(NFSTaskBindedFlow finishedflow) {
		NFSMapReduceJob job = finishedflow.getSender().getJob();
		String jobname = job.getName();
		sumflowloads -= jobloadMap.get(jobname).get(finishedflow);
		jobloadMap.get(jobname).remove(finishedflow);
	}
	
	public void clearJobInfo(NFSMapReduceJob job) {
		//jobweigthMap
		sumjobweights -= job.getPriority();
		jobweightMap.remove(job.getName());
		for (Entry<String, Double> entry : jobweightMap.entrySet()) {
			entry.setValue(entry.getValue() / sumjobweights);
		}
		//jobloadMap
		jobloadMap.remove(job.getName());
	}
}
