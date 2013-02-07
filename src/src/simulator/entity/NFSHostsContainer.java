package simulator.entity;

import java.util.ArrayList;
import java.util.Iterator;

import desmoj.core.simulator.Model;

import simulator.NetworkFlowSimulator;

public class NFSHostsContainer extends NFSNodesContainer implements Iterable<NFSHost> {
	
	private ArrayList<NFSHost> hosts = null;
	
	class HostIterator implements Iterator<NFSHost> {
		
		private int index = 0;
		
		@Override
		public boolean hasNext() {
			return index != hosts.size() ;
		}

		@Override
		public NFSHost next() {
			return hosts.get(index++);
		}

		@Override
		public void remove() {
			hosts.remove(index++);
		}
		
	}
	
	public NFSHostsContainer(Model model, String entityName, boolean showInReport) {
		super(model, entityName, showInReport);
		hosts = new ArrayList<NFSHost>();
	}
	
	@Override
	public void create(int n){
		for (int i = 0; i < n; i++) {
			NFSHost node = new NFSHost(getModel(), "node " + i, 
					true, 
					NetworkFlowSimulator.parser.getDouble("fluidsim.node.nicbandwidth", 1000),
					null, 
					NetworkFlowSimulator.parser.getInt("fluidsim.node.multihome", 1));
			hosts.add(node);
		}
	}
	
	public int GetN() {
		return hosts.size();
	}
	
	public NFSHost Get(int i) {
		return hosts.get(i);
	}
	
	public void addHost(NFSHost host) {
		hosts.add(host);
	}

	@Override
	public Iterator<NFSHost> iterator() {
		return new HostIterator();
	}
}
