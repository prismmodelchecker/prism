//==============================================================================
//	
//	Copyright (c) 2018-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Steffen Märcker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

package common;

import prism.PrismException;

/**
 * This class provides utility methods allow detection of
 * primitive cast errors, e.g. like overflows and special values.
 */
public class SafeCast
{
	/**
	 * Wrapper method for toIntExcapt
	 * converting an ArithmeticException to a PrismExeption.
	 *
	 * @param value {@code double} value
	 * @return the equivalent {@code int} value
	 * @throws ArithmeticException if the value cannot be converted to {@code int}
	 */
	public static int toInt(double value) throws PrismException
	{
		try {
			return toIntExact(value);
		} catch (ArithmeticException e) {
			throw new PrismException(e.getMessage());
		}
	}

	/**
	 * Convert a primitive double to a primitive int value
	 * throwing an exception if the value is a special value or not an {@code int}.
	 *
	 * @param value {@code double} value
	 * @return the equivalent {@code int} value
	 * @throws ArithmeticException if the value cannot be converted to {@code int}
	 */
	public static int toIntExact(double value)
	{
		if (Double.isInfinite(value) || Double.isNaN(value)) {
			throw new ArithmeticException(value + " is a special value");
		}

		if ((int)value != value) {
			throw new ArithmeticException(value + " cannot be converted to int");
		}
		return (int)value;
	}

	/**
	 * Convert a primitive double to a primitive long value
	 * throwing an exception if the value is a special value or not an {@code long}.
	 *
	 * @param value {@code double} value
	 * @return the equivalent {@code long} value
	 * @throws ArithmeticException if the value cannot be converted to {@code long}
	 */
	public static long toLongExact(double value)
	{
		if (Double.isInfinite(value) || Double.isNaN(value)) {
			throw new ArithmeticException(value + " is a special value");
		}

		if ((long)value != value) {
			throw new ArithmeticException(value + " cannot be converted to long");
		}
		return (long)value;
	}
}
