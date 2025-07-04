//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import common.iterable.PrimitiveIterable;
import param.BigRational;

/**
 * Various general-purpose utility methods in Java
 */
public class PrismUtils
{
	// Threshold for comparison of doubles
	public static double epsilonDouble = 1e-12;

	/**
	 * Compute logarithm of x to base b.
	 */
	public static double log(double x, double b)
	{
		// If base is <=0 or ==1 (or +Inf/NaN), then result is NaN
		if (b <= 0 || b == 1 || (Double.isInfinite(b)) || Double.isNaN(b))
			return Double.NaN;

		// Otherwise, log_b (x) is log(x) / log(b)
		return Math.log(x) / Math.log(b);
	}

	/**
	 * Compute logarithm of x to base 2.
	 */
	public static double log2(double x)
	{
		return Math.log(x) / Math.log(2);
	}

	/**
	 * See if two doubles are within epsilon of each other (absolute error).
	 */
	public static boolean doublesAreCloseAbs(double d1, double d2, double epsilon)
	{
		// Deal with infinite cases
		if (Double.isInfinite(d1)) {
			return Double.isInfinite(d2) && (d1 > 0) == (d2 > 0);
		} else if (Double.isInfinite(d2)) {
			return false;
		}
		// Compute/check error
		return (Math.abs(d1 - d2) < epsilon);
	}

	/**
	 * See if two doubles are within epsilon of each other (relative error).
	 */
	public static boolean doublesAreCloseRel(double d1, double d2, double epsilon)
	{
		// Deal with infinite cases
		if (Double.isInfinite(d1)) {
			return Double.isInfinite(d2) && (d1 > 0) == (d2 > 0);
		} else if (Double.isInfinite(d2)) {
			return false;
		}
		// Compute/check error
		if (d2 == 0) {
			// If both are zero, error is 0; otherwise +inf
			return d1 == 0;
		}
		return Math.abs((d1 - d2) / d1) < epsilon;
	}

	/**
	 * See if two doubles are within epsilon of each other (relative or absolute error).
	 * @param abs Absolute if true, relative if false
	 */
	public static boolean doublesAreClose(double d1, double d2, double epsilon, boolean abs)
	{
		if (abs) {
			return doublesAreCloseAbs(d1, d2, epsilon);
		} else {
			return doublesAreCloseRel(d1, d2, epsilon);
		}
	}

	/**
	 * See if two arrays of doubles are all within epsilon of each other (relative or absolute error).
	 */
	public static boolean doublesAreClose(double d1[], double d2[], double epsilon, boolean abs)
	{
		int n = Math.min(d1.length, d2.length);
		if (abs) {
			for (int i = 0; i < n; i++) {
				if (!doublesAreCloseAbs(d1[i], d2[i], epsilon))
					return false;
			}
		} else {
			for (int i = 0; i < n; i++) {
				if (!doublesAreCloseRel(d1[i], d2[i], epsilon))
					return false;
			}
		}
		return true;
	}

	/**
	 * See if, for all the entries given by the {@code indizes}
	 * iterator, two arrays of doubles are all within epsilon of each other (relative or absolute error).
	 * <br>
	 * Considers Inf == Inf and -Inf == -Inf.
	 */
	public static boolean doublesAreClose(double d1[], double d2[], PrimitiveIterable.OfInt indizes, double epsilon, boolean abs)
	{
		return doublesAreClose(d1, d2, indizes.iterator(), epsilon, abs);
	}

	/**
	 * See if, for all the entries given by the {@code indizes}
	 * iterator, two arrays of doubles are all within epsilon of each other (relative or absolute error).
	 * <br>
	 * Considers Inf == Inf and -Inf == -Inf.
	 */
	public static boolean doublesAreClose(double d1[], double d2[], PrimitiveIterator.OfInt indizes, double epsilon, boolean abs)
	{
		if (abs) {
			while (indizes.hasNext()) {
				int i = indizes.nextInt();
				if (!doublesAreCloseAbs(d1[i], d2[i], epsilon))
					return false;
			}
		} else {
			while (indizes.hasNext()) {
				int i = indizes.nextInt();
				if (!doublesAreCloseRel(d1[i], d2[i], epsilon))
					return false;
			}
		}
		return true;
	}

	/**
	 * See if two maps to doubles are all within epsilon of each other (relative or absolute error).
	 */
	public static <X> boolean doublesAreClose(HashMap<X,Double> map1, HashMap<X,Double> map2, double epsilon, boolean abs)
	{
		Set<Entry<X,Double>> entries = map1.entrySet();
		for (Entry<X,Double> entry : entries) {
			double d1 = (double) entry.getValue();
			if (map2.get(entry.getKey()) != null) {
				double d2 = (double) map2.get(entry.getKey());
				if (abs) {
					if (!PrismUtils.doublesAreCloseAbs(d1, d2, epsilon))
						return false;
				} else {
					if (!PrismUtils.doublesAreCloseRel(d1, d2, epsilon))
						return false;
				}
			} else {
				// Only check over common elements
			}
		}
		return true;
	}

	/**
	 * Measure supremum norm, either absolute or relative,
	 * return the maximum difference.
	 */
	public static <X> double measureSupNorm(HashMap<X,Double> map1, HashMap<X,Double> map2, boolean abs)
	{
		double value = 0;
		Set<Entry<X,Double>> entries = map1.entrySet();
		for (Entry<X,Double> entry : entries) {
			double diff;
			double d1 = entry.getValue();
			if (map2.get(entry.getKey()) != null) {
				double d2 = map2.get(entry.getKey());
				if (abs) {
					diff = measureSupNormAbs(d1, d2);
				} else {
					diff = measureSupNormRel(d1, d2);
				}
				if (diff > value) {
					value = diff;
				}
			} else {
				// Only check over common elements
			}
		}
		return value;
	}

	/**
	 * Measure supremum norm, either absolute or relative,
	 * return the maximum difference.
	 */
	public static double measureSupNorm(double[] d1, double[] d2, boolean abs)
	{
		int n = Math.min(d1.length, d2.length);
		double value = 0;
		if (abs) {
			for (int i=0; i < n; i++) {
				double diff = measureSupNormAbs(d1[i], d2[i]);
				if (diff > value)
					value = diff;
			}
		} else {
			for (int i=0; i < n; i++) {
				double diff = measureSupNormRel(d1[i], d2[i]);
				if (diff > value)
					value = diff;
			}
		}
		return value;
	}

	/**
	 * Measure supremum norm for two values, absolute.
	 */
	public static double measureSupNormAbs(double d1, double d2)
	{
		if (Double.isInfinite(d1) && d1==d2)
			return 0;
		return Math.abs(d1 - d2);
	}

	/**
	 * Measure supremum norm for two values, relative,
	 * with the first value used as the divisor.
	 */
	public static double measureSupNormRel(double d1, double d2)
	{
		if (d1==d2)
			return 0;
		return (Math.abs(d1 - d2) / d1);
	}

	/**
	 * Measure supremum norm, for all the entries given by the {@code indizes}
	 * iterator, for an interval iteration.
	 */
	public static double measureSupNormInterval(double[] lower, double[] upper, boolean abs, PrimitiveIterator.OfInt indizes)
	{
		double value = 0;
		while (indizes.hasNext()) {
			int i = indizes.nextInt();
			double diff = measureSupNormInterval(lower[i], upper[i], abs);
			if (diff > value)
				value = diff;
		}
		return value;
	}

	/**
	 * Measure supremum norm, either absolute or relative, for an interval iteration,
	 * return the maximum difference.
	 */
	public static double measureSupNormInterval(double[] lower, double[] upper, boolean abs)
	{
		int n = Math.min(lower.length, upper.length);
		double value = 0;
		for (int i=0; i < n; i++) {
			double diff = measureSupNormInterval(lower[i], upper[i], abs);
			if (diff > value)
				value = diff;
		}
		return value;
	}

	/**
	 * Measure supremum norm for two values, for interval iteration.
	 */
	public static double measureSupNormInterval(double lower, double upper, boolean abs)
	{
		// Deal with infinite cases
		if (Double.isInfinite(lower)) {
			if (Double.isInfinite(upper) && (lower > 0) == (upper > 0)) {
				return 0;
			} else {
				return Double.POSITIVE_INFINITY;
			}
		} else if (Double.isInfinite(upper)) {
			return Double.POSITIVE_INFINITY;
		}

		if (lower == upper)
			return 0.0;

		// Compute/check error
		lower = Math.abs(lower);
		upper = Math.abs(upper);
		double result = upper - lower;
		result = Math.abs(result);
		if (!abs) {
			result = result / lower;
		}
		return result;
	}

	/**
	 * See if two doubles are (nearly) equal.
	 */
	public static boolean doublesAreEqual(double d1, double d2)
	{
		return doublesAreCloseRel(d1, d2, epsilonDouble);
	}

	/**
	 * Return the maximum finite value in a double array, looking at
	 * those entries with indices given bit the integer iterator.
	 */
	public static double findMaxFinite(double[] soln, PrimitiveIterator.OfInt indices)
	{
		double max_v = Double.NEGATIVE_INFINITY;
		while (indices.hasNext()) {
			int i = indices.nextInt();

			double v = soln[i];
			if (v < Double.POSITIVE_INFINITY) {
				max_v = Double.max(v, max_v);
			}
		}
		return max_v;
	}

	/**
	 * Ensure monotonicity from below for interval iteration solution vectors.
	 * Compares the old and new values and overwrites the new value with the old
	 * value if that is larger.
	 * @param old_values old solution vector
	 * @param new_values new solution vector
	 */
	public static void ensureMonotonicityFromBelow(double[] old_values, double[] new_values)
	{
		assert(old_values.length == new_values.length);

		for (int i = 0, n = old_values.length; i < n; i++) {
			double old_value = old_values[i];
			double new_value = new_values[i];
			// from below: do max
			if (old_value > new_value) {
				new_values[i] = old_value;
			}
		}
	}

	/**
	 * Ensure monotonicity from above for interval iteration solution vectors.
	 * Compares the old and new values and overwrites the new value with the old
	 * value if that is smaller.
	 * @param old_values old solution vector
	 * @param new_values new solution vector
	 */
	public static void ensureMonotonicityFromAbove(double[] old_values, double[] new_values)
	{
		assert(old_values.length == new_values.length);

		for (int i = 0, n = old_values.length; i < n; i++) {
			double old_value = old_values[i];
			double new_value = new_values[i];
			// from above: do min
			if (old_value < new_value) {
				new_values[i] = old_value;
			}
		}
	}

	/**
	 * Check for monotonicity: If the new_values are not element-wise less-than-equal the older values
	 * (for from_above == true), then throws an exception. If from_above == false, the logic is reversed,
	 * i.e., if the new_value is not greater-than-equal, an exception is thrown.
	 * @param old_values the old values
	 * @param new_values the new values
	 * @param from_above the direction
	 */
	public static void checkMonotonicity(double[] old_values, double[] new_values, boolean from_above) throws PrismException
	{
		assert(old_values.length == new_values.length);

		for (int i = 0, n = old_values.length; i < n; i++) {
			double old_value = old_values[i];
			double new_value = new_values[i];
			if (from_above && old_value < new_value) {
				throw new PrismException("Monotonicity violated (from above): old value " + old_value + " < new value " + new_value);
			}
			if (!from_above && old_value > new_value) {
				throw new PrismException("Monotonicity violated (from below): old value " + old_value + " > new value " + new_value);
			}
		}
	}

	/**
	 * Select midpoint from two interval iteration solution vectors.
	 * Stores the result in soln_below.
	 * @param soln_below solution vector from below
	 * @param soln_above solution vector from above
	 */
	public static void selectMidpoint(double[] soln_below, double[] soln_above)
	{
		assert(soln_below.length == soln_above.length);

		for (int i = 0, n = soln_below.length; i < n; i++) {
			double below = soln_below[i];
			double above = soln_above[i];

			if (below != above) {
				// use below + ( above - below ) / 2 instead of (below+above)/2 for better numerical
				// stability
				double d = below + (above - below)/2.0;
				if (d >= below && d <= above) {
					// only store result if between below and above
					// guard against rounding problems,
					// fallback is to simply return below as is
					soln_below[i] = d;
				}
			}
		}
	}

	/**
	 * Normalise the entries of a vector in-place such that that they sum to 1.
	 * If {@code sum = 0.0}, all entries are set to {@code NaN} (assuming all entries are non-negative)
	 * @param vector the vector
	 * @return the altered vector (returned for convenience; it's the same one)
	 */
	public static double[] normalise(double[] vector)
	{
		double sum = 0.0;
		int n = vector.length;
		for (int state = 0; state < n; state++) {
			sum += vector[state];
		}
		for (int state = 0; state < n; state++) {
			vector[state] /= sum;
		}
		return vector;
	}

	/**
	 * Normalise the given entries in the vector in-place such that that they sum to 1,
	 * i.e., for all indices {@code s}, set {@code vector[s] = vector[s] / sum},
	 * where {@code sum = sum_{s in entries} (vector[s]).<br>
	 * If {@code sum = 0.0}, all entries are set to {@code NaN} (assuming all entries are non-negative)
	 * @param vector the vector
	 * @param entries Iterable over the entries (must not contain duplicates)
	 * @return the altered vector (returned for convenience; it's the same one)
	 */
	public static double[] normalise(double[] vector, PrimitiveIterable.OfInt entries)
	{
		double sum = 0.0;
		for (PrimitiveIterator.OfInt iter = entries.iterator(); iter.hasNext();) {
			int state = iter.nextInt();
			sum += vector[state];
		}
		for (PrimitiveIterator.OfInt iter = entries.iterator(); iter.hasNext();) {
			int state = iter.nextInt();
			vector[state] /= sum;
		}
		return vector;
	}

	/**
	 * Format a large integer, represented by a double, as a string. Un
	 */
	public static String bigIntToString(double d)
	{
		if (d <= Long.MAX_VALUE) {
			return "" + Math.round(d);
		} else {
			return "" + d;
		}
	}

	/**
	 * Modify a filename f, appending a counter i just before the filetype extension. 
	 */
	public static String addCounterSuffixToFilename(String f, int i)
	{
		return addSuffixToFilename(f, "" + i);
	}

	/**
	 * Modify a filename f, appending a string s just before the filetype extension. 
	 */
	public static String addSuffixToFilename(String f, String s)
	{
		int j = f.lastIndexOf(".");
		if (j != -1) {
			return f.substring(0, j) + s + f.substring(j);
		} else {
			return f + s;
		}
	}

	/**
	 * Format a fraction as a percentage to 1 decimal place.
	 */
	public static String formatPercent1dp(double frac)
	{
		return formatterPercent1dp.format(frac);
	}

	private static DecimalFormat formatterPercent1dp = new DecimalFormat("#0.0%", DecimalFormatSymbols.getInstance(Locale.UK));

	/**
	 * Format a double to 2 decimal places.
	 */
	public static String formatDouble2dp(double d)
	{
		return formatterDouble2dp.format(d);
	}

	private static DecimalFormat formatterDouble2dp = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.UK));

	/**
	 * Format a double, as would be done by printf's %.17g.
	 * Preserving full double precision requires 17 = ceil(log(2^(52+1))) + 1 decimal places,
	 * since the mantissa has 52+1 bits and one additional place is needed to tell close values apart.
	 */
	public static String formatDouble(double d)
	{
		return formatDouble(17, d);
	}

	/**
	 * Format a double, as would be done by printf's %.(prec)g
	 * @param prec precision (significant digits) >= 1
	 */
	public static String formatDouble(int prec, double d)
	{
		if (prec < 1)
			throw new IllegalArgumentException("Precision has to be >= 1; got " + prec);
		// Use no locale to avoid . being changed to , in some countries.
		// To match C's printf, we have to tweak the Java version,
		// strip trailing zeros after the .
		String result = String.format((Locale)null, "%." + prec + "g", d);
		// if there are only zeros after the . (e.g., .000000), strip them including the .
		result = FORMAT_DOUBLE_TRAILING_ZEROS.matcher(result).replaceFirst("$1");
		// handle .xxxx0000
		// we first match .xxx until there are only zeros before the end (or e)
		// as we match reluctantly (using the *?), all trailing zeros are captured
		// by the 0+ part
		return FORMAT_DOUBLE_TRAILING_ZEROS2.matcher(result).replaceFirst("$1$2");
	}

	private static final Pattern FORMAT_DOUBLE_TRAILING_ZEROS = Pattern.compile("\\.0+(e|$)");
	private static final Pattern FORMAT_DOUBLE_TRAILING_ZEROS2 = Pattern.compile("(\\.[0-9]*?)0+(e|$)");

	/**
	 * Format a double (that is known to be an integer, but not necessarily in the int range)
	 * to a string.<br>
	 * Correctly handles values beyond INT_MIN/INT_MAX, as well as -Inf/Inf and NaN.
	 */
	public static String formatIntFromDouble(double d)
	{
		BigRational v = BigRational.from(d);
		return v.toString();
	}

	/**
	 * Create a string for a list of objects, with a specified separator,
	 * e.g. ["a","b","c"], "," -&gt; "a,b,c"
	 */
	public static String joinString(List<?> objs, String separator)
	{
		String s = "";
		boolean first = true;
		for (Object obj : objs) {
			if (first) {
				first = false;
			} else {
				s += separator; 
			}
			s += obj.toString();
		}
		return s;
	}
	
	/**
	 * Create a string for an array of objects, with a specified separator,
	 * e.g. ["a","b","c"], "," -&gt; "a,b,c"
	 */
	public static String joinString(Object[] objs, String separator)
	{
		String s = "";
		boolean first = true;
		for (Object obj : objs) {
			if (first) {
				first = false;
			} else {
				s += separator; 
			}
			s += obj.toString();
		}
		return s;
	}
	
	/**
	 * Check for any cycles in an 2D boolean array representing a graph.
	 * Useful for checking for cyclic dependencies in connected definitions.
	 * Returns the lowest index of a node contained in a cycle, if there is one, -1 if not.  
	 * @param matrix Square matrix of connections: {@code matr[i][j] == true} iff
	 * there is a connection from {@code i} to {@code j}.
	 */
	public static int findCycle(boolean matrix[][])
	{
		int n = matrix.length;
		int firstCycle = -1;
		// Go through nodes
		for (int i = 0; i < n; i++) {
			// See if there is a cycle yet
			for (int j = 0; j < n; j++) {
				if (matrix[j][j]) {
					firstCycle = j;
					break;
				}
			}
			// If so, stop
			if (firstCycle != -1)
				break;
			// Extend dependencies
			for (int j = 0; j < n; j++) {
				for (int k = 0; k < n; k++) {
					if (matrix[j][k]) {
						for (int l = 0; l < n; l++) {
							matrix[j][l] |= matrix[k][l];
						}
					}
				}
			}
		}
		return firstCycle;
	}

	/**
	 * Convert a string representing an amount of memory (e.g. 125k, 50m, 4g) to the value in KB.
	 * If the letter prefix is omitted, we assume it is "k" (i.e. KB).
	 */
	public static long convertMemoryStringtoKB(String mem) throws PrismException
	{
		Pattern p = Pattern.compile("([0-9]+)([kmg]?)");
		Matcher m = p.matcher(mem);
		if (!m.matches()) {
			throw new PrismException("Invalid amount of memory \"" + mem + "\"");
		}
		long num;
		try {
			num = Long.parseLong(m.group(1));
		} catch (NumberFormatException e) {
			throw new PrismException("Invalid amount of memory \"" + mem + "\"");
		}
		switch (m.group(2)) {
		case "":
		case "k":
			return num;
		case "m":
			return num * 1024;
		case "g":
			return num * (1024 * 1024);
		default:
			// Shouldn't happen
			throw new PrismException("Invalid amount of memory \"" + mem + "\"");
		}
	}

	/**
	 * Convert a string representing an amount of time (e.g. 5s, 5m, 5h, 5d, 5w) to the value
	 * in seconds.
	 * If the unit is omitted, we assume it is seconds.
	 */
	public static int convertTimeStringtoSeconds(String time) throws PrismException
	{
		Pattern p = Pattern.compile("([0-9]+)([smhdw]?)");
		Matcher m = p.matcher(time);
		if (!m.matches()) {
			throw new PrismException("Invalid time value \"" + time + "\"");
		}
		int value;
		try {
			value = Integer.parseInt(m.group(1));
		} catch (NumberFormatException e) {
			throw new PrismException("Invalid time value \"" + time + "\"");
		}
		switch (m.group(2)) {
		case "":
		case "s":  // seconds
			return value;
		case "m":  // minutes
			return value * 60;
		case "h":  // hours
			return value * (60 * 60);
		case "d":  // days
			return value * (24 * 60 * 60);
		case "w":  // weeks
			return value * (7 * 24 * 60 * 60);
		default:
			// Shouldn't happen
			throw new PrismException("Invalid time value \"" + time + "\"");
		}
	}

	/**
	 * Convert a number of bytes to a string representing the amount of memory (e.g. 125k, 50m, 4g).
	 */
	public static String convertBytesToMemoryString(long bytes) throws PrismException
	{
		String units[] = { "b", "k", "m", "g" };
		for (int i = 3; i > 0; i--) {
			long pow = 1 << (i * 10);
			if (bytes >= pow) {
				return (bytes % pow == 0 ? (bytes / pow) : String.format(Locale.UK, "%.1f", ((double) bytes) / pow)) + units[i];
			}
		}
		return bytes + units[0];
		
		/*for (String s : new String[] { "1g", "1500m", "2g", "1000m", "1024m", "1" }) {
			System.out.println(s + " => " + PrismUtils.convertMemoryStringtoKB(s) * 1024 + " => " + PrismUtils.convertBytesToMemoryString(PrismUtils.convertMemoryStringtoKB(s) * 1024));
		}*/
	}
	
	/**
	 * Utility method to create a new PrintStream for a file, but any errors are converted to PrismExceptions
	 */
	public static PrintStream newPrintStream(String filename) throws PrismException
	{
		try {
			return new PrintStream(filename);
		} catch (FileNotFoundException e) {
			throw new PrismException("File \"" + filename + "\" could not opened for output");
		}
	}
	
	/**
	 * Compare two version numbers of PRISM (strings).
	 * Example ordering: { "1", "2.0", "2.1.alpha", "2.1.alpha.r5555", "2.1.alpha.r5557", "2.1.beta", "2.1.beta4", "2.1", "2.1.dev", "2.1.dev.r6666", "2.1.dev1", "2.1.dev2", "2.1.2", "2.9", "3", "3.4"};
	 * Returns: 1 if v1&gt;v2, -1 if v1&lt;v2, 0 if v1=v2
	 */
	public static int compareVersions(String v1, String v2)
	{
		String ss1[], ss2[], tmp[];
		int i, n, x;
		double s1 = 0, s2 = 0;
		boolean s1num, s2num;

		// Exactly equal
		if (v1.equals(v2))
			return 0;
		// Otherwise split into sections
		ss1 = v1.split("\\.");
		ss2 = v2.split("\\.");
		// Pad if one is shorter
		n = Math.max(ss1.length, ss2.length);
		if (ss1.length < n) {
			tmp = new String[n];
			for (i = 0; i < ss1.length; i++)
				tmp[i] = ss1[i];
			for (i = ss1.length; i < n; i++)
				tmp[i] = "";
			ss1 = tmp;
		}
		if (ss2.length < n) {
			tmp = new String[n];
			for (i = 0; i < ss2.length; i++)
				tmp[i] = ss2[i];
			for (i = ss2.length; i < n; i++)
				tmp[i] = "";
			ss2 = tmp;
		}
		// Loop through sections of string
		for (i = 0; i < n; i++) {
			// 2.1.alpha < 2.1, etc.
			// 2.1.alpha < 2.1.alpha2 < 2.1.alpha3, etc.
			// so replace alphax with -10000+x
			if (ss1[i].matches("alpha.*")) {
				try {
					if (ss1[i].length() == 5)
						x = 0;
					else
						x = Integer.parseInt(ss1[i].substring(5));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + (-10000 + x);
			}
			if (ss2[i].matches("alpha.*")) {
				try {
					if (ss2[i].length() == 5)
						x = 0;
					else
						x = Integer.parseInt(ss2[i].substring(5));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + (-10000 + x);
			}
			// 2.1.beta < 2.1, etc.
			// 2.1.beta < 2.1.beta2 < 2.1.beta3, etc.
			// so replace betax with -100+x
			if (ss1[i].matches("beta.*")) {
				try {
					if (ss1[i].length() == 4)
						x = 0;
					else
						x = Integer.parseInt(ss1[i].substring(4));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + (-100 + x);
			}
			if (ss2[i].matches("beta.*")) {
				try {
					if (ss2[i].length() == 4)
						x = 0;
					else
						x = Integer.parseInt(ss2[i].substring(4));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + (-100 + x);
			}
			// 2 < 2.1, etc.
			// so treat 2 as 2.0
			if (ss1[i].equals(""))
				ss1[i] = "0";
			if (ss2[i].equals(""))
				ss2[i] = "0";
			// 2.1 < 2.1.dev, etc.
			// 2.1.dev < 2.1.dev2 < 2.1.dev3, etc.
			// so replace devx with 0.5+x/1000
			if (ss1[i].matches("dev.*")) {
				try {
					if (ss1[i].length() == 3)
						x = 0;
					else
						x = Integer.parseInt(ss1[i].substring(3));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + (0.5 + x / 1000.0);
			}
			if (ss2[i].matches("dev.*")) {
				try {
					if (ss2[i].length() == 3)
						x = 0;
					else
						x = Integer.parseInt(ss2[i].substring(3));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + (0.5 + x / 1000.0);
			}
			// replace rx (e.g. as in 4.0.alpha.r5555) with x
			if (ss1[i].matches("r.*")) {
				try {
					x = Integer.parseInt(ss1[i].substring(1));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss1[i] = "" + x;
			}
			if (ss2[i].matches("r.*")) {
				try {
					x = Integer.parseInt(ss2[i].substring(1));
				} catch (NumberFormatException e) {
					x = 0;
				}
				ss2[i] = "" + x;
			}
			// See if strings are integers
			try {
				s1num = true;
				s1 = Double.parseDouble(ss1[i]);
			} catch (NumberFormatException e) {
				s1num = false;
			}
			try {
				s2num = true;
				s2 = Double.parseDouble(ss2[i]);
			} catch (NumberFormatException e) {
				s2num = false;
			}
			if (s1num && s2num) {
				if (s1 < s2)
					return -1;
				if (s1 > s2)
					return 1;
				if (s1 == s2)
					continue;
			}
		}

		return 0;
	}

	/**
	 * Get access to a list's items in reverse order.
	 * Similar to {@code List.reversed()}, which is only in Java 21.
	 */
	public static <E> Iterable<E> listReversed(List<E> list)
	{
		return () -> new Iterator<E>() {
			int i = list.size() - 1;
			@Override
			public boolean hasNext()
			{
				return i >= 0;
			}

			@Override
			public E next()
			{
				return list.get(i--);
			}
		};
	}

	/**
	 * Get access to a list's items in reverse order.
	 * Similar to {@code List.reversed().stream()}, which is only in Java 21.
	 */
	public static <E> Stream<E> listReversedStream(List<E> list)
	{
		return StreamSupport.stream(listReversed(list).spliterator(), false);
	}
}

//------------------------------------------------------------------------------
