//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Nishan Kamaleson <nxk249@bham.ac.uk> (University of Birmingham)
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

import param.Function;
import param.FunctionFactory;
import param.ModelBuilder;
import parser.ast.Expression;

/**
 * Interface for classes that generate a probabilistic model:
 * given a particular model state (represented as a State object),
 * they provide information about the outgoing transitions from that state.
 */
public interface ModelGeneratorSymbolic extends ModelGenerator
{
	// Extra methods for symbolic interface (bit of a hack for now:
	// we assume some methods do not need to be implemented, e.g. getTransitionProbability,
	// and and some new ones to replace them, e.g. getTransitionProbabilityFunction
	
	public void setSymbolic(ModelBuilder modelBuilder, FunctionFactory functionFactory);
	
	public Expression getUnknownConstantDefinition(String name) throws PrismException;

	public Function getTransitionProbabilityFunction(int i, int offset) throws PrismException;
}
