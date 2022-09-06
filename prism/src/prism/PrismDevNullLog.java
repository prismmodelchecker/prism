//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

public class PrismDevNullLog extends PrismLog
{
	public PrismDevNullLog()
	{
	}

	public boolean ready()
	{
		return true;
	}

	public long getFilePointer()
	{
		return 0;
	}

	public void flush()
	{
	}

	public void close()
	{
	}
	
	// Basic print methods
	
	public void print(boolean b)
	{
	}

	public void print(char c)
	{
	}

	public void print(double d)
	{
	}

	public void print(float f)
	{
	}

	public void print(int i)
	{
	}

	public void print(long l)
	{
	}

	public void print(Object obj)
	{
	}

	public void print(String s)
	{
	}

	public void println()
	{
	}
}

//------------------------------------------------------------------------------
