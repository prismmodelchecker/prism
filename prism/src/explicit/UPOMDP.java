//==============================================================================
//
//	Copyright (c) 2025-
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

package explicit;

import prism.ModelType;
import prism.PrismException;

/**
 * Interface for classes that provide (read) access to an explicit-state uncertain POMDP.
 */
public interface UPOMDP<Value> extends NondetModel<Value>, PartiallyObservableModel<Value>
{
    // Accessors (for Model) - default implementations

    @Override
    public default ModelType getModelType()
    {
        return ModelType.UPOMDP;
    }

    @Override
    default void exportToPrismLanguage(final String filename, int precision) throws PrismException
    {
        throw new UnsupportedOperationException("UPOMDP model export not implemented");
    }

    // Accessors

    /**
     * Checks that transition probability lower bounds are positive
     * and throws an exception if any are not.
     */
    public default void checkLowerBoundsArePositive() throws PrismException
    {
        throw new UnsupportedOperationException();
    }
}
