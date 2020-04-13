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

import frodo2.communication.IncomingMsgPolicyInterface;
import frodo2.communication.OutgoingMsgPolicyInterface;

/** Defines a module that eavesdrops on the messages exchanged
 * @author Thomas Leaute
 * @param <T> the class used for message types
 */
public interface Eavesdroppable <T> {

	/** @return a MessageListener that listens to incoming and outgoing messages */
	public < E extends IncomingMsgPolicyInterface<T> & OutgoingMsgPolicyInterface<T> > E getEavesdropper ();
	
}
