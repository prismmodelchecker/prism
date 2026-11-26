package parser;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismLangException;

/**
 * Checks that a modifier on the S operator is parsed (in both the
 * "S(m)=?" and "S(m)>p" forms), and then rejected as unsupported,
 * rather than giving a syntax error.
 */
public class SSModifierTest
{
	@ParameterizedTest
	@ValueSource(strings = { "S(m)=? [ true ]", "S(m)>0.5 [ true ]" })
	public void modifierParsedThenRejected(String prop)
	{
		Prism prism = new Prism(new PrismDevNullLog());
		PrismLangException e = assertThrows(PrismLangException.class, () -> prism.parsePropertiesString(prop));
		assertTrue(e.getMessage().contains("not supported for S operator"), e.getMessage());
	}
}
