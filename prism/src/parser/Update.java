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

package parser;

import java.util.Vector;

import prism.PrismException;
import apmc.*;

public class Update
{
	// list of variable/expression pairs (and types)
	Vector vars;
	Vector exprs;
	Vector types;
	// parent Updates object
	Updates parent;
	
	// constructor
	
	public Update()
	{
		vars = new Vector();
		exprs = new Vector();
		types = new Vector();
	}
	
	// add element (variable/expression pair)
	
	public void addElement(String v, Expression e)
	{
		vars.addElement(v);
		exprs.addElement(e);
	}
	
	// set parent
	
	public void setParent(Updates u)
	{
		parent = u;
	}
	
	// get methods
	
	public int getNumElements()
	{
		return vars.size();
	}
	
	public String getVar(int i)
	{
		return (String)vars.elementAt(i);
	}
	
	public Expression getExpression(int i)
	{
		return (Expression)exprs.elementAt(i);
	}
	
	public int getType(int i)
	{
		return ((Integer)types.elementAt(i)).intValue();
	}
	
	public Updates getParent()
	{
		return parent;
	}
	
	// find all formulas (i.e. locate idents which are formulas)
	
	public void findAllFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		
		n = exprs.size();
		for (i = 0; i < n; i++) {
			exprs.setElementAt(getExpression(i).findAllFormulas(formulaList), i);
		}
	}
	
	// expand any formulas
		
	public void expandFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		
		n = exprs.size();
		for (i = 0; i < n; i++) {
			exprs.setElementAt(getExpression(i).expandFormulas(formulaList), i);
		}
	}
	
	// create and return a new Update by renaming

	public Update rename(RenamedModule rm) throws PrismException
	{
		int i, n;
		String s;
		Update u;
		
		// create new Update
		u = new Update();
		// copy all, changing variables
		n = getNumElements();
		for (i = 0; i < n; i++) {
			s = rm.getNewName(getVar(i));
			s = (s == null) ? getVar(i) : s;
			u.addElement(s, getExpression(i).rename(rm));
		}
		
		return u;
	}
	
	// find all constants (i.e. locate idents which are constants)
	
	public void findAllConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		
		n = exprs.size();
		for (i = 0; i < n; i++) {
			exprs.setElementAt(getExpression(i).findAllConstants(constantList), i);
		}
	}
	
	// find all variables (i.e. locate idents which are variables)
	
	public void findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		int i, j, n;
		String s;
		
		// go thru all update expressions to find vars
		n = exprs.size();
		for (i = 0; i < n; i++) {
			exprs.setElementAt(getExpression(i).findAllVars(varIdents, varTypes), i);
		}
		
		// while we're at it, check all the update vars are valid too
		// also, store their types for future type checking
		for (i = 0; i < n; i++) {
			j = varIdents.indexOf(getVar(i));
			if (j == -1) {
				s = "Unknown variable \"" + getVar(i) + "\" in update ";
				s += "\"(" + getVar(i) + "'=" + getExpression(i) + ")\"";
				throw new PrismException(s);
			}
			types.addElement(varTypes.elementAt(j));
		}
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		int i, n;
		String s;
		Expression e;
		Command c;
		Module m;
		ModulesFile mf;
		boolean isLocal, isGlobal;
		String var;
		
		c = getParent().getParent();
		m = c.getParent();
		mf = m.getParent();
		
		n = getNumElements();
		for (i = 0; i < n; i++) {
			// check that the update is allowed to modify this variable
			var = getVar(i);
			isLocal = m.isLocalVariable(var);
			isGlobal = isLocal ? false : mf.isGlobalVariable(var);
			if (!isLocal && !isGlobal) {
				s = "Module " + m.getName() + " is not allowed to modify variable " + var;
				s += " which belongs to another module";
				throw new PrismException(s);
			}
			if (isGlobal && !c.getSynch().equals("")) {
				s = "Synchronous command (" + c.getSynch() + ") in module " + m.getName();
				s += " is not allowed to modify global variable " + var;
				throw new PrismException(s);
			}
			// check expression
			e = getExpression(i);
			e.check();
			// check evaluates to correct type
			if (!Expression.canAssignTypes(getType(i), e.getType())) {
				throw new PrismException("Type error in update \"(" + var + "'=" + getExpression(i) + ")\"");
			}
		}
	}

	// create new Values object according to this update
	
	public Values update(Values constantValues, Values oldValues) throws PrismException
	{
		int i, n;
		Values res;
		
		res = new Values();
		n = exprs.size();
		for (i = 0; i < n; i++) {
			res.setValue(getVar(i), getExpression(i).evaluate(constantValues, oldValues));
		}
		
		return res;
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
		int i, n, r;
		
		r = -1;
		n = getNumElements();
		for(i = 0; i < n; i++)
		    r = apmc.createAffectation(getVar(i), getExpression(i).toApmc(apmc), r);
		return r;
	}

	// convert to string
	
	public String toString()
	{
		int i, n;
		String s = "";
		
		n = exprs.size();
		// normal case
		if (n > 0) {
			for (i = 0; i < n-1; i++) {
				s = s + "(" + vars.elementAt(i) + "'=" + exprs.elementAt(i) + ") & ";
			}
			s = s + "(" + vars.elementAt(n-1) + "'=" + exprs.elementAt(n-1) + ")";
		}
		// special (empty) case
		else {
			s = "true";
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
