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
import java.lang.reflect.Array;

import frodo2.communication.Message;
import frodo2.communication.MessageType;
import frodo2.communication.MessageWith2Payloads;
import frodo2.solutionSpaces.Addable;

/**
 * @author Brammert Ottens, 7 jul. 2011
 * 
 * @param <V> the type used for domain values
 */
public class VALUEmsg <V extends Addable<V>> extends MessageWith2Payloads<String, String> {
	
	/** Used for externalization of messages */
	public static Addable<?> DOMAIN_VALUE;
	
	/** The variable names*/
	private String[] variables;
	
	/** The context in which sampling must occur */
	private V[] values;
	
	/**
	 * Empty constructor
	 */
	public VALUEmsg() {}
	
	/**
	 * Constructor
	 * @param type		the type of the message 
	 * @param sender	the sender of the message
	 * @param receiver	the name of the variable that is to receive this message
	 * @param variables the names of the variables
	 * @param values 	the reported values
	 */
	public VALUEmsg(MessageType type, String sender, String receiver, String[] variables, V[] values) {
		super(type, sender, receiver);
		this.variables = variables;
		this.values = values;
		assert variables.length == values.length;
		for(V value : values)
			assert value != null;
	}
	
	/** @see Message#writeExternal(java.io.ObjectOutput) */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeShort(variables.length);
		for(String var : variables) {
			out.writeObject(var);
		}
		
		for(int i = 0; i < values.length; i++) {
			values[i].writeExternal(out);
		}
	}

	/** @see Message#readExternal(java.io.ObjectInput) */
	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		int size = in.readShort();
		variables = new String[size];
		for(int i = 0; i < size; i++)
			variables[i] = (String)in.readObject();
		
		V val = (V)DOMAIN_VALUE.getZero();
		values = (V[])Array.newInstance(val.getClass(), size);
				
		for(int i = 0; i < size; i++) {
			val.readExternal(in);
			values[i] = (V) val.readResolve();
		}
	}
	
	/**
	 * @author Brammert Ottens, 7 jul. 2011
	 * @return the sender of the message
	 */
	public String getSender() {
		return this.getPayload1();
	}
	
	/**
	 * @author Brammert Ottens, 7 jul. 2011
	 * @return the name of the variable that is to receive this message
	 */
	public String getReceiver() {
		return this.getPayload2();
	}
	
	/**
	 * @author Brammert Ottens, 29 aug. 2011
	 * @return the variable names
	 */
	public String[] getVariables() {
		return this.variables;
	}
	
	/**
	 * @author Brammert Ottens, 7 jul. 2011
	 * @return the value that has been sampled
	 */
	public V[] getValues() {
		return values;
	}

}
