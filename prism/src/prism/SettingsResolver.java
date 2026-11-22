//==============================================================================
//
//	Copyright (c) 2026-
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
import java.util.Collections;
import java.util.List;

import parser.ast.Expression;
import prism.Prism.PrismEngine;

/**
 * Works out the settings that are actually used for a task (currently:
 * the engine, the precision mode and some solution method overrides),
 * from what was requested (a {@link SettingsResolver.Request})
 * and from the model and property, without changing any settings.
 * The result is a {@link SettingsResolver.ResolvedSettings}, which also lists the reasons
 * for any changes to what was requested.
 * <br><br>
 * Resolution happens in two phases: before a model is built
 * ({@link #forBuild} or {@link #forModelCheck}),
 * and, for the symbolic engines, after it is built ({@link #afterBuild}),
 * when its size is known.
 * <br><br>
 * The rules currently reproduce PRISM's existing automatic engine switching
 * (in {@link Prism}). The same rules apply whether or not the engine was specified;
 * if it was, changes are reported as warnings, and otherwise just as information.
 * <br><br>
 * Precision: if any parameters are given, the mode is {@link PrecisionMode#PARAM};
 * otherwise it is {@link PrecisionMode#EXACT} if exact mode was requested,
 * and {@link PrecisionMode#FP} if not.
 */
public class SettingsResolver
{
	/**
	 * Engines that can be requested, or be the result of resolution.
	 */
	public enum Engine
	{
		MTBDD("MTBDD", Prism.MTBDD), SPARSE("Sparse", Prism.SPARSE), HYBRID("Hybrid", Prism.HYBRID), EXPLICIT("Explicit", Prism.EXPLICIT);

		private final String name;
		private final int prismConstant;

		Engine(String name, int prismConstant)
		{
			this.name = name;
			this.prismConstant = prismConstant;
		}

		/**
		 * Get the name of this engine, as used in settings and log output (e.g. "Hybrid").
		 */
		public String getName()
		{
			return name;
		}

		/**
		 * Get the corresponding engine constant in {@link Prism} (e.g. {@link Prism#HYBRID}).
		 */
		public int toPrismConstant()
		{
			return prismConstant;
		}

		/**
		 * Is this one of the symbolic engines (MTBDD, sparse, hybrid)?
		 */
		public boolean isSymbolic()
		{
			return this == MTBDD || this == SPARSE || this == HYBRID;
		}

		/**
		 * Get the engine for an engine constant in {@link Prism} (e.g. {@link Prism#HYBRID}).
		 */
		public static Engine fromPrismConstant(int e)
		{
			for (Engine engine : values()) {
				if (engine.prismConstant == e) {
					return engine;
				}
			}
			throw new IllegalArgumentException("Unknown engine " + e);
		}
	}

	/** State space size above which heuristic=speed switches symbolic engines to MTBDD */
	public static final double MTBDD_STATES_THRESHOLD = Prism.MTBDD_STATES_THRESHOLD;

	/**
	 * What was requested: the engine, the precision and the settings that affect engine choice.
	 */
	public static class Request
	{
		private Engine engine = Engine.HYBRID;
		private boolean engineSpecified = false;
		private boolean exact = false;
		private List<String> parameters = Collections.emptyList();
		private String heuristic = "None";
		private int mdpSolnMethod = Prism.MDP_VALITER;
		private boolean topologicalVI = false;
		private String linEqMethod = "Jacobi";
		private boolean intervalIter = false;
		private boolean fairness = false;

		/**
		 * Create a request with the settings in {@code settings}.
		 * Parameters are not settings, so the list of parameters is empty: see {@link #setParameters}.
		 */
		public static Request fromSettings(PrismSettings settings)
		{
			Request req = new Request();
			req.engine = Engine.fromPrismConstant(settings.getChoice(PrismSettings.PRISM_ENGINE));
			// (PrismSettings does not record whether a setting was specified, so assume it was)
			req.engineSpecified = true;
			req.exact = settings.getBoolean(PrismSettings.PRISM_EXACT_ENABLED);
			req.heuristic = settings.getString(PrismSettings.PRISM_HEURISTIC);
			req.mdpSolnMethod = settings.getChoice(PrismSettings.PRISM_MDP_SOLN_METHOD);
			req.topologicalVI = settings.getBoolean(PrismSettings.PRISM_TOPOLOGICAL_VI);
			req.linEqMethod = settings.getString(PrismSettings.PRISM_LIN_EQ_METHOD);
			req.intervalIter = settings.getBoolean(PrismSettings.PRISM_INTERVAL_ITER);
			req.fairness = settings.getBoolean(PrismSettings.PRISM_FAIRNESS);
			return req;
		}

		/**
		 * Set the engine, as specified (switches to another engine are then reported as warnings).
		 */
		public Request setEngine(Engine engine)
		{
			this.engine = engine;
			this.engineSpecified = true;
			return this;
		}

		/**
		 * Set whether the engine was specified, rather than left at its default
		 * (if not, switches to another engine are reported just as information).
		 */
		public Request setEngineSpecified(boolean engineSpecified)
		{
			this.engineSpecified = engineSpecified;
			return this;
		}

		public Request setExact(boolean exact)
		{
			this.exact = exact;
			return this;
		}

		/**
		 * Set the names of the constants that are parameters (none means non-parametric).
		 */
		public Request setParameters(List<String> parameters)
		{
			this.parameters = parameters == null ? Collections.emptyList() : new ArrayList<>(parameters);
			return this;
		}

		/**
		 * Set the heuristic mode ("None", "Speed" or "Memory", as for {@link PrismSettings#PRISM_HEURISTIC}).
		 */
		public Request setHeuristic(String heuristic)
		{
			this.heuristic = heuristic;
			return this;
		}

		/**
		 * Set the MDP solution method (e.g. {@link Prism#MDP_VALITER}).
		 */
		public Request setMDPSolnMethod(int mdpSolnMethod)
		{
			this.mdpSolnMethod = mdpSolnMethod;
			return this;
		}

		public Request setTopologicalVI(boolean topologicalVI)
		{
			this.topologicalVI = topologicalVI;
			return this;
		}

		/**
		 * Set the linear equation method (as for {@link PrismSettings#PRISM_LIN_EQ_METHOD}, e.g. "Jacobi").
		 */
		public Request setLinEqMethod(String linEqMethod)
		{
			this.linEqMethod = linEqMethod;
			return this;
		}

		public Request setIntervalIter(boolean intervalIter)
		{
			this.intervalIter = intervalIter;
			return this;
		}

		public Request setFairness(boolean fairness)
		{
			this.fairness = fairness;
			return this;
		}

		public Engine getEngine()
		{
			return engine;
		}

		/**
		 * Was the engine specified, rather than left at its default?
		 */
		public boolean isEngineSpecified()
		{
			return engineSpecified;
		}

		public List<String> getParameters()
		{
			return Collections.unmodifiableList(parameters);
		}

		/**
		 * Get the precision mode for this request: parametric if there are any parameters,
		 * otherwise exact if requested, otherwise floating-point.
		 */
		public PrecisionMode getPrecision()
		{
			if (!parameters.isEmpty()) {
				return PrecisionMode.PARAM;
			}
			return exact ? PrecisionMode.EXACT : PrecisionMode.FP;
		}
	}

	/**
	 * A message explaining a change made during resolution (or a requested setting that is ignored).
	 */
	public static class Note
	{
		private final boolean warning;
		private final String message;

		public Note(boolean warning, String message)
		{
			this.warning = warning;
			this.message = message;
		}

		/** Should this be shown as a warning (rather than information)? */
		public boolean isWarning()
		{
			return warning;
		}

		public String getMessage()
		{
			return message;
		}

		@Override
		public String toString()
		{
			return (warning ? "Warning: " : "") + message;
		}
	}

	/**
	 * The settings to use for a task, as resolved from a {@link Request}:
	 * the engine and precision mode, any solution method overrides,
	 * and the reasons for changes.
	 */
	public static class ResolvedSettings
	{
		private Engine engine;
		private PrecisionMode precision;
		private String linEqMethod;
		private boolean linEqMethodOverridden = false;
		private boolean switchedToExplicit = false;
		private final List<Note> notes = new ArrayList<>();

		private ResolvedSettings(Engine engine, PrecisionMode precision, String linEqMethod)
		{
			this.engine = engine;
			this.precision = precision;
			this.linEqMethod = linEqMethod;
		}

		private ResolvedSettings(ResolvedSettings other)
		{
			engine = other.engine;
			precision = other.precision;
			linEqMethod = other.linEqMethod;
			linEqMethodOverridden = other.linEqMethodOverridden;
			switchedToExplicit = other.switchedToExplicit;
			notes.addAll(other.notes);
		}

		/**
		 * Get the engine to use.
		 * For the exact and parametric modes, this is the engine setting that would be in effect,
		 * but computation goes via the exact/parametric engine: see {@link #getRoute()}.
		 */
		public Engine getEngine()
		{
			return engine;
		}

		public PrecisionMode getPrecision()
		{
			return precision;
		}

		/**
		 * Get the overall way that computation is done, as for {@link Prism#getCurrentEngine()}.
		 */
		public PrismEngine getRoute()
		{
			switch (precision) {
			case PARAM:
				return PrismEngine.PARAM;
			case EXACT:
				return PrismEngine.EXACT;
			default:
				return engine == Engine.EXPLICIT ? PrismEngine.EXPLICIT : PrismEngine.SYMBOLIC;
			}
		}

		/**
		 * Get the linear equation method to use (the requested one, unless overridden).
		 */
		public String getLinEqMethod()
		{
			return linEqMethod;
		}

		/**
		 * Was the requested linear equation method overridden?
		 */
		public boolean isLinEqMethodOverridden()
		{
			return linEqMethodOverridden;
		}

		/**
		 * Was the engine switched to explicit, to support the model, property or settings?
		 */
		public boolean isSwitchedToExplicit()
		{
			return switchedToExplicit;
		}

		/**
		 * Get the reasons for any changes, in the order they were made.
		 */
		public List<Note> getNotes()
		{
			return Collections.unmodifiableList(notes);
		}

		private boolean isSymbolicRoute()
		{
			return getRoute() == PrismEngine.SYMBOLIC;
		}

		private void overrideLinEqMethod(String method)
		{
			linEqMethod = method;
			linEqMethodOverridden = true;
		}
	}

	/**
	 * Resolve the engine to build a model of type {@code modelType},
	 * e.g. for a call to {@link Prism#buildModel()}.
	 */
	public ResolvedSettings forBuild(Request req, ModelType modelType)
	{
		ResolvedSettings res = initialResolution(req);
		applyBuildRules(res, modelType);
		return res;
	}

	/**
	 * Resolve the engine to model check property {@code expr} on a model of type {@code modelType},
	 * before the model is built. The rules for building ({@link #forBuild}) are included.
	 * Real-time models (PTAs etc.) and fast adaptive uniformisation are handled separately
	 * by {@link Prism}, so are not considered here.
	 * @param req What was requested
	 * @param modelType The model type
	 * @param expr The property
	 * @param genStrat Whether a strategy is to be generated
	 */
	public ResolvedSettings forModelCheck(Request req, ModelType modelType, Expression expr, boolean genStrat) throws PrismNotSupportedException
	{
		ResolvedSettings res = initialResolution(req);
		boolean warn = req.isEngineSpecified();
		// Exact/parametric model checking only supported for some models/settings
		boolean exactParamModel = modelType == ModelType.DTMC || modelType == ModelType.CTMC || modelType == ModelType.MDP || !modelType.isProbabilistic();
		if (res.precision == PrecisionMode.EXACT && !exactParamModel) {
			throw new PrismNotSupportedException("Exact model checking not supported for " + modelType + "s");
		}
		if (res.precision == PrecisionMode.PARAM) {
			if (!exactParamModel) {
				throw new PrismNotSupportedException("Parametric model checking not supported for " + modelType + "s");
			}
			if (modelType == ModelType.MDP && req.fairness) {
				throw new PrismNotSupportedException("Parametric model checking does not support checking MDPs under fairness");
			}
		}
		// Heuristic choices of engine/method
		if ("Speed".equals(req.heuristic)) {
			res.notes.add(new Note(warn, "Switching to sparse engine and (backwards) Gauss Seidel (default for heuristic=speed)."));
			res.engine = Engine.SPARSE;
			res.overrideLinEqMethod("Backwards Gauss-Seidel");
		}
		// Switches to the explicit engine, if required
		boolean multiObj = Expression.containsMultiObjective(expr);
		if (modelType == ModelType.MDP && !multiObj) {
			if (req.mdpSolnMethod != Prism.MDP_VALITER && res.isSymbolicRoute()) {
				switchToExplicit(res, warn, "Switching to explicit engine to allow use of chosen MDP solution method.");
			}
		}
		if (req.topologicalVI && res.isSymbolicRoute()) {
			if (modelType == ModelType.MDP && !multiObj) {
				switchToExplicit(res, warn, "Switching to explicit engine to allow topological value iteration.");
			} else if (modelType == ModelType.DTMC || modelType == ModelType.CTMC) {
				// For DTMCs/CTMCs, the explicit engine only supports some linear equation methods
				String linEqMethod = res.linEqMethod;
				if (linEqMethod.equals("Power") || linEqMethod.equals("Jacobi") || linEqMethod.equals("Gauss-Seidel") || linEqMethod.equals("Backwards Gauss-Seidel")) {
					switchToExplicit(res, warn, "Switching to explicit engine to allow topological value iteration.");
				} else {
					res.notes.add(new Note(true, "Topological value iteration is not supported for linear equation method \"" + linEqMethod + "\" and will be ignored."));
				}
			}
		}
		if (Expression.containsNonProbLTLFormula(expr) && res.isSymbolicRoute()) {
			switchToExplicit(res, warn, "Switching to explicit engine to allow non-probabilistic LTL model checking.");
		}
		if (req.intervalIter && res.isSymbolicRoute()) {
			if (modelType == ModelType.MDP && Expression.containsMinReward(expr)) {
				switchToExplicit(res, warn, "Switching to explicit engine to allow interval iteration on Rmin operator.");
			}
		}
		if ((modelType == ModelType.IDTMC || modelType == ModelType.IMDP) && res.isSymbolicRoute()) {
			switchToExplicit(res, warn, "Switching to explicit engine to allow model checking of interval model.");
		}
		if (genStrat && res.isSymbolicRoute()) {
			switchToExplicit(res, warn, "Switching to explicit engine to allow strategy generation.");
		}
		// Then the rules for building the model
		applyBuildRules(res, modelType);
		return res;
	}

	/**
	 * Revise a resolution once the model has been built (symbolically), using its size.
	 * Only does anything if the resolution is for a symbolic engine other than MTBDD.
	 * @param res The resolution from before the model was built (not modified)
	 * @param req What was requested
	 * @param numStatesExceedsInt Whether the number of states is too large for an int
	 * @param numStates The number of states
	 */
	public ResolvedSettings afterBuild(ResolvedSettings res, Request req, boolean numStatesExceedsInt, double numStates)
	{
		ResolvedSettings res2 = new ResolvedSettings(res);
		if (!res2.isSymbolicRoute() || res2.engine == Engine.MTBDD || res2.switchedToExplicit) {
			return res2;
		}
		boolean warn = req.isEngineSpecified();
		boolean switchToMTBDD = false;
		// Either because number of states is too big for double-valued solution vectors
		if (numStatesExceedsInt) {
			res2.notes.add(new Note(warn, "Switching to MTBDD engine, as number of states is too large for " + res2.engine.getName() + " engine."));
			switchToMTBDD = true;
		}
		// Or based on heuristic choices of engine/method
		// (sparse/hybrid typically very slow if need to work with huge state spaces)
		else if ("Speed".equals(req.heuristic) && numStates > MTBDD_STATES_THRESHOLD) {
			res2.notes.add(new Note(warn, "Switching to MTBDD engine (default for heuristic=speed and this state space size)."));
			switchToMTBDD = true;
		}
		// NB: Need to make sure solution methods supported for MTBDDs are used
		if (switchToMTBDD) {
			res2.engine = Engine.MTBDD;
			res2.overrideLinEqMethod("Jacobi");
		}
		return res2;
	}

	// Private methods

	private ResolvedSettings initialResolution(Request req)
	{
		Engine engine = req.getEngine();
		return new ResolvedSettings(engine, req.getPrecision(), req.linEqMethod);
	}

	private void applyBuildRules(ResolvedSettings res, ModelType modelType)
	{
		switch (modelType) {
		case IDTMC:
		case IMDP:
		case LTS:
		case POMDP:
		case IPOMDP:
			if (res.isSymbolicRoute()) {
				res.notes.add(new Note(false, "Switching to explicit engine, which supports " + modelType + "s..."));
				res.engine = Engine.EXPLICIT;
				res.switchedToExplicit = true;
			}
			break;
		default:
			break;
		}
	}

	private void switchToExplicit(ResolvedSettings res, boolean warn, String message)
	{
		res.notes.add(new Note(warn, message));
		res.engine = Engine.EXPLICIT;
		res.switchedToExplicit = true;
	}
}
