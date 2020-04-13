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

package frodo2.communication;

import java.util.Collection;

/** This interface describes a listener that is notified by a queue of outgoing messages
 * @author Thomas Leaute
 * @param <T> the class used for message types
 */
public interface OutgoingMsgPolicyInterface <T> extends MessageListener<T> {
	
	/** The decision made
	 * @author Thomas Leaute 
	 */
	public static enum Decision { 
		/** the message should be discarded */ DISCARD, 
		/** the message can be discarded */ DONTCARE }

	/** Notifies the listener of an outgoing message
	 * @param msg 	outgoing message 
	 * @return The decision on what should be done with \a msg
	 */
	public Decision notifyOut (Message msg);
	
	/** Notifies the listener of an outgoing message
	 * @param fromAgent 	ID of the sender agent 
	 * @param msg 			the outgoing message
	 * @return The decision on what should be done with \a msg
	 */
	public default Decision notifyOut (Object fromAgent, Message msg) {
		return this.notifyOut(fromAgent, msg, null);
	}

	/** Notifies the listener of an outgoing message
	 * @param fromAgent 	ID of the sender agent 
	 * @param msg 			the outgoing message
	 * @param toAgents 		IDs of the destination agents
	 * @return The decision on what should be done with \a msg
	 */
	public default Decision notifyOut (Object fromAgent, Message msg, Collection<? extends Object> toAgents) {
		return this.notifyOut(msg);
	}

	/** Notifies the listener of an outgoing message
	 * @param msg 			the outgoing message
	 * @param toAgents 		IDs of the destination agents
	 * @return The decision on what should be done with \a msg
	 */
	public default Decision notifyOut (Message msg, Collection<? extends Object> toAgents) {
		return this.notifyOut(null, msg, toAgents);
	}

}
