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

package frodo2.algorithms.duct.tests;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;

import frodo2.algorithms.AgentInterface;
import frodo2.algorithms.RandGraphFactory;
import frodo2.algorithms.XCSPparser;
import frodo2.algorithms.dpop.DPOPsolver;
import frodo2.algorithms.duct.Normalize;
import frodo2.algorithms.duct.OUTmsg;
import frodo2.algorithms.test.AllTests;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration.DFSview;
import frodo2.communication.IncomingMsgPolicyInterface;
import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.Queue;
import frodo2.communication.QueueOutputPipeInterface;
import frodo2.communication.sharedMemory.QueueIOPipe;
import frodo2.solutionSpaces.AddableInteger;
import frodo2.solutionSpaces.AddableReal;
import frodo2.solutionSpaces.DCOPProblemInterface;
import frodo2.solutionSpaces.UtilitySolutionSpace;
import junit.extensions.RepeatedTest;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Brammert Ottens, 17 aug. 2011
 * 
 */
public class NormalizeTest extends TestCase implements IncomingMsgPolicyInterface<MessageType> {

	/** Maximum number of variables in the random graph 
	 * @note Must be at least 2. 
	 */
	private final int maxNbrVars = 5;
	
	/** Maximum number of edges in the random graph */
	private final int maxNbrEdges = 9;

	/** Maximum number of agents */
	private final int maxNbrAgents = 5;
	
	/** The number of output messages remaining to be received from the UTIL propagation protocol */
	protected int nbrMsgsRemaining;
	
	/** Used to synchronize the access to \a nbrMsgsRemaining
	 * 
	 * We cannot synchronize directly over \a nbrMsgsRemaining because it will be decremented, and hence the object will change. 
	 */
	protected final Object nbrMsgsRemaining_lock = new Object ();
	
	/** List of queues, indexed by agent name */
	protected Map<String, Queue> queues;
	
	/** The parameters for adopt*/
	protected Element parameters;
	
	/** Random graph used to generate a constraint graph */
	protected RandGraphFactory.Graph graph;
	
	/** The DFS corresponding to the random graph */
	protected Map<String, DFSview<AddableInteger, AddableReal>> dfs;
	
	/** The problem to be solved*/
	protected Document problem;
	
	/** Solver used to calculate the optimal utility*/
	protected DPOPsolver<AddableInteger, AddableReal> solver;
	
	/** The XCSP parser */
	protected XCSPparser<AddableInteger, AddableReal> parser;
	
	/** The optimal utility reported by the UTILpropagation*/
	protected AddableReal optUtil;

	/** Whether to use TCP or SharedMemory pipes */
	protected boolean useTCP;

	/** The queue this class listens to */
	protected Queue myQueue;
	
	/** List of normalized spaces */
	private HashMap<String, ArrayList<UtilitySolutionSpace<AddableInteger, AddableReal>>> spaces;
	
	/**
	 * @param useTCP when \c true TCP pipes are used for communication, otherwise IO pipes
	 */
	public NormalizeTest(boolean useTCP) {
		super("test");
		this.useTCP = useTCP;
	}

	/** 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		// generate the problem graph and the structure
		graph = RandGraphFactory.getRandGraph(maxNbrVars, maxNbrEdges, maxNbrAgents);
		problem = AllTests.generateProblem(graph, true, 0, false, 0.0); // for the moment it only works for problems without infeasible utilities
		solver = new DPOPsolver<AddableInteger, AddableReal> (AddableInteger.class, AddableReal.class);
		parser = new XCSPparser<AddableInteger, AddableReal> (problem);
		parser.setDomClass(AddableInteger.class);
		this.parser.setUtilClass(AddableReal.class);
		
		dfs = frodo2.algorithms.dpop.test.UTILpropagationTest.computeDFS(graph, parser);
		
		this.parameters = new Element ("module");
		this.parameters.setAttribute("reportStats", "true");
	}

	/** 
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		graph = null;
		dfs = null;
		for (Queue queue : queues.values()) {
			queue.end();
		}
		queues = null;
		this.optUtil = null;
		this.parameters = null;
		this.parser = null;
		this.problem = null;
		this.solver = null;
	}
	
	/** @return the test suite for this test */
	static public TestSuite suite () {
		TestSuite testSuite = new TestSuite ("Tests for Normalize");
		
		TestSuite testTmp = new TestSuite ("Tests for the Normalization protocol using shared memory pipes");
		testTmp.addTest(new RepeatedTest (new NormalizeTest (false), 500));
		testSuite.addTest(testTmp);
		
		
		testTmp = new TestSuite ("Tests for the Normalization protocol using TCP pipes");
		testTmp.addTest(new RepeatedTest (new NormalizeTest (true), 500));
		testSuite.addTest(testTmp);
		
		return testSuite;
	}
	
	/** Runs a random test
	 * @throws IOException 					if the method fails to create pipes
	 * @throws NoSuchMethodException 		if the ADOPT class does not have a public constructor that takes in a ProblemInterface and a JDOM Element
	 * @throws IllegalAccessException 		if the ADOPT class does not have a public constructor that takes in a ProblemInterface and a JDOM Element
	 * @throws InstantiationException 		if the instantiation of ADOPT failed
	 * @throws IllegalArgumentException 	if an error occurs in passing arguments to the constructor of ADOPT
	 * @throws ClassNotFoundException 		if the utility class is not found
	 * @throws InvocationTargetException 	if the UTILpropagation constructor throws an exception
	 */
	@SuppressWarnings("unchecked")
	public void test () 
	throws IOException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException {
		
		nbrMsgsRemaining = graph.nodes.size(); // One spaces message per variable
		
		// Create the queue network
		queues = new HashMap<String, Queue> ();
		Map<String, QueueOutputPipeInterface> pipes = AllTests.createQueueNetwork(queues, graph, useTCP);

		// Listen for statistics messages
		myQueue = new Queue (false);
		myQueue.addIncomingMessagePolicy(this);
		QueueIOPipe myPipe = new QueueIOPipe (myQueue);
		for (Queue queue : queues.values()) 
			queue.addOutputPipe(AgentInterface.STATS_MONITOR, myPipe);
		
		// Create the listeners
		XCSPparser<AddableInteger, AddableReal> parser = new XCSPparser<AddableInteger, AddableReal> (problem);
		parser.setDomClass(AddableInteger.class);
		parser.setUtilClass(AddableReal.class);

		for (String agent : parser.getAgents()) {
			Queue queue = queues.get(agent);

			// Instantiate the listener using reflection
			XCSPparser<AddableInteger, AddableReal> subprob = parser.getSubProblem(agent);
			queue.setProblem(subprob);
			Class<?> parTypes[] = new Class[3];
			parTypes = new Class[3];
			parTypes[0] = DCOPProblemInterface.class;
			parTypes[1] = Element.class;
			parTypes[2] = boolean.class;
			Constructor<?> constructor = Normalize.class.getConstructor(parTypes);
			Object[] args = new Object[3];
			args[0] = subprob;
			args[1] = parameters;
			args[2] = true;
			queue.addIncomingMessagePolicy((Normalize<AddableInteger>) constructor.newInstance(args));
		}

		startNormalization(graph, dfs, queues);

		// Wait until all agents have sent their outputs
		long start = System.currentTimeMillis();
		while (true) {
			synchronized (nbrMsgsRemaining_lock) {
				if (nbrMsgsRemaining == 0) {
					break;
				} else if (nbrMsgsRemaining < 0) {
					fail("Received too many output messages from the protocol");
				} else if (System.currentTimeMillis() - start > 5000) {
					fail("Timeout");
				}
			}
		}
		
		// Check that the lower bound is 0 and the upperbound is 1
		
		
		List<List<String>> components = graph.components;
		AddableReal zero = new AddableReal(0);
		AddableReal one = new AddableReal(1);
		for(List<String> component : components) {
			AddableReal lower = new AddableReal(0);
			AddableReal upper = new AddableReal(0);
			for(String var : component) {
				for(UtilitySolutionSpace<?, AddableReal> space : spaces.get(var)) {
					lower = lower == null ? space.blindProjectAll(false) : lower.add(space.blindProjectAll(false));
					upper = upper == null ? space.blindProjectAll(true) : upper.add(space.blindProjectAll(true));		
				}
			}
			if(!lower.equals(upper)) {
				assertTrue(lower.compareTo(zero, 10e-6) >= 0);
				assertTrue(upper.compareTo(one, 10e-6) <= 0);
			}
		}
		
		// Properly close the pipes
		for (QueueOutputPipeInterface pipe : pipes.values()) {
			pipe.close();
		}
		myQueue.end();
	}
	
	/** Sends messages to the queues to initiate O-DPOP
	 * @param graph 		the constraint graph
	 * @param dfs 			the corresponding DFS (for each node in the graph, the relationships of this node)
	 * @param queues 		the array of queues, indexed by the names of the clusters in the graph
	 */
	public static void startNormalization(RandGraphFactory.Graph graph, Map<String, DFSview<AddableInteger, AddableReal>> dfs, 
			Map<String, Queue> queues) {
		
		// To every agent, send its part of the DFS and extract from the problem definition the constraint it is responsible for enforcing
		for (Map.Entry<String, Queue> entry : queues.entrySet()) {
			Queue queue = entry.getValue();
			// Send the start message to this agent
			Message msg = new Message (AgentInterface.START_AGENT);
			queue.sendMessageToSelf(msg);

			// Extract the agent's part of the DFS and the constraint it is responsible for enforcing
			List<String> variables = graph.clusters.get(entry.getKey());
			for (String var : variables) {
				// Extract and send the relationships for this variable
				msg = new DFSgeneration.MessageDFSoutput<AddableInteger, AddableReal> (var, dfs.get(var));
				queue.sendMessageToSelf(msg);
			}
		}
	}

	/** 
	 * @see frodo2.communication.MessageListener#setQueue(frodo2.communication.Queue)
	 */
	@Override
	public void setQueue(Queue queue) {}

	/** 
	 * @see frodo2.communication.MessageListener#getMsgTypes()
	 */
	@Override
	public Collection<MessageType> getMsgTypes() {
		ArrayList<MessageType> msgTypes = new ArrayList<MessageType>(1);
		msgTypes.add(Normalize.OUT_MSG_TYPE);
		return msgTypes;
	}

	/** 
	 * @see frodo2.communication.IncomingMsgPolicyInterface#notifyIn(frodo2.communication.Message)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void notifyIn(Message msg) {
			MessageType type = msg.getType();
			
			if(type.equals(Normalize.OUT_MSG_TYPE)) {
				OUTmsg<AddableInteger> msgCast = (OUTmsg<AddableInteger>)msg;
				
				if(spaces == null)
					spaces = new HashMap<String, ArrayList<UtilitySolutionSpace<AddableInteger, AddableReal>>>();
				
				spaces.put(msgCast.getVariable(), msgCast.getSpaces());
				
				synchronized (nbrMsgsRemaining_lock) {
					nbrMsgsRemaining--;
				}
				
			}
		
	}
	
	

}
