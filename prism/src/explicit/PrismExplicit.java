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

package explicit;

import java.util.*;
import java.io.*;

import parser.ast.*;
import prism.*;
// Override some imports for which there are name clashes
import explicit.Model;
import explicit.StateModelChecker;

/**
 * This class connects PRISM to the various bits of explicit-state functionality that are implemented.
 * The intention is to minimise dependencies on the Prism class by anything in this package.
 * This makes these classes easier to use independently.
 */
public class PrismExplicit
{
	// Parent Prism object
	private Prism prism = null;
	// Model checker(s)
	private StateModelChecker mc = null;
	
	public PrismExplicit(Prism prism)
	{
		this.prism = prism;
	}

	// model checking
	// returns result or throws an exception in case of error
	
	public Result modelCheck(Model model, String labelsFilename, PropertiesFile propertiesFile, Expression expr) throws PrismException, PrismLangException
	{
		Result result = null;
		
		// Check that property is valid for this model type
		// and create new model checker object
		expr.checkValid(model.getModelType());
		switch (model.getModelType()) {
		case DTMC:
			mc = new DTMCModelChecker();
			break;
		case MDP:
			mc = new MDPModelChecker();
			break;
		case CTMC:
			mc = new CTMCModelChecker();
			break;
		default:
			throw new PrismException("Unknown model type"+model.getModelType());
		}
		
		mc.setLog(prism.getMainLog());
		
		// TODO: pass PRISM settings to mc
		
		// pass labels info
		mc.setLabelsFilename(labelsFilename);
		
		// Do model checking
//		res = mc.check(expr);
		result = mc.check(model, expr);
		
		// Return result
		return result;
	}
	
	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		Prism prism;
		try {
			PrismLog log = new PrismFileLog("stdout");
			prism = new Prism(log, log);
			prism.initialise();
			prism.setDoProbChecks(false);
			ModulesFile modulesFile = prism.parseModelFile(new File(args[0]));
			modulesFile.setUndefinedConstants(null);
			PropertiesFile propertiesFile = prism.parsePropertiesFile(modulesFile, new File(args[1]));
			propertiesFile.setUndefinedConstants(null);
			prism.Model model = prism.buildModel(modulesFile);
			prism.exportTransToFile(model, true, Prism.EXPORT_PLAIN, new File("tmp.tra"));
			prism.exportLabelsToFile(model, modulesFile, null, Prism.EXPORT_PLAIN, new File("tmp.lab"));
			DTMCSimple modelExplicit = new DTMCSimple();
			modelExplicit.buildFromPrismExplicit("tmp.tra");
			PrismExplicit pe = new PrismExplicit(prism);
			Object res = pe.modelCheck(modelExplicit, "tmp.lab", propertiesFile, propertiesFile.getProperty(0));
		} catch (PrismException e) {
			System.out.println(e);
		} catch (FileNotFoundException e) {
			System.out.println(e);
		}
	}
}
