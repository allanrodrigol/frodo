package frodo2.algorithms.localSearch.dsasdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;

import frodo2.algorithms.AbstractDCOPsolver;
import frodo2.algorithms.SolutionCollector;
import frodo2.algorithms.SolutionWithConvergence;
import frodo2.algorithms.StatsReporter;
import frodo2.solutionSpaces.Addable;

/**
 * This is a solver that reads in the problem, creates the agents, runs the problem and then collects the
 * statistics.
 */
public class DSASDPsolver < V extends Addable<V>, U extends Addable<U> > extends AbstractDCOPsolver< V, U, SolutionWithConvergence<V, U> > {
	
	/** The DSASDP module */
	protected DSASDP<V, U> dsasdpModule;

	/** The SolutionCollector module */
	protected SolutionCollector<V, U> solCollector;

	/**
	 * Constructor
	 */
	public DSASDPsolver () {
		super ("/frodo2/algorithms/localSearch/dsasdp/DSASDPagent.xml");
	}
	
	/** Constructor
	 * @param useTCP 		Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public DSASDPsolver (boolean useTCP) {
		super ("/frodo2/algorithms/localSearch/dsasdp/DSASDPagent.xml", useTCP);
	}
	
	/** Constructor 
	 * @param domClass 		the class to use for variable values
	 * @param utilClass 	the class to use for utilities
	 */
	public DSASDPsolver (Class<V> domClass, Class<U> utilClass) {
		this();
		this.setDomClass(domClass);
		this.setUtilClass(utilClass);
	}
	
	/** Constructor 
	 * @param domClass 		the class to use for variable values
	 * @param utilClass 	the class to use for utilities
	 * @param useTCP 		Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public DSASDPsolver (Class<V> domClass, Class<U> utilClass, boolean useTCP) {
		this(useTCP);
		this.setDomClass(domClass);
		this.setUtilClass(utilClass);
	}
	
	/**
	 * Constructor
	 * @param agentDesc path to the agent description file
	 */
	public DSASDPsolver (String agentDesc) {
		super (agentDesc);
	}
	
	/**
	 * Constructor
	 * @param agentDesc path to the agent description file
	 * @param useTCP 		Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public DSASDPsolver (String agentDesc, boolean useTCP) {
		super (agentDesc, useTCP);
	}
	
	/** Constructor 
	 * @param agentDescFile description of the agent to be used
	 * @param domClass 		the class to be used for variable values
	 * @param utilClass 	the class to be used for utility values
	 */
	public DSASDPsolver (String agentDescFile, Class<V> domClass, Class<U> utilClass) {
		this (agentDescFile);
		this.setDomClass(domClass);
		this.setUtilClass(utilClass);
	}
	
	/** Constructor 
	 * @param agentDescFile description of the agent to be used
	 * @param domClass 		the class to be used for variable values
	 * @param utilClass 	the class to be used for utility values
	 * @param useTCP 		Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public DSASDPsolver (String agentDescFile, Class<V> domClass, Class<U> utilClass, boolean useTCP) {
		this (agentDescFile, useTCP);
		this.setDomClass(domClass);
		this.setUtilClass(utilClass);
	}
	
	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 */
	public DSASDPsolver (Document agentDesc) {
		super (agentDesc);
	}
	
	/** Constructor 
	 * @param agentDesc 	description of the agent to be used
	 * @param useTCP 		Whether to use TCP pipes or shared memory pipes
	 * @warning Using TCP pipes automatically disables simulated time. 
	 */
	public DSASDPsolver (Document agentDesc, boolean useTCP) {
		super (agentDesc, useTCP);
	}
	
	/**
	 * Sets the convergence parameter to the desired value
	 * @param convergence	\c true when convergence must be measured, and false otherwise
	 */
	public void setConvergence(boolean convergence) {
		for (Element module : (List<Element>) agentDesc.getRootElement().getChild("modules").getChildren()) 
			if (module.getAttributeValue("className").equals(DSASDP.class.getName())) 
				module.setAttribute("convergence", Boolean.toString(convergence));
	}
	
	/** @see AbstractDCOPsolver#getSolGatherers() */
	@Override
	public ArrayList<StatsReporter> getSolGatherers() {

		ArrayList<StatsReporter> solGatherers = new ArrayList<StatsReporter> (2);
		
		dsasdpModule = new DSASDP<V, U>((Element)null, problem);
		dsasdpModule.setSilent(true);
		solGatherers.add(dsasdpModule);
		
		solCollector = new SolutionCollector<V, U>((Element)null, problem);
		solCollector.setSilent(true);
		solGatherers.add(solCollector);
		
		return solGatherers;
	}
		
	/** @see AbstractDCOPsolver#buildSolution() */
	@Override
	public SolutionWithConvergence<V, U> buildSolution() {
		
		HashMap<String, Long> timesNeeded = new HashMap<String, Long> ();

		return new SolutionWithConvergence<V, U> (super.problem.getNbrVars(), null, this.solCollector.getUtility(), solCollector.getSolution(), 
				factory.getNbrMsgs(), factory.getMsgNbrs(), factory.getMsgNbrsSentPerAgent(), factory.getMsgNbrsReceivedPerAgent(), 
				factory.getTotalMsgSize(), factory.getMsgSizes(), factory.getMsgSizesSentPerAgent(), factory.getMsgSizesReceivedPerAgent(), 
				factory.getOverallMaxMsgSize(), factory.getMaxMsgSizes(), factory.getNcccs(), factory.getTime(), timesNeeded, dsasdpModule.getAssignmentHistories());
	}

	/** @see AbstractDCOPsolver#clear() */
	@Override
	public void clear () {
		super.clear();
		this.dsasdpModule = null;
		this.solCollector = null;
	}

}
