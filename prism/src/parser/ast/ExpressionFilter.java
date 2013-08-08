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

import parser.*;
import parser.visitor.*;
import prism.PrismLangException;

public class ExpressionFilter extends Expression
{
	// Enums for  types of filter
	public enum FilterOperator {
		MIN, MAX, ARGMIN, ARGMAX, COUNT, SUM, AVG, FIRST, RANGE, FORALL, EXISTS, PRINT, PRINTALL, STATE;
	};

	// Operator used in filter
	// (and string representation of)
	private FilterOperator opType;
	private String opName;
	// Expression that filter is applied to
	private Expression operand;
	// Expression defining states that filter is over
	// (optional; can be null, denoting "true")
	private Expression filter;
	
	// Filter can be invisible, meaning it is not actually displayed
	// by toString(). This is used to add filters to P/R/S operators that
	// were created with old-style filter syntax. 
	private boolean invisible = false;
	// Whether or not an explanation should be displayed when model checking
	private boolean explanationEnabled = true;
	// whether this is a filter over parameters
	private boolean param = false;

	// Constructors

	public ExpressionFilter(String opName, Expression operand)
	{
		this(opName, operand, null);
	}

	public ExpressionFilter(String opName, Expression operand, Expression filter)
	{
		setOperator(opName);
		setOperand(operand);
		setFilter(filter);
	}

	// Set methods

	public void setOperator(String opName)
	{
		this.opName = opName;
		if (opName.equals("min"))
			opType = FilterOperator.MIN;
		else if (opName.equals("max"))
			opType = FilterOperator.MAX;
		else if (opName.equals("argmin"))
			opType = FilterOperator.ARGMIN;
		else if (opName.equals("argmax"))
			opType = FilterOperator.ARGMAX;
		else if (opName.equals("count"))
			opType = FilterOperator.COUNT;
		else if (opName.equals("sum") || opName.equals("+"))
			opType = FilterOperator.SUM;
		else if (opName.equals("avg"))
			opType = FilterOperator.AVG;
		else if (opName.equals("first"))
			opType = FilterOperator.FIRST;
		else if (opName.equals("range"))
			opType = FilterOperator.RANGE;
		else if (opName.equals("forall") || opName.equals("&"))
			opType = FilterOperator.FORALL;
		else if (opName.equals("exists") || opName.equals("|"))
			opType = FilterOperator.EXISTS;
		else if (opName.equals("print"))
			opType = FilterOperator.PRINT;
		else if (opName.equals("printall"))
			opType = FilterOperator.PRINTALL;
		else if (opName.equals("state"))
			opType = FilterOperator.STATE;
		else opType = null;
	}
	
	public void setOperand(Expression operand)
	{
		this.operand = operand;
	}
	
	public void setFilter(Expression filter)
	{
		this.filter = filter;
	}
	
	public void setInvisible(boolean invisible)
	{
		this.invisible = invisible;
	}
	
	public void setExplanationEnabled(boolean explanationEnabled)
	{
		this.explanationEnabled = explanationEnabled;
	}
	
	public void setParam()
	{
		param = true;
	}

	// Get methods

	public FilterOperator getOperatorType()
	{
		return opType;
	}

	public String getOperatorName()
	{
		return opName;
	}

	public Expression getOperand()
	{
		return operand;
	}

	public Expression getFilter()
	{
		return filter;
	}

	public boolean isInvisible()
	{
		return invisible;
	}

	public boolean getExplanationEnabled()
	{
		return explanationEnabled;
	}
	
	public boolean isParam()
	{
		return param;
	}
	
	// Methods required for Expression:

	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		// Note: In some sense, ExpressionFilters are (often) constant since they return the same
		// value for every state. But the actual value is model dependent, so they are not
		// considered to be constants.
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return false;
	}
	
	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a filter without a model");
	}

	public boolean returnsSingleValue()
	{
		// Most filters return a single value, but there are some exceptions...
		if (opType == FilterOperator.PRINT) return false;
		else if (opType == FilterOperator.PRINTALL) return false;
		else if (opType == FilterOperator.ARGMIN) return false;
		else if (opType == FilterOperator.ARGMAX) return false;
		else if (param) return false;
		else return true;
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
		if (invisible)
			return operand.toString();
		s += (param ? "paramfilter(" : "filter(") + opName + ", " + operand;
		if (filter != null)
			s += ", " + filter;
		s += ")";
		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		ExpressionFilter e;
		e = new ExpressionFilter(opName, operand.deepCopy(), filter == null ? null : filter.deepCopy());
		e.setInvisible(invisible);
		e.setType(type);
		e.setPosition(this);
		e.param = this.param;

		return e;
	}
}

// ------------------------------------------------------------------------------
