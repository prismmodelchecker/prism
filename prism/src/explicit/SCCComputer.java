//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Christian von Essen <christian.vonessen@imag.fr> (Verimag, Grenoble)
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

/**
 * Abstract class for classes that compute (B)SCCs,
 * i.e. (bottom) strongly connected components, for a model's transition graph.
 */
public abstract class SCCComputer
{
	// Method used for finding (B)SCCs
	public enum SCCMethod {
		TARJAN;
		public String fullName()
		{
			switch (this) {
			case TARJAN:
				return "Tarjan";
			default:
				return this.toString();
			}
		}
	};

	/**
	 * Static method to create a new SCCComputer object, depending on requested method.
	 */
	public static SCCComputer createSCCComputer(SCCMethod sccMethod, Model model)
	{
		return new SCCComputerTarjan(model);
	}

	/**
	 * Compute SCCs and store them. They can be retrieved using {@link #getSCCs()}.
	 */
	public abstract void computeSCCs();

	/**
	 * Compute SCCs and store them. They can be retrieved using {@link #getBSCCs()}.
	 */
	public abstract void computeBSCCs();

	/**
	 * Get the list of computed SCCs.
	 */
	public abstract List<BitSet> getSCCs();

	/**
	 * Get the list of computed BSCCs.
	 */
	public abstract List<BitSet> getBSCCs();
	
	/**
	 * Get the states not in any BSCC.
	 */
	public abstract BitSet getNotInBSCCs();
}
