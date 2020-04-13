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

package frodo2.algorithms.dpop.privacy;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.jdom2.Document;

import frodo2.algorithms.Solution;
import frodo2.algorithms.StatsReporter;
import frodo2.algorithms.XCSPparser;
import frodo2.algorithms.dpop.UTILpropagation;
import frodo2.algorithms.varOrdering.dfs.DFSgeneration;
import frodo2.algorithms.varOrdering.election.LeaderElectionMaxID;
import frodo2.communication.MessageType;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableInteger;
import frodo2.solutionSpaces.crypto.AddableBigInteger;

/** A solver for P-DPOP with rerooting
 * @author Thomas Leaute
 * @param <V> type used for variable values
 */
public class P3halves_DPOPsolver < V extends Addable<V> > extends P_DPOPsolver<V> {
	
	/** The module that gathers the solution */
	private RerootRequester<V, AddableBigInteger> solutionGatherer;

	/** Default constructor */
	@SuppressWarnings("unchecked")
	public P3halves_DPOPsolver() {
		super("/frodo2/algorithms/dpop/privacy/P1.5-DPOPagent.xml", (Class<V>) AddableInteger.class);
	}

	/** Constructor 
	 * @param useTCP 		whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	@SuppressWarnings("unchecked")
	public P3halves_DPOPsolver(boolean useTCP) {
		super("/frodo2/algorithms/dpop/privacy/P1.5-DPOPagent.xml", (Class<V>) AddableInteger.class, useTCP);
	}

	/** Constructor 
	 * @param domClass 		the class to use for variable values
	 */
	public P3halves_DPOPsolver (Class<V> domClass) {
		this (domClass, false);
	}
	
	/** Constructor 
	 * @param domClass 		the class to use for variable values
	 * @param useTCP 		whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public P3halves_DPOPsolver (Class<V> domClass, boolean useTCP) {
		super ("/frodo2/algorithms/dpop/privacy/P1.5-DPOPagent.xml", useTCP);
		this.setDomClass(domClass);
		this.setUtilClass(AddableBigInteger.class);
	}
	
	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 * @param domClass 		the class to use for variable values
	 * @param useTCP 		whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public P3halves_DPOPsolver (Document agentDesc, Class<V> domClass, boolean useTCP) {
		super (agentDesc, useTCP);
		this.setDomClass(domClass);
		this.setUtilClass(AddableBigInteger.class);
	}
	
	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 * @param parserClass	the class used to parse problems	
	 */
	public P3halves_DPOPsolver(Document agentDesc, Class<? extends XCSPparser<V, AddableBigInteger>> parserClass) {
		super(agentDesc, parserClass);
	}

	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 */
	public P3halves_DPOPsolver(Document agentDesc) {
		super(agentDesc);
	}

	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 * @param useTCP 		whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public P3halves_DPOPsolver(Document agentDesc, boolean useTCP) {
		super(agentDesc, useTCP);
	}

	/** Constructor
	 * @param filename	the location of the agent description file
	 */
	public P3halves_DPOPsolver(String filename) {
		super(filename);
	}

	/** Constructor
	 * @param filename	the location of the agent description file
	 * @param useTCP 		whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public P3halves_DPOPsolver(String filename, boolean useTCP) {
		super(filename, useTCP);
	}

	/** Constructor
	 * @param agentDescFile description of the agent to be used
	 * @param domClass 		the class to be used for variable values
	 */
	public P3halves_DPOPsolver(String agentDescFile, Class<V> domClass) {
		super(agentDescFile, domClass);
	}

	/** Constructor
	 * @param agentDescFile description of the agent to be used
	 * @param domClass 		the class to be used for variable values
	 * @param useTCP 		whether to use TCP pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public P3halves_DPOPsolver(String agentDescFile, Class<V> domClass, boolean useTCP) {
		super(agentDescFile, domClass, useTCP);
	}

	/** @see P_DPOPsolver#getSolGatherers() */
	@Override
	public ArrayList<StatsReporter> getSolGatherers() {

		ArrayList<StatsReporter> solGatherers = new ArrayList<StatsReporter> (3);
		
		utilModule = new UTILpropagation<V, AddableBigInteger>(null, problem);
		utilModule.setSilent(true);
		solGatherers.add(utilModule);
		
		this.solutionGatherer = new RerootRequester<V, AddableBigInteger> (null, problem);
		solutionGatherer.setSilent(true);
		solGatherers.add(solutionGatherer);
		
		dfsModule = new DFSgeneration<V, AddableBigInteger> (null, problem);
		dfsModule.setSilent(true);
		solGatherers.add(dfsModule);

		return solGatherers;
	}

	/** @see P_DPOPsolver#buildSolution() */
	@Override
	public Solution<V, AddableBigInteger> buildSolution() {

		AddableBigInteger optUtil = this.solutionGatherer.getOptUtil();
		Map<String, V>  solution = solutionGatherer.getSolution();
		int nbrMsgs = factory.getNbrMsgs();
		TreeMap<MessageType, Integer> msgNbrs = factory.getMsgNbrs();
		long totalMsgSize = factory.getTotalMsgSize();
		TreeMap<MessageType, Long> msgSizes = factory.getMsgSizes();
		long maxMsgSize = factory.getOverallMaxMsgSize();
		TreeMap<MessageType, Long> maxMsgSizes = factory.getMaxMsgSizes();
		long ncccs = factory.getNcccs();
		int maxMsgDim = utilModule.getMaxMsgDim();
		int numberOfCoordinationConstraints = problem.getNumberOfCoordinationConstraints();
		int nbrVariables = problem.getNbrVars();
		long totalTime = factory.getTime();
		
		return new Solution<V, AddableBigInteger> (nbrVariables, optUtil, super.problem.getUtility(solution).getUtility(0), solution, 
				nbrMsgs, msgNbrs, this.factory.getMsgNbrsSentPerAgent(), this.factory.getMsgNbrsReceivedPerAgent(), 
				totalMsgSize, msgSizes, this.factory.getMsgSizesSentPerAgent(), this.factory.getMsgSizesReceivedPerAgent(), 
				maxMsgSize, maxMsgSizes, ncccs, totalTime, null, maxMsgDim, numberOfCoordinationConstraints);
	}
	
	/** @see P_DPOPsolver#clear() */
	@Override
	public void clear () {
		super.clear();
		this.solutionGatherer = null;
		
		DFSgeneration.ROOT_VAR_MSG_TYPE = LeaderElectionMaxID.OUTPUT_MSG_TYPE;
	}
	
}
