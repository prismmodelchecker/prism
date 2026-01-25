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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

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

	/**
	 * Default buffer size (in bytes) for reading from UMB file.
	 */
	private static int BUFFER_SIZE = 64 * 1024;

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

	// Methods to extract core model info

	/**
	 * Extract the state choice offsets.
	 */
	public void extractStateChoiceOffsets(LongConsumer longConsumer) throws UMBException
	{
		extractLongArray(UMBFormat.STATE_CHOICE_OFFSETS_FILE, umbIndex.getNumStates() + 1, longConsumer);
	}

	/**
	 * Extract the players that own states (turn-based game models)
	 */
	public void extractStatePlayers(IntConsumer intConsumer) throws UMBException
	{
		extractIntArray(UMBFormat.STATE_PLAYERS, umbIndex.getNumStates(), intConsumer);
	}

	/**
	 * Extract the initial states, in sparse form, i.e., a list of state indices
	 */
	public void extractInitialStates(LongConsumer longConsumer) throws UMBException
	{
		extractBooleanArraySparse(UMBFormat.INITIAL_STATES_FILE, umbIndex.getNumStates(), longConsumer);
	}

	/**
	 * Extract the Markovian states (for Markov automata), in sparse form, i.e., a list of state indices
	 */
	public void extractMarkovianStates(LongConsumer longConsumer) throws UMBException
	{
		extractBooleanArraySparse(UMBFormat.MARKOVIAN_STATES_FILE, umbIndex.getNumStates(), longConsumer);
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
		extractLongArray(UMBFormat.CHOICE_BRANCH_OFFSETS_FILE, umbIndex.getNumChoices() + 1, longConsumer);
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
		extractContinuousNumericArray(UMBFormat.BRANCH_PROBABILITIES_FILE, umbIndex.getBranchProbabilityType(), umbIndex.getNumBranches(), consumer);
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
					throw new RuntimeException(e.getMessage());
				}
			});
		} catch (UMBException | RuntimeException e) {
			throw new UMBException("UMB import problem: " + e.getMessage());
		}
		return varRange;
	}

	// Local methods for extracting data

	private boolean fileExists(String filename) throws UMBException
	{
		UMBIn umbIn = open();
		boolean exists = umbIn.archiveEntryExists(filename);
		umbIn.close();
		return exists;
	}

	private void extractBooleanArraySparse(String filename, long size, LongConsumer longConsumer) throws UMBException
	{
		UMBIn umbIn = open();
		try {
			long entrySize = umbIn.findArchiveEntry(filename);
			//long minExpectedSize = (size + 7) / 8;
			long expectedSize = ((size + 63) / 64) * 8;
			if (entrySize != expectedSize) {
				throw new UMBException("File " + filename + " has unexpected size (" + entrySize + " bytes)");
			}
			ByteBuffer bytes;
			int numBytes = Long.BYTES;
			long leftToRead = ((size + 63) / 64);
			int cacheSize = (int) Math.min((BUFFER_SIZE) / numBytes, leftToRead);
			long[] cache = new long[cacheSize];
			int toRead = (int) (Math.min(leftToRead, cacheSize));
			long index = 0;
			while (toRead > 0 && (bytes = umbIn.readBytes(toRead * numBytes)) != null) {
				// Cache data
				for (int i = 0; i < toRead; i++) {
					cache[i] = bytes.getLong();
				}
				// Pass data to consumer
				for (int i = 0; i < toRead; i++) {
					long l = cache[i];
					// Find local index j of each 1 bit within 64-bit block
					int blockSize = index + Long.BYTES * 8 <= size ? Long.BYTES * 8 : (int) (size - index);
					for (int j = 0; j < blockSize; j++) {
						if ((l & (1L << j)) != 0) {
							longConsumer.accept(index + j);
						}
					}
					index += Long.BYTES * 8;
				}
				leftToRead -= toRead;
				toRead = (int) (Math.min(leftToRead, cacheSize));
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		} finally {
			umbIn.close();
		}
	}

	private void extractBooleanArray(String filename, long size, BooleanConsumer booleanConsumer) throws UMBException
	{
		UMBIn umbIn = open();
		try {
			long entrySize = umbIn.findArchiveEntry(filename);
			//long minExpectedSize = (size + 7) / 8;
			long expectedSize = ((size + 63) / 64) * 8;
			if (entrySize != expectedSize) {
				throw new UMBException("File " + filename + " has unexpected size (" + entrySize + " bytes)");
			}
			ByteBuffer bytes;
			int numBytes = Long.BYTES;
			long leftToRead = ((size + 63) / 64);
			int cacheSize = (int) Math.min((BUFFER_SIZE) / numBytes, leftToRead);
			long[] cache = new long[cacheSize];
			int toRead = (int) (Math.min(leftToRead, cacheSize));
			long index = 0;
			while (toRead > 0 && (bytes = umbIn.readBytes(toRead * numBytes)) != null) {
				// Cache data
				for (int i = 0; i < toRead; i++) {
					cache[i] = bytes.getLong();
				}
				// Pass data to consumer
				for (int i = 0; i < toRead; i++) {
					long l = cache[i];
					// Find local index j of each 1 bit within 64-bit block
					int blockSize = index + Long.BYTES * 8 <= size ? Long.BYTES * 8 : (int) (size - index);
					for (int j = 0; j < blockSize; j++) {
						booleanConsumer.accept((l & (1L << j)) != 0);
					}
					index += Long.BYTES * 8;
				}
				leftToRead -= toRead;
				toRead = (int) (Math.min(leftToRead, cacheSize));
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		} finally {
			umbIn.close();
		}
	}

	private void extractIntArray(String filename, long size, IntConsumer intConsumer) throws UMBException
	{
		UMBIn umbIn = open();
		try {
			long entrySize = umbIn.findArchiveEntry(filename);
			if (entrySize != size * Integer.BYTES) {
				throw new UMBException("File " + filename + " has unexpected size (" + entrySize + " bytes, not " + (size * Integer.BYTES) + ")");
			}
			ByteBuffer bytes;
			int numBytes = Integer.BYTES;
			long leftToRead = size;
			int cacheSize = (int) Math.min((BUFFER_SIZE) / numBytes, leftToRead);
			int[] cache = new int[cacheSize];
			int toRead = (int) (Math.min(leftToRead, cacheSize));
			while (toRead > 0 && (bytes = umbIn.readBytes(toRead * numBytes)) != null) {
				// Cache data
				for (int i = 0; i < toRead; i++) {
					cache[i] = bytes.getInt();
				}
				// Pass data to consumer
				for (int i = 0; i < toRead; i++) {
					intConsumer.accept(cache[i]);
				}
				leftToRead -= toRead;
				toRead = (int) (Math.min(leftToRead, cacheSize));
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		} finally {
			umbIn.close();
		}
	}

	private void extractLongArray(String filename, long size, LongConsumer longConsumer) throws UMBException
	{
		UMBIn umbIn = open();
		try {
			long entrySize = umbIn.findArchiveEntry(filename);
			if (entrySize != size * Long.BYTES) {
				throw new UMBException("File " + filename + " has unexpected size (" + entrySize + " bytes, not " + (size * Long.BYTES) + ")");
			}
			ByteBuffer bytes;
			int numBytes = Long.BYTES;
			long leftToRead = size;
			int cacheSize = (int) Math.min((BUFFER_SIZE) / numBytes, leftToRead);
			long[] cache = new long[cacheSize];
			int toRead = (int) (Math.min(leftToRead, cacheSize));
			while (toRead > 0 && (bytes = umbIn.readBytes(toRead * numBytes)) != null) {
				// Cache data
				for (int i = 0; i < toRead; i++) {
					cache[i] = bytes.getLong();
				}
				// Pass data to consumer
				for (int i = 0; i < toRead; i++) {
					longConsumer.accept(cache[i]);
				}
				leftToRead -= toRead;
				toRead = (int) (Math.min(leftToRead, cacheSize));
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		} finally {
			umbIn.close();
		}
	}

	private void extractDoubleArray(String filename, long size, DoubleConsumer doubleConsumer) throws UMBException
	{
		UMBIn umbIn = open();
		try {
			long entrySize = umbIn.findArchiveEntry(filename);
			if (entrySize != size * Double.BYTES) {
				throw new UMBException("File " + filename + " has unexpected size (" + entrySize + " bytes, not " + (size * Double.BYTES) + ")");
			}
			ByteBuffer bytes;
			int numBytes = Double.BYTES;
			long leftToRead = size;
			int cacheSize = (int) Math.min((BUFFER_SIZE) / numBytes, leftToRead);
			double[] cache = new double[cacheSize];
			int toRead = (int) (Math.min(leftToRead, cacheSize));
			while (toRead > 0 && (bytes = umbIn.readBytes(toRead * numBytes)) != null) {
				// Cache data
				for (int i = 0; i < toRead; i++) {
					cache[i] = bytes.getDouble();
				}
				// Pass data to consumer
				for (int i = 0; i < toRead; i++) {
					doubleConsumer.accept(cache[i]);
				}
				leftToRead -= toRead;
				toRead = (int) (Math.min(leftToRead, cacheSize));
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		} finally {
			umbIn.close();
		}
	}

	private void extractContinuousNumericArray(String filename, UMBType type, long size, Consumer<?> consumer) throws UMBException
	{
		long sizeNew = type.type.isInterval() ? size * 2 : size;
		if (type.type.isDouble()) {
			extractDoubleArray(filename, sizeNew, (it.unimi.dsi.fastutil.doubles.DoubleConsumer) ((Consumer<Double>) consumer)::accept);
		} else if (type.type.isRational()) {
			if (!type.isDefaultSize()) {
				throw new UMBException("Non-default sized rationals are not yet supported");
			}
			extractLongArray(filename, sizeNew * 2, (it.unimi.dsi.fastutil.longs.LongConsumer) ((Consumer<Long>) consumer)::accept);
		} else {
			throw new UMBException("Unsupported continuous numeric type " + type);
		}
	}

	private void extractBitStringArray(String filename, long size, int numBytes, Consumer<UMBBitString> bitstringConsumer) throws UMBException
	{
		UMBIn umbIn = open();
		try {
			long entrySize = umbIn.findArchiveEntry(filename);
			if (entrySize != size * numBytes) {
				throw new UMBException("File " + filename + " has unexpected size (" + entrySize + " bytes, not " + (size * numBytes) + ")");
			}
			ByteBuffer bytes;
			long leftToRead = size;
			int cacheSize = (int) Math.min((BUFFER_SIZE) / numBytes, leftToRead);
			UMBBitString[] cache = new UMBBitString[cacheSize];
			for (int i = 0; i < cacheSize; i++) {
				cache[i] = new UMBBitString(numBytes);
			}
			int toRead = (int) (Math.min(leftToRead, cacheSize));
			while (toRead > 0 && (bytes = umbIn.readBytes(toRead * numBytes)) != null) {
				// Cache data
				for (int i = 0; i < toRead; i++) {
					bytes.get(cache[i].bytes);
				}
				// Pass data to consumer
				for (int i = 0; i < toRead; i++) {
					bitstringConsumer.accept(cache[i]);
				}
				leftToRead -= toRead;
				toRead = (int) (Math.min(leftToRead, cacheSize));
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		} finally {
			umbIn.close();
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
		UMBIn umbIn = open();
		try {
			int numStrings = stringOffsets.size() - 1;
			long entrySize = umbIn.findArchiveEntry(filename);
			if (entrySize != stringOffsets.get(numStrings)) {
				throw new UMBException("File " + filename + " has unexpected size (" + entrySize + " bytes, not " + stringOffsets.get(numStrings) + ")");
			}
			for (int i = 0; i < numStrings; i++) {
				long sLen = stringOffsets.get(i + 1) - stringOffsets.get(i);
				if (sLen > Integer.MAX_VALUE) {
					throw new UMBException("Could not read overlength string (" + sLen + "bytes) from file " + filename);
				}
				String s = umbIn.readString((int) sLen);
				if (s == null) {
					throw new UMBException("Could not read string of length " + sLen + " from file " + filename);
				}
				stringConsumer.accept(s);
			}
		} catch (RuntimeException e) {
			// Errors may occur in consumers so catch runtime exceptions here
			throw new UMBException("Error extracting from UMB file: " + e.getMessage());
		} finally {
			umbIn.close();
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
		 * Check if an entry (file) exists within the archive.
		 * @param name Name of the file
		 */
		public boolean archiveEntryExists(String name) throws UMBException
		{
			try {
				TarArchiveEntry entry;
				while ((entry = tarIn.getNextTarEntry()) != null) {
					if (!tarIn.canReadEntryData(entry)) {
						continue;
					}
					if (entry.getName().equals(name)) {
						return true;
					}
				}
			} catch (IOException e) {
				throw new UMBException("I/O error extracting from UMB file");
			}
			return false;
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

		public TarArchiveInputStream getInputStream()
		{
			return tarIn;
		}

		/**
		 * Read the specified number of bytes from the current entry (file) of the archive.
		 * Returns the bytes in a {@link ByteBuffer}, or returns null if no or too few bytes are available.
		 */
		public ByteBuffer readBytes(int numBytes) throws UMBException
		{
			// Ensure buffer is big enough
			if (numBytes > byteBuffer.capacity()) {
				byteBuffer = ByteBuffer.allocate(numBytes).order(ByteOrder.LITTLE_ENDIAN);
			}
			try {
				byte[] bytes = byteBuffer.array();
				int bytesRead = tarIn.read(bytes, 0, numBytes);
				byteBuffer.position(numBytes);
				if (bytesRead < numBytes) {
					return null;
				}
				// Prepare buffer for reading and return
				byteBuffer.flip();
				return byteBuffer;
			} catch (IOException e) {
				throw new UMBException("I/O error extracting " + numBytes + " bytes from UMB entry \"" + tarIn.getCurrentEntry().getName() + "\"");
			}
		}

		/**
		 * Read the specified number of bytes from the current entry (file) of the archive.
		 * Returns the bytes in a {@link ByteBuffer}. Returns null if there are no bytes
		 * to read (or none were requested). If there are less than {@code numBytes} bytes,
		 * the result is padded with zero bytes.
		 */
		public ByteBuffer readBytesPadded(int numBytes) throws UMBException
		{
			// Ensure buffer is big enough
			if (numBytes > byteBuffer.capacity()) {
				byteBuffer = ByteBuffer.allocate(numBytes).order(ByteOrder.LITTLE_ENDIAN);
			}
			try {
				byte[] bytes = byteBuffer.array();
				int bytesRead = tarIn.read(bytes, 0, numBytes);
				byteBuffer.position(numBytes);
				if (bytesRead <= 0) {
					return null;
				} else if (bytesRead < numBytes) {
					for (int i = bytesRead; i < numBytes; i++) {
						bytes[i] = (byte) 0;
					}
				}
				// Prepare buffer for reading and return
				byteBuffer.flip();
				return byteBuffer;
			} catch (IOException e) {
				throw new UMBException("I/O error extracting " + numBytes + " bytes from UMB entry \"" + tarIn.getCurrentEntry().getName() + "\"");
			}
		}

		/**
		 * Read a string of the specified length from the current entry (file) of the archive.
		 */
		public String readString(int length) throws UMBException
		{
			// Ensure buffer is big enough
			if (length > byteBuffer.capacity()) {
				byteBuffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
			}
			try {
				byte[] bytes = byteBuffer.array();
				int bytesRead = tarIn.read(bytes, 0, length);
				byteBuffer.position(length);
				if (bytesRead < length) {
					return null;
				}
				return new String(bytes, 0, bytesRead, StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UMBException("I/O error extracting string from UMB entry \"" + tarIn.getCurrentEntry().getName() + "\"");
			}
		}

		/**
		 * Read the whole of the current entry (file) of the archive as a string.
		 */
		public String readAsString() throws UMBException
		{
			StringBuilder sb = new StringBuilder();
			try {
				byte[] bytes = byteBuffer.array();
				int bytesRead;
				while ((bytesRead = tarIn.read(bytes)) != -1) {
					sb.append(new String(bytes, 0, bytesRead, StandardCharsets.UTF_8));
				}
				return sb.toString();
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
