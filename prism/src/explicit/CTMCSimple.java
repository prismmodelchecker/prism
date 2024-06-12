//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import prism.Evaluator;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * Simple explicit-state representation of a CTMC.
 */
public class CTMCSimple<Value> extends DTMCSimple<Value> implements CTMC<Value>
{
	/**
	 * The cached embedded DTMC.
	 * <p>
	 * Will become invalid if the CTMC is changed. In this case
	 * construct a new one by calling buildImplicitEmbeddedDTMC()
	 * <p>
	 * We cache this so that the PredecessorRelation of the
	 * embedded DTMC is cached.
	 */
	private DTMCEmbeddedSimple<Value> cachedEmbeddedDTMC = null;

	// Constructors

	/**
	 * Constructor: empty CTMC.
	 */
	public CTMCSimple()
	{
		super();
	}

	/**
	 * Constructor: new CTMC with fixed number of states.
	 */
	public CTMCSimple(int numStates)
	{
		super(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public CTMCSimple(CTMCSimple<Value> ctmc)
	{
		super(ctmc);
	}
	
	/**
	 * Construct a CTMC from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Note: have to build new Distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public CTMCSimple(CTMCSimple<Value> ctmc, int permut[])
	{
		super(ctmc, permut);
	}

	/**
	 * Construct a CTMCSimple object from a CTMC object.
	 */
	public CTMCSimple(CTMC<Value> ctmc)
	{
		super(ctmc);
	}

	/**
	 * Construct a CTMCSimple object from a CTMC object,
	 * mapping rate values using the provided function.
	 */
	public CTMCSimple(CTMC<Value> ctmc, Function<? super Value, ? extends Value> rateMap)
	{
		super(ctmc, rateMap, ctmc.getEvaluator());
	}

	/**
	 * Construct a CTMCSimple object from a CTMC object,
	 * mapping rate values using the provided function.
	 * Since the type changes (T -> Value), an Evaluator for Value must be given.
	 */
	public <T> CTMCSimple(DTMC<T> ctmc, Function<? super T, ? extends Value> rateMap, Evaluator<Value> eval)
	{
		super(ctmc, rateMap, eval);
	}

	// Accessors (for CTMC)
	
	@Override
	public Value getExitRate(int i)
	{
		return trans.get(i).sum();
	}
	
	@Override
	public Value getMaxExitRate()
	{
		Value max = null;
		for (int i = 0; i < numStates; i++) {
			Value d = trans.get(i).sum();
			if (max == null || getEvaluator().gt(d, max)) {
				max = d;
			}
		}
		return max;
	}
	
	@Override
	public Value getMaxExitRate(BitSet subset)
	{
		Value max = null;
		for (int i = subset.nextSetBit(0); i >= 0; i = subset.nextSetBit(i + 1)) {
			Value d = trans.get(i).sum();
			if (max == null || getEvaluator().gt(d, max)) {
				max = d;
			}
		}
		return max;
	}
	
	@Override
	public Value getDefaultUniformisationRate()
	{
		return getEvaluator().multiply(getEvaluator().fromString("1.02"), getMaxExitRate()); 
	}
	
	@Override
	public Value getDefaultUniformisationRate(BitSet nonAbs)
	{
		return getEvaluator().multiply(getEvaluator().fromString("1.02"), getMaxExitRate(nonAbs)); 
	}
	
	@Override
	public DTMC<Value> buildImplicitEmbeddedDTMC()
	{
		DTMCEmbeddedSimple<Value> dtmc = new DTMCEmbeddedSimple<>(this);
		if (cachedEmbeddedDTMC != null) {
			// replace cached DTMC
			cachedEmbeddedDTMC = dtmc;
		}
		return dtmc;
	}
	
	@Override
	public DTMC<Value> getImplicitEmbeddedDTMC()
	{
		if (cachedEmbeddedDTMC == null) {
			cachedEmbeddedDTMC = new DTMCEmbeddedSimple<>(this);
		}
		return cachedEmbeddedDTMC;
	}

	
	@Override
	public DTMCSimple<Value> buildEmbeddedDTMC()
	{
		DTMCSimple<Value> dtmc = new DTMCSimple<>(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			Distribution<Value> distr = trans.get(i);
			Value d = distr.sum();
			if (getEvaluator().isZero(d)) {
				dtmc.setProbability(i, i, getEvaluator().one());
			} else {
				for (Map.Entry<Integer, Value> e : distr) {
					dtmc.setProbability(i, e.getKey(), getEvaluator().divide(e.getValue(), d));
				}
			}
		}
		return dtmc;
	}

	@Override
	public void uniformise(Value q)
	{
		for (int i = 0; i < numStates; i++) {
			Distribution<Value> distr = trans.get(i);
			distr.set(i, getEvaluator().subtract(q, distr.sumAllBut(i)));
		}
	}

	@Override
	public DTMC<Value> buildImplicitUniformisedDTMC(Value q)
	{
		return new DTMCUniformisedSimple<>(this, q);
	}
	
	@Override
	public DTMCSimple<Value> buildUniformisedDTMC(Value q)
	{
		DTMCSimple<Value> dtmc = new DTMCSimple<>(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			// Add scaled off-diagonal entries
			Distribution<Value> distr = trans.get(i);
			for (Map.Entry<Integer, Value> e : distr) {
				dtmc.setProbability(i, e.getKey(), getEvaluator().divide(e.getValue(), q));
			}
			// Add diagonal, if needed
			Value d = distr.sumAllBut(i);
			// if (d < q): P(i,i) = 1 - (d / q)
			if (!getEvaluator().geq(d, q)) {
				dtmc.setProbability(i, i, getEvaluator().subtract(getEvaluator().one(), getEvaluator().divide(d, q)));
			}
		}
		return dtmc;
	}
}
