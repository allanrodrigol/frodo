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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;

import frodo2.algorithms.AgentInterface;
import frodo2.algorithms.duct.BOUNDmsg;
import frodo2.algorithms.duct.NORMmsg;
import frodo2.algorithms.duct.Normalize;
import frodo2.algorithms.duct.OUTmsg;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration.DFSview;
import frodo2.communication.Message;
import frodo2.communication.MessageType;
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
public class NormalizeInf <V extends Addable<V>> extends Normalize<V> {

	/**
	 * Constructor
	 * 
	 * @param problem		the local problem of the agent
	 * @param parameters	the parameters of the module
	 */
	public NormalizeInf(DCOPProblemInterface<V, AddableReal> problem, Element parameters) {
		super(problem, parameters);
	}
	
	/**
	 * Constructor used for debugging
	 * 
	 * @param problem		the local problem of the agent
	 * @param parameters	the parameters of the module
	 * @param reportSpaces  \c true when the spaces should be reported to the statsreporter, and \c false otherwise
	 */
	public NormalizeInf(DCOPProblemInterface<V, AddableReal> problem, Element parameters, boolean reportSpaces) {
		super(problem, parameters, reportSpaces);
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
				if(min != this.infeasibleUtil) {
					listMin.add(min);
					lower = lower == null ? min : lower.add(min);
				} else
					listMin.add(null); /// @todo If we are minimizing, then we have detected infeasibility; terminate early
				
				lowerBounds.put(var, lower);
				AddableReal upper = upperBounds.get(var);
				AddableReal max = space.blindProjectAll(true);
				if(max != this.infeasibleUtil) /// @todo Else, if we are maximizing, then we have detected infeasibility; terminate early
					upper = upper == null ? max : upper.add(max);
				assert upper != null; /// @bug Sometimes fails, most noticeably on locally infeasible problems (bug reported by Duc Thien Nguyen)
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
				sizes.put(var, (long)1);
				
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
					
					queue.sendMessageToSelf(new OUTmsg<V>(var, divide, list, separators.get(var), 1));
					if(this.reportSpaces)
						queue.sendMessage(AgentInterface.STATS_MONITOR, new OUTmsg<V>(var, divide, list, null, 1));
							
					
				} else {
					// start the normalization procedure
					queue.sendMessage(owners.get(parent), new BOUNDmsg(BOUND_MSG_TYPE, parent, lower, upper, numberOfSpaces[index], 1, separators.get(var)));
					
				}
				
				if (this.reportStats) // send a statistics message
					queue.sendMessage(AgentInterface.STATS_MONITOR, new BOUNDmsg(STATS_MSG_TYPE, null, lower, upper, 0, 1, separators.get(var)));
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
			
			long size = sizes.get(var) + msgCast.getSize();
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
			long size = sizes.get(var);
			queue.sendMessageToSelf(new OUTmsg<V>(var, divide, spaces, separators.get(var), size));
			
			if(this.reportSpaces)
				queue.sendMessage(AgentInterface.STATS_MONITOR, new OUTmsg<V>(var, divide, spaces, null, size));
		}

	}

	/** @see frodo2.algorithms.duct.Normalize#normalize(java.lang.String, int, frodo2.solutionSpaces.AddableReal, java.util.ArrayList) */
	@Override
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
}
