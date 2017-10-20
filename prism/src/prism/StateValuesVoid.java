//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.io.File;

import parser.ast.RelOp;
import jdd.JDDNode;
import jdd.JDDVars;

/**
 * Dummy class implementing StateValues to return miscellaneous (single valued) results, typically of type "void".
 */
public class StateValuesVoid implements StateValues
{
	/** The stored object */
	private Object value = null;

	/** Constructor, store value */
	public StateValuesVoid(Object value)
	{
		this.value = value;
	}

	@Override
	public void switchModel(Model newModel)
	{
		// nothing to do...
	}

	@Override
	public int getSize()
	{
		return 1;
	}

	@Override
	public Object getValue(int i)
	{
		return value;
	}

	/** Get the value */
	public Object getValue()
	{
		return value;
	}

	/** Set the value */
	public void setValue(Object value)
	{
		this.value = value;
	}

	@Override
	public StateValuesDV convertToStateValuesDV()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public StateValuesMTBDD convertToStateValuesMTBDD()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void readFromFile(File file) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void roundOff(int places)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void subtractFromOne()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(StateValues sp)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void timesConstant(double d)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double dotProduct(StateValues sp)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void filter(JDDNode filter)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void filter(JDDNode filter, double d)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void maxMTBDD(JDDNode vec2)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear()
	{
		// Do nothing
	}

	@Override
	public int getNNZ()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNNZString()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double firstFromBDD(JDDNode filter)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double minOverBDD(JDDNode filter)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double maxOverBDD(JDDNode filter)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double maxFiniteOverBDD(JDDNode filter)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double sumOverBDD(JDDNode filter)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double sumOverMTBDD(JDDNode mult)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public StateValues sumOverDDVars(JDDVars sumVars, Model newModel) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JDDNode getBDDFromInterval(String relOpString, double bound)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JDDNode getBDDFromInterval(RelOp relOp, double bound)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JDDNode getBDDFromInterval(double lo, double hi)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JDDNode getBDDFromCloseValue(double val, double epsilon, boolean abs)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JDDNode getBDDFromCloseValueAbs(double val, double epsilon)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public JDDNode getBDDFromCloseValueRel(double val, double epsilon)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void print(PrismLog log) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void print(PrismLog log, boolean printSparse, boolean printMatlab, boolean printStates, boolean printIndices) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void printFiltered(PrismLog log, JDDNode filter) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void printFiltered(PrismLog log, JDDNode filter, boolean printSparse, boolean printMatlab, boolean printStates) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public StateValues deepCopy() throws PrismException
	{
		throw new UnsupportedOperationException();
	}
}
