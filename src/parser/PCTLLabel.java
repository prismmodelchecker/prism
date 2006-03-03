//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker, Hinton
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
import apmc.*;
import simulator.*;

public class PCTLLabel extends PCTLFormula
{
	String name;

	// constructor
	
	public PCTLLabel(String s)
	{
		name = s;
	}

	// accessor
	
	public String getName()
	{
		return name;
	}

	// check all labels (make sure the referred labels exist)
	
	public void checkLabelIdents(LabelList labelList) throws PrismException
	{
		// allow special case
		if (name.equals("deadlock")) return;
		// otherwise check list
		if (labelList.getLabelIndex(name) == -1) {
			throw new PrismException("Undeclared label \"" + name + "\"");
		}
	}

	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		return this;
	}

	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		return this;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// set type
		setType(Expression.BOOLEAN);
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
		LabelList ll;
		Expression expr;
		PCTLExpression pctl;
		int i;
		
		// special case
		if (name.equals("deadlock"))
			throw new ApmcException("APMC does not handle \"deadlock\" yet");
		
		// get expression associated with label
		ll = apmc.getLabelList();
		i = ll.getLabelIndex(getName());
		if (i == -1)
			throw new ApmcException("Unknown label \"" + getName() + "\" in PCTL formula");
		expr = ll.getLabel(i);
		
		// create new PCTLExpression and call toApmc
		pctl = new PCTLExpression(expr);
		return pctl.toApmc(apmc);
	}

	/**
	 *	Convert and build simulator data structures
	 */
	public int toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		// I got most of this from the toApmc method above... ;)
		
		LabelList ll;
		Expression expr;
		PCTLExpression pctl;
		int i;
		
		// get expression associated with label
		
		ll = sim.getLabelList();
		
		i = ll.getLabelIndex(getName());
		if (i == -1) throw new SimulatorException("Unknown label \"" + getName() + "\" in PCTL formula");
		expr = ll.getLabel(i);
		
		// create new PCTLExpression and call toSimulator
		pctl = new PCTLExpression(expr);
		return pctl.toSimulator(sim);
	}

	// convert to string
	
	public String toString()
	{
		return "\"" + name + "\"";
	}
}

//------------------------------------------------------------------------------
