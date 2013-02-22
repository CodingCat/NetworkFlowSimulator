package simulator.entity;

import simulator.NetworkFlowSimulator;
import simulator.entity.application.NFSApplication;
import simulator.entity.application.NFSMapReduceApplication;
import simulator.entity.application.NFSOnOffApplication;
import simulator.entity.application.NFSParAgrApplication;
import simulator.entity.flow.NFSFlow;
import simulator.entity.flow.NFSOFController;
import simulator.entity.flow.NFSOpenFlowMessage;
import simulator.entity.topology.NFSLink;
import simulator.events.NFSOpenFlowSubscribeEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

public class NFSHost extends NFSNode{
	
	private NFSApplication [] apps = null;
	private NFSOFController controller = null;
	private double pauseDuration = 0.0;
	private boolean openflowswitch = false;
	private int subscribecnt = 0;
	private int subscribebound = 0;
	
	public NFSHost(Model model, String entityName, boolean showInLog, double bandWidth, String ip, int multihominglevel) {
		super(model, entityName, showInLog, bandWidth, ip);
		installApp();
		controller = NFSOFController._Instance(model);
		pauseDuration = NetworkFlowSimulator.parser.getDouble(
				"fluidsim.openflow.pauseEvent", 0.00025);
		openflowswitch = NetworkFlowSimulator.parser.getBoolean(
				"fluidsim.openflow.onoff", false);
		subscribebound = NetworkFlowSimulator.parser.getInt(
				"fluidsim.openflow.subscribebound", 3);
	}
	
	private void installApp() {
		apps = new NFSApplication[3];
		// onoff application
		apps[0] = new NFSOnOffApplication(getModel(), "OnOffAppOn" + getName(),
				true, NetworkFlowSimulator.parser.getDouble(
						"fluidsim.application.onoff.rate", 0.5), this);
		// mapreduce application
		apps[1] = new NFSMapReduceApplication(getModel(),
				"MRAppOn" + getName(), true,
				NetworkFlowSimulator.parser.getDouble(
						"fluidsim.application.mapreduce.rate", 10), this);
		// partition aggregator application
		apps[2] = new NFSParAgrApplication(getModel(), "PAAppOn" + getName(),
				true, NetworkFlowSimulator.parser.getDouble(
						"fluidsim.application.pa.rate", 0.05), this);
	}
	
	/**
	 * start a new flow from this host
	 * @param flow, the new flow
	 * @return the link selected to pass the flow data
	 */
	public NFSLink startNewFlow(NFSFlow flow) {
		NFSLink link = null;
		if (openflowswitch == false) {
			//select a path with ECMP link, with ECMP style
			//supporting multihoming
			link = flowscheduler.schedule(flow);
			//add the current link to the flow path
			flow.addPath(link);
		}
		else {
			//openflow
			NFSOpenFlowMessage msg = subscribe(flow);
			if (msg != null) {
				//start the flow 
			}
		}
		return link;
	}
	
	public NFSOpenFlowMessage subscribe(NFSFlow flow) {
		flow.expectedrate = flow.demandrate;
		NFSOpenFlowMessage msg = controller.schedule(outLinks.get(0), flow);
		if (msg == null) {
			if ((subscribecnt++) < subscribebound) {
				NFSOpenFlowSubscribeEvent subevent = new NFSOpenFlowSubscribeEvent(
						getModel(), getName() + "SubscribeEvent-" + flow.getName(), true);
				subevent.schedule(this, 
						flow, 
						TimeOperations.add(presentTime(), new TimeSpan(pauseDuration)));
			}
		}
		return msg;
	}
	
	public void run(int appIdx) {
		apps[appIdx].start();
	}	
	
	public void close(int appIdx) {
		apps[appIdx].close();
	}
}
