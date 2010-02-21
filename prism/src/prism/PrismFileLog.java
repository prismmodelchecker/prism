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

public class PrismFileLog extends PrismLog
{
	String filename;
	long fp;
	boolean stdout;
	
	public PrismFileLog()
	{
		filename = "";
		fp = 0;
	}

	public PrismFileLog(String s)
	{
		open(s);
	}

	public PrismFileLog(String s, boolean append)
	{
		open(s, append);
	}

	public void open(String s)
	{
		open (s, false);
	}
	
	public void open(String s, boolean append)
	{
		filename = s;
		if (s.equals("stdout")) {
			fp = PrismNative.PN_GetStdout();
			stdout = true;
		}
		else {
			fp = append ? PrismNative.PN_OpenFileAppend(s) : PrismNative.PN_OpenFile(s);
			stdout = false;
		}
	}

	public boolean ready()
	{
		return (fp != 0);
	}

	public long getFilePointer()
	{
		return fp;
	}

	public void flush()
	{
		PrismNative.PN_FlushFile(fp);
	}

	public void close()
	{
		if (!stdout) PrismNative.PN_CloseFile(fp);
	}
	
	// Basic print methods
	
	public void print(boolean b)
	{
		printToLog("" + b);
	}

	public void print(char c)
	{
		printToLog("" + c);
	}

	public void print(double d)
	{
		printToLog("" + d);
	}

	public void print(float f)
	{
		printToLog("" + f);
	}

	public void print(int i)
	{
		printToLog("" + i);
	}

	public void print(long l)
	{
		printToLog("" + l);
	}

	public void print(Object obj)
	{
		printToLog("" + obj);
	}

	public void print(String s)
	{
		printToLog(s);
	}

	public void println()
	{
		printToLog("\n");
	}

	private void printToLog(String s)
	{
		PrismNative.PN_PrintToFile(fp, s);
	}
}

//------------------------------------------------------------------------------
