//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Christian von Essen <christian.vonessen@imag.fr> (Verimag, Grenoble)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//  * Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import prism.PrismComponent;
import prism.PrismException;

/**
 * Abstract class for (explicit) classes that compute (B)SCCs,
 * i.e. (bottom) strongly connected components, for a model's transition graph.
 */
public abstract class SCCComputer extends PrismComponent
{
	/** The consumer */
	protected SCCConsumer consumer;

	// Method used for finding (B)SCCs
	public enum SCCMethod {
		TARJAN;
		public String fullName()
		{
			switch (this) {
			case TARJAN:
				return "Tarjan";
			default:
				return this.toString();
			}
		}
	};

	/**
	 * Static method to create a new SCCComputer object, depending on current settings.
	 */
	public static SCCComputer createSCCComputer(PrismComponent parent, Model model, SCCConsumer consumer) throws PrismException
	{
		// Only one algorithm implemented currently
		return new SCCComputerTarjan(parent, model, consumer);
	}

	/**
	 * Compute an SCCInfo data structure (topological ordering).
	 * @param parent PrismComponent (for settings)
	 * @param model the model
	 * @param withTrivialSCCs include trivial SCCs?
	 */
	public static SCCInfo computeTopologicalOrdering(PrismComponent parent, Model model, boolean withTrivialSCCs) throws PrismException
	{
		return computeTopologicalOrdering(parent, model, withTrivialSCCs, null);
	}

	/**
	 * Compute an SCCInfo data structure (topological ordering).
	 * <br>
	 * If {@code restrict != null}, restricts the underlying graph to only those states s
	 * for which {@code restrict.test(s)} evaluates to true ("relevant states").
	 * Edges to non-relevant states are ignored and the non-relevant
	 * states are not considered as potential initial states.
	 * @param parent PrismComponent (for settings)
	 * @param model the model
	 * @param withTrivialSCCs include trivial SCCs?
	 * @param restrict predicate that indicates that a state is relevant ({@code null}: all states are relevant)
	 */
	public static SCCInfo computeTopologicalOrdering(PrismComponent parent, Model model, boolean withTrivialSCCs, IntPredicate restrict) throws PrismException
	{
		SCCInfo sccs = new SCCInfo(model.getNumStates());
		SCCComputer sccComputer = SCCComputer.createSCCComputer(parent, model, sccs);
		// Compute SCCInfo, possibly including trivial SCCs,
		// restricted to the sub-graph obtained when only considering
		// states in restrict (if not-null)
		sccComputer.computeSCCs(!withTrivialSCCs, restrict);

		return sccs;
	}

	/**
	 * Convenience method for functional iteration over the SCCs of a model.
	 * <br>
	 * For each non-trivial SCC, the given consumer's accept method is called
	 * with a BitSet containing the state indizes of the states in the SCC.
	 * <br>
	 * Note: The BitSet may be reused during calls to {@code accept}. So,
	 * if you need to store it, clone it.
	 *
	 * @param parent the parent PrismComponent (for access to settings)
	 * @param model the model
	 * @param sccConsumer the consumer
	 */
	public static void forEachSCC(PrismComponent parent, Model model, Consumer<BitSet> sccConsumer) throws PrismException
	{
		// use consumer that reuses the BitSet
		SCCComputer sccComputer = createSCCComputer(parent, model, new SCCConsumerBitSet(true) {
			@Override
			public void notifyNextSCC(BitSet scc) throws PrismException
			{
				sccConsumer.accept(scc);
			}
		});
		sccComputer.computeSCCs();
	}

	/**
	 * Base constructor.
	 */
	public SCCComputer(PrismComponent parent, SCCConsumer consumer) throws PrismException
	{
		super(parent);
		this.consumer = consumer;
	}

	/**
	 * Compute strongly connected components (SCCs) and notify the consumer.
	 * This will only report non-trivial SCCs
	 */
	public void computeSCCs() throws PrismException
	{
		computeSCCs(true);
	}

	/**
	 * Compute strongly connected components (SCCs) and notify the consumer.
	 * Ignores trivial SCCS if {@code filterTrivialSCCs} is set to true.
	 */
	public void computeSCCs(boolean filterTrivialSCCs) throws PrismException
	{
		computeSCCs(filterTrivialSCCs, null);
	}

	/**
	 * Compute strongly connected components (SCCs) and notify the consumer.
	 * Ignores trivial SCCS if {@code filterTrivialSCCs} is set to true.
	 * If {@code restrict != null}, restricts the underlying graph to only those states s
	 * for which {@code restrict.test(s)} evaluates to true ("relevant states").
	 * Edges to non-relevant states are ignored and the non-relevant
	 * states are not considered as potential initial states.
	 * @param filterTrivialSCCs ignore trivial SCCs
	 * @param restrict predicate that indicates that a state is relevant ({@code null}: all states are relevant)
	 */
	public abstract void computeSCCs(boolean filterTrivialSCCs, IntPredicate restrictStates) throws PrismException;

	/**
	 * Returns true if {@code state}, assumed to be an SCC, is a trivial SCC,
	 * i.e., has no self lopp.
	 * @param model the model
	 * @param state the state index
	 */
	protected boolean isTrivialSCC(Model model, int state)
	{
		// false if there is a self-loop, i.e., a successor t == state
		return !(model.someSuccessorsMatch(state, (t) -> {return t == state;}));
	}
}
