//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Xueyi Zou <xz972@york.ac.uk> (University of York)
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

import java.util.Arrays;

public class DiscretizedBelief
{
	public int[] intBeliefArray;

	public DiscretizedBelief(double[] belief, int D)
	{
		int beliefSize = belief.length;
		intBeliefArray = new int[beliefSize];
		for (int i = 0; i < beliefSize; i++) {
			intBeliefArray[i] = (int) Math.ceil(belief[i] * D);
		}
	}

	@Override
	public int hashCode()
	{
		return Arrays.hashCode(intBeliefArray);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		DiscretizedBelief other = (DiscretizedBelief) obj;

		if (intBeliefArray.length != other.intBeliefArray.length) {
			return false;
		} else {
			for (int i = 0; i < intBeliefArray.length; i++) {
				if (Math.abs(intBeliefArray[i] - other.intBeliefArray[i]) > 1e-6)
					return false;
			}
		}
		return true;
	}

	@Override
	public String toString()
	{
		return Arrays.toString(intBeliefArray);
	}
}
