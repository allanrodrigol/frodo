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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;

import junit.extensions.RepeatedTest;
import junit.framework.TestSuite;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import frodo2.algorithms.AgentInterface;
import frodo2.algorithms.RandGraphFactory;
import frodo2.algorithms.XCSPparser;
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
import frodo2.communication.MessageType;
import frodo2.communication.Queue;
import frodo2.communication.QueueOutputPipeInterface;
import frodo2.communication.mailer.CentralMailer;
import frodo2.solutionSpaces.AddableInteger;
import frodo2.solutionSpaces.AddableReal;

/** JUnit test for DUCT
 * @author Thomas Leaute
 * @todo Many tests should inherit this class to favor code reuse. 
 */
public class DUCTagentPruningSearchTest extends DUCTagentTest {

	
	
	/** Creates a JUnit test case corresponding to the input method
	 * @param useTCP 			whether TCP pipes should be used for communication between agents
	 * @param useCentralMailer	\c true when the central mailer should be tested
	 * @param measureMsgs 			whether to measure message numbers and sizes
	 * @param samplingMethodClass 	The sampling method to be used
	 * @param termination 			The termination method to be used
	 * @param bound 				The bound to be used

	 */
	@SuppressWarnings("unchecked")
	public DUCTagentPruningSearchTest(boolean useTCP, boolean useCentralMailer, boolean measureMsgs, String samplingMethodClass, String termination, String bound) {
		this(useTCP, useCentralMailer, (Class<? extends XCSPparser<AddableInteger, AddableReal>>) new XCSPparser<AddableInteger, AddableReal>().getClass(), measureMsgs, samplingMethodClass, termination, bound);
	}
	
	/** Creates a JUnit test case corresponding to the input method
	 * @param useTCP 				whether TCP pipes should be used for communication between agents
	 * @param useCentralMailer		\c true when the central mailer should be tested
	 * @param parserClass 			the class of the parser/subsolver
	 * @param measureMsgs 			whether to measure message numbers and sizes
	 * @param samplingMethod	The sampling method to be used
	 * @param termination 			The termination method to be used
	 * @param bound 				The bound to be used

	 */
	public DUCTagentPruningSearchTest(boolean useTCP, boolean useCentralMailer, Class< ? extends XCSPparser<AddableInteger, AddableReal> > parserClass, boolean measureMsgs, String samplingMethod, String termination, String bound) {
		super (useTCP, useCentralMailer, parserClass, measureMsgs, samplingMethod, termination, bound);
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
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TB log bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TB log bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TM log bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TM log+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TB log+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TB log+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TM log+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundLogSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TM  sqrt bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TB sqrt bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TB sqrt bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TM sqrt bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrt.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TM sqrt+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingM.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SM_TB sqrt+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingM.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TB sqrt+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingB.class.getName(), TerminateBest.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SB_TM sqrt+size bounds");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingB.class.getName(), TerminateMean.class.getName(), BoundSqrtSize.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SR_TB");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingR.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingR.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingR.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingR.class.getName(), TerminateBest.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		agentSuite = new TestSuite("SR_TM");
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, false, false, SamplingR.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, false, SamplingR.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using QueueIOPipes with integer-valued utilities and the central mailer and measuring messages");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (false, true, true, SamplingR.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		tmp = new TestSuite ("Tests using TCP pipes with integer-valued utilities");
		tmp.addTest(new RepeatedTest (new DUCTagentPruningSearchTest (true, false, false, SamplingR.class.getName(), TerminateMean.class.getName(), BoundLog.class.getName()), 25));
		agentSuite.addTest(tmp);
		
		suite.addTest(agentSuite);
		
		return suite;
	}

	/** @see junit.framework.TestCase#setUp() */
	public void setUp () throws Exception {
		
		agentConfig = XCSPparser.parse("src/frodo2/algorithms/duct/DUCTagentPruningSearch.xml", false);
		
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
			if(module.getAttributeValue("className").equals("frodo2.algorithms.duct.SamplingPruningSearch")) {
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
	
}
