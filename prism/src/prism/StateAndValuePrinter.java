//==============================================================================
//	
//	Copyright (c) 2018-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import parser.VarList;
import parser.type.TypeInt;

/**
 * StateValueConsumer that formats the state/value pairs
 * and prints them to a PrismLog.
 */
class StateAndValuePrinter implements StateAndValueConsumer
{
	/** The log for output */
	private PrismLog outputLog;
	/** The VarList (for the type information on the variables) */
	private VarList varList;

	/** Flag: printSparse (only non-zero values) */
	boolean printSparse = true;
	/** Flag: printMatlab */
	boolean printMatlab = false;
	/** Flag: printStates (variable values on the model) */
	boolean printStates = true;
	/** Flag: printIndizes (indizes for the states) */
	boolean printIndices = true;

	/** Flag: Did we print at least one line? */
	private boolean hadOutput = false;

	/**
	 * Constructor.
	 * @param outputLog The log for output
	 * @param varList The VarList (for the type information)
	 * @param printSparse Print only non-zero values?
	 * @param printMatlab Print in Matlab format?
	 * @param printStates Print the state variable values?
	 * @param printIndices Print the state indices?
	 */
	public StateAndValuePrinter(PrismLog outputLog, VarList varList, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices)
	{
		this.outputLog = outputLog;
		this.varList = varList;

		this.printSparse = printSparse;
		this.printMatlab = printMatlab;
		this.printStates = printStates;
		this.printIndices = printIndices;
	}

	@Override
	public void accept(int[] varValues, double value, long stateIndex)
	{
		if (printSparse && value == 0) {
			// skip zeroes
			return;
		}

		hadOutput = true;

		// Matlab
		if (printMatlab) {
			if (printSparse) {
				outputLog.println("v(" + (stateIndex + 1) + ")=" + value + ";");
			} else {
				outputLog.println(value);
			}
			return;
		}

		// PRISM format
		if (printIndices) {
			outputLog.print(stateIndex);
		}
		if (printStates) {
			if (printIndices) {
				outputLog.print(":");
			}
			outputLog.print("(");
			int n = varList.getNumVars();
			for (int i = 0; i < n; i++) {
				if (i > 0)
					outputLog.print(",");
				outputLog.print(varList.decodeFromInt(i, varValues[i]).toString());
			}
			outputLog.print(")");
		}
		if (printIndices || printStates) {
			outputLog.print("=");
		}
		outputLog.println(value);
	}

	/** Return true if we printed at least one state/value pair. */
	public boolean hadOutput()
	{
		return hadOutput;
	}

}
