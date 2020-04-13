package frodo2.algorithms.localSearch.coopt;

import java.io.Externalizable;

import frodo2.communication.MessageWith2Payloads;
import frodo2.solutionSpaces.Addable;

public class CostMessage<U extends Addable<U>> extends MessageWith2Payloads<String, U> implements Externalizable {
	private String variable;
	private int cycle;
	private U utility;

	public CostMessage(String variable, int cycle, U utility) {
		super(COOPT.COST_MSG_TYPE, variable, utility);

		this.variable = variable;
		this.cycle = cycle;
		this.utility = utility;
	}

	public String getVariable() {
		return variable;
	}
	
	public int getCycle() {
		return cycle;
	}
	
	public U getUtility() {
		return utility;
	}
}