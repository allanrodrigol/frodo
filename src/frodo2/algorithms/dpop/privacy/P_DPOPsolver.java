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

/** Classes implementing the P-DPOP and P2-DPOP algorithms that preserve privacy */
package frodo2.algorithms.dpop.privacy;

import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;

import frodo2.algorithms.XCSPparser;
import frodo2.algorithms.dpop.DPOPsolver;
import frodo2.algorithms.varOrdering.election.SecureVarElection;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableInteger;
import frodo2.solutionSpaces.crypto.AddableBigInteger;


/**
 * A DCOP solver using P-DPOP
 * @author Eric Zbinden, Thomas Leaute
 * @param <V> type used for variable values
 */
public class P_DPOPsolver< V extends Addable<V> > extends DPOPsolver<V, AddableBigInteger> {

	/**
	 * Default Constructor
	 */
	@SuppressWarnings("unchecked")
	public P_DPOPsolver(){
		super("/frodo2/algorithms/dpop/privacy/P-DPOPagent.xml", (Class<V>) AddableInteger.class, AddableBigInteger.class);
	}
	
	/** Constructor
	 * @param useTCP 		whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	@SuppressWarnings("unchecked")
	public P_DPOPsolver(boolean useTCP){
		super("/frodo2/algorithms/dpop/privacy/P-DPOPagent.xml", (Class<V>) AddableInteger.class, AddableBigInteger.class, useTCP);
	}
	
	/** Constructor 
	 * @param domClass 	the class to use for variable values
	 */
	public P_DPOPsolver (Class<V> domClass) {
		this(domClass, false);
	}
	
	/** Constructor 
	 * @param domClass 		the class to use for variable values
	 * @param useTCP 		whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public P_DPOPsolver (Class<V> domClass, boolean useTCP) {
		super ("/frodo2/algorithms/dpop/privacy/P-DPOPagent.xml", useTCP);
		this.setDomClass(domClass);
		this.setUtilClass(AddableBigInteger.class);
	}
	
	/** Constructor
	 * @param agentDescFile description of the agent to be used
	 * @param domClass 		the class to be used for variable values
	 */
	public P_DPOPsolver(String agentDescFile, Class<V> domClass) {
		super(agentDescFile, domClass, AddableBigInteger.class);
	}

	/** Constructor
	 * @param agentDescFile description of the agent to be used
	 * @param domClass 		the class to be used for variable values
	 * @param useTCP 		whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public P_DPOPsolver(String agentDescFile, Class<V> domClass, boolean useTCP) {
		super(agentDescFile, domClass, AddableBigInteger.class, useTCP);
	}

	/**
	 * Constructor
	 * @param filename	the location of the agent description file
	 */
	public P_DPOPsolver(String filename) {
		this(filename, false);
	}
	
	/**
	 * Constructor
	 * @param filename	the location of the agent description file
	 * @param useTCP 	whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	@SuppressWarnings("unchecked")
	public P_DPOPsolver(String filename, boolean useTCP) {
		super(filename, (Class<V>) AddableInteger.class, AddableBigInteger.class, useTCP);
	}
	
	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 */
	public P_DPOPsolver (Document agentDesc) {
		super (agentDesc);
	}
	
	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 * @param useTCP 	whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public P_DPOPsolver (Document agentDesc, boolean useTCP) {
		super (agentDesc, useTCP);
	}
	
	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 * @param parserClass	the class used to parse problems	
	 */
	public P_DPOPsolver (Document agentDesc, Class< ? extends XCSPparser<V, AddableBigInteger> > parserClass) {
		super (agentDesc, parserClass);
	}
	
	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 * @param parserClass	the class used to parse problems	
	 * @param useTCP 	whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public P_DPOPsolver (Document agentDesc, Class< ? extends XCSPparser<V, AddableBigInteger> > parserClass, boolean useTCP) {
		super (agentDesc, parserClass, useTCP);
	}
	
	/** @see DPOPsolver#setNbrElectionRounds(int) */
	@Override
	protected void setNbrElectionRounds (int nbrElectionRounds) {
		
		for (Element module : (List<Element>) agentDesc.getRootElement().getChild("modules").getChildren()) 
			if (module.getAttributeValue("className").equals(SecureVarElection.class.getName())) 
				module.setAttribute("minNbrLies", Integer.toString(nbrElectionRounds));
	}
	
	/** @see DPOPsolver#setUtilClass(java.lang.Class) */
	@Override
	public void setUtilClass(Class<AddableBigInteger> utilClass) {
		assert AddableBigInteger.class.equals(utilClass) : "Unsupported utility class " + utilClass + 
			"; only " + AddableBigInteger.class + " is supported";
		super.setUtilClass(utilClass);
	}

}
