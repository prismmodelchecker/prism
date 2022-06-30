//==============================================================================
//	
//	Copyright (c) 2002-
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
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.Accuracy;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismUtils;

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
	
	// Definitions of filter operators
	
	/**
	 * Apply this filter instance to the list of values provided.
	 * The values should all be the same type of Object, which should
	 * be the expected one for the type of this filter's operand.
	 */
	public Object apply(Iterable<Object> values) throws PrismException
	{
		switch (opType) {
		case MIN:
			return applyMin(values, operand.getType());
		case MAX:
			return applyMax(values, operand.getType());
		case COUNT:
			return applyCount(values, operand.getType());
		case SUM:
			return applySum(values, operand.getType());
		case AVG:
			return applyAvg(values, operand.getType());
		case RANGE:
			return applyRange(values, operand.getType());
		case FORALL:
			return applyForAll(values, operand.getType());
		case EXISTS:
			return applyExists(values, operand.getType());
		default:
			throw new PrismException("No apply operator for filter \"" + opName + "\"");
		}
	}
	
	/**
	 * Apply a min filter to the list of values provided.
	 * The values should all be the same type of Object, which should
	 * be the expected one for the provided type.
	 */
	public static Object applyMin(Iterable<Object> values, Type type) throws PrismException
	{
		if (type instanceof TypeInt) {
			int min = Integer.MAX_VALUE;
			for (Object value : values) {
				min = Math.min(min, (int) value);
			}
			return min;
		} else if (type instanceof TypeDouble) {
			double min = Double.POSITIVE_INFINITY;
			for (Object value : values) {
				min = Math.min(min, (double) value);
			}
			return min;
		} else {
			throw new PrismException("Can't apply min over elements of type " + type);
		}
	}

	/**
	 * Apply a max filter to the list of values provided.
	 * The values should all be the same type of Object, which should
	 * be the expected one for the provided type.
	 */
	public static Object applyMax(Iterable<Object> values, Type type) throws PrismException
	{
		if (type instanceof TypeInt) {
			int max = Integer.MIN_VALUE;
			for (Object value : values) {
				max = Math.max(max, (int) value);
			}
			return max;
		} else if (type instanceof TypeDouble) {
			double max = Double.NEGATIVE_INFINITY;
			for (Object value : values) {
				max = Math.max(max, (double) value);
			}
			return max;
		} else {
			throw new PrismException("Can't apply max over elements of type " + type);
		}
	}

	/**
	 * Apply a count filter to the list of values provided.
	 * The values should all be the same type of Object, which should
	 * be the expected one for the provided type.
	 */
	public static Object applyCount(Iterable<Object> values, Type type) throws PrismException
	{
		if (type instanceof TypeBool) {
			int count = 0;
			for (Object value : values) {
				if ((boolean) value) {
					count++;
				}
			}
			return count;
		} else {
			throw new PrismException("Can't apply count over elements of type " + type);
		}
	}
	
	/**
	 * Apply a sum filter to the list of values provided.
	 * The values should all be the same type of Object, which should
	 * be the expected one for the provided type.
	 */
	public static Object applySum(Iterable<Object> values, Type type) throws PrismException
	{
		if (type instanceof TypeInt) {
			int sum = 0;
			for (Object value : values) {
				sum += (int) value;
			}
			return sum;
		} else if (type instanceof TypeDouble) {
			double sum = 0.0;
			for (Object value : values) {
				sum += (double) value;
			}
			return sum;
		} else {
			throw new PrismException("Can't apply sum over elements of type " + type);
		}
	}
	
	/**
	 * Apply an average filter to the list of values provided.
	 * The values should all be the same type of Object, which should
	 * be the expected one for the provided type.
	 */
	public static Object applyAvg(Iterable<Object> values, Type type) throws PrismException
	{
		if (type instanceof TypeInt) {
			int count = 0;
			double sum = 0.0;
			for (Object value : values) {
				count++;
				sum += (int) value;
			}
			return sum / count;
		} else if (type instanceof TypeDouble) {
			int count = 0;
			double sum = 0.0;
			for (Object value : values) {
				count++;
				sum += (double) value;
			}
			return sum / count;
		} else {
			throw new PrismException("Can't apply avg over elements of type " + type);
		}
	}
	
	/**
	 * Apply a range filter to the list of values provided.
	 * The values should all be the same type of Object, which should
	 * be the expected one for the provided type.
	 */
	public static Object applyRange(Iterable<Object> values, Type type) throws PrismException
	{
		if (type instanceof TypeInt) {
			int min = Integer.MAX_VALUE;
			int max = Integer.MIN_VALUE;
			for (Object value : values) {
				min = Math.min(min, (int) value);
				max = Math.max(max, (int) value);
			}
			return new prism.Interval(min, max);
		} else if (type instanceof TypeDouble) {
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			for (Object value : values) {
				min = Math.min(min, (double) value);
				max = Math.max(max, (double) value);
			}
			return new prism.Interval(min, max);
		} else {
			throw new PrismException("Can't apply min over elements of type " + type);
		}
	}

	/**
	 * Apply a for all filter to the list of values provided.
	 * The values should all be the same type of Object, which should
	 * be the expected one for the provided type.
	 */
	public static Object applyForAll(Iterable<Object> values, Type type) throws PrismException
	{
		if (type instanceof TypeBool) {
			for (Object value : values) {
				if (!((boolean) value)) {
					return false;
				}
			}
			return true;
		} else {
			throw new PrismException("Can't apply for all over elements of type " + type);
		}
	}
	
	/**
	 * Apply an exists filter to the list of values provided.
	 * The values should all be the same type of Object, which should
	 * be the expected one for the provided type.
	 */
	public static Object applyExists(Iterable<Object> values, Type type) throws PrismException
	{
		if (type instanceof TypeBool) {
			for (Object value : values) {
				if ((boolean) value) {
					return true;
				}
			}
			return false;
		} else {
			throw new PrismException("Can't apply there exists over elements of type " + type);
		}
	}
	
	/**
	 * Convenience method: check two values, {@code value} and {@code match}
	 * for (approximate) equality. If the value is a double, stored imprecisely
	 * (i.e., floating point), and the passed in accuracy is non-null, then this
	 * returns true iff {@code match} falls within the range of possible values
	 * for {@code value}, given it's accuracy. Otherwise, exactly equality is checked.
	 * The type of Object for the values should be the expected one for the provided type.
	 * @param value The value to check
	 * @param match The value to check against {@code value}
	 * @param type The type corresponding to both {@code value} and {@code match}
	 * @param accuracy Optionally, the accuracy of {@code value}
	 */
	public static boolean isClose(Object value, Object match, Type type, Accuracy accuracy) throws PrismException
	{
		if (value instanceof Double && accuracy != null) {
			double valueD = (double) value;
			double matchD = (double) match;
			return PrismUtils.measureSupNormAbs(valueD, matchD) <= accuracy.getAbsoluteErrorBound(valueD);
		} else {
			return value.equals(match);
		}
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
	public ExpressionFilter deepCopy(DeepCopy copier) throws PrismLangException
	{
		operand = copier.copy(operand);
		filter = copier.copy(filter);

		return this;
	}

	@Override
	public ExpressionFilter clone()
	{
		return (ExpressionFilter) super.clone();
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
