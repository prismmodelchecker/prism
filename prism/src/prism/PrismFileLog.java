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

public class PrismFileLog implements PrismLog
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

	public void open(String s)
	{
		filename = s;
		if (s.equals("stdout")) {
			fp = PrismNative.PN_GetStdout();
			stdout = true;
		}
		else {
			fp = PrismNative.PN_OpenFile(s);
			stdout = false;
		}
	}

	public boolean ready()
	{
		return (fp != 0);
	}

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

	public void println(boolean b)
	{
		printToLog(b + "\n");
	}

	public void println(char c)
	{
		printToLog(c + "\n");
	}

	public void println(double d)
	{
		printToLog(d + "\n");
	}

	public void println(float f)
	{
		printToLog(f + "\n");
	}

	public void println(int i)
	{
		printToLog(i + "\n");
	}

	public void println(long l)
	{
		printToLog(l + "\n");
	}

	public void println(Object obj)
	{
		printToLog(obj + "\n");
	}

	public void println(String s)
	{
		printToLog(s + "\n");
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
	
	private void printToLog(String s)
	{
		PrismNative.PN_PrintToFile(fp, s);
	}
}

//------------------------------------------------------------------------------
