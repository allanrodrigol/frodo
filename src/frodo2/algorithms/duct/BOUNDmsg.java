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
import java.util.HashSet;
import java.util.Set;

import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.MessageWith5Payloads;
import frodo2.solutionSpaces.AddableReal;

/**
 * @author Brammert Ottens, 9 aug. 2011
 * 
 */
public class BOUNDmsg extends MessageWith5Payloads<String, AddableReal, AddableReal, Integer, Long> {
	
	/** The separator of the reporting variable */
	Set<String> separator;
	

	/**
	 * Empty constructor
	 */
	public BOUNDmsg() {}
	
	/**
	 * Constructor
	 * 
	 * @param type 			the type of the message (either BOUND_MSG_TYPE or NORM_MSG_TYPE
	 * @param receiver		the intended receiver of the message
	 * @param lowerBound	the lower bound
	 * @param upperBound	the upper bound
	 * @param counter		the number of spaces that have been visited
	 * @param size			Size of the domain
	 * @param separator		the separator of the sending variable 
	 */
	public BOUNDmsg(MessageType type, String receiver, AddableReal lowerBound, AddableReal upperBound, int counter, long size, Set<String> separator) {
		super(type, receiver, lowerBound, upperBound, counter, size);
		this.separator = separator;
	}
	
	/** @see Message#writeExternal(java.io.ObjectOutput) */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(type);
		out.writeObject(this.getPayload1());
		this.getPayload2().writeExternal(out);
		this.getPayload3().writeExternal(out);
		out.writeInt(this.getPayload4());
		out.writeLong(this.getPayload5());
		out.writeShort(separator.size());
		for(String var : separator)
			out.writeObject(var);
	}

	/** @see Message#readExternal(java.io.ObjectInput) */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		type = (MessageType) in.readObject();
		super.setPayload1((String) in.readObject());
		AddableReal lb = new AddableReal(0);
		lb.readExternal(in);
		super.setPayload2((AddableReal) lb.readResolve());
		AddableReal ub = new AddableReal(0);
		ub.readExternal(in);
		super.setPayload3((AddableReal) ub.readResolve());
		separator = new HashSet<String>();
		super.setPayload4(in.readInt());
		super.setPayload5(in.readLong());
		int size = in.readShort();
		for(int i = 0; i < size; i++)
			separator.add((String)in.readObject());
	}
	
	/**
	 * @author Brammert Ottens, 17 aug. 2011
	 * @return the intended receiver of the message
	 */
	public String getReceiver() {
		return this.getPayload1();
	}
	
	/**
	 * @author Brammert Ottens, 9 aug. 2011
	 * @return the lower bound
	 */
	public AddableReal getLowerBound() {
		return this.getPayload2();
	}
	
	/**
	 * @author Brammert Ottens, 9 aug. 2011
	 * @return the upper bound
	 */
	public AddableReal getUpperBound() {
		return this.getPayload3();
	}
	
	/**
	 * @author Brammert Ottens, 18 aug. 2011
	 * @return the number of spaces that have been visited
	 */
	public int getCounter() {
		return this.getPayload4();
	}
	
	/**
	 * @author Brammert Ottens, 11 nov. 2011
	 * @return the size of the space rooted at the sender of this message
	 */
	public long getSize() {
		return this.getPayload5();
	}
	
	/**
	 * @author Brammert Ottens, 25 aug. 2011
	 * @return the separator of the sending variable
	 */
	public Set<String> getSeperator() {
		return separator;
	}

}
