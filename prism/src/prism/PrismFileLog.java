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

/**
 * PrismLog object that writes all output to a file. 
 */
public class PrismFileLog extends PrismLog
{
	/** Filename (or "stdout") */
	protected String filename;
	/** Native file pointer, cast to a long */
	protected long fp;
	/** Are we writing to stdout? */
	protected boolean stdout;
	
	/**
	 * Pointless constructor. Don't call this.
	 */
	public PrismFileLog()
	{
		filename = "";
		fp = 0;
	}

	/**
	 * Create a PRISM log which will write to {@code filename}, overwriting any previous contents.
	 * @param filename Filename of log file
	 */
	public PrismFileLog(String filename)
	{
		open(filename);
	}

	/**
	 * Create a PRISM log which will write to {@code filename}, appending to an existing file if requested.
	 * @param filename Filename of log file
	 * @param append Append to the existing file?
	 */
	public PrismFileLog(String filename, boolean append)
	{
		open(filename, append);
	}

	/**
	 * Create a PRISM log which will write to {@code filename}, overwriting any previous contents.
	 * Throw a PRISM exception if there is a problem opening the file for writing.
	 * @param filename Filename of log file
	 */
	public static PrismFileLog create(String filename) throws PrismException
	{
		return create(filename, false);
	}
	
	/**
	 * Create a PRISM log which will write to {@code filename}, appending to an existing file if requested.
	 * Throw a PRISM exception if there is a problem opening the file for writing.
	 * @param filename Filename of log file
	 * @param append Append to the existing file?
	 */
	public static PrismFileLog create(String filename, boolean append) throws PrismException
	{
		PrismFileLog log = new PrismFileLog(filename, append);
		if (!log.ready()) {
			throw new PrismException("Could not open file \"" + filename + "\" for output");
		}
		return log;
	}
	
	public void open(String filename)
	{
		open (filename, false);
	}
	
	public void open(String filename, boolean append)
	{
		this.filename = filename;
		if (filename.equals("stdout")) {
			fp = PrismNative.PN_GetStdout();
			stdout = true;
		}
		else {
			fp = append ? PrismNative.PN_OpenFileAppend(filename) : PrismNative.PN_OpenFile(filename);
			stdout = false;
		}
	}

	// Methods for PrismLog
	
	@Override
	public boolean ready()
	{
		return (fp != 0);
	}

	@Override
	public long getFilePointer()
	{
		return fp;
	}

	@Override
	public void flush()
	{
		PrismNative.PN_FlushFile(fp);
	}

	@Override
	public void close()
	{
		if (!stdout) PrismNative.PN_CloseFile(fp);
	}
	
	@Override
	public void print(boolean b)
	{
		printToLog("" + b);
	}

	@Override
	public void print(char c)
	{
		printToLog("" + c);
	}

	@Override
	public void print(double d)
	{
		printToLog("" + d);
	}

	@Override
	public void print(float f)
	{
		printToLog("" + f);
	}

	@Override
	public void print(int i)
	{
		printToLog("" + i);
	}

	@Override
	public void print(long l)
	{
		printToLog("" + l);
	}

	@Override
	public void print(Object obj)
	{
		printToLog("" + obj);
	}

	@Override
	public void print(String s)
	{
		printToLog(s);
	}

	@Override
	public void println()
	{
		printToLog("\n");
	}

	/**
	 * Do the actual write (via native code).
	 */
	private void printToLog(String s)
	{
		PrismNative.PN_PrintToFile(fp, s);
	}
}

//------------------------------------------------------------------------------
