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

package common;

/**
 * Helper class that wraps some other object and strips away
 * custom implementations of {@code hashCode} and {@code equals}.
 *
 * This is useful for storing objects that have such custom implementations
 * in a HashSet or HashMap when the goal is to maintain the distinction
 * between different Java objects, even though they might test equal via
 * {@code equals}.
 *
 * A PlainObjectReference wraps another object and provides {@code hashCode}
 * via {@code System.identityHashCode} and {@code equals} via equality checking
 * with the {@code ==} operator, i.e., what would happen using the
 * default implementation of {@code Object}.
 *
 * @param <T> the type of the underlying class
 */
public class PlainObjectReference<T>
{
	/** The object that is referenced */
	private T object;

	/** Constructor, take reference to object */
	public PlainObjectReference(T object)
	{
		this.object = object;
	}

	/** Get the referenced object */
	public T getObject()
	{
		return object;
	}

	@Override
	public int hashCode()
	{
		// return the identityHashCode of the referenced object,
		// just as it would happen if there were no {@code hashCode}
		// method in the object
		return System.identityHashCode(object);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PlainObjectReference))
			return false;
		PlainObjectReference<?> other = (PlainObjectReference<?>) obj;
		// test for equality via ==
		return other.object == object;
	}
}
