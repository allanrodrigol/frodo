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

package frodo2.solutionSpaces.JaCoP;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.jacop.constraints.Constraint;
import org.jacop.core.IntDomain;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.core.ValueEnumeration;
import org.jacop.core.Var;

/** A JaCoP extensional constraint encoded by a hypercube for faster lookups
 * @author Thomas Leaute
 */
public class ExtensionalSupportHypercube extends Constraint {
	
	/** The JaCoP store */
	private Store store;
	
	/** The variables */
	private IntVar[] vars;
	
	/** this.vars.length - 2 */
	private final int nbrVarsMin2;
	
	/** The multi-dimensional array indicating the utility of each possible tuple */
	private Object hypercube;
	
	/** For each variable (except the utility variable), for each value in its domain, the index of this value in the domain */
	private ArrayList< HashMap<Integer, Integer> > indexes;

	/** Constructor
	 * @param vars 		the list of variables, the last being the utility variable
	 * @param tuples 	the list of allowed tuples
	 */
	public ExtensionalSupportHypercube(IntVar[] vars, ArrayList<int[]> tuples) {
		
		this.vars = vars;
		this.nbrVarsMin2 = vars.length - 2;
		
		// Create the variable value indexes
		final int nbrVars = vars.length;
		this.indexes = new ArrayList< HashMap<Integer, Integer> > (nbrVars - 1); // skipping the last variable, which is the utility variable
		for (int i = 0; i < nbrVars - 1; i++) {
			HashMap<Integer, Integer> map = new HashMap<Integer, Integer> ();
			this.indexes.add(map);
			
			int j = 0;
			for (ValueEnumeration iter = vars[i].dom().valueEnumeration(); iter.hasMoreElements(); ) 
				map.put(iter.nextElement(), j++);
		}
		
		// Create the hypercube
		int[] dimensions = new int [nbrVars - 1]; // skipping the last variable, which is the utility variable
		for (int i = vars.length - 2; i >= 0; i--) 
			dimensions[i] = vars[i].dom().getSize();
		this.hypercube = Array.newInstance(Integer.class, dimensions);
		
		// Populate the hypercube
		for (int[] tuple : tuples) {
			
			// Slide the hypercube following all non-utility variables but the last
			Object slice = this.hypercube;
			for (int i = 0; i < nbrVarsMin2; i++) 
				slice = Array.get(slice, this.indexes.get(i).get(tuple[i]));
			
			// Record the utility of the tuple
			Array.set(slice, this.indexes.get(nbrVarsMin2).get(tuple[nbrVarsMin2]), tuple[nbrVars - 1]);
		}
	}

	/** @see org.jacop.constraints.Constraint#arguments() */
	@Override
	public ArrayList<Var> arguments() {
		return new ArrayList<Var> (Arrays.asList(this.vars));
	}

	/** @see org.jacop.constraints.Constraint#consistency(org.jacop.core.Store) */
	@Override
	public void consistency(Store arg0) {
		
//		System.out.println(Arrays.toString(this.vars));
		
		// Try to look up the value of the utility variable
		Object slice = this.hypercube;
		IntVar var = null;
		for (int i = 0; i < this.nbrVarsMin2; i++) { // for each non-utility variable except the last
			
			// Fail to check consistency if the variable's domain is not a singleton
			/// @todo Propagate earlier
			if (! (var = this.vars[i]).dom().singleton()) 
				return;
			
			// Look up the value index of the variable's current value
			Integer index = this.indexes.get(i).get(var.value());
			if (index == null) // the variable has been assigned an unknown value
				throw Store.failException;
			
			slice = Array.get(slice, index);
		}
		
		// Fail to check consistency if the variable's domain is not a singleton
		/// @todo Propagate earlier
		if (! (var = this.vars[this.nbrVarsMin2]).dom().singleton()) 
			return;
		
		// Look up the value index of the variable's current value
		Integer index = this.indexes.get(this.nbrVarsMin2).get(var.value());
		if (index == null) // the variable has been assigned an unknown value
			throw Store.failException;

		// Look up the utility 
		Integer util = (Integer) Array.get(slice, index);
		if (util == null) // infeasible tuple
			throw Store.failException;
		else 
			(var = this.vars[this.nbrVarsMin2 + 1]).dom().in(this.store.level, var, util, util);
	}

	/** @see org.jacop.constraints.Constraint#getConsistencyPruningEvent(org.jacop.core.Var) */
	@Override
	public int getConsistencyPruningEvent(Var arg0) {
		return IntDomain.GROUND;
	}

	/** @see org.jacop.constraints.Constraint#impose(org.jacop.core.Store) */
	@Override
	public void impose(Store store) {
		this.store = store;
		for (IntVar var : this.vars) 
			var.putModelConstraint(this, this.getConsistencyPruningEvent(var));
		store.addChanged(this);
		store.countConstraint();
	}

	/** @see org.jacop.constraints.Constraint#increaseWeight() */
	@Override
	public void increaseWeight() {
		for (IntVar var : this.vars)
			var.weight++;
	}

	/** @see org.jacop.constraints.Constraint#removeConstraint() */
	@Override
	public void removeConstraint() {
		for (IntVar var : this.vars)
			var.removeConstraint(this);
	}

	/** @see org.jacop.constraints.Constraint#satisfied() */
	@Override
	public boolean satisfied() {
		/// @todo Auto-generated method stub
		assert false : "Not yet implemented";
		return false;
	}

	/** @see org.jacop.constraints.Constraint#toString() */
	@Override
	public String toString() {
		
		StringBuffer out = new StringBuffer ();
		
		out.append(this.id()).append("(");
		
		out.append(Arrays.toString(this.vars));
//		out.append(", ").append(Arrays.deepToString((Object[]) this.hypercube));
		
		out.append(")");
		
		return out.toString();
	}

}
