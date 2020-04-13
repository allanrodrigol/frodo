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

/** All tests for the ASO-DPOP algorithm */
package frodo2.algorithms.asodpop.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/** Tests for the ASODPOP algorithm */
public class AllTestsASODPOP extends TestCase {
	
	/** @return The suite of unit tests */
	public static Test suite() {
		TestSuite suite = new TestSuite("All tests in frodo2.algorithms.asodpop.test");
		//$JUnit-BEGIN$
		suite.addTest(ASODPOPTest.suite());
		suite.addTest(ASODPOPBinaryTest.suite());
		suite.addTest(ASODPOPagentTest.suite());
		suite.addTest(ASODPOPBinaryAgentTest.suite());
		//$JUnit-END$
		return suite;
	}
}
