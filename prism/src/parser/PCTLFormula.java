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

package parser;

import java.util.Vector;

import prism.PrismException;
import simulator.*;

public abstract class PCTLFormula
{
	protected int type = 0; // type of formula - default to unknown (0)
	
	// set type
	
	public void setType(int t) { type = t; }
	
	// get type - if unknown, try and compute first
	
	public int getType() throws PrismException
	{
		if (type==0) {
			typeCheck();
		}
		
		return type;
	}
	
	// get "name" of the result of this formula (used for y-axis of any graphs plotted)
	
	public String getResultName()
	{
		// default is just "Result", will be overridden where necessary
		return "Result";
	}
	
	// check all labels (make sure the referred labels exist)
	
	public void checkLabelIdents(LabelList labelList) throws PrismException
	{
		// default is ok - do nothing
	}
	
	// find all formulas (i.e. locate idents which are formulas)
	
	public abstract PCTLFormula findAllFormulas(FormulaList formulaList) throws PrismException;
	
	// expand any formulas
	
	public abstract PCTLFormula expandFormulas(FormulaList formulaList) throws PrismException;
	
	// find all constants (i.e. locate idents which are constants)
	
	public abstract PCTLFormula findAllConstants(ConstantList constantList) throws PrismException;
	
	// find all variables (i.e. locate idents which are variables)
	
	public abstract PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException;

	// perform type checking
	// (and in doing so compute type)
	
	public abstract void typeCheck() throws PrismException;

	// check everything is ok
	
	public void check() throws PrismException
	{
		// default is ok - do nothing
	}
	
	// check if formula is valid pctl
	
	public void checkValidPCTL() throws PrismException
	{
		// default is ok - do nothing
	}
	
	// check if formula is valid csl
	
	public void checkValidCSL() throws PrismException
	{
		// default is ok - do nothing
	}

	// compute the max depth of prob operator nestings
	
	public int computeMaxNested()
	{
		// default is zero
		return 0;
	}

	/**
	 *	Convert and build simulator data structures
	 */
	public abstract long toSimulator(SimulatorEngine sim ) throws SimulatorException;

	// get expression as string
	// [does nothing to the formula itself]
	public abstract String toString();

	// get expression tree as string
	// [does nothing to the formula itself]
	public String toTreeString(int indent)
	{
		String s = "";
		int i;
		
		for (i = 0; i < indent; i++) s += " ";
		s += getClass() + " : " + toString() + "\n";
		
		return s;
	}
}

//------------------------------------------------------------------------------
