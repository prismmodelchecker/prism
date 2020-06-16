//==============================================================================
//	
//	Copyright (c) 2019-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parser.ast.DeclarationBool;
import parser.ast.DeclarationInt;
import parser.ast.DeclarationType;
import parser.ast.Expression;
import parser.ast.ExpressionIdent;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeInt;

/**
 * Class to build a ModelInfo object corresponding to imported explicit-state file storage of a model.
 * Basically, just stores the model type and variable info.
 * The number of states in the model is also extracted.
 */
public class ExplicitFiles2ModelInfo extends PrismComponent
{
	// Info extracted from files for ModelInfo object
	private int numVars;
	private List<String> varNames;
	private List<Type> varTypes;
	private int varMins[];
	private int varMaxs[];
	private int varRanges[];
	private List<String> labelNames;
	
	// Num states
	private int numStates = 0;

	public ExplicitFiles2ModelInfo(PrismComponent parent)
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
	 * Build a ModelInfo object corresponding to the passed in states/transitions/labels files.
	 * If {@code typeOverride} is provided, this is used as the model type;
	 * if it is null, we try to auto-detect it (or default to MDP if that cannot be done).
	 *
	 * @param statesFile states file (may be {@code null})
	 * @param transFile transitions file
	 * @param labelsFile labels file (may be {@code null})
	 * @param typeOverride model type (auto-detected if {@code null})
	 */
	public ModelInfo buildModelInfo(File statesFile, File transFile, File labelsFile, ModelType typeOverride) throws PrismException
	{
		// Extract variable info from states or transitions file, depending on what is available
		if (statesFile != null) {
			extractVarInfoFromStatesFile(statesFile);
		} else {
			extractVarInfoFromTransFile(transFile);
		}

		// Generate and store label names from the labels file, if available.
		// This way, expressions can refer to the labels later on.
		if (labelsFile != null) {
			extractLabelNamesFromLabelsFile(labelsFile);
		} else {
			labelNames = new ArrayList<>();
		}
		
		// Set model type: if no preference stated, try to autodetect
		ModelType modelType;
		if (typeOverride == null) {
			ModelType typeAutodetect = autodetectModelType(transFile);
			if (typeAutodetect != null) {
				mainLog.println("Auto-detected model type: " + typeAutodetect);
			} else {
				typeAutodetect = ModelType.MDP;
				mainLog.println("Assuming default model type: " + typeAutodetect);
			}
			modelType = typeAutodetect;
		} else {
			mainLog.println("Using specified model type: " + typeOverride);
			modelType = typeOverride;
		}

		// Create and return ModelInfo object with above info 
		ModelInfo modelInfo = new ModelInfo()
		{
			@Override
			public ModelType getModelType()
			{
				return modelType;
			}
			
			@Override
			public List<String> getVarNames()
			{
				return varNames;
			}
			
			@Override
			public List<Type> getVarTypes()
			{
				return varTypes;
			}
			
			@Override
			public DeclarationType getVarDeclarationType(int i) throws PrismException
			{
				if (varTypes.get(i) instanceof TypeInt) {
					return new DeclarationInt(Expression.Int(varMins[i]), Expression.Int(varMaxs[i]));
				} else {
					return new DeclarationBool();
				}
			}
			
			@Override
			public List<String> getLabelNames()
			{
				return labelNames;
			}
		};
		
		return modelInfo;
	}

	/**
	 * Build a "dummy" RewardGenerator object corresponding to the passed in rewards file.
	 * Provides access to reward struct names, but not the rewards themselves.
	 * @param stateRewardsFile state rewards file (may be {@code null})
	 */
	public RewardGenerator buildRewardInfo(File stateRewardsFile) throws PrismException
	{
		// Very simple for now: either 1 unnamed reward struct or none
		if (stateRewardsFile != null) {
			return new RewardGenerator()
			{
				@Override
				public List<String> getRewardStructNames()
				{
					return Collections.singletonList("");
				}
			};
		} else {
			return new RewardGenerator()
			{
			};
		}
	}
	
	/**
	 * Extract variable info from a states file.
	 */
	private void extractVarInfoFromStatesFile(File statesFile) throws PrismException
	{
		int i, j, lineNum = 0;

		// open file for reading, automatic close when done
		try (BufferedReader in = new BufferedReader(new FileReader(statesFile))) {
			// read first line and extract var names
			String s = in.readLine();
			lineNum = 1;
			if (s == null)
				throw new PrismException("empty states file");
			s = s.trim();
			if (s.charAt(0) != '(' || s.charAt(s.length() - 1) != ')')
				throw new PrismException("badly formatted state");
			s = s.substring(1, s.length() - 1);
			varNames = new ArrayList<String>(Arrays.asList(s.split(",")));
			numVars = varNames.size();
			// create arrays to store info about vars
			varMins = new int[numVars];
			varMaxs = new int[numVars];
			varRanges = new int[numVars];
			varTypes = new ArrayList<Type>();
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
					String[] ss = s.split(",");
					if (ss.length != numVars)
						throw new PrismException("wrong number of variables");
					// for each variable...
					for (i = 0; i < numVars; i++) {
						// if this is the first state, establish variable type
						if (numStates == 1) {
							if (ss[i].equals("true") || ss[i].equals("false")) {
								varTypes.add(TypeBool.getInstance());
							} else {
								varTypes.add(TypeInt.getInstance());
							}
						}
						// check for new min/max values (ints only)
						if (varTypes.get(i) instanceof TypeInt) {
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
				if (varTypes.get(i) instanceof TypeInt) {
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
	}

	/**
	 * Extract variable info from a transitions file.
	 */
	private void extractVarInfoFromTransFile(File transFile) throws PrismException
	{
		// Open file for reading, automatic close when done
		try (BufferedReader in = new BufferedReader(new FileReader(transFile))) {
			// Read first line and extract num states
			String s = in.readLine();
			if (s == null)
				throw new PrismException("empty transitions file");
			s = s.trim();
			String[] ss = s.split(" ");
			if (ss.length < 2)
				throw new PrismException("");
			numStates = Integer.parseInt(ss[0]);
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line 1 of transition matrix file \"" + transFile + "\"");
		}
		// Store dummy variable info
		numVars = 1;
		varNames = Collections.singletonList("x");
		varTypes = Collections.singletonList(TypeInt.getInstance());
		varMins = new int[] { 0 };
		varMaxs = new int[] { numStates - 1 };
		varRanges = new int[] { numStates - 1 };
	}
	

	/**
	 * Extract names of labels from the labels file.
	 * The "init" and "deadlock" labels are skipped, as they have special
	 * meaning and are implicitly defined for all models.
	 */
	private void extractLabelNamesFromLabelsFile(File labelsFile) throws PrismException
	{
		try (BufferedReader in = new BufferedReader(new FileReader(labelsFile))) {
			// Read/parse first line (label names)
			// Looks like, e.g.: 0="init" 1="deadlock" 2="heads" 3="tails" 4="end"
			String labelsString = in.readLine();
			Pattern label = Pattern.compile("(\\d+)=\"([^\"]+)\"\\s*");
			Matcher matcher = label.matcher(labelsString);
			labelNames = new ArrayList<>();
			while (matcher.find()) {
				String labelName = matcher.group(2);
				// Skip built-in labels
				if (labelName.equals("init") || labelName.equals("deadlock")) {
					continue;
				}
				// Check legal and non-dupe
				if (!ExpressionIdent.isLegalIdentifierName(labelName)) {
					throw new PrismException("Illegal label name \"" + labelName + "\"");
				}
				if (labelNames.contains(labelName)) {
					throw new PrismException("Duplicate label \"" + labelName + "\"");
				}
				labelNames.add(labelName);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + labelsFile + "\"");
		}
	}

	/**
	 * Autodetect the model type based on a sample of the lines from a transitions file.
	 * If not possible, return null;
	 * @param transFile transitions file
	 */
	private ModelType autodetectModelType(File transFile)
	{
		String s, ss[];

		// Open file for reading, automatic close when done
		try (BufferedReader in = new BufferedReader(new FileReader(transFile))) {
			// Look at first line
			s = in.readLine();
			if (s == null) {
				return null;
			}
			ss = s.trim().split(" ");
			// 3 parts - should be an MDP
			if (ss.length == 3) {
				return ModelType.MDP;
			}
			// Not 2 parts: error; give up
			else if (ss.length != 2) {
				return null;
			}
			// Now choose between DTMC and CTMC
			// Read up to max remaining lines
			int lines = 0;
			int max = 5;
			s = in.readLine();
			while (s != null && lines < max) {
				lines++;
				ss = s.trim().split(" ");
				// Look at probability/rate
				double d = Double.parseDouble(ss[2]);
				// Looks like a rate: guess CTMC
				if (d > 1) {
					return ModelType.CTMC;
				}
				// All non-rates so far: guess DTMC
				if (lines == max) {
					return ModelType.DTMC;
				}
				// Read next line
				s = in.readLine();
			}
			return null;
		} catch (NumberFormatException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}
}
