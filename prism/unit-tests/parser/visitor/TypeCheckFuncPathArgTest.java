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
 * Checks that path formulas are rejected, with a clear type error,
 * as arguments to numerical functions such as min, max and floor.
 */
public class TypeCheckFuncPathArgTest
{
	@ParameterizedTest
	@ValueSource(strings = { "P=? [ min(F true, G true) ]", "P=? [ F max(G true, 1) > 0 ]", "P=? [ F floor(X true) > 0 ]", "P=? [ F pow(2, G true) > 0 ]" })
	public void pathFormulaArgumentRejected(String prop)
	{
		Prism prism = new Prism(new PrismDevNullLog());
		PrismLangException e = assertThrows(PrismLangException.class, () -> prism.parsePropertiesString(prop));
		assertTrue(e.getMessage().contains("Path formula not allowed as argument to function"), e.getMessage());
	}

	@Test
	public void stateFormulaArgumentAllowed()
	{
		Prism prism = new Prism(new PrismDevNullLog());
		assertDoesNotThrow(() -> prism.parsePropertiesString("min(P=? [ F true ], 0.5)"));
	}
}
