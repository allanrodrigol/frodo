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

import frodo2.algorithms.duct.Sampling;
import frodo2.communication.Message;
import frodo2.communication.MessageWithPayload;
import frodo2.solutionSpaces.AddableReal;

/**
 * Message that contains the final bound on the solution quality
 * 
 * @author Brammert Ottens
 *
 */
public class BoundStatsMsg extends MessageWithPayload<AddableReal> {

	/**
	 * Empty constructor
	 */
	public BoundStatsMsg() {};
	
	/**
	 * Constructor
	 * 
	 * @param finalBound the final bound on the solution quality
	 */
	public BoundStatsMsg(AddableReal finalBound) {
		super(Sampling.BOUND_MSG_TYPE, finalBound);
	}
	
	/**
	 * @author Brammert Ottens, Dec 29, 2011
	 * @return the final bound on the solution quality
	 */
	public AddableReal getFinalBound() {
		return this.getPayload();
	}
	
	/** @see Message#writeExternal(java.io.ObjectOutput) */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		this.getPayload().writeExternal(out);
	}
	
	/** @see Message#readExternal(java.io.ObjectInput) */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		AddableReal stat = new AddableReal(0);
		stat.readExternal(in);
		super.setPayload((AddableReal) stat.readResolve());
	}
	
}
