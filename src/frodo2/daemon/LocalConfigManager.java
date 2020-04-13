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

package frodo2.daemon;

import org.jdom2.Document;

import frodo2.algorithms.AgentInterface;
import frodo2.algorithms.XCSPparser;
import frodo2.communication.Message;
import frodo2.communication.MessageWith3Payloads;
import frodo2.controller.ConfigurationManager;
import frodo2.controller.userIO.UserIO;
import frodo2.solutionSpaces.ProblemInterface;

/** A local configuration manager that parses configuration files passed to the daemon
 * @author Thomas Leaute
 */
public class LocalConfigManager extends ConfigurationManager {
	
	/** The daemon */
	private final Daemon daemon;

	/** Constructor 
	 * @param daemon 	the daemon
	 * @param workDir 	the working directory 
	 */
	public LocalConfigManager(Daemon daemon, String workDir) {
		this.daemon = daemon;
		super.workDir = workDir;
		super.msgTypes.add(UserIO.CONFIGURATION_MSG);
		super.msgTypes.add(AGENT_CONFIGURATION_MESSAGE);
		super.msgTypes.add(AgentInterface.AGENT_FINISHED);
	}

	/** @see ConfigurationManager#notifyIn(Message) */
	@Override
	public void notifyIn(Message msg) {
		
		super.notifyIn(msg);
		
		if (msg.getType().equals(AGENT_CONFIGURATION_MESSAGE)) {
			
			@SuppressWarnings("unchecked")
			MessageWith3Payloads <ProblemInterface<?, ?>, Document, Boolean> configMsg = 
					(MessageWith3Payloads <ProblemInterface<?, ?>, Document, Boolean>) msg;
			if (configMsg.getPayload1() == null || configMsg.getPayload2() == null || configMsg.getPayload3() == null) {
				
				if (this.problemList == null) {
					super.tellUser("No configuration file has been loaded yet!");
					return;
				}
				
				else if (this.problem < this.problemList.getChildren().size()) 
					runExperiment((problemList.getChildren().get(problem++)).getAttributeValue("fileName"));
				
				else {
					this.tellUser("No more problems to solve");
					this.problem = 0;
					this.daemon.isFinished = true;
				}
			}
		}
		
		else if (msg.getType().equals(AgentInterface.AGENT_FINISHED)) 
			this.queue.sendMessage(LocalWhitePages.CONTROLLER_ID, msg);
	}

	/** @see ConfigurationManager#distributeAgents(XCSPparser) */
	@Override
	protected void distributeAgents (XCSPparser<?, ?> parser) throws Exception {
		
		super.numberOfAgents = 1;
		super.numberOfAgentsFinished = 0;
		
		MessageWith3Payloads <ProblemInterface<?, ?>, Document, Boolean> configMsg = 
				new MessageWith3Payloads <ProblemInterface<?, ?>, Document, Boolean> (AGENT_CONFIGURATION_MESSAGE, parser, this.agentDescriptionDoc, false);
		super.queue.sendMessageToSelf(configMsg);
	}

}
