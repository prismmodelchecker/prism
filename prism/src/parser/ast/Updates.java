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

package parser.ast;

import java.util.Vector;
import java.util.ArrayList;

import parser.*;
import parser.visitor.*;
import prism.PrismLangException;

public class Updates extends ASTElement
{
	// Pairs of probabilities/updates
	ArrayList<Expression> probs;
	ArrayList<Update> updates;
	// Parent command
	Command parent;
	
	// Constructor
	
	public Updates()
	{
		probs = new ArrayList<Expression>();
		updates = new ArrayList<Update>();
		parent= null;
	}
	
	// Add a probability/update pair (probability can be null, which equates to 1.0)
	
	public void addUpdate(Expression p, Update u)
	{
		probs.add(p);
		updates.add(u);
		u.setParent(this);
	}

	// Set methods
	
	public void setUpdate(int i, Update u)
	{
		updates.set(i, u);
		u.setParent(this);
	}
	
	public void setProbability(int i, Expression p)
	{
		// Note: probability can be null, which equates to 1.0
		probs.set(i, p);
	}
	
	public void setParent(Command c)
	{
		parent = c;
	}

	// Get methods
	
	public int getNumUpdates()
	{
		return updates.size();
	}
	
	public Update getUpdate(int i)
	{
		return updates.get(i);
	}
	
	/**
	 *  Get the probability of the ith update (may be null, which should be interpreted as constant 1.0)
	 */
	public Expression getProbability(int i)
	{
		return probs.get(i);
	}
	
	public Command getParent()
	{
		return parent;
	}

	/**
	 * Create an array of Values objects, one for each update, using oldValues as a basis.
	 */
	
	public Vector<Values> update(Values constantValues, Values oldValues) throws PrismLangException
	{
		int i, n;
		Vector<Values> res;
		
		n = getNumUpdates();
		res = new Vector<Values>(n);
		for (i = 0; i < n; i++) {
			res.add(getUpdate(i).update(constantValues, oldValues));
		}
		
		return res;
	}

	// Methods required for ASTElement:
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
	
	/**
	 * Convert to string.
	 */
	public String toString()
	{
		String s = "";
		int i, n;
		
		n = getNumUpdates();
		for (i = 0; i < n-1; i++) {
			if (getProbability(i) != null) s += getProbability(i) + " : ";
			s += getUpdate(i) + " + ";
		}
		if (getProbability(n-1) != null) s += getProbability(n-1) + " : ";
		s += getUpdate(n-1);
		
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		int i, n;
		Expression p;
		Updates ret = new Updates();
		n = getNumUpdates();
		for (i = 0; i < n; i++) {
			p = getProbability(i);
			if (p != null) p = p.deepCopy();
			ret.addUpdate(p, (Update)getUpdate(i).deepCopy());
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
