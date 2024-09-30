//==============================================================================
//	
//	Copyright (c) 2019-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import common.iterable.Reducible;
import csv.BasicReader;
import csv.CsvFormatException;
import csv.CsvReader;
import param.BigRational;
import parser.State;
import parser.ast.DeclarationBool;
import parser.ast.DeclarationInt;
import parser.ast.DeclarationType;
import parser.ast.Expression;
import parser.ast.ExpressionIdent;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeInt;
import prism.Evaluator;
import prism.ModelInfo;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.RewardGenerator;

import static csv.BasicReader.LF;

/**
 * Class to manage importing models from PRISM explicit files (.tra, .sta, etc.)
 */
public class PrismExplicitImporter implements ExplicitModelImporter
{
	// What to import: files and type override
	private File statesFile;
	private File transFile;
	private File labelsFile;
	private List<File> stateRewardsFiles;
	private List<File> transRewardsFiles;
	private ModelType typeOverride;

	// Model info extracted from files and then stored in a ModelInfo object
	private int numVars;
	private List<String> varNames;
	private List<Type> varTypes;
	private int varMins[];
	private int varMaxs[];
	private int varRanges[];
	private List<String> labelNames;
	private ModelInfo modelInfo;

	// String stating the model type and how it was obtained
	private String modelTypeString;

	// Num states/transitions
	private int numStates = 0;
	private int numChoices = 0;
	private int numTransitions = 0;

	// Mapping from label indices in file to (non-built-in) label indices (also: -1=init, -2=deadlock)
	private List<Integer> labelMap;

	// Reward info extracted from files and then stored in a RewardGenerator object
	private RewardGenerator<?> rewardInfo;

	// File(s) to read in rewards from
	private List<PrismExplicitImporter.RewardFile> stateRewardsReaders = new ArrayList<>();
	private List<PrismExplicitImporter.RewardFile> transRewardsReaders = new ArrayList<>();

	// Regex for comments
	protected static final Pattern COMMENT_PATTERN = Pattern.compile("#.*");
	// Regex for reward name
	protected static final Pattern REWARD_NAME_PATTERN = Pattern.compile("# Reward structure (\"([_a-zA-Z0-9]*)\")$");


	/**
	 * Constructor
	 * @param statesFile States file (may be {@code null})
	 * @param transFile Transitions file
	 * @param labelsFile Labels file (may be {@code null})
	 * @param stateRewardsFiles State rewards files list (can be empty)
	 * @param transRewardsFiles Transition rewards files list (can be empty)
	 * @param typeOverride Specified model type (null mean auto-detect it, or default to MDP if that cannot be done).
	 */
	public PrismExplicitImporter(File statesFile, File transFile, File labelsFile, List<File> stateRewardsFiles, List<File> transRewardsFiles, ModelType typeOverride) throws PrismException
	{
		this.statesFile = statesFile;
		this.transFile = transFile;
		this.labelsFile = labelsFile;
		this.stateRewardsFiles = stateRewardsFiles == null ? new ArrayList<>() : stateRewardsFiles;
		this.transRewardsFiles = transRewardsFiles == null ? new ArrayList<>() : transRewardsFiles;
		this.typeOverride = typeOverride;
		this.stateRewardsReaders = new ArrayList<>(this.stateRewardsFiles.size());
		for (File file : this.stateRewardsFiles) {
			this.stateRewardsReaders.add(new PrismExplicitImporter.RewardFile(file));
		}
		this.transRewardsReaders = new ArrayList<>(this.transRewardsFiles.size());
		for (File file : this.transRewardsFiles) {
			this.transRewardsReaders.add(new PrismExplicitImporter.RewardFile(file));
		}
	}

	/**
	 * Get the states file (null if not used).
	 */
	public File getStatesFile()
	{
		return statesFile;
	}

	/**
	 * Get the transitions file
	 */
	public File getTransFile()
	{
		return transFile;
	}

	/**
	 * Get the labels file (null if not used).
	 */
	public File getLabelsFile()
	{
		return labelsFile;
	}

	/**
	 * Get a list of all files being imported from.
	 */
	public List<File> getAllFiles()
	{
		ArrayList<File> allFiles = new ArrayList<>();
		if (transFile != null) {
			allFiles.add(transFile);
		}
		if (statesFile != null) {
			allFiles.add(statesFile);
		}
		if (labelsFile != null) {
			allFiles.add(labelsFile);
		}
		if (stateRewardsFiles != null) {
			allFiles.addAll(stateRewardsFiles);
		}
		if (transRewardsFiles != null) {
			allFiles.addAll(transRewardsFiles);
		}
		return allFiles;
	}

	@Override
	public boolean providesStates()
	{
		return getStatesFile() != null;
	}

	@Override
	public boolean providesLabels()
	{
		return getLabelsFile() != null;
	}

	@Override
	public String sourceString()
	{
		return getAllFiles().stream().map(f -> "\""+f.toString()+"\"").collect(Collectors.joining(", "));
	}

	@Override
	public ModelInfo getModelInfo() throws PrismException
	{
		// Construct lazily, as needed
		if (modelInfo == null) {
			buildModelInfo();
		}
		return modelInfo;
	}

	@Override
	public int getNumStates() throws PrismException
	{
		// Construct lazily, as needed
	 	// (determined from either states file or transitions file)
		if (modelInfo == null) {
			buildModelInfo();
		}
		return numStates;
	}

	@Override
	public int getNumChoices() throws PrismException
	{
		// Construct lazily, as needed
		// (determined from transitions file)
		if (modelInfo == null) {
			buildModelInfo();
		}
		return numChoices;
	}

	@Override
	public int getNumTransitions() throws PrismException
	{
		// Construct lazily, as needed
		// (determined from transitions file)
		if (modelInfo == null) {
			buildModelInfo();
		}
		return numTransitions;
	}

	@Override
	public String getModelTypeString()
	{
		return modelTypeString;
	}

	@Override
	public RewardGenerator<?> getRewardInfo() throws PrismException
	{
		// Construct lazily, as needed
		if (rewardInfo == null) {
			buildRewardInfo();
		}
		return rewardInfo;
	}

	/**
	 * Build/store model info from the states/transitions/labels files.
	 * Can then be accessed via {@link #getModelInfo()}.
	 * Also available: {@link #getNumStates()}, {@link #getNumChoices()}, {@link #getNumTransitions()}.
	 */
	private void buildModelInfo() throws PrismException
	{
		// Extract model stats from header of transitions file
		extractModelStatsFromTransFile(transFile);

		// Extract variable info from states, if available
		if (statesFile != null) {
			extractVarInfoFromStatesFile(statesFile);
		}
		// Otherwise store dummy variable info
		else {
			numVars = 1;
			varNames = Collections.singletonList("x");
			varTypes = Collections.singletonList(TypeInt.getInstance());
			varMins = new int[] { 0 };
			varMaxs = new int[] { numStates - 1 };
			varRanges = new int[] { numStates - 1 };
		}

		// Generate and store label names from the labels file, if available.
		// This way, expressions can refer to the labels later on.
		if (labelsFile != null) {
			extractLabelNamesFromLabelsFile(labelsFile);
		} else {
			labelNames = new ArrayList<>();
			labelMap = new ArrayList<>();
		}
		
		// Set model type: if no preference stated, try to autodetect
		ModelType modelType;
		if (typeOverride == null) {
			ModelType typeAutodetect = autodetectModelType(transFile);
			if (typeAutodetect != null) {
				modelTypeString = typeAutodetect + " (auto-detected)";
			} else {
				typeAutodetect = ModelType.MDP;
				modelTypeString = typeAutodetect + " (default)";
			}
			modelType = typeAutodetect;
		} else {
			modelTypeString = typeOverride + " (user-specified)";
			modelType = typeOverride;
		}

		// Create and return ModelInfo object with above info 
		modelInfo = new ModelInfo()
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
			int counter = 0;
			while (s != null) {
				// skip blank lines
				s = s.trim();
				if (s.length() > 0) {
					counter++;
					// split string
					s = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
					String[] ss = s.split(",");
					if (ss.length != numVars)
						throw new PrismException("wrong number of variables");
					// for each variable...
					for (i = 0; i < numVars; i++) {
						// if this is the first state, establish variable type
						if (counter == 1) {
							if (ss[i].equals("true") || ss[i].equals("false")) {
								varTypes.add(TypeBool.getInstance());
							} else {
								varTypes.add(TypeInt.getInstance());
							}
						}
						// check for new min/max values (ints only)
						if (varTypes.get(i) instanceof TypeInt) {
							j = Integer.parseInt(ss[i]);
							if (counter == 1) {
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
	 * Extract model stats (number of states/transitions) from a transitions file header.
	 */
	private void extractModelStatsFromTransFile(File transFile) throws PrismException
	{
		try (BufferedReader in = new BufferedReader(new FileReader(transFile))) {
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			if (!csv.hasNextRecord()) {
				throw new PrismException("empty transitions file");
			}
			String[] record = csv.nextRecord();
			checkLineSize(record, 2, 3);
			numStates = Integer.parseInt(record[0]);
			if (record.length == 2) {
				numChoices = numStates;
				numTransitions = Integer.parseInt(record[1]);
			} else {
				numChoices = Integer.parseInt(record[1]);
				numTransitions = Integer.parseInt(record[2]);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (NumberFormatException | CsvFormatException e) {
			throw new PrismException("Error detected at line 1 of transitions file \"" + transFile + "\"");
		}
	}
	

	/**
	 * Extract names of labels from the labels file.
	 * The "init" and "deadlock" labels are skipped, as they have special
	 * meaning and are implicitly defined for all models.
	 */
	private void extractLabelNamesFromLabelsFile(File labelsFile) throws PrismException
	{
		int lineNum = 1;
		try (BufferedReader in = new BufferedReader(new FileReader(labelsFile))) {
			// Read/parse first line (label names)
			// Looks like, e.g.: 0="init" 1="deadlock" 2="heads" 3="tails" 4="end"
			String labelsString = in.readLine();
			Pattern label = Pattern.compile("(\\d+)=\"([^\"]+)\"\\s*");
			Matcher matcher = label.matcher(labelsString);
			labelNames = new ArrayList<>();
			labelMap = new ArrayList<>();
			while (matcher.find()) {
				// Check indices are ascending/contiguous
				int labelIndex = checkLabelIndex(matcher.group(1));
				if (labelIndex != labelMap.size()) {
					throw new PrismException("unexpected label index " + labelIndex);
				}
				// Skip built-in labels
				String labelName = matcher.group(2);
				if (labelName.equals("init")) {
					labelMap.add(-1);
					continue;
				} else if (labelName.equals("deadlock")) {
					labelMap.add(-2);
					continue;
				}
				// Check name legal and non-dupe
				if (!ExpressionIdent.isLegalIdentifierName(labelName)) {
					throw new PrismException("illegal label name \"" + labelName + "\"");
				}
				if (labelNames.contains(labelName)) {
					throw new PrismException("duplicate label name \"" + labelName + "\"");
				}
				labelMap.add(labelNames.size());
				labelNames.add(labelName);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + labelsFile + "\"");
		} catch (PrismException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + lineNum + " of labels file \"" + labelsFile + "\"");
		}
	}

	/**
	 * Autodetect the model type based on a sample of the lines from a transitions file.
	 * If not possible, return null;
	 * @param transFile transitions file
	 */
	private ModelType autodetectModelType(File transFile)
	{
		try (BufferedReader in = new BufferedReader(new FileReader(transFile))) {
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			boolean nondet;
			boolean nonprob;
			// Examine first line
			// 3 numbers should indicate a nondeterministic model, e.g., MDP
			// 2 numbers should indicate a probabilistic model, e.g., DTMC
			// Anything else, give up
			if (!csv.hasNextRecord()) {
				return null;
			}
			// Detect if model is nondeterministic
			String[] recordFirst = csv.nextRecord();
			if (recordFirst.length == 3) {
				nondet = true;
			} else if (recordFirst.length == 2) {
				nondet = false;
			} else {
				return null;
			}
			// Read up to max remaining lines
			int lines = 0;
			int max = 5;
			for (String[] record : csv) {
				if (lines > max) {
					break;
				}
				if ("".equals(record[0])) {
					continue;
				}
				lines++;
				// Detect if model is non-probabilistic (e.g. LTS)
				nonprob = false;
				if (nondet) {
					if (record.length == 3) {
						// LTS
						nonprob = true;
					} else if (record.length >= 4) {
						// LTS with actions or MDP
						nonprob = Prism.isValidIdentifier(record[3]);
					}
				}
				if (nonprob) {
					return ModelType.LTS;
				}
				// Look at probability/rate
				// (give up if line is in unexpected format)
				String probOrRate;
				if (nondet && record.length >= 4) {
					probOrRate = record[3];
				} else if (!nondet && record.length >= 3) {
					probOrRate = record[2];
				} else {
					return null;
				}
				// Interval: guess IMDP/IDTMC
				if (probOrRate.matches("\\[.+,.+\\]")) {
					return nondet ? ModelType.IMDP : ModelType.IDTMC;
				}
				// Get value as double
				double d;
				if (probOrRate.matches("[0-9]+/[0-9]+")) {
					d = new BigRational(probOrRate).doubleValue();
				} else {
					d = Double.parseDouble(probOrRate);
				}
				// Looks like a rate: guess CTMC
				if (d > 1) {
					return ModelType.CTMC;
				}
				// All non-rates so far: guess MDP/DTMC
				if (lines == max) {
					return nondet ? ModelType.MDP : ModelType.DTMC;
				}
			}
			return null;
		} catch (NumberFormatException | CsvFormatException | IOException e) {
			return null;
		}
	}

	@Override
	public void extractStates(IOUtils.StateDefnConsumer storeStateDefn) throws PrismException
	{
		int numVars = modelInfo.getNumVars();
		// If there is no info, just assume that states comprise a single integer value
		if (getStatesFile() == null) {
			for (int s = 0; s < numStates; s++) {
				storeStateDefn.accept(s, 0, s);
			}
			return;
		}
		// Otherwise extract from .sta file
		int lineNum = 0;
		try (BufferedReader in = new BufferedReader(new FileReader(statesFile))) {
			lineNum += skipCommentAndFirstLine(in);
			String st = in.readLine();
			lineNum++;
			while (st != null) {
				st = st.trim();
				if (!st.isEmpty()) {
					// Split into two parts
					String[] ss = st.split(":");
					// Determine which state this line describes
					int s = checkStateIndex(Integer.parseInt(ss[0]), numStates);
					// Now split up middle bit and extract var info
					ss = ss[1].substring(ss[1].indexOf('(') + 1, ss[1].indexOf(')')).split(",");

					State state = new State(numVars);
					if (ss.length != numVars)
						throw new PrismException("(wrong number of variable values) ");
					for (int i = 0; i < numVars; i++) {
						if (ss[i].equals("true")) {
							storeStateDefn.accept(s, i, Boolean.TRUE);
						} else if (ss[i].equals("false")) {
							storeStateDefn.accept(s, i, Boolean.FALSE);
						} else {
							storeStateDefn.accept(s, i, Integer.parseInt(ss[i]));
						}
					}
				}
				st = in.readLine();
				lineNum++;
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + statesFile + "\"");
		} catch (PrismException | NumberFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + lineNum + " of states file \"" + statesFile + "\"");
		}
	}

	@Override
	public int computeMaxNumChoices() throws PrismException
	{
		int lineNum = 0;
		try (BufferedReader in = new BufferedReader(new FileReader(transFile))) {
			lineNum += skipCommentAndFirstLine(in);
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			int maxNumChoices = 0;
			for (String[] record : csv) {
				lineNum++;
				if ("".equals(record[0])) {
					// Skip blank lines
					continue;
				}
				// Lines should be 3-5 long (LTS/MDP with/without actions)
				checkLineSize(record, 3, 5);
				int j = checkChoiceIndex(Integer.parseInt(record[1]));
				if (j + 1 > maxNumChoices) {
					maxNumChoices = j + 1;
				}
			}
			return maxNumChoices;
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\": " + e.getMessage());
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + lineNum + " of transitions file \"" + transFile + "\"");
		}
	}

	@Override
	public <Value> void extractMCTransitions(IOUtils.MCTransitionConsumer<Value> storeTransition, Evaluator<Value> eval) throws PrismException
	{
		int lineNum = 0;
		try (BufferedReader in = new BufferedReader(new FileReader(transFile))) {
			lineNum += skipCommentAndFirstLine(in);
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			for (String[] record : csv) {
				lineNum++;
				if ("".equals(record[0])) {
					// Skip blank lines
					continue;
				}
				checkLineSize(record, 3, 4);
				int s = checkStateIndex(Integer.parseInt(record[0]), numStates);
				int s2 = checkStateIndex(Integer.parseInt(record[1]), numStates);
				Value v = checkValue(record[2], eval);
				Object a = (record.length > 3) ? checkAction(record[3]) : null;
				storeTransition.accept(s, s2, v, a);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + lineNum + " of transitions file \"" + transFile + "\"");
		}
	}

	@Override
	public <Value> void extractMDPTransitions(IOUtils.MDPTransitionConsumer<Value> storeTransition, Evaluator<Value> eval) throws PrismException
	{
		int lineNum = 0;
		try (BufferedReader in = new BufferedReader(new FileReader(transFile))) {
			lineNum += skipCommentAndFirstLine(in);
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			for (String[] record : csv) {
				lineNum++;
				if ("".equals(record[0])) {
					// Skip blank lines
					continue;
				}
				checkLineSize(record, 4, 5);
				int s = checkStateIndex(Integer.parseInt(record[0]), numStates);
				int i = checkChoiceIndex(Integer.parseInt(record[1]));
				int s2 = checkStateIndex(Integer.parseInt(record[2]), numStates);
				Value v = checkValue(record[3], eval);
				Object a = (record.length > 4) ? checkAction(record[4]) : null;
				storeTransition.accept(s, i, s2, v, a);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + lineNum + " of transitions file \"" + transFile + "\"");
		}
	}

	@Override
	public void extractLTSTransitions(IOUtils.LTSTransitionConsumer storeTransition) throws PrismException
	{
		int lineNum = 0;
		try (BufferedReader in = new BufferedReader(new FileReader(transFile))) {
			lineNum += skipCommentAndFirstLine(in);
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			for (String[] record : csv) {
				lineNum++;
				if ("".equals(record[0])) {
					// Skip blank lines
					continue;
				}
				checkLineSize(record, 3, 4);
				int s = checkStateIndex(Integer.parseInt(record[0]), numStates);
				int i = checkChoiceIndex(Integer.parseInt(record[1]));
				int s2 = checkStateIndex(Integer.parseInt(record[2]), numStates);
				Object a = (record.length > 3) ? checkAction(record[3]) : null;
				storeTransition.accept(s, i, s2, a);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + lineNum + " of transitions file \"" + transFile + "\"");
		}
	}

	@Override
	public void extractLabelsAndInitialStates(BiConsumer<Integer, Integer> storeLabel, Consumer<Integer> storeInit) throws PrismException
	{
		// If there is no info, just assume that 0 is the initial state
		if (getLabelsFile() == null) {
			storeInit.accept(0);
			return;
		}
		// Otherwise extract from .lab file
		int lineNum = 0;
		try (BufferedReader in = new BufferedReader(new FileReader(labelsFile))) {
			// Skip first file (label names extracted earlier with model info)
			lineNum += skipCommentAndFirstLine(in);
			String st = in.readLine();
			while (st != null) {
				// Skip blank lines
				st = st.trim();
				if (!st.isEmpty()) {
					// Split line
					String[] ss = st.split(":");
					int s = checkStateIndex(Integer.parseInt(ss[0].trim()), numStates);
					ss = ss[1].trim().split(" ");
					for (int j = 0; j < ss.length; j++) {
						if (ss[j].isEmpty()) {
							continue;
						}
						// Store label info
						int i = checkLabelIndex(ss[j]);
						int l = labelMap.get(i);
						if (l == -1) {
							storeInit.accept(s);
						} else if (l > -1) {
							storeLabel.accept(s, l);
						}
					}
				}
				// Prepare for next iter
				st = in.readLine();
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + labelsFile + "\"");
		} catch (PrismException | NumberFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + lineNum + " of labels file \"" + labelsFile + "\"");
		}
	}

	/**
	 * Load all labels from a PRISM labels (.lab) file and store them in BitSet objects.
	 * Return a map from label name Strings to BitSets.
	 * This is for all labels in the file, including "init", "deadlock".
	 * Note: the size of the BitSet may be smaller than the number of states.
	 */
	public Map<String, BitSet> extractAllLabels() throws PrismException
	{
		// This method only needs the label file
		if (getLabelsFile() == null) {
			throw new PrismException("No labels information available");
		}
		// Extract names first
		extractLabelNamesFromLabelsFile(labelsFile);
		// Build list of bitsets
		BitSet[] bitsets = new BitSet[labelMap.size()];
		for (int i = 0; i < bitsets.length; i++) {
			bitsets[i] = new BitSet();
		}
		// Otherwise extract from .lab file
		int lineNum = 0;
		try (BufferedReader in = new BufferedReader(new FileReader(labelsFile))) {
			// Skip first file (label names extracted earlier)
			lineNum += skipCommentAndFirstLine(in);
			String st = in.readLine();
			while (st != null) {
				// Skip blank lines
				st = st.trim();
				if (!st.isEmpty()) {
					// Split line
					String[] ss = st.split(":");
					int s = checkStateIndex(Integer.parseInt(ss[0].trim()));
					ss = ss[1].trim().split(" ");
					for (int j = 0; j < ss.length; j++) {
						if (ss[j].isEmpty()) {
							continue;
						}
						// Store label info
						int i = checkLabelIndex(ss[j]);
						bitsets[i].set(s);
					}
				}
				// Prepare for next iter
				st = in.readLine();
			}
			// Build BitSet map
			Map<String, BitSet> map = new HashMap<>();
			for (int i = 0; i < bitsets.length; i++) {
				int l = labelMap.get(i);
				if (l == -1) {
					map.put("init", bitsets[i]);
				} else if (l  == -2) {
					map.put("deadlock", bitsets[i]);
				} else if (l > -1) {
					map.put(labelNames.get(l), bitsets[i]);
				}
			}
			return map;
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + labelsFile + "\"");
		} catch (PrismException | NumberFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + lineNum + " of labels file \"" + labelsFile + "\"");
		}
	}

	/**
	 * Build/store rewards info from the input files.
	 * Can then be accessed via {@link #getRewardInfo()}.
	 */
	private void buildRewardInfo() throws PrismException
	{
		rewardInfo = new RewardGenerator<>()
		{
			@Override
			public List<String> getRewardStructNames()
			{
				List<PrismExplicitImporter.RewardFile> rewardsReaders = stateRewardsReaders.size() >= transRewardsFiles.size() ? stateRewardsReaders : transRewardsReaders;
				return Reducible.extend(rewardsReaders).map(f -> f.getName().orElse("")).collect(new ArrayList<>(rewardsReaders.size()));
			}

			@Override
			public int getNumRewardStructs()
			{
				return Math.max(stateRewardsFiles.size(), transRewardsFiles.size());
			}

			@Override
			public boolean rewardStructHasTransitionRewards(int r)
			{
				return false;
			}
		};
	}

	@Override
	public <Value> void extractStateRewards(int rewardIndex, BiConsumer<Integer, Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (rewardIndex < stateRewardsReaders.size()) {
			RewardFile file = stateRewardsReaders.get(rewardIndex);
			file.extractStateRewards(storeReward, eval, numStates);
		}
	}

	@Override
	public <Value> void extractMCTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (rewardIndex < transRewardsReaders.size()) {
			RewardFile file = transRewardsReaders.get(rewardIndex);
			file.extractMCTransitionRewards(storeReward, eval, numStates);
		}
	}

	@Override
	public <Value> void extractMDPTransitionRewards(int rewardIndex, IOUtils.TransitionStateRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (rewardIndex < transRewardsReaders.size()) {
			RewardFile file = transRewardsReaders.get(rewardIndex);
			file.extractMDPTransitionRewards(storeReward, eval, numStates);
		}
	}

	public static class RewardFile
	{
		protected final File file;
		protected final Optional<String> name;

		public RewardFile(File file) throws PrismException
		{
			this.file = Objects.requireNonNull(file);
			this.name = extractRewardStructureName(file);
		}

		public Optional<String> getName()
		{
			return name;
		}

		/**
		 * Extract the state rewards from a .srew file.
		 * The rewards are assumed to be of type double.
		 * @param storeReward Function to be called for each reward
		 * @param numStates Number of states in the associated model
		 */
		protected void extractStateRewards(BiConsumer<Integer, Double> storeReward, int numStates) throws PrismException
		{
			extractStateRewards(storeReward, Evaluator.forDouble(), numStates);
		}

		/**
		 * Extract the state rewards from a .srew file.
		 * The rewards are assumed to be of type Value.
		 * @param storeReward Function to be called for each reward
		 * @param eval Evaluator for Value objects
		 * @param numStates Number of states in the associated model
		 */
		protected <Value> void extractStateRewards(BiConsumer<Integer, Value> storeReward, Evaluator<Value> eval, int numStates) throws PrismException
		{
			int lineNum = 0;
			try (BufferedReader in = new BufferedReader(new FileReader(file))) {
				lineNum += skipCommentAndFirstLine(in);
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0])) {
						// Skip blank lines
						continue;
					}
					checkLineSize(record, 2, 2);
					int s = checkStateIndex(Integer.parseInt(record[0]), numStates);
					Value v = checkValue(record[1], eval);
					storeReward.accept(s, v);
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (PrismException | NumberFormatException | CsvFormatException e) {
				String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
				throw new PrismException("Error detected" + expl + " at line " + lineNum + " of state rewards file \"" + file + "\"");
			}
		}

		/**
		 * Extract the (Markov chain) transition rewards from a .trew file.
		 * The rewards are assumed to be of type double.
		 * @param storeReward Function to be called for each reward
		 * @param numStates Number of states in the associated model
		 */
		protected void extractMCTransitionRewards(IOUtils.TransitionRewardConsumer<Double> storeReward, int numStates) throws PrismException
		{
			extractMCTransitionRewards(storeReward, Evaluator.forDouble(), numStates);
		}

		/**
		 * Extract the (Markov chain) transition rewards from a .trew file.
		 * The rewards are assumed to be of type Value.
		 * @param storeReward Function to be called for each reward
		 * @param eval Evaluator for Value objects
		 * @param numStates Number of states in the associated model
		 */
		protected <Value> void extractMCTransitionRewards(IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval, int numStates) throws PrismException
		{
			int lineNum = 0;
			try (BufferedReader in = new BufferedReader(new FileReader(file))) {
				lineNum += skipCommentAndFirstLine(in);
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0])) {
						// Skip blank lines
						continue;
					}
					checkLineSize(record, 3, 3);
					int s = checkStateIndex(Integer.parseInt(record[0]), numStates);
					int s2 = checkStateIndex(Integer.parseInt(record[1]), numStates);
					Value v = checkValue(record[2], eval);
					storeReward.accept(s, s2, v);
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (PrismException | NumberFormatException | CsvFormatException e) {
				String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
				throw new PrismException("Error detected" + expl + " at line " + lineNum + " of transition rewards file \"" + file + "\"");
			}
		}

		/**
		 * Extract the (Markov decision process) transition rewards from a .trew file.
		 * The rewards are assumed to be of type double.
		 * @param storeReward Function to be called for each reward
		 * @param numStates Number of states in the associated model
		 */
		protected void extractMDPTransitionRewards(IOUtils.TransitionStateRewardConsumer<Double> storeReward, int numStates) throws PrismException
		{
			extractMDPTransitionRewards(storeReward, Evaluator.forDouble(), numStates);
		}

		/**
		 * Extract the (Markov decision process) transition rewards from a .trew file.
		 * The rewards are assumed to be of type Value.
		 * @param storeReward Function to be called for each reward
		 * @param eval Evaluator for Value objects
		 * @param numStates Number of states in the associated model
		 */
		protected <Value> void extractMDPTransitionRewards(IOUtils.TransitionStateRewardConsumer<Value> storeReward, Evaluator<Value> eval, int numStates) throws PrismException
		{
			int lineNum = 0;
			try (BufferedReader in = new BufferedReader(new FileReader(file))) {
				lineNum += skipCommentAndFirstLine(in);
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0])) {
						// Skip blank lines
						continue;
					}
					checkLineSize(record, 4, 4);
					int s = checkStateIndex(Integer.parseInt(record[0]), numStates);
					int i = checkChoiceIndex(Integer.parseInt(record[1]));
					int s2 = checkStateIndex(Integer.parseInt(record[2]), numStates);
					Value v = checkValue(record[3], eval);
					storeReward.accept(s, i, s2, v);
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (PrismException | NumberFormatException | CsvFormatException e) {
				String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
				throw new PrismException("Error detected" + expl + " at line " + lineNum + " of transition rewards file \"" + file + "\"");
			}
		}

		/**
		 * Extract the name of the state rewards structure if present.
		 *
		 * @param rewardFile a state rewards file
		 * @return name of the state rewards structure if present
		 * @throws PrismException if an I/O error occurs or the name is not a unique identifier
		 */
		protected Optional<String> extractRewardStructureName(File rewardFile) throws PrismException
		{
			int lineNum = 0;
			Optional<String> name = Optional.empty();
			try (BufferedReader in = new BufferedReader(new FileReader(rewardFile))) {
				for (String line = in.readLine(); line != null; line = in.readLine()) {
					lineNum++;
					// Process only initial comment block
					if (!COMMENT_PATTERN.matcher(line).matches()) {
						break;
					}
					// Look for reward name in header
					Matcher headerMatcher = REWARD_NAME_PATTERN.matcher(line);
					if (headerMatcher.matches()) {
						if (name.isPresent()) {
							throw new PrismException("multiple reward structure names");
						}
						// check if reward struct name is an identifier
						name = Optional.of(checkRewardName(headerMatcher.group(2)));
					}
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (PrismException e) {
				String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
				throw new PrismException("Error detected" + expl + " at line " + lineNum + " of rewards file \"" + file + "\"");
			}
			return name;
		}
	}

	/**
	 * Skip the next block of lines starting with # and the first line after.
	 *
	 * @param in reader
	 * @return number of lines read
	 * @throws IOException if an I/O error occurs
	 */
	protected static int skipCommentAndFirstLine(BufferedReader in) throws IOException
	{
		int lineNum = 0;
		String line;
		do {
			line = in.readLine();
			lineNum++;
		} while (line != null && COMMENT_PATTERN.matcher(line).matches());
		return lineNum;
	}

	// Utility method to check inputs and generate errors

	protected static void checkLineSize(String[] record, int min, int max) throws PrismException
	{
		if (record.length < min) {
			throw new PrismException("too few entries");
		}
		if (record.length > max) {
			throw new PrismException("too many entries");
		}
	}

	protected static void checkLineSize(String[] record, int min) throws PrismException
	{
		if (record.length < min) {
			throw new PrismException("too few entries");
		}
	}

	protected static int checkStateIndex(int s) throws PrismException
	{
		if (s < 0) {
			throw new PrismException("state index " + s + " is invalid");
		}
		return s;
	}

	protected static int checkStateIndex(int s, int numStates) throws PrismException
	{
		if (s < 0) {
			throw new PrismException("state index " + s + " is invalid");
		}
		if (s > numStates) {
			throw new PrismException("state index " + s + " exceeds number of states");
		}
		return s;
	}

	protected static int checkChoiceIndex(int i) throws PrismException
	{
		if (i < 0) {
			throw new PrismException("choice index " + i + " is invalid");
		}
		return i;
	}

	protected static int checkLabelIndex(String s) throws PrismException
	{
		try {
			int i = Integer.parseInt(s);
			if (i < 0) {
				throw new PrismException("label index " + i + " is invalid");
			}
			return i;
		} catch (NumberFormatException e) {
			throw new PrismException("label index \"" + s + "\" is invalid");
		}
	}

	protected static <Value> Value checkValue(String v, Evaluator<Value> eval) throws PrismException
	{
		try {
			return eval.fromString(v);
		} catch (NumberFormatException e) {
			throw new PrismException("invalid value \"" + v + "\"");
		}
	}

	protected static String checkAction(String a) throws PrismException
	{
		if (!Prism.isValidIdentifier(a)) {
			throw new PrismException("invalid action name \"" + a + "\"");
		}
		return a;
	}

	protected static String checkRewardName(String rewardStructName) throws PrismException
	{
		if (rewardStructName == null) {
			throw new PrismException("missing reward name");
		}
		if (!Prism.isValidIdentifier(rewardStructName)) {
			throw new PrismLangException("invalid reward name \"" + rewardStructName + "\"");
		}
		return rewardStructName;
	}
}
