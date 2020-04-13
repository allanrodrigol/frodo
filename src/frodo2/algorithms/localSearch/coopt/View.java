package frodo2.algorithms.localSearch.coopt;

import frodo2.solutionSpaces.Addable;

public class View<V extends Addable<V>> {
	private String variable;
	private int    cycle;
	private V      value;
	private double phase;
	
	
	public View(String variable, int cycle, V value, double phase){
		this.variable = variable;
		this.cycle    = cycle;
		this.value    = value;
		this.phase    = phase;
	}
	
	public String getVariable() {
		return variable;
	}
	
	public int getCycle(){
		return cycle;
	}

	public V getValue() {
		return value;
	}
	
	public double getPhase(){
		return phase;
	}
	
	public String toString(){
		return "{variable: " + this.getVariable() + ", cycle: " + this.getCycle() + ", value: " + this.getValue() + ", phase: " + this.getPhase() + "}";
	}
}
