/*
FRODO: a FRamework for Open/Distributed Optimization
Copyright (C) 2008-2019  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek

FRODO is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

FRODO is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


How to contact the authors: 
<https://frodo-ai.tech>
*/

package frodo2.algorithms.duct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Element;

import frodo2.algorithms.AgentInterface;
import frodo2.algorithms.StatsReporter;
import frodo2.algorithms.duct.BOUNDmsg;
import frodo2.algorithms.duct.NORMmsg;
import frodo2.algorithms.duct.OUTmsg;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration.DFSview;
import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.Queue;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableReal;
import frodo2.solutionSpaces.DCOPProblemInterface;
import frodo2.solutionSpaces.UtilitySolutionSpace;
import frodo2.solutionSpaces.hypercube.Hypercube;

/**
 * This module is a preprocessing module that normalizes all utilities
 * to between 0 and 1.
 * 
 * It first does bound detection via a bottom up inference procedure,
 * after which a top-down normalization is performed
 * 
 * @author Brammert Ottens, 9 aug. 2011
 * @param <V> type used for domain values
 * 
 */
public class Normalize <V extends Addable<V>> implements StatsReporter {

	/** Type of the message used for bound detection */
	public static final MessageType BOUND_MSG_TYPE = new MessageType ("DUCT", "Normalize", "bounding");
	
	/** Type of the message used for normalization */
	public static final MessageType NORM_MSG_TYPE = new MessageType ("DUCT", "Normalize", "normalizing");
	
	/** Type of the output message of this module */
	public static final MessageType OUT_MSG_TYPE = new MessageType ("DUCT", "Normalize", "output");
	
	/** Type of the statistics message */
	public static final MessageType STATS_MSG_TYPE = new MessageType ("DUCT", "Normalize", "stats");
	
	/** The local problem */
	protected DCOPProblemInterface<V, AddableReal> problem;
	
	/** Maps variables to the agents that own them */
	protected Map<String, String> owners;
	
	/** \c true when the init() method has been called, and \c false otherwise */
	protected boolean started;
	
	/** The queue */
	protected Queue queue;
	
	/** Maps variable names to positions in an array */
	protected HashMap<String, Integer> variablePointer;
	
	/** For each variable, the number of children + 1 (the DFS message) */
	protected int[] numberOfMessagesToReceive;
	
	/** For each variable, the number of messages received */
	protected int[] numberOfMessagesReceived;
	
	/** For each variable the number of spaces it owns */
	protected int[] numberOfSpaces;
	
	/** For each variable, the parent */
	protected String[] parents;
	
	/** For each variable the children in the DFS tree */
	protected List<String>[] children;
	
	/** For each variable, the lower bound is stored here*/
	protected HashMap<String, AddableReal> lowerBounds;
	
	/** For each variable, the upper bound is stored here*/
	protected HashMap<String, AddableReal> upperBounds;
	
	/** The domain size of each variable */
	protected HashMap<String, Long> sizes;
	
	/** For each variable, its separator */
	protected HashMap<String, Set<String>> separators;
	
	/** For each variable, the spaces that it owns */
	protected HashMap<String, ArrayList<UtilitySolutionSpace<V, AddableReal>>> spaces;
	
	/** For each space the minimal value */
	protected HashMap<String, ArrayList<AddableReal>> minLists;
	
	/** When true, the spaces are reported, and false otherwise */
	protected final boolean reportSpaces;
	
	/** The value of an infeasibleUtility */
	protected AddableReal infeasibleUtil;
	
	/** The scaling factor*/
	protected AddableReal divide;
	
	/** The penalty used to replace an infeasible value */
	protected AddableReal penalty;

	/** Whether to report stats */
	protected boolean reportStats = true;
	
	/**
	 * Constructor
	 * 
	 * @param problem		the local problem of the agent
	 * @param parameters	the parameters of the module
	 * @todo check whether the utility class is actually AddableReal
	 */
	public Normalize(DCOPProblemInterface<V, AddableReal> problem, Element parameters) {
		this.problem = problem;
		reportSpaces = false;
		this.infeasibleUtil = problem.maximize() ? problem.getMinInfUtility() : problem.getPlusInfUtility();
		this.reportStats = Boolean.parseBoolean(parameters.getAttributeValue("reportStats"));
		
		String penalty = parameters.getAttributeValue("penalty");
		if(penalty != null)
			this.penalty = this.infeasibleUtil.fromString(penalty);
		else
			this.penalty = problem.maximize() ? new AddableReal(-1000) : new AddableReal(1000);
	}
	
	/**
	 * Constructor used for debugging
	 * 
	 * @param problem		the local problem of the agent
	 * @param parameters	the parameters of the module
	 * @param reportSpaces  \c true when the spaces should be reported to the statsreporter, and \c false otherwise
	 * @todo check whether the utility class is actually AddableReal
	 */
	public Normalize(DCOPProblemInterface<V, AddableReal> problem, Element parameters, boolean reportSpaces) {
		this.problem = problem;
		this.reportSpaces = reportSpaces;
		this.reportStats = Boolean.parseBoolean(parameters.getAttributeValue("reportStats"));
		this.infeasibleUtil = problem.maximize() ? problem.getMinInfUtility() : problem.getPlusInfUtility();
	}
	
	/** 
	 * @see frodo2.communication.IncomingMsgPolicyInterface#notifyIn(frodo2.communication.Message)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void notifyIn(Message msg) {
		
		MessageType type = msg.getType();
		
		if(!started)
			init();
		
		if(type.equals(DFSgeneration.OUTPUT_MSG_TYPE)) {
			// Retrieve the information from the message about children, pseudo-children... 
			DFSgeneration.MessageDFSoutput<V, AddableReal> msgCast = (DFSgeneration.MessageDFSoutput<V, AddableReal>) msg;
			String var = msgCast.getVar();
			int domainSize = problem.getDomainSize(var);
			DFSview<V, AddableReal> myRelationships = msgCast.getNeighbors();
			int index = variablePointer.get(var);

			if (myRelationships == null) // DFS reset message
				return;

			// Record its parent, if any
			parents[index] = myRelationships.getParent();

			// Record which constraints this variable is responsible for enforcing 
			List<String> children = myRelationships.getChildren();
			numberOfMessagesToReceive[index] = children.size() + 1;
			numberOfMessagesReceived[index] += 1;
			this.children[index] = children;
			HashSet<String> varsBelow = new HashSet<String> (children);
			varsBelow.addAll(myRelationships.getAllPseudoChildren());
			ArrayList<UtilitySolutionSpace<V, AddableReal>> list = new ArrayList<UtilitySolutionSpace<V, AddableReal>>();
			ArrayList<AddableReal> listMin = new ArrayList<AddableReal>();
			Set<String> separator = separators.get(var);
			separator.addAll(myRelationships.getPseudoParents());
			if(parents[index] != null)
				separator.add(parents[index]);
			for (UtilitySolutionSpace<V, AddableReal> space : this.problem.getSolutionSpaces(var, false, varsBelow)) { 
				list.add(space);
				AddableReal lower = lowerBounds.get(var);
				AddableReal min = space.blindProjectAll(false);
				if(min == this.infeasibleUtil)
					min = penalty;
				listMin.add(min);
				lower = lower == null ? min : lower.add(min);
				lowerBounds.put(var, lower);
				AddableReal upper = upperBounds.get(var);
				AddableReal max = space.blindProjectAll(true);
				if(max == this.infeasibleUtil)
					max = penalty;
				upper = upper == null ? max : upper.add(max);
				upperBounds.put(var, upper);
			}
			
			numberOfSpaces[index] += list.size();
			spaces.put(var, list);
			minLists.put(var, listMin);
			
			if(this.numberOfMessagesReceived[index] == this.numberOfMessagesToReceive[index]) {
				AddableReal lower = lowerBounds.get(var);
				AddableReal upper = upperBounds.get(var);
				String parent = parents[index];
				separators.get(var).remove(var);
				
				if(parent == null) {
					
					if(lower != null) {
						AddableReal divide = null;
						if(lower.equals(upper)) {
							divide = lower;
						} else {
							divide = upper.subtract(lower);
						}
						normalize(var, index, divide, list);
					}
					
					queue.sendMessageToSelf(new OUTmsg<V>(var, divide, list, separators.get(var), domainSize));
					if(this.reportSpaces)
						queue.sendMessage(AgentInterface.STATS_MONITOR, new OUTmsg<V>(var, divide, list, null, domainSize));
							
					
				} else {
					// start the normalization procedure
					queue.sendMessage(owners.get(parent), new BOUNDmsg(BOUND_MSG_TYPE, parent, lower, upper, numberOfSpaces[index], domainSize, separators.get(var)));
				}
				
				if (this.reportStats) // send a statistics message
					queue.sendMessage(AgentInterface.STATS_MONITOR, new BOUNDmsg(STATS_MSG_TYPE, null, lower, upper, 0, domainSize, separators.get(var)));
			}
		}
		
		else if (type.equals(BOUND_MSG_TYPE)) {
			BOUNDmsg msgCast = (BOUNDmsg)msg;
			String var = msgCast.getReceiver();
			int index = variablePointer.get(var);
			this.numberOfSpaces[index] += msgCast.getCounter();
			this.numberOfMessagesReceived[index]++;
			AddableReal lower = this.lowerBounds.get(var);
			lower = lower == null ? msgCast.getLowerBound() : lower.add(msgCast.getLowerBound());
			this.lowerBounds.put(var, lower);
			separators.get(var).addAll(msgCast.getSeperator());
			
			AddableReal upper = this.upperBounds.get(var);
			upper = upper == null ? msgCast.getUpperBound() : upper.add(msgCast.getUpperBound());
			this.upperBounds.put(var, upper);
			
			long size = Math.max(sizes.get(var), msgCast.getSize());
			this.sizes.put(var, size);
			
			if(this.numberOfMessagesReceived[index] == this.numberOfMessagesToReceive[index]) {
				size += 1;
				this.sizes.put(var, size);
				String parent = parents[index];
				separators.get(var).remove(var);
				for(String child : children[index])
					assert !separators.get(var).contains(child);
				if(parent == null) {
					ArrayList<UtilitySolutionSpace<V, AddableReal>> spaces = this.spaces.get(var);
					AddableReal divide = upper.subtract(lower);
					normalize(var, index, divide, spaces);
					queue.sendMessageToSelf(new OUTmsg<V>(var, divide, spaces, separators.get(var), size));
					if(this.reportSpaces)
						queue.sendMessage(AgentInterface.STATS_MONITOR, new OUTmsg<V>(var, divide, spaces, null, size));
				} else {
					queue.sendMessage(owners.get(parent), new BOUNDmsg(BOUND_MSG_TYPE, parent, lower, upper, numberOfSpaces[index], size, separators.get(var)));
				}
				
				if (this.reportStats) // send a statistics message
					queue.sendMessage(AgentInterface.STATS_MONITOR, new BOUNDmsg(STATS_MSG_TYPE, null, lower, upper, 0, size, null));
			}
		}
		
		else if (type.equals(NORM_MSG_TYPE)) {
			NORMmsg msgCast = (NORMmsg)msg;
			
			String var = msgCast.getReceiver();
			int index = variablePointer.get(var);
			ArrayList<UtilitySolutionSpace<V, AddableReal>> spaces = this.spaces.get(var);
			normalize(var, index, msgCast.getDivide(), spaces);
			separators.get(var).remove(var);
			long size = this.sizes.get(var);
			queue.sendMessageToSelf(new OUTmsg<V>(var, divide, spaces, separators.get(var), size));
			
			if(this.reportSpaces)
				queue.sendMessage(AgentInterface.STATS_MONITOR, new OUTmsg<V>(var, divide, spaces, null, size));
		}

	}
	
	/**
	 * Method to perform the normalization of the spaces owned by a variable
	 * 
	 * @author Brammert Ottens, 17 aug. 2011
	 * @param var		the variable who's spaces should be normalized
	 * @param index		the index of the variable
	 * @param divide 	the scaling
	 * @param spaces 	the spaces owned by \c var
	 */
	protected void normalize(String var, int index, AddableReal divide, ArrayList<UtilitySolutionSpace<V, AddableReal>> spaces) {
		ArrayList<AddableReal> mins = minLists.get(var);
		this.divide = divide;
		List<String> children = this.children[index];
		for(String child : children)
			queue.sendMessage(owners.get(child), new NORMmsg(child, null, divide));
				
		int j = 0;
		for(UtilitySolutionSpace<V, AddableReal> space : spaces) {
			AddableReal min = mins.get(j);
			if(space instanceof Hypercube) {
				for(int i = 0; i < space.getNumberOfSolutions(); i++) {
					if(space.getUtility(i) == this.infeasibleUtil)
						space.setUtility(i, (penalty.subtract(min)).divide(divide));
					else
						space.setUtility(i, (space.getUtility(i).subtract(min)).divide(divide));
				}
			} else {
				spaces.set(j, space.rescale(min.flipSign(), (new AddableReal(1)).divide(divide)));
			}
			j++;
		}
	}
	
	/**
	 * Initialization function, called when the first
	 * message is received
	 * 
	 * @author Brammert Ottens, 17 aug. 2011
	 */
	@SuppressWarnings("unchecked")
	protected void init() {
		started = true;
		
		owners = problem.getOwners();
		
		Set<String> variables = problem.getMyVars();
		
		int numberOfVariables = variables.size();
		variablePointer = new HashMap<String, Integer>();
		this.separators = new HashMap<String, Set<String>>();
		sizes = new HashMap<String, Long>(numberOfVariables);
		
		int i = 0;
		for(String variable : variables) {
			variablePointer.put(variable, i++);
			separators.put(variable, new HashSet<String>());
			sizes.put(variable, (long)problem.getDomainSize(variable));
			assert sizes.get(variable) > 0;
		}
		
		numberOfMessagesToReceive = new int[numberOfVariables];
		numberOfMessagesReceived = new int[numberOfVariables];
		numberOfSpaces = new int[numberOfVariables];
		parents = new String[numberOfVariables];
		children = new List[numberOfVariables];
		lowerBounds = new HashMap<String, AddableReal>(numberOfVariables);
		upperBounds = new HashMap<String, AddableReal>(numberOfVariables);
				
		
		spaces = new HashMap<String, ArrayList<UtilitySolutionSpace<V, AddableReal>>>(numberOfVariables);
		minLists = new HashMap<String, ArrayList<AddableReal>>(numberOfVariables);
	}

	/** 
	 * @see frodo2.communication.MessageListener#setQueue(frodo2.communication.Queue)
	 */
	@Override
	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	/** 
	 * @see frodo2.communication.MessageListener#getMsgTypes()
	 */
	@Override
	public Collection<MessageType> getMsgTypes() {
		ArrayList<MessageType> msgTypes = new ArrayList<MessageType>();
		msgTypes.add(AgentInterface.START_AGENT);
		msgTypes.add(BOUND_MSG_TYPE);
		msgTypes.add(NORM_MSG_TYPE);
		msgTypes.add(DFSgeneration.OUTPUT_MSG_TYPE);
		msgTypes.add(AgentInterface.AGENT_FINISHED);
		return msgTypes;
	}

	/** 
	 * @see frodo2.algorithms.StatsReporter#getStatsFromQueue(frodo2.communication.Queue)
	 */
	@Override
	public void getStatsFromQueue(Queue queue) {
		/// @todo Listen for and report on stats messages of type Normalize.STATS_MSG_TYPE

	}

	/** @see StatsReporter#setSilent(boolean) */
	@Override
	public void setSilent(boolean silent) {
		this.reportStats = ! silent;
	}

	/** 
	 * @see frodo2.algorithms.StatsReporter#reset()
	 */
	@Override
	public void reset() {
		// @todo Auto-generated method stub

	}

}
