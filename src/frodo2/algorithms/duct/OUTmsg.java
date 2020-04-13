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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Set;

import frodo2.algorithms.duct.Normalize;
import frodo2.communication.Message;
import frodo2.communication.MessageWith4Payloads;
import frodo2.solutionSpaces.Addable;
import frodo2.solutionSpaces.AddableReal;
import frodo2.solutionSpaces.UtilitySolutionSpace;

/**
 * @author Brammert Ottens, 17 aug. 2011
 * @param <V> type used for domain values
 * 
 */
public class OUTmsg <V extends Addable<V>> extends MessageWith4Payloads<String, AddableReal, ArrayList<UtilitySolutionSpace<V, AddableReal>>, Long> {

	/** The separators of the variable*/
	Set<String> separators;
	
	/**
	 * Constructor
	 * 
	 * @param variable		the name of the variable
	 * @param scalingFactor the scaling factor
	 * @param spaces		the spaces owned by the variable
	 * @param separators 	the separator of the variable
	 * @param size			the size of the domain
	 */
	public OUTmsg(String variable, AddableReal scalingFactor, ArrayList<UtilitySolutionSpace<V, AddableReal>> spaces, Set<String> separators, long size) {
		super(Normalize.OUT_MSG_TYPE, variable, scalingFactor, spaces, size);
		this.separators = separators;
		assert separators == null || !separators.contains(variable);
	}
	
	/** @see Message#writeExternal(java.io.ObjectOutput) */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		assert false : "This method should never be used, since this is an internal message!";
	}

	/** @see Message#readExternal(java.io.ObjectInput) */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		assert false : "This method should never be used, since this is an internal message!";
	}
	
	/**
	 * @author Brammert Ottens, 17 aug. 2011
	 * @return the name of the variable
	 */
	public String getVariable() {
		return this.getPayload1();
	}
	
	/**
	 * @author Brammert Ottens, 2 sep. 2011
	 * @return the scaling factor
	 */
	public AddableReal getScalingFactor() {
		return this.getPayload2();
	}
	
	/**
	 * @author Brammert Ottens, 17 aug. 2011
	 * @return the spaces owned by the variable
	 */
	public ArrayList<UtilitySolutionSpace<V, AddableReal>> getSpaces() {
		return this.getPayload3();
	}
	
	/**
	 * @author Brammert Ottens, 29 aug. 2011
	 * @return the separator of the variable
	 */
	public Set<String> getSeparators() {
		return this.separators;
	}
	
	/**
	 * @author Brammert Ottens, 11 nov. 2011
	 * @return the size of the space of the problem under the sending variable
	 */
	public long getSize() {
		return this.getPayload4();
	}
}
