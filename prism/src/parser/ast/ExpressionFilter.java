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

import param.BigRational;
import parser.*;
import parser.type.TypeBool;
import parser.visitor.*;
import prism.PrismLangException;

public class ExpressionFilter extends Expression
{
	/**
	 * Types of filter, for expressions of the form "filter(op, prop, states)",
	 * with filter states "states" being optional (denoting "true").
	 */ 
	public enum FilterOperator {
		/** Minimum value of prop over all filter states */
		MIN ("min"),
		/** Maximum value of prop over all filter states */
		MAX ("max"),
		/** True for the filter states that yield the minimum value of prop */
		ARGMIN ("argmin"),
		/** True for the filter states that yield the maximum value of prop */
		ARGMAX ("argmax"),
		/** Number of filter states for which prop is true */
		COUNT ("count"),
		/** Sum of the value of prop for all filter states */
		SUM ("sum"),
		/** Average of the value of prop over all filter states */
		AVG ("avg"),
		/** Value of prop for the first (lowest-indexed) filter state */
		FIRST ("first"),
		/** Range (interval) of values of prop over all filter states */
		RANGE ("range"),
		/** True iff prop is true for all filter states */
		FORALL ("forall"),
		/** True iff prop is true for some filter states */
		EXISTS ("exists"),
		/** Print the (non-zero) values to the log */
		PRINT ("print"),
		/** Print all (including zero) values to the log */
		PRINTALL ("printall"),
		/** Value for the single filter state (if there is more than one, this is an error) */
		STATE ("state"),
		/** Store the results vector (used internally) */
		STORE ("store");
		public final String keyword;
		FilterOperator(final String keyword) {
			this.keyword = keyword;
		}
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
		for (FilterOperator op : FilterOperator.values()) {
			if (op.keyword.equals(opName)) {
				opType = op;
				return;
			}
		}
		// handle shorthands
		if ("+".equals(opName)) {
			opType = FilterOperator.SUM;
		} else if ("&".equals(opName)) {
			opType = FilterOperator.FORALL;
		} else if ("|".equals(opName)) {
			opType = FilterOperator.EXISTS;
		} else {
			opType = null;
		}
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

	@Override
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
	
	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a filter without a model");
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a filter without a model");
	}

	@Override
	public boolean returnsSingleValue()
	{
		// Most filters return a single value, but there are some exceptions...
		if (opType == FilterOperator.PRINT) return false;
		else if (opType == FilterOperator.PRINTALL) return false;
		else if (opType == FilterOperator.ARGMIN) return false;
		else if (opType == FilterOperator.ARGMAX) return false;
		else if (opType == FilterOperator.STORE) return false;
		else if (param) return false;
		else return true;
	}
	
	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
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
	
	// Standard methods
	
	@Override
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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (explanationEnabled ? 1231 : 1237);
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + (invisible ? 1231 : 1237);
		result = prime * result + ((opName == null) ? 0 : opName.hashCode());
		result = prime * result + ((opType == null) ? 0 : opType.hashCode());
		result = prime * result + ((operand == null) ? 0 : operand.hashCode());
		result = prime * result + (param ? 1231 : 1237);
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
		ExpressionFilter other = (ExpressionFilter) obj;
		if (explanationEnabled != other.explanationEnabled)
			return false;
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (invisible != other.invisible)
			return false;
		if (opName == null) {
			if (other.opName != null)
				return false;
		} else if (!opName.equals(other.opName))
			return false;
		if (opType != other.opType)
			return false;
		if (operand == null) {
			if (other.operand != null)
				return false;
		} else if (!operand.equals(other.operand))
			return false;
		if (param != other.param)
			return false;
		return true;
	}
	
	/**
	 * Wrap a "default" ExpressionFilter around an Expression representing a property to be model checked,
	 * in order to pick out a single value (the final result of model checking) from a vector of values for all states.
	 * See the PRISM manual (or check the code below) to see the definition of the "default" filter.
	 * If the expression is already an ExpressionFilter (of the right kind), nothing is done.
	 * Note that we need to know whether the model has multiple initial states, because this affects the default filter.  
	 * @param expr Expression to be model checked
	 * @param singleInit Does the model on which it is being checked have a single initial states? 
	 */
	public static ExpressionFilter addDefaultFilterIfNeeded(Expression expr, boolean singleInit) throws PrismLangException
	{
		ExpressionFilter exprFilter = null;
		
		// The final result of model checking will be a single value. If the expression to be checked does not
		// already yield a single value (e.g. because a filter has not been explicitly included), we need to wrap
		// a new (invisible) filter around it. Note that some filters (e.g. print/argmin/argmax) also do not
		// return single values and have to be treated in this way.
		if (!expr.returnsSingleValue()) {
			// New filter depends on expression type and number of initial states.
			// Boolean expressions...
			if (expr.getType() instanceof TypeBool) {
				// Result is true iff true for all initial states
				exprFilter = new ExpressionFilter("forall", expr, new ExpressionLabel("init"));
			}
			// Non-Boolean (double or integer) expressions...
			else {
				// Result is for the initial state, if there is just one,
				// or the range over all initial states, if multiple
				if (singleInit) {
					exprFilter = new ExpressionFilter("state", expr, new ExpressionLabel("init"));
				} else {
					exprFilter = new ExpressionFilter("range", expr, new ExpressionLabel("init"));
				}
			}
		}
		// Even, when the expression does already return a single value, if the the outermost operator
		// of the expression is not a filter, we still need to wrap a new filter around it.
		// e.g. 2*filter(...) or 1-P=?[...{...}]
		// This because the final result of model checking is only stored when we process a filter.
		else if (!(expr instanceof ExpressionFilter)) {
			// We just pick the first value (they are all the same)
			exprFilter = new ExpressionFilter("first", expr, new ExpressionLabel("init"));
			// We stop any additional explanation being displayed to avoid confusion.
			exprFilter.setExplanationEnabled(false);
		}
		// Finalise filter (if created) and return
		if (exprFilter != null) {
			// Make it invisible (not that it will be displayed)
			exprFilter.setInvisible(true);
			// Compute type of new filter expression (will be same as child)
			exprFilter.typeCheck();
			return exprFilter;
		} else {
			// If no new filter was created, we no expr is an ExpressionFilter
			return (ExpressionFilter) expr;
		}
	}
}

// ------------------------------------------------------------------------------
