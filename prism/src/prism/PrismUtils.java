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

import java.text.DecimalFormat;
import java.util.Formatter;

/**
 * Various general-purpose utility methods in Java
 */
public class PrismUtils
{
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
	 * Format a large integer, represented by a double, as a string. 
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
		int j = f.lastIndexOf(".");
		if (j != -1) {
			return f.substring(0, j) + i + f.substring(j);
		} else {
			return f + i;
		}
	}

	/**
	 * Format a fraction as a percentage to 1 decimal place.
	 */
	public static String formatPercent1dp(double frac)
	{
		return formatterPercent1dp.format(frac);
	}

	private static DecimalFormat formatterPercent1dp = new DecimalFormat("#0.0%");

	/**
	 * Format a double to 2 decimal places.
	 */
	public static String formatDouble2dp(double d)
	{
		return formatterDouble2dp.format(d);
	}
	
	private static DecimalFormat formatterDouble2dp = new DecimalFormat("#0.00 secs");
	
	/**
	 * Format a double, using PRISM settings.
	 */
	public static String formatDouble(PrismSettings settings, double d)
	{
		return formatDouble(settings, new Double(d));
	}

	public static String formatDouble(PrismSettings settings, Double d)
	{
		Formatter formatter = new Formatter();

		formatter.format("%.6g", d); // [the way to format scientific notation with 6 being the precision]

		String res = formatter.toString().trim();

		int trailingZeroEnd = res.lastIndexOf('e');
		if (trailingZeroEnd == -1)
			trailingZeroEnd = res.length();

		int x = trailingZeroEnd - 1;

		while (x > 0 && res.charAt(x) == '0')
			x--;

		if (res.charAt(x) == '.')
			x++;

		res = res.substring(0, x + 1) + res.substring(trailingZeroEnd, res.length());

		//formatter.format("%.6f",d); //(just decimals)
		//formatter.format("%1$.2e", d); // [the way to format scientific notation with 6 being the precision]

		return res;
	}
}

//------------------------------------------------------------------------------
