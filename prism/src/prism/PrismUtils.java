//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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

import java.io.File;

public class PrismUtils
{
	// load jni stuff from shared library
	static
	{
		try {
			System.loadLibrary("prism");
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}
	
//------------------------------------------------------------------------------

	// small utility methods implemented through jni

	public static native int PU_GetStdout();
	public static native int PU_OpenFile(String filename);
	public static native void PU_PrintToFile(int fp, String s);
	public static native void PU_FlushFile(int fp);
	public static native void PU_CloseFile(int fp);

//------------------------------------------------------------------------------

	// small utility methods in java

	public static double log2(double x)
	{
		return Math.log(x) / Math.log(2);
	}
	
	public static String bigIntToString(double d)
	{
		if (d <= Long.MAX_VALUE) {
			return "" + (long)d;
		}
		else {
			return "" + d;
		}
	}
	
	public static String commaSeparateBigInt(String s)
	{	
		int l = s.length();
		if (l <= 3) {
			return s;
		}
		else {
			return commaSeparateBigInt(s.substring(0,l-3)) + "," + s.substring(l-3);
		}
	}
	
	public static String bigIntToHTML(double d)
	{
		int n;
		String s;

		if (d < 10000000) {
			s = PrismUtils.bigIntToString(d);
			s = PrismUtils.commaSeparateBigInt(s);
		}
		else {
			n = (int)Math.floor((Math.log(d)/Math.log(10)));
			d = (Math.round(100.0*(d/Math.pow(10, n))))/100.0;
			s = d + "x10<sup>" + n + "</sup>";
		}
		
		return s;
	}
	
	public static String addCounterSuffixToFilename(String f, int i)
	{
		int j = f.lastIndexOf(".");
		if (j != -1) {
			return f.substring(0, j)+i+f.substring(j);
		}
		else {
			return f+i;
		}
	}
}

//------------------------------------------------------------------------------
