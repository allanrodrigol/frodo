package frodo2.algorithms.localSearch.dsasdp;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import frodo2.communication.MessageType;
import frodo2.communication.MessageWith3Payloads;
import frodo2.solutionSpaces.Addable;

public class ValueMessage<V extends Addable<V>> extends MessageWith3Payloads<Integer,String,V> implements Externalizable {
	
	public ValueMessage(Integer cycle, String variable, V value) {
		super(DSASDP.VALUE_MSG_TYPE, cycle, variable, value);
	}
	
	public ValueMessage(MessageType type, Integer cycle, String variable, V value) {
		super(type, cycle, variable, value);
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(super.getPayload1());
		out.writeObject(super.getPayload2());
		out.writeObject(super.getPayload3());
	}

	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.setPayload1((Integer) in.readObject());
		super.setPayload2((String) in.readObject());
		super.setPayload3((V) in.readObject());
	}

	public Integer getCycle() {
		return this.getPayload1();
	}

	public String getVariable() {
		return this.getPayload2();
	}
	
	public V getValue() {
		return this.getPayload3();
	}

	private static final long serialVersionUID = 6485548935259680832L;
}