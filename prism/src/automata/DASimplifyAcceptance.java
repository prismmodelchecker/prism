package automata;

import java.util.BitSet;

import prism.PrismComponent;
import prism.PrismException;
import explicit.LTS;
import explicit.SCCComputer;
import explicit.SCCConsumerStore;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceRabin;
import acceptance.AcceptanceReach;
import acceptance.AcceptanceType;
import acceptance.AcceptanceRabin.RabinPair;

public class DASimplifyAcceptance
{

	/**
	 * Tries to simplify the acceptance condition of the deterministic automaton.
	 * Note that the passed parameter {@code da} may be destroyed by this function.
	 * @param parent the calling PrismComponent (for SCC computer)
	 * @param da the DA to be simplified (may be destroyed)
	 * @param allowedAcceptance the allowed acceptance types
	 */
	@SuppressWarnings("unchecked")
	public static DA<BitSet, ? extends AcceptanceOmega> simplifyAcceptance(PrismComponent parent, DA<BitSet, ? extends AcceptanceOmega> da, AcceptanceType... allowedAcceptance)
			throws PrismException
	{
		// Simplifications for DRAs
		if (da.getAcceptance() instanceof AcceptanceRabin) {
			DA<BitSet, AcceptanceRabin> dra = (DA<BitSet, AcceptanceRabin>) da;
			// K_i states that do not occur in a (non-trivial) SCC of the DRA may as well be removed
			LTS lts = new LTSFromDA(da);
			SCCConsumerStore sccStore = new SCCConsumerStore();
			SCCComputer sccComp = explicit.SCCComputer.createSCCComputer(parent, lts, sccStore);
			sccComp.computeSCCs();
			BitSet trivial = sccStore.getNotInSCCs();
			for (RabinPair pair : dra.getAcceptance()) {
				if (pair.getK().intersects(trivial)) {
					pair.getK().andNot(trivial);
				}
			}
			// See if the DRA is actually a DFA
			if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.REACH) && isDFA(dra)) {
				// we can switch to AcceptanceReach
				AcceptanceReach reachAcceptance = new AcceptanceReach(getDFAGoalStatesForRabin(dra.getAcceptance()));
				DA.switchAcceptance(dra, reachAcceptance);
				da = dra;
			}
		}
		return da;
	}

	/**
	 * Is this Rabin automaton actually a finite automaton? This check is done syntactically:
	 * it returns true if every transition from a K_i state goes to another K_i state.
	 * We also require that there are no L_i states overlapping with any K_j states.
	 */
	public static boolean isDFA(DA<BitSet, AcceptanceRabin> dra)
	{
		AcceptanceRabin acceptance = dra.getAcceptance();
		// Compute potential set of goal states as the union of all K_i sets
		BitSet goalStates = getDFAGoalStatesForRabin(acceptance);

		// Make sure there are no L_i states in the goal states for any i
		for (int i = 0; i < acceptance.size(); i++) {
			if (goalStates.intersects(acceptance.get(i).getL()))
				return false;
		}
		// Check if every transition from a goal state goes to another goal state
		for (int i = goalStates.nextSetBit(0); i >= 0; i = goalStates.nextSetBit(i + 1)) {
			int m = dra.getNumEdges(i);
			for (int j = 0; j < m; j++) {
				if (!goalStates.get(dra.getEdgeDest(i, j)))
					return false;
			}
		}
		return true;
	}

	/**
	 * Get the union of the K_i states of a Rabin acceptance condition.
	 */
	public static BitSet getDFAGoalStatesForRabin(AcceptanceRabin acceptance)
	{
		// Compute set of goal states as the union of all K_i sets
		BitSet goalStates = new BitSet();
		int n = acceptance.size();
		for (int i = 0; i < n; i++) {
			goalStates.or(acceptance.get(i).getK());
		}
		return goalStates;
	}
}
