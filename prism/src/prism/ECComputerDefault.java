package prism;

import java.util.Stack;
import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

public class ECComputerDefault extends ECComputer
{	
	public ECComputerDefault(Prism prism, JDDNode reach, JDDNode trans, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars,
			JDDVars allDDNondetVars)
	{
		super(prism, reach, trans, trans01, allDDRowVars, allDDColVars, allDDNondetVars);
	}

	public void computeECs()
	{
		vectECs = new Vector<JDDNode>();

		boolean initialCandidate = true;
		Stack<JDDNode> candidates = new Stack<JDDNode>();
		JDD.Ref(reach);
		candidates.push(reach);
		SCCComputer sccComputer;

		while (!candidates.isEmpty()) 
		{
			System.out.println("Checking candidate");
			
			JDDNode candidate = candidates.pop();

			// Compute the stable set
			JDD.Ref(candidate);
			JDDNode stableSet = findMaximalStableSet(candidate);

			System.out.println("found max stable set");
			
			// Drop empty sets
			if (stableSet.equals(JDD.ZERO)) 
			{
				System.out.println("empty set");
				JDD.Deref(stableSet);
				JDD.Deref(candidate);
				continue;
			}

			if (!initialCandidate) 
			{
				System.out.println("not initial");
				
				// candidate is an SCC, check if it's stable
				if (stableSet.equals(candidate)) 
				{
					vectECs.add(maxStableSetChoices(candidate));
					JDD.Deref(stableSet);
					continue;
				}
			} 
			else 
			{
				initialCandidate = false;
			}
			
			JDD.Deref(candidate);

			// Filter bad transitions
			JDD.Ref(stableSet);
			JDDNode stableSetTrans = maxStableSetTrans(stableSet);

			// now find the maximal SCCs in (stableSet, stableSetTrans)
			Vector<JDDNode> sccs;
			sccComputer = prism.getSCCComputer(stableSet, stableSetTrans, allDDRowVars, allDDColVars);
			sccComputer.computeSCCs();
			JDD.Deref(stableSet);
			JDD.Deref(stableSetTrans);
			sccs = sccComputer.getVectSCCs();
			JDD.Deref(sccComputer.getNotInSCCs());
			candidates.addAll(sccs);
		}
	}
	
	/**
	 * Returns a stable set of states contained in candidateStates
	 * 
	 * @param candidateStates
	 *            set of candidate states S x H_i (dereferenced after calling this function)
	 * @return a referenced BDD with the maximal stable set in c
	 */
	private JDDNode findMaximalStableSet(JDDNode candidateStates)
	{
		System.out.println("findMaximalStableSet");

		
		
		JDDNode old = JDD.Constant(0);
		JDDNode current = candidateStates;
		
		if (current.isConstant())
			System.out.println("current = " + current.getValue());

		while (!current.equals(old)) 
		{
			JDD.Deref(old);
			JDD.Ref(current);
			old = current;

			JDD.Ref(current);
			JDD.Ref(trans);
			// Select transitions starting in current
			JDDNode currTrans = JDD.Apply(JDD.TIMES, trans, current);
			// Select transitions starting in current and ending in current
			JDDNode tmp = JDD.PermuteVariables(current, allDDRowVars, allDDColVars);
			tmp = JDD.Apply(JDD.TIMES, currTrans, tmp);
			// Sum all successor probabilities for each (state, action) tuple
			tmp = JDD.SumAbstract(tmp, allDDColVars);
			// If the sum for a (state,action) tuple is 1,
			// there is an action that remains in the stable set with prob 1
			tmp = JDD.GreaterThan(tmp, 1 - prism.getSumRoundOff());
			// Without fairness, we just need one action per state
			current = JDD.ThereExists(tmp, allDDNondetVars);
		}
		JDD.Deref(old);
		return current;
	}
	
	/**
	 * Returns the transition relation of a stable set
	 * 
	 * @param b
	 *            BDD of a stable set (dereferenced after calling this function)
	 * @return referenced BDD of the transition relation restricted to the stable set
	 */
	private JDDNode maxStableSetTrans(JDDNode b)
	{
		System.out.println("maxStableSetTrans");
		JDD.Ref(b);
		JDD.Ref(trans);
		// Select transitions starting in b
		JDDNode currTrans = JDD.Apply(JDD.TIMES, trans, b);
		JDDNode mask = JDD.PermuteVariables(b, allDDRowVars, allDDColVars);
		// Select transitions starting in current and ending in current
		mask = JDD.Apply(JDD.TIMES, currTrans, mask);
		// Sum all successor probabilities for each (state, action) tuple
		mask = JDD.SumAbstract(mask, allDDColVars);
		// If the sum for a (state,action) tuple is 1,
		// there is an action that remains in the stable set with prob 1
		mask = JDD.GreaterThan(mask, 1 - prism.getSumRoundOff());
		// select the transitions starting in these tuples
		JDD.Ref(trans01);
		JDDNode stableTrans01 = JDD.And(trans01, mask);
		// Abstract over actions
		return JDD.ThereExists(stableTrans01, allDDNondetVars);
	}
	
	/**
	 * Returns the transition relation of a stable set
	 * 
	 * @param b
	 *            BDD of a stable set (dereferenced after calling this function)
	 * @return referenced BDD of the transition relation restricted to the stable set
	 */
	public JDDNode maxStableSetChoices(JDDNode b)
	{
		System.out.println("maxStableSetChoices");
		
		JDD.Ref(b);
		JDD.Ref(trans);
		// Select transitions starting in b
		JDDNode currTrans = JDD.Apply(JDD.TIMES, trans, b);
		JDDNode mask = JDD.PermuteVariables(b, allDDRowVars, allDDColVars);
		// Select transitions starting in current and ending in current
		mask = JDD.Apply(JDD.TIMES, currTrans, mask);
		// Sum all successor probabilities for each (state, action) tuple
		mask = JDD.SumAbstract(mask, allDDColVars);
		// If the sum for a (state,action) tuple is 1,
		// there is an action that remains in the stable set with prob 1
		mask = JDD.GreaterThan(mask, 1 - prism.getSumRoundOff());
		// select the transitions starting in these tuples
		JDD.Ref(trans01);
		JDDNode stableTrans01 = JDD.And(trans01, mask);
		// Abstract over actions
		return JDD.ThereExists(stableTrans01, allDDColVars);
	}

}
