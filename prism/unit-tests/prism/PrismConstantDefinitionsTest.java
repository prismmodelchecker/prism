package prism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import param.BigRational;
import parser.Values;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;

/**
 * Definitions of undefined constants held by Prism ({@link Prism#defineConstant(String, String)} etc.)
 * (explicit engine only, so no native code is needed).
 */
public class PrismConstantDefinitionsTest
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
		propertiesFile = prism.parsePropertiesString(modulesFile, "const double q; P=? [ F s=1 ]; P=? [ F s=1 ] + q;");
		prism.loadPRISMModel(modulesFile);
	}

	private Object check(int i) throws PrismException
	{
		return prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(i)).getResult();
	}

	private void setExact(boolean exact) throws PrismException
	{
		prism.getSettings().set(PrismSettings.PRISM_EXACT_ENABLED, exact);
	}

	@Test
	public void testModelConstantInEachPrecision() throws PrismException
	{
		prism.defineConstant("p", "1/3");
		assertEquals(1.0 / 3, (Double) check(0), 1e-10);
		// Evaluated from the string, so exactly 1/3 (not the nearest double)
		setExact(true);
		assertEquals(new BigRational(1, 3), check(0));
		setExact(false);
		assertEquals(1.0 / 3, (Double) check(0), 1e-10);
		// Shown as the model's constant values
		assertEquals(1.0 / 3, (Double) prism.getUndefinedModelValues().getValueOf("p"), 1e-10);
	}

	@Test
	public void testChangingDefinitions() throws PrismException
	{
		prism.defineConstant("p", "0.1");
		assertEquals(0.1, (Double) check(0), 1e-10);
		prism.defineConstant("p", "0.2");
		assertEquals(0.2, (Double) check(0), 1e-10);
		prism.defineConstants("p=0.3");
		assertEquals(0.3, (Double) check(0), 1e-10);
		// Without a definition, the value given in another way is used
		prism.clearConstantDefinitions();
		Values values = new Values();
		values.addValue("p", 0.4);
		prism.setModelConstants(values);
		assertEquals(0.4, (Double) check(0), 1e-10);
		// But a definition takes precedence
		prism.defineConstant("p", "0.5");
		assertEquals(0.5, (Double) check(0), 1e-10);
	}

	@Test
	public void testPropertyConstants() throws PrismException
	{
		prism.defineConstants("p=1/2,q=1/3");
		assertEquals(0.5 + 1.0 / 3, (Double) check(1), 1e-10);
		setExact(true);
		assertEquals(new BigRational(5, 6), check(1));
		// Values set in the properties file are replaced by definitions
		Values values = new Values();
		values.addValue("q", new BigRational(1, 4));
		propertiesFile.setSomeUndefinedConstants(values, true);
		assertEquals(new BigRational(5, 6), check(1));
		// but used for constants without a definition
		prism.clearConstantDefinitions();
		prism.defineConstant("p", "1/2");
		propertiesFile.setSomeUndefinedConstants(values, true);
		assertEquals(new BigRational(3, 4), check(1));
	}

	@Test
	public void testPropertiesFileNotChanged() throws PrismException
	{
		prism.defineConstant("p", "1/2");
		Values values = new Values();
		values.addValue("q", 0.9);
		propertiesFile.setSomeUndefinedConstants(values, false);
		// A definition is used for checking...
		prism.defineConstant("q", "0.05");
		assertEquals(0.55, (Double) check(1), 1e-10);
		// ...but the properties file keeps its own value
		assertEquals(0.9, (Double) propertiesFile.getConstantValues().getValueOf("q"), 1e-10);
		// which is used again once the definition is removed
		prism.clearConstantDefinitions();
		prism.defineConstant("p", "1/2");
		assertEquals(1.4, (Double) check(1), 1e-10);
	}

	@Test
	public void testParametersTakePrecedence() throws PrismException
	{
		prism.defineConstant("p", "1/3");
		prism.setParameters(List.of(new DefinedConstant.Parameter("p", null, "0", "1")));
		assertTrue(check(0).toString().endsWith(": p"));
		prism.setParameters(null);
		assertEquals(1.0 / 3, (Double) check(0), 1e-10);
	}

	@Test
	public void testDefinitionsKeptForNewModel() throws PrismException
	{
		// Definitions for constants that don't exist (here, q in the model) are ignored
		prism.defineConstants("p=0.25,q=0");
		assertEquals(0.25, (Double) check(0), 1e-10);
		ModulesFile modulesFile2 = prism.parseModelString(MODEL.replace("p:(s'=1)", "(p/2):(s'=1)").replace("(1-p)", "(1-p/2)"));
		prism.loadPRISMModel(modulesFile2);
		assertEquals(0.125, (Double) check(0), 1e-10);
	}

	@Test
	public void testRangesAndErrors() throws PrismException
	{
		prism.defineConstants("p=0:0.5:1,q=1");
		prism.defineConstantRange("r", "1", "10", null);
		assertEquals(Map.of("p", "0:0.5:1", "q", "1", "r", "1:10"), prism.getConstantDefinitions());
		// A range is for experiments, so model checking needs a value given another way
		assertThrows(PrismException.class, () -> check(0));
		Values values = new Values();
		values.addValue("p", 0.5);
		prism.setModelConstants(values);
		assertEquals(0.5, (Double) check(0), 1e-10);
		// Errors
		assertThrows(PrismException.class, () -> prism.defineConstants("p=1,p=2"));
		assertThrows(PrismException.class, () -> prism.defineConstants("p"));
		assertThrows(PrismException.class, () -> prism.defineConstant("p", ""));
		prism.defineConstant("p", "x");
		assertThrows(PrismException.class, () -> check(0));
	}

	@Test
	public void testDigitalClocks() throws PrismException
	{
		// Digital clocks translation uses the PTA's constant values directly
		String pta = "pta\n"
				+ "const int K;\n"
				+ "module m x : clock; s : [0..2] init 0; invariant (s=0 => x<=K) endinvariant\n"
				+ "[] s=0 & x>=K -> 0.5:(s'=1) + 0.5:(s'=2); [] s>0 -> true; endmodule\n";
		ModulesFile modulesFilePTA = prism.parseModelString(pta);
		PropertiesFile propertiesFilePTA = prism.parsePropertiesString(modulesFilePTA, "Pmax=? [ F s=1 ]");
		prism.getSettings().set(PrismSettings.PRISM_PTA_METHOD, "Digital clocks");
		prism.loadPRISMModel(modulesFilePTA);
		prism.defineConstant("K", "2");
		assertEquals(0.5, (Double) prism.modelCheck(propertiesFilePTA, propertiesFilePTA.getPropertyObject(0)).getResult(), 1e-10);
	}

	@Test
	public void testExportWithExpandedConstants(@TempDir Path dir) throws Exception
	{
		prism.defineConstant("p", "0.25");
		File file = dir.resolve("out.prism").toFile();
		prism.exportPRISMModelWithExpandedConstants(file);
		assertTrue(Files.readString(file.toPath()).contains("0.25 : (s'=1)"));
	}
}
