package parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import parser.ast.ModulesFile;
import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismLangException;

/**
 * Checks on the values that clocks can be reset to in PTA models.
 */
public class ClockResetTest
{
	private static ModulesFile parsePTA(String reset) throws PrismLangException
	{
		String model = "pta\n"
				+ "const int K;\n"
				+ "module m\n"
				+ "  s : [0..1] init 0;\n"
				+ "  x : clock;\n"
				+ "  invariant x<=4 endinvariant\n"
				+ "  [a] s=0 & x>=2 -> (s'=1) & (x'=" + reset + ");\n"
				+ "endmodule\n";
		return new Prism(new PrismDevNullLog()).parseModelString(model);
	}

	private static Values constK(int k)
	{
		Values v = new Values();
		v.addValue("K", k);
		return v;
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 5 })
	public void nonNegativeResetAllowed(int k) throws PrismLangException
	{
		ModulesFile mf = parsePTA("K");
		assertDoesNotThrow(() -> mf.setSomeUndefinedConstants(constK(k)));
	}

	@Test
	public void negativeResetViaConstantRejected() throws PrismLangException
	{
		ModulesFile mf = parsePTA("K");
		PrismLangException e = assertThrows(PrismLangException.class, () -> mf.setSomeUndefinedConstants(constK(-1)));
		assertTrue(e.getMessage().contains("negative"), e.getMessage());
	}

	@Test
	public void negativeResetViaExpressionRejected() throws PrismLangException
	{
		ModulesFile mf = parsePTA("K-3");
		assertDoesNotThrow(() -> mf.setSomeUndefinedConstants(constK(3)));
		assertThrows(PrismLangException.class, () -> mf.setSomeUndefinedConstants(constK(2)));
	}

	@Test
	public void negativeLiteralResetRejected() throws PrismLangException
	{
		// (model has an undefined constant K, so the check happens once it is defined)
		ModulesFile mf = parsePTA("-1");
		assertThrows(PrismLangException.class, () -> mf.setSomeUndefinedConstants(constK(0)));
	}

	@Test
	public void resetWithUndefinedConstantNotCheckedYet()
	{
		// K is undefined at parse time, so the reset value cannot be checked yet
		assertDoesNotThrow(() -> parsePTA("K"));
	}
}
