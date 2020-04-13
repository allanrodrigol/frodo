package frodo2.algorithms.localSearch.gdba;

import frodo2.solutionSpaces.Addable;

public class View<V extends Addable<V>> {
	private String variable;
	private int    cycle;
	private V      value;
	
	public View(String variable, int cycle, V value) {
		this.variable = variable;
		this.cycle    = cycle;
		this.value    = value;
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
	
	public String toString(){
		return "{variable: " + this.getVariable() + ", cycle: " + this.getCycle() + ", value: " + this.getValue() + "}";
	}
}
