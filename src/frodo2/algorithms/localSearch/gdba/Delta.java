package frodo2.algorithms.localSearch.gdba;

public class Delta<U> {
	private String variable;
	private int cycle;
	private U delta;
	
	public Delta(String variable, int cycle, U delta) {
		this.variable = variable;
		this.cycle = cycle;
		this.delta = delta;
	}
	
	public String getVariable() {
		return variable;
	}
	
	public int getCycle(){
		return cycle;
	}

	public U getDelta() {
		return delta;
	}
	
	public String toString(){
		return "{variable: " + this.getVariable() + ", cycle: " + this.getCycle() + ", value: " + this.getDelta() + "}";
	}
}
