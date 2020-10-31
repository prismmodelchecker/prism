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
	/**
	 * Evaluation mode (floating point? exact?)
	 */
	public enum EvalMode { FP, EXACT }
	protected EvalMode evalMode = EvalMode.FP;
	
	/**
	 * Constant values (optional)
	 */
	protected Values constantValues = null;
	
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
}
