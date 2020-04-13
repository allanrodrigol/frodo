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

import frodo2.communication.Message;
import frodo2.solutionSpaces.DCOPProblemInterface;

/** A simplistic visualizer that just prints to the console
 * @author Thomas Leaute
 */
public class ConsoleVisualizer extends Visualizer {

	/** Constructor */
	public ConsoleVisualizer() { }

	/** @see Visualizer#setCompiled(boolean) */
	@Override
	public boolean setCompiled(boolean compiled) {
		/// @todo Auto-generated method stub
		return false;
	}

	/** @see Visualizer#render(DCOPProblemInterface) */
	@Override
	public void render(DCOPProblemInterface<?, ?> problem) {
		System.out.println(problem);
	}

	/** @see Visualizer#showOutgoingAgentMessage(java.lang.Object, Message, java.lang.Object, MsgVisualization) */
	@Override
	protected void showOutgoingAgentMessage(Object fromAgent, Message msg, Object toAgent, MsgVisualization viz) {
		
		switch (viz) {
		case NONE: 
			return; 
		default:
			System.out.println("Agent `" + fromAgent + "' sends to Agent `" + toAgent + "' the following message:\n" + msg + "\n");
			super.wait(viz);
		}
	}

	/** @see Visualizer#showIncomingAgentMessage(Message, java.lang.Object, MsgVisualization) */
	@Override
	protected void showIncomingAgentMessage(Message msg, Object toAgent, MsgVisualization viz) {
		
		switch (viz) {
		case NONE: 
			return; 
		default:
			System.out.println("Agent `" + toAgent + "' receives the following message:\n" + msg + "\n");
			super.wait(viz);
		}
	}

}
