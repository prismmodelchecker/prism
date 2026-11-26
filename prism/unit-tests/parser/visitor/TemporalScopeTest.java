package parser.visitor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismLangException;

/**
 * Checks that unbracketed unary temporal operators (X, F, G) used as the operand
 * of a non-temporal operator are rejected where their scope is ambiguous
 * (e.g. "F a & G b", which LTL tools usually read as "(F a) & (G b)"),
 * and allowed otherwise.
 */
public class TemporalScopeTest
{
	private static final String CONSTS = "const bool a; const bool b; const bool c; ";

	@ParameterizedTest
	@ValueSource(strings = {
			// Operand with a logical connective (or ? :) at the top
			"P=? [ a & F b & c ]", "P=? [ !F a & b ]", "P=? [ a & F b => c ]", "P=? [ a | X F b & c ]", "P=? [ a & F b ? c : a ]",
			// Within the unbracketed operand of another temporal operator
			"P=? [ F a & G b ]", "P=? [ G F a & G F b ]", "P=? [ G F a => G F b ]", "P=? [ F a | X b ]", "P=? [ F<=3 a & G b ]",
			"P=? [ a & F b U c ]", "P=? [ a U b & F c ]", "P=? [ F a U b & G c ]" })
	public void ambiguousScopeRejected(String prop)
	{
		Prism prism = new Prism(new PrismDevNullLog());
		PrismLangException e = assertThrows(PrismLangException.class, () -> prism.parsePropertiesString(CONSTS + prop));
		assertTrue(e.getMessage().contains("Ambiguous scope of temporal operator"), e.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"P=? [ a & F b ]", "P=? [ !F a ]", "P=? [ a & G F b ]", "P=? [ a & F (b & c) ]", "P=? [ (a & F b) & c ]", "true = !false",
			// Accepted before unary temporal operators could be operands of non-temporal ones
			"P=? [ F a & b ]", "P=? [ G F a ]", "P=? [ (F a) & (G b) ]", "P=? [ F a & (G b) ]", "P=? [ F (a & G b) ]", "P=? [ F a U b ]",
			// Brackets or another operator (P) delimit the scope
			"P=? [ F a & (b | G c) ]", "P=? [ F !(a & G b) ]", "P=? [ a & P>0 [ F b & c ] ]" })
	public void unambiguousScopeAllowed(String prop)
	{
		Prism prism = new Prism(new PrismDevNullLog());
		assertDoesNotThrow(() -> prism.parsePropertiesString(CONSTS + prop));
	}

	@Test
	public void errorSuggestsBrackets()
	{
		Prism prism = new Prism(new PrismDevNullLog());
		PrismLangException e = assertThrows(PrismLangException.class, () -> prism.parsePropertiesString(CONSTS + "P=? [ G F a & G F b ]"));
		assertTrue(e.getMessage().contains("\"(G F a)&(G F b)\" or \"G F (a&(G F b))\""), e.getMessage());
		e = assertThrows(PrismLangException.class, () -> prism.parsePropertiesString(CONSTS + "P=? [ a & F b & c ]"));
		assertTrue(e.getMessage().contains("\"(F b)&c\" or \"F (b&c)\""), e.getMessage());
	}
}
