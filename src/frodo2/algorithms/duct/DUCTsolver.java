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

package frodo2.algorithms.duct;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.jdom2.Document;

import frodo2.algorithms.AbstractDCOPsolver;
import frodo2.algorithms.Solution;
import frodo2.algorithms.SolutionCollector;
import frodo2.algorithms.StatsReporter;
import frodo2.algorithms.duct.Sampling;
import frodo2.algorithms.duct.DUCTSolution;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration;
import frodo2.communication.MessageType;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableReal;
import frodo2.solutionSpaces.UtilitySolutionSpace;

/** A DCOP solver using DUCT
 * @author Brammert Ottens, Thomas Leaute
 * @param <V> type used for variable values
 */
public class DUCTsolver< V extends Addable<V>> 
extends AbstractDCOPsolver< V, AddableReal, Solution<V, AddableReal> > {

	/** The util propagation phase listener*/
	protected Sampling<V> samplingModule;
	
	/** The DFSgeneration module */
	protected DFSgeneration<V, AddableReal> dfsModule;
	
	/** The solution collector */
	private SolutionCollector<V, AddableReal> solCollector;

	/** Default constructor */
	public DUCTsolver () {
		super ("/frodo2/algorithms/duct/DUCTagent.xml");
	}
	
	/** Constructor 
	 * @param useTCP 			Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public DUCTsolver (boolean useTCP) {
		super ("/frodo2/algorithms/duct/DUCTagent.xml", useTCP);
	}
	
	/** Constructor 
	 * @param agentDesc 	the agent description
	 */
	public DUCTsolver (String agentDesc) {
		super (agentDesc);
	}
	
	/** Constructor 
	 * @param agentDesc 	the agent description
	 * @param useTCP 			Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public DUCTsolver (String agentDesc, boolean useTCP) {
		super (agentDesc, useTCP);
	}
	
	/** Constructor 
	 * @param agentDesc 	the agent description
	 */
	public DUCTsolver (Document agentDesc) {
		super (agentDesc);
	}
	
	/** Constructor 
	 * @param agentDesc 	the agent description
	 * @param useTCP 			Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public DUCTsolver (Document agentDesc, boolean useTCP) {
		super (agentDesc, useTCP);
	}
	
	/** Constructor 
	 * @param domClass 		the class to use for variable values
	 * @param utilClass 	the class to use for utilities
	 */
	public DUCTsolver (Class<V> domClass, Class<AddableReal> utilClass) {
		super ("/frodo2/algorithms/duct/DUCTagent.xml");
		this.setDomClass(domClass);
		this.setUtilClass(utilClass);
	}
	
	/** Constructor 
	 * @param domClass 		the class to use for variable values
	 * @param utilClass 	the class to use for utilities
	 * @param useTCP 			Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public DUCTsolver (Class<V> domClass, Class<AddableReal> utilClass, boolean useTCP) {
		super ("/frodo2/algorithms/duct/DUCTagent.xml", useTCP);
		this.setDomClass(domClass);
		this.setUtilClass(utilClass);
	}
	
	/**
	 * Constructor
	 *  
	 * @param agentDescFile location of the agent description file
	 * @param domClass 		the class to be used for variable values
	 * @param utilClass		the class to use for utilities
	 */
	public DUCTsolver (String agentDescFile, Class<V> domClass, Class<AddableReal> utilClass) {
		super (agentDescFile);
		this.setDomClass(domClass);
		this.setUtilClass(utilClass);
	}
	
	/** Constructor
	 *  
	 * @param agentDescFile location of the agent description file
	 * @param domClass 		the class to be used for variable values
	 * @param utilClass		the class to use for utilities
	 * @param useTCP 			Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public DUCTsolver (String agentDescFile, Class<V> domClass, Class<AddableReal> utilClass, boolean useTCP) {
		super (agentDescFile, useTCP);
		this.setDomClass(domClass);
		this.setUtilClass(utilClass);
	}
	
	/** @see AbstractDCOPsolver#getSolGatherers() */
	@Override
	public ArrayList<StatsReporter> getSolGatherers() {
		
		ArrayList<StatsReporter> solGatherers = new ArrayList<StatsReporter> (3);
		
		dfsModule = new DFSgeneration<V, AddableReal>(null, problem);
		dfsModule.setSilent(true);
		solGatherers.add(dfsModule);
		
		samplingModule = new Sampling<V>(null, problem);
		samplingModule.setSilent(true);
		solGatherers.add(samplingModule);
		
		this.solCollector = new SolutionCollector<V, AddableReal> (null, problem);
		this.solCollector.setSilent(true);
		solGatherers.add(this.solCollector);
		
		return solGatherers;
	}
	
	/** @see AbstractDCOPsolver#buildSolution() */
	@SuppressWarnings("unchecked")
	@Override
	public Solution<V, AddableReal> buildSolution() throws OutOfMemoryError {
		
		Map<String, V> assignment = this.solCollector.getSolution();
		AddableReal utility = null;
		List<? extends UtilitySolutionSpace<V,AddableReal>> spaces = problem.getSolutionSpaces();

		String[] variables_names = new String[assignment.size()];
		V v = null;
		for(V value : assignment.values()) {
			v = value;
			break;
		}
			
		V[] variables_values = (V[])Array.newInstance(v.getClass(), assignment.size());
		int i = 0;
		for(Entry<String, V> e : assignment.entrySet()) {
			variables_names[i] = e.getKey();
			variables_values[i++] = e.getValue();
		}
		
		for(UtilitySolutionSpace<V, AddableReal> space : spaces)
			utility = utility == null ? space.getUtility(variables_names, variables_values) : utility.add(space.getUtility(variables_names, variables_values));
		
		int nbrMsgs = factory.getNbrMsgs();
		TreeMap<MessageType, Integer> msgNbrs = factory.getMsgNbrs();
		long msgSize = factory.getTotalMsgSize();
		TreeMap<MessageType, Long> msgSizes = factory.getMsgSizes();
		long maxMsgSize = factory.getOverallMaxMsgSize();
		TreeMap<MessageType, Long> maxMsgSizes = factory.getMaxMsgSizes();
		int numberOfCoordinationConstraint = problem.getNumberOfCoordinationConstraints();
		int nbrVariables = problem.getNbrVars();
		long runningTime = factory.getTime();
		
		HashMap<String, Long> timesNeeded = new HashMap<String, Long> ();
		timesNeeded.put(dfsModule.getClass().getName(), dfsModule.getFinalTime());
		
		return new DUCTSolution<V> (nbrVariables, null, utility == null ? new AddableReal(0) : utility , samplingModule.getFinalBound(), assignment, 
				nbrMsgs, msgNbrs, this.factory.getMsgNbrsSentPerAgent(), this.factory.getMsgNbrsReceivedPerAgent(), 
				msgSize, msgSizes, this.factory.getMsgSizesSentPerAgent(), this.factory.getMsgSizesReceivedPerAgent(), 
				maxMsgSize, maxMsgSizes, factory.getNcccs(), 
				runningTime, timesNeeded, numberOfCoordinationConstraint);
	}
	
	/**
	 * Puts the statistics in a format that can easily be processed after the experiments
	 * @author Brammert Ottens, Dec 29, 2011
	 * @param sol 	the solution
	 * @return string representation of the statistics
	 */
	@Override
	public String plotStats(Solution<V, AddableReal> sol) {
		DUCTSolution<V> solCast = (DUCTSolution<V>)sol;
		return sol.getTimeNeeded() + "\t" + sol.getUtility() + "\t" + solCast.getFinalBound() + "\t" + sol.getNcccCount() + "\t" + sol.getNbrMsgs() + "\t" + sol.getTotalMsgSize() + "\t" + sol.getTreeWidth();
	}
	
	/** Plots dummy stats */
	@Override
	public String plotDummyStats(boolean maximize) {
		int util = (maximize ? Integer.MIN_VALUE : Integer.MAX_VALUE);
		return Long.MAX_VALUE + "\t" + util + "\t" + util + "\t" + Integer.MAX_VALUE + "\t" + Integer.MAX_VALUE + "\t" + Integer.MAX_VALUE + "\t" + -1; 
	}
	
	/** Clear this class' member attributes */
	@Override
	public void clear () {
		super.clear();
		this.samplingModule = null;
		this.solCollector = null;
	}

}
