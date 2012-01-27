//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

public abstract class PrismLog
{
	/**
	 * Specifies that only more important messages should be printed
	 */
	public static final int VL_DEFAULT = 0;
	/**
	 * Specifies that the output should be more verbose
	 */
	public static final int VL_HIGH = 1;
	/**
	 * Specifies that all messages should be printed
	 */
	public static final int VL_ALL = 2;

	protected int verbosityLevel = VL_DEFAULT;

	/**
	 * Keeps the count of warnings printed so far.
	 */
	protected int numberOfWarnings = 0;

	/**
	 * Sets the counter of warnings printed to 0.
	 */
	public void resetNumberOfWarnings()
	{
		this.numberOfWarnings = 0;
	}

	/**
	 * Returns the number of warnings that have been printed since the beginning
	 * or since the last reset of the number of warnings.
	 */
	public int getNumberOfWarnings()
	{
		return this.numberOfWarnings;
	}

	/**
	 * Returns the verbosity level of this log. The verbosity level determines what messages will be printed.
	 * @return
	 */
	public int getVerbosityLevel()
	{
		return verbosityLevel;
	}

	/**
	 * Changes the verbosity level of this log. The verbosity level determines what messages will be printed.
	 * @param verbosityLevel Should be one of {@link #VL_DEFAULT}, {@link #VL_HIGH} or {@link #VL_ALL}.
	 */
	public void setVerbosityLevel(int verbosityLevel)
	{
		this.verbosityLevel = verbosityLevel;
	}

	public abstract boolean ready();

	public abstract long getFilePointer();

	public abstract void flush();

	public abstract void close();

	public abstract void print(boolean b);

	public abstract void print(char c);

	public abstract void print(double d);

	public abstract void print(float f);

	public abstract void print(int i);

	public abstract void print(long l);

	public abstract void print(Object obj);

	public abstract void print(String s);

	public abstract void println();

	/**
	 * Prints out the value of {@code b} if the log's verbosity level is at least {@code level}
	 */
	public void print(boolean b, int level)
	{
		if (level <= this.verbosityLevel)
			print(b);
	}

	/**
	 * Prints out the value of {@code c} if the log's verbosity level is at least {@code level}
	 */
	public void print(char c, int level)
	{
		if (level <= this.verbosityLevel)
			print(c);
	}

	/**
	 * Prints out the value of {@code d} if the log's verbosity level is at least {@code level}
	 */
	public void print(double d, int level)
	{
		if (level <= this.verbosityLevel)
			print(d);
	}

	/**
	 * Prints out the value of {@code f} if the log's verbosity level is at least {@code level}
	 */
	public void print(float f, int level)
	{
		if (level <= this.verbosityLevel)
			print(f);
	}

	/**
	 * Prints out the value of {@code i} if the log's verbosity level is at least {@code level}
	 */
	public void print(int i, int level)
	{
		if (level <= this.verbosityLevel)
			print(i);
	}

	/**
	 * Prints out the value of {@code l} if the log's verbosity level is at least {@code level}
	 */
	public void print(long l, int level)
	{
		if (level <= this.verbosityLevel)
			print(l);
	}

	/**
	 * Prints out the value of {@code obj} if the log's verbosity level is at least {@code level}
	 */
	public void print(Object obj, int level)
	{
		if (level <= this.verbosityLevel)
			print(obj);
	}

	/**
	 * Prints out {@code s} if the log's verbosity level is at least {@code level}
	 */
	public void print(String s, int level)
	{
		if (level <= this.verbosityLevel)
			print(s);
	}

	/**
	 * Prints out the content  of {@code arr} if the log's verbosity level is at least {@code level}
	 */
	public void print(double[] arr, int level)
	{
		if (level <= this.verbosityLevel)
			print(arr);
	}

	public void print(double arr[])
	{
		int i, n = arr.length;
		print("[");
		for (i = 0; i < n; i++) {
			print(i > 0 ? ", " : "");
			print(arr[i]);
		}
		print("]");
	}

	public void print(int arr[])
	{
		int i, n = arr.length;
		print("[");
		for (i = 0; i < n; i++) {
			print(i > 0 ? ", " : "");
			print(arr[i]);
		}
		print("]");
	}

	public void println(boolean b)
	{
		print(b);
		println();
	}

	public void println(char c)
	{
		print(c);
		println();
	}

	public void println(double d)
	{
		print(d);
		println();
	}

	public void println(float f)
	{
		print(f);
		println();
	}

	public void println(int i)
	{
		print(i);
		println();
	}

	public void println(long l)
	{
		print(l);
		println();
	}

	public void println(Object o)
	{
		print(o);
		println();
	}

	public void println(String s)
	{
		print(s);
		println();
	}

	public void println(double arr[])
	{
		print(arr);
		println();
	}

	public void println(int arr[])
	{
		print(arr);
		println();
	}

	/**
	 * Prints out the value of {@code b} followed by a newline character, provided that the log's verbosity level is at least {@code level}
	 */
	public void println(boolean b, int level)
	{
		if (level <= this.verbosityLevel)
			println(b);
	}

	/**
	 * Prints out the value of {@code c} followed by a newline character, provided that the log's verbosity level is at least {@code level}
	 */
	public void println(char c, int level)
	{
		if (level <= this.verbosityLevel)
			println(c);
	}

	/**
	 * Prints out the value of {@code d} followed by a newline character, provided that the log's verbosity level is at least {@code level}
	 */
	public void println(double d, int level)
	{
		if (level <= this.verbosityLevel)
			println(d);
	}

	/**
	 * Prints out the value of {@code f} followed by a newline character, provided that the log's verbosity level is at least {@code level}
	 */
	public void println(float f, int level)
	{
		if (level <= this.verbosityLevel)
			println(f);
	}

	/**
	 * Prints out the value of {@code i} followed by a newline character, provided that the log's verbosity level is at least {@code level}
	 */
	public void println(int i, int level)
	{
		if (level <= this.verbosityLevel)
			println(i);
	}

	/**
	 * Prints out the value of {@code l} followed by a newline character, provided that the log's verbosity level is at least {@code level}
	 */
	public void println(long l, int level)
	{
		if (level <= this.verbosityLevel)
			println(l);
	}

	/**
	 * Prints out the value of {@code o} followed by a newline character, provided that the log's verbosity level is at least {@code level}
	 */
	public void println(Object o, int level)
	{
		if (level <= this.verbosityLevel)
			println(o);
	}

	/**
	 * Prints out {@code s} followed by a newline character, provided that the log's verbosity level is at least {@code level}
	 */
	public void println(String s, int level)
	{
		if (level <= this.verbosityLevel)
			println(s);
	}

	/**
	 * Prints out the content of {@code arr} followed by a newline character, provided that the log's verbosity level is at least {@code level}
	 */
	public void println(double arr[], int level)
	{
		if (level <= this.verbosityLevel)
			println(arr);
	}

	/**
	 * Prints out the content of {@code arr} followed by a newline character, provided that the log's verbosity level is at least {@code level}
	 */
	public void println(int arr[], int level)
	{
		if (level <= this.verbosityLevel)
			println(arr);
	}

	/**
	 * Prints a separator between sections of log output.
	 */
	public void printSeparator()
	{
		println("\n---------------------------------------------------------------------");
	}
	
	/**
	 * Prints a warning message {@code s}, preceded by "\nWarning: " and followed by a newline character.
	 * <p/>
	 * Also increases {@link #numberOfWarnings} by one. This variable can then be
	 * queried using {@link #getNumberOfWarnings()} at the end of computation
	 * and the user can be appropriately informed that there were warnings generated.
	 * @param s The warning message.
	 */
	public void printWarning(String s)
	{
		println("\nWarning: " + s);
		this.numberOfWarnings++;
	}
}

//------------------------------------------------------------------------------
