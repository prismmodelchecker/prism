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

import java.io.PrintStream;

/**
 * A {@link PrismFileLog} that passes all output to a PrintStream object.
 */
public class PrismPrintStreamLog extends PrismLog
{
	/** PrintStream used for the log writing */
	protected PrintStream logStream = null;

	/**
	 * Create a {@link PrismPrintStreamLog} which will write to the given PrintStream.
	 */
	public PrismPrintStreamLog(PrintStream out)
	{
		setPrintStream(out);
	}

	/**
	 * Set the underlying PrintStream.
	 */
	public void setPrintStream(PrintStream out)
	{
		this.logStream = out;
	}

	/**
	 * Get the underlying PrintStream.
	 */
	public PrintStream getPrintStream()
	{
		return logStream;
	}

	// Methods to implement PrismLog

	@Override
	public boolean ready()
	{
		return logStream != null;
	}

	@Override
	public void flush()
	{
		logStream.flush();
	}

	@Override
	public void close()
	{
		if (logStream != null) {
			flush();
			if (logStream != System.out && logStream != System.err) {
				logStream.close();
			}
			logStream = null;
		}
	}

	@Override
	public void print(boolean b)
	{
		logStream.print(b);
	}

	@Override
	public void print(char c)
	{
		logStream.print(c);
	}

	@Override
	public void print(double d)
	{
		logStream.print(d);
	}

	@Override
	public void print(float f)
	{
		logStream.print(f);
	}

	@Override
	public void print(int i)
	{
		logStream.print(i);
	}

	@Override
	public void print(long l)
	{
		logStream.print(l);
	}

	@Override
	public void print(Object obj)
	{
		logStream.print(obj);
	}

	@Override
	public void print(String s)
	{
		logStream.print(s);
	}

	@Override
	public void println()
	{
		logStream.println();
	}
}
