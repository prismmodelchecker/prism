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

import java.io.*;

/**
 * Implementation of the PrismLog interface that passed all output to a PrintStream object.
 */
public class PrismPrintStreamLog extends PrismLog
{
	PrintStream out = null;
	
	public PrismPrintStreamLog(PrintStream out)
	{
		setPrintStream(out);
	}

	public PrintStream getPrintStream()
	{
		return out;
	}

	public void setPrintStream(PrintStream out)
	{
		this.out = out;
	}
	
	public boolean ready()
	{
		return out != null;
	}

	public long getFilePointer()
	{
		// This implementation is Java only so does not return a file pointer.
		return -1;
	}
	
	public void flush()
	{
		out.flush();
	}

	public void close()
	{
		out.close();
	}
	
	// Basic print methods
	
	public void print(boolean b)
	{
		out.print(b);
	}

	public void print(char c)
	{
		out.print(c);
	}

	public void print(double d)
	{
		out.print(d);
	}

	public void print(float f)
	{
		out.print(f);
	}

	public void print(int i)
	{
		out.print(i);
	}

	public void print(long l)
	{
		out.print(l);
	}

	public void print(Object obj)
	{
		out.print(obj);
	}

	public void print(String s)
	{
		out.print(s);
	}

	public void println()
	{
		out.println();
	}
}

//------------------------------------------------------------------------------
