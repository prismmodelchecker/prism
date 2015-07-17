//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

package explicit.rewards;

import explicit.Model;
import explicit.Product;

/**
 * Classes that provide (read) access to explicit-state rewards for an STPG.
 * See the {@link explicit.STPG} interface for details of the accompanying model,
 * in particular, for an explanation of nested transitions. 
 */
public interface STPGRewards extends MDPRewards
{
	/**
	 * Get the transition reward for the {@code i,j}th nested choice from state {@code s}.
	 */
	public abstract double getNestedTransitionReward(int s, int i, int j);

	/**
	 * Build an MDPRewards object containing all the same rewards except for the nested ones.
	 */
	public abstract MDPRewards buildMDPRewards();

	@Override
	public STPGRewards liftFromModel(Product<? extends Model> product);
}
