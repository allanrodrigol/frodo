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

package frodo2.algorithms.dpop.privacy.test;

import java.io.IOException;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import junit.extensions.RepeatedTest;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import frodo2.algorithms.AgentFactory;
import frodo2.algorithms.Solution;
import frodo2.algorithms.XCSPparser;
import frodo2.algorithms.dpop.DPOPsolver;
import frodo2.algorithms.dpop.privacy.CollaborativeDecryption;
import frodo2.algorithms.dpop.privacy.EncryptedUTIL;
import frodo2.algorithms.dpop.privacy.P2_DPOPsolver;
import frodo2.algorithms.dpop.privacy.test.FakeCryptoScheme.FakeEncryptedInteger;
import frodo2.algorithms.reformulation.ProblemRescaler;
import frodo2.algorithms.test.AllTests;
import frodo2.algorithms.varOrdering.dfs.DFSgenerationWithOrder;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableInteger;
import frodo2.solutionSpaces.AddableLimited;
import frodo2.solutionSpaces.AddableReal;
import frodo2.solutionSpaces.UtilitySolutionSpace;
import frodo2.solutionSpaces.crypto.CryptoScheme;
import frodo2.solutionSpaces.crypto.ElGamalBigInteger;
import frodo2.solutionSpaces.crypto.ElGamalScheme;

/**
 * JUnit test for P_DPOP agent
 * @author Eric Zbinden, Thomas Leaute
 * @param <V> the type used for variable values
 * @param <E> The class used for encrypted values
 */
public class P2_DPOPagentTest < V extends Addable<V>, E extends AddableLimited<AddableInteger, E> > extends TestCase {
	
	
	/**
	 * The maximum number of variables in this problem
	 */
	private int maxVar = 5;
	
	/**
	 * The maximum number of agents in this problem
	 */
	private int maxAgent = 5;
	
	/**
	 * The maximum number of constraints in this problem
	 */
	private int maxEdge = 10;
	
	/** The class of the CryptoScheme */
	private Class< ? extends CryptoScheme<AddableInteger, E, ?> > schemeClass;
	
	/** The class used for variable values */
	private Class<V> domClass;
	
	/** The class used for encrypted values */
	private Class<E> classOfE;
	
	/** Whether to enable the merging of back-edges */
	private final boolean mergeBack;

	/** Whether to minimize the NCCC */
	private final boolean minNCCCs;
	
	/** Whether to use TCP pipes */
	private final boolean useTCP;
	
	/** Whether to test on a maximization or a minimization problem */
	private final boolean maximize;
	
	/** The sign of the costs/utilities in the test problem instance */
	private final int sign;
	
	/**
	 * Constructor
	 * @param schemeClass 	The class of the CryptoScheme
	 * @param domClass 		The class used for variable values
	 * @param classOfE 		The class used for encrypted values
	 * @param mergeBack 		Whether to enable the merging of back-edges
	 * @param minNCCCs 		Whether to minimize the NCCC
	 * @param useTCP 		Whether to use TCP pipes
	 * @param maximize 		Whether to test on a maximization or a minimization problem
	 * @param sign 			The sign of the costs/utilities in the test problem instance
	 */
	public P2_DPOPagentTest(Class<V> domClass, Class< ? extends CryptoScheme<AddableInteger, E, ?> > schemeClass, Class<E> classOfE, 
			boolean mergeBack, boolean minNCCCs, boolean useTCP, boolean maximize, int sign) {
		super("testP2DPOPvsDPOP");
		this.domClass = domClass;
		this.schemeClass = schemeClass;
		this.classOfE = classOfE;
		this.mergeBack = mergeBack;
		this.minNCCCs = minNCCCs;
		this.useTCP = useTCP;
		this.maximize = maximize;
		this.sign = sign;
	}
	
	/**
	 * Constructor
	 * @param schemeClass 	The class of the CryptoScheme
	 * @param domClass 		The class used for variable values
	 * @param classOfE 		The class used for encrypted values
	 * @param mergeBack 	Whether to enable the merging of back-edges
	 * @param minNCCCs 		Whether to minimize the NCCC
	 * @param useTCP 		Whether to use TCP pipes
	 */
	public P2_DPOPagentTest(Class<V> domClass, Class< ? extends CryptoScheme<AddableInteger, E, ?> > schemeClass, Class<E> classOfE, 
			boolean mergeBack, boolean minNCCCs, boolean useTCP) {
		this (domClass, schemeClass, classOfE, mergeBack, minNCCCs, useTCP, false, +1);
	}

	/** @return the test suite for this test */
	public static TestSuite suite () {
		TestSuite testSuite = new TestSuite ("Tests for P2DPOP agent");
		
		TestSuite testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with FakeCryptoScheme with mergeBack");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableInteger, FakeEncryptedInteger> (AddableInteger.class, FakeCryptoScheme.class, FakeEncryptedInteger.class, true, false, false), 12000));
		testSuite.addTest(testTmp);
		
		testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with FakeCryptoScheme with mergeBack and TCP pipes");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableInteger, FakeEncryptedInteger> (AddableInteger.class, FakeCryptoScheme.class, FakeEncryptedInteger.class, true, false, true), 100));
		testSuite.addTest(testTmp);
		
		testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with FakeCryptoScheme with mergeBack and with minNCCCs");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableInteger, FakeEncryptedInteger> (AddableInteger.class, FakeCryptoScheme.class, FakeEncryptedInteger.class, true, true, false), 12000));
		testSuite.addTest(testTmp);
		
		testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with FakeCryptoScheme with mergeBack with real-valued variables");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableReal, FakeEncryptedInteger> (AddableReal.class, FakeCryptoScheme.class, FakeEncryptedInteger.class, true, false, false), 25000));
		testSuite.addTest(testTmp);
		
		testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with FakeCryptoScheme without mergeBack");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableInteger, FakeEncryptedInteger> (AddableInteger.class, FakeCryptoScheme.class, FakeEncryptedInteger.class, false, false, false), 5000));
		testSuite.addTest(testTmp);
		
		testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with ElGamalScheme with mergeBack");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableInteger, ElGamalBigInteger> (AddableInteger.class, ElGamalScheme.class, ElGamalBigInteger.class, true, false, false), 250));
		testSuite.addTest(testTmp);
		
		testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with ElGamalScheme with mergeBack and TCP pipes");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableInteger, ElGamalBigInteger> (AddableInteger.class, ElGamalScheme.class, ElGamalBigInteger.class, true, false, true), 100));
		testSuite.addTest(testTmp);
		
		testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with ElGamalScheme without mergeBack");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableInteger, ElGamalBigInteger> (AddableInteger.class, ElGamalScheme.class, ElGamalBigInteger.class, false, false, false), 250));
		testSuite.addTest(testTmp);
		
		testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with ElGamalScheme without mergeBack on maximization problems with negative utilities");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableInteger, ElGamalBigInteger> (AddableInteger.class, ElGamalScheme.class, ElGamalBigInteger.class, false, false, false, true, -1), 250));
		testSuite.addTest(testTmp);
		
		testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with ElGamalScheme without mergeBack on minimization problems with arbitrarily signed costs");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableInteger, ElGamalBigInteger> (AddableInteger.class, ElGamalScheme.class, ElGamalBigInteger.class, false, false, false, false, 0), 250));
		testSuite.addTest(testTmp);
		
		testTmp = new TestSuite ("Tests for P2DPOP vs DPOP with ElGamalScheme without mergeBack on maximization problems with arbitrarily signed utilities");
		testTmp.addTest(new RepeatedTest (new P2_DPOPagentTest<AddableInteger, ElGamalBigInteger> (AddableInteger.class, ElGamalScheme.class, ElGamalBigInteger.class, false, false, false, true, 0), 250));
		testSuite.addTest(testTmp);
		
		return testSuite;
	}
	
	/**
	 * Test whenever P-DPOP's and DPOP's answers to a random problem are equivalent
	 * @throws IOException is thrown if an I/O exception occur when accessing to the description of P-DPOP or DPOP algorithm
	 * @throws JDOMException is thrown if a parsing error occurs
	 */
	public void testP2DPOPvsDPOP () throws JDOMException, IOException {
		
		//Create new random problem
		Document problem = AllTests.createRandProblem(maxVar, maxEdge, maxAgent, maximize, sign, 0.5);
		XCSPparser<V, AddableInteger> parser = new XCSPparser<V, AddableInteger>(problem);
		
		// Compute the required ProblemRescaler shift
		int shift = 0;
		for (UtilitySolutionSpace<V, AddableInteger> space : parser.getSolutionSpaces()) {
			if (maximize) 
				shift = Math.max(shift, Math.max(0, space.blindProjectAll(true).intValue()));
			else 
				shift = Math.max(shift, - Math.min(0, space.blindProjectAll(false).intValue()));
		}
		
		// Set the CryptoScheme and the mergeBack and minNCCCs flags
		Document agentDesc = XCSPparser.parse(AgentFactory.class.getResourceAsStream("/frodo2/algorithms/dpop/privacy/P2-DPOPagent.xml"), false);
		for (Element module : (List<Element>) agentDesc.getRootElement().getChild("modules").getChildren()) {
			String className = module.getAttributeValue("className");
			if (className.equals(CollaborativeDecryption.class.getName())) {
				
				Element schemeElmt = module.getChild("cryptoScheme");
				schemeElmt.setAttribute("className", this.schemeClass.getName());
				
				// Use small numbers of bits for the modulus and the generator to speed up the tests
				schemeElmt.setAttribute("modulus", "57475322849086478933");
				schemeElmt.setAttribute("generator", "5526868997990728076");
				
				schemeElmt.setAttribute("infinity", Integer.toString(1000 + shift));
				break;
				
			} else if (className.equals(EncryptedUTIL.class.getName())) {
				module.setAttribute("mergeBack", Boolean.toString(this.mergeBack));
				module.setAttribute("minNCCCs", Boolean.toString(this.minNCCCs));
				
			} else if (className.equals(DFSgenerationWithOrder.class.getName())) 
				module.setAttribute("minIncr", "2");
			
			else if (className.equals(ProblemRescaler.class.getName())) 
				module.setAttribute("shift", Integer.toString(shift));
		}
		
		//Compute both solutions
		Solution<V, AddableInteger> p2dpopSolution = new P2_DPOPsolver<V, AddableInteger>(agentDesc, this.domClass, AddableInteger.class, this.classOfE, this.useTCP)
			.solve(problem, parser.getNbrVars(), 240000L);
		Solution<V, AddableInteger> dpopSolution = new DPOPsolver<V, AddableInteger>(this.domClass, AddableInteger.class).solve(problem, parser.getNbrVars());
		
		assertNotNull ("P2-DPOP timed out", p2dpopSolution);
				
		//Verify the utilities of the solutions found by P-DPOP and DPOP
		assertEquals("P2-DPOP's and DPOP's utilities are different", dpopSolution.getUtility(), p2dpopSolution.getUtility());
		
		// Verify that P2DPOP's chosen assignments indeed have the correct utility
		if (! this.domClass.equals(AddableReal.class)) 
			assertEquals("The chosen assignments' utility differs from the actual utility", 
					p2dpopSolution.getUtility(), parser.getUtility(p2dpopSolution.getAssignments()).getUtility(0));
		
		// Check that the reported utility is correct 
		AddableInteger rescaled = p2dpopSolution.getReportedUtil().add(new AddableInteger (- shift * parser.getSolutionSpaces().size()));
		if (this.maximize) 
			rescaled = rescaled.multiply(new AddableInteger (-1));
		assertEquals("The actual utility differs from the reported utility", p2dpopSolution.getUtility(), rescaled);		
	}
}
