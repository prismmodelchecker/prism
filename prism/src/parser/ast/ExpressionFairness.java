//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Hongyang Qu <hongyang.qu@cs.ox.ac.uk> (University of Oxford)
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

import java.util.Vector;

import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

/**
 * Encode a fairness constraint, which has two set H and K of states and actions.
 * @author Hongyang Qu
 *
 */
public class ExpressionFairness extends Expression {
	protected boolean isStrong;
	// Strong fairness: G F H -> G F K
	// Weak fairness: F G H -> G F K
	// Each expression can be either Boolean (for states) or Label (for actions) 
	protected Vector<Expression> H;
	protected Vector<Expression> K;
	
	public ExpressionFairness(boolean isStrong, Vector<Expression> H, Vector<Expression> K) {
		this.isStrong = isStrong;
		this.H = H;
		this.K = K;
	}
	
	public boolean isStrongFairness() {
		return isStrong;
	}
	
	public Vector<Expression> getH() {
		return H;
	}
	
	public int getSizeH() {
		if(H == null)
			return 0;
		return H.size();
	}
	
	public Vector<Expression> getK() {
		return K;
	}
	
	public int getSizeK() {
		if(K == null)
			return 0;
		return K.size();
	}
	
	public Expression getExpressionH(int index) {
		if(H != null && H.size()+1>= index)
			return H.get(index);
		return null;
	}
	
	public Expression getExpressionK(int index) {
		if(K != null && K.size()+1>= index)
			return K.get(index);
		return null;
	}
	
	public void setExpressionH(int index, Expression e) {
		if(H != null && H.size()+1>= index) {
			H.remove(index);
			H.add(index, e);
		}
	}
	
	public void setExpressionK(int index, Expression e) {
		if(K != null && K.size()+1>= index) {
			K.remove(index);
			K.add(index, e);
		}
	}

	@Override
	public Expression deepCopy() {
		Vector<Expression> newH = null;
		if(H != null) {
			newH = new Vector<Expression>();
			for(Expression e: H) {
				newH.add(e.deepCopy());
			}
		}
		Vector<Expression> newK = null;
		if(K != null) {
			newK = new Vector<Expression>();
			for(Expression e: K) {
				newK.add(e.deepCopy());
			}
		}
		
		return new ExpressionFairness(isStrong, newH, newK);
	}

	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException {
		throw new PrismLangException("Cannot evaluate Fairness constraint", this);
	}

	@Override
	public boolean isConstant() {
		return false;
	}

	@Override
	public boolean returnsSingleValue() {
		return false;
	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException {
		return v.visit(this);
	}

	@Override
	public String toString() {
		String s = isStrong? "SF" : "WF";
		s += "(";
		s += "{";
		if(H != null) {
			int i=0;
			for(Expression e: H) {
				s += (i==0? "" : ", ") + e.toString();
			}
		}
		s += "}, {";
		if(K != null) {
			int i=0;
			for(Expression e: K) {
				s += (i==0? "" : ", ") + e.toString();
			}
		}
		s += "}";
		s += ")";
		
		return s;
	}
}
