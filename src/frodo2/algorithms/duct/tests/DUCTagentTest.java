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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import junit.extensions.RepeatedTest;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import frodo2.algorithms.AgentFactory;
import frodo2.algorithms.AgentInterface;
import frodo2.algorithms.RandGraphFactory;
import frodo2.algorithms.XCSPparser;
import frodo2.algorithms.AgentInterface.ComStatsMessage;
import frodo2.algorithms.duct.Normalize;
import frodo2.algorithms.duct.Sampling;
import frodo2.algorithms.duct.bound.BoundLog;
import frodo2.algorithms.duct.bound.BoundLogSize;
import frodo2.algorithms.duct.bound.BoundSqrt;
import frodo2.algorithms.duct.bound.BoundSqrtSize;
import frodo2.algorithms.duct.samplingMethods.SamplingB;
import frodo2.algorithms.duct.samplingMethods.SamplingM;
import frodo2.algorithms.duct.samplingMethods.SamplingR;
import frodo2.algorithms.duct.termination.TerminateBest;
import frodo2.algorithms.duct.termination.TerminateMean;
import frodo2.algorithms.test.AllTests;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration;
import frodo2.communication.IncomingMsgPolicyInterface;
import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.MessageWith2Payloads;
import frodo2.communication.Queue;
import frodo2.communication.QueueOutputPipeInterface;
import frodo2.communication.mailer.CentralMailer;
import frodo2.communication.sharedMemory.QueueIOPipe;
import frodo2.controller.Controller;
import frodo2.controller.WhitePages;
import frodo2.daemon.LocalAgentReport;
import frodo2.solutionSpaces.AddableInteger;
import frodo2.solutionSpaces.AddableReal;
import frodo2.solutionSpaces.DCOPProblemInterface;

/** JUnit test for DUCT
 * @author Brammert Ottens
 * @todo Many tests should inherit this class to favor code reuse. 
 */
public class DUCTagentTest extends TestCase implements IncomingMsgPolicyInterface<MessageType> {

	/** Maximum number of variables in the problem 
	 * @note Must be at least 2. 
	 */
	protected final int maxNbrVars = 5;
	
	/** Maximum number of binary constraints in the problem */
	protected final int maxNbrEdges = 20;

	/** Maximum number of agents */
	protected final int maxNbrAgents = 5;
	
	/** The agent configuration file */
	protected Document agentConfig;

	/** The queue used to listen to the agents */
	protected Queue queue;
	
	/** For each agent, the output pipe to it */
	protected Map<Object, QueueOutputPipeInterface> pipes;
	
	/** The testers pipe */
	private QueueIOPipe pipe;
	
	/** All agents, indexed by their IDs */
	private Map< String, AgentInterface<AddableInteger> > agents;
	
	/** Random graph used to generate a constraint graph */
	protected RandGraphFactory.Graph graph;

	/** Total number of agents */
	private int nbrAgents;
	
	/** Used to track the number of various types of messages received from the agents */
	protected int nbrMsgsReceived;
	
	/** Number of agents finished */
	protected int nbrAgentsFinished;
	
	/** Used to make the test thread wait */
	private final ReentrantLock finished_lock = new ReentrantLock ();
	
	/** Used to wake up the test thread when all agents have finished */
	private final Condition finished = finished_lock.newCondition();

	/** \c true if the algorithm should be tested with the central mailer*/
	protected boolean useCentralMailer = false;
	
	/** Whether to measure message numbers and sizes */
	protected boolean measureMsgs;
	
	/** The XCSP random problem file */
	protected Document problemDoc;
	
	/** The class of the parser/subsolver to use */
	protected Class< ? extends XCSPparser<AddableInteger, AddableReal> > parserClass;
	
	/** The problem */
	protected DCOPProblemInterface<AddableInteger, AddableReal> problem;
	
	/** The module listening for the optimal utility to the problem */
	protected Normalize<AddableInteger> normModule;
	
	/** The module listening for the optimal assignment to the problem */
	protected Sampling<AddableInteger> samplingModule;
	
	/** The sampling method to be used */
	protected String samplingMethod;
	
	/** The termination method to be used */
	protected String terminationMethod;
	
	/** The bound to be used */
	protected String bound;

	/** Whether TCP pipes should be used for communication between agents */
	private boolean useTCP;

	/** The type of the start message */
	protected MessageType startMsgType;

	/** Whether we should maximize or minimize */
	protected boolean maximize = true;

	/** The CentralMailer */
	protected CentralMailer mailman;
	
	/** Creates a JUnit test case corresponding to the input method
	 * @param useTCP 			whether TCP pipes should be used for communication between agents
	 * @param useCentralMailer	\c true when the central mailer should be tested
	 * @param measureMsgs			\c true when message sizes have to be measured
	 * @param samplingMethodClass 	The sampling method to be used
	 * @param termination 			The termination method to be used
	 * @param bound 				The bound to be used
	 */
	@SuppressWarnings("unchecked")
	public DUCTagentTest(boolean useTCP, boolean useCentralMailer, boolean measureMsgs, String samplingMethodClass, String termination, String bound) {
		this(useTCP, useCentralMailer, (Class<? extends XCSPparser<AddableInteger, AddableReal>>) new XCSPparser<AddableInteger, AddableReal>().getClass(), measureMsgs, samplingMethodClass, termination, bound);
	}
	
	/** Creates a JUnit test case corresponding to the input method
	 * @param useTCP 				whether TCP pipes should be used for communication between agents
	 * @param useCentralMailer		\c true when the central mailer should be tested
	 * @param parserClass 			the class of the parser/subsolver
	 * @param measureMsgs 			whether to measure message numbers and sizes
	 * @param samplingMethod 	The sampling method to be used
	 * @param termination 			The termination method to be used
	 * @param bound 				The bound to be used
	 */
	public DUCTagentTest(boolean useTCP, boolean useCentralMailer, Class< ? extends XCSPparser<AddableInteger, AddableReal> > parserClass, boolean measureMsgs, String samplingMethod, String termination, String bound) {
		super ("testRandom");
		this.useTCP = useTCP;
		this.useCentralMailer = useCentralMailer;
		this.parserClass = parserClass;
		this.measureMsgs = measureMsgs;
		this.samplingMethod = samplingMethod;
		this.terminationMethod = termination;
		this.bound = bound;
	}
	
	/** Sets the type of the start message for all modules
	 * @param startMsgType 		the new type for the start message
	 * @throws JDOMException 	if parsing the agent configuration file failed
	 */
	protected void setStartMsgType (MessageType startMsgType) throws JDOMException {
		if (startMsgType != null) {
			this.startMsgType = startMsgType;
			for (Element module2 : (List<Element>) agentConfig.getRootElement().getChild("modules").getChildren()) {
				for (Element message : (List<Element>) module2.getChild("messages").getChildren()) {
					if (message.getAttributeValue("myFieldName").equals("START_MSG_TYPE") 
							&& message.getAttributeValue("targetFieldName").equals("START_AGENT")
							&& message.getAttributeValue("targetClass").equals(AgentInterface.class.getName())) {
						message.removeAttribute("targetFieldName");
						message.removeAttribute("targetClass");
						message.addContent(startMsgType.toXML());
					}
				}
			}
		} else 
			this.startMsgType = AgentInterface.START_AGENT;
	}

	/** @return the test suite */
	public static TestSuite suite () {
		TestSuite suite = new TestSuite ("Random tests for DUCT");
		
		TestSuite agentSuite = new TestSuite("SM_TM log bounds");
		TestSuite tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TB log bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TB log bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TM log bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TM log+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TB log+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TB log+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TM log+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TM  sqrt bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TB sqrt bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TB sqrt bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TM sqrt bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TM sqrt+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TB sqrt+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TB sqrt+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TM sqrt+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SR_TB");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingR.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingR.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingR.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingR.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SR_TM");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, false, false, SamplingR.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, false, SamplingR.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (false, true, true, SamplingR.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentTest (true, false, false, SamplingR.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		return suite;
	}
	
	/** @see junit.framework.TestCase#setUp() */
	public void setUp () throws Exception {
		
		agentConfig = XCSPparser.parse("src/frodo2/algorithms/duct/DUCTagent.xml", false);
		
		// Set the type of the start message
		this.setStartMsgType(startMsgType);
		
		// set the proper parser
		if(agentConfig.getRootElement().getChild("parser") != null)
			agentConfig.getRootElement().getChild("parser").setAttribute("parserClass", parserClass.getCanonicalName());
		else {
			Element elmt = new Element("parser");
			elmt.setAttribute("parserClass", parserClass.getCanonicalName());
			agentConfig.getRootElement().addContent(elmt);
		}
		
		// set the algorithm parameters
		Element modules = agentConfig.getRootElement().getChild("modules");
		for(Element module : (List<Element>)modules.getChildren()) {
			if(module.getAttributeValue("className").equals("frodo2.algorithms.duct.Sampling")) {
				module.setAttribute("samplingMethod", this.samplingMethod);
				module.setAttribute("bound", this.bound);
				module.setAttribute("terminationCondition", this.terminationMethod);
			}
		}
	
		nbrMsgsReceived = 0;
		nbrAgentsFinished = 0;
		
		// Create the queue
		if (this.useCentralMailer) {
			mailman = new CentralMailer (this.measureMsgs, false, null);
			this.queue = mailman.newQueue(AgentInterface.STATS_MONITOR);
		} else 
			queue = new Queue (false);
		
		queue.addIncomingMessagePolicy(this);
		pipes = new HashMap<Object, QueueOutputPipeInterface> ();
		graph = RandGraphFactory.getRandGraph(maxNbrVars, maxNbrEdges, maxNbrAgents);
		problemDoc = AllTests.generateProblem(graph, true, 0, false, 0.0); // for the moment it only works for problems without infeasible utilities
		
		
		
		// Instantiate the parser/subsolver
		Constructor< ? extends XCSPparser<AddableInteger, AddableReal> > constructor = this.parserClass.getConstructor(Document.class, Boolean.class);
		XCSPparser<AddableInteger, AddableReal> parser = constructor.newInstance(problemDoc, false);
		this.problem = parser;
		parser.setDomClass(AddableInteger.class);
		parser.setUtilClass(AddableReal.class);
		
//		normModule = new Normalize<AddableInteger> (null, parser);
//		normModule.setSilent(true);
//		normModule.getStatsFromQueue(queue);
		samplingModule = new Sampling<AddableInteger> (null, parser);
		samplingModule.setSilent(true);
		samplingModule.getStatsFromQueue(queue);
		
		DFSgeneration<AddableInteger, AddableReal> module = new DFSgeneration<AddableInteger, AddableReal> (null, parser);
		module.setSilent(true);
		module.getStatsFromQueue(queue);
	}
	
	/** @see junit.framework.TestCase#tearDown() */
	protected void tearDown () throws Exception {
		super.tearDown();
		if (this.useCentralMailer) 
			mailman.end();
		queue.end();
		for (QueueOutputPipeInterface pipe : pipes.values())
			pipe.close();
		pipe.close();
		pipe = null;
		pipes.clear();
		for (AgentInterface<AddableInteger> agent : agents.values()) 
			agent.kill();
		agents.clear();
		queue = null;
		pipes = null;
		agents = null;
		graph = null;
		problemDoc = null;
		problem = null;
		normModule = null;
		samplingModule = null;
		this.startMsgType = null;
	}
	
	/** Tests the DPOPagent on a random problem 
	 * @throws Exception if an error occurs
	 */
	public void testRandom () throws Exception {
		
		// Set up the input pipe for the queue
		pipe = new QueueIOPipe (queue);
		
		// Create the agent descriptions
		String useCentralMailerString = Boolean.toString(useCentralMailer);
		agentConfig.getRootElement().setAttribute("measureTime", useCentralMailerString);
		agentConfig.getRootElement().setAttribute("measureMsgs", Boolean.toString(this.measureMsgs));
		
		// Create the set of agents
		Set<String> agentsSet;
		// We want to test with potentially empty agents
		agentsSet = new HashSet<String> (graph.clusters.keySet());
		nbrAgents = agentsSet.size();
		
		// Go through the list of agents and instantiate them
		agents = new HashMap< String, AgentInterface<AddableInteger> > (nbrAgents);
		synchronized (agents) {
			if (useTCP) { // use TCP pipes
				int port = 5500;
				for (String agent : agentsSet) {
					DCOPProblemInterface<AddableInteger, AddableReal> subproblem = problem.getSubProblem(agent);
					agents.put(agent, AgentFactory.createAgent(pipe, pipe, subproblem, agentConfig, port++));
				}
			} else { // use QueueIOPipes
				for (String agent : agentsSet) {
					DCOPProblemInterface<AddableInteger, AddableReal> subproblem = problem.getSubProblem(agent);
					agents.put(agent, AgentFactory.createAgent(pipe, subproblem, agentConfig, mailman));
				}
			}
		}
		
		if (useCentralMailer) {
			if (! this.mailman.execute(15000))
				fail("Timeout"); /// @bug Often times out
		} else {
			// Wait for all agents to finish
			while (true) {
				this.finished_lock.lock();
				try {
					if (nbrAgentsFinished >= nbrAgents) {
						break;
					} else if (! this.finished.await(60, TimeUnit.SECONDS)) {
						fail("Timeout"); /// @bug Often times out
					}
				} catch (InterruptedException e) {
					break;
				}
				this.finished_lock.unlock();
			}
		}
		
		checkOutput();
	}

	/** Checks that the output of the algorithm is correct 
	 * @throws Exception if an error occurs
	 */
	protected void checkOutput() throws Exception {
		
	}

	/** @see IncomingMsgPolicyInterface#getMsgTypes() */
	public Collection<MessageType> getMsgTypes() {
		ArrayList<MessageType> types = new ArrayList<MessageType> (4);
		types.add(AgentInterface.LOCAL_AGENT_REPORTING);
		types.add(AgentInterface.LOCAL_AGENT_ADDRESS_REQUEST);
		types.add(AgentInterface.AGENT_CONNECTED);
		types.add(AgentInterface.AGENT_FINISHED);
		return types;
	}

	/** @see IncomingMsgPolicyInterface#notifyIn(Message) */
	@SuppressWarnings("unchecked")
	public void notifyIn(Message msg) {
		
		if (msg.getType().equals(AgentInterface.LOCAL_AGENT_REPORTING)) {
			LocalAgentReport msgCast = (LocalAgentReport) msg;
			String agentID = msgCast.getAgentID();
			QueueIOPipe pipe = msgCast.getLocalPipe();
			queue.addOutputPipe(agentID, pipe);
			
			// Create a TCP pipe if required
			int port = msgCast.getPort();
			if (port >= 0) {
				try {
					pipes.put(agentID, Controller.PipeFactoryInstance.outputPipe(Controller.PipeFactoryInstance.getSelfAddress(port)));
				} catch (UnknownHostException e) { // should never happen
					e.printStackTrace();
					fail();
				} catch (IOException e) {
					e.printStackTrace();
					fail();
				}
			} else 
				pipes.put(agentID, pipe);
			
			// Check if all agents have reported, and if so, tell them to connect
			if (pipes.size() >= nbrAgents) {
				synchronized (agents) { // synchronize so we don't tell the agents to connect before the list of agents is ready
					queue.sendMessageToMulti(pipes.keySet(), new Message (WhitePages.CONNECT_AGENT));
				}
			}
		}
		
		else if (msg.getType().equals(AgentInterface.LOCAL_AGENT_ADDRESS_REQUEST)) {
			MessageWith2Payloads<String, String> msgCast = (MessageWith2Payloads<String, String>) msg;
			String recipient = msgCast.getPayload2();
			agents.get(msgCast.getPayload1()).addOutputPipe(recipient, pipes.get(recipient));
		}
		
		else if (msg.getType().equals(AgentInterface.AGENT_CONNECTED)) {
			if (++nbrMsgsReceived >= nbrAgents) { // all agents are now connected; tell them to start
				queue.sendMessageToMulti(pipes.keySet(), new Message (startMsgType));
			}
		}
		
		else if (msg.getType().equals(AgentInterface.AGENT_FINISHED)) {
			if (this.useCentralMailer) 
				assertTrue (queue.getCurrentMessageWrapper().getTime() >= 0);
			
			this.finished_lock.lock();
			if (++nbrAgentsFinished >= this.nbrAgents) 
				this.finished.signal();
			this.finished_lock.unlock();
		}
		
		else if (msg.getType().equals(AgentInterface.ComStatsMessage.COM_STATS_MSG_TYPE) 
				&& this.measureMsgs) {
			
			ComStatsMessage msgCast = (ComStatsMessage) msg;
			assertFalse (msgCast.getMsgNbrs() == null);
			assertFalse (msgCast.getMsgSizes() == null);
			assertFalse (msgCast.getMaxMsgSizes() == null);
		}
		
	}

	/** Does nothing
	 * @see IncomingMsgPolicyInterface#setQueue(Queue)
	 */
	public void setQueue(Queue queue) { }
	
}
