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

/** Tests for the MPC-Dis(W)CSP algorithms */
package frodo2.algorithms.mpc_discsp.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jdom2.Document;
import org.jdom2.Element;

import frodo2.algorithms.Solution;
import frodo2.algorithms.XCSPparser;
import frodo2.algorithms.dpop.DPOPsolver;
import frodo2.algorithms.mpc_discsp.MPC_DisCSP4;
import frodo2.algorithms.mpc_discsp.MPC_DisWCSP4;
import frodo2.algorithms.mpc_discsp.MPC_DisWCSP4solver;
import frodo2.algorithms.reformulation.ProblemRescaler;
import frodo2.algorithms.test.AllTests;
import frodo2.solutionSpaces.AddableInteger;
import frodo2.solutionSpaces.UtilitySolutionSpace;
import junit.extensions.RepeatedTest;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/** JUnit tests for the MPC-Dis(W)CSP4 algorithms
 * @author Thomas Leaute
 */
public class MPC_DisWCSP4tests extends TestCase {
	
	/** The costs in each constraints take values in [0, costAmplitude], allowing infinity */
	private final int costAmplitude;
	
	/** @return a suite of tests with randomized input problem instances */
	public static TestSuite suite () {
		
		TestSuite suite = new TestSuite ("All tests for MPC-Dis[W]CSP4");
		
		TestSuite tmp = new TestSuite ("Tests for MPC-DisCSP4");
		tmp.addTest(new RepeatedTest (new MPC_DisWCSP4tests (false, false, +1, false), 200));
		suite.addTest(tmp);
		
		tmp = new TestSuite ("Tests for MPC-DisCSP4 with TCP pipes");
		tmp.addTest(new RepeatedTest (new MPC_DisWCSP4tests (false, false, +1, true), 200));
		suite.addTest(tmp);
		
		tmp = new TestSuite ("Tests for MPC-DisWCSP4 on minimization problems with non-negative costs");
		tmp.addTest(new RepeatedTest (new MPC_DisWCSP4tests (true, false, +1, false), 200));
		suite.addTest(tmp);
		
		tmp = new TestSuite ("Tests for MPC-DisWCSP4 on minimization problems with non-negative costs with TCP pipes");
		tmp.addTest(new RepeatedTest (new MPC_DisWCSP4tests (true, false, +1, true), 200));
		suite.addTest(tmp);
		
		tmp = new TestSuite ("Tests for MPC-DisWCSP4 on minimization problems with arbitrarily signed costs");
		tmp.addTest(new RepeatedTest (new MPC_DisWCSP4tests (true, false, 0, false), 200));
		suite.addTest(tmp);
		
		tmp = new TestSuite ("Tests for MPC-DisWCSP4 on maximization problems with non-positive utilities");
		tmp.addTest(new RepeatedTest (new MPC_DisWCSP4tests (true, true, -1, false), 200));
		suite.addTest(tmp);
		
		tmp = new TestSuite ("Tests for MPC-DisWCSP4 on maximization problems with arbitrarily signed utilities");
		tmp.addTest(new RepeatedTest (new MPC_DisWCSP4tests (true, true, 0, false), 200));
		suite.addTest(tmp);
		
		return suite;
	}
	
	/** A random DCOP instance */
	private Document problem;
	
	/** The agent configuration file */
	private Document agentConfig;

	/** Whether to test on maximization or minimization problems */
	private final boolean maximize;

	/** The sign of the utilities/costs */
	private final int sign;

	/** Whether to use TCP pipes */
	private final boolean tcp;

	/** Constructor 
	 * @param w 			if true, use MPC-DisWCSP4
	 * @param maximize 	whether to test on maximization or minimization problems
	 * @param sign 		the sign of the utilities/costs
	 * @param tcp 		whether to use TCP pipes
	 */
	public MPC_DisWCSP4tests(boolean w, boolean maximize, int sign, boolean tcp) {
		super ("test");
		this.costAmplitude = (w ? 5 : 0);
		this.maximize = maximize;
		this.sign = sign;
		this.tcp = tcp;
		try {
			this.agentConfig = XCSPparser.parse("src/frodo2/algorithms/mpc_discsp/MPC-Dis" + (w ? "W" : "") + "CSP4.xml", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** @see junit.framework.TestCase#setUp() */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.problem = AllTests.createRandProblem(3, 3, 3, this.maximize, this.sign, costAmplitude);
	}

	/** @see junit.framework.TestCase#tearDown() */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.problem = null;
	}
	
	/** The test method */
	public void test () {
		
		// Solve the problem using DPOP
		AddableInteger optCost = new DPOPsolver<AddableInteger, AddableInteger> ().solve(problem).getUtility();
		
		// Construct the list of possible constraint owners
		XCSPparser<AddableInteger, AddableInteger> parser = new XCSPparser<AddableInteger, AddableInteger> (this.problem);
		ArrayList<String> owners = new ArrayList<String> (parser.getAgents()); // all agents owning at least one variable
		owners.add(null); // no specified owner
		owners.add("PUBLIC"); // public constraint
		Random rnd = new Random();
		String randOwner = Integer.toHexString(rnd.nextInt()); // an agent owning no variable
		Element agentsElmt = this.problem.getRootElement().getChild("agents");
		Element elmt = new Element ("agent");
		agentsElmt.addContent(elmt);
		elmt.setAttribute("name", randOwner);
		agentsElmt.setAttribute("nbAgents", Integer.toString(agentsElmt.getContentSize()));
		owners.add(randOwner);
		final int nbrOwners = owners.size();
		
		// Add random owners to constraints
		for (Element constElmt : (List<Element>) this.problem.getRootElement().getChild("constraints").getChildren()) {
			String owner = owners.get(rnd.nextInt(nbrOwners));
			if (owner != null) 
				constElmt.setAttribute("agent", owner);
		}
		
		// Compute the required ProblemRescaler shift
		int shift = 0;
		for (UtilitySolutionSpace<AddableInteger, AddableInteger> space : parser.getSolutionSpaces()) {
			if (maximize) 
				shift = Math.max(shift, Math.max(0, space.blindProjectAll(true).intValue()));
			else 
				shift = Math.max(shift, - Math.min(0, space.blindProjectAll(false).intValue()));
		}
		
		// Set the module parameters 
		for (Element module : (List<Element>) agentConfig.getRootElement().getChild("modules").getChildren()) {
			String className = module.getAttributeValue("className");
			
			if (className.equals(ProblemRescaler.class.getName())) 
				module.setAttribute("shift", Integer.toString(shift));
			
			else if (className.equals(MPC_DisWCSP4.class.getName()) || className.equals(MPC_DisCSP4.class.getName())) 
				module.setAttribute("nbrBits", "64"); // to speed up the tests
		}
		
		// Solve the problem using MPC-DisWCSP4
		final int nbrConstraints = parser.getSolutionSpaces().size();
		Solution<AddableInteger, AddableInteger> sol = new MPC_DisWCSP4solver<AddableInteger, AddableInteger> (this.agentConfig, this.tcp)
			.solve(problem, false, 60000L, (shift + costAmplitude) * nbrConstraints, 
					(shift + costAmplitude) * nbrConstraints * parser.getAgents().size());
		assertNotNull("timeout", sol); /// @bug Rarely times out
		
		assertEquals(optCost, sol.getUtility()); /// @bug Very rarely fails
	}

}
