package simulator.entity.flow;

import java.util.Comparator;

public class NFSFlowComparator implements Comparator<NFSFlow> {

	@Override
	public int compare(NFSFlow flow1, NFSFlow flow2) {
		double demandgap1 = flow1.demandrate - flow1.datarate;
		double demandgap2 = flow2.demandrate - flow2.datarate;
		return demandgap1 > demandgap2 ? -1 : (demandgap1 == demandgap2 ? 0 : 1);
	}

}
