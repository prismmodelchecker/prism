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

/** @file
 * Provide class StateMapperFuzzy
 */

import java.util.HashMap;
import java.util.Vector;

/**
 * A mapping from KeyType to StateType, with ResultType as an alternative key type, which can be fuzzily matched
 * using CandidateMatcher.
 */
public class StateMapperFuzzy <CandidateMatcher extends SafraTreeCandidateMatcher> implements StateMapperInterface<SafraTreeTemplate, SafraTree, DA_State>
{
	private class ListValue {
		SafraTree _key;
		DA_State _state;
	}

	/** The hash map from StateType to MappedStateType */
	private HashMap<AbstractedKeyType, Vector<ListValue>> _map;
	private int _count;

	/** Constructor. */
	public StateMapperFuzzy() {
		_count = 0;
		_map = new HashMap<AbstractedKeyType, Vector<ListValue>>();
	}

	/** Clear the mapping. */
	public void clear() {
		_map.clear();
		_count = 0;
	}

	/** 
	 * Search for a mapping, fuzzily
	 * @param result the query
	 * @return the corresponding state or NULL otherwise
	 */
	public DA_State find(SafraTreeTemplate result) {

		AbstractedKeyType search_key = new AbstractedKeyType(result.getState());

		Vector<ListValue> list = _map.get(search_key);
		if (list != null) {
			for (ListValue elem : list) {
				if (CandidateMatcher.isMatch(result, elem._key)) {
					return elem._state;
				}
			}
		}
		// not found
		return null;
	}


	/** 
	 * Add a mapping
	 * @param key the key
	 * @param state the state
	 */
	public void add(SafraTree key, DA_State state) {
		
		AbstractedKeyType akey = new AbstractedKeyType(key);

		ListValue item = new ListValue();

		item._key = key;
		item._state = state;

		if (_map.get(akey) == null) {
			_map.put(akey, new Vector<ListValue>());
		}
		_map.get(akey).add(item);

		_count++;
	}

	/** Get the number of trees */
	public int size() {return _count;}



	/** 
	 * A structure that abstracts the Keytype to its abstracted properties
	 */
	private class AbstractedKeyType {

		private SafraTree _key;

		public AbstractedKeyType(SafraTree key) {_key = key;}
		
		public int hashCode() {
			return CandidateMatcher.hash(_key);
		}

		public boolean equals(AbstractedKeyType other) {
			return CandidateMatcher.abstract_equal_to(_key, other._key);
		}
		
		public boolean equals(Object o) {
			return (o.getClass() == this.getClass()) && this.equals((AbstractedKeyType) o);
		}

		public boolean lessThan(AbstractedKeyType other) {
			return CandidateMatcher.abstract_less_than(_key, other._key);
		}
	}
}
