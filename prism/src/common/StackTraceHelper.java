//==============================================================================
//	
//	Copyright (c) 2018-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

/** Helper class for converting stack traces to strings for output */
public class StackTraceHelper
{
	/** Default limit for number of lines in a stack trace */
	public static final int DEFAULT_STACK_TRACE_LIMIT = 25;

	/**
	 * Get a string representation of the stack trace for the Throwable
	 * (up to the given limit of lines).
	 * @param throwable the throwable
	 * @param limit the limit
	 */
	public static String asString(Throwable throwable, int limit)
	{
		return asString(throwable.getStackTrace(), limit);
	}

	/**
	 * Get a string representation of the stack trace for the given stack trace array
	 * (up to the given limit of lines).
	 * @param elements array of stack trace elements (zero-th element is top of stack)
	 * @param limit the limit (0 = no limit)
	 */
	public static String asString(StackTraceElement[] elements, int limit)
	{
		StringBuilder sb = new StringBuilder();

		int i = 0;
		for (StackTraceElement element : elements) {
			if (limit != 0 && i++ > limit) {
				sb.append("    ....\n");
				break;
			}
			sb.append("    at ");
			sb.append(element);
			sb.append("\n");
		}

		return sb.toString();
	}
}
