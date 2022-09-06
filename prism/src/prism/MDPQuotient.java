//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de>
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

import java.util.List;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import jdd.SanityJDD;

/**
 * Transformation for obtaining the quotient MDP for an MDP, given an
 * equivalence relation.
 * <br>
 * Given a list of equivalent classes (have to be disjoint, but don't need
 * to cover the whole state space), each class is collapsed to a single state,
 * the representative for the class.
 * <br>
 * Transitions from and to a state in an equivalence class are attached
 * to the representative instead.
 * To allow a symbolic treatment, several new non-deterministic choice
 * variables are introduced, yielding action labels of the form (t,s,alpha):
 * <ul>
 * <li>(!tau, 0, alpha) corresponds to an action alpha in the original model (not originating
 *     from a state in an equivalence class)</li>
 * <li>(tau, s, alpha) corresponds to an action alpha that originated from state s in some equivalence class</li>
 * </ul>
 * Self-loop actions on the representative, i.e., actions where the representative is the
 * only successor, are removed in most cases. They are only retained if the representative
 * would become a deadlock state otherwise.
 * <br>
 * Rewards are handled as follows:
 * <ul>
 * <li>State rewards for all states in equivalence classes are set to zero</li>
 * <li>Transition rewards for all state-action pairs where all successors
 *     remain in the equivalence class are set to zero as well (this is visible for the self-loops on
 *     representatives that would be deadlocks otherwise)</li>
 * </ul>
 * <br>
 * Labels attached to the original model are currently effectively stripped,
 * i.e., they are replaced with JDD.ZERO. In the future, more advanced treatment
 * may be added.
 * <br>
 * Note as well that evaluating expressions in the quotient model will not lead to correct results.
 */
public class MDPQuotient implements ModelTransformation<NondetModel,NondetModel>
{
	private NondetModel originalModel;
	private NondetModel transformedModel;
	private MDPQuotientOperator transform;
	private JDDNode transformedStatesOfInterest;

	/** Private constructor */
	private MDPQuotient(NondetModel originalModel, NondetModel transformedModel, MDPQuotientOperator transform, JDDNode transformedStatesOfInterest)
	{
		this.originalModel = originalModel;
		this.transformedModel = transformedModel;
		this.transform = transform;
		this.transformedStatesOfInterest = transformedStatesOfInterest;
	}

	@Override
	public NondetModel getOriginalModel()
	{
		return originalModel;
	}

	@Override
	public NondetModel getTransformedModel()
	{
		return transformedModel;
	}

	@Override
	public void clear()
	{
		transformedModel.clear();
		transform.clear();
		JDD.Deref(transformedStatesOfInterest);
	}

	@Override
	public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException
	{
		StateValuesMTBDD sv = svTransformedModel.convertToStateValuesMTBDD();
		JDDNode v = sv.getJDDNode().copy();
		sv.clear();

		v = transform.liftFromRepresentatives(v);

		return new StateValuesMTBDD(v, transform.originalModel);
	}

	/** Map the given state set from the original model to the quotient model */
	public JDDNode mapStateSetToQuotient(JDDNode S)
	{
		return transform.mapStateSet(S);
	}

	@Override
	public JDDNode getTransformedStatesOfInterest()
	{
		return transformedStatesOfInterest;
	}

	/**
	 * Compute the quotient MDP, collapsing each maximal end component (MEC) to a single state.
	 * Only collapses MECs contained in {@code restrict}.
	 *
	 * <br>[ REFs: <i>result</i>, DEREFs: restrict ]
	 */
	public static MDPQuotient mecQuotient(PrismComponent parent, final NondetModel model, JDDNode restrict, JDDNode statesOfInterest) throws PrismException
	{
		ECComputer ec = ECComputer.createECComputer(parent, model);
		ec.computeMECStates(restrict);
		JDD.Deref(restrict);

		return transform(parent, model, ec.getMECStates(), statesOfInterest);
	}

	/**
	 * Compute the quotient MDP for the given list of equivalence classes (the classes have to be disjoint).
	 *
	 * <br>[ REFs: <i>result</i>, DEREFs: equivalenceClasses, statesOfInterest ]
	 */
	public static MDPQuotient transform(PrismComponent parent, final NondetModel model, List<JDDNode> equivalentClasses, JDDNode statesOfInterest)
			throws PrismException
	{
		final MDPQuotientOperator transform = new MDPQuotientOperator(parent, model, equivalentClasses);
		final JDDNode transformedStatesOfInterest = transform.mapStateSet(statesOfInterest);

		final NondetModel quotient = model.getTransformed(transform);

		return new MDPQuotient(model, quotient, transform, transformedStatesOfInterest);
	}

	/** The transformation operator for the MDPQuotient operation. */
	public static class MDPQuotientOperator extends NondetModelTransformationOperator {
		/** the list of equivalence classes */
		private List<JDDNode> equivalentClasses;

		/**
		 * A symbolic mapping (0/1-ADD) from states (row vars) to their representative (col vars)
		 * in the quotient model.<br>
		 * For states in equivalence classes map to the representative, states not contained in
		 * an equivalent class are mapped to themselves.
		 */
		private JDDNode map;

		/** State set: states contained in equivalence classes */
		private JDDNode inEC;

		/** State set: states not contained in equivalence classes */
		private JDDNode notInEC;

		/** State set: representative states for equivalence classes */
		private JDDNode representatives = JDD.Constant(0);

		/**
		 * n JDDVars, where n = number of row vars in the original model.
		 * Used to store the original originating state of a transition in
		 * the quotient model.
		 */
		private JDDVars actFromStates;

		/** Set of (s,alpha) where all successors are again in the same equivalence class */
		private JDDNode stateActionsInsideECs;

		/** The transition relation of the quotient model */
		private JDDNode newTrans;
		/** The 0/1-transition relation of the quotient model */
		private JDDNode newTrans01;
		/** Self loops on quotient representative states that are not to be dropped */
		private JDDNode ecRemainingSelfLoops = JDD.Constant(0);

		/** New transition matrix already computed? */
		private boolean computed = false;

		/** The parent prism component (for logging) */
		private PrismComponent parent;

		/** Debug: Verbose output? */
		private boolean verbose = false;

		/** Constructor */
		public MDPQuotientOperator(PrismComponent parent, NondetModel model, List<JDDNode> equivalentClasses)
		{
			super(model);

			this.equivalentClasses = equivalentClasses;
			this.parent = parent;

			map = JDD.Constant(0);
			inEC = JDD.Constant(0);
			stateActionsInsideECs = JDD.Constant(0);

			for (JDDNode ec : equivalentClasses) {
				// determine representative for the EC
				JDDNode rep = JDD.RestrictToFirst(ec.copy(), model.getAllDDRowVars());
				// map = { s in EC } -> representative'
				map = JDD.Or(map, JDD.And(ec.copy(),
						JDD.PermuteVariables(rep.copy(),
								model.getAllDDRowVars(),
								model.getAllDDColVars())));
				// remember all states in ECs
				inEC = JDD.Or(inEC, ec.copy());
				representatives = JDD.Or(representatives, rep);

				JDDNode ecCol = JDD.PermuteVariables(ec.copy(), model.getAllDDRowVars(), model.getAllDDColVars());
				JDDNode transFromEC01 = JDD.And(ec.copy(), model.getTrans01().copy());
				JDDNode selfLoop = JDD.Times(transFromEC01.copy(), ecCol.copy());
				if (verbose) JDD.PrintMinterms(parent.getLog(), selfLoop.copy(), "selfLoop");
				JDDNode stateActionWithSelfLoop = JDD.ThereExists(selfLoop, originalModel.getAllDDColVars());
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionWithSelfLoop.copy(), "stateActionWithSelfLoop");

				// from the state action pairs with self loop, find those that also go somewhere
				// else
				JDDNode stateActionElse = JDD.And(stateActionWithSelfLoop.copy(), transFromEC01.copy());
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionElse.copy(), "stateActionElse");
				stateActionElse = JDD.And(stateActionElse, JDD.Not(ecCol.copy()));
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionElse.copy(), "stateActionElse (2)");
				stateActionElse = JDD.ThereExists(stateActionElse, originalModel.getAllDDColVars());
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionElse.copy(), "stateActionElse (3)");
				JDDNode stateActionWithOnlySelfLoop = JDD.And(stateActionWithSelfLoop, JDD.Not(stateActionElse));
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionWithOnlySelfLoop.copy(), "stateActionOnlySelfLoop");

				stateActionsInsideECs = JDD.Or(stateActionsInsideECs, stateActionWithOnlySelfLoop);

				JDD.Deref(ecCol, transFromEC01);
			}

			// all states not in EC
			notInEC = JDD.And(model.getReach().copy(),
					JDD.Not(inEC.copy()));

			// map all states not in EC to themselves
			map = JDD.ITE(notInEC.copy(),
					JDD.Identity(model.getAllDDRowVars(), model.getAllDDColVars()),
					map);
		}

		@Override
		public void clear()
		{
			for (JDDNode ec : equivalentClasses) {
				JDD.Deref(ec);
			}
			JDD.Deref(map);
			JDD.Deref(inEC);
			JDD.Deref(notInEC);
			JDD.Deref(representatives);
			JDD.Deref(stateActionsInsideECs);
			JDD.Deref(ecRemainingSelfLoops);
			if (newTrans != null) JDD.Deref(newTrans);
			if (newTrans01 != null) JDD.Deref(newTrans01);
			if (actFromStates != null)
				actFromStates.derefAll();
			super.clear();
		}

		@Override
		public int getExtraStateVariableCount()
		{
			return 0;
		}

		@Override
		public int getExtraActionVariableCount()
		{
			// 1 bit (tau) for normal vs special action (leaving a representative),
			// numDDRowVar bits to remember original originating state
			// for the states leaving a representative
			return originalModel.getNumDDRowVars() + 1;
		}

		@Override
		public void hookExtraActionVariableAllocation(JDDVars extraActionVars)
		{
			// call super to store extraActionVars
			super.hookExtraActionVariableAllocation(extraActionVars);

			// initialize actFromStates
			actFromStates = new JDDVars();
			for (int i = 1; i < extraActionVars.n(); i++) {
				actFromStates.addVar(extraActionVars.getVar(i).copy());
			}
		}

		/** Get the tau action variable */
		public JDDNode getTauVar()
		{
			return extraActionVars.getVar(0).copy();
		}

		/** Get the marker for a tau action */
		public JDDNode tau()
		{
			return getTauVar();
		}

		/** Get the marker for a non-tau action (!tau & zeros for all other extra action vars) */
		public JDDNode notTau()
		{
			JDDNode notTau = JDD.Not(getTauVar());
			notTau = JDD.And(notTau, actFromStates.allZero());
			return notTau;
		}

		private void compute() throws PrismException
		{
			JDDNode trans = originalModel.getTrans().copy();

			// first run: collapse the target state ECs

			if (verbose) parent.mainLog.println("Collapsing target states");
			for (JDDNode ec : equivalentClasses) {
				if (verbose) JDD.PrintMinterms(parent.getLog(), trans.copy(), "trans");
				JDDNode ecCol = JDD.PermuteVariables(ec.copy(), originalModel.getAllDDRowVars(), originalModel.getAllDDColVars());
				if (verbose) JDD.PrintMinterms(parent.getLog(), ec.copy(), "EC");
				if (verbose) JDD.PrintMinterms(parent.getLog(), ecCol.copy(), "EC'");

				JDDNode representative = JDD.RestrictToFirst(ec.copy(), originalModel.getAllDDRowVars());
				if (verbose) JDD.PrintMinterms(parent.getLog(), representative.copy(), "rep");
				JDDNode representativeCol = JDD.PermuteVariables(representative.copy(), originalModel.getAllDDRowVars(), originalModel.getAllDDColVars());

				JDDNode transToEC = JDD.Times(trans.copy(), ecCol.copy());
				if (verbose) JDD.PrintMinterms(parent.getLog(), transToEC.copy(), "transToEC");
				transToEC = JDD.SumAbstract(transToEC, originalModel.getAllDDColVars());
				if (verbose) JDD.PrintMinterms(parent.getLog(), transToEC.copy(), "transToEC (2)");
				transToEC = JDD.Times(transToEC, representativeCol.copy());
				if (verbose) JDD.PrintMinterms(parent.getLog(), transToEC.copy(), "transToEC (3)");

				trans = JDD.ITE(ecCol, transToEC, trans);
				if (verbose) JDD.PrintMinterms(parent.getLog(), trans.copy(), "trans''");

				JDD.Deref(representative, representativeCol);
			}

			if (verbose) JDD.PrintMinterms(parent.getLog(), trans.copy(), "trans (after collapsing target states)");

			newTrans = JDD.Constant(0);
			if (verbose) parent.mainLog.println("\nCollapsing from states");
			for (JDDNode ec : equivalentClasses) {
				if (verbose) JDD.PrintMinterms(parent.getLog(), ec.copy(), "EC");

				JDDNode representative = JDD.RestrictToFirst(ec.copy(), originalModel.getAllDDRowVars());
				if (verbose) JDD.PrintMinterms(parent.getLog(), representative.copy(), "rep");
				JDDNode representativeCol = JDD.PermuteVariables(representative.copy(), originalModel.getAllDDRowVars(), originalModel.getAllDDColVars());

				JDDNode transFromEC = JDD.Times(ec.copy(), trans.copy());
				if (verbose) JDD.PrintMinterms(parent.getLog(), transFromEC.copy(), "transFromEC");

				// shift from states to actFromEC
				transFromEC = JDD.PermuteVariables(transFromEC, originalModel.getAllDDRowVars(), actFromStates);
				if (verbose) JDD.PrintMinterms(parent.getLog(), transFromEC.copy(), "transFromEC (2)");
				transFromEC = JDD.Times(tau(), representative.copy(), transFromEC);
				if (verbose) JDD.PrintMinterms(parent.getLog(), transFromEC.copy(), "transFromEC (3)");

				// remove self-loop actions back to the EC,
				// i.e. actions representative,a where all successors go back to
				// the representative
				JDDNode transFromEC01 = JDD.GreaterThan(transFromEC.copy(), 0);
				if (verbose) JDD.PrintMinterms(parent.getLog(), transFromEC01.copy(), "transFromEC01");
				JDDNode selfLoop = JDD.Times(transFromEC01.copy(), representativeCol.copy());
				if (verbose) JDD.PrintMinterms(parent.getLog(), selfLoop.copy(), "selfLoop");
				JDDNode stateActionWithSelfLoop = JDD.ThereExists(selfLoop, originalModel.getAllDDColVars());
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionWithSelfLoop.copy(), "stateActionWithSelfLoop");

				// from the state action pairs with self loop, find those that also go somewhere
				// else
				JDDNode stateActionElse = JDD.And(stateActionWithSelfLoop.copy(), transFromEC01.copy());
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionElse.copy(), "stateActionElse");
				stateActionElse = JDD.And(stateActionElse, JDD.Not(representativeCol.copy()));
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionElse.copy(), "stateActionElse (2)");
				stateActionElse = JDD.ThereExists(stateActionElse, originalModel.getAllDDColVars());
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionElse.copy(), "stateActionElse (3)");
				JDDNode stateActionWithOnlySelfLoop = JDD.And(stateActionWithSelfLoop, JDD.Not(stateActionElse));
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionWithOnlySelfLoop.copy(), "stateActionOnlySelfLoop");

				// find states where removing those actions would lead to deadlocks
				JDDNode trans01Removed = JDD.Times(transFromEC01, JDD.Not(stateActionWithOnlySelfLoop.copy()));
				if (verbose) JDD.PrintMinterms(parent.getLog(), trans01Removed.copy(), "trans01Removed");
				JDDNode notDeadlocked = JDD.ThereExists(trans01Removed, originalModel.getAllDDColVars());
				notDeadlocked = JDD.ThereExists(notDeadlocked, originalModel.getAllDDNondetVars());
				notDeadlocked = JDD.ThereExists(notDeadlocked, extraActionVars);
				if (verbose) JDD.PrintMinterms(parent.getLog(), notDeadlocked.copy(), "notDeadlocked");

				JDDNode stateActionsToRemove = JDD.And(stateActionWithOnlySelfLoop.copy(), notDeadlocked);
				if (verbose) JDD.PrintMinterms(parent.getLog(), stateActionsToRemove.copy(), "stateActionsToRemove");
				transFromEC = JDD.Times(transFromEC, JDD.Not(stateActionsToRemove.copy()));
				if (verbose) JDD.PrintMinterms(parent.getLog(), transFromEC.copy(), "transFromEC");

				JDDNode selfLoopRemaining = JDD.And(stateActionWithOnlySelfLoop, JDD.Not(stateActionsToRemove));
				ecRemainingSelfLoops = JDD.Or(ecRemainingSelfLoops, selfLoopRemaining);

				newTrans = JDD.Apply(JDD.MAX, newTrans, transFromEC);
				if (verbose) JDD.PrintMinterms(parent.getLog(), newTrans.copy(), "newTrans");

				JDD.Deref(representative, representativeCol);
			}

			JDDNode transUntouched = JDD.Times(trans.copy(), notInEC.copy());
			if (verbose) JDD.PrintMinterms(parent.getLog(), transUntouched.copy(), "transUntouched");
			transUntouched = JDD.Times(transUntouched, notTau());
			if (verbose) JDD.PrintMinterms(parent.getLog(), transUntouched.copy(), "transUntouched (2)");
			newTrans = JDD.Apply(JDD.MAX, newTrans, transUntouched);
			if (verbose) JDD.PrintMinterms(parent.getLog(), newTrans.copy(), "newTrans");

			newTrans01 = JDD.GreaterThan(newTrans.copy(), 0);

			JDD.Deref(trans);

			computed = true;
		}

		/**
		 * Maps a state set from the original model to the corresponding state set
		 * in the quotient model.
		 * [ REFS: <i>result</i>, DEREFS: s ]
		 */
		public JDDNode mapStateSet(JDDNode S)
		{
			if (verbose) JDD.PrintMinterms(parent.getLog(), S.copy(), "S");
			JDDNode mapped = JDD.And(S, map.copy());
			if (verbose) JDD.PrintMinterms(parent.getLog(), mapped.copy(), "mapped");

			mapped = JDD.ThereExists(mapped, originalModel.getAllDDRowVars());
			mapped = JDD.PermuteVariables(mapped, originalModel.getAllDDColVars(), originalModel.getAllDDRowVars());

			if (verbose) JDD.PrintMinterms(parent.getLog(), mapped.copy(), "mapped (result)");
			return mapped;
		}

		@Override
		public JDDNode getTransformedTrans() throws PrismException
		{
			if (!computed) compute();
			return newTrans.copy();
		}

		@Override
		public JDDNode getTransformedStart() throws PrismException
		{
			return mapStateSet(originalModel.getStart().copy());
		}

		@Override
		public JDDNode getTransformedStateReward(JDDNode rew) throws PrismException
		{
			if (!computed) compute();

			if (SanityJDD.enabled) {
				SanityJDD.checkIsDDOverVars(rew, originalModel.getAllDDRowVars());
			}

			if (verbose) JDD.PrintMinterms(parent.getLog(), rew.copy(), "state rew");
			// set state rewards for all EC states to zero
			JDDNode result = JDD.Times(rew.copy(), JDD.Not(inEC.copy()));
			if (verbose) JDD.PrintMinterms(parent.getLog(), result.copy(), "state rew (transformed)");

			return result;
		}

		@Override
		public JDDNode getTransformedTransReward(JDDNode rew) throws PrismException
		{
			if (!computed) compute();

			if (SanityJDD.enabled) {
				SanityJDD.checkIsDDOverVars(rew, originalModel.getAllDDRowVars(), originalModel.getAllDDNondetVars(), originalModel.getAllDDColVars());
			}

			// outgoing actions from ECs
			if (verbose) JDD.PrintMinterms(parent.getLog(), rew.copy(), "trans rew");
			JDDNode rewFromEC = JDD.Times(rew.copy(), inEC.copy());
			if (verbose) JDD.PrintMinterms(parent.getLog(), rewFromEC.copy(), "rewFromEC (1)");
			rewFromEC = JDD.PermuteVariables(rewFromEC, originalModel.getAllDDRowVars(), actFromStates);
			if (verbose) JDD.PrintMinterms(parent.getLog(), rewFromEC.copy(), "rewFromEC (2)");
			rewFromEC = JDD.Times(tau(), rewFromEC);
			if (verbose) JDD.PrintMinterms(parent.getLog(), rewFromEC.copy(), "rewFromEC (3)");
			rewFromEC = JDD.PermuteVariables(rewFromEC, originalModel.getAllDDColVars(), originalModel.getAllDDRowVars());
			if (verbose) JDD.PrintMinterms(parent.getLog(), rewFromEC.copy(), "rewFromEC (4)");
			rewFromEC = JDD.Times(rewFromEC, map.copy());
			if (verbose) JDD.PrintMinterms(parent.getLog(), rewFromEC.copy(), "rewFromEC (5)");
			rewFromEC = JDD.SumAbstract(rewFromEC, originalModel.getAllDDRowVars());
			if (verbose) JDD.PrintMinterms(parent.getLog(), rewFromEC.copy(), "rewFromEC (6)");
			rewFromEC = JDD.Times(rewFromEC, newTrans01.copy());
			if (verbose) JDD.PrintMinterms(parent.getLog(), rewFromEC.copy(), "rewFromEC (7)");

			// for the remaining self loops from ECs (to avoid deadlocks), we strip the
			// transition rewards
			if (verbose) JDD.PrintMinterms(parent.getLog(), ecRemainingSelfLoops.copy(), "ecRemainingSelfLoops");
			rewFromEC = JDD.Times(rewFromEC, JDD.Not(ecRemainingSelfLoops.copy()));
			if (verbose) JDD.PrintMinterms(parent.getLog(), rewFromEC.copy(), "rewFromEC (8)");

			// transformedRew is the combination of the outgoing actions from the ECs
			// and the original actions (tagged with notTau)
			if (verbose) JDD.PrintMinterms(parent.getLog(), rew.copy(), "trans rew");
			JDDNode transformedRew = JDD.Apply(JDD.MAX, JDD.Times(notTau(), rew.copy()), rewFromEC);
			transformedRew = JDD.Times(transformedRew, newTrans01.copy());
			if (verbose) JDD.PrintMinterms(parent.getLog(), transformedRew.copy(), "transformedRew");

			return transformedRew;
		}

		@Override
		public JDDNode getTransformedTransActions()
		{
			if (originalModel.getTransActions() == null) {
				return null;
			}

			JDDNode transActionsNormal = originalModel.getTransActions().copy();
			if (verbose) JDD.PrintMinterms(parent.getLog(), transActionsNormal.copy(), "transActionsNormal (1)");
			transActionsNormal = JDD.Times(transActionsNormal, notTau());
			if (verbose) JDD.PrintMinterms(parent.getLog(), transActionsNormal.copy(), "transActionsNormal (2)");

			JDDNode transActionsFromEC = originalModel.getTransActions().copy();
			if (verbose) JDD.PrintMinterms(parent.getLog(), transActionsFromEC.copy(), "transActionsFromEC (1)");
			// shift from states to actFromEC
			transActionsFromEC = JDD.PermuteVariables(transActionsFromEC, originalModel.getAllDDRowVars(), actFromStates);
			if (verbose) JDD.PrintMinterms(parent.getLog(), transActionsFromEC.copy(), "transActionsFromEC (2)");
			transActionsFromEC = JDD.Times(transActionsFromEC, tau());
			if (verbose) JDD.PrintMinterms(parent.getLog(), transActionsFromEC.copy(), "transActionsFromEC (3)");

			JDDNode transformedTransActions = JDD.Apply(JDD.MAX, transActionsNormal, transActionsFromEC);
			if (verbose) JDD.PrintMinterms(parent.getLog(), transformedTransActions.copy(), "transformedTransActions");

			transformedTransActions = JDD.Times(transformedTransActions, JDD.ThereExists(newTrans01.copy(), originalModel.getAllDDColVars()));

			//transformedTransActions = JDD.Times(transformedTransActions, newTrans01);
			if (verbose) JDD.PrintMinterms(parent.getLog(), transformedTransActions.copy(), "transformedTransActions");

			return transformedTransActions;
		}

		@Override
		public JDDNode getTransformedLabelStates(JDDNode oldLabelStates, JDDNode transformedReach)
		{
			// we always return 'false' here, as it's difficult to decide how to label
			// the representative states if not all the states in the EC are labelled
			// the same
			return JDD.Constant(0);
		}

		/** Provide the set of reachable states */
		public JDDNode getReachableStates()
		{
			// reachable states in the quotient MDP:
			//  remove non-representative states contained in ECs
			//  from the reachable states in the original model
			JDDNode removed = JDD.And(inEC.copy(), JDD.Not(representatives.copy()));
			JDDNode newReach = JDD.And(originalModel.getReach().copy(), JDD.Not(removed));

			return newReach;
		}

		/**
		 * Lift an ADD over row variables from the quotient model to the original model,
		 * by copying the values from the representatives to the other states in the EC.
		 */
		public JDDNode liftFromRepresentatives(JDDNode n)
		{
			// we first shift the result to the column variables
			JDDNode result = JDD.PermuteVariables(n, originalModel.getAllDDRowVars(), originalModel.getAllDDColVars());
			// combine with the map. we now have (state,representative) -> value
			result = JDD.Times(result, map.copy());
			// as there is exactly one representative per state, we can abstract using SumAbstract
			// (works for positive and negative values)
			result = JDD.SumAbstract(result, originalModel.getAllDDColVars());
			return result;
		}
	}

}
