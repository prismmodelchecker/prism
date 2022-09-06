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

import common.StackTraceHelper;
import prism.*;

/**
 * Class providing a few convenience methods for the various computation/*Thread classes.
 */
public class GUIComputationThread extends Thread
{
	protected GUIPlugin plug;
	protected Prism prism;

	/** Limit on number of lines for logging a stack trace */
	protected int STACK_TRACE_LIMIT = StackTraceHelper.DEFAULT_STACK_TRACE_LIMIT;

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

	/**
	 * Report an error for an Exception/Error/Throwable (in log and popup dialog).
	 * <br>
	 * In case of a serious error that is unlikely to be recovered from
	 * (i.e., an Error that is not a StackOverflowError), rethrow the error.
	 */
	public void error(Throwable e) {
		if (e instanceof jdd.JDD.CuddOutOfMemoryException) {
			error(e.getMessage()+".\nTry increasing the value of \"CUDD max. memory\" in the options and then restart PRISM");
		} else if (e instanceof PrismException) {
			error(e.getMessage());
		} else if (e instanceof StackOverflowError) {
			String hint = "\nTry increasing the value of the Java stack size (via the -javastack argument)";

			// use message with stack trace for log
			errorLog(e.toString() + "\n" + StackTraceHelper.asString(e, STACK_TRACE_LIMIT) + hint);

			// use short message, without stack trace, for dialog
			errorDialog(e.toString() + hint);
		} else {
			error(e.toString()+"\nThis is an unexpected error, it might be a good idea to restart PRISM");
		}

		// for a serious error, i.e., derived from Error, except stack overflow, rethrow the error
		if (e instanceof Error && !(e instanceof StackOverflowError)) {
			throw (Error)e;
		}
	}

	/** Report an error (in log) */
	public void errorLog(String s)
	{
		logln("\nError: " + s + ".");
	}

	/** Report an Exception/Throwable error (in log) */
	public void errorLog(Throwable e)
	{
		if (e instanceof PrismException || e instanceof jdd.JDD.CuddOutOfMemoryException) {
			logln("\nError: " + e.getMessage() + ".");
		} else {
			logln("\nError: " + e.toString() +".");
		}
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
