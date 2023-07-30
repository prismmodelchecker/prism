//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
//	* Alberto Puggelli <alberto.puggelli@gmail.com>
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

import common.Interval;
import prism.Evaluator;

/**
 * Simple explicit-state representation of an IDTMC.
 */
public class IDTMCSimple<Value> extends DTMCSimple<Interval<Value>> implements IDTMC<Value>
{
	// Constructors

	/**
	 * Constructor: empty IDTMC.
	 */
	public IDTMCSimple()
	{
		super();
		createDefaultEvaluator();
	}

	/**
	 * Constructor: new IDTMC with fixed number of states.
	 */
	public IDTMCSimple(int numStates)
	{
		super(numStates);
		createDefaultEvaluator();
	}

	/**
	 * Copy constructor.
	 */
	public IDTMCSimple(IDTMCSimple<Value> idtmc)
	{
		super(idtmc);
		createDefaultEvaluator();
	}

	/**
	 * Construct an IDTMC from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Pointer to states list is NOT copied (since now wrong).
	 * Note: have to build new Distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public IDTMCSimple(IDTMCSimple<Value> idtmc, int permut[])
	{
		super(idtmc, permut);
		createDefaultEvaluator();
	}
	
	/**
	 * Delimit the intervals for probabilities from state s,
	 * i.e., trim the bounds of the intervals such that at least one
	 * possible distribution takes each of the extremal values.
	 * @param s The index of the state to delimit
	 * @param evalChil An evaluator for the interval's child type (Value)
	 */
	public void delimit(int s, Evaluator<Value> evalChil)
	{
		IntervalUtils.delimit(trans.get(s), evalChil);
	}
	
	/**
	 * Create a default Evaluator for double intervals (default for ModelExplicit is for doubles)
	 */
	@SuppressWarnings("unchecked")
	private void createDefaultEvaluator()
	{
		((IDTMCSimple<Double>) this).setEvaluator(Evaluator.forDoubleInterval());
	}
}
