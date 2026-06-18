//==============================================================================
//
//	Copyright (c) 2002-
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

package prism;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import acceptance.AcceptanceRabin;
import automata.DA;

/**
 * Bundles a multi-objective query ({@link MultiObjQuery}) together with the
 * engine-specific reward structures and accepting target sets computed for that
 * query against a particular product model.
 *
 * <p>Type parameters:
 * <ul>
 *   <li>{@code R} — per-reward-objective reward structure
 *       (e.g. {@code JDDNode} for the symbolic engine)
 *   <li>{@code T} — per-probability-objective accepting-EC target set
 *       (e.g. {@code JDDNode} for the symbolic engine)
 * </ul>
 *
 * <p>The instance is populated in two phases:
 * <ol>
 *   <li>Construction — {@link #moQuery}, {@link #dra} and {@link #rewards} are set once.
 *   <li>Build phase — {@link #targets} is filled by the model checker, then
 *       {@link #combinations}, {@link #combinationIDs} and {@link #conflictCount}
 *       would be set if probabilistic objectives had conflicting accepting states.
 *       Currently, conflict detection is never enabled (see {@link #conflictCount}).
 * </ol>
 */
public class MultiObjQueryInstance<R, T>
{
	/** The multi-objective query: operators, bounds, path formulas. */
	public final MultiObjQuery moQuery;

	/**
	 * Deterministic Rabin automaton per overall objective slot; null in reward-objective slots.
	 * Indexed by the same position as {@link MultiObjQuery}'s global objective list.
	 */
	public final DA<BitSet, AcceptanceRabin>[] dra;

	/** Engine-specific reward structure, one entry per reward objective. */
	public final List<R> rewards;

	/**
	 * Engine-specific accepting-EC target set, one entry per probabilistic objective.
	 * Populated during the build phase; entries may be refined by conflict resolution.
	 */
	public List<T> targets;

	/**
	 * Combined-EC target sets when probabilistic objectives share accepting states;
	 * null if {@link #conflictCount} &le; 1.
	 */
	public List<T> combinations;

	/**
	 * Bitmask per entry in {@link #combinations}: which objective indices share that EC.
	 * null when {@link #combinations} is null.
	 */
	public List<Integer> combinationIDs;

	/**
	 * Number of probabilistic objectives with potentially conflicting accepting formulae.
	 * Currently always zero, since nothing computes it, so {@link #hasConflicts()} is false.
	 */
	public int conflictCount;

	/**
	 * @param moQuery  The multi-objective query
	 * @param dra      DRA array indexed by overall objective position (null entries for reward objectives)
	 * @param rewards  Engine-specific reward structures, one per reward objective
	 */
	public MultiObjQueryInstance(MultiObjQuery moQuery, DA<BitSet, AcceptanceRabin>[] dra, List<R> rewards)
	{
		this.moQuery = moQuery;
		this.dra = dra;
		this.rewards = rewards;
		this.targets = new ArrayList<>();
	}

	/** Returns the total number of objectives. */
	public int numObjectives()
	{
		return moQuery.numObjectives();
	}

	/** Returns true if probabilistic objectives have conflicting accepting states. */
	public boolean hasConflicts()
	{
		return conflictCount > 1;
	}
}
