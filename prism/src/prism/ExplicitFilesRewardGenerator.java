//==============================================================================
//	
//	Copyright (c) 2019-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
// 	* Ludwig Pauly <ludwigpauly@gmail.com> (TU Dresden)
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

import static csv.BasicReader.LF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
import io.IOUtils.TransitionRewardConsumer;
import io.IOUtils.TransitionStateRewardConsumer;
import parser.State;

/**
 * This abstract class implements the import and storage of state reward structures.
 *
 * It is possible to import the state rewards structure with a header.
 * Header format with optional reward struct name:
 * <pre>
 *   # Reward structure &lt;double-quoted-name&gt;
 *   # State rewards
 * </pre>
 * where &lt;double-quoted-name&gt; ("<name>") is omitted if the reward structure is not named.<br />
 * We do not enforce the correct format right now.
 */
public class ExplicitFilesRewardGenerator extends PrismComponent implements RewardGenerator<Double>
{
	// File(s) to read in rewards from
	protected List<RewardFile> stateRewardsFiles = new ArrayList<>();
	protected List<RewardFile> transRewardsFiles = new ArrayList<>();
	// Num reward structs (max of sizes of rewards file lists)
	protected int numRewardStructs;
	// Model info
	protected int numStates;
	// State list (optionally)
	protected List<State> statesList = null;

	// Regex for comments
	protected static final Pattern COMMENT_PATTERN = Pattern.compile("#.*");
	// Regex for reward name
	protected static final Pattern REWARD_NAME_PATTERN = Pattern.compile("# Reward structure (\"([_a-zA-Z0-9]*)\")$");

	/**
	 * Constructor
	 *
	 * @throws PrismException       if an I/O error occurs or the name is not a unique identifier
	 * @throws NullPointerException if a file is null
	 */
	public ExplicitFilesRewardGenerator(PrismComponent parent, List<File> stateRewardsFiles, List<File> transRewardsFiles, int numStates) throws PrismException
	{
		super(parent);
		if (numStates < 1) {
			throw new PrismException("Expected number of states > 0");
		}
		this.numStates = numStates;
		this.stateRewardsFiles = new ArrayList<>(stateRewardsFiles.size());
		for (File file : stateRewardsFiles) {
			this.stateRewardsFiles.add(new RewardFile(file));
		}
		this.transRewardsFiles = new ArrayList<>(transRewardsFiles.size());
		for (File file : transRewardsFiles) {
			this.transRewardsFiles.add(new RewardFile(file));
		}
		numRewardStructs = Math.max(stateRewardsFiles.size(), transRewardsFiles.size());
	}

	/**
	 * Optionally, provide a list of model states,
	 * so that rewards can be looked up by State object, as well as state index.
	 */
	public void setStatesList(List<State> statesList)
	{
		this.statesList = statesList;
	}

	/**
	 * Extract the state rewards for a given reward structure index.
	 *
	 * @throws PrismException if an error occurs during reward extraction
	 */
	public void extractStateRewards(int rewardIndex, BiConsumer<Integer, Double> storeReward) throws PrismException
	{
		if (rewardIndex < stateRewardsFiles.size()) {
			RewardFile file = stateRewardsFiles.get(rewardIndex);
			file.extractStateRewards(storeReward, numStates);
		}
	}

	/**
	 * Extract the (Markov chain) transition rewards for a given reward structure index.
	 *
	 * @throws PrismException if an error occurs during reward extraction
	 */
	public void extractMCTransitionRewards(int rewardIndex, TransitionRewardConsumer<Double> storeReward) throws PrismException
	{
		if (rewardIndex < transRewardsFiles.size()) {
			RewardFile file = transRewardsFiles.get(rewardIndex);
			file.extractMCTransitionRewards(storeReward, numStates);
		}
	}

	/**
	 * Extract the (MDP) transition rewards for a given reward structure index.
	 *
	 * @throws PrismException if an error occurs during reward extraction
	 */
	public void extractMDPTransitionRewards(int rewardIndex, TransitionStateRewardConsumer<Double> storeReward) throws PrismException
	{
		if (rewardIndex < transRewardsFiles.size()) {
			RewardFile file = transRewardsFiles.get(rewardIndex);
			file.extractMDPTransitionRewards(storeReward, numStates);
		}
	}

	// Methods to implement RewardGenerator

	@Override
	public List<String> getRewardStructNames()
	{
		return Reducible.extend(stateRewardsFiles).map(f -> f.getName().orElse("")).collect(new ArrayList<>(stateRewardsFiles.size()));
	}

	@Override
	public int getNumRewardStructs()
	{
		return numRewardStructs;
	}

	@Override
	public boolean rewardStructHasTransitionRewards(int r)
	{
		return false;
	}

	@Override
	public boolean isRewardLookupSupported(RewardLookup lookup)
	{
		return (lookup == RewardLookup.BY_STATE_INDEX) || (lookup == RewardLookup.BY_STATE && statesList != null);
	}

	@Override
	public Double getStateReward(int r, State state) throws PrismException
	{
		if (statesList == null) {
			throw new PrismException("Reward lookup by State not possible since state list is missing");
		}
		int s = statesList.indexOf(state);
		if (s == -1) {
			throw new PrismException("Unknown state " + state);
		}
		return getStateReward(r, s);
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
		 * Extract and store state rewards from the file.
		 *
		 * @param storeReward function to store a state reward
		 * @param numStates number of model states
		 * @throws PrismException if an I/O error occurs or the file is malformatted
		 */
		protected void extractStateRewards(BiConsumer<Integer, Double> storeReward, int numStates) throws PrismException
		{
			int lineNum = 0;
			try (BufferedReader in = new BufferedReader(new FileReader(file))) {
				lineNum += skipCommentAndFirstLine(in);
				// init csv reader
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, true, false, ' ', LF);
				// read state rewards
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0])) {
						break;
					}
					int i = Objects.checkIndex(Integer.parseInt(record[0]), numStates);
					double d = Double.parseDouble(record[1]);
					storeReward.accept(i, d);
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (NumberFormatException | IndexOutOfBoundsException | CsvFormatException e) {
				throw new PrismException("Error detected " + e.getMessage() + " at line " + lineNum + " of state rewards file \"" + file + "\"");
			}
		}

		/**
		 * Extract and store (Markov chain) transition rewards from the file.
		 *
		 * @param storeReward function to store a state reward
		 * @param numStates number of model states
		 * @throws PrismException if an I/O error occurs or the file is malformatted
		 */
		protected void extractMCTransitionRewards(TransitionRewardConsumer<Double> storeReward, int numStates) throws PrismException
		{
			int lineNum = 0;
			try (BufferedReader in = new BufferedReader(new FileReader(file))) {
				lineNum += skipCommentAndFirstLine(in);
				// init csv reader
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, true, false, ' ', LF);
				// read state rewards
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0])) {
						break;
					}
					int s = Objects.checkIndex(Integer.parseInt(record[0]), numStates);
					int s2 = Objects.checkIndex(Integer.parseInt(record[1]), numStates);
					double d = Double.parseDouble(record[2]);
					storeReward.accept(s, s2, d);
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (NumberFormatException | IndexOutOfBoundsException | CsvFormatException e) {
				throw new PrismException("Error detected " + e.getMessage() + " at line " + lineNum + " of state rewards file \"" + file + "\"");
			}
		}

		/**
		 * Extract and store (MDP) transition rewards from the file.
		 *
		 * @param storeReward function to store a state reward
		 * @param numStates number of model states
		 * @throws PrismException if an I/O error occurs or the file is malformatted
		 */
		protected void extractMDPTransitionRewards(TransitionStateRewardConsumer<Double> storeReward, int numStates) throws PrismException
		{
			int lineNum = 0;
			try (BufferedReader in = new BufferedReader(new FileReader(file))) {
				lineNum += skipCommentAndFirstLine(in);
				// init csv reader
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, true, false, ' ', LF);
				// read state rewards
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0])) {
						break;
					}
					int s = Objects.checkIndex(Integer.parseInt(record[0]), numStates);
					int i = Objects.checkIndex(Integer.parseInt(record[1]), numStates);
					int s2 = Objects.checkIndex(Integer.parseInt(record[2]), numStates);
					double d = Double.parseDouble(record[3]);
					storeReward.accept(s, i, s2, d);
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
		 * @param file a state rewards file
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

		/**
		 * Skip the next block of lines starting with # and the first line after.
		 *
		 * @param in reader
		 * @return number of lines read
		 * @throws IOException if an I/O error occurs
		 */
		protected int skipCommentAndFirstLine(BufferedReader in) throws IOException
		{
			int lineNum = 0;
			String line;
			do {
				line = in.readLine();
				lineNum++;
			} while (line != null && COMMENT_PATTERN.matcher(line).matches());
			return lineNum;
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
}