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

/**
 * Set methods to pass options to native code.
 * And a few utility methods, relying on native methods in the "prism" shared library.
 */ 
public class PrismNative
{
	// Load "prism" shared library
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
	
	// Initialise/close down methods
	
	public static void initialise(Prism prism)
	{
		setPrism(prism);
	}
	
	public static void closeDown()
	{
		// Tidy up any JNI stuff
		PN_FreeGlobalRefs();
	}

	// tidy up in jni (free global references)
	private static native void PN_FreeGlobalRefs();

	// Prism object
	
	// Place to store Prism object for Java code
	private static Prism prism;
	// JNI method to set Prism object for native code
	private static native void PN_SetPrism(Prism prism);
	// Method to set Prism object both in Java and C++
	public static void setPrism(Prism prism)
	{
		PrismNative.prism = prism;
		PN_SetPrism(prism);
	}
	
	// Options passing
	
	private static native void PN_SetCompact(boolean b);
	public static void setCompact(boolean b)
	{
		PN_SetCompact(b);
	}

	private static native void PN_SetLinEqMethod(int i);
	public static void setLinEqMethod(int i)
	{
		PN_SetLinEqMethod(i);
	}
	
	private static native void PN_SetLinEqMethodParam(double d);
	public static void setLinEqMethodParam(double d)
	{
		PN_SetLinEqMethodParam(d);
	}
	
	private static native void PN_SetTermCrit(int i);
	public static void setTermCrit(int i)
	{
		PN_SetTermCrit(i);
	}
	
	private static native void PN_SetTermCritParam(double d);
	public static void setTermCritParam(double d)
	{
		PN_SetTermCritParam(d);
	}
	
	private static native void PN_SetMaxIters(int i);
	public static void setMaxIters(int i)
	{
		PN_SetMaxIters(i);
	}

	private static native void PN_SetSBMaxMem(int i);
	public static void setSBMaxMem(int i)
	{
		PN_SetSBMaxMem(i);
	}
	
	private static native void PN_SetNumSBLevels(int i);
	public static void setNumSBLevels(int i)
	{
		PN_SetNumSBLevels(i);
	}
	
	private static native void PN_SetSORMaxMem(int i);
	public static void setSORMaxMem(int i)
	{
		PN_SetSORMaxMem(i);
	}
	
	private static native void PN_SetNumSORLevels(int i);
	public static void setNumSORLevels(int i)
	{
		PN_SetNumSORLevels(i);
	}
	
	private static native void PN_SetDoSSDetect(boolean b);
	public static void setDoSSDetect(boolean b)
	{
		PN_SetDoSSDetect(b);
	}

	private static native void PN_SetExportAdv(int i);
	public static void setExportAdv(int i)
	{
		PN_SetExportAdv(i);
	}
	
	private static native void PN_SetExportAdvFilename(String filename);
	public static void setExportAdvFilename(String filename)
	{
		PN_SetExportAdvFilename(filename);
	}

	private static native void PN_SetDefaultExportIterationsFilename(String filename);
	public static void setDefaultExportIterationsFilename(String filename)
	{
		PN_SetDefaultExportIterationsFilename(filename);
	}

	private static native int PN_SetWorkingDirectory(String dirname);
	/** Changes the current working directory. Returns 0 on success. */
	public static int setWorkingDirectory(String dirname) {
		return PN_SetWorkingDirectory(dirname);
	}
	
	// Some miscellaneous native methods
	public static native long PN_GetStdout();
	public static native long PN_OpenFile(String filename);
	public static native long PN_OpenFileAppend(String filename);
	public static native void PN_PrintToFile(long fp, String s);
	public static native void PN_FlushFile(long fp);
	public static native void PN_CloseFile(long fp);
}

//------------------------------------------------------------------------------
