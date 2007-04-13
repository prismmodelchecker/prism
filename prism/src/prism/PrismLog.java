//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

public interface PrismLog
{
	boolean ready();
	void print(boolean b);
	void print(char c);
	void print(double d);
	void print(float f);
	void print(int i);
	void print(long l);
	void print(Object obj);
	void print(String s);
	void println();
	void println(boolean b);
	void println(char c);
	void println(double d);
	void println(float f);
	void println(int i);
	void println(long l);
	void println(Object obj);
	void println(String s);
	long getFilePointer();
	void flush();
	void close();
}

//------------------------------------------------------------------------------
