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

import java.io.Closeable;

/**
 * Base class for PRISM logs: PrintStream-like objects which can write messages, warnings, etc.
 *
 * . A log is an output stream to which PRISM can write messages, warnings, etc.
 * <br>
 * The log also has a verbosity level, which determines what messages will be printed. The verbosity level can be changed at any time.
 * <br>
 * The log also keeps track of the number of warnings that have been printed, so that this can be queried at the end of computation and the user informed if there were any warnings.
 */
public abstract class PrismLog implements Closeable, AutoCloseable
{
	// Verbosity settings + log state

	/** Specifies that only more important messages should be printed */
	public static final int VL_DEFAULT = 0;
	/** Specifies that the output should be more verbose */
	public static final int VL_HIGH = 1;
	/** Specifies that all messages should be printed */
	public static final int VL_ALL = 2;

	/** Verbosity level of this log */
	protected int verbosityLevel = VL_DEFAULT;

	/** Number of warnings printed so far */
	protected int numberOfWarnings = 0;

	// Setters

	/**
	 * Changes the verbosity level of this log. The verbosity level determines what messages will be printed.
	 * @param verbosityLevel Should be one of {@link #VL_DEFAULT}, {@link #VL_HIGH} or {@link #VL_ALL}.
	 */
	public void setVerbosityLevel(int verbosityLevel)
	{
		this.verbosityLevel = verbosityLevel;
	}

	/**
	 * Sets the counter of warnings printed to 0.
	 */
	public void resetNumberOfWarnings()
	{
		this.numberOfWarnings = 0;
	}

	// Getters

	/**
	 * Returns the verbosity level of this log. The verbosity level determines what messages will be printed.
	 */
	public int getVerbosityLevel()
	{
		return verbosityLevel;
	}

	/**
	 * Returns the number of warnings that have been printed since the beginning
	 * or since the last reset of the number of warnings.
	 */
	public int getNumberOfWarnings()
	{
		return this.numberOfWarnings;
	}

	// Core log methods to be implemented by subclasses

	/**
	 * Returns true if this log is ready to be written to, and false otherwise.
	 */
	public abstract boolean ready();

	/**
	 * For a log with a native implementation, returns the underlying file pointer,
	 * cast to a long (or 0 if the log is not ready). Returns -1 for non-native logs.
	 */
	public long getFilePointer()
	{
		// Default implementation assumes non-native log
		return -1;
	}

	/**
	 * Flushes this log, ensuring that all buffered output is output.
	 */
	public abstract void flush();

	@Override
	public abstract void close();

	/**
	 * Prints a Boolean value.
	 */
	public abstract void print(boolean b);
	/**
	 * Prints a character.
	 */
	public abstract void print(char c);

	/**
	 * Prints a double-precision floating point number.
	 */
	public abstract void print(double d);

	/**
	 * Prints a single-precision floating point number.
	 */
	public abstract void print(float f);

	/**
	 * Prints an integer.
	 */
	public abstract void print(int i);

	/**
	 * Prints a long integer.
	 */
	public abstract void print(long l);

	/**
	 * Prints an object.
	 */
	public abstract void print(Object obj);

	/**
	 * Prints a string.
	 */
	public abstract void print(String s);

	/**
	 * Prints a newline character.
	 */
	public abstract void println();

	// Additional print methods (other objects)

	/**
	 * Prints a double array in a human-readable format, e.g. "[1.0, 2.0, 3.0]".
	 */
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

	/**
	 * Prints an integer array in a human-readable format, e.g. "[1, 2, 3]".
	 */
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

	// Additional print methods (checking verbosity level)

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

	// Additional print methods (println)

	/**
	 * Prints a Boolean value followed by a newline character.
	 */
	public void println(boolean b)
	{
		print(b);
		println();
	}

	/**
	 * Prints a character followed by a newline character.
	 */
	public void println(char c)
	{
		print(c);
		println();
	}

	/**
	 * Prints a double-precision floating point number followed by a newline character.
	 */
	public void println(double d)
	{
		print(d);
		println();
	}

	/**
	 * Prints a single-precision floating point number followed by a newline character.
	 */
	public void println(float f)
	{
		print(f);
		println();
	}

	/**
	 * Prints an integer followed by a newline character.
	 */
	public void println(int i)
	{
		print(i);
		println();
	}

	/**
	 * Prints a long integer followed by a newline character.
	 */
	public void println(long l)
	{
		print(l);
		println();
	}

	/**
	 * Prints an object followed by a newline character.
	 */
	public void println(Object o)
	{
		print(o);
		println();
	}

	/**
	 * Prints a string followed by a newline character.
	 */
	public void println(String s)
	{
		print(s);
		println();
	}

	/**
	 * Prints a double array followed by a newline character.
	 */
	public void println(double arr[])
	{
		print(arr);
		println();
	}

	/**
	 * Prints an integer array followed by a newline character.
	 */
	public void println(int arr[])
	{
		print(arr);
		println();
	}

	// Additional print methods (println, checking verbosity level)

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

	// Other print methods

	/**
	 * Prints a separator between sections of log output.
	 */
	public void printSeparator()
	{
		println("\n---------------------------------------------------------------------");
	}
	
	/**
	 * Prints a warning message {@code s}, preceded by "\nWarning: " and followed by a newline character.
	 * <br>
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
