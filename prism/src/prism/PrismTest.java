//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package prism;

import java.io.*;

import parser.ast.*;

/**
 * Example class demonstrating how to control PRISM programmatically,
 * i.e. through the "API" exposed by the class prism.Prism.
 * Test like this:
 * PRISM_MAINCLASS=prism.PrismTest bin/prism ../prism-examples/polling/poll2.sm ../prism-examples/polling/poll3.sm
 */
public class PrismTest
{
	public static void main(String args[])
	{
		new PrismTest().go(args);
	}
	
	public void go(String args[])
	{
		try {
			PrismLog mainLog;
			Prism prism;
			ModulesFile modulesFile;
			Model model;
			PropertiesFile propertiesFile;
			Result result;
			
			// Init
			mainLog = new PrismFileLog("stdout");
			prism = new Prism(mainLog, mainLog);
			prism.initialise();
			
			// Parse build model 1
			modulesFile = prism.parseModelFile(new File(args[0]));
			modulesFile.setUndefinedConstants(null);
			model = prism.buildModel(modulesFile);
			
			// Parse a prop, check on model 1
			propertiesFile = prism.parsePropertiesString(modulesFile, "P=?[F<=0.1 s1=1]");
			propertiesFile.setUndefinedConstants(null);
			result = prism.modelCheck(model, propertiesFile, propertiesFile.getProperty(0));
			System.out.println(result.getResult());
			
			// Parse another prop, check on model 1
			propertiesFile = prism.parsePropertiesString(modulesFile, "P=?[F<=0.1 s1=1]");
			propertiesFile.setUndefinedConstants(null);
			result = prism.modelCheck(model, propertiesFile, propertiesFile.getProperty(0));
			System.out.println(result.getResult());
			
			// Clear model 1
			model.clear();
			
			// Parse build model 2
			modulesFile = prism.parseModelFile(new File(args[1]));
			modulesFile.setUndefinedConstants(null);
			model = prism.buildModel(modulesFile);
			
			// Parse a prop, check on model 2
			propertiesFile = prism.parsePropertiesString(modulesFile, "P=?[F<=0.1 s1=1]");
			propertiesFile.setUndefinedConstants(null);
			result = prism.modelCheck(model, propertiesFile, propertiesFile.getProperty(0));
			System.out.println(result.getResult());
			
			// Clear model 2
			model.clear();
			
			// Close down
			prism.closeDown();
		}
		catch (FileNotFoundException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
		catch (PrismException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}
}
