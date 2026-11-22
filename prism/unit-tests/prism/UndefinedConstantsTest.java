package prism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import param.BigRational;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;

public class UndefinedConstantsTest
{
	private static final String MODEL = "dtmc\n"
			+ "const double p;\n"
			+ "const double q;\n"
			+ "const int N;\n"
			+ "module m s : [0..N]; [] s=0 -> p:(s'=1) + (1-p):true; endmodule\n";

	private static final String PROPS = "const double r;\n"
			+ "P>=r [ F s=1 ];\n";

	private ModulesFile modulesFile;
	private PropertiesFile propertiesFile;

	@BeforeEach
	public void setUp() throws PrismException
	{
		Prism prism = new Prism(new PrismDevNullLog());
		modulesFile = prism.parseModelString(MODEL);
		propertiesFile = prism.parsePropertiesString(modulesFile, PROPS);
	}

	private UndefinedConstants create()
	{
		return new UndefinedConstants(modulesFile, propertiesFile);
	}

	@Test
	public void testValuesAndRanges() throws PrismException
	{
		UndefinedConstants uc = create();
		uc.defineUsingConstSwitch("p=0.5,q=0:0.25:0.5,N=3,r=0.1");
		assertEquals(3, uc.getNumModelIterations());
		assertEquals(1, uc.getNumPropertyIterations());
		assertEquals(0.5, uc.getMFConstantValues().getValueOf("p"));
		assertEquals(0.0, uc.getMFConstantValues().getValueOf("q"));
		uc.iterateModel();
		assertEquals(0.25, uc.getMFConstantValues().getValueOf("q"));
		assertEquals(0.1, uc.getPFConstantValues().getValueOf("r"));
		// Missing definitions
		assertThrows(PrismException.class, () -> create().defineUsingConstSwitch("p=0.5,q=1"));
	}

	@Test
	public void testExactModeChangedAfterDefining() throws PrismException
	{
		UndefinedConstants uc = create();
		uc.defineUsingConstSwitch("p=1/3,q=0:1/4:1/2,N=3,r=0.1");
		assertEquals(1.0 / 3.0, uc.getMFConstantValues().getValueOf("p"));
		// Switch to exact mode: definitions are redone from their strings, iterators restarted
		uc.setExactMode(true);
		assertTrue(uc.getExactMode());
		assertEquals(new BigRational(1, 3), uc.getMFConstantValues().getValueOf("p"));
		assertEquals(BigRational.ZERO, uc.getMFConstantValues().getValueOf("q"));
		uc.iterateModel();
		assertEquals(new BigRational(1, 4), uc.getMFConstantValues().getValueOf("q"));
		assertEquals(new BigRational(1, 10), uc.getPFConstantValues().getValueOf("r"));
		assertEquals(3, uc.getMFConstantValues().getValueOf("N"));
		// And back again
		uc.setExactMode(false);
		assertEquals(1.0 / 3.0, uc.getMFConstantValues().getValueOf("p"));
		// Definitions made one at a time (as in the GUI) are remembered too
		UndefinedConstants uc2 = create();
		uc2.defineConstant("p", "1/3");
		uc2.setExactMode(true);
		uc2.defineConstant("q", "0");
		uc2.defineConstant("N", "1");
		uc2.defineConstant("r", "0");
		uc2.checkAllDefined();
		uc2.initialiseIterators();
		assertEquals(new BigRational(1, 3), uc2.getMFConstantValues().getValueOf("p"));
		// A failed switch (1d is a double but not a rational) leaves everything as it was
		UndefinedConstants uc3 = create();
		uc3.defineUsingConstSwitch("p=1/3,q=1d,N=3,r=0.1");
		assertThrows(PrismException.class, () -> uc3.setExactMode(true));
		assertFalse(uc3.getExactMode());
		assertEquals(1.0 / 3.0, uc3.getMFConstantValues().getValueOf("p"));
		assertEquals(1.0, uc3.getMFConstantValues().getValueOf("q"));
		assertThrows(PrismException.class, () -> uc3.setExactMode(true));
	}

	@Test
	public void testParameters() throws PrismException
	{
		UndefinedConstants uc = create();
		uc.defineUsingParamSwitch("p,r=0:1/2", true);
		assertTrue(uc.isParameter("p"));
		assertTrue(uc.isParameter("r"));
		assertFalse(uc.isParameter("q"));
		List<DefinedConstant.Parameter> params = uc.getParameters();
		assertEquals(2, params.size());
		assertEquals("p", params.get(0).getName());
		assertEquals("0", params.get(0).getLower());
		assertEquals("1", params.get(0).getUpper());
		assertEquals("1/2", params.get(1).getUpper());
		// Parameters are no longer undefined constants needing values
		assertEquals(2, uc.getMFNumUndefined());
		assertEquals(0, uc.getPFNumUndefined());
		uc.defineUsingConstSwitch("q=0.5,N=3");
		assertFalse(uc.getMFConstantValues().contains("p"));
		// Parameters survive clearing definitions and changing exact mode
		uc.clearAllDefinitions();
		uc.setExactMode(true);
		assertEquals(2, uc.getParameters().size());
	}

	@Test
	public void testParameterErrors() throws PrismException
	{
		// Not an undefined constant (only an error if all must be used)
		assertThrows(PrismException.class, () -> create().defineUsingParamSwitch("x", true));
		UndefinedConstants uc = create();
		uc.defineUsingParamSwitch("x", false);
		assertTrue(uc.getParameters().isEmpty());
		// Duplicates (found when parsing, whether or not the constant is needed)
		assertThrows(PrismException.class, () -> create().defineUsingParamSwitch("p,p", true));
		assertThrows(PrismException.class, () -> UndefinedConstants.parseParamSwitch("x,y=0:1,x"));
		// Bad range
		assertThrows(PrismException.class, () -> UndefinedConstants.parseParamSwitch("p=0"));
		// A parameter cannot also be given a value...
		UndefinedConstants uc2 = create();
		uc2.defineUsingParamSwitch("p", true);
		assertThrows(PrismException.class, () -> uc2.defineUsingConstSwitch("p=0.5,q=0.5,N=3,r=0"));
		// ...or vice versa
		UndefinedConstants uc3 = create();
		uc3.defineConstant("p", "0.5");
		assertThrows(PrismException.class, () -> uc3.defineUsingParamSwitch("p", true));
		// ...checked across the -param and -const switches, whether or not the constant is needed
		assertThrows(PrismException.class, () -> UndefinedConstants.checkParamSwitchAgainstConstSwitch("p,x=0:1", "N=1,x=0.3"));
		UndefinedConstants.checkParamSwitchAgainstConstSwitch("p,x=0:1", "N=1,y=0.3");
		UndefinedConstants.checkParamSwitchAgainstConstSwitch("p", null);
	}
}
