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

package frodo2.gui;

import java.util.concurrent.ConcurrentHashMap;

import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.solutionSpaces.DCOPProblemInterface;

/** An interface for visualizing agents, variables, constraints, and message exchanges
 * @author Thomas Leaute
 * @todo Make it possible to switch between primal constraint graph, bi-partite function graph, and dual decision graph
 * @todo Make it possible to switch between decomposition, clustering (nodes owned by common agent close together), and compilation
 * @todo Make it possible to switch between pair-wise graph and hypergraph (= function graph with hidden nodes?) 
 * @todo Make it possible to display messages exchanged between nodes
 * @todo Render the construction of the variable ordering
 * @todo Display outbound UTIL message computation progress bars for DPOP
 */
public abstract class Visualizer {
	
	/** The GUI to control how much time each message is displayed */
	private static VisualizerControls controls;
	
	/** How long each message should be displayed, in ms */
	private static long displayTime = 25; /// @todo Make it settable in the GUI in a thread-safe fashion
	
	/** @return how long each message should be displayed, in ms */
	public static long getDisplayTime () {
		return displayTime;
	}
	
	/** Whether and how long to display a message type */
	public static enum MsgVisualization {
		/** Do not display the message */
		NONE, 
		/** Display the message for DISPLAY_TIME*/
		TIMED, 
		/** Display until the user clicks on NEXT */
		STEPPED
	}
	
	/** The default message visualization strategy */
	private static MsgVisualization defaultViz = MsgVisualization.STEPPED;
	
	/** For each message type, how long to display messages */
	private final static ConcurrentHashMap<MessageType, MsgVisualization> msgViz = new ConcurrentHashMap<MessageType, MsgVisualization> ();
	
	/** Looks up the visualization strategy for a given message type
	 * @param msgType 	the message type
	 * @return how long to display each message of the input message type
	 */
	public static MsgVisualization getMsgViz (MessageType msgType) {
		
		MsgVisualization viz = msgViz.get(msgType);
		
		/// @todo Notify the visualizer controls so that it can highlight the current message type
		
		// If this is the first time this message type is queried, set the strategy to its parent's 
		if (viz == null) {
			MessageType parentType = msgType.getParent();
			msgViz.put(msgType, viz = (parentType == null ? defaultViz : getMsgViz(parentType)));
			
			Visualizer.controls.addControlPanel(msgType);
		}
		
		// Only enable the STEP button if relevant
		controls.enableStepButton(viz == MsgVisualization.STEPPED);
		
		return viz;
	}
	
	/** Sets the visualization strategy for a given message type
	 * @param msgType 	the message type
	 * @param viz 		how long to display each message of the input type
	 */
	public static void setMsgViz (MessageType msgType, MsgVisualization viz) {
		
		assert msgViz.containsKey(msgType);
		
		msgViz.put(msgType, viz);
	}
	
	/** Constructor */
	protected Visualizer () {
		
		// Instantiate the controller GUI
		if (controls == null) 
			controls = new VisualizerControls ();
	}
	
	/** Configures the compilation mode
	 * @param compiled 	whether or not the graph should be compiled
	 * @return the previous value of the setting
	 */
	public abstract boolean setCompiled (boolean compiled);
	
	/** Renders the input problem instance
	 * @param problem 	the problem instance
	 */
	public abstract void render (DCOPProblemInterface<?, ?> problem);
	
	/** Visualizes an outgoing message
	 * @param fromAgent 	ID of the source agent
	 * @param msg 			the message
	 * @param toAgent 		ID of the destination agent
	 */
	public final void showOutgoingAgentMessage (Object fromAgent, Message msg, Object toAgent) {
		
		// Check whether and how long to display the message
		MsgVisualization viz = getMsgViz(msg.getType());
		if (viz != MsgVisualization.NONE) 
			this.showOutgoingAgentMessage(fromAgent, msg, toAgent, viz);
	}
	
	/** Visualizes an outgoing message
	 * @param fromAgent 	ID of the source agent
	 * @param msg 			the message
	 * @param toAgent 		ID of the destination agent
	 * @param viz 			the visualization strategy
	 */
	protected abstract void showOutgoingAgentMessage (Object fromAgent, Message msg, Object toAgent, MsgVisualization viz);
	
	/** Visualizes an incoming message
	 * @param msg 			the message
	 * @param toAgent 		ID of the destination agent
	 */
	public final void showIncomingAgentMessage (Message msg, Object toAgent) {
		
		// Check whether and how long to display the message
		MsgVisualization viz = getMsgViz(msg.getType());
		if (viz != MsgVisualization.NONE) 
			this.showIncomingAgentMessage(msg, toAgent, viz);
	}
	
	/** Visualizes an incoming message
	 * @param msg 			the message
	 * @param toAgent 		ID of the destination agent
	 * @param viz 			the visualization strategy
	 */
	protected abstract void showIncomingAgentMessage (Message msg, Object toAgent, MsgVisualization viz);

	/** Waits until the specified condition 
	 * @param viz the visualization strategy 
	 */
	protected final void wait (MsgVisualization viz) {
		
		switch (viz) {
		
		case NONE:
			return;
			
		case TIMED:
			try {
				Thread.sleep(getDisplayTime());
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		default:
			try {
				synchronized (controls) {
					controls.wait();
				}
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
