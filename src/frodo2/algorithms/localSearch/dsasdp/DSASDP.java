package frodo2.algorithms.localSearch.dsasdp;

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
 * This class implements the DSA Slope Dependent Probability (DSA-SDP) as described in
 * "Explorative anytime local search for distributed constraint optimization" by Zivan et al, 2014.
 * 
 * This algorithm is a DSA version that uses a generic framework that ensures an anytime property 
 * on local search algorithms; that is, the algorithm always return the best global solution explored 
 * when it ends up. This framework, called Anytime Local Search DCOP (ALS DCOP), provides a 
 * communication model for anytime local search from a spanning tree on the constraint graph. Authors 
 * demonstrated the effectiveness of this general framework in a new algorithm, named DSA-SDP,
 * which employs a new heuristic for calculating the probability of replacing the assignment.
 * 
 * @param <V>	type used for variable values
 * @param <U> 	type used for utility values
 */
public class DSASDP<V extends Addable<V>, U extends Addable<U>> implements StatsReporterWithConvergence<V> {
	/** Type of the message used for reporting to neighboring the new value assigned to a given variable */
	public static final MessageType VALUE_MSG_TYPE = new MessageType("Value");
	
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
	
	/** The pA, pB, pC, and pD represent the potential of improvement for calculating the probability of choosing new assignments */
	private double pA;
	private double pB;
	private double pC;
	private double pD;
	
	/** The map representing a container with information for each variable held by the agent */
	protected HashMap<String, VariableInfo> infos;

	/** \c true when the module has been finished, and \c false otherwise */
	protected boolean done;
	
	/** Whether the listener should record the assignment history or not */
	protected boolean convergence;

	/** Constructor for the stats gatherer mode
	 * @param parameters	the parameters of the module
	 * @param problem		the overall problem
	 */
	public DSASDP(Element params, DCOPProblemInterface<V, U> prob) {
		problem = prob;
		convergence = false;
		
		infos = new HashMap<String, VariableInfo>();
	}

	/**
	 * Constructor for the agent module
	 * @param problem		description of the local problem
	 * @param parameters	parameters of the module
	 */	
	public DSASDP(DCOPProblemInterface<V, U> problem, Element parameters) {
		this(parameters, problem);
		
		String maxCycles = parameters.getAttributeValue("maxCycles");
		
		String pA = parameters.getAttributeValue("pA");
		String pB = parameters.getAttributeValue("pB");
		String pC = parameters.getAttributeValue("pC");
		String pD = parameters.getAttributeValue("pD");
		
		this.maxCycles = maxCycles == null ? 100 : Integer.parseInt(maxCycles);
		
		/* Suggested values by authors (see comments for more information) */
		this.pA = pA == null ? 0.6 : Double.parseDouble(pA);
		this.pB = pB == null ? 0.15 : Double.parseDouble(pB);
		this.pC = pC == null ? 0.4 : Double.parseDouble(pC);
		this.pD = pD == null ? 0.8 : Double.parseDouble(pD);
		
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
	
						this.computeLocalCost(variable, value.getCycle());
					}
				}
	
			} else if (type.equals(BEST_CYCLE_MSG_TYPE)) {
				BestCycleMessage<V> best = (BestCycleMessage<V>) message;
	
				for (String variable : problem.getMyVars()) {
					VariableInfo info = infos.get(variable);
	
					if (info.parent != null && info.parent.equals(best.getVariable())) {
						info.updateBestCycle(best.getBestCycle());
	
						this.chooseValue(variable);
					}
				}
	
			} else if (type.equals(DSASDP.COST_MSG_TYPE)) {
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

	public void initVar(String variable) {
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

	public void assign(String variable, V value) {
		VariableInfo info = infos.get(variable);
		info.assign(value);

		this.sendStatsMessage(variable);
	}

	public void chooseValue(String variable) {
		VariableInfo info = infos.get(variable);

		if (info.counter < maxCycles) {
			Map<String, V> assignments = info.getAssigments(info.counter);

			U cost = null;
			V best = null;

			for (int i = 0; i < info.domain.length; i++) {
				assignments.put(variable, info.domain[i]);

				U util = info.calculateUtility(assignments);

				if (cost == null || cost.doubleValue() > util.doubleValue()) {
					best = info.domain[i];
					cost = util;
				}
			}

			double prob = 0;
			double fact = (Math.abs(info.bestCost.doubleValue() - cost.doubleValue()) / cost.doubleValue());

			if (cost.doubleValue() < info.bestCost.doubleValue()) { // calculates probability with PA and PB
				prob = pA + Math.min(pB, fact);
			} else { // calculates probability with PC and PD
				if (fact <= 1) {
					prob = Math.max(pC, pD - fact);
				}
			}

			if (Math.random() > prob) {
				// restore last assignment
				best = info.value;
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

		if (!info.children.isEmpty()) {
			this.sendBestCycleMessage(variable);
		}
	}

	public void computeLocalCost(String variable, int cycle) {
		VariableInfo info = infos.get(variable);

		if (info.isViewUpdated(cycle) && info.isLocalCostUpdated(cycle)) {
			if (info.parent == null) { // root
				U cost = info.getSubtreeCost();

				if (info.bestCost.doubleValue() > cost.doubleValue()) {
					info.bestCost = cost;

					info.bestCycle = info.counter;
					info.bestValue = info.value;
				}

				this.chooseValue(variable);
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

	private Collection<String> getAgents(Collection<String> variables) {
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

		public Map<String, View<V>> view;
		public Map<String, U> costs;

		public String parent;
		public Collection<String> children;

		public V value;
		public V[] domain;

		public V bestValue;
		public U bestCost;
		public int bestCycle;

		public int counter;

		public VariableInfo(String var) {
			name = var;
			counter = 1;

			bestCycle = 1;
			bestCost = problem.getPlusInfUtility();

			children = new ArrayList<String>();

			view = new HashMap<String, View<V>>();
			costs = new HashMap<String, U>();

			domain = problem.getDomain(name);
			spaces = problem.getSolutionSpaces(name, false);
		}

		public void assign(V v) {
			value = v;
			if (bestValue == null)
				bestValue = value;
		}

		public void updateLocalView(int cycle, String neighbor, V value) {
			view.put(neighbor + "-" + cycle, new View<V>(neighbor, cycle, value));
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
