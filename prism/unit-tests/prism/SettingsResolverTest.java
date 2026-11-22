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
}
