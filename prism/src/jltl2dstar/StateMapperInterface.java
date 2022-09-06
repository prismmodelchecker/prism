/*
 * This file is part of a Java port of the program ltl2dstar
 * (http://www.ltl2dstar.de/) for PRISM (http://www.prismmodelchecker.org/)
 * Copyright (C) 2005-2007 Joachim Klein <j.klein@ltl2dstar.de>
 * Copyright (c) 2007 Carlos Bederian
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as 
 *  published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jltl2dstar;

public interface StateMapperInterface<R extends NBA2DAResult<K>, K extends NBA2DAState, S extends DA_State> {

	/** Clear the mapping */
	public abstract void clear();
	
	/** Add a mapping. 
	 * @param key the key
	 * @param state the state
	 */
	public abstract void add(K key, S state);
	
	/** Find a mapping using ResultType. 
	 * @param result
	 * @return the state (or the NULL pointer if not found)
	 */
	public abstract S find(R result);

	/** Get number of mappings.
	 * @return the number of mappings
	 */
	public abstract int size();
}
