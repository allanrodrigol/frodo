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
import frodo2.algorithms.duct.bound.Bound;
import frodo2.solutionSpaces.Addable;

/**
 * @author Brammert Ottens, Nov 21, 2011
 * 
 * @param <V> type used for domain values
 */
public class BoundLogSize <V extends Addable<V>> implements Bound<V> {

	/** size of the space that is to be sampled */
	long size;
	
	/**
	 * Constructor
	 * 
	 * @param size size of the space that is to be sampled
	 */
	public BoundLogSize(Long size) {
		this.size = size + 1;
		assert size > 0;
	}
	
	/** @see frodo2.algorithms.duct.bound.Bound#sampleBound(int, frodo2.algorithms.duct.SearchNode) */
	@Override
	public double sampleBound(int index, SearchNode<V> node) {
		return Math.sqrt(size*Math.log(2*node.frequencie)/node.actionFrequencies[index]);
	}
	
}
