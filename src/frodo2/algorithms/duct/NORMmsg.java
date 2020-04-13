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

import frodo2.algorithms.duct.Normalize;
import frodo2.communication.Message;
import frodo2.communication.MessageWith3Payloads;
import frodo2.solutionSpaces.AddableReal;

/**
 * @author Brammert Ottens, 18 aug. 2011
 * 
 */
public class NORMmsg extends MessageWith3Payloads<String, AddableReal, AddableReal> {

	/**
	 * Empty constructor
	 */
	public NORMmsg() {}
	
	/**
	 * Constructor
	 * 
	 * @param receiver 	the receiver of the message
	 * @param minus		the shift value
	 * @param divide	the scaling value
	 */
	public NORMmsg(String receiver, AddableReal minus, AddableReal divide) {
		super(Normalize.NORM_MSG_TYPE, receiver, minus, divide);
		assert receiver != null;
		assert divide != null;
	}
	
	/** @see Message#writeExternal(java.io.ObjectOutput) */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(this.getPayload1());
		if(this.getPayload2() != null) {
			out.writeBoolean(true);
			this.getPayload2().writeExternal(out);
		} else
			out.writeBoolean(false);
		this.getPayload3().writeExternal(out);
	}

	/** @see Message#readExternal(java.io.ObjectInput) */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		super.setPayload1((String) in.readObject());
		if(in.readBoolean()) {
			AddableReal minus = new AddableReal(0);
			minus.readExternal(in);
			super.setPayload2((AddableReal) minus.readResolve());
		}
		AddableReal divide = new AddableReal(0);
		divide.readExternal(in);
		super.setPayload3((AddableReal) divide.readResolve());
	}
	
	/**
	 * @author Brammert Ottens, 29 aug. 2011
	 * @return the intended receiver of this message
	 */
	public String getReceiver() {
		return this.getPayload1();
	}
	
	/**
	 * @author Brammert Ottens, 29 aug. 2011
	 * @return the shift
	 */
	public AddableReal getMinus() {
		return this.getPayload2();
	}
	
	/**
	 * @author Brammert Ottens, 29 aug. 2011
	 * @return the scaling factor
	 */
	public AddableReal getDivide() {
		return this.getPayload3();
	}
	
}
