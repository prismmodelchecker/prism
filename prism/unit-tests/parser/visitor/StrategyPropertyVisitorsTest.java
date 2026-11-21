package parser.visitor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import parser.PrismParser;
import parser.ast.Expression;
import parser.ast.ExpressionStrategy;
import prism.Prism;
import prism.PrismLangException;

/**
 * Tests for the visitors used to check properties under a strategy:
 * {@link RemoveStrategyOptimisation} and {@link ReplaceNestedOperatorsByLabels}.
 */
public class StrategyPropertyVisitorsTest
{
	/** Parse a single property (without semantic checks, so labels/variables need not exist) */
	private static Expression parse(String s) throws PrismLangException, InterruptedException
	{
		PrismParser parser = Prism.getPrismParser();
		try {
			return parser.parsePropertiesFile(null, new ByteArrayInputStream(s.getBytes())).getProperty(0);
		} finally {
			Prism.releasePrismParser();
		}
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"Pmax=? [ F \"goal\" ]           | false | P=? [ F \"goal\" ]",
		"Pmin=? [ F \"goal\" ]           | false | P=? [ F \"goal\" ]",
		"P>=0.5 [ F \"goal\" ]           | false | P>=0.5 [ F \"goal\" ]",
		"R{\"r\"}min=? [ F \"goal\" ]    | false | R{\"r\"}=? [ F \"goal\" ]",
		"Pmaxmin=? [ F \"goal\" ]        | true  | Pmin=? [ F \"goal\" ]",
		"Pminmax=? [ F \"goal\" ]        | true  | Pmax=? [ F \"goal\" ]",
		"Pmax=? [ F \"goal\" ]           | true  | Pmax=? [ F \"goal\" ]",
		"<<p1>> Pmax=? [ F \"goal\" ]    | false | P=? [ F \"goal\" ]",
		"Pmax=? [ F Pmin=? [ X \"a\" ]>0.5 ] | false | P=? [ F P=? [ X \"a\" ]>0.5 ]",
		"<<p1>> (P>0.5 [ F \"a\" ] & R<3 [ F \"b\" ]) | false | (P>0.5 [ F \"a\" ] & R<3 [ F \"b\" ])",
	})
	public void removeStrategyOptimisation(String in, boolean uncertain, String out) throws PrismLangException, InterruptedException
	{
		Expression expr = (Expression) parse(in).accept(new RemoveStrategyOptimisation(uncertain));
		assertEquals(parse(out).toString(), expr.toString());
	}

	@Test
	public void removeStrategyOptimisationMultipleOperands() throws PrismLangException, InterruptedException
	{
		ExpressionStrategy expr = new ExpressionStrategy(true);
		expr.addOperand(parse("Pmax=? [ F \"a\" ]"));
		expr.addOperand(parse("Pmax=? [ F \"b\" ]"));
		assertThrows(PrismLangException.class, () -> expr.accept(new RemoveStrategyOptimisation(false)));
	}

	@Test
	public void replaceNestedOperators() throws PrismLangException, InterruptedException
	{
		// Outermost nested operators only, avoiding used names; top-level ones are kept
		List<String> used = Arrays.asList("_nested0");
		ReplaceNestedOperatorsByLabels replacer = new ReplaceNestedOperatorsByLabels("_nested", used::contains);
		Expression expr = (Expression) parse("Pmax=? [ P>0.5 [ F P>0.1 [ X \"a\" ] ] U R{\"r\"}<3 [ F \"b\" ] ] > 0.2 & P>0.3 [ F \"c\" ]").accept(replacer);
		assertEquals(parse("Pmax=? [ \"_nested1\" U \"_nested2\" ] > 0.2 & P>0.3 [ F \"c\" ]").toString(), expr.toString());
		Map<String, Expression> replacements = replacer.getReplacements();
		assertEquals(Arrays.asList("_nested1", "_nested2"), List.copyOf(replacements.keySet()));
		assertEquals(parse("P>0.5 [ F P>0.1 [ X \"a\" ] ]").toString(), replacements.get("_nested1").toString());
		assertEquals(parse("R{\"r\"}<3 [ F \"b\" ]").toString(), replacements.get("_nested2").toString());
	}

	@Test
	public void replaceNestedStrategyQuantifiers() throws PrismLangException, InterruptedException
	{
		// A top-level strategy quantifier is not a nesting level; a nested one is replaced
		ReplaceNestedOperatorsByLabels replacer = new ReplaceNestedOperatorsByLabels("n", name -> false);
		Expression expr = (Expression) parse("<<p1>> Pmax=? [ F <<p2>> P>0.5 [ X \"a\" ] ]").accept(replacer);
		assertEquals(parse("<<p1>> Pmax=? [ F \"n0\" ]").toString(), expr.toString());
		assertEquals(1, replacer.getReplacements().size());
	}

	@Test
	public void replaceNothingWithoutNesting() throws PrismLangException, InterruptedException
	{
		ReplaceNestedOperatorsByLabels replacer = new ReplaceNestedOperatorsByLabels("n", name -> false);
		Expression expr = (Expression) parse("Pmax=? [ F \"a\" & s=1 ]").accept(replacer);
		assertEquals(parse("Pmax=? [ F \"a\" & s=1 ]").toString(), expr.toString());
		assertTrue(replacer.getReplacements().isEmpty());
	}
}
