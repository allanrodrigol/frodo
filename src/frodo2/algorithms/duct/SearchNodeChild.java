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

import java.util.Arrays;

import frodo2.algorithms.duct.SearchNode;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableReal;

/**
 * Convenience class to store information associated to a specific state
 * @author Brammert Ottens, 29 aug. 2011
 * 
 * @param <V> type used for domain values
 */
public class SearchNodeChild <V extends Addable<V>> extends SearchNode<V> {
	
	// sampling
	
	/** The index with the max/min value is to be sampled */
	public double[] childB;
	
	/** \c true when for all domain values the optimal cost is known */
	public boolean complete;
	
	/**
	 * Constructor
	 * 
	 * @param numberOfValues	the domain size of the variable
	 * @param maximize			\c true when maximizing, and \c false otherwise
	 * @param ignore_inf		\c true when infeasible utilities should be totally ignored, \c false otherwise
	 */
	public SearchNodeChild(int numberOfValues, boolean maximize, boolean ignore_inf) {
		super(numberOfValues, maximize, ignore_inf);
		childB = new double[numberOfValues];
		Arrays.fill(childB, maximize ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
	}
	
	/**
	 * Stores the sum of the costs reported by the children for this particular state
	 * 
	 * @author Brammert Ottens, 24 aug. 2011
	 * @param valueIndex			the index of the sampled value
	 * @param costSample			the sum of the costs reported by the children
	 * @param childBound			bound reported by the child
	 * @param infeasibleUtility		utility of infeasible assignments
	 * @param maximize				\c true when maximizing, and \c false otherwise
	 * @return the sum of the reported and local cost
	 */
	public AddableReal storeCost(int valueIndex, AddableReal costSample, AddableReal childBound, AddableReal infeasibleUtility, final boolean maximize) {
		if(random)
			random = this.unknowLocalSolutions.size() > 0;
			
		if(costSample == infeasibleUtility) {
			localCosts[valueIndex] = infeasibleUtility;
			childB[valueIndex] = infeasibleUtility.doubleValue();
			if(--nbrFeasibleLocalSolutions == 0)
				this.feasible = false;
		}
		
		boolean infeasible = localCosts[valueIndex] == infeasibleUtility;
		double cost = costSample.doubleValue();
		
		if(!infeasible) {
			assert actionFrequencies[valueIndex] != 0;
			
			double average = averageUtil[valueIndex];
			average *= actionFrequencies[valueIndex] - 1;
			average /= actionFrequencies[valueIndex];
			average += cost/actionFrequencies[valueIndex];
			averageUtil[valueIndex] = average;

			
			cost += localCosts[valueIndex].doubleValue();
			this.childB[valueIndex] = childBound.doubleValue() + localCosts[valueIndex].doubleValue();
			
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
	
	/** @see frodo2.algorithms.duct.SearchNode#toString(double) */
	@Override
	public String toString(double delta) {
		if(!random) {
			double[] mean = new double[this.numberOfValues];
			double[] upper = new double[this.numberOfValues];
			double[] lower = new double[this.numberOfValues];
			double[] lowerUsed = new double[this.numberOfValues];
			double[] diff = new double[this.numberOfValues];
			int i = 0;
			for(; i < numberOfValues; i++) {
				mean[i] = (averageUtil[i] + this.localCosts[i].doubleValue());
				upper[i] = (bestUtil[i] + this.convergenceBound(i, delta));
				lower[i] = (bestUtil[i] - this.convergenceBound(i, delta));
				lowerUsed[i] = Math.max(lower[i], childB[i]);
				diff[i] = bestUtil[maxValueIndex] - lowerUsed[i]; 
			}

			String str = "random = " + random + "\n"; 
			str += "mean = \t" + arrayToString(mean, maxValueIndex) + "\n";
			str += "best = \t" + arrayToString(bestUtil, maxValueIndex) + "\n";
			str += "local = \t" + Arrays.toString(localCosts) + "\n";
			str += "lower = \t" + arrayToString(lower, maxBoundIndex) + "\n";
			str += "upper = \t" + arrayToString(upper, maxBoundIndex) + "\n";
			str += "child = \t" + arrayToString(childB, maxBoundIndex) + "\n";
			str += "lowerUsed = \t" + arrayToString(lowerUsed, maxBoundIndex) + "\n";
			str += "difference = \t" + arrayToString(diff, maxValueIndex) + "\n";
			str += "visited = \t" + Arrays.toString(actionFrequencies) + "\n";
			return str;
		}

		return "";
	}
}
