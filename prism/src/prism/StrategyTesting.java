//==============================================================================
//
//	Copyright (c) 2026-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.util.BitSet;
import java.util.List;

import explicit.AppliedStrategyExplicit;
import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionQuant;
import parser.ast.ExpressionStrategy;
import parser.ast.ExpressionUnaryOp;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import parser.type.TypeBool;
import strat.Strategy;
import strat.StrategyExportOptions;
import strat.StrategyInfo.UndefinedReason;
import strat.UndefinedChoicePolicy;

/**
 * Strategy testing (test mode {@code -test:strat}): check a property under the strategy
 * generated when model checking it (the one that {@code -exportstrat} would export),
 * and compare the result with the one from model checking.
 * The property is checked on the model induced by the strategy, with any state subformulas
 * nested inside its operators evaluated on the original model. So, for a numerical query,
 * the strategy should achieve the optimal value; for a bound, it should be a witness if the
 * property holds and a counterexample if not. For POMDPs, where the model checking result
 * is only an approximation, the strategy's value should be no better than it.
 */
public class StrategyTesting
{
	/**
	 * Outcome of a strategy test.
	 */
	public enum Outcome {
		PASS, FAIL, UNSUPPORTED, SKIPPED
	}

	/**
	 * Result of a strategy test: an outcome and an (optional) explanation.
	 */
	public static class TestResult
	{
		public final Outcome outcome;
		public final String message;

		public TestResult(Outcome outcome, String message)
		{
			this.outcome = outcome;
			this.message = message;
		}

		@Override
		public String toString()
		{
			return outcome + (message == null || message.isEmpty() ? "" : ": " + message);
		}
	}

	/**
	 * Test the strategy generated when model checking a property, on the current model.
	 * Returns the outcome (PASS, FAIL, UNSUPPORTED, or SKIPPED if not applicable).
	 * Reachable states where the strategy is undefined, for a reason other than one
	 * meaning that the choice does not matter, are reported as a warning.
	 * @param prism The Prism object (with the model on which the property was checked)
	 * @param propertiesFile Parent property file of property (for labels/constants/...)
	 * @param prop The property
	 * @param res The result of model checking the property (with a strategy, if generated)
	 */
	public static TestResult testStrategy(Prism prism, PropertiesFile propertiesFile, Property prop, Result res)
	{
		try {
			return doTestStrategy(prism, propertiesFile, prop, res);
		} catch (PrismNotSupportedException e) {
			return new TestResult(Outcome.UNSUPPORTED, e.getMessage());
		} catch (PrismException e) {
			return new TestResult(Outcome.FAIL, e.getMessage());
		} catch (RuntimeException e) {
			return new TestResult(Outcome.FAIL, "unexpected exception: " + e);
		}
	}

	private static TestResult doTestStrategy(Prism prism, PropertiesFile propertiesFile, Property prop, Result res) throws PrismException
	{
		ModelType modelType = prism.getModelType();
		if (res.getResult() instanceof Exception) {
			return new TestResult(Outcome.SKIPPED, "no result");
		}
		if (!modelType.nondeterministic() || modelType.realTime()) {
			return new TestResult(Outcome.SKIPPED, "not applicable for " + modelType + "s");
		}
		if (modelType.multiplePlayers()) {
			return new TestResult(Outcome.UNSUPPORTED, "strategy testing not yet supported for " + modelType + "s");
		}
		// Strip any (invisible) default filter, and a "state" filter (the value for a single state)
		Expression expr = prop.getExpression();
		if (expr instanceof ExpressionFilter && ((ExpressionFilter) expr).isInvisible()) {
			expr = ((ExpressionFilter) expr).getOperand();
		}
		Expression filterState = null;
		if (expr instanceof ExpressionFilter && ((ExpressionFilter) expr).getOperatorType() == ExpressionFilter.FilterOperator.STATE) {
			filterState = ((ExpressionFilter) expr).getFilter();
			expr = ((ExpressionFilter) expr).getOperand();
		}
		// Other filters are not supported: their states would be those reachable under the strategy
		if (expr instanceof ExpressionFilter) {
			return new TestResult(Outcome.SKIPPED, "filters other than \"state\" not supported");
		}
		if (isMultiObjective(expr)) {
			return new TestResult(Outcome.SKIPPED, "multi-objective queries not yet supported");
		}
		// A single strategy is generated, so the property should have a single top-level operator
		if (!isSingleOperator(expr)) {
			return new TestResult(Outcome.SKIPPED, "only properties with a single top-level P/R/... operator supported");
		}
		Strategy<?> strat = res.getStrategy();
		if (strat == null) {
			return new TestResult(Outcome.SKIPPED, "no strategy generated");
		}
		Object resOrig = res.getResult();
		if (!(resOrig instanceof Double || resOrig instanceof Boolean)) {
			return new TestResult(Outcome.UNSUPPORTED, "results of type " + resOrig.getClass().getSimpleName() + " not supported");
		}
		boolean pomdp = modelType.partiallyObservable();
		ExpressionQuant exprQuant = getTopLevelOperator(expr);
		if (pomdp && (resOrig instanceof Boolean || exprQuant == null || !(exprQuant.getRelOp().isMin() || exprQuant.getRelOp().isMax()))) {
			return new TestResult(Outcome.SKIPPED, "only numerical min/max queries supported for " + modelType + "s");
		}

		prism.getMainLog().printSeparator();
		prism.getMainLog().println("\nStrategy test: " + prop);

		// Construct the induced model as -exportstrat would (but resolve unknown undefined choices too: see below).
		// For a "state" filter, start from that state (it may not be reachable from the initial ones under the strategy).
		StrategyExportOptions options = AppliedStrategyExplicit.defaultOptions();
		options.setUndefinedChoiceResolution(UndefinedReason.UNKNOWN, UndefinedChoicePolicy.Resolution.FIRST);
		Property propCheck = prop;
		if (filterState != null) {
			BitSet filterStates = checkStateFormula(prism, propertiesFile, filterState);
			if (filterStates.cardinality() != 1) {
				return new TestResult(Outcome.UNSUPPORTED, "filter does not identify a single state");
			}
			int s = filterStates.nextSetBit(0);
			if (strat.hasMemory() && !prism.getBuiltModelExplicit().isInitialState(s)) {
				return new TestResult(Outcome.UNSUPPORTED, "state filter for a non-initial state, for a strategy with memory");
			}
			options.setInitialStates(filterStates);
			propCheck = new Property(expr, prop.getName(), prop.getComment());
		}
		// (strategies with ambiguous choices, i.e., several choices with the same action, can't be applied,
		// nor exported, until they can be disambiguated: this is reported as not supported)
		prism.applyStrategy(strat, options);
		Result resStrat;
		try {
			warnUndefined(prism, (AppliedStrategyExplicit<?>) prism.getAppliedStrategy());
			resStrat = prism.modelCheckUnderAppliedStrategy(propertiesFile, propCheck, true);
		} finally {
			prism.unapplyStrategy();
		}
		// Repeat the model checking result, so that the last one in the log is the property's own
		prism.getMainLog().printSeparator();
		prism.getMainLog().println("\nResult: " + res.getResultAndAccuracy());

		// Compare results
		Object resUnder = resStrat.getResult();
		String strUnder = "under the strategy: " + resStrat.getResultAndAccuracy() + ", model checking: " + res.getResultAndAccuracy();
		if (resOrig instanceof Boolean) {
			if (!resOrig.equals(resUnder)) {
				return new TestResult(Outcome.FAIL, "wrong result " + strUnder);
			}
			return new TestResult(Outcome.PASS, "");
		}
		if (!(resUnder instanceof Double)) {
			return new TestResult(Outcome.FAIL, "wrong result type " + strUnder);
		}
		double vOrig = (Double) resOrig;
		double vUnder = (Double) resUnder;
		if (Double.isNaN(vOrig) || Double.isNaN(vUnder) || Double.isInfinite(vOrig) || Double.isInfinite(vUnder)) {
			if (Double.compare(vOrig, vUnder) != 0) {
				return new TestResult(Outcome.FAIL, "wrong result " + strUnder);
			}
			return new TestResult(Outcome.PASS, "");
		}
		// Compare using the results' accuracies, as for RESULT tests
		Accuracy accOrig = ResultTesting.getTestingAccuracy(res.getAccuracy());
		Accuracy accUnder = ResultTesting.getTestingAccuracy(resStrat.getAccuracy());
		double origLow = accOrig.getResultLowerBound(vOrig, true);
		double origHigh = accOrig.getResultUpperBound(vOrig, true);
		double underLow = accUnder.getResultLowerBound(vUnder, true);
		double underHigh = accUnder.getResultUpperBound(vUnder, true);
		boolean ok;
		if (pomdp) {
			// Model checking only gives a bound: the strategy should be no better than it
			ok = exprQuant.getRelOp().isMin() ? underHigh >= origLow : underLow <= origHigh;
		} else {
			ok = underLow <= origHigh && origLow <= underHigh;
		}
		if (!ok) {
			return new TestResult(Outcome.FAIL, (pomdp ? "strategy better than bound " : "wrong result ") + strUnder);
		}
		return new TestResult(Outcome.PASS, "");
	}

	/**
	 * Is this a multi-objective query (possibly inside a strategy quantifier)?
	 */
	private static boolean isMultiObjective(Expression expr)
	{
		if (expr instanceof ExpressionStrategy && ((ExpressionStrategy) expr).getNumOperands() == 1) {
			expr = ((ExpressionStrategy) expr).getOperand(0);
		}
		return expr instanceof ExpressionFunc && ((ExpressionFunc) expr).getNameCode() == ExpressionFunc.MULTI;
	}

	/**
	 * Is this property (possibly inside a strategy quantifier) a single P/R/... operator,
	 * possibly negated, or compared with (or combined with) constants? E.g., {@code Pmax=? [ F "goal" ] > 0.9},
	 * but not {@code Pmax=? [ F "goal" ] - Pmin=? [ F "goal" ]}, which the strategy could not achieve.
	 */
	private static boolean isSingleOperator(Expression expr)
	{
		if (expr instanceof ExpressionStrategy) {
			if (((ExpressionStrategy) expr).getNumOperands() != 1) {
				return false;
			}
			expr = ((ExpressionStrategy) expr).getOperand(0);
		}
		while (true) {
			if (expr instanceof ExpressionQuant) {
				return true;
			} else if (expr instanceof ExpressionUnaryOp) {
				expr = ((ExpressionUnaryOp) expr).getOperand();
			} else if (expr instanceof ExpressionBinaryOp) {
				Expression op1 = ((ExpressionBinaryOp) expr).getOperand1();
				Expression op2 = ((ExpressionBinaryOp) expr).getOperand2();
				if (op2.isConstant()) {
					expr = op1;
				} else if (op1.isConstant()) {
					expr = op2;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}

	/**
	 * Get the top-level P/R/... operator of a property (possibly inside a strategy quantifier),
	 * or null if there is none.
	 */
	private static ExpressionQuant getTopLevelOperator(Expression expr)
	{
		if (expr instanceof ExpressionStrategy && ((ExpressionStrategy) expr).getNumOperands() == 1) {
			expr = ((ExpressionStrategy) expr).getOperand(0);
		}
		return expr instanceof ExpressionQuant ? (ExpressionQuant) expr : null;
	}

	/**
	 * Evaluate a (Boolean-valued) state formula on the current model.
	 */
	private static BitSet checkStateFormula(Prism prism, PropertiesFile propertiesFile, Expression expr) throws PrismException
	{
		expr = expr.deepCopy();
		expr.typeCheck();
		if (!(expr.getType() instanceof TypeBool)) {
			throw new PrismNotSupportedException("non-Boolean filter");
		}
		explicit.StateModelChecker mc = prism.createModelCheckerExplicit(propertiesFile);
		mc.setGenStrat(false);
		return mc.checkExpression(prism.getBuiltModelExplicit(), expr, null).getBitSet();
	}

	/**
	 * Print a warning if the applied strategy is undefined in some states of its induced model
	 * (i.e., reachable under it), for a reason other than one meaning that the choice does not matter
	 * (such choices are resolved as for strategy export, or by picking the first choice if unknown).
	 */
	private static void warnUndefined(Prism prism, AppliedStrategyExplicit<?> applied)
	{
		Strategy<?> strat = applied.getStrategy();
		List<State> statesList = applied.getOriginalModel().getStatesList();
		int numUndefined = 0;
		String example = null;
		int n = applied.getInducedModel().getNumStates();
		for (int i = 0; i < n; i++) {
			int s = applied.getOriginalState(i);
			int m = applied.getMemory(i);
			if (!strat.isChoiceDefined(s, m)) {
				UndefinedReason why = strat.whyUndefined(s, m);
				if (!choiceDoesNotMatter(why) && numUndefined++ == 0) {
					example = (statesList != null ? statesList.get(s).toString() : Integer.toString(s));
					example += (m != -1 ? " (memory " + strat.getMemoryString(m) + ")" : "");
					example += " (" + (why == null ? "no reason" : why.toString().toLowerCase()) + ")";
				}
			}
		}
		if (numUndefined > 0) {
			prism.getMainLog().printWarning("Strategy undefined in " + numUndefined + " reachable state(s), e.g. " + example);
		}
	}

	/**
	 * Is an undefined strategy choice, for the given reason, one that does not matter
	 * (i.e., any choice is fine), rather than a potential gap in the strategy?
	 */
	private static boolean choiceDoesNotMatter(UndefinedReason why)
	{
		if (why == null) {
			return false;
		}
		switch (why) {
		case ARBITRARY:
		case SATISFIED:
		case UNSATISFIABLE:
		case HORIZON:
			return true;
		default:
			return false;
		}
	}
}
