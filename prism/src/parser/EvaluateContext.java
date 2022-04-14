//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

/**
 * Classes providing the information required to evaluate an expression.
 * Primarily, this means providing values for variables.
 * Optionally, values for constants can also be supplied.
 */
public abstract class EvaluateContext
{
	// Core information stored in an EvaluateContext
	
	/**
	 * Evaluation mode (floating point? exact?)
	 */
	public enum EvalMode { FP, EXACT }
	protected EvalMode evalMode = EvalMode.FP;
	
	/**
	 * Constant values (optional)
	 */
	protected Values constantValues = null;
	
	// Setters and getters for core information
	
	/**
	 * Set the evaluation mode (floating point? exact?)
	 * Returns a copy of this EvaluateContext to allow chaining of method calls.
	 */
	public EvaluateContext setEvaluationMode(EvalMode evalMode)
	{
		this.evalMode = evalMode;
		return this;
	}

	/**
	 * Get the evaluation mode (floating point? exact?)
	 */
	public EvalMode getEvaluationMode()
	{
		return evalMode;
	}

	/**
	 * Set the constant values. The Values object is stored, not copied.
	 * Returns a copy of this EvaluateContext to allow chaining of method calls.
	 */
	public EvaluateContext setConstantValues(Values constantValues)
	{
		this.constantValues = constantValues;
		return this;
	}
	
	/**
	 * Add some constant values. The Values object is copied, not stored.
	 * There is no checking for duplicates.
	 * Returns a copy of this EvaluateContext to allow chaining of method calls.
	 */
	public EvaluateContext addConstantValues(Values moreConstantValues)
	{
		this.constantValues = new Values(this.constantValues, moreConstantValues);
		return this;
	}
	
	/**
	 * Get the constant values.
	 */
	public Values getConstantValues()
	{
		return constantValues;
	}
	
	/**
	 * Copy core information (constants, evaluation mode, etc.) from another EvaluateContext.
	 * Return a copy of this EvaluateContext (to allow method chaining).
	 */
	public EvaluateContext copyFrom(EvaluateContext ec)
	{
		this.evalMode = ec.evalMode;
		this.constantValues = ec.constantValues == null ? null : new Values(ec.constantValues);
		return this;
	}
	
	// Main interface for EvaluateContext classes
	
	/**
	 * Return the value for a constant (by name); null if unknown.
	 */
	public Object getConstantValue(String name)
	{
		if (constantValues == null) {
			return null;
		}
		int i = constantValues.getIndexOf(name);
		if (i == -1) {
			return null;
		}
		return constantValues.getValue(i);
	}

	/**
	 * Return the value for a variable (by name or index); null if unknown.
	 */
	public abstract Object getVarValue(String name, int index);

	/**
	 * Return the value for an observable (by name or index); null if unknown.
	 */
	public Object getObservableValue(String name, int index)
	{
		// No observables defined by default
		return null;
	}
	
	// Utility methods
	
	/*
	 * Create an EvaluateContext with no constants and default (floating point) evaluation mode.
	 */
	public static EvaluateContext create()
	{
		return new EvaluateContextConstants(null);
	}
	
	/*
	 * Create an EvaluateContext with no constants and the specified evaluation mode.
	 */
	public static EvaluateContext create(EvalMode evalMode)
	{
		return new EvaluateContextConstants(null).setEvaluationMode(evalMode);
	}
	
	/*
	 * Create an EvaluateContext with the specified constants and default (floating point) evaluation mode.
	 */
	public static EvaluateContext create(Values constantValues)
	{
		return new EvaluateContextConstants(constantValues);
	}
	
	/*
	 * Create an EvaluateContext with the specified constants and evaluation mode.
	 */
	public static EvaluateContext create(Values constantValues, EvalMode evalMode)
	{
		return new EvaluateContextConstants(constantValues).setEvaluationMode(evalMode);
	}
	
	/*
	 * Create an EvaluateContext with the specified constants and evaluation mode (exact or not).
	 */
	public static EvaluateContext create(Values constantValues, boolean exact)
	{
		return new EvaluateContextConstants(constantValues).setEvaluationMode(exact ? EvalMode.EXACT : EvalMode.FP);
	}
	
	/*
	 * Create an EvaluateContext by copying core information
	 * (constants, evaluation mode, etc.) from another EvaluateContext.
	 */
	public static EvaluateContext create(EvaluateContext ec)
	{
		return create().copyFrom(ec);
	}
}
