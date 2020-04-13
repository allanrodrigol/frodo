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

package frodo2.algorithms.duct.bound;

import frodo2.algorithms.duct.SearchNode;
import frodo2.solutionSpaces.Addable;

/**
 * @author Brammert Ottens, Nov 21, 2011
 * 
 * @param <V>	type used for domain values
 */
public interface Bound <V extends Addable<V>> {

	/**
	 * Calculate the error bound on the utility
	 * @author Brammert Ottens, 25 aug. 2011
	 * @param index the index of the value for which the bound is to be calculated
	 * @param node the node that is to be sampled
	 * @return	the error bound
	 */
	public double sampleBound(int index, SearchNode<V> node);
	
}
