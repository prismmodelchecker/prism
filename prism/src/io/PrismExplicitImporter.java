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
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import csv.BasicReader;
import csv.CsvFormatException;
import csv.CsvReader;
import explicit.DTMCSimple;
import explicit.ModelExplicit;
import explicit.NondetModel;
import explicit.SuccessorsIterator;
import param.BigRational;
import parser.State;
import parser.ast.DeclarationBool;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import parser.ast.ExpressionIdent;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeInt;
import prism.BasicModelInfo;
import prism.BasicRewardInfo;
import prism.Evaluator;
import prism.ModelInfo;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.RewardInfo;

import static csv.BasicReader.LF;

/**
 * Class to manage importing models from PRISM explicit files (.tra, .sta, etc.)
 */
public class PrismExplicitImporter extends ExplicitModelImporter
{
	// What to import: files and type override
	private FileSection statesFile;
	private FileSection transFile;
	private FileSection observationsFile;
	private FileSection labelsFile;
	private List<File> stateRewardsFiles;
	private List<File> transRewardsFiles;
	private ModelType typeOverride;
	// Model type extracted from the transitions file header, e.g. "# Transitions (DTMC)"
	private ModelType typeFromHeader;
	// Does the transitions file store initial states info?
	private boolean transFileStoresInitialStates;

	// Model info extracted from files and then stored in a BasicModelInfo object
	private BasicModelInfo basicModelInfo;

	// String stating the model type and how it was obtained
	private String modelTypeString;

	// Model statistics (num states/choices/transitions)
	private class ModelStats
	{
		int numStates = 0;
		int numChoices = 0;
		int numTransitions = 0;
		int numObservations = 0;
	}
	private ModelStats modelStats;

	// Info about deadlocks
	private class DeadlockInfo
	{
		BitSet deadlocks = new BitSet();
		int numDeadlocks = 0;
	}
	private DeadlockInfo deadlockInfo;

	// Mapping from label indices in file to (non-built-in) label indices (also: -1=init, -2=deadlock)
	private List<Integer> labelMap;

	// Reward info extracted from files and then stored in a BasicRewardInfo object
	private BasicRewardInfo basicRewardInfo;

	// File(s) to read in rewards from
	private List<PrismExplicitImporter.RewardFile> stateRewardsReaders = new ArrayList<>();
	private List<PrismExplicitImporter.RewardFile> transRewardsReaders = new ArrayList<>();

	// Regex for comments
	protected static final Pattern COMMENT_PATTERN = Pattern.compile("#.*");
	// Regex for reward name
	protected static final Pattern REWARD_NAME_PATTERN = Pattern.compile("# Reward structure (\"([_a-zA-Z0-9]*)\")$");

	/**
	 * A view onto a (1-indexed, line-numbered) range of a file: either the whole file
	 * (the usual case, when each entity has its own separate file) or a sub-range of lines
	 * within a larger combined file (e.g. a single section of a .pexp file). Lets the rest
	 * of this class keep reading "a file" via a normal {@link BufferedReader}, whether or
	 * not it is really sharing a file with other sections.
	 */
	private static class FileSection
	{
		final File file;
		final int startLine;        // 1-indexed, inclusive: line on which this section's header begins
		final int endLineExclusive; // 1-indexed, exclusive; Integer.MAX_VALUE means "to EOF"

		/** Whole-file section (legacy/non-combined case). */
		FileSection(File file)
		{
			this(file, 1, Integer.MAX_VALUE);
		}

		FileSection(File file, int startLine, int endLineExclusive)
		{
			this.file = file;
			this.startLine = startLine;
			this.endLineExclusive = endLineExclusive;
		}

		/**
		 * Open a reader for just this section, positioned at its first line.
		 * For a whole-file section, this is just a plain {@link BufferedReader} on the file.
		 * For a bounded sub-range of a combined file, lines before {@code startLine} are
		 * skipped, and the returned reader reports EOF once {@code endLineExclusive} is reached,
		 * even though the underlying file continues.
		 */
		BufferedReader openBuffered() throws IOException
		{
			BufferedReader raw = ModelImportUnzipper.openBuffered(file);
			for (int i = 1; i < startLine; i++) {
				if (raw.readLine() == null) {
					break;
				}
			}
			if (endLineExclusive == Integer.MAX_VALUE) {
				return raw;
			}
			return new BufferedReader(new LineBoundedReader(raw, endLineExclusive - startLine));
		}

		@Override
		public String toString()
		{
			return file.toString();
		}

		/**
		 * Convert a line number local to this section (1-indexed, as tracked while
		 * reading via {@link #openBuffered()}) to the corresponding absolute line
		 * number within the underlying file, e.g. for use in error messages.
		 * For a whole-file section, this is just {@code localLineNum} unchanged.
		 */
		int toAbsoluteLine(int localLineNum)
		{
			return startLine + localLineNum - 1;
		}
	}

	/**
	 * A {@link Reader} that wraps another reader (already positioned at the start of a
	 * section) and re-serves at most {@code maxLines} more lines as a character stream,
	 * then reports EOF, regardless of how much more data the underlying reader actually has.
	 * Lines are re-emitted via {@link BufferedReader#readLine()} (so the original line-ending
	 * style of the file, which may vary by platform, does not matter) followed by a single
	 * {@code '\n'}.
	 */
	private static class LineBoundedReader extends Reader
	{
		private final BufferedReader in;
		private int linesRemaining;
		private String currentLine;
		private int posInLine;
		private boolean pendingNewline;
		private boolean eof;

		LineBoundedReader(BufferedReader in, int maxLines)
		{
			this.in = in;
			this.linesRemaining = maxLines;
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException
		{
			if (len == 0) {
				return 0;
			}
			if (eof) {
				return -1;
			}
			int written = 0;
			while (written < len) {
				if (currentLine == null) {
					if (linesRemaining <= 0) {
						eof = true;
						break;
					}
					currentLine = in.readLine();
					if (currentLine == null) {
						// Underlying file ended before the expected number of lines
						eof = true;
						break;
					}
					linesRemaining--;
					posInLine = 0;
					pendingNewline = true;
				}
				if (posInLine < currentLine.length()) {
					int toCopy = Math.min(len - written, currentLine.length() - posInLine);
					currentLine.getChars(posInLine, posInLine + toCopy, cbuf, off + written);
					posInLine += toCopy;
					written += toCopy;
				} else if (pendingNewline) {
					cbuf[off + written] = '\n';
					written++;
					pendingNewline = false;
				} else {
					// Finished this line; move on to the next
					currentLine = null;
				}
			}
			return written == 0 ? -1 : written;
		}

		@Override
		public void close() throws IOException
		{
			in.close();
		}
	}

	/**
	 * Constructor
	 * @param statesFile States file (may be {@code null})
	 * @param transFile Transitions file
	 * @param labelsFile Labels file (may be {@code null})
	 * @param stateRewardsFiles State rewards files list (can be null/empty)
	 * @param transRewardsFiles Transition rewards files list (can be null/empty)
	 * @param typeOverride Specified model type (null mean auto-detect it, or default to MDP if that cannot be done).
	 */
	public PrismExplicitImporter(File statesFile, File transFile, File labelsFile, List<File> stateRewardsFiles, List<File> transRewardsFiles, ModelType typeOverride) throws PrismException
	{
		this(typeOverride);
		setStatesFile(statesFile);
		setTransFile(transFile);
		setLabelsFile(labelsFile);
		if (stateRewardsFiles != null) {
			for (File stateRewardsFile : stateRewardsFiles) {
				addStateRewardsFile(stateRewardsFile);
			}
		}
		if (transRewardsFiles != null) {
			for (File transRewardsFile : transRewardsFiles) {
				addTransitionRewardsFile(transRewardsFile);
			}
		}
	}

	/**
	 * Constructor
	 * @param transFile Transitions file
	 * @param typeOverride Specified model type (null mean auto-detect it, or default to MDP if that cannot be done).
	 */
	public PrismExplicitImporter(File transFile, ModelType typeOverride) throws PrismException
	{
		this(typeOverride);
		setTransFile(transFile);
	}

	/**
	 * Constructor
	 * @param transFile Transitions file
	 */
	public PrismExplicitImporter(File transFile) throws PrismException
	{
		this(transFile, null);
	}

	/**
	 * Constructor
	 * @param typeOverride Specified model type (null mean auto-detect it, or default to MDP if that cannot be done).
	 */
	public PrismExplicitImporter(ModelType typeOverride) throws PrismException
	{
		this.stateRewardsFiles = new ArrayList<>();
		this.stateRewardsReaders = new ArrayList<>();
		this.transRewardsFiles = new ArrayList<>();
		this.transRewardsReaders = new ArrayList<>();
		this.typeOverride = typeOverride;
	}

	/**
	 * Set the states file.
	 * @param statesFile States file (may be {@code null})
	 */
	public void setStatesFile(File statesFile)
	{
		this.statesFile = statesFile == null ? null : new FileSection(statesFile);
	}

	/**
	 * Set the transitions file.
	 * @param transFile Transitions file
	 */
	public void setTransFile(File transFile)
	{
		this.transFile = transFile == null ? null : new FileSection(transFile);
	}

	/**
	 * Set the observations file.
	 * @param observationsFile Observations file (may be {@code null})
	 */
	public void setObservationsFile(File observationsFile)
	{
		this.observationsFile = observationsFile == null ? null : new FileSection(observationsFile);
	}

	/**
	 * Set the labels file.
	 * @param labelsFile Labels file (may be {@code null})
	 */
	public void setLabelsFile(File labelsFile)
	{
		this.labelsFile = labelsFile == null ? null : new FileSection(labelsFile);
	}

	/**
	 * Add a state rewards file.
	 * @param stateRewardsFile State rewards file
	 */
	public void addStateRewardsFile(File stateRewardsFile) throws PrismException
	{
		stateRewardsFiles.add(stateRewardsFile);
		stateRewardsReaders.add(new RewardFile(new FileSection(stateRewardsFile)));
	}

	/**
	 * Add a transition rewards file.
	 * @param transitionRewardsFile Transition rewards file
	 */
	public void addTransitionRewardsFile(File transitionRewardsFile) throws PrismException
	{
		transRewardsFiles.add(transitionRewardsFile);
		transRewardsReaders.add(new RewardFile(new FileSection(transitionRewardsFile)));
	}

	/**
	 * Get the states file (null if not used).
	 */
	public File getStatesFile()
	{
		return statesFile == null ? null : statesFile.file;
	}

	/**
	 * Get the transitions file
	 */
	public File getTransFile()
	{
		return transFile == null ? null : transFile.file;
	}

	/**
	 * Get the observations file (null if not used).
	 */
	public File getObservationsFile()
	{
		return observationsFile == null ? null : observationsFile.file;
	}

	/**
	 * Get the labels file (null if not used).
	 */
	public File getLabelsFile()
	{
		return labelsFile == null ? null : labelsFile.file;
	}

	/**
	 * Get a list of all (distinct) files being imported from.
	 * For a combined file, multiple sections share the same underlying file,
	 * so duplicates are removed.
	 */
	public List<File> getAllFiles()
	{
		LinkedHashSet<File> allFiles = new LinkedHashSet<>();
		if (transFile != null) {
			allFiles.add(transFile.file);
		}
		if (statesFile != null) {
			allFiles.add(statesFile.file);
		}
		if (observationsFile != null) {
			allFiles.add(observationsFile.file);
		}
		if (labelsFile != null) {
			allFiles.add(labelsFile.file);
		}
		if (stateRewardsFiles != null) {
			allFiles.addAll(stateRewardsFiles);
		}
		if (transRewardsFiles != null) {
			allFiles.addAll(transRewardsFiles);
		}
		return new ArrayList<>(allFiles);
	}

	/** Kinds of section recognised within a combined "explicit" model file (.pexp). */
	private enum SectionKind { TRANS, STATES, OBS, LABELS, SREW, TREW }

	/** A recognised section header found while scanning a combined file, and the line it starts on. */
	private static class Header
	{
		final SectionKind kind;
		final int startLine;

		Header(SectionKind kind, int startLine)
		{
			this.kind = kind;
			this.startLine = startLine;
		}
	}

	/**
	 * Add a single combined "explicit" model file (.pexp) to this importer, populating the
	 * transitions/states/observations/labels/rewards fields from sections within it.
	 * Sections are identified by their {@code # SectionName} header lines and may appear
	 * in any order after the first section. The transitions section is always first; if no
	 * {@code # Transitions} header is present, the whole file (or everything up to the first
	 * recognised header) is treated as the transitions section. This allows a plain
	 * header-less {@code .tra} file to be passed here when it is not known in advance whether
	 * the file is in combined ({@code .pexp}) form or not.
	 * @param pexpFile The combined explicit model file (or a plain {@code .tra} file)
	 */
	public void addCombinedFile(File pexpFile) throws PrismException
	{
		List<Header> headers = new ArrayList<>();
		int lineNum = 0;
		try (BufferedReader in = ModelImportUnzipper.openBuffered(pexpFile)) {
			String line;
			while ((line = in.readLine()) != null) {
				lineNum++;
				if (line.startsWith("# Transitions")) {
					headers.add(new Header(SectionKind.TRANS, lineNum));
				} else if (line.startsWith("# States")) {
					headers.add(new Header(SectionKind.STATES, lineNum));
				} else if (line.startsWith("# Observations")) {
					headers.add(new Header(SectionKind.OBS, lineNum));
				} else if (line.startsWith("# Labels")) {
					headers.add(new Header(SectionKind.LABELS, lineNum));
				} else if (line.startsWith("# Reward structure")) {
					int headerLine = lineNum;
					String next = in.readLine();
					if (next == null) {
						throw new PrismException("Reward structure header at line " + headerLine
								+ " of \"" + pexpFile + "\" has no body");
					}
					lineNum++;
					if (next.startsWith("# State rewards")) {
						headers.add(new Header(SectionKind.SREW, headerLine));
					} else if (next.startsWith("# Transition rewards")) {
						headers.add(new Header(SectionKind.TREW, headerLine));
					} else {
						throw new PrismException("Reward structure header at line " + headerLine
								+ " of \"" + pexpFile + "\" not followed by \"# State rewards\" or \"# Transition rewards\"");
					}
				} else if (line.startsWith("#") && isHeaderLike(line)) {
					throw new PrismException("Unexpected header at line " + lineNum
							+ " of \"" + pexpFile + "\": \"" + line + "\"");
				}
				// else: ordinary data line - ignore during this scan
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + pexpFile + "\"");
		}
		// If the file doesn't start with a "# Transitions" header (e.g. a plain .tra file),
		// treat everything from line 1 up to the first recognised header as the transitions section.
		if (headers.isEmpty() || headers.get(0).kind != SectionKind.TRANS) {
			headers.add(0, new Header(SectionKind.TRANS, 1));
		}
		for (int i = 0; i < headers.size(); i++) {
			int start = headers.get(i).startLine;
			int end = (i + 1 < headers.size()) ? headers.get(i + 1).startLine : Integer.MAX_VALUE;
			FileSection section = new FileSection(pexpFile, start, end);
			switch (headers.get(i).kind) {
				case TRANS:
					this.transFile = section;
					break;
				case STATES:
					this.statesFile = section;
					break;
				case OBS:
					this.observationsFile = section;
					break;
				case LABELS:
					this.labelsFile = section;
					break;
				case SREW:
					stateRewardsFiles.add(pexpFile);
					stateRewardsReaders.add(new RewardFile(section));
					break;
				case TREW:
					transRewardsFiles.add(pexpFile);
					transRewardsReaders.add(new RewardFile(section));
					break;
			}
		}
	}

	@Override
	public boolean providesStates()
	{
		return getStatesFile() != null;
	}

	@Override
	public boolean providesObservations()
	{
		return getObservationsFile() != null;
	}

	@Override
	public boolean providesLabels()
	{
		return getLabelsFile() != null;
	}

	/**
	 * Does the transitions file store initial states info?
	 */
	private boolean transFileProvidesInitialStates() throws PrismException
	{
		if (modelStats == null) {
			buildModelStats();
		}
		return transFileStoresInitialStates;
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
		if (basicModelInfo == null) {
			buildModelInfo();
		}
		return basicModelInfo;
	}

	@Override
	public int getNumStates() throws PrismException
	{
		// Construct lazily, as needed
	 	// (determined from either states file or transitions file)
		if (modelStats == null) {
			buildModelStats();
		}
		return modelStats.numStates;
	}

	@Override
	public int getNumChoices() throws PrismException
	{
		// Construct lazily, as needed
		// (determined from transitions file)
		if (modelStats == null) {
			buildModelStats();
		}
		int numChoices = modelStats.numChoices;
		// Add extras if deadlocks are being fixed
		if (fixdl) {
			numChoices += getNumDeadlockStates();
		}
		return numChoices;
	}

	@Override
	public int getNumTransitions() throws PrismException
	{
		// Construct lazily, as needed
		// (determined from transitions file)
		if (modelStats == null) {
			buildModelStats();
		}
		int numTransitions = modelStats.numTransitions;
		// Add extras if deadlocks are being fixed
		if (fixdl) {
			numTransitions += getNumDeadlockStates();
		}
		return numTransitions;
	}

	@Override
	public int getNumObservations() throws PrismException
	{
		// Construct lazily, as needed
		// (determined from transitions file, if needed)
		if (!getModelInfo().getModelType().partiallyObservable()) {
			return 0;
		}
		if (modelStats == null) {
			buildModelStats();
		}
		return modelStats.numObservations;
	}

	@Override
	public BitSet getDeadlockStates() throws PrismException
	{
		// Do deadlock state detection lazily, as needed
		if (deadlockInfo == null) {
			findDeadlocks();
		}
		return deadlockInfo.deadlocks;
	}

	@Override
	public int getNumDeadlockStates() throws PrismException
	{
		// Do deadlock state detection lazily, as needed
		if (deadlockInfo == null) {
			findDeadlocks();
		}
		return deadlockInfo.numDeadlocks;
	}

	@Override
	public String getModelTypeString()
	{
		return modelTypeString;
	}

	@Override
	public RewardInfo getRewardInfo() throws PrismException
	{
		// Construct lazily, as needed
		if (basicRewardInfo == null) {
			buildRewardInfo();
		}
		return basicRewardInfo;
	}

	/**
	 * Build/store model stats (from the transitions file).
	 * Can then be accessed via {@link #getNumStates()}, {@link #getNumChoices()}, {@link #getNumTransitions()}.
	 */
	private void buildModelStats() throws PrismException
	{
		// Extract model stats from header of transitions file
		extractModelStatsFromTransFile(transFile);
	}

	/**
	 * Build/store model info from the states/transitions/labels files.
	 * Can then be accessed via {@link #getModelInfo()}.
	 * Also calls {@link #buildModelStats()} if needed.
	 * Which makes available {@link #getNumStates()}, {@link #getNumChoices()}, {@link #getNumTransitions()}.
	 */
	private void buildModelInfo() throws PrismException
	{
		// Build model stats, if not already done
		if (modelStats == null) {
			buildModelStats();
		}

		// Set model type: prefer user override, then type from file header, then autodetect
		ModelType modelType;
		if (typeOverride != null) {
			modelTypeString = typeOverride + " (user-specified)";
			modelType = typeOverride;
		} else if (typeFromHeader != null) {
			modelTypeString = typeFromHeader + " (from file header)";
			modelType = typeFromHeader;
		} else {
			ModelType typeAutodetect = autodetectModelType(transFile);
			if (typeAutodetect != null) {
				modelTypeString = typeAutodetect + " (auto-detected)";
			} else {
				typeAutodetect = ModelType.MDP;
				modelTypeString = typeAutodetect + " (default)";
			}
			modelType = typeAutodetect;
		}

		// Store model info
		basicModelInfo = new BasicModelInfo(modelType);

		// Extract variable info from states, if available
		if (statesFile != null) {
			extractVarInfoFromStatesFile(statesFile);
		}
		// Otherwise store default variable info
		else {
			basicModelInfo.getVarList().addVar(defaultVariableName(), defaultVariableDeclarationType(), -1);
		}

		// Extract observable info from observations, if available
		if (modelType.partiallyObservable() && observationsFile != null) {
			extractObservableInfoFromObservationsFile(observationsFile);
		}
		// Otherwise store default observable info
		else {
			basicModelInfo.getObservableNames().add(defaultObservableName());
			basicModelInfo.getObservableTypeList().add(defaultObservableType());
		}

		// Generate and store label names from the labels file, if available.
		// This way, expressions can refer to the labels later on.
		if (labelsFile != null) {
			extractLabelNamesFromLabelsFile(labelsFile);
		} else {
			labelMap = new ArrayList<>();
		}
	}

	/**
	 * Extract variable info from a states file.
	 */
	private void extractVarInfoFromStatesFile(FileSection statesFile) throws PrismException
	{
		extractVarInfoFromFile(statesFile, ModelExportTask.ModelExportEntity.STATES);
	}

	/**
	 * Extract observable info from an observations file.
	 */
	private void extractObservableInfoFromObservationsFile(FileSection observationsFile) throws PrismException
	{
		extractVarInfoFromFile(observationsFile, ModelExportTask.ModelExportEntity.OBSERVATIONS);
	}

	/**
	 * Extract variable/observable info from a states/observations file.
	 * The info is stored in {@code basicModelInfo}, which should already exist.
	 * @param file States/observations file
	 * @param entity State or observations?
	 */
	private void extractVarInfoFromFile(FileSection file, ModelExportTask.ModelExportEntity entity) throws PrismException
	{
		String entityString = (entity == ModelExportTask.ModelExportEntity.STATES) ? "state" : "observation";
		// open file for reading, automatic close when done
		int lineNum = 0;
		String expectedHeader = (entity == ModelExportTask.ModelExportEntity.STATES) ? "# States" : "# Observations";
		try (BufferedReader in = file.openBuffered()) {
			// read first non-comment line and extract var names
			String s;
			do {
				s = in.readLine();
				lineNum++;
				if (s != null && COMMENT_PATTERN.matcher(s).matches() && isHeaderLike(s)) {
					if (!s.startsWith(expectedHeader)) {
						throw new PrismException("File does not appear to be a " + entityString + "s file"
								+ " (unexpected header: \"" + s + "\")");
					}
				}
			} while (s != null && COMMENT_PATTERN.matcher(s).matches());
			if (s == null)
				throw new PrismException("empty " + entityString + "s file");
			s = s.trim();
			if (s.charAt(0) != '(' || s.charAt(s.length() - 1) != ')')
				throw new PrismException("badly formatted " + entityString);
			s = s.substring(1, s.length() - 1);
			List<String> varNames = new ArrayList<>(Arrays.asList(s.split(",")));
			int numVars = varNames.size();
			// create arrays to (temporarily) store info about vars
			int[] varMins = new int[numVars];
			int[] varMaxs = new int[numVars];
			List<Type> varTypes = new ArrayList<>();
			// read remaining lines
			s = in.readLine();
			lineNum++;
			int counter = 0;
			while (s != null) {
				// skip blank/commented lines
				s = s.trim();
				if (s.length() > 0 && !s.startsWith("#")) {
					counter++;
					// split string
					s = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
					String[] ss = s.split(",");
					if (ss.length != numVars)
						throw new PrismException("wrong number of variables");
					// for each variable...
					for (int i = 0; i < numVars; i++) {
						// if this is the first state/observation, establish variable type
						if (counter == 1) {
							if (ss[i].equals("true") || ss[i].equals("false")) {
								varTypes.add(TypeBool.getInstance());
							} else {
								varTypes.add(TypeInt.getInstance());
							}
						}
						// check for new min/max values (ints only)
						if (varTypes.get(i) instanceof TypeInt) {
							int j = Integer.parseInt(ss[i]);
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

			if (entity == ModelExportTask.ModelExportEntity.STATES) {
				// Add variables to the VarList
				for (int i = 0; i < numVars; i++) {
					if (varTypes.get(i) instanceof TypeBool) {
						basicModelInfo.getVarList().addVar(varNames.get(i), new DeclarationBool(), -1);
					} else {
						// Note: we do not yet allow 0-range variables
						if (varMins[i] == varMaxs[i]) {
							varMaxs[i]++;
						}
						basicModelInfo.getVarList().addVar(varNames.get(i), new DeclarationInt(Expression.Int(varMins[i]), Expression.Int(varMaxs[i])), -1);
					}
				}
			} else if (entity == ModelExportTask.ModelExportEntity.OBSERVATIONS) {
				// Add observables to the model info
				for (int i = 0; i < numVars; i++) {
					basicModelInfo.getObservableNameList().add(varNames.get(i));
					basicModelInfo.getObservableTypeList().add(varTypes.get(i));
				}
			}

		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + file + "\"");
		} catch (PrismException | NumberFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + file.toAbsoluteLine(lineNum) + " of " + entityString + "s file \"" + file + "\"");
		}
	}

	/**
	 * Extract model stats (number of states/transitions) from a transitions file header.
	 */
	private void extractModelStatsFromTransFile(FileSection transFile) throws PrismException
	{
		try (BufferedReader in = transFile.openBuffered()) {
			modelStats = new ModelStats();
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			if (!csv.hasNextRecord()) {
				throw new PrismException("empty transitions file");
			}
			String[] record = csv.nextRecord();
			while (record.length > 0 && record[0].startsWith("#")) {
				String headerLine = String.join(" ", record);
				if (isHeaderLike(headerLine)) {
					if (!headerLine.startsWith("# Transitions")) {
						throw new PrismException("File does not appear to be a transitions file"
								+ " (unexpected header: \"" + headerLine + "\")");
					}
				}
				if (headerLine.startsWith("# Transitions (") && headerLine.endsWith(")")) {
					String typeName = headerLine.substring("# Transitions (".length(), headerLine.length() - 1);
					typeFromHeader = ModelType.parseName(typeName);
					if (typeFromHeader == null) {
						throw new PrismException("Unrecognised model type \"" + typeName + "\" in transitions file header");
					}
				}
				if (!csv.hasNextRecord()) {
					throw new PrismException("empty transitions file");
				}
				record = csv.nextRecord();
			}
			checkLineSize(record, 2, 4);
			modelStats.numStates = Integer.parseInt(record[0]);
			if (record.length == 2) {
				// Markov chain
				modelStats.numChoices = modelStats.numStates;
				modelStats.numTransitions = Integer.parseInt(record[1]);
			} else if (record.length == 3) {
				// MDP/LTS
				modelStats.numChoices = Integer.parseInt(record[1]);
				modelStats.numTransitions = Integer.parseInt(record[2]);
			} else {
				// POMDP
				modelStats.numChoices = Integer.parseInt(record[1]);
				modelStats.numTransitions = Integer.parseInt(record[2]);
				modelStats.numObservations = Integer.parseInt(record[3]);
			}
			// Also peek at the next line to see if initial states info is provided
			if (csv.hasNextRecord()) {
				record = csv.nextRecord();
				if (record.length >= 1 && record[0].equals("-")) {
					transFileStoresInitialStates = true;
				}
			}
		} catch (IOException e) {
			modelStats = null;
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			modelStats = null;
			int lineNum = 1;
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + transFile.toAbsoluteLine(lineNum) + " of transitions file \"" + transFile + "\"");
		}
	}

	/**
	 * Extract names of labels from the labels file.
	 * The "init" and "deadlock" labels are skipped, as they have special
	 * meaning and are implicitly defined for all models.
	 * The info is stored in {@code basicModelInfo} and {@code labelMap}.
	 * {@code basicModelInfo} would usually already exist but is created if not
	 * (this is only really for the case where this class is extracting labels in isolation);
	 * {@code labelMap} is created by this method.
	 */
	private void extractLabelNamesFromLabelsFile(FileSection labelsFile) throws PrismException
	{
		int lineNum = 0;
		try (BufferedReader in = labelsFile.openBuffered()) {
			// Read/parse first non-comment line (label names)
			// Looks like, e.g.: 0="init" 1="deadlock" 2="heads" 3="tails" 4="end"
			String labelsString;
			do {
				labelsString = in.readLine();
				lineNum++;
				if (labelsString != null && COMMENT_PATTERN.matcher(labelsString).matches() && isHeaderLike(labelsString)) {
					if (!labelsString.startsWith("# Labels")) {
						throw new PrismException("File does not appear to be a labels file"
								+ " (unexpected header: \"" + labelsString + "\")");
					}
				}
			} while (labelsString != null && COMMENT_PATTERN.matcher(labelsString).matches());
			Pattern label = Pattern.compile("(\\d+)=\"([^\"]+)\"\\s*");
			Matcher matcher = label.matcher(labelsString);
			if (basicModelInfo == null) {
				basicModelInfo = new BasicModelInfo();
			}
			List<String> labelNames = basicModelInfo.getLabelNameList();
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
			throw new PrismException("Error detected" + expl + " at line " + labelsFile.toAbsoluteLine(lineNum) + " of labels file \"" + labelsFile + "\"");
		}
	}

	/**
	 * Autodetect the model type based on a sample of the lines from a transitions file.
	 * If not possible, return null;
	 * @param transFile transitions file
	 */
	private ModelType autodetectModelType(FileSection transFile)
	{
		try (BufferedReader in = transFile.openBuffered()) {
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			boolean nondet;
			boolean partObs;
			boolean nonprob;
			// Examine first line
			// 3 numbers should indicate a nondeterministic model, e.g., MDP
			// 2 numbers should indicate a probabilistic model, e.g., DTMC
			// Anything else, give up
			if (!csv.hasNextRecord()) {
				return null;
			}
			// Skip comment lines, then examine the stats line
			String[] recordFirst = csv.nextRecord();
			while (recordFirst.length > 0 && recordFirst[0].startsWith("#")) {
				if (!csv.hasNextRecord()) {
					return null;
				}
				recordFirst = csv.nextRecord();
			}
			// Detect if model is nondeterministic
			if (recordFirst.length == 4) {
				nondet = true;
				partObs = true;
			} else if (recordFirst.length == 3) {
				nondet = true;
				partObs = false;
			} else if (recordFirst.length == 2) {
				nondet = false;
				partObs = false;
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
				// Skip blank/commented lines or initial states lines
				if ("".equals(record[0]) || record[0].startsWith("#") || "-".equals(record[0])) {
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
			}
			// All non-rates seen: guess (PO)MDP/DTMC
			return nondet ? (partObs ? ModelType.POMDP : ModelType.MDP) : ModelType.DTMC;
		} catch (NumberFormatException | CsvFormatException | IOException e) {
			return null;
		}
	}

	/**
	 * Traverse the transitions file to detect any deadlock states
	 * and then store the details in deadlockInfo.
	 */
	private void findDeadlocks() throws PrismException
	{
		// Record which states have transitions
		BitSet statesWithTransitions = new BitSet();
		int lineNum = 0;
		try (BufferedReader in = transFile.openBuffered()) {
			lineNum += skipAndValidateHeader(in, "# Transitions", "transitions file");
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			for (String[] record : csv) {
				lineNum++;
				// Skip blank/commented lines or initial states lines
				if ("".equals(record[0]) || record[0].startsWith("#") || "-".equals(record[0])) {
					continue;
				}
				// Lines should be 3-6 long (LTS/MDP/POMDP with/without actions)
				checkLineSize(record, 3, 6);
				// Extract/store source state
				int s = checkStateIndex(Integer.parseInt(record[0]), modelStats.numStates);
				statesWithTransitions.set(s);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\": " + e.getMessage());
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + transFile.toAbsoluteLine(lineNum) + " of transitions file \"" + transFile + "\"");
		}
		// Store deadlock info
		deadlockInfo = new DeadlockInfo();
		if (statesWithTransitions.cardinality() != modelStats.numStates) {
			for (int s = statesWithTransitions.nextClearBit(0); s < modelStats.numStates; s = statesWithTransitions.nextClearBit(s + 1)) {
				deadlockInfo.deadlocks.set(s);
				deadlockInfo.numDeadlocks++;
			}
		}
	}

	@Override
	public void extractStates(IOUtils.StateDefnConsumer storeStateDefn) throws PrismException
	{
		// If there is no info, just assume that states comprise a single integer value
		if (getStatesFile() == null) {
			super.extractStates(storeStateDefn);
			return;
		}
		// Otherwise extract from .sta file
		extractStateDefinitions(statesFile, getModelInfo().getNumVars(), storeStateDefn, "# States");
	}

	@Override
	public void extractObservationDefinitions(IOUtils.StateDefnConsumer storeObservationDefn) throws PrismException
	{
		// If there is no info, just assume that observations comprise a single integer observable
		if (getObservationsFile() == null) {
			super.extractObservationDefinitions(storeObservationDefn);
			return;
		}
		// Otherwise extract from .obs file
		extractStateDefinitions(observationsFile, getModelInfo().getNumObservables(), storeObservationDefn, "# Observations");
	}

	/**
	 * Extract state definitions from a states/observables (.sta/.obs) file.
	 * Calls {@code storeStateDefn(s, i, v)} for each state s, variable (index) i and variable value v.
	 * @param file States/observations  (.sta/.obs) file
	 * @param numVars Number of variables/observables
	 * @param storeStateDefn Consumer to store state/observation definitions
	 */
	private void extractStateDefinitions(FileSection file, int numVars, IOUtils.StateDefnConsumer storeStateDefn, String expectedHeader) throws PrismException
	{
		int lineNum = 0;
		try (BufferedReader in = file.openBuffered()) {
			lineNum += skipAndValidateHeader(in, expectedHeader, expectedHeader.replace("# ", "") + " file");
			String st = in.readLine();
			lineNum++;
			while (st != null) {
				st = st.trim();
				if (!st.isEmpty() && !st.startsWith("#")) {
					// Split into two parts
					String[] ss = st.split(":");
					// Determine which state this line describes
					int s = checkStateIndex(Integer.parseInt(ss[0]), modelStats.numStates);
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
			throw new PrismException("File I/O error reading from \"" + file + "\"");
		} catch (PrismException | NumberFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + file.toAbsoluteLine(lineNum) + " of states file \"" + file + "\"");
		}
	}

	@Override
	public int computeMaxNumChoices() throws PrismException
	{
		int lineNum = 0;
		int maxNumChoices = 0;
		try (BufferedReader in = transFile.openBuffered()) {
			lineNum += skipAndValidateHeader(in, "# Transitions", "transitions file");
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			for (String[] record : csv) {
				lineNum++;
				// Skip blank/commented lines or initial states lines
				if ("".equals(record[0]) || record[0].startsWith("#") || "-".equals(record[0])) {
					continue;
				}
				// Lines should be 3-5 long (LTS/MDP/POMDP with/without actions)
				checkLineSize(record, 3, 6);
				int j = checkChoiceIndex(Integer.parseInt(record[1]));
				if (j + 1 > maxNumChoices) {
					maxNumChoices = j + 1;
				}
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\": " + e.getMessage());
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + transFile.toAbsoluteLine(lineNum) + " of transitions file \"" + transFile + "\"");
		}
		if (fixdl && getNumDeadlockStates() > 0) {
			maxNumChoices = Math.max(maxNumChoices, 1);
		}
		return maxNumChoices;
	}

	@Override
	public <Value> void extractMCTransitions(IOUtils.MCTransitionConsumer<Value> storeTransition, Evaluator<Value> eval) throws PrismException
	{
		BitSet deadlocks = new BitSet();
		int nextDeadlock = -1;
		if (fixdl) {
			deadlocks = getDeadlockStates();
			nextDeadlock = deadlocks.nextSetBit(0);
		}
		int lineNum = 0;
		try (BufferedReader in = transFile.openBuffered()) {
			lineNum += skipAndValidateHeader(in, "# Transitions", "transitions file");
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			for (String[] record : csv) {
				lineNum++;
				// Skip blank/commented lines or initial states lines
				if ("".equals(record[0]) || record[0].startsWith("#") || "-".equals(record[0])) {
					continue;
				}
				checkLineSize(record, 3, 4);
				int s = checkStateIndex(Integer.parseInt(record[0]), modelStats.numStates);
				int s2 = checkStateIndex(Integer.parseInt(record[1]), modelStats.numStates);
				Value v = checkValue(record[2], eval);
				Object a = (record.length > 3) ? checkAction(record[3]) : null;
				// Add self-loops for any deadlock states before s
				while (nextDeadlock != -1 && nextDeadlock < s) {
					storeTransition.accept(nextDeadlock, nextDeadlock, eval.one(), null);
					nextDeadlock = deadlocks.nextSetBit(nextDeadlock + 1);
				}
				// Add transition
				storeTransition.accept(s, s2, v, a);
			}
			// Add self-loops for any remaining deadlock states
			while (nextDeadlock != -1) {
				storeTransition.accept(nextDeadlock, nextDeadlock, eval.one(), null);
				nextDeadlock = deadlocks.nextSetBit(nextDeadlock + 1);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + transFile.toAbsoluteLine(lineNum) + " of transitions file \"" + transFile + "\"");
		}
	}

	@Override
	public <Value> void extractMDPTransitions(IOUtils.MDPTransitionConsumer<Value> storeTransition, Evaluator<Value> eval) throws PrismException
	{
		BitSet deadlocks = new BitSet();
		int nextDeadlock = -1;
		if (fixdl) {
			deadlocks = getDeadlockStates();
			nextDeadlock = deadlocks.nextSetBit(0);
		}
		int lineNum = 0;
		try (BufferedReader in = transFile.openBuffered()) {
			lineNum += skipAndValidateHeader(in, "# Transitions", "transitions file");
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			for (String[] record : csv) {
				lineNum++;
				// Skip blank/commented lines or initial states lines
				if ("".equals(record[0]) || record[0].startsWith("#") || "-".equals(record[0])) {
					continue;
				}
				// Lines should be 4-6 long (MDP/POMDP with/without actions)
				checkLineSize(record, 4, 6);
				int s = checkStateIndex(Integer.parseInt(record[0]), modelStats.numStates);
				int i = checkChoiceIndex(Integer.parseInt(record[1]));
				int s2 = checkStateIndex(Integer.parseInt(record[2]), modelStats.numStates);
				Value v = checkValue(record[3], eval);
				int actIndex = getModelInfo().getModelType().partiallyObservable() ? 5 : 4;
				Object a = (record.length > actIndex) ? checkAction(record[actIndex]) : null;
				// Add self-loops for any deadlock states before s
				while (nextDeadlock != -1 && nextDeadlock < s) {
					storeTransition.accept(nextDeadlock, 0, nextDeadlock, eval.one(), null);
					nextDeadlock = deadlocks.nextSetBit(nextDeadlock + 1);
				}
				// Add transition
				storeTransition.accept(s, i, s2, v, a);
			}
			// Add self-loops for any remaining deadlock states
			while (nextDeadlock != -1) {
				storeTransition.accept(nextDeadlock, 0, nextDeadlock, eval.one(), null);
				nextDeadlock = deadlocks.nextSetBit(nextDeadlock + 1);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + transFile.toAbsoluteLine(lineNum) + " of transitions file \"" + transFile + "\"");
		}
	}

	@Override
	public void extractLTSTransitions(IOUtils.LTSTransitionConsumer storeTransition) throws PrismException
	{
		BitSet deadlocks = new BitSet();
		int nextDeadlock = -1;
		if (fixdl) {
			deadlocks = getDeadlockStates();
			nextDeadlock = deadlocks.nextSetBit(0);
		}
		int lineNum = 0;
		try (BufferedReader in = transFile.openBuffered()) {
			lineNum += skipAndValidateHeader(in, "# Transitions", "transitions file");
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			for (String[] record : csv) {
				lineNum++;
				// Skip blank/commented lines or initial states lines
				if ("".equals(record[0]) || record[0].startsWith("#") || "-".equals(record[0])) {
					continue;
				}
				checkLineSize(record, 3, 4);
				int s = checkStateIndex(Integer.parseInt(record[0]), modelStats.numStates);
				int i = checkChoiceIndex(Integer.parseInt(record[1]));
				int s2 = checkStateIndex(Integer.parseInt(record[2]), modelStats.numStates);
				Object a = (record.length > 3) ? checkAction(record[3]) : null;
				// Add self-loops for any deadlock states before s
				while (nextDeadlock != -1 && nextDeadlock < s) {
					storeTransition.accept(nextDeadlock, 0, nextDeadlock, null);
					nextDeadlock = deadlocks.nextSetBit(nextDeadlock + 1);
				}
				// Add transition
				storeTransition.accept(s, i, s2, a);
			}
			// Add self-loops for any remaining deadlock states
			while (nextDeadlock != -1) {
				storeTransition.accept(nextDeadlock, 0, nextDeadlock, null);
				nextDeadlock = deadlocks.nextSetBit(nextDeadlock + 1);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + transFile.toAbsoluteLine(lineNum) + " of transitions file \"" + transFile + "\"");
		}
	}

	@Override
	public void extractLabelsAndInitialStates(BiConsumer<Integer, Integer> storeLabel, Consumer<Integer> storeInit, Consumer<Integer> storeDeadlock) throws PrismException
	{
		// Extract any initial states info from the .tra file first
		BitSet initialStatesTra = new BitSet();
		if (transFileProvidesInitialStates()) {
			extractInitialStatesFromTransFile(initialStatesTra::set);
		}

		// If there is no .lab file, we are done; just store initial states
		// Assume that 0 is the initial state in the absence of any other info
		if (getLabelsFile() == null) {
			if (transFileProvidesInitialStates()) {
				for (int s = initialStatesTra.nextSetBit(0); s >= 0; s = initialStatesTra.nextSetBit(s + 1)) {
					storeInit.accept(s);
				}
			} else {
				storeInit.accept(0);
			}
			return;
		}

		// Otherwise extract from .lab file
		BitSet initialStatesLab = new BitSet();
		int lineNum = 0;
		try (BufferedReader in = labelsFile.openBuffered()) {
			// Skip first file (label names extracted earlier with model info)
			lineNum += skipAndValidateHeader(in, "# Labels", "labels file");
			String st = in.readLine();
			while (st != null) {
				// Skip blank/commented lines
				st = st.trim();
				if (!st.isEmpty() && !st.startsWith("#")) {
					// Split line
					String[] ss = st.split(":");
					int s = checkStateIndex(Integer.parseInt(ss[0].trim()), modelStats.numStates);
					ss = ss[1].trim().split(" ");
					for (int j = 0; j < ss.length; j++) {
						if (ss[j].isEmpty()) {
							continue;
						}
						// Store label info
						int i = checkLabelIndex(ss[j]);
						int l = labelMap.get(i);
						if (l == -2) {
							if (storeDeadlock != null) {
								storeDeadlock.accept(s);
							}
						} else if (l == -1) {
							initialStatesLab.set(s);
						} else if (l > -1) {
							storeLabel.accept(s, l);
						}
					}
				}
				// Prepare for next iter
				st = in.readLine();
			}

			// If initial states were provided in .tra file, we check for consistency with .lab file
			if (transFileProvidesInitialStates()) {
				if (!initialStatesTra.equals(initialStatesLab)) {
					throw new PrismException("Inconsistent initial states information between transitions and labels files");
				}
			}

			// Finally, store initial states
			for (int s = initialStatesLab.nextSetBit(0); s >= 0; s = initialStatesLab.nextSetBit(s + 1)) {
				storeInit.accept(s);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + labelsFile + "\"");
		} catch (PrismException | NumberFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + labelsFile.toAbsoluteLine(lineNum) + " of labels file \"" + labelsFile + "\"");
		}
	}

	@Override
	public void extractObservations(IOUtils.StateIntConsumer storeObservation) throws PrismException
	{
		// Skip this if model is not partially observable
		if (!getModelInfo().getModelType().partiallyObservable()) {
			return;
		}

		// Extract observations from transitions file
		// Temporarily store in an array to check for conflicting info
		int observations[] = new int[modelStats.numStates];
		Arrays.fill(observations, -1);
		int lineNum = 0;
		try (BufferedReader in = transFile.openBuffered()) {
			lineNum += skipAndValidateHeader(in, "# Transitions", "transitions file");
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			for (String[] record : csv) {
				lineNum++;
				if ("".equals(record[0]) || record[0].startsWith("#")) {
					// Skip blank/commented lines
					continue;
				}
				// Lines should be 5-6 long (POMDP with/without actions)
				checkLineSize(record, 5, 6);
				int s2 = checkStateIndex(Integer.parseInt(record[2]), modelStats.numStates);
				int o = checkStateIndex(Integer.parseInt(record[4]), modelStats.numStates);
				// Check/store observation
				if (observations[s2] != -1 && observations[s2] != o) {
					throw new PrismException("Conflicting observation information for state " + s2);
				}
				observations[s2] = o;
			}
			// Finally, store observations in the model
			for (int s = 0; s < modelStats.numStates; s++) {
				if (observations[s] == -1) {
					throw new PrismException("No observation information for state " + s);
				} else {
					storeObservation.accept(s, observations[s]);
				}
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + transFile.toAbsoluteLine(lineNum) + " of transitions file \"" + transFile + "\"");
		}
	}

	/**
	 * Extract any info about initial states stored in the .tra file.
	 * Calls {@code storeInit(s)} for each initial state s.
	 * @param storeInit Function to be called for each initial state
	 */
	private void extractInitialStatesFromTransFile(Consumer<Integer> storeInit) throws PrismException
	{
		ModelType modelType = getModelInfo().getModelType();
		int stateField = modelType.nondeterministic() ? 2 : 1;
		int lineNum = 0;
		try (BufferedReader in = transFile.openBuffered()) {
			lineNum += skipAndValidateHeader(in, "# Transitions", "transitions file");
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
			for (String[] record : csv) {
				lineNum++;
				// Skip blank/commented lines
				if ("".equals(record[0]) || record[0].startsWith("#")) {
					continue;
				}
				checkLineSize(record, 3, 6);
				// Break as soon as a non-initial states line is found
				if (!"-".equals(record[0])) {
					break;
				}
				int s2 = checkStateIndex(Integer.parseInt(record[stateField]), modelStats.numStates);
				storeInit.accept(s2);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (PrismException | NumberFormatException | CsvFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + transFile.toAbsoluteLine(lineNum) + " of transitions file \"" + transFile + "\"");
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
		try (BufferedReader in = labelsFile.openBuffered()) {
			// Skip first file (label names extracted earlier)
			lineNum += skipAndValidateHeader(in, "# Labels", "labels file");
			String st = in.readLine();
			while (st != null) {
				// Skip blank/commented lines
				st = st.trim();
				if (!st.isEmpty() && !st.startsWith("#")) {
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
					map.put(basicModelInfo.getLabelNameList().get(l), bitsets[i]);
				}
			}
			return map;
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + labelsFile + "\"");
		} catch (PrismException | NumberFormatException e) {
			String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
			throw new PrismException("Error detected" + expl + " at line " + labelsFile.toAbsoluteLine(lineNum) + " of labels file \"" + labelsFile + "\"");
		}
	}

	/**
	 * Build/store rewards info from the input files.
	 * Can then be accessed via {@link #getRewardInfo()}.
	 */
	private void buildRewardInfo() throws PrismException
	{
		basicRewardInfo = new BasicRewardInfo();
		int numRewards = Math.max(stateRewardsReaders.size(), transRewardsReaders.size());
		for (int r = 0; r < numRewards; r++) {
			String stateRewardName = null;
			String transRewardName = null;
			if (r < stateRewardsReaders.size()) {
				stateRewardName = stateRewardsReaders.get(r).getName().orElse("");
			}
			if (r < transRewardsReaders.size()) {
				transRewardName = transRewardsReaders.get(r).getName().orElse("");
			}
			if (transRewardName != null && stateRewardName != null && !transRewardName.equals(stateRewardName)) {
				throw new PrismException("Reward structure names do not match for state/transition rewards");
			}
			basicRewardInfo.addReward(stateRewardName != null ? stateRewardName : transRewardName);
			basicRewardInfo.setHasStateRewards(r, r < stateRewardsReaders.size());
			basicRewardInfo.setHasTransitionRewards(r, r < transRewardsReaders.size());
		}
	}

	@Override
	public <Value> void extractStateRewards(int rewardIndex, BiConsumer<Integer, Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (rewardIndex < stateRewardsReaders.size()) {
			RewardFile file = stateRewardsReaders.get(rewardIndex);
			file.extractStateRewards(storeReward, eval);
		}
	}

	@Override
	public <Value> void extractMCTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (rewardIndex < transRewardsReaders.size()) {
			RewardFile file = transRewardsReaders.get(rewardIndex);
			file.extractMCTransitionRewards(storeReward, eval);
		}
	}

	@Override
	public <Value> void extractMDPTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (rewardIndex < transRewardsReaders.size()) {
			RewardFile file = transRewardsReaders.get(rewardIndex);
			file.extractMDPTransitionRewards(storeReward, eval);
		}
	}

	public class RewardFile
	{
		protected final FileSection file;
		protected final Optional<String> name;

		public RewardFile(FileSection file) throws PrismException
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
		 */
		protected void extractStateRewards(BiConsumer<Integer, Double> storeReward) throws PrismException
		{
			extractStateRewards(storeReward, Evaluator.forDouble());
		}

		/**
		 * Extract the state rewards from a .srew file.
		 * The rewards are assumed to be of type Value.
		 * @param storeReward Function to be called for each reward
		 * @param eval Evaluator for Value objects
		 */
		protected <Value> void extractStateRewards(BiConsumer<Integer, Value> storeReward, Evaluator<Value> eval) throws PrismException
		{
			int lineNum = 0;
			try (BufferedReader in = file.openBuffered()) {
				lineNum += skipAndValidateRewardKindHeader(in, "# State rewards", "state rewards file");
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0]) || record[0].startsWith("#")) {
						// Skip blank/commented lines
						continue;
					}
					checkLineSize(record, 2, 2);
					int s = checkStateIndex(Integer.parseInt(record[0]), modelStats.numStates);
					Value v = checkValue(record[1], eval);
					storeReward.accept(s, v);
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (PrismException | NumberFormatException | CsvFormatException e) {
				String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
				throw new PrismException("Error detected" + expl + " at line " + file.toAbsoluteLine(lineNum) + " of state rewards file \"" + file + "\"");
			}
		}

		/**
		 * Extract the (Markov chain) transition rewards from a .trew file.
		 * The rewards are assumed to be of type double.
		 * @param storeReward Function to be called for each reward
		 */
		protected void extractMCTransitionRewards(IOUtils.TransitionRewardConsumer<Double> storeReward) throws PrismException
		{
			extractMCTransitionRewards(storeReward, Evaluator.forDouble());
		}

		/**
		 * Extract the (Markov chain) transition rewards from a .trew file.
		 * The rewards are assumed to be of type Value.
		 * @param storeReward Function to be called for each reward
		 * @param eval Evaluator for Value objects
		 */
		protected <Value> void extractMCTransitionRewards(IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException
		{
			// Check that we have access to a model if needed for transition indexing
			// If not, we build one via this importer
			if (transitionRewardIndexing == TransitionRewardIndexing.OFFSET && modelLookup == null) {
				modelLookup = new DTMCSimple<>();
				((ModelExplicit) modelLookup).setEvaluator(eval);
				((ModelExplicit) modelLookup).buildFromExplicitImport(PrismExplicitImporter.this);
			}

			int lineNum = 0;
			try (BufferedReader in = file.openBuffered()) {
				lineNum += skipAndValidateRewardKindHeader(in, "# Transition rewards", "transition rewards file");
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0]) || record[0].startsWith("#")) {
						// Skip blank/commented lines
						continue;
					}
					checkLineSize(record, 3, 3);
					int s = checkStateIndex(Integer.parseInt(record[0]), modelStats.numStates);
					int s2 = checkStateIndex(Integer.parseInt(record[1]), modelStats.numStates);
					Value v = checkValue(record[2], eval);

					switch (transitionRewardIndexing) {
						case STATE:
							storeReward.accept(s, s2, v);
							break;
						case OFFSET:
							// Need to look up transition offset from successor state
							SuccessorsIterator it = modelLookup.getSuccessors(s);
							int i = 0;
							while (it.hasNext()) {
								if (it.nextInt() == s2) {
									storeReward.accept(s, i, v);
									break;
								}
								i++;
							}
							if (i > modelLookup.getNumTransitions(s)) {
								throw new PrismException("No matching transition for transition reward " + s + "->" + s2);
							}
							break;
						default:
							throw new PrismException("Unknown transition reward indexing " + transitionRewardIndexing);
					}
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (PrismException | NumberFormatException | CsvFormatException e) {
				String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
				throw new PrismException("Error detected" + expl + " at line " + file.toAbsoluteLine(lineNum) + " of transition rewards file \"" + file + "\"");
			}
		}

		/**
		 * Extract the (Markov decision process) transition rewards from a .trew file.
		 * The rewards are assumed to be of type double.
		 * @param storeReward Function to be called for each reward
		 */
		protected void extractMDPTransitionRewards(IOUtils.TransitionRewardConsumer<Double> storeReward) throws PrismException
		{
			extractMDPTransitionRewards(storeReward, Evaluator.forDouble());
		}

		/**
		 * Extract the (Markov decision process) transition rewards from a .trew file.
		 * The rewards are assumed to be of type Value.
		 * @param storeReward Function to be called for each reward
		 * @param eval Evaluator for Value objects
		 */
		protected <Value> void extractMDPTransitionRewards(IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException
		{
			int lineNum = 0;
			try (BufferedReader in = file.openBuffered()) {
				lineNum += skipAndValidateRewardKindHeader(in, "# Transition rewards", "transition rewards file");
				BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, false, false, false, ' ', LF);
				int count = 0;
				int sLast = -1;
				int iLast = -1;
				Value vLast = null;
				for (String[] record : csv) {
					lineNum++;
					if ("".equals(record[0]) || record[0].startsWith("#")) {
						// Skip blank/commented lines
						continue;
					}
					checkLineSize(record, 4, 4);
					int s = checkStateIndex(Integer.parseInt(record[0]), modelStats.numStates);
					int i = checkChoiceIndex(Integer.parseInt(record[1]));
					int s2 = checkStateIndex(Integer.parseInt(record[2]), modelStats.numStates);
					Value v = checkValue(record[3], eval);
					// Check that transition rewards for the same state/choice are the same
					// (currently no support for state-choice-state rewards)
					if (s == sLast && i == iLast) {
						if (!eval.equals(vLast, v)) {
							throw new PrismException("mismatching transition rewards " + vLast + " and " + v + " in choice " + i + " of state " + s);
						}
					}
					// If possible, check that were rewards on all successors for each choice
					// (for speed, we just check that the right number were present)
					// For now, don't bother to check that the reward is the same for all s2
					// for a given state s and index i (so the first one in the file will define it)
					else {
						if (modelLookup != null && modelLookup instanceof NondetModel && sLast != -1 && count != ((NondetModel<?>) modelLookup).getNumTransitions(sLast, iLast)) {
							throw new PrismException("wrong number of transition rewards in choice " + iLast + " of state " + sLast);
						}
						sLast = s;
						iLast = i;
						vLast = v;
						count = 0;
					}
					// Only store the reward for the first instance of state-choice (s,i)
					if (count == 0) {
						storeReward.accept(s, i, v);
					}
					count++;
				}
			} catch (IOException e) {
				throw new PrismException("File I/O error reading from \"" + file + "\"");
			} catch (PrismException | NumberFormatException | CsvFormatException e) {
				String expl = (e.getMessage() == null || e.getMessage().isEmpty()) ? "" : (" (" + e.getMessage() + ")");
				throw new PrismException("Error detected" + expl + " at line " + file.toAbsoluteLine(lineNum) + " of transition rewards file \"" + file + "\"");
			}
		}

		/**
		 * Extract the name of the state rewards structure if present.
		 *
		 * @param rewardFile a state rewards file
		 * @return name of the state rewards structure if present
		 * @throws PrismException if an I/O error occurs or the name is not a unique identifier
		 */
		protected Optional<String> extractRewardStructureName(FileSection rewardFile) throws PrismException
		{
			int lineNum = 0;
			Optional<String> name = Optional.empty();
			try (BufferedReader in = rewardFile.openBuffered()) {
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
				throw new PrismException("Error detected" + expl + " at line " + file.toAbsoluteLine(lineNum) + " of rewards file \"" + file + "\"");
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

	/**
	 * Returns true if {@code line} looks like a section header that was meant to be
	 * recognised by the importer. This matches single-word headers such as
	 * {@code # States} or a typo like {@code # State} (text after the space consists
	 * only of letters and parentheses, no numbers, punctuation or embedded spaces),
	 * as well as the specific multi-word headers actually produced by the exporter
	 * ({@code # Transitions (MDP)}, {@code # Reward structure "name"},
	 * {@code # State rewards}, {@code # Transition rewards}), while leaving ordinary
	 * commented-out data lines (e.g. {@code # 3 5 0.5}) and other multi-word comments
	 * silently ignored.
	 */
	private static boolean isHeaderLike(String line)
	{
		if (line.matches("# [A-Za-z()]+ *")) {
			return true;
		}
		return line.matches("# (?:Transitions \\([A-Za-z]+\\)|Reward structure(?: \"[^\"]*\")?|State rewards|Transition rewards) *");
	}

	/**
	 * Like {@link #skipCommentAndFirstLine(BufferedReader)}, but validates any comment
	 * line that {@link #isHeaderLike looks like a section header}: it must start with
	 * {@code expectedPrefix}. Plain commented-out data lines are skipped without
	 * validation.
	 */
	protected static int skipAndValidateHeader(BufferedReader in, String expectedPrefix, String fileDescription)
			throws IOException, PrismException
	{
		int lineNum = 0;
		String line;
		do {
			line = in.readLine();
			lineNum++;
			if (line != null && COMMENT_PATTERN.matcher(line).matches() && isHeaderLike(line)) {
				if (!line.startsWith(expectedPrefix)) {
					throw new PrismException("File does not appear to be a " + fileDescription
							+ " (unexpected header: \"" + line + "\")");
				}
			}
		} while (line != null && COMMENT_PATTERN.matcher(line).matches());
		return lineNum;
	}

	/**
	 * Like {@link #skipCommentAndFirstLine(BufferedReader)}, but validates the reward
	 * section header: any comment line that {@link #isHeaderLike looks like a section
	 * header} is checked — the first such line must start with {@code "# Reward structure"}
	 * and the second must start with {@code kindPrefix}
	 * (either {@code "# State rewards"} or {@code "# Transition rewards"}).
	 */
	protected static int skipAndValidateRewardKindHeader(BufferedReader in, String kindPrefix, String fileDescription)
			throws IOException, PrismException
	{
		int lineNum = 0;
		int commentCount = 0;
		String line;
		do {
			line = in.readLine();
			lineNum++;
			if (line != null && COMMENT_PATTERN.matcher(line).matches() && isHeaderLike(line)) {
				commentCount++;
				if (commentCount == 1 && !line.startsWith("# Reward structure")) {
					throw new PrismException("File does not appear to be a " + fileDescription
							+ " (unexpected header: \"" + line + "\")");
				}
				if (commentCount == 2 && !line.startsWith(kindPrefix)) {
					throw new PrismException("File does not appear to be a " + fileDescription
							+ " (expected \"" + kindPrefix + "\", got: \"" + line + "\")");
				}
			}
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

	/**
	 * Check that a (string) action label is legal and return it if so.
	 * Otherwise, an explanatory exception is thrown.
	 * A legal action label is either "" (unlabelled) or a legal PRISM identifier.
	 * In the case of an empty ("") action, this returns null.
	 */
	protected static String checkAction(String a) throws PrismException
	{
		if (a == null || a.isEmpty()) {
			return null;
		}
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
