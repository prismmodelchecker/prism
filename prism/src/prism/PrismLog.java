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

public abstract class PrismLog
{
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
}

//------------------------------------------------------------------------------
