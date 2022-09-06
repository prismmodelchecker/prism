//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.util.BitSet;
import java.util.List;

import prism.PrismComponent;
import prism.PrismException;

/**
 * Abstract class for (explicit) classes that compute (M)ECs, i.e. (maximal) end components,
 * for a nondeterministic model such as an MDP.
 */
public abstract class ECComputer extends PrismComponent
{
	/**
	 * Static method to create a new ECComputer object, depending on current settings.
	 */
	public static ECComputer createECComputer(PrismComponent parent, NondetModel model) throws PrismException
	{
		// Only one algorithm implemented currently
		return new ECComputerDefault(parent, model);
	}

	/**
	 * Base constructor.
	 */
	public ECComputer(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	/**
	 * Compute states of maximal end components (MECs) and store them.
	 * They can be retrieved using {@link #getMECStates()}.
	 */
	public abstract void computeMECStates() throws PrismException;

	/**
	 * Compute states of all maximal end components (MECs) in the submodel obtained
	 * by restricting this one to the set of states {@code restrict}, and store them.
	 * They can be retrieved using {@link #getMECStates()}.
	 * If {@code restrict} is null, we look at the whole model, not a submodel.
	 * @param restrict BitSet for the set of states to restrict to.
	 */
	public abstract void computeMECStates(BitSet restrict) throws PrismException;

	/**
	 * Compute states of all accepting maximal end components (MECs) in the submodel obtained
	 * by restricting this one to the set of states {@code restrict}, and store them,
	 * where acceptance is defined as those which intersect with {@code accept}.
	 * They can be retrieved using {@link #getMECStates()}.
	 * If {@code restrict} is null, we look at the whole model, not a submodel.
	 * If {@code accept} is null, the acceptance condition is trivially satisfied.
	 * @param restrict BitSet for the set of states to restrict to
	 * @param accept BitSet for the set of accepting states
	 */
	public abstract void computeMECStates(BitSet restrict, BitSet accept) throws PrismException;
	
	/**
	 * Get the list of states for computed MECs.
	 */
	public abstract List<BitSet> getMECStates();
}
