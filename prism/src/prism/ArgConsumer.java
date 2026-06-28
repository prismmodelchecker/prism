//==============================================================================
//
//	Copyright (c) 2026-
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

package prism;

/**
 * Wraps a CLI argument array and current position for cleaner switch handling.
 * Provides typed argument consumption that throws {@link PrismException}s on error.
 */
class ArgConsumer
{
	private final String[] args;
	private int i;
	private String optionsString;

	/**
	 * Creates a consumer over the full argument array, positioned before the first element.
	 * The caller drives iteration by calling {@link #advance()} then {@link #parseSwitch(String)}.
	 */
	ArgConsumer(String[] args)
	{
		this.args = args;
		this.i = -1;
		this.optionsString = null;
	}

	/**
	 * Advances to and returns the next raw argument.
	 * The caller must check {@link #hasNext()} before calling this.
	 */
	String advance()
	{
		return args[++i];
	}

	/**
	 * Parses a raw CLI token as a switch: strips the leading {@code -} or {@code --},
	 * splits on the first {@code :} to extract any sub-options, stores them in
	 * {@link #optionsString()}, and returns the switch name.
	 * Returns {@code null} if the token does not start with {@code -} (i.e. it is a filename).
	 * Throws if the switch name would be empty (bare {@code -} or {@code --}).
	 */
	String parseSwitch(String arg) throws PrismException
	{
		if (arg.isEmpty() || arg.charAt(0) != '-') {
			optionsString = null;
			return null;
		}
		String sw = arg.substring(1);
		if (sw.isEmpty()) throw new PrismException("Invalid empty switch");
		if (sw.charAt(0) == '-') {
			sw = sw.substring(1);
			if (sw.isEmpty()) throw new PrismException("Invalid empty switch");
		}
		int colon = sw.indexOf(':');
		if (colon == -1) {
			optionsString = null;
			return sw;
		}
		optionsString = sw.substring(colon + 1);
		return sw.substring(0, colon);
	}

	/** Colon sub-options from {@code -switch:opts} syntax (e.g. {@code "upper,lower"} from {@code -intervaliter:upper,lower}), or {@code null}. */
	String optionsString()
	{
		return optionsString;
	}

	/** Returns the current index (updated by any {@code next*()} calls). */
	int index()
	{
		return i;
	}

	/** Whether a next argument is available. */
	boolean hasNext()
	{
		return i < args.length - 1;
	}

	/** Returns the next argument without consuming it, or {@code null} if there is none. */
	String peek()
	{
		return hasNext() ? args[i + 1] : null;
	}

	/** Consume and return the next argument, or throw a descriptive error for switch {@code sw}. */
	String next(String sw) throws PrismException
	{
		if (!hasNext())
			throw new PrismException("No value specified for -" + sw + " switch");
		return args[++i];
	}

	/** Consume the next argument, parse it as an {@code int}, and return it. */
	int nextInt(String sw) throws PrismException
	{
		String s = next(sw);
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new PrismException("Invalid value \"" + s + "\" for -" + sw + " switch");
		}
	}

	/** Consume the next argument, parse it as a {@code double}, and return it. */
	double nextDouble(String sw) throws PrismException
	{
		String s = next(sw);
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			throw new PrismException("Invalid value \"" + s + "\" for -" + sw + " switch");
		}
	}

	/** Consume the next argument, parse it as a {@code long}, and return it. */
	long nextLong(String sw) throws PrismException
	{
		String s = next(sw);
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new PrismException("Invalid value \"" + s + "\" for -" + sw + " switch");
		}
	}
}
