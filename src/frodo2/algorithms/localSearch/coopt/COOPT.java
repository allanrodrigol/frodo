package frodo2.algorithms.localSearch.coopt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jdom2.Element;

import frodo2.algorithms.AgentInterface;
import frodo2.algorithms.SolutionCollector;
import frodo2.algorithms.StatsReporterWithConvergence;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration.DFSview;
import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.Queue;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableConflicts;
import frodo2.solutionSpaces.AddableDelayed;
import frodo2.solutionSpaces.DCOPProblemInterface;
import frodo2.solutionSpaces.UtilitySolutionSpace;

/**
 * 
 * This class implements the Coupled Oscillator OPTimization (COOPT) as described in
 * "COOPT: Using Collective Behavior of Coupled Oscillators for Solving DCOP" by 
 * Leite and Enembreck, 2019. This algorithm introduces concepts of synchronization 
 * of coupled oscillators inspired on the Kuramoto model for speeding up the convergence 
 * process towards high-quality solutions. COOPT solves DCOP iteratively by agents 
 * exchanging local information that brings them to a consensus, when reproducing the 
 * synchronization behavior in a network of coupled oscillators.
 * 
 * @param <V>	type used for variable values
 * @param <U> 	type used for utility values
 */
public class COOPT<V extends Addable<V>, U extends Addable<U>> implements StatsReporterWithConvergence<V> {
	/** Type of the message used for reporting to neighboring the new value assigned to a given variable */
	public static final MessageType ASSIGN_MSG_TYPE = new MessageType("Assign");
	
	/** Type of the message used for reporting the start of a new synchronous cycle to children variables */
	public static final MessageType SYNC_MSG_TYPE = new MessageType("Sync");
	
	/** Type of the message used for reporting to parent the cost of a given child variable */
	public static final MessageType COST_MSG_TYPE = new MessageType("Cost");
	
	/** Type of the message containing the assignment history */
	public static final MessageType CONV_STATS_MSG_TYPE = new MessageType("COOPT", "ConvStats");
	
	/** The global coupling strength represented by K in original Kuramoto model */
	private double coupling;

	/** The number of synchronous cycles before termination, except except for isolated variables */
	private int maxCycles;

	/** The number of variables that already finish the search process */
	private int variableFinishedCounter;
	
	/** The message queue */
	private Queue queue;

	/** The agent's problem */
	private DCOPProblemInterface<V,U> problem;

	/** For each variable its assignment history */
	private HashMap<String, ArrayList<CurrentAssignment<V>>> assignmentHistoriesMap;

	/** The map representing a container with information for each variable held by the agent */
	private HashMap<String,VariableInfo> infos;

	/** \c true when the module has been finished, and \c false otherwise */
	private boolean done;
	
	/** Whether the listener should record the assignment history or not */
	private boolean convergence;
	
	/** Random value generator used for choosing initial values for each variable */
	public static Random RANDOM = new Random(1);
	
	/** Constructor for the stats gatherer mode
	 * @param parameters	the parameters of the module
	 * @param problem		the overall problem
	 */	
	public COOPT(Element params, DCOPProblemInterface<V, U> prob) {
		problem = prob;
		convergence = false;
		
		infos = new HashMap<String,VariableInfo>();
	}

	/**
	 * Constructor for the agent module
	 * @param problem		description of the local problem
	 * @param parameters	parameters of the module
	 */		
	public COOPT(DCOPProblemInterface<V, U> problem, Element parameters) {
		this(parameters,problem);
		
		String maxCycles = parameters.getAttributeValue("maxCycles");
		String coupling = parameters.getAttributeValue("coupling");
		
		this.maxCycles = maxCycles == null ? 100 : Integer.parseInt(maxCycles);
		
		/* Suggested value by authors (see comments for more information) */
		this.coupling = coupling == null ? 1.0 : Double.parseDouble(coupling);
		
		convergence = Boolean.parseBoolean(parameters.getAttributeValue("convergence"));
	}
	
	public void getStatsFromQueue(Queue queue) {
		queue.addIncomingMessagePolicy(CONV_STATS_MSG_TYPE,this);
	}
	
	public void setSilent(boolean silent) {
		//not implemented
	}

	public void reset() {
		infos.clear();
		
		done = false;
		variableFinishedCounter = 0;
	}
	
	public void setQueue(Queue queue) {
		this.queue = queue;
	}
	
	public Collection<MessageType> getMsgTypes() {
		Collection<MessageType> types = new ArrayList<>();
		
		types.add(DFSgeneration.OUTPUT_MSG_TYPE);
		
		types.add(AgentInterface.START_AGENT);
		types.add(AgentInterface.AGENT_FINISHED);
		
		types.add(ASSIGN_MSG_TYPE);
		types.add(SYNC_MSG_TYPE);
		types.add(COST_MSG_TYPE);

		return types;
	}
	
	public HashMap<String, ArrayList<StatsReporterWithConvergence.CurrentAssignment<V>>> getAssignmentHistories() {
		/* Not implemented */
		return new HashMap<String, ArrayList<StatsReporterWithConvergence.CurrentAssignment<V>>>();
	}

	public Map<String, V> getCurrentSolution() {
		/* Not implemented */
		return null;
	}

	@SuppressWarnings({ "unchecked" })
	public void notifyIn(Message message) {
		MessageType type = message.getType();

		if (type.equals(CONV_STATS_MSG_TYPE)) { // in stats gatherer mode, the message sent by a variable containing the assignment history 
			StatsReporterWithConvergence.ConvStatMessage<V> convStatMessage = (StatsReporterWithConvergence.ConvStatMessage<V>) message;
			assignmentHistoriesMap.put(convStatMessage.getVar(), convStatMessage.getAssignmentHistory());
			
			return;
		}
		
		if (type.equals(AgentInterface.START_AGENT)) {
			this.reset();
			
			return;
		}
		
		if (type.equals(DFSgeneration.OUTPUT_MSG_TYPE)) {
			DFSgeneration.MessageDFSoutput<V,U> DFSout = (DFSgeneration.MessageDFSoutput<V,U>) message;
			DFSview<V,U> view = DFSout.getNeighbors();

			VariableInfo info = new VariableInfo(view.getID());
			info.parent = view.getParent();
			info.children.addAll(view.getChildren());

			infos.put(view.getID(), info);

			this.initVar(view.getID());
			
			return;
		}
		
		if (type.equals(AgentInterface.AGENT_FINISHED)) {
			done = true;
			
			return;
		}
		
		if (!done) { 
			if (!this.isReady()) {
				queue.sendMessageToSelf(message);
				return;
			}
			
			if (type.equals(ASSIGN_MSG_TYPE)) {
				AssignMessage<V> assign = (AssignMessage<V>) message;
				
				for (String variable : problem.getMyVars()) {
					VariableInfo info = infos.get(variable);
					
					if (problem.getNeighborVars(variable).contains(assign.getVariable())) {
						info.updateLocalView(assign.getCycle(), assign.getVariable(), assign.getValue(), assign.getPhase());
						
						this.computeLocalCost(variable, assign.getCycle());
					}
				}

			} else if (type.equals(SYNC_MSG_TYPE)) {
				SyncMessage sync = (SyncMessage) message;
				
				for (String variable : problem.getMyVars()) {
					VariableInfo info = infos.get(variable);

					if (info.parent != null && info.parent.equals(sync.getVariable())) {
						info.updateBestCycle(sync.getBestCycle());
					
						this.synchronize(variable);
					}
				}

			} else if (type.equals(COST_MSG_TYPE)) {
				CostMessage<U> cost = (CostMessage<U>) message;
				
				for (String variable : problem.getMyVars()) {
					VariableInfo info = infos.get(variable);

					if (info.children.contains(cost.getVariable())) {
						info.updateLocalCost(cost.getCycle(), cost.getVariable(), cost.getUtility());

						this.computeLocalCost(variable, cost.getCycle());
					}
				}
			}
		}
	}
		
	private void initVar(String variable) {
		VariableInfo info = infos.get(variable);
		V value = info.domain[(int) (Math.random() * info.domain.length)]; //chooses a value randomly
		U utility = info.getBestLocalUtility(value); //calculates the best local utility
		
		info.frequency = utility.doubleValue();
		info.phase = RANDOM.nextGaussian();
		
		this.assign(variable, value);
		this.sendAssignMessage(variable);
	}
	

	public boolean isReady() {
		for (String variable : problem.getMyVars()) {
			if (infos.get(variable) == null)
				return false;
		}

		return true;
	}

	protected void assign(String variable, V value) {
		VariableInfo info = infos.get(variable);
		info.assign(value);

		this.sendStatsMessage(variable);
	}

	public void synchronize(String variable) {
		VariableInfo info = infos.get(variable);
		
		if (info.counter < maxCycles) {
			V value = this.updateFrequence(variable);
			this.updatePhase(variable, value);

			info.counter++;
			
			this.assign(variable, value);
			this.sendAssignMessage(variable);
		} else {
			this.sendStatsMessage(variable);
			queue.sendMessage(AgentInterface.STATS_MONITOR, new SolutionCollector.AssignmentMessage<V> (variable, info.bestValue));
			
			variableFinishedCounter++;
			
			if (variableFinishedCounter == infos.size()) {
				queue.sendMessageToSelf(new Message(AgentInterface.AGENT_FINISHED));
			}
		}
		
		if (info.children.size() > 0) { //not leaves
			this.sendSyncMessage(variable);
		}
	}

	private void computeLocalCost(String variable, int cycle) {
		VariableInfo info = infos.get(variable);
		
		if (info.isViewUpdated(cycle) && info.isLocalCostUpdated(cycle)) {
			if (info.parent == null) { //root
				U cost = info.getSubtreeCost();
					
				if (info.bestCost.doubleValue() > cost.doubleValue()) {
					info.bestValue = info.value;
					info.bestCycle = info.counter;
					info.bestCost = cost;
				}
				
				this.synchronize(variable);
			} else {
				this.sendCostMessage(variable, cycle);
			}
		}
	}

	private V updateFrequence(String variable) {
		VariableInfo info = infos.get(variable);
		
		double freq = 0;
		double min = Double.MAX_VALUE;
		double ti = info.phase;

		V value = null;

		Map<V, Double> argmax = new HashMap<V, Double>();

		for (int i = 0; i < info.domain.length; i++) {
			V di = info.domain[i];

			double cost = 0;
			double sum = 0;

			for (String neighbor : problem.getNeighborVars(info.name)) {
				View<V> view = info.view.get(neighbor + "-" + info.counter);

				if (view != null) {
					V dj = info.solution.get(neighbor); //view.getValue();
					
					double tj = view.getPhase();
					double fij = info.fij(di,neighbor,dj).doubleValue();

					cost += fij;
					sum += fij + (fij * 0.5 * (Math.cos(tj - ti) + 1));
				}
			}

			if (sum < min || value == null) {
				min = sum;
				freq = cost;
				value = di;
			}

			argmax.put(di, sum);
		}

		info.frequency = freq;

		return value;
	}

	private void updatePhase(String variable, V di) {
		VariableInfo info = infos.get(variable);
		
		double sum = 0;
		double ti = info.phase;

		for (String neighbor : problem.getNeighborVars(info.name)) {
			View<V> view = info.view.get(neighbor + "-" + info.counter);

			if (view != null) {
				V dj = info.solution.get(neighbor); //view.getValue();
				
				double tj = view.getPhase();
				double fij = info.fij(di,neighbor,dj).doubleValue();

				sum += fij * Math.sin(tj - ti);
			}
		}

		info.phase = info.frequency + ((coupling / problem.getVariables().size()) * sum);
	}

	private void sendAssignMessage(String variable) {
		VariableInfo info = infos.get(variable);
		AssignMessage<V> message = new AssignMessage<V>(info.name, info.counter, info.value, info.phase);
		queue.sendMessageToMulti(this.getAgents(problem.getNeighborVars(variable)), message);
	}

	private void sendSyncMessage(String variable) {
		VariableInfo info = infos.get(variable);
		SyncMessage message = new SyncMessage(info.name, info.bestCycle, info.counter);

		queue.sendMessageToMulti(this.getAgents(info.children), message);
	}

	private void sendCostMessage(String variable, int cycle) {
		VariableInfo info = infos.get(variable);
		CostMessage<U> message = new CostMessage<U>(info.name, cycle, info.getSubtreeCost());

		queue.sendMessage(problem.getOwner(info.parent), message);
	}

	private void sendStatsMessage(String variable) {
		if(convergence) {
			VariableInfo info = infos.get(variable);
			
			ArrayList<CurrentAssignment<V>> history = assignmentHistoriesMap.get(variable);
			history.add(new CurrentAssignment<V>(queue.getCurrentTime(), info.counter, info.value));
			queue.sendMessage(AgentInterface.STATS_MONITOR, new StatsReporterWithConvergence.ConvStatMessage<V>(CONV_STATS_MSG_TYPE, variable, history));
		}
	}
	
	public Collection<String> getAgents(Collection<String> variables) {
		Collection<String> agents = new ArrayList<String>();

		for (String variable : variables) {
			String a = problem.getOwner(variable);
			if (!agents.contains(a))
				agents.add(a);
		}

		return agents;
	}
	
	class VariableInfo {
		public String name;
		public List<? extends UtilitySolutionSpace<V, U>> spaces;

		public Map<String,View<V>> view;
		public Map<String, U> costs;
		
		public String parent;
		public Collection<String> children;

		public V value;
		public V[] domain;

		public double phase;
		public double frequency;
		
		public V bestValue;
		public U bestCost;
		public int bestCycle;
		
		public Map<String,V> solution;
		public Map<String,Double> phases;

		public int counter;
		
		public VariableInfo(String var) {
			name = var;
			counter = 1;

			bestCycle = 1;
			bestCost = problem.getPlusInfUtility();

			children = new ArrayList<String>();

			view = new HashMap<String,View<V>>();
			costs = new HashMap<String,U>();
			
			solution = new HashMap<String,V>();
			phases =  new HashMap<String,Double>();
			
			domain = problem.getDomain(name);
			spaces = problem.getSolutionSpaces(name, false);
		}
		
		public void assign(V v) {
			solution.put(name, v);
			
			value = v;
			
			if (bestValue == null)
				bestValue = value;
		}
		
		@SuppressWarnings("unchecked")
		public V getBestLocalValue() {
			V value = null;
			U utility = null;
			
			Collection<String> neighbors = problem.getNeighborVars(name);
			
			for (int i = 0; i < domain.length; i++) {
				int conflicts = 0;
				
				AddableDelayed<U> util = problem.getZeroUtility().addDelayed();
				
				Map<String,V> assignments = new HashMap<String,V>();
				assignments.put(name, domain[i]);
				
				for (int j = 0; j < spaces.size(); j++) {
					UtilitySolutionSpace<V, U> space = spaces.get(j);
					
					for (int k = 0; k < space.getVariables().length; k++) {
						if (space.getVariable(k).equals(name))
							continue;
						
						for (String neighbor : neighbors) {
							if (space.getVariable(k).equals(neighbor)) {
								space = space.blindProject(neighbor, false);
								break;
							}
						}
					}
					
					String[] variables = space.getVariables();
					int size = variables.length;
					
					V[] values = (V[]) Array.newInstance(domain.getClass().getComponentType(), size);
	
					boolean complete = true;
					
					for (int k = 0; k < size; k++) {
						if (assignments.get(variables[k]) != null) {
							values[k] = assignments.get(variables[k]);
						} else {
							complete = false;
							break;
						}
					}
					
					if (!complete)
						continue;
	
					U c = space.getUtility(variables, values);
					
					if (c == problem.getPlusInfUtility())
						conflicts++;
	
					util.addDelayed(c);
				}
	
				U cost = new AddableConflicts<U>(util.resolve(), conflicts).getUtility();
				
				if (utility == null || utility.compareTo(cost) > 0){
					utility = cost;
					value = domain[i];
				}
			}
			
			return value;
		}
		
		@SuppressWarnings("unchecked")
		public U getBestLocalUtility(V di) {
			U utility = null;
			
			Collection<String> neighbors = problem.getNeighborVars(name);
			
			int conflicts = 0;
			
			AddableDelayed<U> util = problem.getZeroUtility().addDelayed();
			
			Map<String,V> assignments = new HashMap<String,V>();
			assignments.put(name, di);
			
			for (int j = 0; j < spaces.size(); j++) {
				UtilitySolutionSpace<V, U> space = spaces.get(j);
				
				for (int k = 0; k < space.getVariables().length; k++) {
					if (space.getVariable(k).equals(name))
						continue;
					
					for (String neighbor : neighbors) {
						if (space.getVariable(k).equals(neighbor)) {
							space = space.blindProject(neighbor, false);
							break;
						}
					}
				}
				
				String[] variables = space.getVariables();
				int size = variables.length;
				
				V[] values = (V[]) Array.newInstance(domain.getClass().getComponentType(), size);

				boolean complete = true;
				
				for (int k = 0; k < size; k++) {
					if (assignments.get(variables[k]) != null) {
						values[k] = assignments.get(variables[k]);
					} else {
						complete = false;
						break;
					}
				}
				
				if (!complete) 
					continue;

				U c = space.getUtility(variables, values);
				
				if (c == problem.getPlusInfUtility())
					conflicts++;

				util.addDelayed(c);
			}

			U cost = new AddableConflicts<U>(util.resolve(), conflicts).getUtility();
			
			if (utility == null || utility.compareTo(cost) > 0){
				utility = cost;
			}
			
			return utility;
		}
		
		public void updateLocalView(int cycle, String neighbor, V value, double phase) {
			solution.put(neighbor, value);
			phases.put(neighbor, phase);
			view.put(neighbor + "-" + cycle, new View<V>(neighbor, cycle, value, phase));
		}
		
		public void updateBestCycle(int cycle) {
			if (cycle != bestCycle) {
				bestValue = value;
				bestCycle = cycle;
			}
		}

		public void updateLocalCost(int cycle, String variable, U utility) {
			costs.put(variable + "-" + cycle, utility);
		}

		public Map<String, V> getAssigments(int cycle) {
			Map<String, V> assignments = new HashMap<String, V>();
			assignments.put(name, value);

			for (String neighbor : problem.getNeighborVars(name)) {
				V value = view.get(neighbor + "-" + cycle).getValue();
				if (value != null)
					assignments.put(neighbor, value);
			}

			return assignments;
		}

		public boolean isViewUpdated(int cycle) {
			for (String neighbor : problem.getNeighborVars(name)) {
				if (view.get(neighbor + "-" + cycle) == null) {
					return false;
				}
			}

			return true;
		}
		
		public boolean isLocalCostUpdated(int cycle) {
			for (String child : children) {
				if (costs.get(child + "-" + cycle) == null) {
					return false;
				}
			}

			return true;
		}

		public U getSubtreeCost() {
			Map<String, V> assignments = this.getAssigments(counter);
			U cost = this.calculateUtility(assignments);

			for (String variable : children) {
				cost = cost.add(costs.get(variable + "-" + counter));
			}
			
			return cost;
		}
		
		@SuppressWarnings("unchecked")
		public U fij(V di, String neighbor, V dj) {
			U cost = null;
			
			int conflicts = 0;

			AddableDelayed<U> util = problem.getZeroUtility().addDelayed();
			
			Map<String,V> assignments = new HashMap<String,V>();
			assignments.put(name, di);
			assignments.put(neighbor, dj);
			
			for (int j = 0; j < spaces.size(); j++) {
				UtilitySolutionSpace<V, U> space = spaces.get(j);

				String[] variables = space.getVariables();
				int size = variables.length;
				
				V[] values = (V[]) Array.newInstance(domain.getClass().getComponentType(), size);

				boolean complete = true;
				
				for (int k = 0; k < size; k++) {
					if (assignments.get(variables[k]) != null) {
						values[k] = assignments.get(variables[k]);
					} else {
						complete = false;
						break;
					}
				}
				
				if (!complete) continue;

				U c = space.getUtility(variables, values);

				if (c == problem.getPlusInfUtility()) {
					conflicts++;
				}

				util.addDelayed(c);
			}

			cost = new AddableConflicts<U>(util.resolve(), conflicts).getUtility();
			
			return cost;
		}

		@SuppressWarnings("unchecked")
		public U calculateUtility(Map<String, V> assignments) {
			int conflicts = 0;

			AddableDelayed<U> util = problem.getZeroUtility().addDelayed();

			for (int j = 0; j < spaces.size(); j++) {
				UtilitySolutionSpace<V, U> space = spaces.get(j);

				String[] variables = space.getVariables();
				int size = variables.length;

				V[] values = (V[]) Array.newInstance(domain.getClass().getComponentType(), size);

				for (int k = 0; k < size; k++) {
					values[k] = assignments.get(variables[k]);
				}

				U u = space.getUtility(variables, values);

				if (u == problem.getPlusInfUtility()) {
					conflicts++;
				}

				util.addDelayed(u);
			}

			return new AddableConflicts<U>(util.resolve(), conflicts).getUtility();
		}
	}
}
