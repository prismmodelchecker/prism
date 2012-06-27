//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

public class Utils
{
	/**
	 * Compute the minimum or maximum value over a subset of an array of doubles.
	 * @param array The array
	 * @param subset The subset
	 * @param min Min or max (true = min, false = max) 
	 */
	public static double minMaxOverArraySubset(double[] array, Iterable<Integer> subset, boolean min)
	{
		if (min)
			return minOverArraySubset(array, subset);
		else
			return maxOverArraySubset(array, subset);
	}

	/**
	 * Compute the minimum value over a subset of an array of doubles.
	 * @param array The array
	 * @param subset The subset
	 */
	public static double minOverArraySubset(double[] array, Iterable<Integer> subset)
	{
		double d;
		d = Double.POSITIVE_INFINITY;
		for (int j : subset) {
			if (array[j] < d)
				d = array[j];
		}
		return d;
	}

	/**
	 * Compute the maximum value over a subset of an array of doubles.
	 * @param array The array
	 * @param subset The subset
	 */
	public static double maxOverArraySubset(double[] array, Iterable<Integer> subset)
	{
		double d;
		d = Double.NEGATIVE_INFINITY;
		for (int j : subset) {
			if (array[j] > d)
				d = array[j];
		}
		return d;
	}

	/**
	 * Create an n-element array of doubles (0s and 1s) from a BitSet.
	 *  @param bs The bitset specifying 0s and 1s
	 *  @param n The size of the array.
	 */
	public static double[] bitsetToDoubleArray(BitSet bs, int n)
	{
		int i;
		double res[] = new double[n];
		for (i = 0; i < n; i++)
			res[i] = bs.get(i) ? 1.0 : 0.0;
		return res;
	}

	/**
	 * Extend a double array to be at least as big as requested size.
	 * @param array The array to be resized
	 * @param nOld The size of the old array (not necessarily array.length)
	 * @param nNew The desired new size
	 * @param valNew Value to initialise new elements with
	 * @return The new array
	 */
	public static double[] extendDoubleArray(double array[], int nOld, int nNew, double valNew)
	{
		int i, n, n2;
		double[] arrayNew;
		// Do nothing for null pointers
		if (array == null)
			return null;
		// If array already long enough, just return 
		n = array.length;
		if (n > nNew)
			return array;
		// Create new array (of size nNew + some spare)
		n2 = nNew + 100;
		arrayNew = new double[n2];
		// Copy across old values
		for (i = 0; i < nOld; i++) {
			arrayNew[i] = array[i];
		}
		// Initialise new values
		for (i = nOld; i < nNew; i++) {
			arrayNew[i] = valNew;
		}
		return arrayNew;
	}

	/**
	 * Clone a double array.
	 * @param array The array to be cloned
	 * @return The new array
	 */
	public static double[] cloneDoubleArray(double array[])
	{
		int i, n;
		double[] arrayNew;
		// Do nothing for null pointers
		if (array == null)
			return null;
		// Otherwise copy and return
		n = array.length;
		arrayNew = new double[n];
		for (i = 0; i < n; i++) {
			arrayNew[i] = array[i];
		}
		return arrayNew;
	}

	/**
	 * Clone an integer array.
	 * @param array The array to be cloned
	 * @return The new array
	 */
	public static int[] cloneIntArray(int array[])
	{
		int i, n;
		int[] arrayNew;
		// Do nothing for null pointers
		if (array == null)
			return null;
		// Otherwise copy and return
		n = array.length;
		arrayNew = new int[n];
		for (i = 0; i < n; i++) {
			arrayNew[i] = array[i];
		}
		return arrayNew;
	}

	/**
	 * Copy a double array.
	 * @param array The array to be cloned
	 * @param copy The destination array (should exist and be same size)
	 */
	public static void copyDoubleArray(double array[], double copyTo[])
	{
		int i, n;
		// Do nothing for null pointers
		if (array == null)
			return;
		// Otherwise copy
		n = array.length;
		for (i = 0; i < n; i++) {
			copyTo[i] = array[i];
		}
	}

	/**
	 * Copy an integer array.
	 * @param array The array to be cloned
	 * @param copy The destination array (should exist and be same size)
	 */
	public static void copyIntArray(int array[], int copyTo[])
	{
		int i, n;
		// Do nothing for null pointers
		if (array == null)
			return;
		// Otherwise copy
		n = array.length;
		for (i = 0; i < n; i++) {
			copyTo[i] = array[i];
		}
	}

	/**
	 * Test if two double arrays are equal.
	 */
	public static boolean doubleArraysAreEqual(double array1[], double array2[])
	{
		int i, n;
		if (array1 == null)
			return (array2 == null);
		n = array1.length;
		if (n != array2.length)
			return false;
		for (i = 0; i < n; i++) {
			if (array1[i] != array2[i])
				return false;
			;
		}
		return true;
	}

	/**
	 * Test if two int arrays are equal.
	 */
	public static boolean intArraysAreEqual(int array1[], int array2[])
	{
		int i, n;
		if (array1 == null)
			return (array2 == null);
		n = array1.length;
		if (n != array2.length)
			return false;
		for (i = 0; i < n; i++) {
			if (array1[i] != array2[i])
				return false;
		}
		return true;
	}

	/**
	 * Returns true if int array 'array' contains value 'val'.
	 */
	public static boolean intArrayContains(int val, int[] array)
	{
		for (int i = 0; i < array.length; i++)
			if (val == array[i])
				return true;
		return false;
	}
}
