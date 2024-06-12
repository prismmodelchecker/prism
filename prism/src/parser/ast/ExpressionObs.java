//==============================================================================
//	
//	Copyright (c) 2020-
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

package parser.ast;

import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.PrismLangException;

/*
 * A reference to an observable.
 */
public class ExpressionObs extends Expression
{
	// Observable name
	private String name;
	// The index of the observable in the model to which it belongs
	private int index;
	
	// Constructors
	
	public ExpressionObs(String name)
	{
		this.name = name;
		index = -1;
	}
	
	// Set methods
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public void setIndex(int i)
	{
		index = i;
	}
	
	// Get methods
	
	public String getName()
	{
		return name;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	// Methods required for Expression:
	
	@Override
	public boolean isConstant()
	{
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return true;
	}
	
	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		// Extract observable value from the evaluation context
		Object res = ec.getObservableValue(name, index);
		if (res == null) {
			throw new PrismLangException("Could not evaluate observable", this);
		}
		// And cast it to the right type/mode if needed
		return getType().castValueTo(res, ec.getEvaluationMode());
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	// Methods required for ASTElement:
	
	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public ExpressionObs deepCopy(DeepCopy copier)
	{
		return this;
	}

	@Override
	public ExpressionObs clone()
	{
		return (ExpressionObs) super.clone();
	}

	// Standard methods
	
	@Override
	public String toString()
	{
		return "\"" + name + "\"";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpressionObs other = (ExpressionObs) obj;
		if (index != other.index)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}

//------------------------------------------------------------------------------
