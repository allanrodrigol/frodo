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

package frodo2.algorithms.duct.termination;

import frodo2.algorithms.duct.SearchNode;
import frodo2.solutionSpaces.Addable;

/**
 * @author Brammert Ottens, Oct 24, 2011
 * @param <V> type used for domain values
 * 
 */
public interface TerminationCondition<V extends Addable<V>> {
	
	/**
	 * Checks whether the distribution \c dist has converged to a value
	 * @author Brammert Ottens, Oct 24, 2011
	 * @param dist the distribution
	 * @param error 	parameter of termination condition
	 * @param delta     parameter of termination condition
	 * @param maximize \c true when maximizing, \c false otherwise
	 * @return \c true when the distribution converged, and false otherwise
	 */
	public boolean converged(SearchNode<V> dist, double error, double delta, boolean maximize);

}
