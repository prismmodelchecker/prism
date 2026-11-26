//==============================================================================
//
//	Copyright (c) 2026-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package prism;

import parser.EvaluateContext.EvalMode;

/**
 * The arithmetic used to build a model and to model check it.
 * For {@link #PARAM}, which constants are parameters (and their ranges)
 * is specified separately, alongside the values of the other constants.
 * <br><br>
 * This is not the same as {@link EvalMode}, which says how values are represented
 * when evaluating expressions (e.g. Double or BigRational). Parametric model checking
 * evaluates expressions exactly, and builds functions over the parameters separately,
 * so there is no parametric evaluation mode: see {@link #getEvalMode()}.
 */
public enum PrecisionMode
{
	/** Floating-point (double) arithmetic */
	FP,
	/** Exact (rational) arithmetic */
	EXACT,
	/** Parametric: rational functions over some parameters */
	PARAM;

	/**
	 * Get the mode used to evaluate expressions in this precision mode:
	 * {@link EvalMode#FP} for {@link #FP}, and {@link EvalMode#EXACT} otherwise.
	 */
	public EvalMode getEvalMode()
	{
		return this == FP ? EvalMode.FP : EvalMode.EXACT;
	}

	/**
	 * Get a description of this mode, e.g., for log output.
	 */
	public String description()
	{
		switch (this) {
		case FP:
			return "floating-point";
		case EXACT:
			return "exact";
		case PARAM:
			return "parametric";
		default:
			return toString();
		}
	}
}
