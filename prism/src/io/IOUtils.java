//==============================================================================
//
//	Copyright (c) 2024-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package io;

import prism.PrismException;

/**
 * Various utilities for input-output functionality
 */
public class IOUtils
{
	/**
	 * Functional interface for a consumer accepting state variable values (s,i,o),
	 * i.e., state s, variable index i, value o.
	 */
	@FunctionalInterface
	public interface StateDefnConsumer {
		void accept(int s, int i, Object o) throws PrismException;
	}

	/**
	 * Functional interface for a consumer accepting Markov chain like transitions (s,s2,v,a),
	 * i.e., source state s, target state s2, probability v, (optional) action a.
	 */
	@FunctionalInterface
	public interface MCTransitionConsumer<V> {
		void accept(int s, int s2, V v, Object a);
	}

	/**
	 * Functional interface for a consumer accepting MDP-like transitions (s,i,s2,v,a),
	 * i.e., source state s, index i, target state s2, probability v, (optional) action a.
	 */
	@FunctionalInterface
	public interface MDPTransitionConsumer<V> {
		void accept(int s, int i, int s2, V v, Object a);
	}

	/**
	 * Functional interface for a consumer accepting LTS-like transitions (s,i,s2,a),
	 * i.e., source state s, index i, target state s2, (optional) action a.
	 */
	@FunctionalInterface
	public interface LTSTransitionConsumer {
		void accept(int s, int i, int s2, Object a) throws PrismException;
	}

	/**
	 * Functional interface for a consumer accepting transition rewards (s,i,v),
	 * i.e., source state s, index i, value v.
	 */
	@FunctionalInterface
	public interface TransitionRewardConsumer<V> {
		void accept(int s, int i, V v) throws PrismException;
	}

	/**
	 * Functional interface for a consumer accepting transition-successor-state rewards (s,i,s2,v),
	 * i.e., source state s, index i, target state s2, value v.
	 */
	@FunctionalInterface
	public interface TransitionStateRewardConsumer<V> {
		void accept(int s, int i, int s2, V v) throws PrismException;
	}
}
