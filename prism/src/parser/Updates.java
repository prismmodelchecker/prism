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

package parser;

import java.util.Vector;

import prism.PrismException;

public class Updates
{
	// pairs of probabilities/updates
	Vector probs;
	Vector updates;
	// parent command
	Command parent;
	
	public Updates()
	{
		probs = new Vector();
		updates = new Vector();
		parent= null;
	}
	
	// add a probability/update pair
	
	public void addUpdate(Expression p, Update u)
	{
		probs.addElement(p);
		updates.addElement(u);
		u.setParent(this);
	}

	// set methods
	
	public void setUpdate(int i, Update u)
	{
		updates.setElementAt(u, i);
		u.setParent(this);
	}
	
	public void setProbability(int i, Expression p)
	{
		probs.setElementAt(p, i);
	}
	
	public void setParent(Command c)
	{
		parent = c;
	}

	// get methods
	
	public int getNumUpdates()
	{
		return updates.size();
	}
	
	public Update getUpdate(int i)
	{
		return (Update)updates.elementAt(i);
	}
	
	public Expression getProbability(int i)
	{
		return (Expression)probs.elementAt(i);
	}
	
	public Command getParent()
	{
		return parent;
	}

	// find all formulas (i.e. locate idents which are formulas)
	
	public void findAllFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		
		n = getNumUpdates();
		for (i = 0; i < n; i++)  {
			setProbability(i, getProbability(i).findAllFormulas(formulaList));
			getUpdate(i).findAllFormulas(formulaList);
		}
	}
	
	// expand any formulas
		
	public void expandFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		
		n = getNumUpdates();
		for (i = 0; i < n; i++)  {
			setProbability(i, getProbability(i).expandFormulas(formulaList));
			getUpdate(i).expandFormulas(formulaList);
		}
	}
	
	// create and return a new Updates by renaming

	public Updates rename(RenamedModule rm) throws PrismException
	{
		int i, n;
		String s;
		Updates u;
		Object o;
		
		// create a new Updates object
		u = new Updates();
		// go thru all updates, rename them and
		// add them to the new Updates
		n = getNumUpdates();
		for (i = 0; i < n; i++)  {
			u.addUpdate(getProbability(i).rename(rm), getUpdate(i).rename(rm));
		}
		
		return u;
	}

	// find all constants (i.e. locate idents which are constants)
	
	public void findAllConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		
		n = getNumUpdates();
		for (i = 0; i < n; i++)  {
			setProbability(i, getProbability(i).findAllConstants(constantList));
			getUpdate(i).findAllConstants(constantList);
		}
	}
	
	// find all variables (i.e. locate idents which are variables)
	
	public void findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		int i, n;
		
		n = getNumUpdates();
		for (i = 0; i < n; i++)  {
			setProbability(i, getProbability(i).findAllVars(varIdents, varTypes));
			getUpdate(i).findAllVars(varIdents, varTypes);
		}
	}
		
	// check everything is ok
	
	public void check() throws PrismException
	{
		int i, n;
		Expression e;
		
		// go thru updates
		n = getNumUpdates();
		for (i = 0; i < n; i++) {
			e = getProbability(i);
			// check probability/rate expression is ok
			e.check();
			// check probability/rate type is int/double
			if (e.getType() == Expression.BOOLEAN) {
				throw new PrismException("Probability/rate \"" + e + "\" cannot be of type boolean");
			}
			// check update
			getUpdate(i).check();
		}
	}

	// create and return an array of Values objects, one for each update
	// use oldValues as a basis
	
	public Vector update(Values constantValues, Values oldValues) throws PrismException
	{
		int i, n;
		Vector res;
		
		n = getNumUpdates();
		res = new Vector(n);
		for (i = 0; i < n; i++) {
			res.add(getUpdate(i).update(constantValues, oldValues));
		}
		
		return res;
	}

	// convert to string
	
	public String toString()
	{
		String s = "";
		int i, n;
		
		n = updates.size();
		for (i = 0; i < n-1; i++) {
			s += probs.elementAt(i) + " : " + updates.elementAt(i) + " + ";
		}
		s += probs.elementAt(n-1) + " : ";
		s += updates.elementAt(n-1);
		
		return s;
	}
}

//------------------------------------------------------------------------------
