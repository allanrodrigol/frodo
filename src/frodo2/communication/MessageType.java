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

package frodo2.communication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.jdom2.Element;

/** The (hierarchical) type of a message
 * @author Thomas Leaute
 */
public class MessageType implements Serializable, Comparable<MessageType> {
	
	/** serialVersionUID for serialization */
	private static final long serialVersionUID = 5665743800545692648L;

	/** The type of the root of the type hierarchy */
	private static final transient String ROOT_TYPE = "ALL";
	
	/** The root of the type hierarchy */
	public static final transient MessageType ROOT = new MessageType ();
	
	/** The type of system messages */
	public static final transient MessageType SYSTEM = new MessageType ("SYSTEM");
	
	/** The path in the type hierarchy */
	final private String[] path;

	/** Constructor
	 * @param path the type(s) of the message
	 * @warning Assumes the input path does not already start with the ROOT
	 */
	public MessageType(String... path) {
		
		// All message types are rooted at ROOT
		this.path = new String [path.length + 1];
		this.path[0] = ROOT_TYPE;
		System.arraycopy(path, 0, this.path, 1, path.length);
		
		assert this.path.length < 2 || ! this.path[1].equals(ROOT_TYPE);
	}
	
	/** @see java.lang.Object#equals(java.lang.Object) */
	@Override
	public boolean equals (Object o) {
		
		if (o == null) 
			return false;
		
		try {
			return Arrays.deepEquals(this.path, ((MessageType) o).path);
		} catch (ClassCastException e) {
			return false;
		}
	}
	
	/** @see java.lang.Object#hashCode() */
	@Override
	public int hashCode() {
		return Arrays.hashCode(this.path);
	}
	
	/** @return the parent of this message type in the hierarchy */
	public MessageType getParent () {
		
		// The root has no parent
		if (this.path.length == 1) 
			return null;
		
		return new MessageType (Arrays.copyOfRange(this.path, 1, path.length - 1)); // Starting at 1 to skip the ROOT
	}
	
	/** @see java.lang.Object#toString() */
	@Override
	public String toString () {
		return Arrays.toString(this.path);
	}
	
	/** @return the leaf type */
	public String getLeafType () {
		return this.path[this.path.length - 1];
	}
	
	/** * @return an XML representation of this message type */
	public Element toXML () {
		
		Element out = new Element ("type"); 
		
		Element typeElmt = out;
		for (int i = 1; i < this.path.length; i++) { // Start at i = 1 to skip the ROOT
			typeElmt.setAttribute("name", this.path[i]);
			if (i < this.path.length - 1) {
				Element subTypeElmt = new Element ("type");
				typeElmt.addContent(subTypeElmt);
				typeElmt = subTypeElmt;
			}
		}
		
		return out; 
	}
	
	/** Parses a message type from XML
	 * @param typeElmt 	a "type" tag
	 * @return the parsed message type
	 */
	public static MessageType fromXML (Element typeElmt) {
		
		if (typeElmt == null) 
			return null;
		
		ArrayList<String> newPath = new ArrayList<String> ();
		Element subElmt = typeElmt;
		while (subElmt != null) {
			newPath.add(subElmt.getAttributeValue("name"));
			subElmt = subElmt.getChild("type");
		}
		
		return new MessageType (newPath.toArray(new String [newPath.size()]));
	}
	
	/** Extends a message type with the input suffix, skipping ROOT if present
	 * @param subpath the suffix
	 * @return a new message type rooted at this
	 */
	public MessageType newChild (String... subpath) {
		
		if (subpath.length == 0) 
			return this;
		
		final int pad = (ROOT_TYPE.equals(subpath[0]) ? 1 : 0); // skip the root in the input subpath if present
		
		String[] newpath = new String[this.path.length - 1 + subpath.length - pad]; // -1 to skip my ROOT too
		System.arraycopy(path, 1, newpath, 0, path.length - 1);
		System.arraycopy(subpath, pad, newpath, path.length - 1, subpath.length - pad);
		
		return new MessageType (newpath);
	}
	
	/** Extends this message type with the input one, skipping the ROOT
	 * @param subType 	input message subtype
	 * @return a concatenation of this message type and the input one 
	 */
	public MessageType newChild (MessageType subType) {
		return this.newChild(subType.path);
	}
	
	/** Checks whether this message type is a parent of the input 
	 * @param child 	potential child message type
	 * @return whether this is a parent of the input
	 */
	public boolean isParent (MessageType child) {
		
		if (child.path.length < this.path.length) 
			return false;
		
		for (int i = this.path.length - 1; i >= 0; i--) 
			if (! this.path[i].equals(child.path[i])) 
				return false; 
		
		return true;
	}

	/** @see java.lang.Comparable#compareTo(java.lang.Object) */
	@Override
	public int compareTo(MessageType o) {
		
		int out; 
		
		for (int i = 0; i < this.path.length; i ++) {
			
			if (i >= o.path.length) 
				return 1; // o is a parent of this
			
			if ((out = this.path[i].compareTo(o.path[i])) != 0) 
				return out; 
		}
		
		// this is a parent of o 
		return -1;
	}

}
