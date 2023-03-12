//==============================================================================
//	
//	Copyright (c) 2022-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

/**
 * Flat storage of a distribution with probabilities as intervals of doubles
 */
public class DoubleIntervalDistribution
{
	public int size;
	public double[] lower;
	public double[] upper;
	public int[] index;

	public DoubleIntervalDistribution(int size)
	{
		this.size = size;
		this.lower = new double[size];
		this.upper = new double[size];
		this.index = new int[size];
	}
	
	@Override
	public String toString()
	{
		String s = "";
		for (int i = 0; i < size; i++) {
			if (i > 0) s += " + ";
			s += "[" + lower[i] + "," + upper[i] + "]:" + index[i];
		}
		return s;
	}
}
