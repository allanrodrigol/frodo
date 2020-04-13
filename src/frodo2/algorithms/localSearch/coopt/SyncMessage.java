package frodo2.algorithms.localSearch.coopt;

import java.io.Externalizable;

import frodo2.communication.Message;

public class SyncMessage extends Message implements Externalizable {
	private String variable;
	private int bestCycle;
	private int cycle;
	

	public SyncMessage(String variable, int bestCycle, int cycle) {
		super(COOPT.SYNC_MSG_TYPE);

		this.variable = variable;
		this.bestCycle = bestCycle;
	}
	
	public String getVariable() {
		return variable;
	}

	public int getBestCycle() {
		return this.bestCycle;
	}
	
	public int getCycle() {
		return cycle;
	}
}