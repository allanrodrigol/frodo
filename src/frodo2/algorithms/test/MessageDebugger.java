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

package frodo2.algorithms.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import org.jdom2.Element;

import frodo2.algorithms.AgentInterface;
import frodo2.algorithms.Eavesdroppable;
import frodo2.communication.IncomingMsgPolicyInterface;
import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.OutgoingMsgPolicyInterface;
import frodo2.communication.Queue;
import frodo2.controller.Controller;
import frodo2.daemon.Daemon;
import frodo2.gui.Visualizer;
import frodo2.solutionSpaces.DCOPProblemInterface;

/** Prints out all messages received and sent by the agent
 * @author Thomas Leaute
 * @warning Printing out all messages exchanges can be computationally expensive. Only use this feature for debugging purposes. 
 */
public class MessageDebugger implements IncomingMsgPolicyInterface<MessageType>, OutgoingMsgPolicyInterface<MessageType>, Eavesdroppable<String> {
	
	/** The name of this agent */
	private final String agent;
	
	/** Whether to display system messages */
	private final boolean hideSystemMessages;
	
	/** If true, the eavesdropper will be added to each agent's queue and report message exchanged */
	private boolean perAgent = false;
	
	/** The class name for the Visualizer */
	private final String vizClassName;
	
	/** The Visualizer used to display the messages exchanged */
	private final Visualizer viz;

	/** The problem instance */
	private final DCOPProblemInterface<?, ?> problem;
	
	/** Constructor
	 * @param problem 	the problem instance
	 * @param params 	the parameters for this module
	 */
	public MessageDebugger (DCOPProblemInterface<?, ?> problem, Element params) {
		this.problem = problem;
		this.agent = problem.getAgent();
		
		String str = params == null ? null : params.getAttributeValue("hideSystemMessages");
		this.hideSystemMessages = str == null ? true : Boolean.parseBoolean(str);
		
		if (params != null) 
			this.perAgent = Boolean.parseBoolean(params.getAttributeValue("perAgent"));
		
		if (this.agent != null && this.perAgent) 
			System.err.println(
					  "************************************************************************\n"
					+ "WARNING! Messages are being printed out, which slows down the algorithms\n"
					+ "************************************************************************");
		
		this.vizClassName = (params == null ? null : params.getAttributeValue("visualizer"));
		this.viz = (this.perAgent ? this.createVisualizer() : null);
	}
	
	/** Constructor
	 * @param problem 				the problem instance
	 * @param perAgent 				if true, the eavesdropper will be added to each agent's queue and report message exchanged
	 * @param hideSystemMessages 	whether to display system messages
	 * @param vizClassName 			the class name for the Visualizer
	 */
	private MessageDebugger (DCOPProblemInterface<?, ?> problem, boolean perAgent, boolean hideSystemMessages, String vizClassName) {
		this.problem = problem;
		this.agent = problem.getAgent();
		this.perAgent = perAgent;
		this.hideSystemMessages = hideSystemMessages;
		this.vizClassName = vizClassName;
		this.viz = this.createVisualizer();
	}
	
	/** @return a Visualizer of the class specified in the agent config */
	private Visualizer createVisualizer () {
		
		if (this.agent != null 			// local eavesdropper 
				&& ! this.perAgent) 	// local, silent eavesdropper
			return null; 
		
		if (this.vizClassName != null && ! this.vizClassName.isEmpty()) {
			try {
				Visualizer out = (Visualizer) Class.forName(this.vizClassName).getConstructor().newInstance();
				out.setCompiled(true); // show the agents as nodes
				out.render(this.problem);
				return out;
				
			} catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				System.err.println("Failed to instantiate a visualizer of class " + this.vizClassName);
				e.printStackTrace();
			}
		}

		return null;
	}
	
	/** @see IncomingMsgPolicyInterface#setQueue(Queue) */
	@Override
	public void setQueue(Queue queue) { }

	/** @see IncomingMsgPolicyInterface#getMsgTypes() */
	@Override
	public Collection<MessageType> getMsgTypes() {
		if (this.viz != null &&
				(this.agent == null // global eavesdropper 
				|| this.perAgent)) 	// local, talkative eavesdropper
			return Arrays.asList(MessageType.ROOT);
		else 
			return Arrays.asList();	// local, silent eavesdropper 
	}

	/** @see OutgoingMsgPolicyInterface#notifyOut(Message) */
	@Override
	public Decision notifyOut(Message msg) {
		return this.notifyOut(this.agent, msg);
	}

	/** @see OutgoingMsgPolicyInterface#notifyOut(Object, Message, Collection) */
	@Override
	public Decision notifyOut(Object fromAgent, Message msg, Collection<? extends Object> toAgents) {
		
		assert this.agent == null || this.agent.equals(fromAgent);
		
		// Filter out system messages
		if (this.hideSystemMessages) {
			
			// Don't report messages sent by system agents
			if (this.agent != null) {
				switch (this.agent) {
				case Daemon.DAEMON:
				case Controller.CONTROLLER:
				case AgentInterface.STATS_MONITOR:
					return Decision.DONTCARE;
				}
			}
			
			// Don't report system messages
			if (MessageType.SYSTEM.isParent(msg.getType())) 
				return Decision.DONTCARE;

		}
		
		// Visualize the message
		if (this.viz != null) 
			for (Object toAgent : toAgents) 
				this.viz.showOutgoingAgentMessage(fromAgent, msg, toAgent);

		return Decision.DONTCARE;
	}

	/** @see IncomingMsgPolicyInterface#notifyIn(Message) */
	@Override
	public void notifyIn(Message msg) {
		this.notifyIn(msg, this.agent);
	}

	/** @see IncomingMsgPolicyInterface#notifyIn(Message, Object) */
	@Override
	public void notifyIn(Message msg, Object toAgent) {
		
		assert this.agent == null || this.agent.equals(toAgent);
		
		// Filter out system messages
		if (this.hideSystemMessages && MessageType.SYSTEM.isParent(msg.getType())) 
			return;
		
		// Visualize the message
		if (this.viz != null) 
			this.viz.showIncomingAgentMessage(msg, toAgent);
	}

	/** @see Eavesdroppable#getEavesdropper() */
	@SuppressWarnings("unchecked")
	@Override
	public MessageDebugger getEavesdropper() {
		return new MessageDebugger (this.problem, false, this.hideSystemMessages, this.vizClassName); // perAgent = false to avoid double-reporting
	}

}
