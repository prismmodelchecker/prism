package prism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
		prism.setModelConstants(values);
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

	@Test
	@SuppressWarnings("deprecation")
	public void testPrecisionApi() throws PrismException
	{
		prism.loadPRISMModel(modulesFile);
		Values values = new Values();
		values.addValue("p", 0.1);
		prism.setModelConstants(values);
		assertEquals(PrecisionMode.FP, prism.getPrecision());
		prism.setPrecision(PrecisionMode.EXACT);
		assertEquals(PrecisionMode.EXACT, prism.getPrecision());
		assertEquals(new BigRational(1, 10), check());
		prism.setPrecision(PrecisionMode.FP);
		assertEquals(0.1, (Double) check(), 1e-10);
		assertTrue(prism.getSettings().isSpecified(PrismSettings.PRISM_EXACT_ENABLED));
		prism.unsetPrecision();
		assertFalse(prism.getSettings().isSpecified(PrismSettings.PRISM_EXACT_ENABLED));
		assertEquals(PrecisionMode.FP, prism.getPrecision());
		// Parametric: by giving parameters, not as a precision
		assertThrows(PrismException.class, () -> prism.setPrecision(PrecisionMode.PARAM));
		prism.setParameters(List.of(new DefinedConstant.Parameter("p", null, "0", "1")));
		assertEquals(PrecisionMode.PARAM, prism.getPrecision());
		prism.setParameters(null);
		// The deprecated modelCheckExact leaves the precision as it was, even after an error
		assertEquals(new BigRational(1, 10), prism.modelCheckExact(propertiesFile, propertiesFile.getPropertyObject(0)).getResult());
		assertFalse(prism.getSettings().isSpecified(PrismSettings.PRISM_EXACT_ENABLED));
		PropertiesFile bad = prism.parsePropertiesString(modulesFile, "const double q; P=? [ F s=1 ] + q;");
		assertThrows(PrismException.class, () -> prism.modelCheckExact(bad, bad.getPropertyObject(0)));
		assertEquals(PrecisionMode.FP, prism.getPrecision());
		assertFalse(prism.getSettings().isSpecified(PrismSettings.PRISM_EXACT_ENABLED));
	}

	@Test
	public void testModelConstantsForModelGenerator() throws PrismException
	{
		// Constants are passed to a model loaded as a model generator too
		// (the generator needs values before loading, to print model info)
		ModelGenerator<?> modelGen = simulator.ModulesFileModelGenerator.createForDoubles(modulesFile, prism);
		Values values = new Values();
		values.addValue("p", 0.25);
		modelGen.setSomeUndefinedConstants(values);
		prism.loadModelGenerator(modelGen);
		assertEquals(0.25, (Double) check(), 1e-10);
		values.setValue("p", 0.5);
		prism.setModelConstants(values);
		assertEquals(0.5, (Double) check(), 1e-10);
	}
}
