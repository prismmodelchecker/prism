package prism;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;

import javax.swing.event.ListSelectionEvent;

import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import simulator.ModulesFileModelGenerator;
import simulator.SimulatorEngine;
import simulator.method.CIwidth;
import simulator.method.SimulationMethod;

public class TestSim
{
	public static void main(String[] args)
	{
		new TestSim().run();
	}

	public void run()
	{
		try {
			PrismLog mainLog = new PrismFileLog("stdout");
			Prism prism = new Prism(mainLog);
			prism.initialise();
			
			ModulesFile modulesFile = prism.parseModelFile(new File("../prism-examples/simple/dice/dice.pm"));
			
			PropertiesFile propertiesFile = prism.parsePropertiesFile(modulesFile, new File("../prism-examples/simple/dice/dice.pctl"));
			
			SimulatorEngine simEngine = new SimulatorEngine(prism);
			ModulesFileModelGenerator modelGen = new ModulesFileModelGenerator(modulesFile, prism);
			simEngine.loadModel(modelGen, modelGen);
			
			SimulationMethod simMethod = new CIwidth(0.01, 1000);
			simEngine.modelCheckMultipleProperties(propertiesFile, Collections.singletonList(propertiesFile.getProperty(1)), null, 10000, simMethod); 
			
			simEngine.createNewOnTheFlyPath();
			simEngine.initialisePath(null);
			simEngine.automaticTransitions(100, false);

			
		} catch (PrismException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
