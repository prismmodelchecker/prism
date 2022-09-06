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

import jdd.JDDNode;
import parser.ast.Expression;
import prism.PrismException;

/**
 * Nesting of two model/expression transformations.
 * <br>
 * original -(innerTransformation)-&gt; intermediate -(outerTransformation)-&gt; transformed
 */

public class ModelExpressionTransformationNested<OriginalModel extends Model, IntermediateModel extends Model, TransformedModel extends Model> implements
       ModelExpressionTransformation<OriginalModel, TransformedModel> {

	/** The inner transformation */
	protected ModelExpressionTransformation<OriginalModel, IntermediateModel> innerTransformation;
	/** The outer transformation */
	protected ModelExpressionTransformation<IntermediateModel,TransformedModel> outerTransformation;

	/**
	 * Constructor, chain the two transformations.
	 * <br>
	 * The transformations will be cleared on a call to {@code clear()}.
	 * <br>
	 * [STORE: innerTransformation, outerTransformation ]
	 */
	public ModelExpressionTransformationNested(ModelExpressionTransformation<OriginalModel, IntermediateModel> innerTransformation,
			ModelExpressionTransformation<IntermediateModel, TransformedModel> outerTransformation)
	{
		this.innerTransformation = innerTransformation;
		this.outerTransformation = outerTransformation;

		if (innerTransformation.getTransformedModel() != outerTransformation.getOriginalModel()) {
			throw new IllegalArgumentException("Trying to nest unrelated ModelExpressionTransformations.");
		}
	}

	@Override
	public TransformedModel getTransformedModel()
	{
		return outerTransformation.getTransformedModel();
	}

	@Override
	public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException
	{
		StateValues svIntermediate = outerTransformation.projectToOriginalModel(svTransformedModel);
		StateValues svOriginal = innerTransformation.projectToOriginalModel(svIntermediate);
		return svOriginal;
	}

	@Override
	public Expression getTransformedExpression()
	{
		return outerTransformation.getTransformedExpression();
	}

	@Override
	public OriginalModel getOriginalModel() {
		return innerTransformation.getOriginalModel();
	}

	@Override
	public Expression getOriginalExpression()
	{
		return innerTransformation.getOriginalExpression();
	}

	@Override
	public JDDNode getTransformedStatesOfInterest()
	{
		return outerTransformation.getTransformedStatesOfInterest();
	}

	@Override
	public void clear()
	{
		outerTransformation.clear();
		innerTransformation.clear();
	}
}