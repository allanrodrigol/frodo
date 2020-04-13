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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Element;

import frodo2.algorithms.AgentInterface;
import frodo2.algorithms.SolutionCollector;
import frodo2.algorithms.StatsReporter;
import frodo2.algorithms.duct.bound.Bound;
import frodo2.algorithms.duct.bound.BoundLog;
import frodo2.algorithms.duct.samplingMethods.SamplingM;
import frodo2.algorithms.duct.samplingMethods.SamplingProcedure;
import frodo2.algorithms.duct.termination.TerminateMean;
import frodo2.algorithms.duct.termination.TerminationCondition;
import frodo2.algorithms.duct.BoundStatsMsg;
import frodo2.algorithms.duct.COSTmsg;
import frodo2.algorithms.duct.Normalize;
import frodo2.algorithms.duct.OUTmsg;
import frodo2.algorithms.duct.SearchNode;
import frodo2.algorithms.duct.VALUEmsg;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration.DFSview;
import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.Queue;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableReal;
import frodo2.solutionSpaces.DCOPProblemInterface;
import frodo2.solutionSpaces.UtilitySolutionSpace;
import frodo2.solutionSpaces.UtilitySolutionSpace.ProjOutput;

/**
 * @author Brammert Ottens, 7 jul. 2011
 * 
 * @param <V> type used for domain values
 */
public class Sampling <V extends Addable<V>> implements StatsReporter {
	
	/** The type of a VALUE message */
	public final static MessageType VALUE_MSG_TYPE = new MessageType ("DUCT", "Sampling", "value");
	
	/** The type of a VALUE message */
	public final static MessageType VALUE_FIN_MSG_TYPE = new MessageType ("DUCT", "Sampling", "value_fin");
	
	/** The type of a COST message */
	public final static MessageType COST_MSG_TYPE = new MessageType ("DUCT", "Sampling", "cost");
	
	/** The type of the output message */
	public final static MessageType OUTPUT_MSG_TYPE = new MessageType ("DUCT", "Sampling", "Output");
	
	/** The type of the final bound statistics message */
	public final static MessageType BOUND_MSG_TYPE = new MessageType ("DUCT", "Sampling", "final_bound");
	
	/** When \c true, infeasible utilities should be ignored */
	protected final boolean IGNORE_INF;
	
	/** \c true when the start message has been received, and \c false otherwise */
	protected boolean started;
	
	/** \c true when maximizing, and \c false when minimizing */
	protected final boolean maximize;
	
	/** Whether to report stats */
	protected boolean reportStats = true;
	
	/** For each variable the available information */
	protected HashMap<String, VariableInfo> infos;
	
	/** The queue the listener is connected to */
	protected Queue queue;
	
	/** For each variable, the name of the owner */
	protected Map<String, String> owners;
	
	/** The agent's problem */
	protected DCOPProblemInterface<V, AddableReal> problem;
	
	/** The number of variables that have not yet finished */
	protected int numberOfActiveVariables;
	
	/** If the error bound is smaller than error, we stop */
	protected double error;
	
	/** The delta that is used to calculate the error bound*/
	protected double delta;
	
	/** Class of the sampling methods */
	protected Class<SamplingProcedure<V>> samplingClass;
	
	/** Class of the termination method */
	protected Class<TerminationCondition<V>> terminationClass;
	
	/** Class of the bound method */
	protected Class<Bound<V>> boundClass;
	
	/** Utility of an infeasible solution */
	protected AddableReal infeasibleUtility;
	
	/** penalty used to replace an infeasible solution */
	protected AddableReal penalty;
	
	/** The factor by which all utilities are scaled down */
	protected AddableReal scalingFactor;
	
	/** The final bound on the solution quality */
	protected AddableReal finalBound;
	
	/**
	 * Constructor for the stats reporter
	 * 
	 * @param parameters	listeners parameters (not used for the moment)
	 * @param problem		problem description
	 */
	public Sampling(Element parameters, DCOPProblemInterface<V, AddableReal> problem) {
		this.problem = problem;
		this.maximize = problem.maximize();
		this.IGNORE_INF = true;
	}
	
	/**
	 * Constructor
	 * 
	 * @param problem		problem description
	 * @param parameters	listeners parameters (not used for the moment)
	 */
	@SuppressWarnings("unchecked")
	public Sampling(DCOPProblemInterface<V, AddableReal> problem, Element parameters) {
		this.problem = problem;
		this.maximize = problem.maximize();
		this.infeasibleUtility = maximize ? problem.getMinInfUtility() : problem.getPlusInfUtility();

		if(VALUEmsg.DOMAIN_VALUE == null) {
			try {
				VALUEmsg.DOMAIN_VALUE = problem.getDomClass().getConstructor().newInstance().getZero();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
		String error = parameters.getAttributeValue("error");
		if(error != null)
			this.error = Double.parseDouble(error);
		else
			this.error = 0.1;
		
		String delta = parameters.getAttributeValue("delta");
		if(delta != null)
			this.delta = Double.parseDouble(delta);
		else
			this.delta = 0.1;
		
		String penalty = parameters.getAttributeValue("penalty");
		if(penalty != null)
			this.penalty = this.infeasibleUtility.fromString(penalty);
		else
			this.penalty = maximize ? new AddableReal(-1000) : new AddableReal(1000);
		
		this.IGNORE_INF = Boolean.parseBoolean(parameters.getAttributeValue("ignoreInf"));
			
		String samplingClass = parameters.getAttributeValue("samplingMethod");
		if(samplingClass == null || samplingClass.length() == 0)
			samplingClass = SamplingM.class.getName();
		
		String terminationClass = parameters.getAttributeValue("terminationCondition");
		if(terminationClass == null || terminationClass.length() == 0)
			terminationClass = TerminateMean.class.getName();
		
		String boundClass = parameters.getAttributeValue("bound");
		if(boundClass == null || boundClass.length() == 0)
			boundClass = BoundLog.class.getName();
		
		this.reportStats = Boolean.parseBoolean(parameters.getAttributeValue("reportStats"));
		
		try {
			this.samplingClass = (Class< SamplingProcedure<V> >) Class.forName(samplingClass);
			this.terminationClass = (Class< TerminationCondition<V> >) Class.forName(terminationClass);
			this.boundClass = (Class< Bound<V> >) Class.forName(boundClass);
		} catch (ClassNotFoundException e) {
			// @todo Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** 
	 * @see frodo2.communication.IncomingMsgPolicyInterface#notifyIn(frodo2.communication.Message)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void notifyIn(Message msg) {
		MessageType type = msg.getType();
		
		if (type.equals(BOUND_MSG_TYPE)) {
			BoundStatsMsg msgCast = (BoundStatsMsg)msg;
			finalBound = finalBound == null ? msgCast.getFinalBound() : finalBound.add(msgCast.getFinalBound());
			return;
		}
		
		if (!started)
			init();
		
		if (type.equals(VALUE_MSG_TYPE)) {
			VALUEmsg<V> msgCast = (VALUEmsg<V>)msg;
			VariableInfo varInfo = infos.get(msgCast.getReceiver());
			
			if(!varInfo.receivedNormalize) {
				varInfo.toBeProcessed.add(msgCast);
			} else {
				// update context
				varInfo.setContext(msgCast.getSender(), msgCast.getVariables(), msgCast.getValues());

				if(varInfo.leaf) {
					queue.sendMessage(owners.get(varInfo.parent), new COSTmsg(varInfo.parent, varInfo.solveLeaf()));
				} else {
					// sample the domain of the variable
					if(varInfo.sample())
						// report the value
						reportValue(varInfo, false);
					else
						queue.sendMessage(owners.get(varInfo.parent), new COSTmsg(varInfo.parent, varInfo.penalty));
				}
			}
		}
		
		if (type.equals(VALUE_FIN_MSG_TYPE)) {
			VALUEmsg<V> msgCast = (VALUEmsg<V>)msg;
			VariableInfo varInfo = infos.get(msgCast.getReceiver());

			// update context
			varInfo.parentFinished = true;
			
			varInfo.setContext(msgCast.getSender(), msgCast.getVariables(), msgCast.getValues());
			
			boolean finished = varInfo.leaf || varInfo.finishedSampling(error, delta);
			
			if(finished) {
				if(varInfo.leaf) {
					varInfo.solveLeaf();
					queue.sendMessage(AgentInterface.STATS_MONITOR, varInfo.getAssignmentMessage(varInfo.currentValue));
				} else {
					reportValue(varInfo, finished);
					queue.sendMessage(AgentInterface.STATS_MONITOR, varInfo.getAssignmentMessage(varInfo.currentValue));
				}
				if(--this.numberOfActiveVariables == 0)
					queue.sendMessageToSelf(new Message(AgentInterface.AGENT_FINISHED));
			} else {
				// sample the domain of the variable
				varInfo.sample();
				
				// report the value
				reportValue(varInfo, finished);
			}
		}
		
		else if (type.equals(COST_MSG_TYPE)) {
			COSTmsg msgCast = (COSTmsg)msg;
			VariableInfo varInfo = infos.get(msgCast.getReceiver());
			
			if(varInfo.storeCOSTmsg(msgCast)) { // all cost messages have been received
				if(varInfo.parentFinished) { // the parent has finished sampling, and found its optimal value
					boolean finished = varInfo.finishedSampling(error, delta);
					
					if(!finished) // if not converged, perform sampling
						varInfo.sample();
					else { // report optimal value to the stats reporter
						queue.sendMessage(AgentInterface.STATS_MONITOR, varInfo.getAssignmentMessage(varInfo.variableID, varInfo.currentValue));
						if(this.reportStats && varInfo.parent == null) // the root has finished
							queue.sendMessage(AgentInterface.STATS_MONITOR, new BoundStatsMsg(varInfo.getFinalBound()));
						if(--this.numberOfActiveVariables == 0)
							queue.sendMessageToSelf(new Message(AgentInterface.AGENT_FINISHED));
					}
					
					// report the new value to the children
					reportValue(varInfo, finished);
				} else {
					queue.sendMessage(owners.get(varInfo.getParent()), varInfo.getCostMessage());
				}
			}
			
		}
		
		else if (type.equals(Normalize.OUT_MSG_TYPE)) {
			OUTmsg<V> msgCast = (OUTmsg<V>)msg;
			String receiver = msgCast.getVariable();
			VariableInfo varInfo = infos.get(receiver);
			assert varInfo != null;
			varInfo.setSize(msgCast.getSize());
			varInfo.setSeparator(msgCast.getSeparators());
			scalingFactor = msgCast.getScalingFactor();
			varInfo.penalty = scalingFactor == null ? null : penalty.divide(scalingFactor);
			varInfo.receivedNormalize = true;
			for(UtilitySolutionSpace<V, AddableReal> space : msgCast.getSpaces())
				varInfo.storeConstraint(space);
					
			if(varInfo.parent == null) {
				if(varInfo.leaf) {
					queue.sendMessage(AgentInterface.STATS_MONITOR, varInfo.getAssignmentMessage(receiver, varInfo.solveSingleton(maximize)));

					if(--this.numberOfActiveVariables == 0) {
						queue.sendMessageToSelf(new Message(AgentInterface.AGENT_FINISHED));
					}
				} else {
					varInfo.setContext(receiver, new String[0], (V[])new Addable[0]);
					varInfo.sample();
					
					reportValue(varInfo, false);
				}
			} else if (!varInfo.toBeProcessed.isEmpty()) {
				assert varInfo.toBeProcessed.size() == 1;
				VALUEmsg<V> msgV = varInfo.toBeProcessed.get(0);
				// update context
				varInfo.setContext(msgV.getSender(), msgV.getVariables(), msgV.getValues());

				if(varInfo.leaf) {
					queue.sendMessage(owners.get(varInfo.parent), new COSTmsg(varInfo.parent, varInfo.solveLeaf()));
				} else {
					// sample the domain of the variable
					varInfo.sample();

					// report the value
					reportValue(varInfo, false);
				}
			}
			
		}
		
		else if (type.equals(DFSgeneration.OUTPUT_MSG_TYPE)) {
			DFSgeneration.MessageDFSoutput<V, AddableReal> msgCast = (DFSgeneration.MessageDFSoutput<V, AddableReal>) msg;

			String var = msgCast.getVar();
			DFSview<V, AddableReal> neighbours = msgCast.getNeighbors();
			
			// get the lower neighbours
			List<String> children = neighbours.getChildren();
			
			// get the parent
			String parent = neighbours.getParent();
			
			infos.put(var, newVariableInfo(var, problem.getDomain(var), parent, children));
		}

	}
	
	/**
	 * 
	 * @author Brammert Ottens, 16 sep. 2011
	 * @param variableID		the name of the variable
	 * @param domain			the domain of the variable
	 * @param parent			the parent of the variable
	 * @param children			list of the children of the variable
	 * @return a VariableInfo object
	 */
	protected VariableInfo newVariableInfo(String variableID, V[] domain, String parent, List<String> children) {
		return new VariableInfo(variableID, domain, parent, children, samplingClass, terminationClass);
	}
	
	/**
	 * Method used to report the value to all children and
	 * pseudo children
	 * 
	 * @author Brammert Ottens, 7 jul. 2011
	 * @param varInfo	the variable information object
	 * @param finished  \c true when the variable is ready with sampling, and false otherwise
	 */
	protected void reportValue(VariableInfo varInfo, boolean finished) {
		// report value to children
		for(String child : varInfo.getChildren())
			queue.sendMessage(owners.get(child), varInfo.getNewValueMessage(child, finished));
	}
	
	/**
	 * @author Brammert Ottens, Dec 29, 2011
	 * @return the final bound on the solution quality
	 */
	public AddableReal getFinalBound() {
		return this.finalBound;
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
		ArrayList<MessageType> msgTypes = new ArrayList<MessageType>(7);
		msgTypes.add(AgentInterface.START_AGENT);
		msgTypes.add(DFSgeneration.OUTPUT_MSG_TYPE);
		msgTypes.add(Normalize.OUT_MSG_TYPE);
		msgTypes.add(COST_MSG_TYPE);
		msgTypes.add(VALUE_MSG_TYPE);
		msgTypes.add(VALUE_FIN_MSG_TYPE);
		msgTypes.add(AgentInterface.AGENT_FINISHED);
		return msgTypes;
	}

	/** 
	 * @see frodo2.algorithms.StatsReporter#getStatsFromQueue(frodo2.communication.Queue)
	 */
	@Override
	public void getStatsFromQueue(Queue queue) {
		queue.addIncomingMessagePolicy(BOUND_MSG_TYPE, this);
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
	
	/**
	 * Initialization method called when the start message is received.
	 * 
	 * @author Brammert Ottens, 7 jul. 2011
	 */
	protected void init() {
		this.started = true;
		owners = problem.getOwners();
		
		infos = new HashMap<String, VariableInfo>();
		this.numberOfActiveVariables = problem.getNbrIntVars();
	}

	/**
	 * Convenience class to hold variable information
	 * 
	 * @author Brammert Ottens, 7 jul. 2011
	 *
	 */
	protected class VariableInfo {
		
		// Variable information
		
		/** The name of the variable */
		protected String variableID;
		
		/** The domain of the variable */
		protected V[] domain;
		
		/** The size of the domain */
		protected int domainSize;
		
		/** The parent of the variable */
		protected String parent;
		
		/** \c true when the variable is a leaf node, and \c false otherwise */
		protected final boolean leaf;
		
		/** \c true when the parent finished sampling */
		protected boolean parentFinished;
		
		/** The list of children */
		private List<String> children;
		
		/** The number of children of this variable */
		protected int nbrChildren;
		
		/** The maximal number of nodes below any node of this variable */
		protected long size;
		
		// Context information
		
		/** The size of the context */
		protected int contextSize;
		
		/** The current value assignment of all agents in the separator */
		protected V[] context;
		
		/** For each context variable, the position in the context array */
		protected HashMap<String, Integer> contextPointer;
		
		/** The context variables */
		protected String[] contextVariables;
		
		/** The currently sampled value */
		protected V currentValue;
		
		/** The index of the current value */
		protected int currentValueIndex;
		
		/** The local problem of the variable */
		protected UtilitySolutionSpace<V, AddableReal> space;
		
		// Sampling
		
		/** The sampler used */
		protected SamplingProcedure<V> sampler;
		
		/** The termination condition used */
		private TerminationCondition<V> termination;
		
		/** The bound used for sampling */
		private Bound<V> bound;
		
		/** The distribution corresponding to the current context */
		protected SearchNode<V> node;
		
		/** For each possible context value seen, the collected information */
		protected HashMap<State<V>, SearchNode<V>> distributions;
		
		/** sum of all samples reported by the children for the current context */
		protected AddableReal reportedSample;
		
		// Other		
		
		/** The next cost message to be sent */
		protected Message nextCostMsg;
		
		/** The number of COST messages received as a reply on the last VALUE message*/
		protected int costMessagesReceived;
		
		/** List of to be processed value messages */
		protected ArrayList<VALUEmsg<V>> toBeProcessed;
		
		/** penalty used to replace an infeasible solution */
		protected AddableReal penalty;
		
		/** \c true when normalization is finished for this variable, and \c false otherwise */
		protected boolean receivedNormalize;

		
		/**
		 * Constructor
		 * 
		 * @param variableID		the name of the variable
		 * @param domain			the domain of the variable
		 * @param parent			the parent of the variable
		 * @param children			list of the children of the variable
		 * @param samplingClass		the class of the sampling method
		 * @param terminationClass 	the class of the termination condition
		 */
		public VariableInfo(String variableID, V[] domain, String parent, List<String> children, Class <SamplingProcedure<V>> samplingClass, Class<TerminationCondition<V>> terminationClass) {
			this.variableID = variableID;
			this.domain = domain;
			this.domainSize = domain.length;
			this.parent = parent;
			this.parentFinished = parent == null;
			this.children = children;
			this.nbrChildren = children.size();
			this.leaf = nbrChildren == 0;
			this.distributions = new HashMap<State<V>, SearchNode<V>>();
			this.toBeProcessed = new ArrayList<VALUEmsg<V>>();
			
			try {
				Class<?> parTypes[] = new Class[0];
				Constructor< SamplingProcedure<V> > constructorS = samplingClass.getConstructor(parTypes);
				Constructor< TerminationCondition<V> > constructorT = terminationClass.getConstructor(parTypes);
				Object[] args = new Object[0];
				this.sampler = constructorS.newInstance(args);
				this.termination = constructorT.newInstance(args);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		
		/**
		 * Method to create search node
		 * 
		 * @author Brammert Ottens, 4 nov. 2011
		 * @param domainSize	the size of the domain
		 * @param maximize		\c true when maximizing, and \c false otherwise
		 * @param IGNORE_INF	\c true when infeasible utilities are to be ignored, and \c false otherwise
		 * @return	a new search node
		 */
		protected SearchNode<V> createNode(int domainSize, boolean maximize, boolean IGNORE_INF) {
			return new SearchNode<V>(domainSize, maximize, IGNORE_INF);
		}

		/**
		 * 
		 * @author Brammert Ottens, 7 jul. 2011
		 * @param msg the message to be stored
		 * @return	\c true when enough cost messages have been received, \c false otherwise
		 */
		public boolean storeCOSTmsg(COSTmsg msg) {
			
			this.reportedSample = reportedSample == null ? msg.getCost() : reportedSample.add(msg.getCost());
			
			// store the COST message
			
			if(++this.costMessagesReceived == nbrChildren) {
				AddableReal cost = node.storeCost(this.currentValueIndex, reportedSample, infeasibleUtility, maximize);
				if(!node.random)
					sampler.processSample(node, infeasibleUtility, maximize);
				this.reportedSample = null;
				this.costMessagesReceived = 0;
				nextCostMsg = new COSTmsg(parent, cost == null ? penalty : cost);
				return true;
			}
			
			return false;
		}

		/**
		 * @author Brammert Ottens, 11 nov. 2011
		 * @param size	the maximal number of nodes below any node of this variable
		 */
		public void setSize(long size) {
			this.size = size;
			try {
				Class<?> parTypes[] = new Class[1];
				parTypes[0] = Long.class;
				Constructor< Bound<V> > constructorB = boundClass.getConstructor(parTypes);
				Object[] args = new Object[1];
				args[0] = size;
				this.bound = constructorB.newInstance(args);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		
		/**
		 * @author Brammert Ottens, 25 aug. 2011
		 * @param separator the separator of the variable
		 */
		@SuppressWarnings("unchecked")
		public void setSeparator(Set<String> separator) {
			this.contextSize = separator.size() + 1;
			this.contextVariables = new String[contextSize];
			this.context = (V[])Array.newInstance(domain[0].getClass(), contextSize);
			
			contextPointer = new HashMap<String, Integer>();
			int i = 0;
			for(String var : separator) {
				contextPointer.put(var, i);
				contextVariables[i] = var;
				i++;
			}
			
			contextVariables[i] = variableID;	
		}
		
		/**
		 * Solve the local problem of a singleton variable
		 * 
		 * @author Brammert Ottens, 24 aug. 2011
		 * @param maximize \c true when maximizing, and \c false otherwise
		 * @return the optimal value assignment
		 */
		public V solveSingleton(boolean maximize) {
			assert parent == null;
			assert children.size() == 0;
			
			if(space == null)
				return this.domain[0];
			ProjOutput<V, AddableReal> sol = space.projectAll(maximize);
			return sol.getAssignments().getUtility(0).get(0);
		}
		
		/**
		 * @author Brammert Ottens, 7 jul. 2011
		 * @param error the intended error
		 * @param delta the delta that determines the probability
		 * @return \c true when enough samples have been received, \c false otherwise 
		 */
		public boolean finishedSampling(double error, double delta) {
			if(!node.random && termination.converged(node, error, delta, maximize)) {
				if(node.maxValueIndex != -1) {
					this.context[context.length - 1] = domain[node.maxValueIndex];
					this.currentValue = domain[node.maxValueIndex];
				}
				return true;
			}
			
			return false;
		}
		
		/**
		 * Method used to sample the values of the variable. Based on the context, and
		 * a give probability distribution this method chooses a value for \c variablID
		 * and stores it in \c context
		 * @author Brammert Ottens, 7 jul. 2011
		 * @return \c true when the current local problem is feasible, \c false otherwise 
		 */
		public boolean sample() {
			if(node.random)
				currentValueIndex = node.solveLocalProblem(space, infeasibleUtility, contextVariables, context, domain);
			else
				currentValueIndex = sampler.sampling(node);
			
			if(currentValueIndex == -1)
				return false;
			
			node.visited(bound);
			currentValue = this.domain[currentValueIndex];
			context[context.length - 1] = currentValue;
			return true;
			
		}
		
		/**
		 * Method used to update the context after a VALUE message has been received
		 * 
		 * @author Brammert Ottens, 7 jul. 2011
		 * @param sender	the sender of the VALUE message
		 * @param variables the reported variables
		 * @param values	the context as reported by the parent
		 * @return \c always returns true
		 */
		public boolean setContext(String sender, String[] variables, V[] values) {
			for(int i = 0; i < values.length; i++) {
				String var = variables[i];
				if(contextPointer.containsKey(var))
					this.context[contextPointer.get(var)] = values[i];
			}
			
			if(!leaf) {
				V ownValue = context[context.length - 1];
				context[context.length - 1] = null;
				node = distributions.get(new State<V>(context));
				if(node == null) {
					SearchNode<V> d = createNode(domainSize, maximize, IGNORE_INF);
					d.initSampling(space, infeasibleUtility, contextVariables, context, domain);
					node = d;
					distributions.put(new State<V>(context.clone()), d);
				}
				context[context.length - 1] = ownValue;
			}
			
			return true;
		}
		
		/**
		 * Solve the local problem of a leaf, given the context
		 * @author Brammert Ottens, 29 aug. 2011
		 * @return the maximal utility obtainable given the context
		 */
		public AddableReal solveLeaf() {
			assert leaf;
			
			AddableReal max = null;
			int index = 0;
			
			for(int i = 0; i < domainSize; i++) {
				context[contextSize - 1] = domain[i];
				AddableReal util = space.getUtility(contextVariables, context);
				if(max == null || (maximize ? max.compareTo(util) < 0 : max.compareTo(util) > 0)) {
					max = util;
					index = i;
				}
			}
			this.currentValueIndex = index;
			this.currentValue = domain[index];
			this.context[contextSize - 1] = currentValue;
			
			if(max == infeasibleUtility)
				return penalty;
			
			return max;
		}
		
		/**
		 * @author Brammert Ottens, 7 jul. 2011
		 * @param child 	the child for which the VALUE message is meant
		 * @param finished 	\c true when the variable is ready with sampling, and \c false otherwise
		 * @return	a value message containing the newly chosen value;
		 */
		public VALUEmsg<V> getNewValueMessage(String child, boolean finished) {
			if(finished) {
				assert this.parentFinished;
				return new VALUEmsg<V>(VALUE_FIN_MSG_TYPE, variableID, child, this.contextVariables, this.context);
			} else
				return new VALUEmsg<V>(VALUE_MSG_TYPE, variableID, child, this.contextVariables, this.context);
		}
		
		/**
		 * @author Brammert Ottens, 7 jul. 2011
		 * @return a COST message to be reported to the parent
		 */
		public Message getCostMessage() {
			
			return this.nextCostMsg;
		}
		
		/**
		 * @author Brammert Ottens, 7 jul. 2011
		 * @return the parent of this variable
		 */
		public String getParent() {
			return this.parent;
		}
		
		/**
		 * Makes this variable responsible for this constraint
		 * 
		 * @param space the constraint
		 */
		public void storeConstraint(UtilitySolutionSpace<V, AddableReal> space) {
			
			if (this.space == null) 
				this.space = space;
			else 
				this.space = this.space.join(space);
		}
		
		/**
		 * @author Brammert Ottens, 7 jul. 2011
		 * @return a list containing the children of this variable
		 */
		public List<String> getChildren() {
			return children;
		}
		
		/**
		 * @author Brammert Ottens, 18 okt. 2011
		 * @param value the value of the assignment
		 * @return an assignment message to report value \c value
		 */
		public SolutionCollector.AssignmentMessage<V> getAssignmentMessage(V value) {
			return new SolutionCollector.AssignmentMessage<V>(variableID, value);
		}
		
		/**
		 * @author Brammert Ottens, 18 okt. 2011
		 * @param var	the variable for which the assignment is reported
		 * @param value the value of the assignment
		 * @return an assignment message to report value \c value
		 */
		public SolutionCollector.AssignmentMessage<V> getAssignmentMessage(String var, V value) {
			return new SolutionCollector.AssignmentMessage<V>(var, value);
		}
		
		/** @return the final bound */
		public AddableReal getFinalBound() {
			return scalingFactor.multiply(new AddableReal(node.convergenceBound(node.maxValueIndex, delta)));
		}
		
		/** @see java.lang.Object#toString() */
		@Override
		public String toString() {
			String str = "-------" + variableID + "---------\n";
			str += "Parent = " + this.parent;
			str += "\n[";
			int i = 0;
			for(; i < contextSize - 1; i++)
				str += this.contextVariables[i] + "=" + this.context[i] + ", ";
			str += this.contextVariables[i] + "=" + this.context[i] + "]\n";
			if(node != null) {
				str += node.toString(delta);
				str += "max value = " + node.maxValueIndex + "\n";
				str += "bound = " + node.maxBound + "\n";
				str += "sampling bound = " + node.convergenceBound(node.maxValueIndex, delta);
			}
			
			return str;
		}
		
	}
	
	/**
	 * Convenience class to store a context
	 * 
	 * @author Brammert Ottens, 29 aug. 2011
	 * 
	 * @param <V> type used for domain values
	 */
	protected static class State <V extends Addable<V>> {
		
		/** The values in the assignment */
		V[] values;
		
		/** The hashCode of this key */
		final int hashCode;
		
		/**
		 * Constructor
		 * 
		 * @param values the value that constitute this key
		 */
		public State(V[] values) {
			this.values = values;
			StringBuffer b = new StringBuffer();
			if(values.length > 0) {
				int i = 0;
				for(; i < values.length - 2; i++) {
					b.append(values[i]);
					b.append(" ");
				}
				b.append(values[i]);
			}
			
			hashCode = b.toString().hashCode();
		}
		
		/** @see java.lang.Object#equals(java.lang.Object) */
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object o) {
			State<V> k = (State<V>)o;
			
			if(values.length == k.values.length) {
				for(int i = 0; i < values.length - 1; i++)
					if(!values[i].equals(k.values[i]))
						return false;
				return true;
			}
			
			return false;
		}
		
		/** @see java.lang.Object#hashCode() */
		@Override
		public int hashCode() {
			return hashCode;
		}
		
		/** @see java.lang.Object#toString() */
		@Override
		public String toString() {
			String str = "key (";
			str += this.hashCode + " [";
			int i = 0;
			for(;i < this.values.length-1; i++)
				str += values[i] + ", ";
			if(i < this.values.length)
				str += values[i] + "]\n";
			else
				str += "]\n";
			
			return str;
		}
	}
	
}
