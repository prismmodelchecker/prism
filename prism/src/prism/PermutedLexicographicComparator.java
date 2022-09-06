//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
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

package prism;

import java.util.Arrays;
import java.util.Comparator;

/**
 * A class that allows to compare points w.r.g. a lexicographic order.
 * The order of elements is given by the array {@code dimensionPermutation}
 * passed in the constructor {@link #PermutedLexicographicComparator(int[],boolean[])}.
 * <br>
 * Note that the comparator is only capable of comparing points with equal
 * dimension. This probably violates the specification of the {@link Comparator}
 * class, but it shouldn't be any problem as comparing points of different dimensions
 * makes no sense in our case.
 */
public class PermutedLexicographicComparator implements Comparator<Point>
{
	private int[] permutation;
	private boolean[] increasing;
	
	/**
	 * Creates a new instance of the comparator.
	 * @param dimensionPermutation The order of coordinates, the length of
	 * the array must be equal to the dimension of points to be compared. For example,
	 * supposing the dimension of points to compare is 3, the array [2,0,1] says
	 * that the lexicographic order should be used in which the last coordinate
	 * has the highest priority, and then the first two follow.
	 * @param increasing if increasing[i] is true, then the ith dimension
	 * will be used such that the points with the smaller values go first, otherwise they go last
	 */
	public PermutedLexicographicComparator(int[] dimensionPermutation, boolean[] increasing) {
		//check that the array is a valid permutation
		int[] clone = dimensionPermutation.clone();
		//TODO check size etc.
		this.increasing = increasing;
		Arrays.sort(clone);
		for(int i = 0; i < clone.length; i++)
			if (clone[i] != i)
				throw new IllegalArgumentException("The array is not a valid permutation");
		this.permutation = dimensionPermutation;
	}

	@Override
	public int compare(Point o1, Point o2)
	{
		//check if the input is allowed;
		if (o1.getDimension() > permutation.length
				|| o2.getDimension() > permutation.length)
			throw new ClassCastException("The dimension of the point is greater that comparator's dimension");
		
		if(o1.isCloseTo(o2))
			return 0;
		
		return compare(o1,o2,0);
	}
	
	/**
	 * Compares two points, starting with the coordinate with the
	 * {@code i}-th highest priority. 
	 * @param o1
	 * @param o2
	 * @param i
	 * @return
	 */
	protected int compare(Point o1, Point o2, int i)
	{
		int dim = this.permutation[i];
		if ( o1.getCoord(dim) < o2.getCoord(dim))
			return (increasing[dim]) ? -1 : 1;
		else if (o1.getCoord(dim) > o2.getCoord(dim))
			return (increasing[dim]) ? 1 : -1;
		else return compare(o1, o2, i+1);
	}
}
