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

import frodo2.algorithms.duct.SearchNode;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableReal;

/**
 * Convenience class to store information associated to a specific state
 * @author Brammert Ottens, 29 aug. 2011
 * 
 * @param <V> type used for domain values
 */
public class SearchNodePruning <V extends Addable<V>> extends SearchNode<V> {
	
	/**
	 * Constructor
	 * 
	 * @param numberOfValues	the domain size of the variable
	 * @param maximize			\c true when maximizing, and \c false otherwise
	 * @param ignore_inf		when \c true, infeasible utilities are completely ignored. \c false otherwise
	 */
	public SearchNodePruning(int numberOfValues, boolean maximize, boolean ignore_inf) {
		super(numberOfValues, maximize, ignore_inf);
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
	@Override
	public AddableReal storeCost(int valueIndex, AddableReal costSample, AddableReal infeasibleUtility, final boolean maximize) {
		if(random)
			random = this.unknowLocalSolutions.size() > 0;//lastVisited < numberOfValues;
		
		assert !random || this.unknowLocalSolutions.size() > 0;
			
		if(costSample == infeasibleUtility) {
			localCosts[valueIndex] = infeasibleUtility;
			if(--nbrFeasibleLocalSolutions == 0)
				this.feasible = false;
		}
		
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
}
