//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

package common.iterable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * An adaptor that extends non-functional Iterables with the methods provided by {@link FunctionalIterable}.
 *
 * @param <E> type of the {@link Iterable}'s elements
 * @param <I> type of the Iterable to extend
 */
public class IterableAdaptor<E> implements FunctionalIterable<E>
{
	/** the Iterable that is extended */
	protected final Iterable<E> iterable;

	/**
	 * Generic constructor that wraps an Iterable.
	 *
	 * @param iterable the {@link Iterable} to be extended
	 */
	public IterableAdaptor(Iterable<E> iterable)
	{
		Objects.requireNonNull(iterable);
		this.iterable = iterable;
	}

	@Override
	public FunctionalIterator<E> iterator()
	{
		return FunctionalIterator.extend(iterable.iterator());
	}

	@Override
	public void forEach(Consumer<? super E> action)
	{
		iterable.forEach(action);
	}
}
