//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Mateusz Ujma <mateusz.ujma@cs.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import explicit.SCCComputer.SCCMethod;

/**
 * Maximal end component computer for a nondeterministic model such as MDP.
 */
public class ECComputerDefault extends ECComputer
{
	/* The model to compute (M)ECs for */
	private NondetModel model;
	/* Computed list of MECs */
	private List<BitSet> mecs = new ArrayList<BitSet>();

	/**
	 * Build (M)EC computer for a given model.
	 */
	public ECComputerDefault(NondetModel model)
	{
		this.model = model;
	}

	// Methods for SCCComputer interface

	@Override
	public void computeMECs()
	{
		SCCComputer sccc = SCCComputer.createSCCComputer(SCCMethod.TARJAN, model);
		sccc.computeSCCs();
		
		List<BitSet> sccs = sccc.getSCCs();
		
		for(int i=0;i<sccs.size();i++) {
			if(isMEC(sccs.get(i))) {
				System.out.println("SCCs " + sccs.get(i));
				mecs.add(sccs.get(i));
			}
		}
	}

	@Override
	public List<BitSet> getMECs()
	{
		return mecs;
	}
	
	private boolean isMEC(BitSet b) {
		if(b.cardinality() == 0) return false;
		
		int state = b.nextSetBit(0);
		while(state != -1) {
			boolean atLeastOneAction = false;
			for(int i=0;i<model.getNumChoices(state);i++) {
				if(model.allSuccessorsInSet(state, i, b)) {
					atLeastOneAction = true;
				}
			}
			if(!atLeastOneAction) {
				return false;
			}
			state = b.nextSetBit(state+1);
		}
		
		return true;
	}
}
