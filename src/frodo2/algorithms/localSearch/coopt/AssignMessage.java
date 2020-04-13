package frodo2.algorithms.localSearch.coopt;

import java.io.Externalizable;

import frodo2.communication.MessageWith3Payloads;
import frodo2.solutionSpaces.Addable;

public class AssignMessage<V extends Addable<V>> extends MessageWith3Payloads<String, String, V> implements Externalizable {
	private String variable;
	private int cycle;
	private V value;
	private double phase;
	
	public AssignMessage (String variable,int cycle, V value, double phase) {
		super(COOPT.ASSIGN_MSG_TYPE, variable, String.valueOf(cycle), value);
		
		this.variable = variable;
		this.value    = value;
		this.cycle    = cycle;
		this.phase    = phase;
	}
	
	public String getVariable() {
		return variable;
	}
	
	public int getCycle() {
		return cycle;
	}

	public V getValue() {
		return value;
	}
	
	public double getPhase() {
		return phase;
	}
	
	public View<V> getView() {
		return new View<V>(this.getVariable(), this.getCycle(), this.getValue(), this.getPhase());
	}
}