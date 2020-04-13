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

package frodo2.algorithms.afb;

import java.util.ArrayList;

import org.jdom2.Document;

import frodo2.algorithms.AbstractDCOPsolver;
import frodo2.algorithms.SolutionCollector;
import frodo2.algorithms.SolutionWithConvergence;
import frodo2.algorithms.StatsReporter;
import frodo2.solutionSpaces.Addable;

/** A DCOP solver using AFB
 * @param <V> type used for variable values
 * @param <U> type used for utility values
 * @author Alexandra Olteanu, Thomas Leaute
 */
public class AFBsolver< V extends Addable<V>, U extends Addable<U> > extends AbstractDCOPsolver< V, U, SolutionWithConvergence<V, U> > {

	/** The AFB module */
	protected AFB<V, U> module;
	
	/** The solution collector module */
	private SolutionCollector<V, U> solCollector;

	/** Default constructor 
	 * @param filename the name of the file containing the description of the algorithm*/
	public AFBsolver (String filename) {
		super (filename);
	}
	
	/** Constructor 
	 * @param agentDescFile description of the agent to be used
	 * @param domClass 		the class to be used for variable values
	 * @param utilClass 	the class to be used for utility values
	 */
	public AFBsolver (String agentDescFile, Class<V> domClass, Class<U> utilClass) {
		this (agentDescFile);
		this.setDomClass(domClass);
		this.setUtilClass(utilClass);
	}
	
	/** Default constructor */
	public AFBsolver () {
		super ("/frodo2/algorithms/afb/AFBagent.xml");
	}
	
	/** Constructor 
	 * @param domClass 		the class to use for variable values
	 * @param utilClass 	the class to use for utilities
	 */
	public AFBsolver (Class<V> domClass, Class<U> utilClass) {
		this();
		this.setDomClass(domClass);
		this.setUtilClass(utilClass);
	}
	
	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 */
	public AFBsolver (Document agentDesc) {
		super (agentDesc);
	}
	
	/**
	 * Constructor
	 * @param agentDesc 	the agent description
	 * @param useTCP 		Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public AFBsolver(Document agentDesc, boolean useTCP) {
		super(agentDesc, useTCP);
	}
	
	/** @see AbstractDCOPsolver#getSolGatherers() */
	@Override
	public ArrayList<StatsReporter> getSolGatherers() {

		ArrayList<StatsReporter> solGatherers = new ArrayList<StatsReporter> (2);
		
		module = new AFB<V, U> (null, problem);
		module.setSilent(true);
		solGatherers.add(module);
		
		this.solCollector = new SolutionCollector<V, U> (null, problem);
		this.solCollector.setSilent(true);
		solGatherers.add(solCollector);
		
		return solGatherers;
	}
	
	/** @see AbstractDCOPsolver#buildSolution() */
	@Override
	public SolutionWithConvergence<V, U> buildSolution () {
		return new SolutionWithConvergence<V, U> (this.problem.getNbrVars(), null, this.solCollector.getUtility(), this.solCollector.getSolution(), 
				factory.getNbrMsgs(), factory.getMsgNbrs(), factory.getMsgNbrsSentPerAgent(), factory.getMsgNbrsReceivedPerAgent(), 
				factory.getTotalMsgSize(), factory.getMsgSizes(), factory.getMsgSizesSentPerAgent(), factory.getMsgSizesReceivedPerAgent(), 
				factory.getOverallMaxMsgSize(), factory.getMaxMsgSizes(), 
				factory.getNcccs(), factory.getTime(), null, module.getAssignmentHistories());
	}
	
	/** @see AbstractDCOPsolver#clear() */
	@Override
	protected void clear () {
		super.clear();
		this.module = null;
	}
}