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

package frodo2.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;

import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.MessageWith2Payloads;
import frodo2.communication.Queue;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.DCOPProblemInterface;

/** A StatsReporter that collects the generic solution to a DCOP
 * @author Thomas Leaute
 * @param <V> type used for variable values
 * @param <U> type used for utility values
 */
public class SolutionCollector< V extends Addable<V>, U extends Addable<U> > implements StatsReporter {
	
	/** The type of the messages containing assignments to variables */
	public static final MessageType ASSIGNMENTS_MSG_TYPE = new MessageType ("SolutionCollector", "VariableValueAssignments");

	/** A message holding assignments to variables
	 * @param <V> type used for variable values
	 */
	public static class AssignmentsMessage < V >
	extends MessageWith2Payloads< String[], ArrayList<V> > {
		
		/** Empty constructor used for externalization */
		public AssignmentsMessage () { }

		/** Constructor 
		 * @param var 		the variable(s)
		 * @param val 		the value(s) assigned to the variable(s) \a var
		 */
		public AssignmentsMessage (String[] var, ArrayList<V> val) {
			super (ASSIGNMENTS_MSG_TYPE, var, val);
		}
		
		/** @return the variable(s) */
		public String[] getVariables () {
			return this.getPayload1();
		}
		
		/** @return the value(s) */
		public ArrayList<V> getValues () {
			return this.getPayload2();
		}
		
		/** @see MessageWith2Payloads#toString() */
		@Override
		public String toString () {
			return "Message(type = `" + super.type + "')\n\tvariables: " + Arrays.asList(this.getVariables()) + "\n\tvalues: " + this.getValues();
		}
	}
	
	/** The type of the messages containing an assignment to a variable */
	public static final MessageType ASSIGNMENT_MSG_TYPE = new MessageType ("SolutionCollector", "VariableValueAssignment");

	/** A message holding an assignment to a variable
	 * @param <V> type used for variable values
	 */
	public static class AssignmentMessage < V extends Addable<V> >
	extends MessageWith2Payloads< String, V > {
		
		/** Empty constructor used for externalization */
		public AssignmentMessage () { }

		/** Constructor 
		 * @param var 		the variable
		 * @param val 		the value assigned to the variable \a var
		 */
		public AssignmentMessage (String var, V val) {
			super (ASSIGNMENT_MSG_TYPE, var, val);
		}
		
		/** @return the variable */
		public String getVariable () {
			return this.getPayload1();
		}
		
		/** @return the value */
		public V getValue () {
			return this.getPayload2();
		}
		
		/** @see MessageWith2Payloads#toString() */
		@Override
		public String toString () {
			return "Message(type = `" + super.type + "')\n\tvariable: " + this.getVariable() + "\n\tvalue: " + this.getValue();
		}
	}
	
	/** The queue form which this module collects solution messages */
	private Queue queue;
	
	/** Whether this module should print out its solution */
	private boolean silent;

	/** \c true if we are maximizing utility, \c false if we are minimizing cost */
	protected boolean maximize = true;

	/** The problem */
	private DCOPProblemInterface<V, U> problem;
	
	/** For each variable, its reported value */
	private HashMap<String, V> solution;
	
	/** @return for each variable, its assignment in the solution reported to the problem */
	public Map<String, V> getSolution () {
		return this.solution;
	}
	
	/** The true utility of the reported solution */
	private U utility;
	
	/** @return true utility of the reported solution */
	public U getUtility() {
		return utility;
	}

	/** The time at which the final solution was reported */
	private Long finalTime;

	/** @return The time at which the final solution was reported */
	public long getFinalTime() {
		return this.finalTime == null ? 0 : this.finalTime;
	}

	/** Constructor for the module added to the agents' queues
	 * @param problem 	problem instance
	 * @param params 	module parameters
	 * @note This is unused, since this module will not take part in the algorithm execution
	 */
	public SolutionCollector (DCOPProblemInterface<V, U> problem, Element params) {}

	/** Constructor in StatsReporter mode
	 * @param params 	module parameters
	 * @param problem 	problem instance
	 */
	public SolutionCollector (Element params, DCOPProblemInterface<V, U> problem) {
		this.silent = (params == null ? true : ! Boolean.parseBoolean(params.getAttributeValue("reportStats")));
		this.maximize = problem.maximize();
		this.solution = new HashMap<String, V> ();
		this.problem = problem;
	}

	/** @see StatsReporter#notifyIn(Message) */
	@Override
	public void notifyIn(Message msg) {
		
		MessageType type = msg.getType();

		if (type.equals(SolutionCollector.ASSIGNMENTS_MSG_TYPE)) {
			
			@SuppressWarnings("unchecked")
			AssignmentsMessage<V> msgCast = (AssignmentsMessage<V>) msg;
			String[] vars = msgCast.getVariables();
			ArrayList<V> vals = msgCast.getValues();
			for (int i = 0; i < vars.length; i++) {
				String var = vars[i];
				V val = vals.get(i);
				if (val != null && solution.put(var, val) == null && ! this.silent) 
					System.out.println("var `" + var + "' = " + val);
			}
		}

		else if (type.equals(SolutionCollector.ASSIGNMENT_MSG_TYPE)) {
			
			@SuppressWarnings("unchecked")
			AssignmentMessage<V> msgCast = (AssignmentMessage<V>) msg;
			String var = msgCast.getVariable();
			V val = msgCast.getValue();
			if (val != null && solution.put(var, val) == null && ! this.silent) 
				System.out.println("var `" + var + "' = " + val);
		}
		
		// When we have received all messages, print out the corresponding utility. 
		if (this.solution.keySet().containsAll(this.problem.getVariables())) {
			this.utility = this.problem.getUtility(this.solution, true).getUtility(0);
			if (! this.silent) 
				System.out.println("Total " + (this.problem.maximize() ? "utility" : "cost") + " of reported solution: " + this.utility);
		}

		// Record the timestamp for this type of solution message
		Long time = queue.getCurrentMessageWrapper().getTime();
		if (this.finalTime == null || this.finalTime < time) 
			this.finalTime = time;
	}

	/** @see StatsReporter#setQueue(Queue) */
	@Override
	public void setQueue(Queue queue) {}

	/** 
	 * @see StatsReporter#getMsgTypes() 
	 * @return an empty list, since this module does not participate in algorithm execution
	 */
	@Override
	public ArrayList<MessageType> getMsgTypes() {
		return new ArrayList<MessageType> ();
	}

	/** @see StatsReporter#getStatsFromQueue(Queue) */
	@Override
	public void getStatsFromQueue(Queue queue) {
		ArrayList <MessageType> msgTypes = new ArrayList <MessageType> (2);
		msgTypes.add(ASSIGNMENT_MSG_TYPE);
		msgTypes.add(ASSIGNMENTS_MSG_TYPE);
		queue.addIncomingMessagePolicy(msgTypes, this);
		this.queue = queue;
	}

	/** @see StatsReporter#setSilent(boolean) */
	@Override
	public void setSilent(boolean silent) {
		this.silent = silent;
	}

	/** @see StatsReporter#reset() */
	@Override
	public void reset() {
		this.finalTime = null;
		this.solution = new HashMap<String, V> ();
	}

}
