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

package explicit;

import java.util.BitSet;

import explicit.graphviz.Decorator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;

/**
 * Interface for classes that provide (read) access to an explicit-state labelled transition system (LTS).
 */
public interface LTS<Value> extends NondetModel<Value>
{
	// Accessors (for Model) - default implementations
	
	@Override
	default ModelType getModelType()
	{
		return ModelType.LTS;
	}

	@Override
	default void exportToPrismLanguage(String filename, int precision) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	// Accessors (for NondetModel) - default implementations
	
	@Override
	default void exportToDotFileWithStrat(PrismLog out, BitSet mark, int[] strat, int precision)
	{
		throw new UnsupportedOperationException();
	}
	
	// Accessors
	
	/**
	 * Get the successor state for the {@code i}th choice/transition from state {@code s}.
	 */
	public int getSuccessor(int s, int i);
}
