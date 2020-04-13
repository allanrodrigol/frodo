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

import java.util.ArrayList;

import org.jdom2.Element;

import frodo2.algorithms.AgentInterface;
import frodo2.communication.IncomingMsgPolicyInterface;
import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.Queue;
import frodo2.solutionSpaces.AddableInteger;
import frodo2.solutionSpaces.MASProblemInterface;

/** A sample MAS agent that just prints its problem
 * @author Thomas Leaute
 */
public class SampleMASagent implements IncomingMsgPolicyInterface<MessageType> {
	
	/** The agent's subproblem */
	private MASProblemInterface<AddableInteger, AddableInteger> problem;
	
	/** Constructor
	 * @param problem 	problem instance
	 * @param agentDesc agent description
	 */
	public SampleMASagent (MASProblemInterface<AddableInteger, AddableInteger> problem, Element agentDesc) {
		this.problem = problem;
	}

	/** @see frodo2.communication.MessageListener#setQueue(frodo2.communication.Queue) */
	@Override
	public void setQueue(Queue queue) {}

	/** @see frodo2.communication.MessageListener#getMsgTypes() */
	@Override
	public ArrayList<MessageType> getMsgTypes() {
		
		ArrayList<MessageType> out = new ArrayList<MessageType> ();
		out.add(AgentInterface.START_AGENT);
		return out;
	}

	/** @see frodo2.communication.IncomingMsgPolicyInterface#notifyIn(frodo2.communication.Message) */
	@Override
	public void notifyIn(Message msg) {
		
		System.out.println("Subproblem for agent `" + this.problem.getAgent() + "':\n" + this.problem);
	}

}
