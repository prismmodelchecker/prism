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

import java.util.HashMap;

/** @file
 * Provides StateMapper
 */

/** A mapper from KeyType to StateType, ResultType can be used as an alternative key type. */
public class StateMapper <R extends NBA2DAResult<K>, K extends NBA2DAState, S extends DA_State> implements StateMapperInterface<R,K,S> {

	/** The hash map from StateType to MappedStateType */
	private HashMap<K,S> _map;
	/** The number of mappings */
	private int _count;

	/** Constructor. */
	public StateMapper() {
		_count = 0;
		_map = new HashMap<K,S>();
	}
		
	/** Clear the mapping */
	public void clear() {
		_map.clear();
		_count = 0;
	}

	/** Add a mapping. 
	 * @param key the key
	 * @param state the state
	 */
	// public StateType add(KeyType key, StateType state) {
	public void add(K key, S state) {
		// System.out.println("Adding hash " + key.hashCode());
		if (!_map.containsKey(key))
			++_count;
		_map.put(key, state);
		// return state; // different semantics than java hashmaps
		// System.out.println("Item count: " + _count);
	}


	/** Find a mapping. 
	 * @param key the key
	 * @return the state (or the NULL pointer if not found)
	 */
	public S find(K key) {
		// System.out.println("Looking for hash " + key.hashCode());
		// if (_map.containsKey(key)) System.out.println("Match");
		// else System.out.println("Miss");
		return _map.get(key);
	}

	/** Find a mapping using ResultType. 
	 * @param result
	 * @return the state (or the NULL pointer if not found)
	 */
	public S find(R result) {
		return find(result.getState());
	}

	/** Get number of mappings.
	 * @return the number of mappings
	 */
	public int size() {
		return _count;
	}
}
