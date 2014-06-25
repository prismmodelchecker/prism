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

import java.io.*;

import parser.ast.*;
import parser.type.TypeBool;
import prism.*;
import simulator.SimulatorEngine;
import explicit.Model;
import explicit.StateModelChecker;

/**
 * This class connects PRISM to the various bits of explicit-state functionality that are implemented.
 * The intention is to minimise dependencies on the Prism class by anything in this package.
 * This makes these classes easier to use independently.
 */
public class PrismExplicit extends PrismComponent
{
	public PrismExplicit(PrismLog mainLog, PrismSettings settings)
	{
		this.mainLog = mainLog;
		this.settings = settings;
	}

	/**
	 * Build a model from a PRISM modelling language description, storing it explicitly.
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
		constructModel = new ConstructModel(this, simEngine);

		// build model
		l = System.currentTimeMillis();
		modelExpl = constructModel.constructModel(modulesFile, false, true);
		l = System.currentTimeMillis() - l;

		mainLog.println("\nTime for model construction: " + l / 1000.0 + " seconds.");

		return modelExpl;
	}

	/**
	 * Export a model's transition matrix to a file (or to the log)
	 * @param model The model
	 * @param ordered Ensure that (source) states are in ascending order?
	 * @param exportType Type of export; one of: <ul>
	 * <li> {@link #EXPORT_PLAIN} 
	 * <li> {@link #EXPORT_MATLAB}
	 * <li> {@link #EXPORT_DOT}
	 * <li> {@link #EXPORT_MRMC}
	 * <li> {@link #EXPORT_MRMC}
	 * <li> {@link #EXPORT_DOT_STATES}
	 * </ul>
	 * @param file File to export to (if null, print to the log instead)
	 */
	public void exportTransToFile(Model model, boolean ordered, int exportType, File file) throws FileNotFoundException, PrismException
	{
		// can only do ordered version of export for explicit engine
		if (!ordered) {
			mainLog.printWarning("Cannot export unordered transition matrix with the explicit engine; using ordered.");
			ordered = true;
		}
		// print message
		mainLog.print("\nExporting transition matrix ");
		switch (exportType) {
		case Prism.EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case Prism.EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		case Prism.EXPORT_DOT: mainLog.print("in Dot format "); break;
		case Prism.EXPORT_MRMC: mainLog.print("in MRMC format "); break;
		case Prism.EXPORT_ROWS: mainLog.print("in rows format "); break;
		case Prism.EXPORT_DOT_STATES: mainLog.print("in Dot format (with states) "); break;
		}
		if (file != null) mainLog.println("to file \"" + file + "\"..."); else mainLog.println("below:");
		PrismLog tmpLog = getPrismLogForFile(file);
		
		// do export
		switch (exportType) {
		case Prism.EXPORT_PLAIN:
			model.exportToPrismExplicitTra(tmpLog);
			break;
		case Prism.EXPORT_MATLAB:
		case Prism.EXPORT_DOT:
		case Prism.EXPORT_MRMC:
		case Prism.EXPORT_ROWS:
		case Prism.EXPORT_DOT_STATES:
			throw new PrismException("Export not yet supported"); // TODO
		}
	}

	public void exportStatesToFile(ModulesFile mf, Model model, int exportType, File file) throws FileNotFoundException
	{
		int i;
		PrismLog tmpLog;
		
		// no specific states format for MRMC
		if (exportType == Prism.EXPORT_MRMC) exportType = Prism.EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == Prism.EXPORT_ROWS) exportType = Prism.EXPORT_PLAIN;
		
		// print message
		mainLog.print("\nExporting list of reachable states ");
		switch (exportType) {
		case Prism.EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case Prism.EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		}
		if (file != null) mainLog.println("to file \"" + file + "\"..."); else mainLog.println("below:");
		
		// create new file log or use main log
		if (file != null) {
			tmpLog = new PrismFileLog(file.getPath());
			if (!tmpLog.ready()) {
				throw new FileNotFoundException();
			}
		} else {
			tmpLog = mainLog;
		}
		
		// print header: list of model vars
		if (exportType == Prism.EXPORT_MATLAB) tmpLog.print("% ");
		tmpLog.print("(");
		for (i = 0; i < mf.getNumVars(); i++) {
			tmpLog.print(mf.getVarName(i));
			if (i < mf.getNumVars()-1) tmpLog.print(",");
		}
		tmpLog.println(")");
		if (exportType == Prism.EXPORT_MATLAB) tmpLog.println("states=[");
		
		// print states
		StateValues statesList = null;
		try {
			statesList = new StateValues(TypeBool.getInstance(), new Boolean(true), model);
		} catch (PrismLangException e) {
			// Can't go wrong - type always fine
		}
		if (exportType != Prism.EXPORT_MATLAB)
			statesList.print(tmpLog);
		else
			statesList.print(tmpLog, true, true, true, true);
		
		// print footer
		if (exportType == Prism.EXPORT_MATLAB) tmpLog.println("];");
		
		// tidy up
		if (file != null) tmpLog.close();
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
		StateModelChecker mc = null;
		Result result = null;

		// Check that property is valid for this model type
		// and create new model checker object
		expr.checkValid(model.getModelType());
		switch (model.getModelType()) {
		case DTMC:
			mc = new DTMCModelChecker(this);
			break;
		case MDP:
			mc = new MDPModelChecker(this);
			break;
		case CTMC:
			mc = new CTMCModelChecker(this);
			break;
		case CTMDP:
			mc = new CTMDPModelChecker(this);
			break;
		case STPG:
			mc = new STPGModelChecker(this);
			break;
		default:
			throw new PrismException("Unknown model type " + model.getModelType());
		}

		// Do model checking
		mc.setModulesFileAndPropertiesFile(modulesFile, propertiesFile);
		result = mc.check(model, expr);

		// Return result
		return result;
	}

	/**
	 * Compute steady-state probabilities (for a DTMC or CTMC).
	 * Output probability distribution to log. 
	 */
	public void doSteadyState(Model model) throws PrismException
	{
		doSteadyState(model, Prism.EXPORT_PLAIN, null);
	}
	
	/**
	 * Compute steady-state probabilities (for a DTMC or CTMC).
	 * Output probability distribution to a file (or, if file is null, to log). 
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 */
	public void doSteadyState(Model model, int exportType, File fileOut) throws PrismException
	{
		long l = 0; // timer
		StateValues probs = null;
		PrismLog tmpLog;
		
		if (!(model.getModelType() == ModelType.CTMC || model.getModelType() == ModelType.DTMC))
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");
		
		// no specific states format for MRMC
		if (exportType == Prism.EXPORT_MRMC) exportType = Prism.EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == Prism.EXPORT_ROWS) exportType = Prism.EXPORT_PLAIN;
		
		mainLog.println("\nComputing steady-state probabilities...");
		l = System.currentTimeMillis();
		probs = computeSteadyStateProbabilities(model);
		l = System.currentTimeMillis() - l;
		
		// print message
		mainLog.print("\nPrinting steady-state probabilities ");
		switch (exportType) {
		case Prism.EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case Prism.EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		}
		if (fileOut != null) mainLog.println("to file \"" + fileOut + "\"..."); else mainLog.println("below:");
		
		// create new file log or use main log
		if (fileOut != null) {
			tmpLog = new PrismFileLog(fileOut.getPath());
			if (!tmpLog.ready()) {
				throw new PrismException("Could not open file \"" + fileOut + "\" for output");
			}
		} else {
			tmpLog = mainLog;
		}
		
		// print out or export probabilities
		probs.print(tmpLog, fileOut == null, exportType == Prism.EXPORT_MATLAB, fileOut == null, true);
		
		// print out computation time
		mainLog.println("\nTime for steady-state probability computation: " + l/1000.0 + " seconds.");
		
		// tidy up
		probs.clear();
		if (fileOut != null) tmpLog.close();
	}

	/**
	 * Compute steady-state probabilities (for a DTMC or CTMC).
	 * Start from initial state or a uniform distribution over multiple initial states.
	 */
	public StateValues computeSteadyStateProbabilities(Model model) throws PrismException
	{
		StateValues probs;
		if (model.getModelType() == ModelType.DTMC) {
			DTMCModelChecker mcDTMC = new DTMCModelChecker(this);
			probs = mcDTMC.doSteadyState((DTMC) model);
		}
		else if (model.getModelType() == ModelType.CTMC) {
			throw new PrismException("Not implemented yet"); // TODO
		}
		else {
			throw new PrismException("Steady-state probabilities only computed for DTMCs/CTMCs");
		}
		return probs;
	}

	/**
	 * Compute transient probabilities (for a DTMC or CTMC).
	 * Output probability distribution to log. 
	 */
	public void doTransient(Model model, double time) throws PrismException
	{
		doTransient(model, time, Prism.EXPORT_PLAIN, null, null);
	}
	
	/**
	 * Compute transient probabilities (for a DTMC or CTMC).
	 * Output probability distribution to a file (or, if file is null, to log). 
	 * The exportType should be EXPORT_PLAIN or EXPORT_MATLAB.
	 * Optionally (if non-null), read in the initial probability distribution from a file.
	 */
	public void doTransient(Model model, double time, int exportType, File fileOut, File fileIn) throws PrismException
	{
		long l = 0; // timer
		StateValues probs = null;
		PrismLog tmpLog;
		
		if (time < 0) throw new PrismException("Cannot compute transient probabilities for negative time value");
		
		// no specific states format for MRMC
		if (exportType == Prism.EXPORT_MRMC) exportType = Prism.EXPORT_PLAIN;
		// rows format does not apply to states output
		if (exportType == Prism.EXPORT_ROWS) exportType = Prism.EXPORT_PLAIN;
		
		l = System.currentTimeMillis();

		if (model.getModelType() == ModelType.DTMC) {
			throw new PrismException("Not implemented yet"); // TODO
		}
		else if (model.getModelType() == ModelType.CTMC) {
			mainLog.println("\nComputing transient probabilities (time = " + time + ")...");
			CTMCModelChecker mcCTMC = new CTMCModelChecker(this);
			probs = mcCTMC.doTransient((CTMC) model, time, fileIn);
		}
		else {
			throw new PrismException("Transient probabilities only computed for DTMCs/CTMCs");
		}
		
		l = System.currentTimeMillis() - l;
		
		// print message
		mainLog.print("\nPrinting transient probabilities ");
		switch (exportType) {
		case Prism.EXPORT_PLAIN: mainLog.print("in plain text format "); break;
		case Prism.EXPORT_MATLAB: mainLog.print("in Matlab format "); break;
		}
		if (fileOut != null) mainLog.println("to file \"" + fileOut + "\"..."); else mainLog.println("below:");
		
		// create new file log or use main log
		if (fileOut != null) {
			tmpLog = new PrismFileLog(fileOut.getPath());
			if (!tmpLog.ready()) {
				throw new PrismException("Could not open file \"" + fileOut + "\" for output");
			}
		} else {
			tmpLog = mainLog;
		}
		
		// print out or export probabilities
		probs.print(tmpLog, fileOut == null, exportType == Prism.EXPORT_MATLAB, fileOut == null, fileOut == null);
		
		// print out computation time
		mainLog.println("\nTime for transient probability computation: " + l/1000.0 + " seconds.");

		// tidy up
		probs.clear();
		if (fileOut != null) tmpLog.close();
	}
	
	/**
	 * Either create a new PrismFileLog for {@code file} or,
	 * if {@code file} is null, return {@code mainLog}.
	 * Throws a {@code PrismException} if there is a problem opening the file.
	 */
	private PrismLog getPrismLogForFile(File file) throws PrismException
	{
		// create new file log or use main log
		PrismLog tmpLog;
		if (file != null) {
			tmpLog = new PrismFileLog(file.getPath());
			if (!tmpLog.ready()) {
				throw new PrismException("Could not open file \"" + file + "\" for output");
			}
		} else {
			tmpLog = mainLog;
		}
		return tmpLog;
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
			PropertiesFile propertiesFile = prism.parsePropertiesFile(modulesFile, new File(args[1]));
			prism.loadPRISMModel(modulesFile);
			prism.exportTransToFile(true, Prism.EXPORT_PLAIN, new File("tmp.tra"));
			prism.exportLabelsToFile(null, Prism.EXPORT_PLAIN, new File("tmp.lab"));
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
