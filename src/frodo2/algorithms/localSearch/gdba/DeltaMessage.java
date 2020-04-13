package frodo2.algorithms.localSearch.gdba;

import java.io.Externalizable;

import frodo2.communication.MessageWith2Payloads;
import frodo2.solutionSpaces.Addable;

public class DeltaMessage<U extends Addable<U>> extends MessageWith2Payloads<String, U> implements Externalizable {
	private String variable;
	private int cycle;
	private U delta;

	public DeltaMessage(int cycle, String variable, U utility) {
		super(GDBA.DELTA_MSG_TYPE, variable, utility);

		this.variable = variable;
		this.cycle    = cycle;
		this.delta    = utility;
	}

	public String getVariable(){
		return variable;
	}
	
	public U getDelta(){
		return delta;
	}
	
	public int getCycle(){
		return cycle;
	}
}