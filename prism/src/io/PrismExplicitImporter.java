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

package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.iterable.Reducible;
import csv.BasicReader;
import csv.CsvFormatException;
import csv.CsvReader;
import param.BigRational;
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
public class PrismExplicitImporter
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

	// Num states
	private int numStates = 0;

	// Reward info extracted from files and then stored in a RewardGenerator object
	private RewardGenerator<?> rewardInfo;

	// File(s) to read in rewards from
	private List<PrismExplicitImporter.RewardFile> stateRewardsReaders = new ArrayList<>();
	private List<PrismExplicitImporter.RewardFile> transRewardsRewaders = new ArrayList<>();

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
	 * @param typeOverride Specified model type (null mean sauto-detect it, or default to MDP if that cannot be done).
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
		this.transRewardsRewaders = new ArrayList<>(this.transRewardsFiles.size());
		for (File file : this.transRewardsFiles) {
			this.transRewardsRewaders.add(new PrismExplicitImporter.RewardFile(file));
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
	 * Is there a states file?
	 */
	public boolean hasStatesFile()
	{
		return getStatesFile() != null;
	}

	/**
	 * Is there a labels file?
	 */
	public boolean hasLabelsFile()
	{
		return getLabelsFile() != null;
	}

	/**
	 * Are there any transition rewards files?
	 */
	public boolean hasTransitionRewardsFiles()
	{
		return transRewardsFiles != null && transRewardsFiles.size() > 0;
	}

	/**
	 * Get info about the model, extracted from relevant files.
	 */
	public ModelInfo getModelInfo() throws PrismException
	{
		// Construct lazily, as needed
		if (modelInfo == null) {
			buildModelInfo();
		}
		return modelInfo;
	}

	/**
	 * Get the number of states (determined from either states file or transitions file).
	 */
	public int getNumStates() throws PrismException
	{
		// Construct lazily, as needed
		if (modelInfo == null) {
			buildModelInfo();
		}
		return numStates;
	}

	/**
	 * Get a string stating the model type and how it was obtained.
	 */
	public String getModelTypeString()
	{
		return modelTypeString;
	}

	/**
	 * Get info about the rewards, extracted from relevant files.
	 */
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
	 * Can then be accessed via {@link #getModelInfo()} and {@link #getNumStates()}.
	 */
	private void buildModelInfo() throws PrismException
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
		try (BufferedReader in = new BufferedReader(new FileReader(transFile))) {
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			boolean nondet;
			// Examine first line
			// 3 numbers should indicate a nondeterministic model, e.g., MDP
			// 2 numbers should indicate a probabilistic model, e.g., DTMC
			// Anything else, give up
			if (!csv.hasNextRecord()) {
				return null;
			}
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

	/**
	 * Compute the maximum number of choices (in a nondeterministic model).
	 */
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
					continue;
				}
				if (record.length < 4 || record.length > 5) {
					throw new PrismException("");
				}
				int j = Integer.parseInt(record[1]);
				if (j + 1 > maxNumChoices) {
					maxNumChoices = j + 1;
				}
			}
			return maxNumChoices;
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\": " + e.getMessage());
		} catch (NumberFormatException | IndexOutOfBoundsException | CsvFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of transition matrix file \"" + transFile + "\"");
		} catch (PrismException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of transition matrix file \"" + transFile + "\"");
		}
	}

	/**
	 * Extract the (Markov chain) transitions from a .tra file.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param storeTransition Function to be called for each transition
	 */
	public void extractMCTransitions(IOUtils.MCTransitionConsumer<Double> storeTransition) throws PrismException
	{
		extractMCTransitions(storeTransition, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov chain) transitions from a .tra file.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param storeTransition Function to be called for each transition
	 * @param eval Evaluator for Value objects
	 */
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
					continue;
				}
				int s = Objects.checkIndex(Integer.parseInt(record[0]), numStates);
				int s2 = Objects.checkIndex(Integer.parseInt(record[1]), numStates);
				Value v = eval.fromString(record[2]);
				Object a = null;
				if (record.length > 3) {
					a = record[3];
				}
				storeTransition.accept(s, s2, v, a);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (NumberFormatException | IndexOutOfBoundsException | CsvFormatException e) {
			throw new PrismException("Error detected " + e.getMessage() + " at line " + lineNum + " of state rewards file \"" + transFile + "\"");
		}
	}

	/**
	 * Extract the (Markov decision process) transitions from a .tra file.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param storeTransition Function to be called for each transition
	 */
	public void extractMDPTransitions(IOUtils.MDPTransitionConsumer<Double> storeTransition) throws PrismException
	{
		extractMDPTransitions(storeTransition, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov decision process) transitions from a .tra file.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param storeTransition Function to be called for each transition
	 * @param eval Evaluator for Value objects
	 */
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
					continue;
				}
				int s = Objects.checkIndex(Integer.parseInt(record[0]), numStates);
				int i = Objects.checkIndex(Integer.parseInt(record[1]), numStates);
				int s2 = Objects.checkIndex(Integer.parseInt(record[2]), numStates);
				Value v = eval.fromString(record[3]);
				Object a = null;
				if (record.length > 4) {
					a = record[4];
				}
				storeTransition.accept(s, i, s2, v, a);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (NumberFormatException | IndexOutOfBoundsException | CsvFormatException e) {
			throw new PrismException("Error detected " + e.getMessage() + " at line " + lineNum + " of state rewards file \"" + transFile + "\"");
		}
	}

	/**
	 * Build/store rewards info from the input files.
	 * Can then be accessed via {@link #getRewardInfo()}.
	 */
	public void buildRewardInfo() throws PrismException
	{
		rewardInfo = new RewardGenerator<>()
		{
			@Override
			public List<String> getRewardStructNames()
			{
				return Reducible.extend(stateRewardsReaders).map(f -> f.getName().orElse("")).collect(new ArrayList<>(stateRewardsReaders.size()));
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

	/**
	 * Extract the state rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 */
	public void extractStateRewards(int rewardIndex, BiConsumer<Integer, Double> storeReward) throws PrismException
	{
		extractStateRewards(rewardIndex, storeReward, Evaluator.forDouble());
	}

	/**
	 * Extract the state rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 * @param eval Evaluator for Value objects
	 */
	public <Value> void extractStateRewards(int rewardIndex, BiConsumer<Integer, Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (rewardIndex < stateRewardsReaders.size()) {
			RewardFile file = stateRewardsReaders.get(rewardIndex);
			file.extractStateRewards(storeReward, eval, numStates);
		}
	}

	/**
	 * Extract the (Markov chain) transition rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 */
	public void extractMCTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Double> storeReward) throws PrismException
	{
		extractMCTransitionRewards(rewardIndex, storeReward, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov chain) transition rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 * @param eval Evaluator for Value objects
	 */
	public <Value> void extractMCTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (rewardIndex < transRewardsRewaders.size()) {
			RewardFile file = transRewardsRewaders.get(rewardIndex);
			file.extractMCTransitionRewards(storeReward, eval, numStates);
		}
	}

	/**
	 * Extract the (Markov decision process) transition rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 */
	public void extractMDPTransitionRewards(int rewardIndex, IOUtils.TransitionStateRewardConsumer<Double> storeReward) throws PrismException
	{
		extractMDPTransitionRewards(rewardIndex, storeReward, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov decision process) transition rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 * @param eval Evaluator for Value objects
	 */
	public <Value> void extractMDPTransitionRewards(int rewardIndex, IOUtils.TransitionStateRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (rewardIndex < transRewardsRewaders.size()) {
			RewardFile file = transRewardsRewaders.get(rewardIndex);
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
				// init csv reader
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
				// read state rewards
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0])) {
						continue;
					}
					int i = Objects.checkIndex(Integer.parseInt(record[0]), numStates);
					Value v = eval.fromString(record[1]);
					storeReward.accept(i, v);
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (NumberFormatException | IndexOutOfBoundsException | CsvFormatException e) {
				throw new PrismException("Error detected " + e.getMessage() + " at line " + lineNum + " of state rewards file \"" + file + "\"");
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
				// init csv reader
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
				// read state rewards
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0])) {
						continue;
					}
					int s = Objects.checkIndex(Integer.parseInt(record[0]), numStates);
					int s2 = Objects.checkIndex(Integer.parseInt(record[1]), numStates);
					Value v = eval.fromString(record[2]);
					storeReward.accept(s, s2, v);
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (NumberFormatException | IndexOutOfBoundsException | CsvFormatException e) {
				throw new PrismException("Error detected " + e.getMessage() + " at line " + lineNum + " of state rewards file \"" + file + "\"");
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
				// init csv reader
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
				// read state rewards
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0])) {
						continue;
					}
					int s = Objects.checkIndex(Integer.parseInt(record[0]), numStates);
					int i = Objects.checkIndex(Integer.parseInt(record[1]), numStates);
					int s2 = Objects.checkIndex(Integer.parseInt(record[2]), numStates);
					Value v = eval.fromString(record[3]);
					storeReward.accept(s, i, s2, v);
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (NumberFormatException | IndexOutOfBoundsException | CsvFormatException e) {
				throw new PrismException("Error detected " + e.getMessage() + " at line " + lineNum + " of state rewards file \"" + file + "\"");
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
			Optional<String> name = Optional.empty();
			try (BufferedReader in = new BufferedReader(new FileReader(rewardFile))) {
				int lineNum = 0;
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
							throw new PrismException("Found second reward structure names" + printFileLocation(rewardFile, lineNum));
						}
						// check if reward struct name is an identifier
						try {
							name = Optional.of(checkRewardName(headerMatcher.group(2)));
						} catch (PrismException e) {
							throw new PrismException(e.getMessage() + printFileLocation(rewardFile, lineNum));
						}
					}
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			}
			return name;
		}

		protected static String printFileLocation(File rewardFile, int linenum)
		{
			return ": line " + linenum + " in " + rewardFile;
		}

		/**
		 * Verify that the imported reward struct name is not null and is an identifier.
		 *
		 * @param rewardStructName reward struct name to be checked
		 * @throws PrismException if name is null or no identifier
		 */
		protected static String checkRewardName(String rewardStructName) throws PrismException
		{
			if (rewardStructName == null) {
				throw new PrismException("Missing reward name");
			}
			if (!Prism.isValidIdentifier(rewardStructName)) {
				throw new PrismLangException("Reward name \"" + rewardStructName + "\" is not valid");
			}
			return rewardStructName;
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
}
