//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Hongyang Qu <hongyang.qu@comlab.ox.ac.uk> (University of Oxford)
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

package parser.ast;

import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

/**
 * Store an action in fairness constraints 
 * @author hongyang
 *
 */
public class ExpressionAction extends Expression {
	protected String action;
	protected int index; // index in the action list
	
	public ExpressionAction(String action) {
		this.action = action;
	}
	
	public String getName() {
		return action;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public Expression deepCopy() {
		return new ExpressionAction(new String(action));
	}

	public Object evaluate(EvaluateContext ec) throws PrismLangException {
		throw new PrismLangException("Cannot evaluate action", this);
	}

	public boolean isConstant() {
		return false;
	}

	public boolean returnsSingleValue() {
		return false;
	}

	public Object accept(ASTVisitor v) throws PrismLangException {
		return v.visit(this);
	}

	public String toString() {
		return "[" + action + "]";
	}

}
