package frodo2.algorithms.localSearch.gdba;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * @author allan
 * 
 * This class implements the Generalized Distributed Breakout Algorithm as described in
 * "Distributed breakout: Beyond satisfaction" by Okamoto et al, 2016. This algorithm is 
 * a generalization of Distributed Breakout Algorithm (DBA), a popular incomplete algorithm
 * for solving DisCSP. The DBA originally increases the weights of constraints whenever 
 * breakouts are detected. In contrast, GDBA suggests three design choices of replacing 
 * the variable assignments. First, modifying weights of constraints as in original
 * DBA, but in different manners (multiplicative or additive). Second, using definitions of
 * constraint violation (non-zero, non-minimum and maximum constraint violation). Third,
 * the scope of changes specifies which weights increase when breakouts are performed for a
 * violated constraint.
 * 
 * @param <V>	type used for variable values
 * @param <U> 	type used for utility values
 */
public class GDBA<V extends Addable<V>,U extends Addable<U>> implements StatsReporterWithConvergence<V> {
	/** Type of the message used for reporting to neighboring the new value assigned to a given variable */
	public static final MessageType VALUE_MSG_TYPE = new MessageType("Value");
	
	/** Type of the message used for reporting to neighboring the effective cost after collecting new assignments from neighbors */
	public static final MessageType DELTA_MSG_TYPE = new MessageType("Delta");
	
	/** Type of the message used for reporting to parent the cost of a given child variable */
	public static final MessageType COST_MSG_TYPE = new MessageType("Cost");
	
	/** Type of the message used for reporting the best cycle found so far to children variables */
	public static final MessageType BEST_CYCLE_MSG_TYPE = new MessageType("BestCycle");
	
	/** Type of the message containing the assignment history */
	public static final MessageType CONV_STATS_MSG_TYPE = new MessageType("DSADSP", "ConvStats");

	/** The message queue */
	private Queue queue;
	
	/** The agent's problem */
	private DCOPProblemInterface<V,U> problem;

	/** For each variable its assignment history */
	private HashMap<String, ArrayList<CurrentAssignment<V>>> assignmentHistoriesMap;

	/** The number of synchronous cycles before termination, except except for isolated variables */
	private int maxCycles;
	
	/** The number of variables that already finish the search process */
	private int variableFinishedCounter;
	
	/** 
	 * This parameters represent design alternatives used for calculated the effective cost of an variable
	 * M - Multiplicative manner
	 * A - Additive manner
	 */
	public static int MANNER_M = 0;
	public static int MANNER_A = 1;

	/** 
	 * This parameters represent design alternatives used when a constraint is violated
	 * NZ - Non-zero constraint violation
	 * NM - Non-minimum constraint violation
	 * MX - Maximum constraint violation
	 */
	public static int VIOLATION_NZ = 0;
	public static int VIOLATION_NM = 1;
	public static int VIOLATION_MX = 2;
	
	/** 
	 * This parameters represent design alternatives which determine the way (scope) for increasing the weight of a constraint  
	 * E - Entry scope
	 * C - Column scope
	 * R - Row scope
	 * T - Table scope
	 */
	public static int SCOPE_E = 0;
	public static int SCOPE_C = 1;
	public static int SCOPE_R = 2;
	public static int SCOPE_T = 3;
	
	/** 
	 * Setup of design alternatives for GDBA  
	 */
	public int manner;
	public int violation;
	public int scope;
	
	/** The map representing a container with information for each variable held by the agent */
	protected HashMap<String,VariableInfo> infos;

	/** \c true when the module has been finished, and \c false otherwise */
	protected boolean done;
	
	/** Whether the listener should record the assignment history or not */
	protected boolean convergence;
	
	/** Constructor for the stats gatherer mode
	 * @param parameters	the parameters of the module
	 * @param problem		the overall problem
	 */	
	public GDBA(Element params, DCOPProblemInterface<V, U> prob)  {
		problem = prob;
		convergence = false;
		
		infos = new HashMap<String,VariableInfo>();
	}
	
	/**
	 * Constructor for the agent module
	 * @param problem		description of the local problem
	 * @param parameters	parameters of the module
	 */	
	public GDBA(DCOPProblemInterface<V, U> problem, Element parameters) {
		this(parameters,problem);
		
		String maxCycles = parameters.getAttributeValue("maxCycles");
		
		String manner = parameters.getAttributeValue("manner");
		String violation = parameters.getAttributeValue("violation");
		String scope = parameters.getAttributeValue("scope");
		
		this.maxCycles = maxCycles == null ? 100 : Integer.parseInt(maxCycles);
		
		/* Suggested values by authors (see comments for more information) */
		this.manner = GDBA.MANNER_M;
		this.violation = GDBA.VIOLATION_NM;
		this.scope = GDBA.SCOPE_T;
		
		if (manner != null) {
			if (manner.equals("M")) {
				this.manner = MANNER_M;
			} else if (manner.equals("A")) {
				this.manner = MANNER_A;
			}
		}
		
		if (violation != null) {
			if (violation.equals("NZ")) {
				this.violation = VIOLATION_NZ;
			} else if (violation.equals("NM")) {
				this.violation = VIOLATION_NM;
			} else if (violation.equals("MX")) {
				this.violation = VIOLATION_MX;
			}
		}
		
		if (scope != null) {
			if (scope.equals("E")) {
				this.scope = SCOPE_E;
			} else if (scope.equals("C")) {
				this.scope = SCOPE_C;
			} else if (scope.equals("R")) {
				this.scope = SCOPE_R;
			} else if (scope.equals("T")) {
				this.scope = SCOPE_T;
			}
		}
		
		convergence = Boolean.parseBoolean(parameters.getAttributeValue("convergence"));
	}
	
	public void getStatsFromQueue(Queue queue) {
		queue.addIncomingMessagePolicy(CONV_STATS_MSG_TYPE, this);
	}

	public void setSilent(boolean silent) {
		/* Not implemented */
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

		types.add(DELTA_MSG_TYPE);
		types.add(VALUE_MSG_TYPE);
		types.add(COST_MSG_TYPE);
		types.add(BEST_CYCLE_MSG_TYPE);

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
			
			if (type.equals(VALUE_MSG_TYPE)) {
				ValueMessage<V> value = (ValueMessage<V>) message;

				for (String variable : problem.getMyVars()) {
					VariableInfo info = infos.get(variable);

					if (problem.getNeighborVars(variable).contains(value.getVariable())) {
						info.updateLocalView(value.getCycle(), value.getVariable(), value.getValue());

						this.computeDelta(variable, value.getCycle());
					}
				}
				
			} else if (type.equals(DELTA_MSG_TYPE)) {
				DeltaMessage<U> delta = (DeltaMessage<U>) message;
				
				for (String variable : problem.getMyVars()) {
					VariableInfo info = infos.get(variable);

					if (problem.getNeighborVars(variable).contains(delta.getVariable())) {
						info.updateDelta(delta.getCycle(), delta.getVariable(), delta.getDelta());

						this.computeLocalCost(variable, delta.getCycle());
					}
				}

			} else if (type.equals(BEST_CYCLE_MSG_TYPE)) {
				BestCycleMessage<V> best = (BestCycleMessage<V>) message;
				
				for (String variable : problem.getMyVars()) {
					VariableInfo info = infos.get(variable);

					if (info.parent != null && info.parent.equals(best.getVariable())) {
						info.updateBestCycle(best.getBestCycle());
					
						this.improveValue(variable);
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
		V[] domain = problem.getDomain(variable);
		V value = domain[(int) (Math.random() * domain.length)];

		this.assign(variable, value);
		this.sendAssignMessage(variable);
	}
	
	public boolean isReady() {
		for (String variable : problem.getMyVars()) {
			if (infos.get(variable) == null) {
				return false;
			}
		}

		return true;
	}	
	
	private void computeDelta(String variable, int cycle) {
		VariableInfo info = infos.get(variable);
		
		if (info.isViewUpdated(cycle)) {
			info.improve = null;
			
			U min = problem.getPlusInfUtility();
			
			for (int i = 0; i < info.domain.length; i++) {
				U sum = problem.getZeroUtility();
				V d = info.domain[i];
				
				for (String neighbor : problem.getNeighborVars(variable)) {
					V dj = info.view.get(neighbor + "-" + cycle).getValue();
					U u = this.effCost(variable, d, neighbor, dj);
					
					// 'sum' adds 'u'
					sum = sum.add(u);
				}
				
				if (sum.compareTo(min) < 0) {
					min = sum;
					info.improve = d;
				}
			}
			
			U delta = problem.getZeroUtility();
			
			V di = info.value;
			
			for (String neighbor : problem.getNeighborVars(variable)) {
				V dj = info.view.get(neighbor + "-" + cycle).getValue();
				
				U current = this.effCost(variable, di, neighbor, dj);
				U improve = this.effCost(variable, info.improve, neighbor, dj);
				
				// 'delta' receives the difference between 'current' and 'improve'
				delta = delta.add(current.subtract(improve));
			}
			
			info.delta = problem.getZeroUtility().fromString(String.valueOf(delta));
			
			this.sendDeltaMessage(variable, cycle);
		}
	}
	
	private U effCost(String variable, V di, String neighbor, V dj) {
		VariableInfo info = infos.get(variable);
		
		U[][] modifier = info.modifiers.get(neighbor); 
		
		int x = info.domain[0].intValue() == 0 ? 0 : 1;
		
		U fij = info.fij(di, neighbor, dj);
		U mij = modifier[di.intValue() - x][dj.intValue() - x];
		
		if (manner == MANNER_M) {
			// 'fij' = 'fij' ('mij' + 1) 
			fij = fij.multiply(mij.add(mij.fromInt(1)));
		} else if (manner == MANNER_A) {
			// 'fij' = 'fij' + 'mij'
			fij = fij.add(mij);
		}
		
		return fij;
	}
	
	private void increaseMod(String variable, V di, String neighbor, V dj) {
		VariableInfo info = infos.get(variable);
		
		U[][] modifier = info.modifiers.get(neighbor);
		
		if (scope == SCOPE_E) {
			int i = info.getDomainIndex(di);
			int j = info.getDomainIndex(dj);
			
			modifier[i][j].add(modifier[i][j].fromInt(1)); // 'modifier[i][j]' plus 1
		} else if (scope == SCOPE_C) {
			for (int i = 0; i < modifier.length; i++) {
				int j = info.getDomainIndex(dj);

				modifier[i][j].add(modifier[i][j].fromInt(1)); // 'modifier[i][j]' plus 1
			}
		} else if (scope == SCOPE_R) {
			for (int j = 0; j < modifier.length; j++) {
				int i = info.getDomainIndex(di);
				
				modifier[i][j].add(modifier[i][j].fromInt(1)); // 'modifier[i][j]' plus 1
			}
		} else if (scope == SCOPE_T) {
			for (int i = 0; i < modifier.length; i++) {
				for (int j = 0; j < modifier.length; j++) {
					modifier[i][j].add(modifier[i][j].fromInt(1)); // 'modifier[i][j]' plus 1
				}
			}
		}
	}
	
	private boolean isViolated(String variable, V di, String neighbor, V dj) {
		VariableInfo info = infos.get(variable);
		
		U c = this.effCost(variable, di, neighbor, dj);
		
		if (violation == VIOLATION_NZ) {
			// 'c' greater than zero utility
			return c.compareTo(problem.getZeroUtility()) > 0;
		} else if (violation == VIOLATION_NM) {
			U min = problem.getPlusInfUtility();
			
			for (int i = 0; i < info.domain.length; i++) {
				for (int j = 0; j < info.domain.length; j++) {
					U fij = info.fij(info.domain[i], neighbor, info.domain[j]);
					
					// 'fij' less than 'min'
					if (fij.compareTo(min) < 0) {
						min = fij;
					}
				}
			}
			
			// 'c' greater than 'min'
			return c.compareTo(min) > 0;
			
		} else if (violation == VIOLATION_MX) {
			U max = problem.getZeroUtility();
			
			for (int i = 0; i < info.domain.length; i++) {
				for (int j = 0; j < info.domain.length; j++) {
					U fij = info.fij(info.domain[i], neighbor, info.domain[j]);
					
					// 'fij' greater than 'max'
					if (fij.compareTo(max) > 0) {
						max = fij;
					}
				}
			}
			
			// 'c' equals to 'max'
			return c.compareTo(max) == 0;
		}
		
		return false;
	}
	
	public void assign(String variable, V value) {
		VariableInfo info = infos.get(variable);
		info.assign(value);

		this.sendStatsMessage(variable);
	}

	public void improveValue(String variable) {
		VariableInfo info = infos.get(variable);
		
		if (info.counter < maxCycles) {
			V best = info.value;
			
			if (info.delta.doubleValue() > 0) {
				boolean bestImprovement = true;
				
				for (String neighbor : problem.getNeighborVars(variable)) {
					Delta<U> dj = info.deltas.get(neighbor + "-" + info.counter);
					
					if (info.delta.doubleValue() < dj.getDelta().doubleValue()) {
						bestImprovement = false;
						break;
					}
				}
				
				if (bestImprovement) {
					best = info.improve;
				}
			} else {
				boolean neighborCanImprove = false;
				
				for (String neighbor : problem.getNeighborVars(variable)) {
					Delta<U> dj = info.deltas.get(neighbor + "-" + info.counter);
					
					if (dj.getDelta().doubleValue() > 0) {
						neighborCanImprove = true;
						break;
					}
				}
				
				if (!neighborCanImprove) {
					for (String neighbor : problem.getNeighborVars(variable)) {
						V di = info.value;
						V dj = info.view.get(neighbor + "-" + info.counter).getValue();
						
						if (this.isViolated(variable, di, neighbor, dj)) {
							this.increaseMod(variable, di, neighbor, dj);
						}
					}
				}
			}
			
			info.counter++;
			
			this.assign(variable, best);
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
			this.sendBestCycleMessage(variable);
		}
	}

	private void computeLocalCost(String variable, int cycle) {
		VariableInfo info = infos.get(variable);
		
		if (info.isDeltaUpdated(cycle) && info.isLocalCostUpdated(cycle)) {
			if (info.parent == null) { //root
				U cost = info.getSubtreeCost();
					
				if (info.bestCost.doubleValue() > cost.doubleValue()) {
					info.bestValue = info.value;
					info.bestCycle = info.counter;
					info.bestCost  = cost;
				}
				
				this.improveValue(variable);
			} else {
				this.sendCostMessage(variable, cycle);
			}
		}
	}
	
	private void sendAssignMessage(String variable) {
		VariableInfo info = infos.get(variable);
		ValueMessage<V> message = new ValueMessage<V>(info.counter, info.name, info.value);

		queue.sendMessageToMulti(this.getAgents(problem.getNeighborVars(variable)), message);
	}
	
	private void sendDeltaMessage(String variable, int cycle) {
		VariableInfo info = infos.get(variable);
		DeltaMessage<U> message = new DeltaMessage<U>(cycle, info.name, info.delta);
		
		queue.sendMessageToMulti(this.getAgents(problem.getNeighborVars(variable)), message);
	}
	
	private void sendBestCycleMessage(String variable) {
		VariableInfo info = infos.get(variable);
		BestCycleMessage<V> message = new BestCycleMessage<V>(info.name, info.bestCycle);

		queue.sendMessageToMulti(this.getAgents(info.children), message);
	}
	
	private void sendCostMessage(String variable, int cycle) {
		VariableInfo info = infos.get(variable);
		CostMessage<U> message = new CostMessage<U>(cycle, info.name, info.getSubtreeCost());

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
		public List<? extends UtilitySolutionSpace<V,U>> spaces;

		public Map<String, View<V>> view;
		public Map<String, U> costs;
		
		public Map<String,U[][]> modifiers;
		public Map<String,Delta<U>> deltas;

		public String parent;
		public Collection<String> children;

		public V value;
		public V[] domain;

		public V bestValue;
		public U bestCost;
		public int bestCycle;

		public int counter;
		
		public V improve;
		public U delta;
		
		public VariableInfo(String var) {
			name = var;
			counter = 1;

			bestCycle = 1;
			bestCost = problem.getPlusInfUtility();

			children = new ArrayList<String>();

			view = new HashMap<String,View<V>>();
			costs = new HashMap<String,U>();
			
			modifiers = new HashMap<String,U[][]>();
			deltas = new HashMap<String,Delta<U>>();

			domain = problem.getDomain(name);
			spaces = problem.getSolutionSpaces(name, false);
			
			this.createModifiers();
		}
		
		@SuppressWarnings("unchecked")
		private void createModifiers() {
			for (String neighbor : problem.getNeighborVars(name)) {
				U[][] modifier = (U[][]) Array.newInstance(problem.getZeroUtility().getClass(), domain.length, domain.length); 
						
				for (int i = 0; i < modifier.length; i++) {
					for (int j = 0; j < modifier.length; j++) {
						modifier[i][j] = problem.getZeroUtility();
					}
				}
				
				modifiers.put(neighbor, modifier);
			}
		}
		
		public int getDomainIndex(V d) {
			for (int i = 0; i < domain.length; i++) {
				if (d.compareTo(domain[i]) == 0) {
					return i;
				}
			}
			
			return -1;
			
			
		}

		public void assign(V v) {
			value = v;
			
			if (bestValue == null) bestValue = value;
		}

		public void updateLocalView(int cycle, String neighbor, V value) {
			view.put(neighbor + "-" + cycle, new View<V>(neighbor, cycle, value));
		}
		
		public void updateDelta(int cycle, String variable, U delta) {
			deltas.put(variable + "-" + cycle, new Delta<U>(variable, cycle, delta));
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
		
		public boolean isDeltaUpdated(int cycle) {
			for (String neighbor : problem.getNeighborVars(name)) {
				if (deltas.get(neighbor + "-" + cycle) == null) {
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

			U cost = new AddableConflicts<U>(util.resolve(), conflicts).getUtility();
			
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
