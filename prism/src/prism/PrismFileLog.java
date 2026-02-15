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

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * A {@link PrismLog} that will write to a file (or stdout).
 */
public class PrismFileLog extends PrismPrintStreamLog
{
	/** Filename (or "stdout") */
	protected String filename;
	/** Are we writing to stdout? */
	protected boolean stdout;
	/** Are we using native code to write to the file? */
	protected boolean nativeCode;

	/**
	 * Create a {@link PrismLog} which will write to {@code filename}, overwriting any previous contents.
	 * If {@code filename} is "stdout", then output will be written to standard output.
	 * Throw a PRISM exception if there is a problem opening the file for writing.
	 * @param filename Filename of log file
	 */
	public PrismFileLog(String filename) throws PrismException
	{
		this(filename, false);
	}

	/**
	 * Create a {@link PrismLog} which will write to {@code filename}, appending to an existing file if requested.
	 * If {@code filename} is "stdout", then output will be written to standard output.
	 * Throw a PRISM exception if there is a problem opening the file for writing.
	 * @param filename Filename of log file
	 * @param append Append to the existing file?
	 */
	public PrismFileLog(String filename, boolean append) throws PrismException
	{
		this(filename, append, false);
	}

	/**
	 * Create a {@link PrismLog} which will write to {@code filename}, appending to an existing file if requested.
	 * If {@code filename} is "stdout", then output will be written to standard output.
	 * Throw a PRISM exception if there is a problem opening the file for writing.
	 * @param filename Filename of log file
	 * @param append Append to the existing file?
	 * @param nativeCode Use native code to write to the file?
	 */
	public PrismFileLog(String filename, boolean append, boolean nativeCode) throws PrismException
	{
		createLogStream(filename, append, nativeCode);
	}

	/**
	 * Set up the PrintStream that will be used to write to the log
	 * @param filename Filename of log file
	 * @param append Append to the existing file?
	 * @param nativeCode Use native code to write to the file?
	 */
	private void createLogStream(String filename, boolean append, boolean nativeCode) throws PrismException
	{
		this.filename = filename;
		this.stdout = "stdout".equals(filename);
		this.nativeCode = nativeCode;
		try {
			if (nativeCode) {
				setPrintStream(new PrismFileLogNative(filename, append));
			} else {
				if (stdout) {
					setPrintStream(System.out);
				} else {
					setPrintStream(new PrintStream(new BufferedOutputStream(new FileOutputStream(filename, append))));
				}
			}
		} catch (FileNotFoundException e) {
			throw new PrismException("Could not open file \"" + filename + "\" for output");
		}
	}

	/**
	 * Ensure the log is using native code to write to the file.
	 * If this is currently not the case, this method will {@code close()}
	 * the current log and open a new equivalent one using native code.
	 * Throw a PRISM exception if there is a problem opening the file for writing.
	 */
	public void useNative() throws PrismException
	{
		if (!nativeCode) {
			close();
			createLogStream(filename, true, true);
		}
	}

	/**
	 * Get the filename (or "stdout" if writing to standard output)
	 **/
	public String getFileName()
	{
		return stdout ? "stdout" : filename;
	}

	/**
	 * Is this log using native code to write to the file?
	 */
	public boolean isNative()
	{
		return nativeCode;
	}

	// Methods for PrismLog
	
	@Override
	public boolean ready()
	{
		if (logStream == null) {
			return false;
		}
		if (logStream instanceof PrismFileLogNative) {
			return ((PrismFileLogNative) logStream).ready();
		} else {
			return !logStream.checkError();
		}
	}

	@Override
	public long getFilePointer()
	{
		if (logStream instanceof PrismFileLogNative) {
			return ((PrismFileLogNative) logStream).getFilePointer();
		} else {
			return -1;
		}
	}

	// Static methods for creating logs

	/**
	 /**
	 * Create a {@link PrismLog} which will write to {@code filename}, overwriting any previous contents.
	 * If {@code filename} is "stdout", then output will be written to standard output.
	 * Throw a PRISM exception if there is a problem opening the file for writing.
	 * @param filename Filename of log file
	 */
	public static PrismFileLog create(String filename) throws PrismException
	{
		return new PrismFileLog(filename);
	}

	/**
	 * Create a {@link PrismLog} which will write to {@code filename}, appending to an existing file if requested.
	 * If {@code filename} is "stdout", then output will be written to standard output.
	 * Throw a PRISM exception if there is a problem opening the file for writing.
	 * @param filename Filename of log file
	 * @param append Append to the existing file?
	 */
	public static PrismFileLog create(String filename, boolean append) throws PrismException
	{
		return new PrismFileLog(filename, append);
	}

	/**
	 * Create a {@link PrismLog} which will write to {@code filename}, appending to an existing file if requested.
	 * If {@code filename} is "stdout", then output will be written to standard output.
	 * Throw a PRISM exception if there is a problem opening the file for writing.
	 * @param filename Filename of log file
	 * @param append Append to the existing file?
	 * @param nativeCode Use native code to write to the file?
	 */
	public static PrismFileLog create(String filename, boolean append, boolean nativeCode) throws PrismException
	{
		return new PrismFileLog(filename, append, nativeCode);
	}

	/**
	 * Create a {@link PrismLog} which will write to standard output.
	 */
	public static PrismFileLog createStdout() throws PrismException
	{
		return create("stdout");
	}
}

//------------------------------------------------------------------------------
