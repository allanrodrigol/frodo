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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import frodo2.algorithms.AgentInterface.ComStatsMessage;
import frodo2.algorithms.test.AllTests;
import frodo2.communication.IncomingMsgPolicyInterface;
import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.MessageWith2Payloads;
import frodo2.communication.MessageWrapper;
import frodo2.communication.Queue;
import frodo2.communication.QueueOutputPipeInterface;
import frodo2.communication.mailer.CentralMailer;
import frodo2.communication.sharedMemory.QueueIOPipe;
import frodo2.controller.Controller;
import frodo2.controller.WhitePages;
import frodo2.daemon.LocalAgentReport;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.DCOPProblemInterface;
import frodo2.solutionSpaces.ProblemInterface;

/** A convenience class to create agents
 * @author Thomas Leaute
 * @author Brammert Ottens
 * @param <V> the type used for variable values
 * @param <U> the type used for utility values
 */
public class AgentFactory < V extends Addable<V>, U extends Addable<U> > implements IncomingMsgPolicyInterface<MessageType> {
	
	static {
		assert assertWarning();
	}

	/** Prints a warning a returns \c true
	 * @return \c true
	 */
	private static boolean assertWarning() {
		System.err.println("****************************************************************\n" +
						   "WARNING! Asserts are enabled, which may slow down the algorithms\n" +
						   "****************************************************************");
		return true;
	}

	/** Creates an agent that connects with the controller through TCP
	 * @param <V> 				the type used for variable values
	 * @param toDaemonPipe 		output pipe used to send messages to the daemon
	 * @param toControllerPipe 	output pipe used to send messages to the controller
	 * @param probDesc 			the problem
	 * @param agentDesc 		a JDOM Document describing the agent
	 * @param port 				the port number on which the agent should listen
	 * @return a new instance of an agent
	 * @todo Make it possible to not have to manually choose the port number
	 */
	public static < V extends Addable<V>, U extends Addable<U> > AgentInterface<V> createAgent (
			QueueOutputPipeInterface toDaemonPipe, QueueOutputPipeInterface toControllerPipe, 
			ProblemInterface<V, U> probDesc, Document agentDesc, int port) {
		return createAgent(toDaemonPipe, toControllerPipe, probDesc, agentDesc, true, port);
	}
	
	/** Creates an agent that connects with the controller through TCP
	 * @param <V> 				the type used for variable values
	 * @param toDaemonPipe 		output pipe used to send messages to the daemon
	 * @param toControllerPipe 	output pipe used to send messages to the controller
	 * @param probDesc 			the problem
	 * @param agentDesc 		a JDOM Document describing the agent
	 * @param statsToController if true, stats should be sent to the controller; else, to the daemon
	 * @param port 				the port number on which the agent should listen
	 * @return a new instance of an agent
	 * @todo Make it possible to not have to manually choose the port number
	 */
	public static < V extends Addable<V>, U extends Addable<U> > AgentInterface<V> createAgent (
			QueueOutputPipeInterface toDaemonPipe, QueueOutputPipeInterface toControllerPipe, 
			ProblemInterface<V, U> probDesc, Document agentDesc, boolean statsToController, int port) {

		assert ! Boolean.parseBoolean(agentDesc.getRootElement().getAttributeValue("measureTime")) :
			"measureTime == true, but the Simulated Time metric does not support TCP pipes";

		try {
			AgentInterface<V> agent = instantiateAgent(probDesc, agentDesc, null);
			agent.setup(toDaemonPipe, toControllerPipe, statsToController, port);
			return agent;
		} catch (Exception e) {
			e.printStackTrace();
		} 

		return null;

	}

	/** Creates an agent that runs in the same JVM as the controller
	 * @param <V> 				the type used for variable values
	 * @param controllerPipe 	the output pipe to send messages to the controller
	 * @param probDesc 			the problem
	 * @param agentDesc 		a JDOM Document describing the agent
	 * @param mailman 			the CentralMailer; ignored if not measuring Simulated Time
	 * @return a new instance of an agent
	 */
	public static < V extends Addable<V>, U extends Addable<U> > AgentInterface<V> createAgent(
			QueueOutputPipeInterface controllerPipe, ProblemInterface<V, U> probDesc, Document agentDesc, CentralMailer mailman) {

		try {
			AgentInterface<V> agent = instantiateAgent(probDesc, agentDesc, mailman);
			agent.setup(controllerPipe, controllerPipe, true, -1);
			return agent;
		} catch (Exception e) {
			e.printStackTrace();
		} 

		return null;
	}

	/** Extracts the agent class from the input description, and creates an instance of it
	 * @param <V> 			the type used for variable values
	 * @param probDesc 		the problem
	 * @param agentDesc 	a JDOM Document describing the agent
	 * @param mailman 		the CentralMailer; ignored if not measuring Simulated Time
	 * @return the name of the agent's class to be used
	 * @throws ClassNotFoundException 		thrown if the agent class mentioned in the description is unknown
	 * @throws NoSuchMethodException 		thrown if the agent class used does not contain a constructor that takes in two JDOM Documents
	 * @throws InvocationTargetException 	thrown if the agent constructor throws an exception
	 * @throws IllegalAccessException 		if the constructor of the agent class is not accessible
	 * @throws InstantiationException 		thrown if the agent class provided in the description is an abstract class
	 * @throws IllegalArgumentException 	if an error occurs in passing arguments to the constructor of the agent class
	 */
	@SuppressWarnings("unchecked")
	private static < V extends Addable<V>, U extends Addable<U> > AgentInterface<V> instantiateAgent (
			ProblemInterface<V, U> probDesc, Document agentDesc, CentralMailer mailman) 
	throws ClassNotFoundException, NoSuchMethodException, IllegalArgumentException, InstantiationException, 
	IllegalAccessException, InvocationTargetException {

		// Read the agent class name from the agent description
		String agentClassName = agentDesc.getRootElement().getAttributeValue("className");
		assert agentClassName != null : "The agent description does not have an attribute of name \"className\"";

		// Create a new instance of that class, passing the two descriptions as input parameters
		Class< ? extends AgentInterface<V> > agentClass = (Class<? extends AgentInterface<V>>) Class.forName(agentClassName);
		Constructor< ? extends AgentInterface<V> > constructor = agentClass.getConstructor(ProblemInterface.class, Document.class, CentralMailer.class);
		return constructor.newInstance(probDesc, agentDesc, mailman);
	}

	/** Runs the input algorithm on the input problem
	 * @param args the problem file and the agent description file
	 * @todo Test the simple mode. 
	 * @todo Add a way to check the version number. 
	 */
	@SuppressWarnings("rawtypes")
	public static void main (String[] args) {

		// The GNU GPL copyright notice
		System.out.println("FRODO  Copyright (C) 2008-2019  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek");
		System.out.println("This program comes with ABSOLUTELY NO WARRANTY.");
		System.out.println("This is free software, and you are welcome to redistribute it");
		System.out.println("under certain conditions. Use the option -license to display the license.\n");

		// Parse the inputs
		String timeout = null;
		Document problem = null;
		Document agent = null;
		String outputFilePath = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			// If given the option "-license", print out the license and quit
			if (arg.equals("-license")) {
				try {
					BufferedReader reader = new BufferedReader (new FileReader (new File ("LICENSE.txt")));
					String line = reader.readLine();
					while (line != null) {
						System.out.println(line);
						line = reader.readLine();
					}
					reader.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					System.exit(1);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				return;
			}

			// If passed the option "-timeout", parse the timeout
			if (arg.equals("-timeout")) {
				if (++i == args.length) { // no timeout specified
					System.err.println("Ignoring the option -timeout, which is not followed by any number of milliseconds");
				} else 
					timeout = args[i];
				continue;
			}

			// Parse the output file name
			if ("-o".equals(args[i])) {
				if (i + 1 >= args.length) 
					System.err.println("Option `-o' must be followed by an output file path");
				else 
					outputFilePath = args[++i];
				continue;
			}

			if (problem == null) { // parse the problem file
				try {
					System.out.println("Parsing the input problem file: " + arg);
					problem = XCSPparser.parse(new File (arg), false);
				} catch (JDOMException e) {
					e.printStackTrace();
					System.exit(2);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(3);
				}
			} else { // parse the agent configuration file
				try {
					System.out.println("Parsing the input agent configuration file: " + arg);
					agent = XCSPparser.parse(new File (arg), false);
				} catch (JDOMException e) {
					e.printStackTrace();
					System.exit(4);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(5);
				}				
			}
		}

		if (problem == null) { // no problem file specified
			problem = AllTests.createRandProblem(10, 40, 10, true);
			System.out.println("Using the following random problem:\n" + XCSPparser.toString(problem));
		}

		if (agent == null) { // no agent configuration file specified
			try {
				agent = XCSPparser.parse(AgentFactory.class.getResourceAsStream("/frodo2/algorithms/dpop/DPOPagent.xml"), false);
			} catch (JDOMException e) {
				e.printStackTrace();
				System.exit(4);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(5);
			}
			System.out.println("Using the following agent:\n" + XCSPparser.toString(agent));
		}

		System.out.println("Setting up the agents...");

		if (timeout == null) {
			new AgentFactory (problem, agent, outputFilePath);
		} else 
			new AgentFactory (problem, agent, outputFilePath, Long.parseLong(timeout));
	}

	/** The queue used to listen to the agents */
	private Queue queue;

	/** Total number of agents */
	private int nbrAgents;

	/** All agents, indexed by their IDs */
	private Map< String, AgentInterface<V> > agents;

	/** Each agent's subproblem */
	private Map< String, ProblemInterface<V, U> > subProbs;

	/** Number of agents finished */
	private int nbrAgentsFinished;

	/** Used to synchronize access to \a nbrAgentsFinished */
	private Object nbrAgentsFinished_lock = new Object ();

	/** Whether the agents have finished */
	private boolean done = false;

	/** For each agent, its input pipe */
	private Map<Object, QueueOutputPipeInterface> pipes = new HashMap<Object, QueueOutputPipeInterface> ();

	/** The central listener's own pipe */
	private QueueIOPipe pipe;

	/** Used to track the number of various types of messages received from the agents */
	private int nbrMsgsReceived = 0;

	/** Whether to measure the number of messages and the total amount of information sent */
	private final boolean measureMsgs;

	/** For each message type, the number of messages sent of that type */
	private TreeMap<MessageType, Integer> msgNbrs = new TreeMap<MessageType, Integer> ();

	/** For each agent, the number of messages sent by that agent */
	private TreeMap<Object, Integer> msgNbrsSentPerAgent = new TreeMap<Object, Integer> ();
	
	/** For each agent, the number of messages received by that agent */
	private TreeMap<Object, Integer> msgNbrsReceivedPerAgent = new TreeMap<Object, Integer> ();
	
	/** For each message type, the total amount of information sent in messages of that type, in bytes */
	private TreeMap<MessageType, Long> msgSizes = new TreeMap<MessageType, Long> ();
	
	/** For each agent, the total amount of information sent by that agent, in bytes */
	private TreeMap<Object, Long> msgSizesSentPerAgent = new TreeMap<Object, Long> ();
	
	/** For each agent, the total amount of information received by that agent, in bytes */
	private TreeMap<Object, Long> msgSizesReceivedPerAgent = new TreeMap<Object, Long> ();
	
	/** For each message type, the size (in bytes) of the largest message */
	private TreeMap<MessageType, Long> maxMsgSizes = new TreeMap<MessageType, Long> ();

	/** If true, stats should be sent to the controller; else, to the daemon */
	private boolean statsToController;

	/** The start time of the algorithm, in milliseconds */
	private long startTime;

	/** Whether information should be printed out */
	private boolean silent = false;

	/** The timestamp (in nanoseconds) of the AGENT_FINISHED message with the highest time stamp*/
	private long finalTime = -1;

	/** The nccc stamp of the AGENT_FINISHED message with the highest nccc stamp*/
	private long finalNCCCcount = -1;
	
	/** The default timeout in milliseconds */
	public static long DEFAULT_TIMEOUT = 600000;

	/** The timeout in milliseconds */
	private long timeout = DEFAULT_TIMEOUT;

	/** \c true when the agent factory timed out before all the agents finished*/
	private boolean timedOut = false;

	/** Whether we should measure simulated time (\c true) or wall clock time (\c false) */
	private final boolean measureTime;

	/** The agent configuration */
	private Document agentDesc;

	/** The problem */
	private ProblemInterface<V, U> problem;

	/** The CentralMailer */
	private CentralMailer mailman;
	
	/** The TCP port used for the first agent, which gets incremented for each subsequent agent */
	private static int port = 5000;
	
	/** Whether to use TCP pipes or shared memory pipes */
	private final boolean useTCP;

	/** \c true when algorithm ran out of memory */
	private boolean outOfMemory;
	
	/** The path to the output file */
	private final String outputFilePath;
	
	/** The solution collector */
	private SolutionCollector<V, U> solCollector;
	
	/** Empty constructor */
	protected AgentFactory () {
		this.measureMsgs = this.measureTime = this.useTCP = false;
		this.outputFilePath = null;
	}

	/** Constructor
	 * @param problemDesc 	the problem description
	 * @param agentDesc 	the agent description
	 */
	public AgentFactory (Document problemDesc, Document agentDesc) {
		this (problemDesc, agentDesc, null, null, true);
	}

	/** Constructor
	 * @param problemDesc 		the problem description
	 * @param agentDesc 			the agent description
	 * @param outputFilePath 	the path to the output file
	 */
	public AgentFactory (Document problemDesc, Document agentDesc, String outputFilePath) {
		this (problemDesc, agentDesc, outputFilePath, null, null, true);
	}

	/** Constructor
	 * @param problemDesc 	the problem description
	 * @param agentDesc 	the agent description
	 * @param timeout 		the timeout in milliseconds
	 */
	public AgentFactory (Document problemDesc, Document agentDesc, long timeout) {
		this (problemDesc, agentDesc, null, timeout, true);
	}

	/** Constructor
	 * @param problemDesc 		the problem description
	 * @param agentDesc 			the agent description
	 * @param outputFilePath 	the path to the output file
	 * @param timeout 			the timeout in milliseconds
	 */
	public AgentFactory (Document problemDesc, Document agentDesc, String outputFilePath, long timeout) {
		this (problemDesc, agentDesc, outputFilePath, null, timeout, true);
	}

	/** Constructor
	 * @param problemDesc 	the problem description
	 * @param agentDesc 	the agent description
	 * @param solGatherers 	listeners that will be notified of the statistics sent by the agents (if not \c null, behaves silently)
	 */
	public AgentFactory (Document problemDesc, Document agentDesc, Collection<? extends StatsReporter> solGatherers)  {
		this(problemDesc, agentDesc, solGatherers, null, true);
	}

	/** Constructor
	 * @param problemDesc 		the problem description
	 * @param agentDesc 			the agent description
	 * @param outputFilePath 	the path to the output file
	 * @param solGatherers 		listeners that will be notified of the statistics sent by the agents (if not \c null, behaves silently)
	 * @param timeout 			the timeout, in milliseconds. If \c null, uses the default timeout. 
	 * @param statsToController if true, stats should be sent to the controller; else, to the daemon
	 */
	public AgentFactory (Document problemDesc, Document agentDesc, String outputFilePath, Collection<? extends StatsReporter> solGatherers, 
			Long timeout, boolean statsToController)  {
		this(problemDesc, agentDesc, outputFilePath, solGatherers, timeout, false, statsToController);
	}
	
	/** Constructor
	 * @param problemDesc 		the problem description
	 * @param agentDesc 		the agent description
	 * @param solGatherers 		listeners that will be notified of the statistics sent by the agents (if not \c null, behaves silently)
	 * @param timeout 			the timeout, in milliseconds. If \c null, uses the default timeout. 
	 * @param statsToController if true, stats should be sent to the controller; else, to the daemon
	 */
	public AgentFactory (Document problemDesc, Document agentDesc, Collection<? extends StatsReporter> solGatherers, 
			Long timeout, boolean statsToController)  {
		this(problemDesc, agentDesc, solGatherers, timeout, false, statsToController);
	}
	
	/** Constructor
	 * @param problemDesc 		the problem description
	 * @param agentDesc 		the agent description
	 * @param solGatherers 		listeners that will be notified of the statistics sent by the agents (if not \c null, behaves silently)
	 * @param timeout 			the timeout, in milliseconds. If \c null, uses the default timeout. 
	 * @param useTCP 			Whether to use TCP pipes or shared memory pipes
	 * @param statsToController if true, stats should be sent to the controller; else, to the daemon
	 */
	public AgentFactory (Document problemDesc, Document agentDesc, Collection<? extends StatsReporter> solGatherers, 
			Long timeout, boolean useTCP, boolean statsToController)  {
		this (problemDesc, agentDesc, null, solGatherers, timeout, useTCP, statsToController);
	}
	
	/** Constructor
	 * @param problemDesc 		the problem description
	 * @param agentDesc 			the agent description
	 * @param outputFilePath 	the path to the output file
	 * @param solGatherers 		listeners that will be notified of the statistics sent by the agents (if not \c null, behaves silently)
	 * @param timeout 			the timeout, in milliseconds. If \c null, uses the default timeout. 
	 * @param useTCP 			Whether to use TCP pipes or shared memory pipes
	 * @param statsToController if true, stats should be sent to the controller; else, to the daemon
	 */
	@SuppressWarnings("unchecked")
	public AgentFactory (Document problemDesc, Document agentDesc, String outputFilePath, Collection<? extends StatsReporter> solGatherers, 
			Long timeout, boolean useTCP, boolean statsToController)  {

		this.agentDesc = agentDesc;
		this.useTCP = useTCP;
		this.outputFilePath = outputFilePath;

		// Parse the problem
		try {
			problem = this.parseProblem(problemDesc);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(6);
		}

		// Check whether we should measure distributed time
		String measureTime = agentDesc.getRootElement().getAttributeValue("measureTime");
		if (measureTime == null) 
			this.measureTime = true;
		else 
			this.measureTime = Boolean.parseBoolean(measureTime);

		// Check whether we should be counting messages
		String measureMsgs = agentDesc.getRootElement().getAttributeValue("measureMsgs");
		if (measureMsgs != null) 
			this.measureMsgs = Boolean.parseBoolean(measureMsgs);
		else 
			this.measureMsgs = false;

		this.init(solGatherers, timeout, statsToController);
	}
	
	/** Constructor
	 * @param problem 		the problem
	 * @param agentDesc 	the agent description
	 * @param solGatherers 	listeners that will be notified of the statistics sent by the agents (if not \c null, behaves silently)
	 * @param timeout 		the timeout, in milliseconds. If \c null, uses the default timeout. 
	 */
	public AgentFactory (ProblemInterface<V, U> problem, Document agentDesc, Collection<? extends StatsReporter> solGatherers, Long timeout)  {
		this(problem, agentDesc, solGatherers, timeout, false, true);
	}
	
	/** Constructor
	 * @param problem 			the problem
	 * @param agentDesc 		the agent description
	 * @param solGatherers 		listeners that will be notified of the statistics sent by the agents (if not \c null, behaves silently)
	 * @param timeout 			the timeout, in milliseconds. If \c null, uses the default timeout. 
	 * @param useTCP 			Whether to use TCP pipes or shared memory pipes
	 */
	public AgentFactory (ProblemInterface<V, U> problem, Document agentDesc, Collection<? extends StatsReporter> solGatherers, 
			Long timeout, boolean useTCP)  {
		this(problem, agentDesc, solGatherers, timeout, useTCP, true);
	}
	
	/** Constructor
	 * @param problem 			the problem
	 * @param agentDesc 		the agent description
	 * @param solGatherers 		listeners that will be notified of the statistics sent by the agents (if not \c null, behaves silently)
	 * @param timeout 			the timeout, in milliseconds. If \c null, uses the default timeout. 
	 * @param useTCP 			Whether to use TCP pipes or shared memory pipes
	 * @param statsToController if true, stats should be sent to the controller; else, to the daemon
	 */
	public AgentFactory (ProblemInterface<V, U> problem, Document agentDesc, Collection<? extends StatsReporter> solGatherers, 
			Long timeout, boolean useTCP, boolean statsToController)  {
		this (problem, agentDesc, null, solGatherers, timeout, useTCP, statsToController);
	}
	
	/** Constructor
	 * @param problem 			the problem
	 * @param agentDesc 			the agent description
	 * @param outputFilePath 	the path to the output file
	 * @param solGatherers 		listeners that will be notified of the statistics sent by the agents (if not \c null, behaves silently)
	 * @param timeout 			the timeout, in milliseconds. If \c null, uses the default timeout. 
	 * @param useTCP 			Whether to use TCP pipes or shared memory pipes
	 * @param statsToController if true, stats should be sent to the controller; else, to the daemon
	 */
	public AgentFactory (ProblemInterface<V, U> problem, Document agentDesc, String outputFilePath, Collection<? extends StatsReporter> solGatherers, 
			Long timeout, boolean useTCP, boolean statsToController)  {
		
		this.agentDesc = agentDesc;
		this.problem = problem;
		this.useTCP = useTCP;
		this.outputFilePath = outputFilePath;

		// Check whether we should measure distributed time
		String measureTime = agentDesc.getRootElement().getAttributeValue("measureTime");
		if (measureTime == null) 
			this.measureTime = true;
		else 
			this.measureTime = Boolean.parseBoolean(measureTime);

		// Check whether we should be counting messages
		String measureMsgs = agentDesc.getRootElement().getAttributeValue("measureMsgs");
		if (measureMsgs != null) 
			this.measureMsgs = Boolean.parseBoolean(measureMsgs);
		else 
			this.measureMsgs = false;

		this.init(solGatherers, timeout, statsToController);
	}
	
	/** Convenience method called by constructors to reuse code
	 * @param solGatherers 			listeners that will be notified of the statistics sent by the agents (if not \c null, behaves silently)
	 * @param timeout 				the timeout, in milliseconds. If \c null, uses the default timeout. 
	 * @param statsToController 	if true, stats should be sent to the controller; else, to the daemon
	 */
	@SuppressWarnings("unchecked")
	private void init (Collection<? extends StatsReporter> solGatherers, Long timeout, boolean statsToController)  {
		
		assert !this.measureMsgs || !useTCP : "Cannot measure simulated time while using TCP pipes";
		this.statsToController = statsToController;
		
		if (timeout != null) 
			this.timeout = (timeout <= 0 ? Long.MAX_VALUE : timeout);

		try {

			// Create the queue
			if (! this.measureTime) 
				this.queue = new Queue(false);

			else { // use the CentralMailer
				Element mailmanElmt = agentDesc.getRootElement().getChild("mailman");
				if(mailmanElmt == null)
					mailman = new CentralMailer (this.problem, this.agentDesc.getRootElement());
				else {
					Class<? extends CentralMailer> mailerClass = (Class<? extends CentralMailer>)Class.forName(mailmanElmt.getAttributeValue("mailmanClass"));
					Constructor<? extends CentralMailer> constructor = mailerClass.getConstructor(ProblemInterface.class, Element.class);
					mailman = constructor.newInstance(this.problem, this.agentDesc.getRootElement());
				}

				this.queue = mailman.newQueue(Controller.CONTROLLER, false);
			}

			// Set up the input pipe for the queue
			pipe = new QueueIOPipe (queue);
			queue.addIncomingMessagePolicy(this);

			// Add to the queue all the input solution gatherers
			if (solGatherers != null) {
				for (StatsReporter gatherer : solGatherers) {
					gatherer.getStatsFromQueue(queue);
				}
				silent = true;
			}

			// Go through the list of agents and instantiate them
			Set<String> agentNames = problem.getAgents();
			nbrAgents = agentNames.size();
			agents = new HashMap< String, AgentInterface<V> > (nbrAgents);
			subProbs = new HashMap< String, ProblemInterface<V, U> > ();
			synchronized (agents) {
				for (String agent : agentNames) {
					ProblemInterface<V, U> subProb = problem.getSubProblem(agent);
					if (this.useTCP) 
						agents.put(agent, (AgentInterface<V>) AgentFactory.createAgent(pipe, pipe, subProb, agentDesc, statsToController, ++port));
					else 
						agents.put(agent, (AgentInterface<V>) AgentFactory.createAgent(pipe, subProb, agentDesc, mailman));
					subProbs.put(agent, subProb);
				}

				// Add to my queue all the statistics listeners
				if (!silent) {

					// Read the problem description class name from the agent description, the standard value is DCOPProblemInterface
					Element parserDesc = agentDesc.getRootElement().getChild("parser");
					Class<? extends ProblemInterface<V, U>> probDescClass = (Class<? extends ProblemInterface<V, U>>) Class.forName(DCOPProblemInterface.class.getName());
					if(parserDesc != null) {
						String probDescClassName = parserDesc.getAttributeValue("probDescClass");
						if(probDescClassName != null)
							probDescClass = (Class<? extends ProblemInterface<V, U>>) Class.forName(probDescClassName);
					}

					List<Element> modules = agentDesc.getRootElement().getChild("modules").getChildren();
					for (Element statsListener : modules) {

						// Skip it if it is not a stats reporter with reportStats == true
						String reportStats = statsListener.getAttributeValue("reportStats");
						if (reportStats == null || ! Boolean.parseBoolean(reportStats)) 
							continue;

						// Instantiate the listener by calling its constructor in "statistics gatherer" mode
						Class<? extends StatsReporter> listenerClass = (Class<? extends StatsReporter>) Class.forName(statsListener.getAttributeValue("className"));
						Constructor<? extends StatsReporter> constructor = listenerClass.getConstructor(Element.class, probDescClass);
						StatsReporter listener = constructor.newInstance(statsListener, problem);
						listener.getStatsFromQueue(this.queue);
					}

					// Listen for the solution
					if (this.problem instanceof DCOPProblemInterface) {
						this.solCollector = new SolutionCollector<V, U> (null, (DCOPProblemInterface<V, U>) this.problem);
						this.solCollector.getStatsFromQueue(this.queue);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(6);
		}

		// Wait for all agents to finish
		this.waitForEnd();
	}

	/** Parses the given problem
	 * @param problemDesc 					the problem in XCSP format
	 * @return 								the problem
	 * @throws ClassNotFoundException 		if the parser class was not found
	 * @throws NoSuchMethodException 		if the parser does not have a constructor that takes in a Document
	 * @throws InvocationTargetException 	if the parser constructor throws an exception
	 * @throws IllegalAccessException 		if the parser constructor is not accessible
	 * @throws InstantiationException 		if the parser class is abstract
	 * @throws IllegalArgumentException 	if the argument to the parser constructor is illegal
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ProblemInterface parseProblem (Document problemDesc) throws ClassNotFoundException, NoSuchMethodException, IllegalArgumentException, 
	InstantiationException, IllegalAccessException, InvocationTargetException {

		Element parserElmt = this.agentDesc.getRootElement().getChild("parser");
		if (parserElmt != null) {
			String parserClassName = parserElmt.getAttributeValue("parserClass");
			Class<? extends ProblemInterface<V, U>> parserClass = (Class<? extends ProblemInterface<V, U>>) Class.forName(parserClassName);
			Constructor<? extends ProblemInterface<V, U>> constructor = parserClass.getConstructor(Document.class, Element.class);
			return constructor.newInstance(problemDesc, parserElmt);
		} else 
			return new XCSPparser (problemDesc);
	}

	/** Waits for the algorithm to terminate */
	private void waitForEnd () {

		if (this.measureTime) {
			if (! this.mailman.execute(timeout)) {
				timedOut = true;
				System.err.println("Timed out after " + this.timeout + " ms (simulated time)");
			}
			
			// Write the solution found to the output file
			this.writeSol();

			return;
		}

		synchronized (agents) {
			if (! done) {
				try {
					long startTime = System.currentTimeMillis();
					long timeLeft = this.timeout;
					long timeSpent = 0;
					while(this.nbrAgentsFinished < this.nbrAgents && !outOfMemory && timeLeft > 0 && !this.timedOut) { // This loop is necessary to handle spurious wakeups
						agents.wait(timeLeft);
						timeSpent = System.currentTimeMillis() - startTime;
						timeLeft =  this.timeout - timeSpent;
					}
					if (timeSpent >= this.timeout) {
						timedOut = true;
						System.err.println("Timed out after " + this.timeout + " ms");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		// Write the solution found to the output file
		this.writeSol();
	}

	/** Writes the reported solution to the output file*/
	private void writeSol() {
		
		// Return if we have not been collecting the solution or an output file has not been defined
		if (this.solCollector == null || this.outputFilePath == null || this.outputFilePath.isEmpty()) 
			return; 
		
		Element outElmt = new Element ("solution");
		Document outDoc = new Document (outElmt);
		
		if (this.solCollector != null) {
			
			// Write the solution valuation (utility/cost)
			U valuation = this.solCollector.getUtility();
			if (valuation != null) 
				outElmt.setAttribute("valuation", valuation.toString());
			
			// Write the assignment to each variable
			Map<String, V> sol = this.solCollector.getSolution();
			if (sol != null) {
				for (Map.Entry<String, V> entry : sol.entrySet()) {
					Element assignment = new Element ("assignment");
					assignment.setAttribute("variable", entry.getKey());
					assignment.setAttribute("value", entry.getValue().toString());
					outElmt.addContent(assignment);
				}
			}
		}
		
		try {
			System.out.println("Writing the solution to the file: " + this.outputFilePath);
			new XMLOutputter(Format.getPrettyFormat()).output(outDoc, new FileWriter (this.outputFilePath));
		} catch (IOException e) {
			System.err.println("Failed to write to the file: " + this.outputFilePath);
			e.printStackTrace();
		}
	}

	/** Restarts the algorithm on a new problem
	 * @param problem 	the new problem
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void restart (ProblemInterface<V, U> problem) {

		// Update the problem
		this.problem.reset((ProblemInterface) problem);

		// Reset the relevant fields
		this.done = false;
		this.finalNCCCcount = -1;
		this.finalTime = -1;
		if (this.measureMsgs) {
			this.msgNbrs = new TreeMap<MessageType, Integer> ();
			this.msgSizes = new TreeMap<MessageType, Long> ();
			this.maxMsgSizes = new TreeMap<MessageType, Long> ();
		}
		Set<String> agentNames = problem.getAgents();
		this.nbrAgents = agentNames.size();
		this.nbrAgentsFinished = 0;
		this.nbrMsgsReceived = 0;
		this.pipes.clear();
		this.timedOut = false;

		// First kill the agents that have disappeared since the previous run
		for (Iterator< Map.Entry< String, AgentInterface<V> > > iter = this.agents.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry< String, AgentInterface<V> > entry = iter.next();
			String agentName = entry.getKey();

			if (! agentNames.contains(agentName)) { // the agent has disappeared
				entry.getValue().kill();
				iter.remove();
				this.subProbs.remove(agentName);
			}
		}

		// Instantiate the new agents and restarts the old ones
		synchronized (agents) {
			for (String agentName : agentNames) {

				// Check if this agent is old or new
				AgentInterface<V> agent = this.agents.get(agentName);
				if (agent != null) {// old agent
					this.subProbs.get(agentName).reset((ProblemInterface) problem.getSubProblem(agentName));
					agent.report();

				} else { // new agent
					ProblemInterface<V, U> subProb = problem.getSubProblem(agentName);
					if (this.useTCP) 
						agents.put(agentName, (AgentInterface<V>) AgentFactory.createAgent(pipe, pipe, subProb, agentDesc, statsToController, ++port));
					else 
						agents.put(agentName, (AgentInterface<V>) AgentFactory.createAgent(pipe, subProb, agentDesc, mailman));
					this.subProbs.put(agentName, subProb);
				}
			}
		}

		// Wait for all agents to finish
		this.waitForEnd();
	}

	/** Kills all agents and threads */
	public void end () {

		if (this.measureTime) 
			mailman.end();

		for (AgentInterface<V> agent : agents.values()) 
			agent.kill();
		queue.end();
		
		for (QueueOutputPipeInterface out : this.pipes.values()) 
			out.close();

		this.problem = null;
		this.subProbs = null;
	}

	/** @see IncomingMsgPolicyInterface#getMsgTypes() */
	public Collection<MessageType> getMsgTypes() {
		ArrayList<MessageType> types = new ArrayList<MessageType> (7);
		types.add(AgentInterface.LOCAL_AGENT_REPORTING);
		types.add(AgentInterface.LOCAL_AGENT_ADDRESS_REQUEST);
		types.add(AgentInterface.AGENT_CONNECTED);
		types.add(AgentInterface.AGENT_FINISHED);
		types.add(CentralMailer.OutOfMemMsg);
		types.add(CentralMailer.ERROR_MSG);
		types.add(AgentInterface.ComStatsMessage.COM_STATS_MSG_TYPE);
		return types;
	}

	/** @see IncomingMsgPolicyInterface#notifyIn(Message) */
	@SuppressWarnings("unchecked")
	public void notifyIn(Message msg) {

		MessageType type = msg.getType();

		if (type.equals(AgentInterface.LOCAL_AGENT_REPORTING)) {
			LocalAgentReport msgCast = (LocalAgentReport) msg;
			String agentID = msgCast.getAgentID();
			QueueIOPipe pipe = msgCast.getLocalPipe();
			try {
				pipes.put(agentID, (this.useTCP ? Controller.PipeFactoryInstance.outputPipe(Controller.PipeFactoryInstance.getSelfAddress(msgCast.getPort())) : pipe));
			} catch (IOException e) {
				System.err.println("Unable to create the agent's TCP input pipe");
				e.printStackTrace();
			}
			queue.addOutputPipe(agentID, pipe);

			// Check if all agents have reported, and if so, tell them to connect
			if (pipes.size() >= nbrAgents) 
				queue.sendMessageToMulti(pipes.keySet(), new Message (WhitePages.CONNECT_AGENT));
		}

		else if (type.equals(AgentInterface.LOCAL_AGENT_ADDRESS_REQUEST)) {
			MessageWith2Payloads<String, String> msgCast = (MessageWith2Payloads<String, String>) msg;
			String recipient = msgCast.getPayload2();
			synchronized (agents) {
				agents.get(msgCast.getPayload1()).addOutputPipe(recipient, pipes.get(recipient)); 
			}
		}

		else if (type.equals(AgentInterface.AGENT_CONNECTED)) {
			if (++nbrMsgsReceived >= nbrAgents) { // all agents are now connected; tell them to start
				if (!silent) 
					System.out.println("Starting the algorithm...");
				this.startTime = System.currentTimeMillis();
				queue.sendMessageToMulti(pipes.keySet(), new Message (AgentInterface.START_AGENT));
			}
		}

		else if (type.equals(AgentInterface.ComStatsMessage.COM_STATS_MSG_TYPE)) {
			synchronized (nbrAgentsFinished_lock) { /// @todo is this synchronization necessary?... 

				// Record the stats
				if (this.measureMsgs) {

					ComStatsMessage msgCast = (ComStatsMessage) msg;

					// Increment nbrMsgs
					for (Map.Entry<MessageType, Integer> entry : msgCast.getMsgNbrs().entrySet()) {
						MessageType msgType = entry.getKey();

						Integer nbr = this.msgNbrs.get(msgType);
						if (nbr == null) 
							this.msgNbrs.put(msgType, entry.getValue());
						else 
							this.msgNbrs.put(msgType, nbr + entry.getValue());
					}

					// Increment nbrMsgsSent & Received
					int totalSent = 0;
					for (Map.Entry<Object, Integer> entry : msgCast.getMsgNbrsSent().entrySet()) {
						Object toAgent = entry.getKey();
						totalSent += entry.getValue();

						Integer nbr = this.msgNbrsReceivedPerAgent.get(toAgent);
						if (nbr == null) 
							this.msgNbrsReceivedPerAgent.put(toAgent, entry.getValue());
						else 
							this.msgNbrsReceivedPerAgent.put(toAgent, nbr + entry.getValue());
					}
					Object sender = msgCast.getSender();
					Integer nbr = this.msgNbrsSentPerAgent.get(sender);
					if (nbr == null) 
						this.msgNbrsSentPerAgent.put(sender, totalSent);
					else 
						this.msgNbrsSentPerAgent.put(sender, nbr + totalSent);
					
					// Increment msgSizes
					for (Map.Entry<MessageType, Long> entry : msgCast.getMsgSizes().entrySet()) {
						MessageType msgType = entry.getKey();

						Long size = this.msgSizes.get(msgType);
						if (size == null) 
							this.msgSizes.put(msgType, entry.getValue());
						else 
							this.msgSizes.put(msgType, size + entry.getValue());
					}
					
					// Increment msgSizesSent & Received
					long totalInfoSent = 0;
					for (Map.Entry<Object, Long> entry : msgCast.getMsgSizesSent().entrySet()) {
						Object toAgent = entry.getKey();
						totalInfoSent += entry.getValue();

						Long info = this.msgSizesReceivedPerAgent.get(toAgent);
						if (info == null) 
							this.msgSizesReceivedPerAgent.put(toAgent, entry.getValue());
						else 
							this.msgSizesReceivedPerAgent.put(toAgent, info + entry.getValue());
					}
					Long info = this.msgSizesSentPerAgent.get(sender);
					if (info == null) 
						this.msgSizesSentPerAgent.put(sender, totalInfoSent);
					else 
						this.msgSizesSentPerAgent.put(sender, info + totalInfoSent);
					
					// Update maxMsgSizes
					for (Map.Entry<MessageType, Long> entry : msgCast.getMaxMsgSizes().entrySet()) {
						MessageType msgType = entry.getKey();
						
						Long maxSize = this.maxMsgSizes.get(msgType);
						if (maxSize == null || entry.getValue() > maxSize) 
							this.maxMsgSizes.put(msgType, entry.getValue());
					}
				}
			}
		}
		
		else if (type.equals(AgentInterface.AGENT_FINISHED)) {
			synchronized (nbrAgentsFinished_lock) { /// @todo is this synchronization necessary?... 
				MessageWrapper msgWrap = queue.getCurrentMessageWrapper();
				long time = msgWrap.getTime();
				if(finalTime < time) {
					finalTime = time;
				}

				long nccc = msgWrap.getNCCCs();
				if(finalNCCCcount < nccc) {
					finalNCCCcount = nccc;
				}

				if (++nbrAgentsFinished >= nbrAgents) {

					if (! this.measureTime) 
						this.finalTime = (System.currentTimeMillis() - this.startTime) * 1000000; // in nanoseconds

					if (!silent) {
						NumberFormat formatter = NumberFormat.getInstance();

						if(this.measureTime)
							System.out.println("Algorithm finished in " + formatter.format(this.getTime()) + " ms (simulated time)");
						else 
							System.out.println("Algorithm finished in " + formatter.format(this.getTime()) + " ms (wall clock time)");

						// Print NCCCs
						if (finalNCCCcount > 0) 
							System.out.println("Number of NCCCs = " + formatter.format(finalNCCCcount));

						if (this.measureMsgs) {
							
							/// @todo Also report communication stats after a timeout. 
							
							AgentFactory.printMsgStats(this.msgNbrs, this.msgNbrsSentPerAgent, this.msgNbrsReceivedPerAgent, this.msgSizes, 
									this.msgSizesSentPerAgent, this.msgSizesReceivedPerAgent, this.maxMsgSizes);
						}
					}

					// Terminate
					synchronized (agents) {
						done = true;
						agents.notify();
					}
				}
			}
		}
		else if (type.equals(CentralMailer.OutOfMemMsg)) {
			synchronized (agents) {
				System.err.println("Out of Memory");
				done = true;
				outOfMemory = true;
				this.problem = null;
				this.subProbs = null;
				agents.notify();
			}
		}
		else if (type.equals(CentralMailer.ERROR_MSG)) {
			synchronized (agents) {
				done = true;
				this.timedOut = true;
				this.problem = null;
				this.subProbs = null;
				agents.notify();
			}
		}

	}

	/** Prints the message statistics
	 * @param msgNbrs 					For each message type, the number of messages sent of that type
	 * @param msgNbrsSentPerAgent 		For each agent, the number of messages sent by that agent
	 * @param msgNbrsReceivedPerAgent 	For each agent, the number of messages received by that agent
	 * @param msgSizes 					For each message type, the total amount of information sent in messages of that type, in bytes
	 * @param msgSizesSentPerAgent 		For each agent, the total amount of information sent by that agent, in bytes
	 * @param msgSizesReceivedPerAgent 	For each agent, the total amount of information received by that agent, in bytes
	 * @param maxMsgSizes 				For each message type, the size (in bytes) of the largest message
	 */
	public static void printMsgStats(Map<MessageType, Integer> msgNbrs,
			Map<Object, Integer> msgNbrsSentPerAgent,
			Map<Object, Integer> msgNbrsReceivedPerAgent,
			Map<MessageType, Long> msgSizes,
			Map<Object, Long> msgSizesSentPerAgent,
			Map<Object, Long> msgSizesReceivedPerAgent,
			Map<MessageType, Long> maxMsgSizes) {
		
		NumberFormat formatter = NumberFormat.getInstance();

		// Print the number of messages sent and received
		int totalNbr = 0;
		System.out.println("Number of messages sent (by type): ");
		for (Map.Entry<MessageType, Integer> entry : msgNbrs.entrySet()) {
			int nbr = entry.getValue();
			System.out.println("\t" + entry.getKey() + ":\t" + formatter.format(nbr));
			totalNbr += nbr;
		}
		System.out.println("\t- Total:\t" + formatter.format(totalNbr));
		
		System.out.println("Number of messages sent (by agent): ");
		for (Map.Entry<Object, Integer> entry : msgNbrsSentPerAgent.entrySet()) 
			System.out.println("\t" + entry.getKey() + ":\t" + formatter.format(entry.getValue()));

		System.out.println("Number of messages received (by agent): ");
		for (Map.Entry<Object, Integer> entry : msgNbrsReceivedPerAgent.entrySet()) 
			System.out.println("\t" + entry.getKey() + ":\t" + formatter.format(entry.getValue()));

		// Print the amount of information sent
		long totalSize = 0;
		System.out.println("Amount of information sent (by type, in bytes): ");
		for (Map.Entry<MessageType, Long> entry : msgSizes.entrySet()) {
			long size = entry.getValue();
			System.out.println("\t" + entry.getKey() + ":\t" + formatter.format(size));
			totalSize += size;
		}
		System.out.println("\t- Total:\t" + formatter.format(totalSize));

		System.out.println("Amount of information sent (by agent, in bytes): ");
		for (Map.Entry<Object, Long> entry : msgSizesSentPerAgent.entrySet()) 
			System.out.println("\t" + entry.getKey() + ":\t" + formatter.format(entry.getValue()));

		System.out.println("Amount of information received (by agent, in bytes): ");
		for (Map.Entry<Object, Long> entry : msgSizesReceivedPerAgent.entrySet()) 
			System.out.println("\t" + entry.getKey() + ":\t" + formatter.format(entry.getValue()));

		// Print the maximum message size
		long maxSize = 0;
		System.out.println("Size of the largest message sent (by type, in bytes): ");
		for (Map.Entry<MessageType, Long> entry : maxMsgSizes.entrySet()) {
			long size = entry.getValue();
			System.out.println("\t" + entry.getKey() + ":\t" + formatter.format(size));
			maxSize = Math.max(size, maxSize);
		}
		System.out.println("\t- Overall maximum:\t" + formatter.format(maxSize));
	}

	/** Does nothing
	 * @see IncomingMsgPolicyInterface#setQueue(Queue)
	 */
	public void setQueue(Queue queue) { }

	/**
	 * @return the total number of ncccs used during experimentation
	 */
	public long getNcccs() {
		return this.finalNCCCcount;
	}

	/**
	 * @return the total needed time, equal to the highest timestamp across all AGENT_FINISHED
	 * messages received (in milliseconds)
	 */
	public long getTime() {
		return finalTime / 1000000;
	}

	/** @return the total number of messages sent */
	public int getNbrMsgs() {
		int nbr = 0;
		for (Integer i : this.msgNbrs.values()) 
			nbr += i;
		return nbr;
	}

	/** @return the total amount of information sent, in bytes */
	public long getTotalMsgSize() {
		long size = 0;
		for (Long l : this.msgSizes.values()) 
			size += l;
		return size;
	}
	
	/** @return the size (in bytes) of the largest message */
	public long getOverallMaxMsgSize() {
		long maxSize = 0;
		for (Long l : this.maxMsgSizes.values()) 
			maxSize = Math.max(maxSize, l);
		return maxSize;
	}

	/**
	 * @author Brammert Ottens, 24 aug 2009
	 * @return the total number of messages that have been sent
	 */
	public TreeMap<MessageType, Integer> getMsgNbrs() {
		return this.msgNbrs;
	}

	/** @return the number of messages sent by each agent */
	public TreeMap<Object, Integer> getMsgNbrsSentPerAgent() {
		return msgNbrsSentPerAgent;
	}

	/** @return the number of messages received by each agent */
	public TreeMap<Object, Integer> getMsgNbrsReceivedPerAgent() {
		return msgNbrsReceivedPerAgent;
	}

	/**
	 * @author Brammert Ottens, 24 aug 2009
	 * @return the total amount of information that has been sent
	 */
	public TreeMap<MessageType, Long> getMsgSizes() {
		return this.msgSizes;
	}
	
	/** @return the amount of information sent by each agent, in bytes */
	public TreeMap<Object, Long> getMsgSizesSentPerAgent() {
		return msgSizesSentPerAgent;
	}

	/** @return the amount of information received by each agent, in bytes */
	public TreeMap<Object, Long> getMsgSizesReceivedPerAgent() {
		return msgSizesReceivedPerAgent;
	}

	/** @return for each message type, the size (in bytes) of the largest message of that type */
	public TreeMap<MessageType, Long> getMaxMsgSizes() {
		return this.maxMsgSizes;
	}

	/**
	 * @author Brammert Ottens, 22 sep 2009
	 * @return \c true when the agent factory timed 
	 */
	public boolean timedOut() {
		return this.timedOut;
	}

	/**
	 * @author Brammert Ottens, 7 jan 2010
	 * @return \c true when the algorithm ran out of memory
	 */
	public boolean outOfMemory() {
		return this.outOfMemory;
	}

	/**
	 * Method used to obtain the final solution in the case of 
	 * a time out or an OutOfMemory exceptions
	 * 
	 * @author Brammert Ottens, Thomas Leaute
	 * @return the solution found by the algorithm upon termination
	 */
	public Map<String, V> getCurrentSolution() {
		Map<String, V> solution = new HashMap<String, V> ();
		for(AgentInterface<V> a : agents.values()) {
			solution.putAll(a.getCurrentSolution());
		}

		return solution;
	}
}
