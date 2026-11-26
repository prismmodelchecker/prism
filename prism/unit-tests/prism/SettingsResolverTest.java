package prism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import parser.EvaluateContext.EvalMode;
import parser.PrismParser;
import parser.ast.Expression;
import parser.ast.PropertiesFile;
import prism.SettingsResolver.Engine;
import prism.SettingsResolver.Request;
import prism.SettingsResolver.ResolvedSettings;
import prism.Prism.PrismEngine;

public class SettingsResolverTest
{
	private final SettingsResolver resolver = new SettingsResolver();

	private static Expression expr(String s) throws PrismLangException, InterruptedException
	{
		// Use PRISM's shared (static) parser
		PrismParser prismParser = Prism.getPrismParser();
		try {
			PropertiesFile pf = prismParser.parsePropertiesFile(null, new ByteArrayInputStream(s.getBytes()));
			return pf.getProperty(0);
		} finally {
			Prism.releasePrismParser();
		}
	}

	private ResolvedSettings check(Request req, ModelType modelType, String prop) throws PrismException, InterruptedException
	{
		return resolver.forModelCheck(req, modelType, expr(prop), false);
	}

	@Test
	public void testDefaultsFromSettings() throws PrismException, InterruptedException
	{
		Request req = Request.fromSettings(new PrismSettings());
		assertEquals(Engine.HYBRID, req.getEngine());
		assertEquals(PrecisionMode.FP, req.getPrecision());
		ResolvedSettings res = check(req, ModelType.DTMC, "P=? [ F x=1 ]");
		assertEquals(Engine.HYBRID, res.getEngine());
		assertEquals(PrismEngine.SYMBOLIC, res.getRoute());
		assertFalse(res.isLinEqMethodOverridden());
		assertTrue(res.getNotes().isEmpty());
	}

	@Test
	public void testUnspecifiedEngineIsDefault() throws PrismException, InterruptedException
	{
		ResolvedSettings res = check(new Request(), ModelType.CTMC, "P=? [ F x=1 ]");
		assertEquals(Engine.HYBRID, res.getEngine());
		assertTrue(res.getNotes().isEmpty());
	}

	@Test
	public void testBuildSwitchesToExplicitForSomeModelTypes()
	{
		for (ModelType modelType : List.of(ModelType.IDTMC, ModelType.IMDP, ModelType.LTS, ModelType.POMDP, ModelType.IPOMDP)) {
			ResolvedSettings res = resolver.forBuild(new Request().setEngine(Engine.MTBDD), modelType);
			assertEquals(Engine.EXPLICIT, res.getEngine(), modelType.toString());
			assertEquals(1, res.getNotes().size());
			assertFalse(res.getNotes().get(0).isWarning());
			assertEquals("Switching to explicit engine, which supports " + modelType + "s...", res.getNotes().get(0).getMessage());
		}
		// No switch for other model types, or if already explicit
		assertEquals(Engine.SPARSE, resolver.forBuild(new Request().setEngine(Engine.SPARSE), ModelType.MDP).getEngine());
		assertTrue(resolver.forBuild(new Request().setEngine(Engine.EXPLICIT), ModelType.POMDP).getNotes().isEmpty());
	}

	@Test
	public void testMDPSolnMethod() throws PrismException, InterruptedException
	{
		Request req = new Request().setEngine(Engine.HYBRID).setMDPSolnMethod(Prism.MDP_POLITER);
		ResolvedSettings res = check(req, ModelType.MDP, "Pmax=? [ F x=1 ]");
		assertEquals(Engine.EXPLICIT, res.getEngine());
		assertTrue(res.isSwitchedToExplicit());
		assertEquals(1, res.getNotes().size());
		assertTrue(res.getNotes().get(0).isWarning());
		assertEquals("Switching to explicit engine to allow use of chosen MDP solution method.", res.getNotes().get(0).getMessage());
		// If the engine was not specified, the same switch is information, not a warning
		res = check(req.setEngine(Engine.HYBRID).setEngineSpecified(false), ModelType.MDP, "Pmax=? [ F x=1 ]");
		assertEquals(Engine.EXPLICIT, res.getEngine());
		assertFalse(res.getNotes().get(0).isWarning());
		// No switch for multi-objective properties
		res = check(req.setEngine(Engine.HYBRID), ModelType.MDP, "multi(Pmax=? [ F x=1 ], Pmax=? [ F x=2 ])");
		assertEquals(Engine.HYBRID, res.getEngine());
	}

	@Test
	public void testHeuristicSpeed() throws PrismException, InterruptedException
	{
		Request req = new Request().setEngine(Engine.EXPLICIT).setHeuristic("Speed");
		ResolvedSettings res = check(req, ModelType.DTMC, "P=? [ F x=1 ]");
		assertEquals(Engine.SPARSE, res.getEngine());
		assertTrue(res.isLinEqMethodOverridden());
		assertEquals("Backwards Gauss-Seidel", res.getLinEqMethod());
		// Topological VI then sees the overridden linear equation method, which the explicit engine supports
		res = check(req.setTopologicalVI(true).setLinEqMethod("SOR"), ModelType.DTMC, "P=? [ F x=1 ]");
		assertEquals(Engine.EXPLICIT, res.getEngine());
		assertEquals(2, res.getNotes().size());
	}

	@Test
	public void testTopologicalIgnored() throws PrismException, InterruptedException
	{
		Request req = new Request().setTopologicalVI(true).setLinEqMethod("SOR");
		ResolvedSettings res = check(req, ModelType.DTMC, "P=? [ F x=1 ]");
		assertEquals(Engine.HYBRID, res.getEngine());
		assertEquals(1, res.getNotes().size());
		assertTrue(res.getNotes().get(0).isWarning());
		assertEquals("Topological value iteration is not supported for linear equation method \"SOR\" and will be ignored.", res.getNotes().get(0).getMessage());
		// For MDPs, it always switches
		assertEquals(Engine.EXPLICIT, check(req, ModelType.MDP, "Pmax=? [ F x=1 ]").getEngine());
	}

	@Test
	public void testPropertyAndTaskSwitches() throws PrismException, InterruptedException
	{
		Request req = new Request().setEngine(Engine.MTBDD);
		// Non-probabilistic LTL
		assertEquals(Engine.EXPLICIT, check(req, ModelType.MDP, "A [ G F x=1 ]").getEngine());
		// Interval iteration on Rmin (but not Rmax)
		req.setIntervalIter(true);
		assertEquals(Engine.EXPLICIT, check(req, ModelType.MDP, "Rmin=? [ F x=1 ]").getEngine());
		assertEquals(Engine.MTBDD, check(req, ModelType.MDP, "Rmax=? [ F x=1 ]").getEngine());
		// Interval models: switched for model checking, so the build rule adds nothing
		ResolvedSettings res = check(new Request(), ModelType.IMDP, "Pmax=? [ F x=1 ]");
		assertEquals(Engine.EXPLICIT, res.getEngine());
		assertEquals(1, res.getNotes().size());
		assertEquals("Switching to explicit engine to allow model checking of interval model.", res.getNotes().get(0).getMessage());
		// Strategy generation
		res = resolver.forModelCheck(new Request(), ModelType.MDP, expr("Pmax=? [ F x=1 ]"), true);
		assertEquals(Engine.EXPLICIT, res.getEngine());
		// Build rules are included for model checking too
		res = check(new Request(), ModelType.POMDP, "Pmax=? [ F x=1 ]");
		assertEquals(Engine.EXPLICIT, res.getEngine());
	}

	@Test
	public void testPrecision() throws PrismException, InterruptedException
	{
		// Parameters imply parametric mode, even if exact is also requested
		Request req = new Request().setExact(true).setParameters(List.of("p"));
		assertEquals(PrecisionMode.PARAM, req.getPrecision());
		// Symbolic-engine switches don't apply for exact/parametric
		ResolvedSettings res = check(req.setMDPSolnMethod(Prism.MDP_POLITER), ModelType.MDP, "Pmax=? [ F x=1 ]");
		assertEquals(PrismEngine.PARAM, res.getRoute());
		assertTrue(res.getNotes().isEmpty());
		res = check(new Request().setExact(true), ModelType.DTMC, "P=? [ F x=1 ]");
		assertEquals(PrecisionMode.EXACT, res.getPrecision());
		assertEquals(PrismEngine.EXACT, res.getRoute());
		// Expressions are evaluated exactly for both exact and parametric
		assertEquals(EvalMode.FP, PrecisionMode.FP.getEvalMode());
		assertEquals(EvalMode.EXACT, PrecisionMode.EXACT.getEvalMode());
		assertEquals(EvalMode.EXACT, PrecisionMode.PARAM.getEvalMode());
		// Unsupported model types/settings
		assertThrows(PrismNotSupportedException.class, () -> check(new Request().setExact(true), ModelType.POMDP, "Pmax=? [ F x=1 ]"));
		assertThrows(PrismNotSupportedException.class, () -> check(new Request().setParameters(List.of("p")), ModelType.IMDP, "Pmax=? [ F x=1 ]"));
		assertThrows(PrismNotSupportedException.class, () -> check(new Request().setParameters(List.of("p")).setFairness(true), ModelType.MDP, "Pmax=? [ F x=1 ]"));
	}

	@Test
	public void testAfterBuild() throws PrismException, InterruptedException
	{
		Request req = new Request();
		ResolvedSettings res = check(req, ModelType.DTMC, "P=? [ F x=1 ]");
		// Too many states for the hybrid engine
		ResolvedSettings res2 = resolver.afterBuild(res, req, true, 1e12);
		assertEquals(Engine.MTBDD, res2.getEngine());
		assertEquals("Jacobi", res2.getLinEqMethod());
		assertTrue(res2.isLinEqMethodOverridden());
		assertEquals("Switching to MTBDD engine, as number of states is too large for Hybrid engine.", res2.getNotes().get(0).getMessage());
		// The original resolution is unchanged
		assertEquals(Engine.HYBRID, res.getEngine());
		assertTrue(res.getNotes().isEmpty());
		// Large, but not too large: only switch for heuristic=speed
		assertEquals(Engine.HYBRID, resolver.afterBuild(res, req, false, 2e8).getEngine());
		req.setHeuristic("Speed");
		res = check(req, ModelType.DTMC, "P=? [ F x=1 ]");
		assertEquals(Engine.SPARSE, res.getEngine());
		assertEquals(Engine.SPARSE, resolver.afterBuild(res, req, false, 1e6).getEngine());
		assertEquals(Engine.MTBDD, resolver.afterBuild(res, req, false, 2e8).getEngine());
		// Nothing to do for MTBDD, explicit or exact
		Request reqM = new Request().setEngine(Engine.MTBDD);
		ResolvedSettings resM = check(reqM, ModelType.DTMC, "P=? [ F x=1 ]");
		assertTrue(resolver.afterBuild(resM, reqM, true, 1e12).getNotes().isEmpty());
		Request reqX = new Request().setEngine(Engine.EXPLICIT);
		assertSame(Engine.EXPLICIT, resolver.afterBuild(check(reqX, ModelType.DTMC, "P=? [ F x=1 ]"), reqX, true, 1e12).getEngine());
		Request reqE = new Request().setExact(true);
		assertEquals(Engine.HYBRID, resolver.afterBuild(check(reqE, ModelType.DTMC, "P=? [ F x=1 ]"), reqE, true, 1e12).getEngine());
	}

	@Test
	public void testInducedModel() throws PrismException, InterruptedException
	{
		// A model induced by a strategy, stored explicitly, needs an explicit engine
		ResolvedSettings res = resolver.forBuild(new Request(), ModelType.DTMC, true);
		assertEquals(Engine.EXPLICIT, res.getEngine());
		assertTrue(res.isSwitchedToExplicit());
		assertEquals("Switching to explicit engine, since the model induced by the strategy is stored explicitly...", res.getNotes().get(0).getMessage());
		assertFalse(res.getNotes().get(0).isWarning());
		res = resolver.forModelCheck(new Request(), ModelType.DTMC, true, expr("P=? [ F x=1 ]"), false);
		assertEquals(Engine.EXPLICIT, res.getEngine());
		// No switch (or note) if explicit already
		assertTrue(resolver.forBuild(new Request().setEngine(Engine.EXPLICIT), ModelType.DTMC, true).getNotes().isEmpty());
		// No model (type) yet: no model-specific rules
		assertEquals(Engine.HYBRID, resolver.forBuild(new Request(), null).getEngine());
	}

	@Test
	public void testEngineSpecifiedFromSettings() throws PrismException
	{
		PrismSettings settings = new PrismSettings();
		assertFalse(Request.fromSettings(settings).isEngineSpecified());
		// Specified, even if it is the default
		settings.set(PrismSettings.PRISM_ENGINE, "Hybrid");
		assertTrue(Request.fromSettings(settings).isEngineSpecified());
		settings.set(PrismSettings.PRISM_ENGINE, "Sparse");
		assertEquals(Engine.SPARSE, Request.fromSettings(settings).getEngine());
		// Copies keep this
		assertTrue(Request.fromSettings(new PrismSettings(settings)).isEngineSpecified());
		// Unset (e.g. -engine auto)
		settings.unset(PrismSettings.PRISM_ENGINE);
		Request req = Request.fromSettings(settings);
		assertFalse(req.isEngineSpecified());
		assertEquals(Engine.HYBRID, req.getEngine());
		assertFalse(new PrismSettings(settings).isSpecified(PrismSettings.PRISM_ENGINE));
	}

	@Test
	public void testPrismDoesNotChangeSettings() throws PrismException
	{
		// Prism resolves the engine from the settings and model, without changing the settings
		Prism prism = new Prism(new PrismDevNullLog());
		prism.loadPRISMModel(prism.parseModelString("pomdp\nobservables o endobservables\nmodule m o : [0..1]; [] o=0 -> (o'=1); endmodule\n"));
		assertEquals(PrismEngine.EXPLICIT, prism.getCurrentEngine());
		assertEquals(Prism.EXPLICIT, prism.getResolvedSettings().getEngine().toPrismConstant());
		assertEquals(Prism.HYBRID, prism.getEngine());
		prism.loadPRISMModel(prism.parseModelString("dtmc\nmodule m s : [0..1]; [] s=0 -> (s'=1); endmodule\n"));
		assertEquals(PrismEngine.SYMBOLIC, prism.getCurrentEngine());
		assertEquals(Prism.HYBRID, prism.getResolvedSettings().getEngine().toPrismConstant());
		// Unset
		prism.setEngine(Prism.SPARSE);
		assertTrue(prism.getSettings().isSpecified(PrismSettings.PRISM_ENGINE));
		prism.unsetEngine();
		assertFalse(prism.getSettings().isSpecified(PrismSettings.PRISM_ENGINE));
		assertEquals(Prism.HYBRID, prism.getEngine());
		assertEquals(Prism.HYBRID, prism.getResolvedSettings().getEngine().toPrismConstant());
		assertEquals(Prism.JACOBI, prism.getResolvedSettings().getLinEqMethodConstant());
	}

	@Test
	public void testUnsupportedCombinations() throws PrismException, InterruptedException
	{
		// Linear equation methods not supported by the chosen engine (for DTMCs/CTMCs)
		Request req = new Request().setEngine(Engine.MTBDD).setLinEqMethod("Gauss-Seidel");
		assertThrows(PrismNotSupportedException.class, () -> check(req, ModelType.DTMC, "P=? [ F x=1 ]"));
		assertThrows(PrismNotSupportedException.class, () -> check(req.setLinEqMethod("Backwards SOR"), ModelType.CTMC, "P=? [ F x=1 ]"));
		check(req.setLinEqMethod("JOR"), ModelType.DTMC, "P=? [ F x=1 ]");
		// Not used for MDPs, so no error
		check(req.setLinEqMethod("Gauss-Seidel"), ModelType.MDP, "Pmax=? [ F x=1 ]");
		Request reqS = new Request().setEngine(Engine.SPARSE).setLinEqMethod("Pseudo-Gauss-Seidel");
		assertThrows(PrismNotSupportedException.class, () -> check(reqS, ModelType.DTMC, "P=? [ F x=1 ]"));
		// Fine for hybrid (the default)
		check(new Request().setLinEqMethod("Pseudo-Gauss-Seidel"), ModelType.DTMC, "P=? [ F x=1 ]");
		check(new Request().setLinEqMethod("Pseudo-SOR"), ModelType.DTMC, "P=? [ F x=1 ]");
		// But no error if the method is overridden (here, by heuristic=speed)
		check(reqS.setEngine(Engine.MTBDD).setHeuristic("Speed"), ModelType.DTMC, "P=? [ F x=1 ]");
		// Adversary export is not supported by the explicit engine
		Request reqA = new Request().setExportAdv(true);
		check(reqA, ModelType.MDP, "Pmax=? [ F x=1 ]");
		assertThrows(PrismNotSupportedException.class, () -> check(reqA.setEngine(Engine.EXPLICIT), ModelType.MDP, "Pmax=? [ F x=1 ]"));
		assertThrows(PrismNotSupportedException.class, () -> check(reqA.setEngine(Engine.HYBRID), ModelType.POMDP, "Pmax=? [ F x=1 ]"));
		assertThrows(PrismNotSupportedException.class, () -> check(reqA.setMDPSolnMethod(Prism.MDP_POLITER), ModelType.MDP, "Pmax=? [ F x=1 ]"));
		// The error keeps the resolution, whose notes explain the switch to explicit
		SettingsResolver.UnsupportedSettingsException e = assertThrows(SettingsResolver.UnsupportedSettingsException.class,
				() -> check(new Request().setExportAdv(true).setMDPSolnMethod(Prism.MDP_POLITER), ModelType.MDP, "Pmax=? [ F x=1 ]"));
		assertEquals(Engine.EXPLICIT, e.getResolvedSettings().getEngine());
		assertEquals(1, e.getResolvedSettings().getNotes().size());
		check(reqA.setEngine(Engine.EXPLICIT), ModelType.DTMC, "P=? [ F x=1 ]");
	}
}
