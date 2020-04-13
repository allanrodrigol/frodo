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

/** Package containing various algorithms */
package frodo2.algorithms;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

import frodo2.communication.MessageType;
import frodo2.communication.MessageWith3Payloads;
import frodo2.communication.QueueOutputPipeInterface;
import frodo2.solutionSpaces.Addable;

/** All agents corresponding to various algorithms should implement this interface
 * @author Brammert Ottens
 * @author Thomas Leaute
 * @param <V> the type used for variable values
 * @warning Any class implementing this interface should have a constructor that takes in: 
 * a ProblemInterface containing the problem description, a Document containing the agent description, and a CentralMailer. 
 * @todo The methods connect(), start() and kill() are defined here so that the daemon can call them. 
 * Instead, the daemon currently sends messages to the agent. This should be clarified. If these methods
 * are never called except by the agent itself, they should be removed from the interface. 
 */
public interface AgentInterface < V extends Addable<V> > {
	
	/** Recipient ID to which statistics about algorithm execution should be sent */
	public static final String STATS_MONITOR = "Statistics Monitor";
	
	/** Message to be sent if an agent has a connection with all its neighbours */
	public static final MessageType AGENT_CONNECTED = MessageType.SYSTEM.newChild("AgentInterface", "Agent-Connected");
	
	/** The message sent when an agent has terminated */
	public static final MessageType AGENT_FINISHED = MessageType.SYSTEM.newChild("AgentInterface", "Agent-Ready");
	
	/** The message sent when it has been detected that all agents are waiting for messages, but there are no more messages on the way */
	public static final MessageType ALL_AGENTS_IDLE = MessageType.SYSTEM.newChild("AgentInterface", "ALL_AGENTS_IDLE");
	
	/** A message containing statistics about messages sent
	 * @author Thomas Leaute
	 */
	public static class ComStatsMessage extends MessageWith3Payloads< HashMap<MessageType, Integer>, HashMap<MessageType, Long>, HashMap<MessageType, Long> > {

		/** The type of this message */
		public static final MessageType COM_STATS_MSG_TYPE = MessageType.SYSTEM.newChild("AgentInterface", "Communication statistics");
		
		/** The sender agent */
		private Object sender;
		
		/** The number of messages sent to each other agent */
		private HashMap<Object, Integer> msgNbrsSent;
		
		/** The amount of information sent to each other agent, in bytes */
		private HashMap<Object, Long> msgSizesSent;
		
		/** Empty constructor used for externalization */
		public ComStatsMessage () { }

		/** Constructor
		 * @param sender 			the sender agent
		 * @param msgNbrs 			for each message type, the number of messages sent of that type
		 * @param msgNbrsSent 		the number of messages sent to each other agent
		 * @param msgSizes 			for each message type, the total amount of information sent in messages of that type, in bytes
		 * @param msgSizesSent 		the amount of information sent to each other agent, in bytes
		 * @param maxMsgSizes 		for each message type, the size (in bytes) of the largest message of this type
		 */
		public ComStatsMessage(Object sender, HashMap<MessageType, Integer> msgNbrs, HashMap<Object, Integer> msgNbrsSent, 
				HashMap<MessageType, Long> msgSizes, HashMap<Object, Long> msgSizesSent, HashMap<MessageType, Long> maxMsgSizes) {
			super(COM_STATS_MSG_TYPE, msgNbrs, msgSizes, maxMsgSizes);
			this.sender = sender;
			this.msgNbrsSent = msgNbrsSent;
			this.msgSizesSent = msgSizesSent;
		}
		
		/** @return the sender agent */
		public Object getSender() {
			return sender;
		}

		/** Sets the sender agent
		 * @param sender 	the sender agent
		 */
		public void setSender(Object sender) {
			this.sender = sender;
		}

		/** @return for each message type, the number of messages sent of that type */
		public HashMap<MessageType, Integer> getMsgNbrs () {
			return this.getPayload1();
		}
		
		/** @return for each message type, the total amount of information sent in messages of that type, in bytes */
		public HashMap<MessageType, Long> getMsgSizes () {
			return this.getPayload2();
		}
		
		/** @return for each message type, the size (in bytes) of the largest message of this type */
		public HashMap<MessageType, Long> getMaxMsgSizes () {
			return this.getPayload3();
		}
		
		/** @return the number of messages sent to each other agent */
		public HashMap<Object, Integer> getMsgNbrsSent() {
			return msgNbrsSent;
		}

		/** Sets the number of messages sent to each other agent
		 * @param msgNbrsSent 	the number of messages sent
		 */
		public void setMsgNbrsSent(HashMap<Object, Integer> msgNbrsSent) {
			this.msgNbrsSent = msgNbrsSent;
		}

		/** @return the amount of information sent to each other agent, in bytes */
		public HashMap<Object, Long> getMsgSizesSent() {
			return msgSizesSent;
		}

		/** Sets the amount of information sent to each other agent, in bytes
		 * @param msgSizesSent 	the amount of information sent
		 */
		public void setMsgSizesSent(HashMap<Object, Long> msgSizesSent) {
			this.msgSizesSent = msgSizesSent;
		}

		/** @see MessageWith3Payloads#writeExternal(java.io.ObjectOutput) */
		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			super.writeExternal(out);
			out.writeObject(this.sender);
			out.writeObject(this.msgNbrsSent);
			out.writeObject(this.msgSizesSent);			
		}

		/** @see MessageWith3Payloads#readExternal(java.io.ObjectInput) */
		@SuppressWarnings("unchecked")
		@Override
		public void readExternal(ObjectInput in) throws IOException,
				ClassNotFoundException {
			super.readExternal(in);
			this.sender = in.readObject();
			this.msgNbrsSent = (HashMap<Object, Integer>) in.readObject();
			this.msgSizesSent = (HashMap<Object, Long>) in.readObject();
		}

		/** @see MessageWith3Payloads#toString() */
		@Override
		public String toString () {
			return "Message(" + this.getType() + ")"
					+ "\n\tsender = " + this.sender
					+ "\n\tmsgNbrs = " + this.getMsgNbrs() 
					+ "\n\tmsgSizes = " + this.getMsgSizes() 
					+ "\n\tmaxMsgSizes = " + this.getMaxMsgSizes()
					+ "\n\tmsgNbrsSent = " + this.msgNbrsSent
					+ "\n\tmsgSizesSent = " + this.msgSizesSent;					
		}
	}
	
	/** The message an agent uses to ask the white pages for an address*/
	public static final MessageType LOCAL_AGENT_ADDRESS_REQUEST = MessageType.SYSTEM.newChild("AgentInterface", "local-address-request");
	
	/** an agent reports to its local white pages*/
	public static final MessageType LOCAL_AGENT_REPORTING = MessageType.SYSTEM.newChild("AgentInterface", "local-agent-reporting");

	/** Message used to tell an agent to start its algorithm */
	public static final MessageType START_AGENT = MessageType.SYSTEM.newChild("AgentInterface", "Start-Agent");
	
	/** Message used to tell an agent to stop */
	public static final MessageType STOP_AGENT = MessageType.SYSTEM.newChild("AgentInterface", "Stop-Agent");
	
	/** Tells the agent to report to the local white pages */
	public void report ();
	
	/** Tells the agent to start requesting connections to other agents from the white pages */
	public void connect ();
	
	/** Starts the algorithm */
	public void start ();
	
	/** Stops the algorithm */
	public void kill ();
	
	/** @return the agent's ID */
	public String getID();
	
	/** Returns the solution found by the algorithm upon termination 
	 * @return a global assignment
	 */
	public Map<String, V> getCurrentSolution();
	
	/**
	 * Adds an output pipe to the agent. If an agent is connected to all
	 * its neighbours, it should send an AGENT_CONNECTED message to the controller
	 * @param agent destination of the pipe
	 * @param outputPipe output pipe
	 */
	public void addOutputPipe(String agent, QueueOutputPipeInterface outputPipe);

	/** Sets up the agent to communicate with a daemon, a controller, and its neighbors
	 * @param toDaemonPipe output pipe to the daemon
	 * @param toControllerPipe the output pipe that should be used to communicate with the controller
	 * @param statsToController if true, stats should be sent to the controller; else, to the daemon
	 * @param port the port the agent is listening on. If < 0, no TCP pipe should be created. 
	 */
	public void setup (QueueOutputPipeInterface toDaemonPipe, QueueOutputPipeInterface toControllerPipe, boolean statsToController, int port);
}
