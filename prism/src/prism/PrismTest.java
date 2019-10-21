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
 * (this now uses the newer version of the API, released after PRISM 4.0.3)
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
			PropertiesFile propertiesFile;
			Result result;
			
			// Init
			mainLog = new PrismFileLog("stdout");
			prism = new Prism(mainLog);
			prism.initialise();
			
			// Parse/load model 1
			// NB: no need to build explicitly - it will be done if/when neeed
			modulesFile = prism.parseModelFile(new File(args[0]));
			prism.loadPRISMModel(modulesFile);
			
			// Parse a prop, check on model 1
			propertiesFile = prism.parsePropertiesString("P=?[F<=0.1 s1=1]");
			result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(0));
			System.out.println(result.getResult());
			
			// Parse another prop, check on model 1
			propertiesFile = prism.parsePropertiesString("P=?[F<=0.1 s1=1]");
			result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(0));
			System.out.println(result.getResult());
			
			// Parse/load model 2
			modulesFile = prism.parseModelFile(new File(args[1]));
			prism.loadPRISMModel(modulesFile);
			
			// Parse a prop, check on model 2
			propertiesFile = prism.parsePropertiesString("P=?[F<=0.1 s1=1]");
			result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(0));
			System.out.println(result.getResult());
			
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
