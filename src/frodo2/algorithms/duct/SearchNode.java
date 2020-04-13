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

import java.util.ArrayList;
import java.util.Arrays;

import frodo2.algorithms.duct.bound.Bound;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableReal;
import frodo2.solutionSpaces.UtilitySolutionSpace;

/**
 * Convenience class to store information associated to a specific state
 * @author Brammert Ottens, 29 aug. 2011
 * 
 * @param <V> type used for domain values
 */
public class SearchNode <V extends Addable<V>> {
	
	/** when \c true, infeasible values should always be ignored, when \c false the should be sampled once */
	protected final boolean IGNORE_INF;
	
	// problem information
	/** For each domain value, the local costs assocated to it */
	public AddableReal[] localCosts;
	
	/** List of local solutions that have not yet been sampled */
	public ArrayList<Integer> unknowLocalSolutions;
	
	/** Counts the number of local infeasible solutions*/
	public int nbrFeasibleLocalSolutions;
	
	/** \c true when the local problem has at least one feasible solution, \c false otherwise */
	public boolean feasible;
	
	/** the size of the domain */
	public int numberOfValues;
	
	// utilities
	
	/** For each domain value, the average utility received so far*/
	public double[] averageUtil;
	
	/** The best util received so far */
	public double[] bestUtil;
	
	/** For each domain value, the current bound*/
	public double[] bounds;
	
	// frequencies
	
	/** For each posible domain values, the number of times it has been selected */
	public int[] actionFrequencies;
	
	/** The number of times the corresponding state has been visited */
	public int frequencie;
	
	// sampling
	
	/** The value woth the currently best utility */
	public double maxValue;
	
	/** The value with the currently highest upper bound */
	public double maxBound;
	
	/** The index of the currently best value */
	public int maxValueIndex;
	
	/** The index of the currently highest bound */
	public int maxBoundIndex;
	
	// initialization
	
	/** \c true when not all domain values have been sampled, and \c false otherwise */
	protected boolean random;
	
	/** Last value visited */
	protected int lastVisited;
	
	
	/**
	 * Constructor
	 * 
	 * @param numberOfValues	the domain size of the variable
	 * @param maximize			\c true when maximizing, and \c false otherwise
	 * @param ignore_inf		\c true when infeasible utilities should be totally ignored, \c false otherwise
	 */
	public SearchNode(int numberOfValues, boolean maximize, boolean ignore_inf) {
		this.numberOfValues = numberOfValues;
		averageUtil = new double[numberOfValues];
		actionFrequencies = new int[numberOfValues];
		localCosts = new AddableReal[numberOfValues];
		bounds = new double[numberOfValues];
		this.nbrFeasibleLocalSolutions = numberOfValues;
		this.unknowLocalSolutions = new ArrayList<Integer>(numberOfValues);
		for(int i = 0; i < numberOfValues; i++)
			this.unknowLocalSolutions.add(i);
		random = true;
		bestUtil = new double[numberOfValues];
		Arrays.fill(bestUtil, maximize ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
		IGNORE_INF = ignore_inf;
		if(!IGNORE_INF)
			feasible = true;
	}
	
	/**
	 * 
	 * Finds the first feasible local solution, given the current context
	 * 
	 * @author Brammert Ottens, Oct 24, 2011
	 * @param space					the local space
	 * @param infeasibleUtility		the infeasible utility
	 * @param contextVariables		the context variables
	 * @param context				the context
	 * @param domain				the variable domain
	 */
	public void initSampling(UtilitySolutionSpace<V, AddableReal> space, AddableReal infeasibleUtility, String[] contextVariables, V[] context, V[] domain) {
		// find the first feasible local solution
		lastVisited = this.getRandomUnknowLocal(space, infeasibleUtility, contextVariables, context, domain);
		
//		lastVisited = 0;
//		context[context.length - 1] = domain[lastVisited];
//		AddableReal cost = space == null ? new AddableReal(0) : space.getUtility(contextVariables, context);
//		localCosts[lastVisited] = cost;
//		actionFrequencies[lastVisited]++;
//		if(IGNORE_INF) {
//			boolean done = cost != infeasibleUtility; 
//
//			while(!done && lastVisited < numberOfValues - 1) {
//				lastVisited++;
//				context[context.length - 1] = domain[lastVisited];
//				cost = space == null ? new AddableReal(0) : space.getUtility(contextVariables, context);
//				done = cost != infeasibleUtility;
//				localCosts[lastVisited] = cost;
//				actionFrequencies[lastVisited]++;
//			}
//
//			feasible = feasible || done;
//			if(feasible)
//				this.nbrFeasibleLocalSolutions++;
//		}
//		random = feasible;
	}
	
	/**
	 * Method called whenever the state is sampled
	 * @author Brammert Ottens, 24 aug. 2011
	 * @param b the type of bound used
	 */
	public void visited(Bound<V> b) {
		frequencie++;
		for(int i = 0; i < numberOfValues; i++) {
			bounds[i] = b.sampleBound(i, this);
			assert bounds[i] != 0;
		}
	}
	
	/**
	 * Finds the next feasible local solution
	 * 
	 * @author Brammert Ottens, Oct 24, 2011
	 * @param space					the local space
	 * @param infeasibleUtility		the infeasible utility
	 * @param contextVariables		the context variables
	 * @param context				the context
	 * @param domain				the variable domain
	 * @return the last visited feasible local solution
	 */
	public int solveLocalProblem(UtilitySolutionSpace<V, AddableReal> space, AddableReal infeasibleUtility, String[] contextVariables, V[] context, V[] domain) {
		int returnIndex = lastVisited;
		int returnValue = returnIndex == -1 ? -1 : unknowLocalSolutions.remove(returnIndex); 
		if(this.unknowLocalSolutions.size() > 0)
			lastVisited = this.getRandomUnknowLocal(space, infeasibleUtility, contextVariables, context, domain);
		
//		int returnValue = lastVisited;
//		AddableReal cost = null;
//		assert returnValue <= numberOfValues;
//		this.lastVisited++;
//		if(lastVisited < numberOfValues) {
//			context[context.length - 1] = domain[lastVisited];
//			cost = space == null ? new AddableReal(0) : space.getUtility(contextVariables, context);
//			localCosts[lastVisited] = cost;
//			actionFrequencies[lastVisited]++;
//
//			if(IGNORE_INF) {
//				while(cost == infeasibleUtility && lastVisited < numberOfValues - 1) {
//					this.lastVisited++;
//					context[context.length - 1] = domain[lastVisited];
//					cost = space == null ? new AddableReal(0) : space.getUtility(contextVariables, context);
//					localCosts[lastVisited] = cost;
//					actionFrequencies[lastVisited]++;
//				}
//				
//				if(cost != infeasibleUtility)
//					this.nbrFeasibleLocalSolutions++;
//			}
//		} else if(!IGNORE_INF) {
//			feasible = false;
//			for(AddableReal l : localCosts)
//				if(l != infeasibleUtility) {
//					feasible = true;
//					this.nbrFeasibleLocalSolutions++;
//				}
//		}
//		
//		if(IGNORE_INF) {
//			random = cost != infeasibleUtility;
//		}
		return returnValue;
	}
	
	/**
	 * Stores the sum of the costs reported by the children for this particular state
	 * 
	 * @author Brammert Ottens, 24 aug. 2011
	 * @param valueIndex			the index of the sampled value
	 * @param costSample			the sum of the costs reported by the children
	 * @param infeasibleUtility		utility of infeasible assignments
	 * @param maximize				\c true when maximizing, and \c false otherwise
	 * @return the sum of the reported and local cost
	 */
	public AddableReal storeCost(int valueIndex, AddableReal costSample, AddableReal infeasibleUtility, final boolean maximize) {
		if(random)
			random = this.unknowLocalSolutions.size() > 0;
		
		boolean infeasible = localCosts[valueIndex] == infeasibleUtility;
		double cost = costSample.doubleValue();

		if(!infeasible) {
			double average = averageUtil[valueIndex];
			average *= actionFrequencies[valueIndex] - 1;
			average /= actionFrequencies[valueIndex];
			average += cost/actionFrequencies[valueIndex];
			averageUtil[valueIndex] = average;

			cost += localCosts[valueIndex].doubleValue();

			if(maximize)
				bestUtil[valueIndex] = (bestUtil[valueIndex] < cost ? cost : bestUtil[valueIndex]);
			else
				bestUtil[valueIndex] = (bestUtil[valueIndex] > cost ? cost : bestUtil[valueIndex]);

		}


		if(infeasible)
			return null;
		
		if(random)
			return new AddableReal(costSample.doubleValue() + localCosts[valueIndex].doubleValue());
		
		return new AddableReal(cost);
	}
	
	/**
	 * With probablity 1- \c delta the error between the real value and the
	 * estimated value lies in 
	 * @author Brammert Ottens, 29 aug. 2011
	 * @param index the index of the value for which the bound should be calculated
	 * @param delta the probability
	 * @return the bound
	 */
	public double convergenceBound(int index, double delta) {
		return Math.sqrt(Math.log(2/delta)/(2*actionFrequencies[index]));
	}
	
	/**
	 * 
	 * @author Brammert Ottens, 18 okt. 2011
	 * @param delta the delta
	 * @return	string representation of the object, used for debugging purposses
	 */
	public String toString(double delta) {
		if(!random) {
			double[] mean = new double[this.numberOfValues];
			double[] upper = new double[this.numberOfValues];
			double[] lower = new double[this.numberOfValues];
			int i = 0;
			for(; i < numberOfValues; i++) {
				mean[i] += (averageUtil[i] + this.localCosts[i].doubleValue());
				upper[i] += (bestUtil[i] + this.localCosts[i].doubleValue() + this.convergenceBound(i, delta));
				lower[i] += (bestUtil[i] + this.localCosts[i].doubleValue() - this.convergenceBound(i, delta));
			}

			String str = "random = " + random + "\n"; 
			str += "mean = \t" + arrayToString(mean, maxValueIndex) + "\n";
			str += "best = \t" + arrayToString(bestUtil, maxValueIndex) + "\n";
			str += "local = \t" + Arrays.toString(localCosts) + "\n";
			str += "lower = \t" + arrayToString(lower, maxBoundIndex) + "\n";
			str += "upper = \t" + arrayToString(upper, maxBoundIndex) + "\n";
			str += "visited = \t" + Arrays.toString(actionFrequencies) + "\n";
			return str;
		}

		return "";
	}
	
	/** The sampling order */
	ArrayList<Integer> samplingOrder = new ArrayList<Integer>();
	
	/**
	 * Method to randomly pick a value that has not been sampled yet
	 * @param space				the space owned by the variable
	 * @param infeasibleUtility	the infeasible utility
	 * @param contextVariables	the variables in the separator
	 * @param context			the current context (variable assignment)
	 * @param domain			the domain of the variable
	 * @return an index of an unsampled, feasible solution, or -1 if no such value exists
	 */
	public int getRandomUnknowLocal(UtilitySolutionSpace<V, AddableReal> space, AddableReal infeasibleUtility, String[] contextVariables, V[] context, V[] domain) {
		int sample = (int)(Math.random()*this.unknowLocalSolutions.size());
		int value = unknowLocalSolutions.get(sample);
		context[context.length - 1] = domain[value];
		AddableReal cost = space == null ? new AddableReal(0) : space.getUtility(contextVariables, context);
		this.actionFrequencies[value]++;
		this.localCosts[value] = cost;
		
		if(this.IGNORE_INF) {
			while(cost == infeasibleUtility) {
				unknowLocalSolutions.remove(sample);
				this.nbrFeasibleLocalSolutions--;
				if(this.unknowLocalSolutions.size() == 0)
					break;
				sample = (int)(Math.random()*this.unknowLocalSolutions.size());
				value = unknowLocalSolutions.get(sample);
				context[context.length - 1] = domain[value];
				cost = space == null ? new AddableReal(0) : space.getUtility(contextVariables, context);
				this.actionFrequencies[value]++;
				this.localCosts[value] = cost;
			}
		}
		
		if(this.nbrFeasibleLocalSolutions == 0) {
			if(this.IGNORE_INF)
				return -1;
		}
		
		this.samplingOrder.add(value);
		this.feasible = true;

		return sample;
	}
	

	/**
	 * Method for printing an array, highlighting the entry at \c highlight
	 * @author Brammert Ottens, Oct 31, 2011
	 * @param arr 			the array to be printed
	 * @param highlight		the index to be highlighted
	 * @return	string representation of the array
	 */
	String arrayToString(Object[] arr, int highlight) {
		String str = "[";
		int i = 0;
		for(; i < arr.length - 1; i++) {
			if(i == highlight)
				str += "(" + arr[i] + "), ";
			else
				str += arr[i] + ", ";
		}
		
		if(i == highlight)
			str += "(" + arr[i] + ")]";
		else
			str += arr[i] + "]";
		
		return str;
	}
	
	/**
	 * Method for printing an array, highlighting the entry at \c highlight
	 * @author Brammert Ottens, Oct 31, 2011
	 * @param arr 			the array to be printed
	 * @param highlight		the index to be highlighted
	 * @return	string representation of the array
	 */
	String arrayToString(double[] arr, int highlight) {
		String str = "[";
		int i = 0;
		for(; i < arr.length - 1; i++) {
			if(i == highlight)
				str += "(" + arr[i] + "), ";
			else
				str += arr[i] + ", ";
		}
		
		if(i == highlight)
			str += "(" + arr[i] + ")]";
		else
			str += arr[i] + "]";
		
		return str;
	}
}
