package frodo2.algorithms.localSearch.dsasdp;

import java.io.Externalizable;

import frodo2.communication.Message;
import frodo2.solutionSpaces.Addable;

public class BestCycleMessage<V extends Addable<V>> extends Message implements Externalizable {
	private String variable;
	private int bestCycle;

	public BestCycleMessage(String variable, int bestCycle) {
		super(DSASDP.BEST_CYCLE_MSG_TYPE);
		
		this.variable = variable;
		this.bestCycle = bestCycle;
	}
	
	public String getVariable() {
		return variable;
	}

	public int getBestCycle() {
		return this.bestCycle;
	}
}