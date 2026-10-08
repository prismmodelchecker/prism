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
	 * Functional interface for a consumer accepting state variable values (s,i,v),
	 * i.e., state s, variable index i, value v.
	 */
	@FunctionalInterface
	public interface StateDefnConsumer {
		void accept(int s, int i, Object v) throws PrismException;
	}

	/**
	 * Functional interface for a consumer accepting Markov chain like transitions (s,s2,v,a),
	 * i.e., source state s, target state s2, probability v, (optional) action a.
	 */
	@FunctionalInterface
	public interface MCTransitionConsumer<V> {
		void accept(int s, int s2, V v, Object a) throws PrismException;
	}

	/**
	 * Functional interface for a consumer accepting MDP-like transitions (s,i,s2,v,a),
	 * i.e., source state s, index i, target state s2, probability v, (optional) action a.
	 */
	@FunctionalInterface
	public interface MDPTransitionConsumer<V> {
		void accept(int s, int i, int s2, V v, Object a) throws PrismException;
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
	 * Functional interface for a consumer accepting integer state values (s,v),
	 * i.e., state s, value v.
	 */
	@FunctionalInterface
	public interface StateIntConsumer {
		void accept(int s, int v) throws PrismException;
	}

	/**
	 * Functional interface for a consumer accepting state values (s,v),
	 * i.e., state s, value v.
	 */
	@FunctionalInterface
	public interface StateValueConsumer<V> {
		void accept(int s, V v) throws PrismException;
	}

	/**
	 * Functional interface for a consumer accepting state rewards (s,v),
	 * i.e., state s, value v.
	 */
	@FunctionalInterface
	public interface StateRewardConsumer<V> {
		void accept(int s, V v) throws PrismException;
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
	// TODO REMOVE
	@FunctionalInterface
	public interface TransitionStateRewardConsumer<V> {
		void accept(int s, int i, int s2, V v) throws PrismException;
	}

	/**
	 * Functional interface for a consumer accepting transition-successor-state rewards (s,i,j,v),
	 * i.e., source state s, index one i, index two j, target state s2, value v.
	 */
	@FunctionalInterface
	public interface TransitionSuccRewardConsumer<V> {
		void accept(int s, int i, int j, V v) throws PrismException;
	}

	/**
	 * Transitions of a (double-valued) Markov chain, stored in sparse matrix form.
	 * The transitions of state s are those with indices rowStarts[s] to rowStarts[s+1]-1.
	 */
	public static class SparseMCTransitions
	{
		/** Index of first transition of each state (length: numStates + 1) */
		public int[] rowStarts;
		/** Successor state of each transition */
		public int[] successors;
		/** Probability (or rate) of each transition */
		public double[] probabilities;
		/** Action of each transition */
		public Object[] actions;
	}

	/**
	 * Transitions of a (double-valued) Markov decision process, stored in sparse matrix form.
	 * The choices of state s are those with indices rowStarts[s] to rowStarts[s+1]-1,
	 * and the transitions of choice i are those with indices choiceStarts[i] to choiceStarts[i+1]-1.
	 */
	public static class SparseMDPTransitions
	{
		/** Index of first choice of each state (length: numStates + 1) */
		public int[] rowStarts;
		/** Index of first transition of each choice (length: numChoices + 1) */
		public int[] choiceStarts;
		/** Successor state of each transition */
		public int[] successors;
		/** Probability of each transition */
		public double[] probabilities;
		/** Action of each choice */
		public Object[] actions;
	}
}
