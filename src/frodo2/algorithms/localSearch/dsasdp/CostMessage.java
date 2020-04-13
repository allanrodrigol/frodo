package frodo2.algorithms.localSearch.dsasdp;

import java.io.Externalizable;

import frodo2.communication.MessageWith3Payloads;
import frodo2.solutionSpaces.Addable;

public class CostMessage<U extends Addable<U>> extends MessageWith3Payloads<Integer, String, U> implements Externalizable {
	private Integer cycle;
	private String variable;
	private U utility;

	public CostMessage(Integer cycle, String variable, U utility) {
		super(DSASDP.COST_MSG_TYPE, cycle, variable, utility);
		
		this.cycle = cycle;
		this.variable = variable;
		this.utility = utility;
	}
	
	public String getVariable() {
		return variable;
	}
	
	public int getCycle() {
		return cycle.intValue();
	}
	
	public U getUtility() {
		return utility;
	}
}