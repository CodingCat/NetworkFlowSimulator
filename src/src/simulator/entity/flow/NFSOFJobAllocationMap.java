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
	private double sumbesteffortbandwidth = 0;
	private NFSLink keylink = null;
	
	public NFSOFJobAllocationMap(NFSLink link) {
		jobflowMap = new HashMap<String, ArrayList<NFSTaskBindedFlow>>();
		joblist = new ArrayList<NFSMapReduceJob>();
		joballocMap = new HashMap<String, Double>();
		keylink = link;
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
		sumbesteffortbandwidth += newflow.datarate;
	}
	
	private void registerNewJob(NFSMapReduceJob newjob) {
		if (joblist.contains(newjob) == false) {
			joblist.add(newjob);
			sumjobweights += newjob.getPriority();
			jobflowMap.put(newjob.getName(), new ArrayList<NFSTaskBindedFlow>());
			joballocMap.put(newjob.getName(), 0.0);
		//	System.out.println("register new job " + newjob.getName() + " for link " + keylink.getName());
		}
	}
	
	private void registerNewFlow(NFSTaskBindedFlow flow) {
		String jobname = flow.getSender().getJob().getName();
		if (jobflowMap.containsKey(jobname)) {
			
			jobflowMap.get(jobname).add(flow);
			System.out.println("add a new flow " + flow.getName() + 
					" with the status: " + flow.getStatus().toString());
			joballocMap.put(jobname, 
					NFSDoubleCalculator.sum(joballocMap.get(jobname), flow.getDemandSize()));
			System.out.println("register new flow " + flow.getName() + "-" + 
					flow.expectedrate + "-" + flow.getDemandSize() + " on link " + 
					keylink.getName());
		}
	}
	
	public void sync() {
		double newsumbesteffortbandwidth = 0;
		for (Entry<String, ArrayList<NFSTaskBindedFlow>> jobflowentry : jobflowMap.entrySet()) {
			ArrayList<NFSTaskBindedFlow> flowlist = jobflowentry.getValue();
			for (NFSTaskBindedFlow flow : flowlist) {
				//TODO: new? running
				newsumbesteffortbandwidth += flow.datarate;
			}
		}
		sumbesteffortbandwidth = newsumbesteffortbandwidth;
	}
	
	public void updateflowrate(String modelflag) {
		for (Entry<String, ArrayList<NFSTaskBindedFlow>> entry : jobflowMap.entrySet()) {
			ArrayList<NFSTaskBindedFlow> jobflows = entry.getValue();
			for (NFSTaskBindedFlow flow : jobflows) {
				if (flow.getStatus().equals(NFSFlowStatus.NEWSTARTED)) continue;
				if (modelflag.equals("closeflow") && flow.getBottleneckLink().equals(keylink) == false) continue;
				NFSMapReduceJob job = flow.getSender().getJob();
				double jobweight = NFSDoubleCalculator.div(job.getPriority(),
						sumjobweights);
				double flowweight = NFSDoubleCalculator.div(flow.getDemandSize(),
						joballocMap.get(job.getName()));
				System.out.print("change " + flow.getName() + " rate from " + flow.datarate + " to ");
				double possibleRate = NFSDoubleCalculator.mul(
						NFSDoubleCalculator.sum(sumbesteffortbandwidth, keylink.getAvailableBandwidth()), 
						NFSDoubleCalculator.mul(jobweight, flowweight));
				if (!modelflag.equals("closeflow")) {
					if (flow.datarate > possibleRate) flow.update(possibleRate); 
				}
				else {
					flow.update(possibleRate);
				}
				System.out.println(flow.datarate);
				if (modelflag.equals("closeflow")) flow.expectedrate = flow.datarate;
			}
		}
	}
	
	public double getJobWeight(NFSMapReduceJob job) {
		return NFSDoubleCalculator.div((double) job.getPriority(),
				sumjobweights);
	}
	
	public void setsumbesteffortbw(double bw) {
		sumbesteffortbandwidth = bw;
	}
	
	public void finishflow(NFSTaskBindedFlow finishedflow) {
		NFSMapReduceJob job = finishedflow.getSender().getJob();
		String jobname = job.getName();
		sumbesteffortbandwidth -= finishedflow.datarate;
		jobflowMap.get(jobname).remove(finishedflow);
		joballocMap.put(
				jobname, 
				NFSDoubleCalculator.sub(joballocMap.get(jobname), 
						finishedflow.getDemandSize()));
		if (joballocMap.get(jobname) == 0.0) {
			System.out.println("remove job " + job.getName() + " on link " + keylink.getName());
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
