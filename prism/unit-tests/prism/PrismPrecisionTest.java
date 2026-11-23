package prism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import param.BigRational;
import parser.Values;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;

/**
 * Changing the precision (floating-point, exact, parametric) and parameters
 * between model checking calls on the same Prism object
 * (explicit engine only, so no native code is needed).
 */
public class PrismPrecisionTest
{
	private static final String MODEL = "dtmc\n"
			+ "const double p;\n"
			+ "module m s : [0..2] init 0; [] s=0 -> p:(s'=1) + (1-p):(s'=2); [] s>0 -> true; endmodule\n";

	private Prism prism;
	private ModulesFile modulesFile;
	private PropertiesFile propertiesFile;

	@BeforeEach
	public void setUp() throws PrismException
	{
		prism = new Prism(new PrismDevNullLog());
		prism.setEngine(Prism.EXPLICIT);
		modulesFile = prism.parseModelString(MODEL);
		propertiesFile = prism.parsePropertiesString(modulesFile, "P=? [ F s=1 ]");
	}

	private Object check() throws PrismException
	{
		return prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(0)).getResult();
	}

	private void setExact(boolean exact) throws PrismException
	{
		prism.getSettings().set(PrismSettings.PRISM_EXACT_ENABLED, exact);
	}

	@Test
	public void testSwitchingPrecision() throws PrismException
	{
		prism.loadPRISMModel(modulesFile);
		Values values = new Values();
		values.addValue("p", 0.1);
		prism.setPRISMModelConstants(values);
		assertEquals(0.1, (Double) check(), 1e-10);
		// The constant is re-evaluated (exactly) and the model generator recreated
		setExact(true);
		assertEquals(new BigRational(1, 10), check());
		// Parameters take precedence over the value given for p
		setExact(false);
		prism.setParameters(List.of(new DefinedConstant.Parameter("p", null, "0", "1")));
		assertTrue(check().toString().endsWith(": p"));
		// Changing the parameters is taken into account too
		prism.setParameters(List.of(new DefinedConstant.Parameter("p", null, "0", "1/2")));
		assertTrue(prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(0)).getResultString().contains("0.5"));
		// And back again
		prism.setParameters(null);
		assertTrue(prism.getParameters().isEmpty());
		assertEquals(0.1, (Double) check(), 1e-10);
		setExact(true);
		assertEquals(new BigRational(1, 10), check());
	}

	@Test
	public void testConstantsSetInModulesFile() throws PrismException
	{
		// Constants set directly in the ModulesFile before loading it are kept
		Values values = new Values();
		values.addValue("p", 0.25);
		modulesFile.setSomeUndefinedConstants(values);
		prism.loadPRISMModel(modulesFile);
		assertEquals(0.25, (Double) check(), 1e-10);
		setExact(true);
		assertEquals(new BigRational(1, 4), check());
	}
}
