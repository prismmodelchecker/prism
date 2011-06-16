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
import parser.*; // Override some imports for which there are name clashes
import simulator.SimulatorEngine;
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
	private PrismLog mainLog = null;
	private PrismSettings settings = null;
	// Model checker(s)
	private StateModelChecker mc = null;

	public PrismExplicit(PrismLog mainLog, PrismSettings settings)
	{
		this.mainLog = mainLog;
		this.settings = settings;
	}

	/**
	 * Build a model from a PRISM modelling language description, storing it explicitly.,
	 * It is assumed that all constants in the model file have been defined by now.  
	 * @param modulesFile Model to build
	 * @param simEngine PRISM simulator engine (for model exploration)
	 */
	public Model buildModel(ModulesFile modulesFile, SimulatorEngine simEngine) throws PrismException
	{
		long l; // timer
		Model modelExpl;
		ConstructModel constructModel;

		if (modulesFile.getModelType() == ModelType.PTA) {
			throw new PrismException("You cannot build a PTA model explicitly, only perform model checking");
		}

		mainLog.print("\nBuilding model...\n");

		// create translator
		constructModel = new ConstructModel(simEngine, mainLog);

		// build model
		l = System.currentTimeMillis();
		modelExpl = constructModel.constructModel(modulesFile, false, true);
		l = System.currentTimeMillis() - l;

		mainLog.println("\nTime for model construction: " + l / 1000.0 + " seconds.");

		return modelExpl;
	}

	/**
	 * Perform model checking of a property on a model and return result.
	 * @param model The model
	 * @param modulesFile Original model file (for rewards/...) (optional; null if not needed)
	 * @param propertiesFile Parent property file of property (for labels/constants/...) (optional; null if not needed)
	 * @param expr The property
	 */
	public Result modelCheck(Model model, ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr) throws PrismException, PrismLangException
	{
		Result result = null;
		String s;

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
		case CTMDP:
			mc = new CTMDPModelChecker();
			break;
		case STPG:
			mc = new STPGModelChecker();
			break;
		default:
			throw new PrismException("Unknown model type " + model.getModelType());
		}

		mc.setLog(mainLog);
		mc.setSettings(settings);

		mc.setModulesFile(modulesFile);
		mc.setPropertiesFile(propertiesFile);

		mc.setPrecomp(settings.getBoolean(PrismSettings.PRISM_PRECOMPUTATION));
		s = settings.getString(PrismSettings.PRISM_TERM_CRIT);
		if (s.equals("Absolute")) {
			mc.setTermCrit(StateModelChecker.TermCrit.ABSOLUTE);
		} else if (s.equals("Relative")) {
			mc.setTermCrit(StateModelChecker.TermCrit.RELATIVE);
		}
		mc.setTermCritParam(settings.getDouble(PrismSettings.PRISM_TERM_CRIT_PARAM));
		mc.setMaxIters(settings.getInteger(PrismSettings.PRISM_MAX_ITERS));
		switch (model.getModelType()) {
		case DTMC:
			s = settings.getString(PrismSettings.PRISM_LIN_EQ_METHOD);
			if (s.equals("Gauss-Seidel")) {
				mc.setSolnMethod(StateModelChecker.SolnMethod.GAUSS_SEIDEL);
			} else {
				mc.setSolnMethod(StateModelChecker.SolnMethod.VALUE_ITERATION);
			}
			break;
		case MDP:
			s = settings.getString(PrismSettings.PRISM_MDP_SOLN_METHOD);
			if (s.equals("Gauss-Seidel")) {
				mc.setSolnMethod(StateModelChecker.SolnMethod.GAUSS_SEIDEL);
			} else if (s.equals("Policy iteration")) {
				mc.setSolnMethod(StateModelChecker.SolnMethod.POLICY_ITERATION);
			} else if (s.equals("Modified policy iteration")) {
				mc.setSolnMethod(StateModelChecker.SolnMethod.MODIFIED_POLICY_ITERATION);
			} else {
				mc.setSolnMethod(StateModelChecker.SolnMethod.VALUE_ITERATION);
			}
			break;
		}

		// TODO: pass PRISM settings to mc

		// Do model checking
		result = mc.check(model, expr);

		// Return result
		return result;
	}

	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		modelCheckFromPrismFile(args);
		//modelCheckViaExplicitFiles(args);
	}

	/**
	 * Simple test program.
	 */
	private static void modelCheckFromPrismFile(String args[])
	{
		Prism prism;
		try {
			PrismLog mainLog = new PrismFileLog("stdout");
			prism = new Prism(mainLog, mainLog);
			//prism.initialise();
			ModulesFile modulesFile = prism.parseModelFile(new File(args[0]));
			modulesFile.setUndefinedConstants(null);
			PropertiesFile propertiesFile = prism.parsePropertiesFile(modulesFile, new File(args[1]));
			propertiesFile.setUndefinedConstants(null);
			PrismExplicit pe = new PrismExplicit(prism.getMainLog(), prism.getSettings());
			Model modelExpl = pe.buildModel(modulesFile, prism.getSimulator());
			pe.modelCheck(modelExpl, modulesFile, propertiesFile, propertiesFile.getProperty(0));
		} catch (PrismException e) {
			System.out.println(e);
		} catch (FileNotFoundException e) {
			System.out.println(e);
		}
	}

	/**
	 * Simple test program.
	 */
	private static void modelCheckViaExplicitFiles(String args[])
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
			PrismExplicit pe = new PrismExplicit(prism.getMainLog(), prism.getSettings());
			pe.modelCheck(modelExplicit, null, propertiesFile, propertiesFile.getProperty(0));
		} catch (PrismException e) {
			System.out.println(e);
		} catch (FileNotFoundException e) {
			System.out.println(e);
		}
	}
}
