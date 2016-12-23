//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import parser.ast.Expression;

/**
 * Interface for a model and expression transformation.<br>
 *
 * Implementing classes provide a combined model and expression transformation, allowing the calculation
 * of a (potentially simpler) expression in the transformed model to calculate the result of the
 * original expression in the original model.<br>
 *
 * The general idea is the following sequence of calls:<br>
 *
 * {@code ModelExpressionTransformation t = ...;}<br>
 * {@code StateValues resultTransformed = check t.getTransformedExpression() in t.getTransformedModel()}<br>
 * {@code StateValues result = t.projectToOriginalModel(resultTransformed);}
 */
public interface ModelExpressionTransformation<OriginalModel extends Model, TransformedModel extends Model>
       extends ModelTransformation<OriginalModel, TransformedModel> {

	/** Get the transformed expression. */
	public Expression getTransformedExpression();

	/** Get the original expression. */
	public Expression getOriginalExpression();

}
