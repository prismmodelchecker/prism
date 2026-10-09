/*
 * Copyright 2025 Dave Parker (University of Oxford)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pmctools.umbj;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

/**
 * Class to handle reading from UMB files.
 */
public class UMBReader
{
	/**
	 * File to be read from.
	 */
	private final File fileIn;

	/**
	 * The (JSON) index extracted from the UMB file.
	 */
	private UMBIndex umbIndex;

	// Contents of the archive are read into memory, to avoid decompressing it repeatedly.
	// This is done lazily, in (at most) two passes through the archive:
	// - a "metadata" pass, on the first request for metadata (see isMetadataEntry)
	//   or for whether an entry exists, which reads all entry names but only the metadata entries
	// - a full pass, on the first request for any other entry, which reads all (remaining) entries
	// Retrieving only metadata (e.g. variables or action names) thus avoids reading everything.

	/**
	 * Names of all entries (files) in the archive; null until the archive has been scanned.
	 */
	private Set<String> entryNames = null;

	/**
	 * Contents of entries (files) in the archive that have been read into memory, keyed by name.
	 */
	private Map<String, byte[]> entryData = new HashMap<>();

	/**
	 * Have all entries been read into memory?
	 */
	private boolean allEntriesLoaded = false;

	/**
	 * Has the in-memory copy of the contents been released (see {@link #releaseData()})?
	 */
	private boolean dataReleased = false;

	/**
	 * Construct a new {@link UMBReader} reading from the specified file.
	 * @param fileIn The UMB file to read from.
	 */
	public UMBReader(File fileIn) throws UMBException
	{
		this.fileIn = fileIn;
		extractIndex();
	}

	/**
	 * Extract, parse, validate and store the JSON index.
	 */
	private void extractIndex() throws UMBException
	{
		// Extract index JSON as string
		UMBIn umbIn = open();
		umbIn.findArchiveEntry(UMBFormat.INDEX_FILE);
		String json = umbIn.readAsString();
		umbIn.close();

		// Parse/validate JSON
		// Note that we check for required fields, but do not complain about unexpected ones
		// (GSON does not make the latter process very easy)
		umbIndex = UMBIndex.fromJSON(json);
		umbIndex.validate();
	}

	/**
	 * Get the (JSON) index of the UMB file.
	 */
	public UMBIndex getUMBIndex()
	{
		return umbIndex;
	}

	/**
	 * Release the in-memory copy of the contents of the archive.
	 * Subsequent extraction remains possible, but each one re-reads its entry from the file.
	 */
	public void releaseData()
	{
		entryData = new HashMap<>();
		dataReleased = true;
	}

	// Methods to extract core model info

	/**
	 * Extract the state choice offsets.
	 */
	public void extractStateChoiceOffsets(LongConsumer longConsumer) throws UMBException
	{
		if (!fileExists(UMBFormat.STATE_CHOICE_OFFSETS_FILE)) {
			// Indices default to identities if requested when absent
			extractDefaultLongArray(umbIndex.getNumStates() + 1, UMBFormat.BinFileDefaultValue.IDENTITY, longConsumer);
		} else {
			extractLongArray(UMBFormat.STATE_CHOICE_OFFSETS_FILE, umbIndex.getNumStates() + 1, longConsumer);
		}
	}

	/**
	 * Extract the players that own states (turn-based game models)
	 */
	public void extractStatePlayers(IntConsumer intConsumer) throws UMBException
	{
		if (!fileExists(UMBFormat.STATE_PLAYERS)) {
			// Players default to 0 if requested when absent
			extractDefaultIntArray(umbIndex.getNumStates(), UMBFormat.BinFileDefaultValue.ZERO, intConsumer);
		} else {
			extractIntArray(UMBFormat.STATE_PLAYERS, umbIndex.getNumStates(), intConsumer);
		}
	}

	/**
	 * Extract the initial states, in sparse form, i.e., a list of state indices
	 */
	public void extractInitialStates(LongConsumer longConsumer) throws UMBException
	{
		if (!fileExists(UMBFormat.INITIAL_STATES_FILE)) {
			// Default to no initial states if requested when absent
		} else {
			extractBooleanArraySparse(UMBFormat.INITIAL_STATES_FILE, umbIndex.getNumStates(), longConsumer);
		}
	}

	/**
	 * Extract the Markovian states (for Markov automata), in sparse form, i.e., a list of state indices
	 */
	public void extractMarkovianStates(LongConsumer longConsumer) throws UMBException
	{
		if (!fileExists(UMBFormat.MARKOVIAN_STATES_FILE)) {
			// Default to no Markovian states if requested when absent
		} else {
			extractBooleanArraySparse(UMBFormat.MARKOVIAN_STATES_FILE, umbIndex.getNumStates(), longConsumer);
		}
	}

	/**
	 * Extract the exit rates for all states (for continuous-time models).
	 * The type of the values depends on {@link UMBIndex#getExitRateType()}.
	 */
	public void extractExitRates(Consumer<?> consumer) throws UMBException
	{
		extractContinuousNumericArray(UMBFormat.STATE_EXIT_RATES_FILE, umbIndex.getExitRateType(), umbIndex.getNumStates(), consumer);
	}

	/**
	 * Extract the choice branch offsets.
	 */
	public void extractChoiceBranchOffsets(LongConsumer longConsumer) throws UMBException
	{
		if (!fileExists(UMBFormat.CHOICE_BRANCH_OFFSETS_FILE)) {
			// Indices default to identities if requested when absent
			extractDefaultLongArray(umbIndex.getNumChoices() + 1, UMBFormat.BinFileDefaultValue.IDENTITY, longConsumer);
		} else {
			extractLongArray(UMBFormat.CHOICE_BRANCH_OFFSETS_FILE, umbIndex.getNumChoices() + 1, longConsumer);
		}
	}

	/**
	 * Extract the branch targets.
	 */
	public void extractBranchTargets(LongConsumer longConsumer) throws UMBException
	{
		extractLongArray(UMBFormat.BRANCH_TARGETS_FILE, umbIndex.getNumBranches(), longConsumer);
	}

	/**
	 * Extract the branch probabilities.
	 * The type (and number) of values provided depends on {@link UMBIndex#getBranchProbabilityType()}.
	 * For interval types, this method will extract two values (lower/upper bound, successively) for each branch.
	 * If values are rationals, this method will extract two values (numerator/denominator, successively) for each one.
	 */
	public void extractBranchProbabilities(Consumer<?> consumer) throws UMBException
	{
		if (!fileExists(UMBFormat.BRANCH_PROBABILITIES_FILE)) {
			// Branch probabilities default to 1 if requested when absent
			extractDefaultContinuousNumericArray(umbIndex.getBranchProbabilityType(), umbIndex.getNumBranches(), UMBFormat.BinFileDefaultValue.ONE, consumer);
		} else {
			extractContinuousNumericArray(UMBFormat.BRANCH_PROBABILITIES_FILE, umbIndex.getBranchProbabilityType(), umbIndex.getNumBranches(), consumer);
		}
	}

	/**
	 * Does this file store actions for choices?
	 */
	public boolean hasChoiceActionIndices() throws UMBException
	{
		return fileExists(umbIndex.actionsAnnotation.getFilename(UMBIndex.UMBEntity.CHOICES));
	}

	/**
	 * Extract the indices for actions of all choices
	 */
	public void extractChoiceActionIndices(IntConsumer intConsumer) throws UMBException
	{
		// ACTIONS_ANNOTATION is a string annotation but we just extract the indices here
		extractIntAnnotation(umbIndex.actionsAnnotation, UMBIndex.UMBEntity.CHOICES, intConsumer);
	}

	/**
	 * Does this file store actions for branches?
	 */
	public boolean hasBranchActionIndices() throws UMBException
	{
		return fileExists(umbIndex.actionsAnnotation.getFilename(UMBIndex.UMBEntity.BRANCHES));
	}

	/**
	 * Extract the indices for actions of all branches
	 */
	public void extractBranchActionIndices(IntConsumer intConsumer) throws UMBException
	{
		// ACTIONS_ANNOTATION is a string annotation but we just extract the indices here
		extractIntAnnotation(umbIndex.actionsAnnotation, UMBIndex.UMBEntity.BRANCHES, intConsumer);
	}

	/**
	 * Does this file store a list of choice action strings?
	 */
	public boolean hasChoiceActionStrings() throws UMBException
	{
		return fileExists(UMBFormat.stringOffsetsFile(umbIndex.actionsAnnotation.getFolderName(UMBIndex.UMBEntity.CHOICES)))
			&& fileExists(UMBFormat.stringsFile(umbIndex.actionsAnnotation.getFolderName(UMBIndex.UMBEntity.CHOICES)));
	}

	/**
	 * Extract the choice action strings
	 */
	public void extractChoiceActionStrings(Consumer<String> stringConsumer) throws UMBException
	{
		String folderName = umbIndex.actionsAnnotation.getFolderName(UMBIndex.UMBEntity.CHOICES);
		extractStrings(folderName, umbIndex.getNumChoiceActions(), stringConsumer);
	}

	/**
	 * Does this file store a list of branch action strings?
	 */
	public boolean hasBranchActionStrings() throws UMBException
	{
		return fileExists(UMBFormat.stringOffsetsFile(umbIndex.actionsAnnotation.getFolderName(UMBIndex.UMBEntity.BRANCHES)))
				&& fileExists(UMBFormat.stringsFile(umbIndex.actionsAnnotation.getFolderName(UMBIndex.UMBEntity.BRANCHES)));
	}

	/**
	 * Extract the branch action strings
	 */
	public void extractBranchActionStrings(Consumer<String> stringConsumer) throws UMBException
	{
		String folderName = umbIndex.actionsAnnotation.getFolderName(UMBIndex.UMBEntity.BRANCHES);
		extractStrings(folderName, umbIndex.getNumBranchActions(), stringConsumer);
	}

	/**
	 * Extract the (deterministic) observations for all states
	 * @param longConsumer Consumer to receive the observation indices
	 */
	public void extractStateObservations(LongConsumer longConsumer) throws UMBException
	{
		extractObservations(UMBIndex.UMBEntity.STATES, longConsumer);
	}

	/**
	 * Extract the (deterministic) observations for all branches
	 * @param longConsumer Consumer to receive the observation indices
	 */
	public void extractBranchObservations(LongConsumer longConsumer) throws UMBException
	{
		extractObservations(UMBIndex.UMBEntity.BRANCHES, longConsumer);
	}

	/**
	 * Extract the (deterministic) observations for some entity (states, branches)
	 * @param entity The entity for which observations are being extracted
	 * @param longConsumer Consumer to receive the observation indices
	 */
	public void extractObservations(UMBIndex.UMBEntity entity, LongConsumer longConsumer) throws UMBException
	{
		extractLongArray(umbIndex.observationsAnnotation.getFilename(entity), umbIndex.getEntityCount(entity), longConsumer);
	}

	// Utility methods for extracting date

	public <T extends LongConsumer> T extractStateChoiceCounts(T longConsumer) throws UMBException
	{
		extractStateChoiceOffsets(new OffsetsToCounts(longConsumer));
		return longConsumer;
	}

	public long extractMaxStateChoiceCount() throws UMBException
	{
		return extractStateChoiceCounts(new LongMax()).getMax();
	}

	// Methods to extract standard annotations

	/**
	 * Extract a state AP annotation via its index.
	 * @param i AP annotation index
	 * @param longConsumer Consumer to receive the indices of states satisfying the AP
	 */
	public void extractStateAP(int i, LongConsumer longConsumer) throws UMBException
	{
		UMBIndex.Annotation annotation = getUMBIndex().getAPAnnotation(i);
		extractBooleanAnnotationSparse(annotation, UMBIndex.UMBEntity.STATES, longConsumer);
	}

	/**
	 * Extract a state AP annotation via its ID.
	 * @param apID AP annotation ID
	 * @param longConsumer Consumer to receive the indices of states satisfying the AP
	 */
	public void extractStateAP(String apID, LongConsumer longConsumer) throws UMBException
	{
		UMBIndex.Annotation annotation = getUMBIndex().getAPAnnotationByID(apID);
		extractBooleanAnnotationSparse(annotation, UMBIndex.UMBEntity.STATES, longConsumer);
	}

	/**
	 * Extract a state reward annotation via its index.
	 * @param i Reward annotation index
	 * @param consumer Consumer to receive the values of the rewards
	 */
	public void extractStateRewards(int i, Consumer<?> consumer) throws UMBException
	{
		UMBIndex.Annotation annotation = getUMBIndex().getRewardAnnotation(i);
		extractContinuousNumericAnnotation(annotation, UMBIndex.UMBEntity.STATES, consumer);
	}

	/**
	 * Extract a state reward annotation from its alias.
	 * @param rewardID Reward annotation ID
	 * @param consumer Consumer to receive the values of the rewards
	 */
	public void extractStateRewards(String rewardID, Consumer<?> consumer) throws UMBException
	{
		UMBIndex.Annotation annotation = getUMBIndex().getRewardAnnotationByID(rewardID);
		extractContinuousNumericAnnotation(annotation, UMBIndex.UMBEntity.STATES, consumer);
	}

	/**
	 * Extract a choice reward annotation via its index.
	 * @param i Reward annotation index
	 * @param consumer Consumer to receive the values of the rewards
	 */
	public void extractChoiceRewards(int i, Consumer<?> consumer) throws UMBException
	{
		UMBIndex.Annotation annotation = getUMBIndex().getRewardAnnotation(i);
		extractContinuousNumericAnnotation(annotation, UMBIndex.UMBEntity.CHOICES, consumer);
	}

	/**
	 * Extract a choice reward annotation via its ID.
	 * @param rewardID Reward annotation ID
	 * @param consumer Consumer to receive the values of the rewards
	 */
	public void extractChoiceRewards(String rewardID, Consumer<?> consumer) throws UMBException
	{
		UMBIndex.Annotation annotation = getUMBIndex().getRewardAnnotationByID(rewardID);
		extractContinuousNumericAnnotation(annotation, UMBIndex.UMBEntity.CHOICES, consumer);
	}

	/**
	 * Extract a branch reward annotation via its index.
	 * @param i Reward annotation index
	 * @param consumer Consumer to receive the values of the rewards
	 */
	public void extractBranchRewards(int i, Consumer<?> consumer) throws UMBException
	{
		UMBIndex.Annotation annotation = getUMBIndex().getRewardAnnotation(i);
		extractContinuousNumericAnnotation(annotation, UMBIndex.UMBEntity.BRANCHES, consumer);
	}

	/**
	 * Extract a branch reward annotation via its ID.
	 * @param rewardID Reward annotation ID
	 * @param consumer Consumer to receive the values of the rewards
	 */
	public void extractBranchRewards(String rewardID, Consumer<?> consumer) throws UMBException
	{
		UMBIndex.Annotation annotation = getUMBIndex().getRewardAnnotationByID(rewardID);
		extractContinuousNumericAnnotation(annotation, UMBIndex.UMBEntity.BRANCHES, consumer);
	}

	// Methods to extract annotations

	public void extractBooleanAnnotationSparse(String group, String id, UMBIndex.UMBEntity appliesTo, LongConsumer longConsumer) throws UMBException
	{
		extractBooleanAnnotationSparse(umbIndex.getAnnotation(group, id), appliesTo, longConsumer);
	}

	public void extractBooleanAnnotationSparse(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, LongConsumer longConsumer) throws UMBException
	{
		String filename = annotation.getFilename(appliesTo);
		extractBooleanArraySparse(filename, getUMBIndex().getEntityCount(appliesTo), longConsumer);
	}

	public void extractIndexedBooleanAnnotation(String group, String id, UMBIndex.UMBEntity appliesTo, LongBooleanConsumer longBooleanConsumer) throws UMBException
	{
		extractIndexedBooleanAnnotation(umbIndex.getAnnotation(group, id), appliesTo, longBooleanConsumer);
	}

	public void extractIndexedBooleanAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, LongBooleanConsumer longBooleanConsumer) throws UMBException
	{
		String filename = annotation.getFilename(appliesTo);
		extractBooleanArray(filename, getUMBIndex().getEntityCount(appliesTo), new IndexedBooleanConsumer(longBooleanConsumer));
	}

	public void extractIntAnnotation(String group, String id, UMBIndex.UMBEntity appliesTo, IntConsumer intConsumer) throws UMBException
	{
		extractIntAnnotation(umbIndex.getAnnotation(group, id), appliesTo, intConsumer);
	}

	public void extractIntAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, IntConsumer intConsumer) throws UMBException
	{
		String filename = annotation.getFilename(appliesTo);
		extractIntArray(filename, getUMBIndex().getEntityCount(appliesTo), intConsumer);
	}

	public void extractIndexedIntAnnotation(String group, String id, UMBIndex.UMBEntity appliesTo, LongIntConsumer longIntConsumer) throws UMBException
	{
		extractIndexedIntAnnotation(umbIndex.getAnnotation(group, id), appliesTo, longIntConsumer);
	}

	public void extractIndexedIntAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, LongIntConsumer longIntConsumer) throws UMBException
	{
		String filename = annotation.getFilename(appliesTo);
		extractIntArray(filename, getUMBIndex().getEntityCount(appliesTo), new IndexedIntConsumer(longIntConsumer));
	}

	public void extractDoubleAnnotation(String group, String id, UMBIndex.UMBEntity appliesTo, DoubleConsumer doubleConsumer) throws UMBException
	{
		extractDoubleAnnotation(umbIndex.getAnnotation(group, id), appliesTo, doubleConsumer);
	}

	public void extractDoubleAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, DoubleConsumer doubleConsumer) throws UMBException
	{
		String filename = annotation.getFilename(appliesTo);
		extractDoubleArray(filename, getUMBIndex().getEntityCount(appliesTo), doubleConsumer);
	}

	public void extractContinuousNumericAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, Consumer<?> consumer) throws UMBException
	{
		String filename = annotation.getFilename(appliesTo);
		extractContinuousNumericArray(filename, annotation.getType(), getUMBIndex().getEntityCount(appliesTo), consumer);
	}

	public void extractStringAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, Consumer<String> stringConsumer) throws UMBException
	{
		String folderName = annotation.getFolderName(appliesTo);
		extractStrings(folderName, annotation.getNumStrings(), stringConsumer);
	}

	/**
	 * Extract the state valuations (variable values), one bitstring per state.
	 * @param bitstringConsumer Bitstring consumer
	 */
	public void extractStateValuations(Consumer<UMBBitString> bitstringConsumer) throws UMBException
	{
		extractValuations(UMBIndex.UMBEntity.STATES, bitstringConsumer);
	}

	/**
	 * Extract the observation valuations (observable values), one bitstring per observation.
	 * @param bitstringConsumer Bitstring consumer
	 */
	public void extractObservationValuations(Consumer<UMBBitString> bitstringConsumer) throws UMBException
	{
		extractValuations(UMBIndex.UMBEntity.OBSERVATIONS, bitstringConsumer);
	}

	/**
	 * Extract the class of valuations (variable values) used for each one of the specified entity type.
	 * @param entity The entity to which the valuations apply
	 * @param intConsumer Integer class consumer
	 */
	public void extractValuationClasses(UMBIndex.UMBEntity entity, IntConsumer intConsumer) throws UMBException
	{
		extractIntArray(UMBFormat.valuationClassesFile(entity), umbIndex.getEntityCount(entity), intConsumer);
	}

	/**
	 * Extract the valuations (variable values) for an entity, as bitstrings.
	 * @param entity The entity to which the valuations apply
	 * @param bitstringConsumer Bitstring consumer
	 */
	public void extractValuations(UMBIndex.UMBEntity entity, Consumer<UMBBitString> bitstringConsumer) throws UMBException
	{
		UMBBitPacking bitPacking = umbIndex.getValuationBitPacking(entity);
		extractBitStringArray(UMBFormat.valuationsFile(entity), umbIndex.getEntityCount(entity), bitPacking.getTotalNumBytes(), bitstringConsumer);
	}

	/**
	 * Compute the range of a (signed or unsigned) integer variable, from the values stored for it in a list of valuations.
	 * This assumes that the min/max values needs at most 32 bits, so that they can be stored in an {@code int}.
	 * @param entity The entity to which the valuations apply
	 * @param bitPacking The bit-packing for the valuations
	 * @param i Index of the variable (in the bit-packing)
	 */
	public UMBReader.IntRange getValuationIntRange(UMBIndex.UMBEntity entity, UMBBitPacking bitPacking, int i) throws UMBException
	{
		UMBReader.IntRangeComputer varRange = new UMBReader.IntRangeComputer();
		try {
			extractValuations(entity, bitString -> {
				try {
					switch (bitPacking.getVariable(i).getType().type) {
						case INT:
							varRange.accept(bitPacking.getIntVariableValue(bitString, i));
							break;
						case UINT:
							varRange.accept(bitPacking.getUIntVariableValue(bitString, i));
							break;
						default:
							throw new UMBException("Cannot compute the integer range of a " + bitPacking.getVariable(i).getType().type);
					}
				} catch (UMBException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (UMBException | RuntimeException e) {
			throw new UMBException("UMB import problem: " + e.getMessage());
		}
		return varRange;
	}

	/**
	 * Compute the range of a (signed or unsigned) integer variable, from the values stored for it in a list of valuations.
	 * This assumes that the min/max values needs at most 64 bits, so that they can be stored in a {@code long}.
	 * @param entity The entity to which the valuations apply
	 * @param bitPacking The bit-packing for the valuations
	 * @param i Index of the variable (in the bit-packing)
	 */
	public UMBReader.LongRange getValuationLongRange(UMBIndex.UMBEntity entity, UMBBitPacking bitPacking, int i) throws UMBException
	{
		UMBReader.LongRangeComputer varRange = new UMBReader.LongRangeComputer();
		try {
			extractValuations(entity, bitString -> {
				try {
					switch (bitPacking.getVariable(i).getType().type) {
						case INT:
							varRange.accept(bitPacking.getLongVariableValue(bitString, i));
							break;
						case UINT:
							varRange.accept(bitPacking.getULongVariableValue(bitString, i));
							break;
						default:
							throw new UMBException("Cannot compute the integer range of a " + bitPacking.getVariable(i).getType().type);
					}
				} catch (UMBException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (UMBException | RuntimeException e) {
			throw new UMBException("UMB import problem: " + e.getMessage());
		}
		return varRange;
	}

	/**
	 * Compute the ranges of all (signed or unsigned) integer variables, from the values stored for them in a list of valuations,
	 * in a single pass over the valuations. This assumes that the min/max values need at most 64 bits.
	 * The returned array is indexed by variable (in the bit-packing); entries for non-integer variables are null.
	 * @param entity The entity to which the valuations apply
	 * @param bitPacking The bit-packing for the valuations
	 */
	public UMBReader.LongRange[] getValuationLongRanges(UMBIndex.UMBEntity entity, UMBBitPacking bitPacking) throws UMBException
	{
		int numVars = bitPacking.getNumVariables();
		UMBReader.LongRangeComputer[] varRanges = new UMBReader.LongRangeComputer[numVars];
		// Store offsets/sizes/signedness of the integer variables to process
		int numIntVars = 0;
		int[] intVars = new int[numVars];
		int[] offsets = new int[numVars];
		int[] sizes = new int[numVars];
		boolean[] signed = new boolean[numVars];
		for (int i = 0; i < numVars; i++) {
			UMBType.Type type = bitPacking.getVariable(i).getType().type;
			if (type == UMBType.Type.INT || type == UMBType.Type.UINT) {
				varRanges[i] = new UMBReader.LongRangeComputer();
				intVars[numIntVars] = i;
				offsets[numIntVars] = bitPacking.getVariableOffset(i);
				sizes[numIntVars] = bitPacking.getVariableSize(i);
				signed[numIntVars] = type == UMBType.Type.INT;
				numIntVars++;
			}
		}
		// Nothing to compute if there are no integer variables
		if (numIntVars == 0) {
			return varRanges;
		}
		int finalNumIntVars = numIntVars;
		try {
			extractValuations(entity, bitString -> {
				try {
					for (int j = 0; j < finalNumIntVars; j++) {
						long value = signed[j] ? bitString.getLong(offsets[j], sizes[j]) : bitString.getULong(offsets[j], sizes[j]);
						varRanges[intVars[j]].accept(value);
					}
				} catch (UMBException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (UMBException | RuntimeException e) {
			throw new UMBException("UMB import problem: " + e.getMessage());
		}
		return varRanges;
	}

	// Local methods for extracting data

	/**
	 * Is an entry (file) in the archive "metadata", i.e., describing the model's variables or
	 * strings (e.g. action names), rather than its transition structure or annotation values?
	 * These are read in an initial lighter pass of the archive, before any other data is requested.
	 */
	private static boolean isMetadataEntry(String filename)
	{
		return filename.startsWith(UMBFormat.VALUATIONS_FOLDER + "/")
				|| filename.endsWith("/" + UMBFormat.STRINGS_FILE)
				|| filename.endsWith("/" + UMBFormat.STRING_OFFSETS_FILE);
	}

	/**
	 * Do a single pass through the archive, recording the names of all entries (files)
	 * and reading the contents of those entries satisfying {@code toRead} into {@code data}.
	 */
	private void scanEntries(Predicate<String> toRead, Map<String, byte[]> data) throws UMBException
	{
		Set<String> names = new HashSet<>();
		UMBIn umbIn = open();
		try {
			umbIn.readEntries(toRead, names, data);
		} finally {
			umbIn.close();
		}
		entryNames = names;
	}

	private boolean fileExists(String filename) throws UMBException
	{
		if (entryNames == null) {
			// Read metadata at the same time, unless the data has been released
			scanEntries(dataReleased ? name -> false : UMBReader::isMetadataEntry, entryData);
		}
		return entryNames.contains(filename);
	}

	/**
	 * Get the contents of an entry (file) in the archive, checking that it has the expected size (in bytes).
	 * If needed, the contents are read into memory, along with other entries (see above).
	 * If the in-memory copy has been released, the entry is re-read from the file.
	 */
	private byte[] getEntryBytes(String filename, long expectedSize) throws UMBException
	{
		byte[] bytes = entryData.get(filename);
		if (bytes == null && (entryNames == null || entryNames.contains(filename))) {
			if (dataReleased) {
				// Read just this entry (not retained)
				Map<String, byte[]> data = new HashMap<>();
				scanEntries(filename::equals, data);
				bytes = data.get(filename);
			} else if (!allEntriesLoaded) {
				if (entryNames == null && isMetadataEntry(filename)) {
					scanEntries(UMBReader::isMetadataEntry, entryData);
				} else {
					// Read all entries not already read
					scanEntries(name -> !entryData.containsKey(name), entryData);
					allEntriesLoaded = true;
				}
				bytes = entryData.get(filename);
			}
		}
		if (bytes == null) {
			throw new UMBException("UMB archive entry \"" + filename + "\" not found");
		}
		if (bytes.length != expectedSize) {
			throw new UMBException("File " + filename + " has unexpected size (" + bytes.length + " bytes, not " + expectedSize + ")");
		}
		return bytes;
	}

	/**
	 * Get the contents of an entry (file) in the archive, as a (little-endian) {@link ByteBuffer},
	 * checking that it has the expected size (in bytes).
	 */
	private ByteBuffer getEntryBuffer(String filename, long expectedSize) throws UMBException
	{
		return ByteBuffer.wrap(getEntryBytes(filename, expectedSize)).order(ByteOrder.LITTLE_ENDIAN);
	}

	private void extractBooleanArraySparse(String filename, long size, LongConsumer longConsumer) throws UMBException
	{
		long numLongs = (size + 63) / 64;
		ByteBuffer bytes = getEntryBuffer(filename, numLongs * Long.BYTES);
		try {
			long index = 0;
			for (long i = 0; i < numLongs; i++) {
				long l = bytes.getLong();
				// Find local index j of each 1 bit within 64-bit block
				int blockSize = index + Long.BYTES * 8 <= size ? Long.BYTES * 8 : (int) (size - index);
				for (int j = 0; j < blockSize; j++) {
					if ((l & (1L << j)) != 0) {
						longConsumer.accept(index + j);
					}
				}
				index += Long.BYTES * 8;
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		}
	}

	private void extractBooleanArray(String filename, long size, BooleanConsumer booleanConsumer) throws UMBException
	{
		long numLongs = (size + 63) / 64;
		ByteBuffer bytes = getEntryBuffer(filename, numLongs * Long.BYTES);
		try {
			long index = 0;
			for (long i = 0; i < numLongs; i++) {
				long l = bytes.getLong();
				// Find local index j of each 1 bit within 64-bit block
				int blockSize = index + Long.BYTES * 8 <= size ? Long.BYTES * 8 : (int) (size - index);
				for (int j = 0; j < blockSize; j++) {
					booleanConsumer.accept((l & (1L << j)) != 0);
				}
				index += Long.BYTES * 8;
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		}
	}

	private void extractIntArray(String filename, long size, IntConsumer intConsumer) throws UMBException
	{
		ByteBuffer bytes = getEntryBuffer(filename, size * Integer.BYTES);
		try {
			for (long i = 0; i < size; i++) {
				intConsumer.accept(bytes.getInt());
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		}
	}

	private void extractLongArray(String filename, long size, LongConsumer longConsumer) throws UMBException
	{
		ByteBuffer bytes = getEntryBuffer(filename, size * Long.BYTES);
		try {
			for (long i = 0; i < size; i++) {
				longConsumer.accept(bytes.getLong());
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		}
	}

	private void extractDoubleArray(String filename, long size, DoubleConsumer doubleConsumer) throws UMBException
	{
		ByteBuffer bytes = getEntryBuffer(filename, size * Double.BYTES);
		try {
			for (long i = 0; i < size; i++) {
				doubleConsumer.accept(bytes.getDouble());
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		}
	}

	private void extractContinuousNumericArray(String filename, UMBType type, long size, Consumer<?> consumer) throws UMBException
	{
		long sizeNew = type.type.isInterval() ? size * 2 : size;
		if (type.type.isDouble()) {
			extractDoubleArray(filename, sizeNew, asDoubleConsumer(consumer));
		} else if (type.type.isRational()) {
			if (!type.isDefaultSize()) {
				throw new UMBException("Non-default sized rationals are not yet supported");
			}
			extractLongArray(filename, sizeNew * 2, asLongConsumer(consumer));
		} else {
			throw new UMBException("Unsupported continuous numeric type " + type);
		}
	}

	private void extractBitStringArray(String filename, long size, int numBytes, Consumer<UMBBitString> bitstringConsumer) throws UMBException
	{
		ByteBuffer bytes = getEntryBuffer(filename, size * numBytes);
		try {
			// Note: the same bitstring object is reused for each value
			UMBBitString bitString = new UMBBitString(numBytes);
			for (long i = 0; i < size; i++) {
				bytes.get(bitString.bytes);
				bitstringConsumer.accept(bitString);
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		}
	}

	/**
	 * Extract strings, as stored in a files for string offsets and data in a folder
	 */
	private void extractStrings(String folderName, int numStrings, Consumer<String> stringConsumer) throws UMBException
	{
		List<Long> stringOffsets = new ArrayList<>(numStrings);
		extractLongArray(UMBFormat.stringOffsetsFile(folderName), numStrings + 1, stringOffsets::add);
		extractStringList(UMBFormat.stringsFile(folderName), stringOffsets, stringConsumer);
	}

	private void extractStringList(String filename, List<Long> stringOffsets, Consumer<String> stringConsumer) throws UMBException
	{
		int numStrings = stringOffsets.size() - 1;
		byte[] bytes = getEntryBytes(filename, stringOffsets.get(numStrings));
		try {
			for (int i = 0; i < numStrings; i++) {
				long sStart = stringOffsets.get(i);
				long sEnd = stringOffsets.get(i + 1);
				if (sStart < 0 || sEnd < sStart || sEnd > bytes.length) {
					throw new UMBException("Invalid string offsets (" + sStart + ", " + sEnd + ") for file " + filename);
				}
				stringConsumer.accept(new String(bytes, (int) sStart, (int) (sEnd - sStart), StandardCharsets.UTF_8));
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		}
	}

	/**
	 * Simulate extraction of a missing int array by filling the consumer with a default value.
	 */
	private void extractDefaultIntArray(long size, UMBFormat.BinFileDefaultValue value, IntConsumer intConsumer) throws UMBException
	{
		switch (value) {
			case IDENTITY:
				for (long i = 0; i < size; i++) {
					intConsumer.accept((int) i);
				}
				break;
			case ZERO:
				for (long i = 0; i < size; i++) {
					intConsumer.accept(0);
				}
				break;
			case ONE:
				for (long i = 0; i < size; i++) {
					intConsumer.accept(1);
				}
				break;
			default:
				throw new UMBException("Unsupported default value " + value);
		}
	}

	/**
	 * Simulate extraction of a missing long array by filling the consumer with a default value.
	 */
	private void extractDefaultLongArray(long size, UMBFormat.BinFileDefaultValue value, LongConsumer longConsumer) throws UMBException
	{
		switch (value) {
			case IDENTITY:
				for (long i = 0; i < size; i++) {
					longConsumer.accept(i);
				}
				break;
			case ZERO:
				for (long i = 0; i < size; i++) {
					longConsumer.accept(0L);
				}
				break;
			case ONE:
				for (long i = 0; i < size; i++) {
					longConsumer.accept(1L);
				}
				break;
			default:
				throw new UMBException("Unsupported default value " + value);
		}
	}

	/**
	 * Simulate extraction of a missing continuous numeric array by filling the consumer with a default value.
	 */
	private void extractDefaultContinuousNumericArray(UMBType type, long size, UMBFormat.BinFileDefaultValue value, Consumer<?> consumer) throws UMBException
	{
		long sizeNew = type.type.isInterval() ? size * 2 : size;
		if (type.type.isDouble()) {
			DoubleConsumer doubleConsumer = asDoubleConsumer(consumer);
			double doubleValue;
			switch (value) {
				case ZERO:
					doubleValue = 0.0;
					break;
				case ONE:
					doubleValue = 1.0;
					break;
				default:
					throw new UMBException("Unsupported default value " + value);
			}
			for (long i = 0; i < sizeNew; i++) {
				doubleConsumer.accept(doubleValue);
			}
		} else if (type.type.isRational()) {
			if (!type.isDefaultSize()) {
				throw new UMBException("Non-default sized rationals are not yet supported");
			}
			LongConsumer longConsumer = asLongConsumer(consumer);
			long longValue1;
			long longValue2;
			switch (value) {
				case ZERO:
					longValue1 = 0L;
					longValue2 = 1L;
					break;
				case ONE:
					longValue1 = 1L;
					longValue2 = 1L;
					break;
				default:
					throw new UMBException("Unsupported default value " + value);
			}
			for (long i = 0; i < sizeNew; i++) {
				longConsumer.accept(longValue1);
				longConsumer.accept(longValue2);
			}
		} else {
			throw new UMBException("Unsupported continuous numeric type " + type);
		}
	}

	UMBIn umbInCached = null;

	private UMBIn open() throws UMBException
	{
		if (umbInCached != null) {
			return umbInCached;
		} else {
//			umbInCached = new UMBIn(fileIn);;
//			return umbInCached;
			return new UMBIn(fileIn);
		}
	}

	//

	/**
	 * Class to manage reading from the zipped archive for a UMB file
	 */
	private static class UMBIn
	{
		/** Input stream from zip file */
		private final InputStream fsIn;
		/** Input stream after unzipping */
		private CompressorInputStream zipIn;
		/** Input stream from tar file */
		private TarArchiveInputStream tarIn;

		/** Byte buffer used to return file contents */
		private ByteBuffer byteBuffer;
		/** Initial size of byte buffer */
		private static final int DEFAULT_BUFFER_SIZE = 1024;
		/** Maximum size (in bytes) of an entry that can be read into memory (Java array limit) */
		private static final long MAX_ENTRY_SIZE = Integer.MAX_VALUE - 8;

		/**
		 * Open a new UMB file for reading
		 */
		public UMBIn(File fileIn) throws UMBException
		{
			try {
				// Open file/zip/tar and create buffer
				fsIn = new BufferedInputStream(Files.newInputStream(fileIn.toPath()));
				try {
					// Any supported zip format is fine
					zipIn = new CompressorStreamFactory().createCompressorInputStream(fsIn);
					tarIn = new TarArchiveInputStream(zipIn);
				} catch (CompressorException e) {
					// No zipping also fine
					zipIn = null;
					tarIn = new TarArchiveInputStream(fsIn);
				}
				byteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
			} catch (IOException e) {
				throw new UMBException("Could not open UMB file: " + e.getMessage());
			}
		}

		/**
		 * Read through all entries (files) in the archive, in a single pass.
		 * The names of all (readable) entries are added to {@code names}.
		 * The contents of entries whose names satisfy {@code toRead}
		 * are read into memory and stored in {@code data}.
		 */
		public void readEntries(Predicate<String> toRead, Set<String> names, Map<String, byte[]> data) throws UMBException
		{
			try {
				TarArchiveEntry entry;
				while ((entry = tarIn.getNextTarEntry()) != null) {
					if (!tarIn.canReadEntryData(entry)) {
						continue;
					}
					names.add(entry.getName());
					if (toRead.test(entry.getName())) {
						long size = entry.getSize();
						if (size > MAX_ENTRY_SIZE) {
							throw new UMBException("UMB archive entry \"" + entry.getName() + "\" is too large (" + size + " bytes) to be read");
						}
						byte[] bytes = new byte[(int) size];
						if (readFully(bytes, (int) size) < size) {
							throw new UMBException("Unexpected end of UMB archive entry \"" + entry.getName() + "\"");
						}
						data.put(entry.getName(), bytes);
					}
				}
			} catch (IOException e) {
				throw new UMBException("I/O error extracting from UMB file");
			}
		}

		/**
		 * Find an entry (file) within the archive for subsequent reading.
		 * Returns the size (number of bytes) of the entry if it is found,
		 * or throws an exception if not.
		 * @param name Name of the file
		 */
		public long findArchiveEntry(String name) throws UMBException
		{
			try {
				TarArchiveEntry entry;
				while ((entry = tarIn.getNextTarEntry()) != null) {
					if (!tarIn.canReadEntryData(entry)) {
						continue;
					}
					if (entry.getName().equals(name)) {
						return entry.getSize();
					}
				}
			} catch (IOException e) {
				throw new UMBException("I/O error extracting from UMB file");
			}
			throw new UMBException("UMB archive entry \"" + name + "\" not found");
		}

		/**
		 * Read up to {@code numBytes} bytes from the current entry (file) of the archive into {@code bytes}.
		 * Unlike a single call to {@code read}, this only stops early if the end of the entry is reached.
		 * Returns the number of bytes actually read.
		 */
		private int readFully(byte[] bytes, int numBytes) throws IOException
		{
			int offset = 0;
			while (offset < numBytes) {
				int n = tarIn.read(bytes, offset, numBytes - offset);
				if (n == -1) {
					break;
				}
				offset += n;
			}
			return offset;
		}

		/**
		 * Read the whole of the current entry (file) of the archive as a string.
		 */
		public String readAsString() throws UMBException
		{
			// Decode only once all bytes are read, since multi-byte characters may span chunks
			ByteArrayOutputStream allBytes = new ByteArrayOutputStream();
			try {
				byte[] bytes = byteBuffer.array();
				int bytesRead;
				while ((bytesRead = tarIn.read(bytes)) != -1) {
					allBytes.write(bytes, 0, bytesRead);
				}
				return allBytes.toString(StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UMBException("I/O error extracting string from UMB entry \"" + tarIn.getCurrentEntry().getName() + "\"");
			}
		}

		/**
		 * Close the UMB file.
		 */
		public void close() throws UMBException
		{
			try {
				if (tarIn != null) {
					tarIn.close();
				}
				if (zipIn != null) {
					zipIn.close();
				}
				if (fsIn != null) {
					fsIn.close();
				}
			} catch (IOException e) {
				throw new UMBException("I/O error closing UMB file");
			}
		}
	}

	// Utility methods

	/**
	 * Convert a consumer passed for (continuous numeric) double values to a {@link DoubleConsumer}.
	 * If it is already a {@link DoubleConsumer} (e.g., a fastutil one), values are passed to it directly;
	 * otherwise, it should be a {@code Consumer<Double>}, and values are boxed.
	 */
	@SuppressWarnings("unchecked")
	private static DoubleConsumer asDoubleConsumer(Consumer<?> consumer)
	{
		if (consumer instanceof DoubleConsumer) {
			return (DoubleConsumer) consumer;
		}
		return ((Consumer<Double>) consumer)::accept;
	}

	/**
	 * Convert a consumer passed for (continuous numeric) long values (e.g., rationals) to a {@link LongConsumer}.
	 * If it is already a {@link LongConsumer} (e.g., a fastutil one), values are passed to it directly;
	 * otherwise, it should be a {@code Consumer<Long>}, and values are boxed.
	 */
	@SuppressWarnings("unchecked")
	private static LongConsumer asLongConsumer(Consumer<?> consumer)
	{
		if (consumer instanceof LongConsumer) {
			return (LongConsumer) consumer;
		}
		return ((Consumer<Long>) consumer)::accept;
	}

	// Utility classes

	@FunctionalInterface
	public interface LongBooleanConsumer
	{
		void accept(long index, boolean value);
	}

	@FunctionalInterface
	public interface LongIntConsumer
	{
		void accept(long index, int value);
	}

	@FunctionalInterface
	public interface LongLongConsumer
	{
		void accept(long index, long value);
	}

	/**
	 * Class to convert a sequence of ints to a sequence of (long) indexed ints.
	 */
	public static class IndexedIntConsumer implements IntConsumer
	{
		private final LongIntConsumer longIntConsumer;
		private long index = 0;

		public IndexedIntConsumer(LongIntConsumer longIntConsumer)
		{
			this.longIntConsumer = longIntConsumer;
		}

		@Override
		public void accept(int intValue)
		{
			longIntConsumer.accept(index++, intValue);
		}
	}

	/**
	 * Class to convert a sequence of booleans to a sequence of (long) indexed booleans.
	 */
	public static class IndexedBooleanConsumer implements BooleanConsumer
	{
		private final LongBooleanConsumer longBooleanConsumer;
		private long index = 0;

		public IndexedBooleanConsumer(LongBooleanConsumer longBooleanConsumer)
		{
			this.longBooleanConsumer = longBooleanConsumer;
		}

		@Override
		public void accept(boolean booleanValue)
		{
			longBooleanConsumer.accept(index++, booleanValue);
		}
	}

	/**
	 * Class to convert a non-decreasing sequence of n+1 non-negative (long) offsets
	 * to a corresponding sequence of n (long) counts. Both via consumers of longs.
	 */
	public static class OffsetsToCounts implements LongConsumer
	{
		LongConsumer out;
		long offsetLast;

		OffsetsToCounts(LongConsumer out)
		{
			this.out = out;
		}

		@Override
		public void accept(long offset)
		{
			if (offsetLast != -1) {
				out.accept(offset - offsetLast);
			}
			offsetLast = offset;
		}
	}

	/**
	 * Class to represent the minimum/maximum value of a set of ints.
	 */
	public static class IntRange
	{
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;

		public int getMin()
		{
			return min;
		}

		public int getMax()
		{
			return max;
		}
	}

	/**
	 * Class to represent the minimum/maximum value of a set of longs.
	 */
	public static class LongRange
	{
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;

		public long getMin()
		{
			return min;
		}

		public long getMax()
		{
			return max;
		}
	}

	/**
	 * Class to compute the minimum/maximum value of a sequence of ints, provided via a consumer.
	 */
	public static class IntRangeComputer extends IntRange implements IntConsumer
	{
		@Override
		public void accept(int i)
		{
			min = Integer.min(min, i); max = Integer.max(max, i);
		}
	}

	/**
	 * Class to compute the minimum/maximum value of a sequence of longs, provided via a consumer.
	 */
	public static class LongRangeComputer extends LongRange implements LongConsumer
	{
		@Override
		public void accept(long i)
		{
			min = Long.min(min, i); max = Long.max(max, i);
		}
	}

	/**
	 * Class to compute the maximum value of a sequence of longs, provided via a consumer.
	 */
	public static class LongMax implements LongConsumer
	{
		long max = Long.MIN_VALUE;

		public long getMax()
		{
			return max;
		}

		@Override
		public void accept(long l)
		{
			max = Long.max(max, l);
		}
	}
}
