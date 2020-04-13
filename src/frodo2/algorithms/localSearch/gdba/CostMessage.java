package frodo2.algorithms.localSearch.gdba;

import java.io.Externalizable;

import frodo2.communication.MessageWith2Payloads;
import frodo2.solutionSpaces.Addable;

public class CostMessage<U extends Addable<U>> extends MessageWith2Payloads<String, U> implements Externalizable {
	private String variable;
	private U utility;
	private int cycle;

	public CostMessage(Integer cycle, String variable, U utility) {
		super(GDBA.COST_MSG_TYPE, variable, utility);

		this.cycle    = cycle;
		this.variable = variable;
		this.utility  = utility;
	}
	
	public int getCycle() {
		return cycle;
	}

	public String getVariable() {
		return variable;
	}
	
	public U getUtility() {
		return utility;
	}
}