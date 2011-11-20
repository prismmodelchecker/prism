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

package userinterface;

import prism.*;

/**
 * Class providing a few convenience methods for the various computation/*Thread classes.
 */
public class GUIComputationThread extends Thread
{
	protected GUIPlugin plug;
	protected Prism prism;
	
	/** Creates a new instance of GUIComputationThread */
	public GUIComputationThread(GUIPlugin plug)
	{
		this.plug = plug;
		prism = plug.getPrism();
	}
	
	/** Report an error (in log and popup dialog) */
	public void error(String s)
	{
		errorLog(s);
		errorDialog(s);
	}
	
	/** Report an error (in log) */
	public void errorLog(String s)
	{
		logln("\nError: " + s + ".");
	}
	
	// send messages to log
	public void log(Object o) { plug.log(o); }
	public void log(int i) { plug.log(i); }
	public void log(double d) { plug.log(d); }
	public void log(float f) { plug.log(f); }
	public void log(short s) { plug.log(s); }
	public void log(byte b) { plug.log(b); }
	public void log(boolean b) { plug.log(b); }
	public void logln() { plug.logln(); }
	public void logln(Object o) { plug.logln(o); }
	public void logln(int i) { plug.logln(i); }
	public void logln(double d) { plug.logln(d); }
	public void logln(float f) { plug.logln(f); }
	public void logln(short s) { plug.logln(s); }
	public void logln(byte b) { plug.logln(b); }
	public void logln(boolean b) { plug.logln(b); }
	public void logSeparator() { plug.logSeparator(); }
	public void logWarning(String s) { plug.logWarning(s); }
	
	/** pop up an error dialog */
	public void errorDialog(String s)
	{
		plug.error(s);
	}
}
