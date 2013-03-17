package simulator.entity.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import simulator.entity.application.NFSMapReduceJob;
import simulator.entity.flow.NFSFlow.NFSFlowStatus;
import simulator.entity.topology.NFSLink;
import simulator.utils.NFSDoubleCalculator;

/**
 * 
 * store the job loads in certain link
 * 
 */
public class NFSOFJobAllocationMap {
	
	//job name -> sum of remaining input of flows on the link
	private HashMap<String, ArrayList<NFSTaskBindedFlow>> jobflowMap = null;
	//job list
	private ArrayList<NFSMapReduceJob> joblist = null;
	//job name -> allocation (sum of all flows' left size belonging to this job)
	private HashMap<String, Double> joballocMap = null;
	
	private double sumjobweights = 0;
	private NFSLink keylink = null;
	
	public NFSOFJobAllocationMap(NFSLink link) {
		jobflowMap = new HashMap<String, ArrayList<NFSTaskBindedFlow>>();
		joblist = new ArrayList<NFSMapReduceJob>();
		joballocMap = new HashMap<String, Double>();
		keylink = link;
	}
	
	public double getFlowAllocation(NFSTaskBindedFlow flow) {
		double jobweight = getPossibleJobAllocation(flow.getSender().getJob());
		double flowweight = getFlowWeight(flow);
		double sumrateLatencyflows = 0.0;
		for (NFSFlow runflow : keylink.getRunningFlows()) {
			if (runflow.isLatencySensitive()) 
				sumrateLatencyflows = NFSDoubleCalculator.sum(sumrateLatencyflows, runflow.datarate);
		}
		double availbandwidth = NFSDoubleCalculator.sub(keylink.getTotalBandwidth(), sumrateLatencyflows);
		return NFSDoubleCalculator.mul(availbandwidth, NFSDoubleCalculator.mul(jobweight, flowweight));
	}
	
	public double getFlowWeight(NFSTaskBindedFlow flow) {
		String jobname = flow.getSender().getJob().getName();
		return NFSDoubleCalculator.div(flow.getDemandSize(), joballocMap.get(jobname));
	}
	
	public double getPossibleJobAllocation(NFSMapReduceJob job) {
		if (!joblist.contains(job)) {
			return NFSDoubleCalculator.div((double) job.getPriority(), 
					(double) (job.getPriority() + sumjobweights));
		}
		return NFSDoubleCalculator.div((double) job.getPriority(), sumjobweights);
	}
	
	public double getPossibleFlowWeight(NFSTaskBindedFlow newflow) {
		NFSMapReduceJob job = newflow.getSender().getJob();
		if (!joblist.contains(job)) registerNewJob(job);
		return NFSDoubleCalculator.div(
				newflow.getDemandSize(),
				NFSDoubleCalculator.sum(newflow.getDemandSize(), 
						joballocMap.get(job.getName())));
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
			jobflowMap.put(newjob.getName(), new ArrayList<NFSTaskBindedFlow>());
			joballocMap.put(newjob.getName(), 0.0);
		}
	}
	
	private void registerNewFlow(NFSTaskBindedFlow flow) {
		String jobname = flow.getSender().getJob().getName();
		if (jobflowMap.containsKey(jobname)) {
			jobflowMap.get(jobname).add(flow);
			joballocMap.put(jobname, 
					NFSDoubleCalculator.sum(joballocMap.get(jobname), flow.getDemandSize()));
			System.out.println("register new flow " + flow.getName() + "-" + 
					flow.datarate + "-" + flow.getDemandSize() + " on link " + 
					keylink.getName());
		}
	}
	
	public void updateflowrate(String modelflag) {
		double sumrateLatencyflows = 0.0;
		for (NFSFlow flow : keylink.getRunningFlows()) {
			if (flow.isLatencySensitive()) 
				sumrateLatencyflows = NFSDoubleCalculator.sum(sumrateLatencyflows, flow.datarate);
		}
		for (Entry<String, ArrayList<NFSTaskBindedFlow>> entry : jobflowMap.entrySet()) {
			ArrayList<NFSTaskBindedFlow> jobflows = entry.getValue();
			for (NFSTaskBindedFlow flow : jobflows) {
				if (flow.getStatus().equals(NFSFlowStatus.NEWSTARTED)) continue;
				if (modelflag.equals("closeflow") &&
						(flow.isFullyMeet() || flow.getBottleneckLink().equals(keylink))) continue;
				NFSMapReduceJob job = flow.getSender().getJob();
				double jobweight = NFSDoubleCalculator.div(job.getPriority(),
						sumjobweights);
				double flowweight = NFSDoubleCalculator.div(flow.getDemandSize(),
						joballocMap.get(job.getName()));
				double possibleRate = NFSDoubleCalculator.mul(
						NFSDoubleCalculator.sub(keylink.getTotalBandwidth(), sumrateLatencyflows), 
						NFSDoubleCalculator.mul(jobweight, flowweight));
				if (!modelflag.equals("closeflow")) {
					if (flow.datarate > possibleRate) {
				//		System.out.println("jobweight:" + jobweight + " flowweight:" + flowweight + 
					//			" possibleRate:" + possibleRate);
						flow.update(possibleRate); 
						flow.setBottleneckLink(keylink);
					}
				}
				else {
		//			flow.update(possibleRate);
					increaseflowrate(flow, possibleRate);
				}
				if (modelflag.equals("closeflow")) flow.expectedrate = flow.datarate;
			}
		}
	}
	
	
	
	private void increaseflowrate(NFSTaskBindedFlow flow, double possibleRate) {
		double candidateRate = possibleRate;
		for (NFSLink link : flow.getPaths()) {
			double localallocation = NFSOFController._Instance().getFlowRate(link, flow);
			if (candidateRate > localallocation) {
				candidateRate = localallocation;
				flow.setBottleneckLink(link);
			}
		}
		flow.update(candidateRate);
	}
	
	public double getJobWeight(NFSMapReduceJob job) {
		return NFSDoubleCalculator.div((double) job.getPriority(),
				sumjobweights);
	}
	
	public void finishflow(NFSTaskBindedFlow finishedflow) {
		NFSMapReduceJob job = finishedflow.getSender().getJob();
		String jobname = job.getName();
		jobflowMap.get(jobname).remove(finishedflow);
		joballocMap.put(
				jobname, 
				NFSDoubleCalculator.sub(joballocMap.get(jobname), 
						finishedflow.getDemandSize()));
		if (joballocMap.get(jobname) == 0.0) {
			//System.out.println("remove job " + job.getName() + " on link " + keylink.getName());
			clearJobInfo(job);
		}
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
