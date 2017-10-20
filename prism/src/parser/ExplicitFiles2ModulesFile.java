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

package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parser.ast.Declaration;
import parser.ast.DeclarationBool;
import parser.ast.DeclarationInt;
import parser.ast.DeclarationType;
import parser.ast.Expression;
import parser.ast.ExpressionIdent;
import parser.ast.ExpressionLiteral;
import parser.ast.LabelList;
import parser.ast.Module;
import parser.ast.ModulesFile;
import parser.ast.RewardStruct;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeInt;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;

/**
 * Class to build a (partial) ModulesFile corresponding to imported explicit-state file storage of a model.
 * Basically, the ModulesFile just stores the model type and variable info.
 * The number of states in the model is also extracted.
 */
public class ExplicitFiles2ModulesFile extends PrismComponent
{
	// Num states
	private int numStates = 0;

	public ExplicitFiles2ModulesFile(PrismComponent parent)
	{
		super(parent);
	}

	/**
	 * Get the number of states
	 * (determined from either states file or transitions file). 
	 */
	public int getNumStates()
	{
		return numStates;
	}

	/**
	 * Build a ModulesFile corresponding to the passed in states/transitions/labels files.
	 * If {@code typeOverride} is null, we assume model is an MDP.
	 *
	 * @param statesFile states file (may be {@code null})
	 * @param transFile transitions file
	 * @param labelsFile labels file (may be {@code null})
	 */
	public ModulesFile buildModulesFile(File statesFile, File transFile, File labelsFile, File stateRewardsFile, ModelType typeOverride) throws PrismException
	{
		ModulesFile modulesFile;
		ModelType modelType;

		// Generate ModulesFile from states or transitions file, depending what is available
		if (statesFile != null) {
			modulesFile = createVarInfoFromStatesFile(statesFile);
		} else {
			modulesFile = createVarInfoFromTransFile(transFile);
		}

		// Generate and store a label list from the labelsFile, if available.
		// This way, expressions can refer to the labels later on.
		if (labelsFile != null) {
			modulesFile.setLabelList(createLabelListFromLabelsFile(labelsFile));
		}
		
		// Add a new (unnamed) reward structure if there is a state rewards file
		if (stateRewardsFile != null) {
			modulesFile.addRewardStruct(new RewardStruct());
		}

		// Set model type: if no preference stated, assume default of MDP
		modelType = (typeOverride == null) ? ModelType.MDP : typeOverride;
		modulesFile.setModelType(modelType);

		return modulesFile;
	}

	/**
	 * Build a ModulesFile corresponding to a states file.
	 */
	private ModulesFile createVarInfoFromStatesFile(File statesFile) throws PrismException
	{
		String s, ss[];
		int i, j, lineNum = 0;
		Module m;
		Declaration d;
		DeclarationType dt;
		// Var info
		int numVars;
		String varNames[];
		int varMins[];
		int varMaxs[];
		int varRanges[];
		Type varTypes[];
		ModulesFile modulesFile;

		// open file for reading, automatic close when done
		try (BufferedReader in = new BufferedReader(new FileReader(statesFile))) {
			// read first line and extract var names
			s = in.readLine();
			lineNum = 1;
			if (s == null)
				throw new PrismException("empty states file");
			s = s.trim();
			if (s.charAt(0) != '(' || s.charAt(s.length() - 1) != ')')
				throw new PrismException("badly formatted state");
			s = s.substring(1, s.length() - 1);
			varNames = s.split(",");
			numVars = varNames.length;
			// create arrays to store info about vars
			varMins = new int[numVars];
			varMaxs = new int[numVars];
			varRanges = new int[numVars];
			varTypes = new Type[numVars];
			// read remaining lines
			s = in.readLine();
			lineNum++;
			numStates = 0;
			while (s != null) {
				// skip blank lines
				s = s.trim();
				if (s.length() > 0) {
					// increment state count
					numStates++;
					// split string
					s = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
					ss = s.split(",");
					if (ss.length != numVars)
						throw new PrismException("wrong number of variables");
					// for each variable...
					for (i = 0; i < numVars; i++) {
						// if this is the first state, establish variable type
						if (numStates == 1) {
							if (ss[i].equals("true") || ss[i].equals("false"))
								varTypes[i] = TypeBool.getInstance();
							else
								varTypes[i] = TypeInt.getInstance();
						}
						// check for new min/max values (ints only)
						if (varTypes[i] instanceof TypeInt) {
							j = Integer.parseInt(ss[i]);
							if (numStates == 1) {
								varMins[i] = varMaxs[i] = j;
							} else {
								if (j < varMins[i])
									varMins[i] = j;
								if (j > varMaxs[i])
									varMaxs[i] = j;
							}
						}
					}
				}
				// read next line
				s = in.readLine();
				lineNum++;
			}
			// compute variable ranges
			for (i = 0; i < numVars; i++) {
				if (varTypes[i] instanceof TypeInt) {
					varRanges[i] = varMaxs[i] - varMins[i];
					// if range = 0, increment maximum - we don't allow zero-range variables
					if (varRanges[i] == 0)
						varMaxs[i]++;
				}
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + statesFile + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of states file \"" + statesFile + "\"");
		} catch (PrismException e) {
			throw new PrismException("Error detected (" + e.getMessage() + ") at line " + lineNum + " of states file \"" + statesFile + "\"");
		}
		// create modules file
		modulesFile = new ModulesFile();
		m = new Module("M");
		for (i = 0; i < numVars; i++) {
			if (varTypes[i] instanceof TypeInt) {
				dt = new DeclarationInt(Expression.Int(varMins[i]), Expression.Int(varMaxs[i]));
				d = new Declaration(varNames[i], dt);
				d.setStart(Expression.Int(varMins[i]));
			} else {
				dt = new DeclarationBool();
				d = new Declaration(varNames[i], dt);
				d.setStart(Expression.False());
			}
			m.addDeclaration(d);
		}
		modulesFile.addModule(m);
		modulesFile.tidyUp();

		return modulesFile;
	}

	/**
	 * Build a ModulesFile corresponding to a transitions file.
	 */
	private ModulesFile createVarInfoFromTransFile(File transFile) throws PrismException
	{
		String s, ss[];
		int lineNum = 0;
		Module m;
		Declaration d;
		DeclarationType dt;
		ModulesFile modulesFile;

		// open file for reading, automatic close when done
		try (BufferedReader in = new BufferedReader(new FileReader(transFile))) {
			// read first line and extract num states
			s = in.readLine();
			lineNum = 1;
			if (s == null)
				throw new PrismException("empty transitions file");
			s = s.trim();
			ss = s.split(" ");
			if (ss.length < 2)
				throw new PrismException("");
			numStates = Integer.parseInt(ss[0]);
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of transition matrix file \"" + transFile + "\"");
		} catch (PrismException e) {
			throw new PrismException("Error detected (" + e.getMessage() + ") at line " + lineNum + " of transition matrix file \"" + transFile + "\"");
		}
		// create modules file
		modulesFile = new ModulesFile();
		m = new Module("M");
		dt = new DeclarationInt(Expression.Int(0), Expression.Int(numStates - 1));
		d = new Declaration("x", dt);
		d.setStart(Expression.Int(0));
		m.addDeclaration(d);
		modulesFile.addModule(m);
		modulesFile.tidyUp();

		return modulesFile;
	}
	

	/**
	 * Create a LabelList from a labels file, i.e., a label definition per
	 * label that is encountered in the file.
	 * <br>
	 * In the label definitions, the label expression is set to "false",
	 * the actual information about the states belonging to the label
	 * is later on directly stored in the model.
	 * <br>
	 * The "init" and "deadlock" labels are skipped, as they have special
	 * meaning and are implicitly defined for all models.
	 */
	private LabelList createLabelListFromLabelsFile(File labelsFile) throws PrismException
	{
		LabelList list = new LabelList();

		try (BufferedReader in = new BufferedReader(new FileReader(labelsFile))) {
			// read first line (label names)
			String labelNames = in.readLine();
			// split
			Pattern label = Pattern.compile("(\\d+)=\"([^\"]+)\"\\s*");
			Matcher matcher = label.matcher(labelNames);
			while (matcher.find()) {
				String labelName = matcher.group(2);

				if (labelName.equals("init") || labelName.equals("deadlock")) {
					// "init" and "deadlock" are special labels that exist
					// in every model, so we don't need to add them to the ModulesFile
					continue;
				}

				// TODO: Check that label name is valid identifier

				if (list.getLabelIndex(labelName) != -1) {
					throw new PrismException("duplicate label \"" + labelName + "\"");
				}

				Expression falseExpression = new ExpressionLiteral(TypeBool.getInstance(), false);
				list.addLabel(new ExpressionIdent(labelName), falseExpression);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + labelsFile + "\"");
		}
		return list;
	}

}
