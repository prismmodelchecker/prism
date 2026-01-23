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

import it.unimi.dsi.fastutil.doubles.DoubleIterators;
import it.unimi.dsi.fastutil.longs.LongIterators;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Class to handle writing to UMB files.
 */
public class UMBWriter
{
	/**
	 * The (JSON) index to be included in the UMB file.
	 */
	private final UMBIndex umbIndex;

	/**
	 * Representations of the data to be included in the UMB file.
	 */
	private final List<UMBDataFile> umbDataFiles = new ArrayList<>();

	/**
	 * Default buffer size (in bytes) for writing to UMB file.
	 */
	private static int BUFFER_SIZE = 64 * 1024;

	/**
	 * Construct a new {@link UMBWriter} to create a UMB file.
	 */
	public UMBWriter()
	{
		umbIndex = new UMBIndex();
	}

	/**
	 * Get access to the index that will be included in the export file.
	 */
	public UMBIndex getUmbIndex()
	{
		return umbIndex;
	}

	// Methods to add core model info

	/**
	 * Add the state choice offsets, as an iterator of longs
	 */
	public void addStateChoiceOffsets(PrimitiveIterator.OfLong stateChoiceOffsets)
	{
		addLongArray(UMBFormat.STATE_CHOICE_OFFSETS_FILE, stateChoiceOffsets, umbIndex.getNumStates() + 1);
	}

	/**
	 * Add the state choice offsets, as an iterator of ints
	 */
	public void addStateChoiceOffsets(PrimitiveIterator.OfInt stateChoiceOffsets)
	{
		addLongArray(UMBFormat.STATE_CHOICE_OFFSETS_FILE, new UMBUtils.IntToLongIteratorAdapter(stateChoiceOffsets), umbIndex.getNumStates() + 1);
	}

	/**
	 * Add the players that own states (turn-based game models)
	 */
	public void addStatePlayers(PrimitiveIterator.OfInt statePlayers)
	{
		addIntArray(UMBFormat.STATE_PLAYERS, statePlayers, umbIndex.getNumStates());
	}

	/**
	 * Add the initial states, as a BitSet
	 */
	public void addInitialStates(BitSet initialStates) throws UMBException
	{
		addBooleanArray(UMBFormat.INITIAL_STATES_FILE, initialStates, umbIndex.getNumStates());
	}

	/**
	 * Add the initial states, in sparse form, i.e., a list of (long) state indices
	 */
	public void addInitialStates(PrimitiveIterator.OfLong initStates) throws UMBException
	{
		// TODO - can't use BitSet; need BooleanArraySparse
		BitSet bsInitStates = new BitSet();
		initStates.forEachRemaining((long s) -> bsInitStates.set((int) s));
		addBooleanArray(UMBFormat.INITIAL_STATES_FILE, bsInitStates, umbIndex.getNumStates());
	}

	/**
	 * Add the initial states, in sparse form, i.e., a list of (int) state indices
	 */
	public void addInitialStates(PrimitiveIterator.OfInt initStates) throws UMBException
	{
		BitSet bsInitStates = new BitSet();
		initStates.forEachRemaining((int s) -> bsInitStates.set(s));
		addBooleanArray(UMBFormat.INITIAL_STATES_FILE, bsInitStates, umbIndex.getNumStates());
	}

	/**
	 * Add the Markovian states (for Markov automata), as a BitSet
	 */
	public void addMarkovianStates(BitSet markovianStates) throws UMBException
	{
		addBooleanArray(UMBFormat.MARKOVIAN_STATES_FILE, markovianStates, umbIndex.getNumStates());
	}

	/**
	 * Add the Markovian states (for Markov automata), in sparse form, i.e., a list of (long) state indices
	 */
	public void addMarkovianStates(PrimitiveIterator.OfLong markovianStates) throws UMBException
	{
		// TODO - can't use BitSet; need BooleanArraySparse
		BitSet bsMarkovianStates = new BitSet();
		markovianStates.forEachRemaining((long s) -> bsMarkovianStates.set((int) s));
		addBooleanArray(UMBFormat.MARKOVIAN_STATES_FILE, bsMarkovianStates, umbIndex.getNumStates());
	}

	/**
	 * Add the Markovian states (for Markov automata), in sparse form, i.e., a list of (int) state indices
	 */
	public void addMarkovianStates(PrimitiveIterator.OfInt markovianStates) throws UMBException
	{
		BitSet bsMarkovianStates = new BitSet();
		markovianStates.forEachRemaining((int s) -> bsMarkovianStates.set(s));
		addBooleanArray(UMBFormat.MARKOVIAN_STATES_FILE, bsMarkovianStates, umbIndex.getNumStates());
	}

	/**
	 * Add the exit rates for each state of a CTMC.
	 * The type of the values depends on {@link UMBIndex#getExitRateType()}.
	 */
	public void addExitRates(Iterator<?> exitRates) throws UMBException
	{
		addContinuousNumericArray(UMBFormat.STATE_EXIT_RATES_FILE, exitRates, umbIndex.getExitRateType(), umbIndex.getNumStates());
	}

	/**
	 * Add the choice branch offsets, as an iterator of longs
	 */
	public void addChoiceBranchOffsets(PrimitiveIterator.OfLong choiceBranchOffsets)
	{
		addLongArray(UMBFormat.CHOICE_BRANCH_OFFSETS_FILE, choiceBranchOffsets, umbIndex.getNumChoices() + 1);
	}

	/**
	 * Add the choice branch offsets, as an iterator of ints
	 */
	public void addChoiceBranchOffsets(PrimitiveIterator.OfInt choiceBranchOffsets)
	{
		addLongArray(UMBFormat.CHOICE_BRANCH_OFFSETS_FILE, new UMBUtils.IntToLongIteratorAdapter(choiceBranchOffsets), umbIndex.getNumChoices() + 1);
	}

	/**
	 * Add the branch targets, as an iterator of long
	 */
	public void addBranchTargets(PrimitiveIterator.OfLong branchTargets)
	{
		addLongArray(UMBFormat.BRANCH_TARGETS_FILE, branchTargets, umbIndex.getNumBranches());
	}

	/**
	 * Add the branch targets, as an iterator of ints
	 */
	public void addBranchTargets(PrimitiveIterator.OfInt branchTargets)
	{
		addLongArray(UMBFormat.BRANCH_TARGETS_FILE, new UMBUtils.IntToLongIteratorAdapter(branchTargets), umbIndex.getNumBranches());
	}

	/**
	 * Add the branch probabilities, as an iterator of values
	 * The type (and number) of values provided depends on {@link UMBIndex#getBranchProbabilityType()}.
	 * For interval types, this method will expect two values (lower/upper bound, successively) for each branch.
	 * If values are rationals, this method will expect two values (numerator/denominator, successively) for each one.
	 */
	public void addBranchProbabilities(Iterator<?> branchValues) throws UMBException
	{
		addContinuousNumericArray(UMBFormat.BRANCH_PROBABILITIES_FILE, branchValues, umbIndex.getBranchProbabilityType(), umbIndex.getNumBranches());
	}

	/**
	 * Add actions for all choices
	 * @param choiceActionIndices Iterator providing the indices for actions of all choices
	 * @param choiceActionStrings Names of the actions
	 */
	public void addChoiceActions(PrimitiveIterator.OfInt choiceActionIndices, List<String> choiceActionStrings) throws UMBException
	{
		addStringDataToAnnotation(umbIndex.actionsAnnotation, UMBIndex.UMBEntity.CHOICES, choiceActionIndices, choiceActionStrings);
	}

	/**
	 * Add (unnamed) actions for all choices
	 * @param choiceActionIndices Iterator providing the indices for actions of all choices
	 */
	public void addChoiceActions(PrimitiveIterator.OfInt choiceActionIndices) throws UMBException
	{
		// ACTIONS_ANNOTATION is a string annotation but we can just store the indices in this case
		addIntDataToAnnotation(umbIndex.actionsAnnotation, UMBIndex.UMBEntity.CHOICES, choiceActionIndices);
	}

	/**
	 * Add the same action for all choices
	 * @param choiceActionString Name of the action
	 */
	public void addSingleChoiceAction(String choiceActionString) throws UMBException
	{
		addChoiceActions(null, Collections.singletonList(choiceActionString));
	}

	/**
	 * Add actions for all branches
	 * @param branchActionIndices Iterator providing the indices for actions of all branches
	 * @param branchActionStrings Names of the actions
	 */
	public void addBranchActions(PrimitiveIterator.OfInt branchActionIndices, List<String> branchActionStrings) throws UMBException
	{
		addStringDataToAnnotation(umbIndex.actionsAnnotation, UMBIndex.UMBEntity.BRANCHES, branchActionIndices, branchActionStrings);
	}

	/**
	 * Add (unnamed) actions for all branches
	 * @param branchActionIndices Iterator providing the indices for actions of all branches
	 */
	public void addBranchActions(PrimitiveIterator.OfInt branchActionIndices) throws UMBException
	{
		// ACTIONS_ANNOTATION is a string annotation but we can just store the indices in this case
		addIntDataToAnnotation(umbIndex.actionsAnnotation, UMBIndex.UMBEntity.BRANCHES, branchActionIndices);
	}

	/**
	 * Add the same action for all branches
	 * @param branchActionString Name of the action
	 */
	public void addSingleBranchAction(String branchActionString) throws UMBException
	{
		addBranchActions(null, Collections.singletonList(branchActionString));
	}

	/**
	 * Add the (deterministic) observations for all states, as an iterator of longs
	 * @param stateObservations Iterator providing the observations for all states
	 */
	public void addStateObservations(PrimitiveIterator.OfLong stateObservations) throws UMBException
	{
		addObservations(UMBIndex.UMBEntity.STATES, stateObservations);
	}

	/**
	 * Add the (deterministic) observations for all states, as an iterator of ints
	 * @param stateObservations Iterator providing the observations for all states
	 */
	public void addStateObservations(PrimitiveIterator.OfInt stateObservations) throws UMBException
	{
		addObservations(UMBIndex.UMBEntity.STATES, stateObservations);
	}

	/**
	 * Add the (deterministic) observations for all branches, as an iterator of longs
	 * @param branchObservations Iterator providing the observations for all branches
	 */
	public void addBranchObservations(PrimitiveIterator.OfLong branchObservations) throws UMBException
	{
		addObservations(UMBIndex.UMBEntity.BRANCHES, branchObservations);
	}

	/**
	 * Add the (deterministic) observations for all branches, as an iterator of ints
	 * @param branchObservations Iterator providing the observations for all branches
	 */
	public void addBranchObservations(PrimitiveIterator.OfInt branchObservations) throws UMBException
	{
		addObservations(UMBIndex.UMBEntity.BRANCHES, branchObservations);
	}

	/**
	 * Add the (deterministic) observations for some entity (states, branches), as an iterator of longs
	 * @param entity The entity for which observations are being added
	 * @param observations Iterator providing the observations
	 */
	public void addObservations(UMBIndex.UMBEntity entity, PrimitiveIterator.OfLong observations) throws UMBException
	{
		addLongDataToAnnotation(umbIndex.observationsAnnotation, entity, observations);
	}

	/**
	 * Add the (deterministic) observations for some entity (states, branches), as an iterator of ints
	 * @param entity The entity for which observations are being added
	 * @param observations Iterator providing the observations
	 */
	public void addObservations(UMBIndex.UMBEntity entity, PrimitiveIterator.OfInt observations) throws UMBException
	{
		addLongDataToAnnotation(umbIndex.observationsAnnotation, entity, new UMBUtils.IntToLongIteratorAdapter(observations));
	}

	// Methods to add standard annotations

	/**
	 * Add a new state AP annotation.
	 * @param apAlias AP annotation alias
	 * @param apStates BitSet providing indices of states satisfying the AP
	 */
	public void addStateAP(String apAlias, BitSet apStates) throws UMBException
	{
		UMBIndex.Annotation annotation = umbIndex.addAnnotation(UMBFormat.AP_ANNOTATIONS_GROUP, apAlias, UMBType.create(UMBType.Type.BOOL));
		addBooleanDataToAnnotation(annotation, UMBIndex.UMBEntity.STATES, apStates);
	}

	/**
	 * Add a new reward annotation, for now without any data attached.
	 * If the alias (name) is non-empty, there should not already exist a reward annotation with the same alias.
	 * @param rewardAlias Optional alias (name) for the rewards (can be omitted: "" or null)
	 * @param rational Whether the reward values are rational numbers
	 */
	public String addRewards(String rewardAlias, boolean rational) throws UMBException
	{
		UMBType type = UMBType.contNum(rational);
		UMBIndex.Annotation annotation = umbIndex.addAnnotation(UMBFormat.REWARD_ANNOTATIONS_GROUP, rewardAlias, type);
		return annotation.id;
	}

	/**
	 * Add state rewards to a previously created reward annotation.
	 * @param rewardID Reward annotation ID
	 * @param stateRewards Iterator providing values defining the reward
	 */
	public void addStateRewardsByID(String rewardID, Iterator<?> stateRewards) throws UMBException
	{
		UMBIndex.Annotation annotation = umbIndex.getAnnotation(UMBFormat.REWARD_ANNOTATIONS_GROUP, rewardID);
		addContinuousNumericDataToAnnotation(annotation, UMBIndex.UMBEntity.STATES, stateRewards, annotation.getType());
	}

	/**
	 * Add choice rewards to a previously created reward annotation.
	 * @param rewardID Reward annotation ID
	 * @param choiceRewards Iterator providing values defining the reward
	 */
	public void addChoiceRewardsByID(String rewardID, Iterator<?> choiceRewards) throws UMBException
	{
		UMBIndex.Annotation annotation = umbIndex.getAnnotation(UMBFormat.REWARD_ANNOTATIONS_GROUP, rewardID);
		addContinuousNumericDataToAnnotation(annotation, UMBIndex.UMBEntity.CHOICES, choiceRewards, annotation.getType());
	}

	/**
	 * Add branch rewards to a previously created reward annotation.
	 * @param rewardID Reward annotation ID
	 * @param branchRewards Iterator providing values defining the reward
	 */
	public void addBranchRewardsByID(String rewardID, Iterator<?> branchRewards) throws UMBException
	{
		UMBIndex.Annotation annotation = umbIndex.getAnnotation(UMBFormat.REWARD_ANNOTATIONS_GROUP, rewardID);
		addContinuousNumericDataToAnnotation(annotation, UMBIndex.UMBEntity.BRANCHES, branchRewards, annotation.getType());
	}

	/**
	 * Add a new reward annotation, applied to states.
	 * If the alias (name) is non-empty, there should not already exist a reward annotation with the same alias.
	 * If you want to add a reward annotation that applies to multiple entities,
	 * e.g., to both states and branches, use first {@link #addRewards(String, boolean)}
	 * and then {@link #addStateRewardsByID} and {@link #addBranchRewardsByID}.
	 * @param rewardAlias Optional alias (name) for the rewards (can be omitted: "" or null)
	 * @param rational Whether the reward values are rational numbers
	 * @param stateRewards Iterator providing values defining the reward
	 */
	public void addStateRewards(String rewardAlias, boolean rational, Iterator<?> stateRewards) throws UMBException
	{
		addStateRewardsByID(addRewards(rewardAlias, rational), stateRewards);
	}

	/**
	 * Add a new reward annotation, applied to choices.
	 * If the alias (name) is non-empty, there should not already exist a reward annotation with the same alias.
	 * If you want to add a reward annotation that applies to multiple entities,
	 * e.g., to both states and branches, use first {@link #addRewards(String, boolean)}
	 * and then {@link #addStateRewardsByID} and {@link #addBranchRewardsByID}.
	 * @param rewardAlias Optional alias (name) for the rewards (can be omitted: "" or null)
	 * @param rational Whether the reward values are rational numbers
	 * @param choiceRewards Iterator providing values defining the reward
	 */
	public void addChoiceRewards(String rewardAlias, boolean rational, Iterator<?> choiceRewards) throws UMBException
	{
		addChoiceRewardsByID(addRewards(rewardAlias, rational), choiceRewards);
	}

	/**
	 * Add a new reward annotation, applied to branches.
	 * If the alias (name) is non-empty, there should not already exist a reward annotation with the same alias.
	 * If you want to add a reward annotation that applies to multiple entities,
	 * e.g., to both states and branches, use first {@link #addRewards(String, boolean)}
	 * and then {@link #addStateRewardsByID} and {@link #addBranchRewardsByID}.
	 * @param rewardAlias Optional alias (name) for the rewards (can be omitted: "" or null)
	 * @param rational Whether the reward values are rational numbers
	 * @param branchRewards Iterator providing values defining the reward
	 */
	public void addBranchRewards(String rewardAlias, boolean rational, Iterator<?> branchRewards) throws UMBException
	{
		addBranchRewardsByID(addRewards(rewardAlias, rational), branchRewards);
	}

	// Methods to add annotations

	/**
	 * Add a new annotation, for now without any data attached.
	 * If an alias is provided, there should not already exist
	 * an annotation in the same group with the same alias.
	 * @param group The ID of the group
	 * @param alias Optional alias (name) for the annotation ("" or null if not needed)
	 * @param type The type of the values to be stored in the annotation
	 */
	public UMBIndex.Annotation addAnnotation(String group, String alias, UMBType type) throws UMBException
	{
		return umbIndex.addAnnotation(group, alias, type);
	}

	/**
	 * Add a new Boolean-valued annotation.
	 * If an alias is provided, there should not already exist
	 * an annotation in the same group with the same alias.
	 * @param group The ID of the group
	 * @param alias Optional alias (name) for the annotation ("" or null if not needed)
	 * @param appliesTo The entity to which the annotation applies
	 * @param bitset BitSet providing data
	 */
	public void addBooleanAnnotation(String group, String alias, UMBIndex.UMBEntity appliesTo, BitSet bitset) throws UMBException
	{
		UMBIndex.Annotation annotation = addAnnotation(group, alias, UMBType.create(UMBType.Type.BOOL));
		addBooleanDataToAnnotation(annotation, appliesTo, bitset);
	}

	/**
	 * Add a new double-valued annotation.
	 * If an alias is provided, there should not already exist
	 * an annotation in the same group with the same alias.
	 * @param group The ID of the group
	 * @param alias Optional alias (name) for the annotation ("" or null if not needed)
	 * @param appliesTo The entity to which the annotation applies
	 * @param doubleValues Iterator providing data
	 */
	public void addDoubleAnnotation(String group, String alias, UMBIndex.UMBEntity appliesTo, PrimitiveIterator.OfDouble doubleValues) throws UMBException
	{
		UMBIndex.Annotation annotation = addAnnotation(group, alias, UMBType.create(UMBType.Type.DOUBLE));
		addDoubleDataToAnnotation(annotation, appliesTo, doubleValues);
	}

	/**
	 * Add new boolean-valued data to an existing annotation.
	 * @param annotation The annotation
	 * @param appliesTo The entity to which the annotation applies
	 * @param bitset BitSet providing data
	 */
	public void addBooleanDataToAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, BitSet bitset) throws UMBException
	{
		if (annotation.appliesTo(appliesTo)) {
			throw new UMBException("Duplicate data for " + appliesTo + "s in annotation \"" + annotation.id + "\" in group \"" + annotation.group + "\"");
		}
		annotation.addAppliesTo(appliesTo);
		long annotationSize = umbIndex.getEntityCount(appliesTo);
		addBooleanArray(annotation.getFilename(appliesTo), bitset, annotationSize);
	}

	/**
	 * Add new int-valued data to an existing annotation.
	 * @param annotation The annotation
	 * @param appliesTo The entity to which the annotation applies
	 * @param intValues Iterator providing data
	 */
	public void addIntDataToAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, PrimitiveIterator.OfInt intValues) throws UMBException
	{
		if (annotation.appliesTo(appliesTo)) {
			throw new UMBException("Duplicate data for " + appliesTo + "s in annotation \"" + annotation.id + "\" in group \"" + annotation.group + "\"");
		}
		annotation.addAppliesTo(appliesTo);
		long annotationSize = umbIndex.getEntityCount(appliesTo);
		addIntArray(annotation.getFilename(appliesTo), intValues, annotationSize);
	}

	/**
	 * Add new long-valued data to an existing annotation.
	 * @param annotation The annotation
	 * @param appliesTo The entity to which the annotation applies
	 * @param longValues Iterator providing data
	 */
	public void addLongDataToAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, PrimitiveIterator.OfLong longValues) throws UMBException
	{
		if (annotation.appliesTo(appliesTo)) {
			throw new UMBException("Duplicate data for " + appliesTo + "s in annotation \"" + annotation.id + "\" in group \"" + annotation.group + "\"");
		}
		annotation.addAppliesTo(appliesTo);
		long annotationSize = umbIndex.getEntityCount(appliesTo);
		addLongArray(annotation.getFilename(appliesTo), longValues, annotationSize);
	}

	/**
	 * Add new double-valued data to an existing annotation.
	 * @param annotation The annotation
	 * @param appliesTo The entity to which the annotation applies
	 * @param doubleValues Iterator providing data
	 */
	public void addDoubleDataToAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, PrimitiveIterator.OfDouble doubleValues) throws UMBException
	{
		if (annotation.appliesTo(appliesTo)) {
			throw new UMBException("Duplicate data for " + appliesTo + "s in annotation \"" + annotation.id + "\" in group \"" + annotation.group + "\"");
		}
		annotation.addAppliesTo(appliesTo);
		long annotationSize = umbIndex.getEntityCount(appliesTo);
		addDoubleArray(annotation.getFilename(appliesTo), doubleValues, annotationSize);
	}

	public void addContinuousNumericDataToAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, Iterator<?> values, UMBType valueType) throws UMBException
	{
		if (annotation.appliesTo(appliesTo)) {
			throw new UMBException("Duplicate data for " + appliesTo + "s in annotation \"" + annotation.id + "\" in group \"" + annotation.group + "\"");
		}
		annotation.addAppliesTo(appliesTo);
		long annotationSize = umbIndex.getEntityCount(appliesTo);
		addContinuousNumericArray(annotation.getFilename(appliesTo), values, valueType, annotationSize);
	}

	public void addStringDataToAnnotation(UMBIndex.Annotation annotation, UMBIndex.UMBEntity appliesTo, PrimitiveIterator.OfInt indices, List<String> strings) throws UMBException
	{
		if (annotation.appliesTo(appliesTo)) {
			throw new UMBException("Duplicate data for " + appliesTo + "s in annotation \"" + annotation.id + "\" in group \"" + annotation.group + "\"");
		}
		annotation.addAppliesTo(appliesTo);
		String folderName = annotation.getFolderName(appliesTo);
		addStringsFiles(strings, UMBFormat.stringOffsetsFile(folderName), UMBFormat.stringsFile(folderName));
		if (indices != null) {
			long annotationSize = umbIndex.getEntityCount(appliesTo);
			addIntArray(annotation.getFilename(appliesTo), indices, annotationSize);
		}
	}

	/**
	 * Add the files encoding a list of strings.
	 * @param strings The strings
	 * @param stringOffsetsFilename The name of the file to contain the string offsets
	 * @param stringsFilename The name of file to contain the string data
	 */
	private void addStringsFiles(List<String> strings, String stringOffsetsFilename, String stringsFilename) throws UMBException
	{
		int numStrings = strings.size();
		long[] stringOffsets = new long[numStrings + 1];
		stringOffsets[0] = 0;
		for (int i = 0; i < numStrings; i++) {
			stringOffsets[i + 1] = stringOffsets[i] + strings.get(i).getBytes().length;
		}
		PrimitiveIterator.OfLong it = Arrays.stream(stringOffsets).iterator();
		addLongArray(stringOffsetsFilename, it, strings.size() + 1);
		addStringList(stringsFilename, strings);
	}

	// Methods to add valuations

	/**
	 * Add a new description for the valuations to be attached to states,
	 * extracted from a {@link UMBBitPacking} object.
	 * @param unique Whether the valuations are unique across states
	 * @param bitPacking Definition of valuation contents
	 */
	public void addStateValuationDescription(boolean unique, UMBBitPacking bitPacking) throws UMBException
	{
		addValuationDescription(UMBIndex.UMBEntity.STATES, unique, bitPacking);
	}

	/**
	 * Add state valuations, i.e., variable values for each state, encoded as a bitstring
	 * @param stateValuations Iterator providing bitstrings defining the state valuations
	 * @param numBytes Number of bytes in each bitstring
	 */
	public void addStateValuations(Iterator<UMBBitString> stateValuations, int numBytes) throws UMBException
	{
		addValuations(UMBIndex.UMBEntity.STATES, stateValuations, numBytes);
	}

	/**
	 * Add state valuations, i.e., variable values for each state, encoded as a bitstring
	 * @param stateValuations Iterator providing bitstrings defining the state valuations
	 * @param bitPacking Information about how the bitstrings are packed
	 */
	public void addStateValuations(Iterator<UMBBitString> stateValuations, UMBBitPacking bitPacking) throws UMBException
	{
		addValuations(UMBIndex.UMBEntity.STATES, stateValuations, bitPacking);
	}

	/**
	 * Add a new description for the valuations to be attached to observations,
	 * extracted from a {@link UMBBitPacking} object.
	 * @param unique Whether the valuations are unique across states
	 * @param bitPacking Definition of valuation contents
	 */
	public void addObservationValuationDescription(boolean unique, UMBBitPacking bitPacking) throws UMBException
	{
		addValuationDescription(UMBIndex.UMBEntity.OBSERVATIONS, unique, bitPacking);
	}

	/**
	 * Add observation valuations, i.e., observable values for each observation, encoded as a bitstring
	 * @param observationValuations Iterator providing bitstrings defining the observation valuations
	 * @param numBytes Number of bytes in each bitstring
	 */
	public void addObservationValuations(Iterator<UMBBitString> observationValuations, int numBytes) throws UMBException
	{
		addValuations(UMBIndex.UMBEntity.OBSERVATIONS, observationValuations, numBytes);
	}

	/**
	 * Add observation valuations, i.e., observable values for each observation, encoded as a bitstring
	 * @param observationValuations Iterator providing bitstrings defining the observation valuations
	 * @param bitPacking Information about how the bitstrings are packed
	 */
	public void addObservationValuations(Iterator<UMBBitString> observationValuations, UMBBitPacking bitPacking) throws UMBException
	{
		addValuations(UMBIndex.UMBEntity.OBSERVATIONS, observationValuations, bitPacking);
	}

	/**
	 * Add a new description for the valuations to be attached to some model entity,
	 * extracted from a {@link UMBBitPacking} object.
	 * @param entity The entity to which the valuations apply
	 * @param unique Whether the valuations are unique across the entity
	 * @param bitPacking Definition of valuation contents
	 */
	public void addValuationDescription(UMBIndex.UMBEntity entity, boolean unique, UMBBitPacking bitPacking) throws UMBException
	{
		umbIndex.addSingleValuationDescription(entity, unique, bitPacking);
	}

	/**
	 * Add valuations for an entity, i.e., variable values for each one, encoded as a bitstring
	 * @param valuations Iterator providing bitstrings defining the valuations
	 * @param entity The entity to which the valuations apply
	 * @param numBytes Number of bytes in each bitstring
	 */
	public void addValuations(UMBIndex.UMBEntity entity, Iterator<UMBBitString> valuations, int numBytes) throws UMBException
	{
		addBitStringArray(UMBFormat.valuationsFile(entity), valuations, numBytes, umbIndex.getEntityCount(entity));
	}

	/**
	 * Add valuations for an entity, i.e., variable values for each one, encoded as a bitstring
	 * @param entity The entity to which the valuations apply
	 * @param valuations Iterator providing bitstrings defining the valuations
	 * @param bitPacking Information about how the bitstrings are packed
	 */
	public void addValuations(UMBIndex.UMBEntity entity, Iterator<UMBBitString> valuations, UMBBitPacking bitPacking) throws UMBException
	{
		addBitStringArray(UMBFormat.valuationsFile(entity), valuations, bitPacking, umbIndex.getEntityCount(entity));
	}

	// Methods to add binary files

	public void addBooleanArray(String name, BitSet booleanValues, long size)
	{
		umbDataFiles.add(new BooleanArray(booleanValues, size, name));
	}

	public void addCharArray(String name, PrimitiveIterator.OfLong longValues, long size)
	{
		umbDataFiles.add(new LongArray(longValues, size, name));
	}

	public void addIntArray(String name, PrimitiveIterator.OfInt intValues, long size)
	{
		umbDataFiles.add(new IntArray(intValues, size, name));
	}

	public void addLongArray(String name, PrimitiveIterator.OfLong longValues, long size)
	{
		umbDataFiles.add(new LongArray(longValues, size, name));
	}

	public void addDoubleArray(String name, PrimitiveIterator.OfDouble doubleValues, long size)
	{
		umbDataFiles.add(new DoubleArray(doubleValues, size, name));
	}

	public void addBigIntegerArray(String name, Iterator<BigInteger> bigIntegerValues, int numLongs, long size)
	{
		umbDataFiles.add(new BigIntegerArray(bigIntegerValues, numLongs, size, name));
	}

	private void addContinuousNumericArray(String name, Iterator<?> values, UMBType type, long size) throws UMBException
	{
		long sizeNew = type.type.isInterval() ? size * 2 : size;
		if (type.type.isDouble()) {
			addDoubleArray(name, DoubleIterators.asDoubleIterator(values), sizeNew);
		} else if (type.type.isRational()) {
			addLongArray(name, LongIterators.asLongIterator(values), sizeNew * 2);
		} else {
			throw new UMBException("Unsupported continuous numeric type " + type);
		}
	}

	public void addBitStringArray(String name, Iterator<UMBBitString> bitStrings, int numBytes, long size)
	{
		umbDataFiles.add(new BitStringArray(bitStrings, numBytes, size, name));
	}

	public void addBitStringArray(String name, Iterator<UMBBitString> bitStrings, UMBBitPacking bitPacking, long size)
	{
		umbDataFiles.add(new BitStringArray(bitStrings, bitPacking, size, name));
	}

	public void addStringList(String name, List<String> strings)
	{
		umbDataFiles.add(new StringListFile(strings, name));
	}

	/**
	 * Export content to a UMB file.
	 * @param fileOut The file to export to.
	 */
	public void export(File fileOut) throws UMBException
	{
		export(fileOut, true);
	}

	/**
	 * Export content to a UMB file.
	 * @param fileOut The file to export to.
	 * @param zipped Whether to zip the file
	 */
	public void export(File fileOut, boolean zipped) throws UMBException
	{
		export(fileOut, zipped, null);
	}

	/**
	 * Export content to a UMB file.
	 * @param fileOut The file to export to.
	 * @param zipped Whether to zip the file
	 * @param compressionFormat How to zip the file (null means use default)
	 */
	public void export(File fileOut, boolean zipped, UMBFormat.CompressionFormat compressionFormat) throws UMBException
	{
		UMBOut umbOut = new UMBOut(fileOut, zipped, compressionFormat);
		exportIndex(umbOut);
		for (UMBDataFile umbDataFile : umbDataFiles) {
			exportUMBFile(umbDataFile, umbOut);
		}
		umbOut.close();
	}

	/**
	 * Export content to a textual representation of a UMB file.
	 * @param sb Where to write the text to.
	 */
	public void exportAsText(StringBuffer sb) throws UMBException
	{
		exportIndexToText(sb);
		for (UMBDataFile umbDataFile : umbDataFiles) {
			exportUMBFileToText(umbDataFile, sb);
		}
	}

	/**
	 * Export the index to a UMB file.
	 */
	private void exportIndex(UMBOut umbOut) throws UMBException
	{
		exportTextToTar(umbIndex.toJSON(), UMBFormat.INDEX_FILE, umbOut);
	}

	/**
	 * Export the index to a StringBuffer
	 */
	private void exportIndexToText(StringBuffer sb)
	{
		exportTextToText(umbIndex.toJSON(), new File(UMBFormat.INDEX_FILE), sb);
	}

	private void exportTextToTar(String text, String filename, UMBOut umbOut) throws UMBException
	{
		byte[] bytes = text.getBytes();
		umbOut.createArchiveEntry(filename, bytes.length);
		umbOut.write(bytes, 0, bytes.length);
		umbOut.closeArchiveEntry();
	}

	private void exportUMBFile(UMBDataFile umbDataFile, UMBOut umbOut) throws UMBException
	{
		try {
			umbOut.createArchiveEntry(umbDataFile.name, umbDataFile.totalBytes());
			Iterator<ByteBuffer> byteIter = umbDataFile.byteIterator();
			ByteBuffer buffer;
			while (byteIter.hasNext()) {
				buffer = byteIter.next();
				umbOut.write(buffer.array(), 0, buffer.position());
			}
			umbOut.closeArchiveEntry();
		} catch (RuntimeException e) {
			// Errors may occur in iterators so catch runtime exceptions here
			throw new UMBException("Error exporting UMB file: " + e.getMessage());
		}
	}

	private void exportTextToText(String text, File file, StringBuffer sb)
	{
		sb.append("/" + file.getName() + ":\n");
		sb.append(text);
		sb.append("\n");
	}

	private void exportUMBFileToText(UMBDataFile umbDataFile, StringBuffer sb) throws UMBException
	{
		try {
			sb.append("/" + umbDataFile.name + ":\n");
			sb.append(umbDataFile.toText());
			sb.append("\n");
		} catch (RuntimeException e) {
			// Errors may occur in iterators so catch runtime exceptions here
			throw new UMBException("Error exporting UMB file: " + e.getMessage());
		}
	}

	private static String toArrayString(Iterator<?> iter)
	{
		StringJoiner sj = new StringJoiner(",", "[", "]");
		iter.forEachRemaining(o -> sj.add(o.toString()));
		return sj.toString();
	}

	/**
	 * Class to manage writing to the zipped archive for a UMB file
	 */
	private static class UMBOut
	{
		/** Output stream for zip file */
		private final OutputStream fsOut;
		/** Output stream for zipping */
		private final CompressorOutputStream zipOut;
		/** Output stream for tar file */
		private final ArchiveOutputStream tarOut;

		/**
		 * Open a new UMB file for writing
		 * @param fileOut The file to write to
		 */
		public UMBOut(File fileOut) throws UMBException
		{
			this(fileOut, true);
		}

		/**
		 * Open a new UMB file for writing
		 * @param fileOut The file to write to
		 * @param zipped Whether to zip the file
		 */
		public UMBOut(File fileOut, boolean zipped) throws UMBException
		{
			this(fileOut, zipped, null);
		}

		/**
		 * Open a new UMB file for writing
		 * @param fileOut The file to write to
		 * @param zipped Whether to zip the file
		 * @param compressionFormat How to zip the file (null means use the default)
		 */
		public UMBOut(File fileOut, boolean zipped, UMBFormat.CompressionFormat compressionFormat) throws UMBException
		{
			try {
				// Open file/zip/tar
				fsOut = new BufferedOutputStream(Files.newOutputStream(fileOut.toPath()));
				if (zipped) {
					if (compressionFormat == null) {
						compressionFormat = UMBFormat.DEFAULT_COMPRESSION_FORMAT;
					}
					zipOut = new CompressorStreamFactory().createCompressorOutputStream(compressionFormat.extension(), fsOut);
					tarOut = new TarArchiveOutputStream(zipOut);
				} else {
					zipOut = null;
					tarOut = new TarArchiveOutputStream(fsOut);
				}
			} catch (IOException e) {
				throw new UMBException("Could not create UMB file: " + e.getMessage());
			} catch (CompressorException e) {
				throw new UMBException("Could not create zip for UMB file: " + e.getMessage());
			}
		}

		/**
		 * Create an entry (a file) within the archive for subsequent writing.
		 * @param name Name of the file
		 * @param size Size of the file (number of bytes)
		 */
		public void createArchiveEntry(String name, long size) throws UMBException
		{
			try {
				File file = new File(name);
				TarArchiveEntry entry = new TarArchiveEntry(file);
				entry.setSize(size);
				tarOut.putArchiveEntry(entry);
			} catch (IOException e) {
				throw new UMBException("I/O error writing \"" + name + "\" to UMB file: " + e.getMessage());
			}
		}

		/**
		 * Close the current entry (file) of the archive.
		 */
		public void closeArchiveEntry() throws UMBException
		{
			try {
				tarOut.closeArchiveEntry();
			} catch (IOException e) {
				throw new UMBException("I/O error writing to UMB file: " + e.getMessage());
			}
		}

		/**
		 * Write some data (bytes) to the current entry.
		 * @param bytes The data, as an array of bytes
		 * @param off The offset into the array to start from
		 * @param len The number of bytes to write
		 */
		public void write(byte[] bytes, int off, int len) throws UMBException
		{
			try {
				tarOut.write(bytes, off, len);
			} catch (IOException e) {
				throw new UMBException("I/O error writing to UMB file");
			}
		}

		/**
		 * Close the UMB file.
		 */
		public void close() throws UMBException
		{
			try {
				if (tarOut != null) {
					tarOut.finish();
				}
				if (zipOut != null) {
					zipOut.close();
				}
				if (fsOut != null) {
					fsOut.close();
				}
			} catch (IOException e) {
				throw new UMBException("I/O error closing UMB file");
			}
		}
	}

	/**
	 * Classes representing the various data files stored in a UMB file.
	 */
	abstract static class UMBDataFile
	{
		protected String name;
		protected ByteBuffer buffer;

		public abstract long totalBytes();
		public abstract Iterator<ByteBuffer> byteIterator();
		public abstract String toText() throws UMBException;
	}

	/**
	 * A UMB data file to be stored containing an array of values.
	 */
	abstract static class Array extends UMBDataFile
	{
		protected long size;
		protected int bufferSize;

		public abstract int numBytes();
		public abstract boolean hasNextBytes();
		public abstract void encodeNextBytes();

		public long totalBytes()
		{
			return size * numBytes();
		}

		public abstract Iterator<? extends Object> iterator();

		@Override
		public Iterator<ByteBuffer> byteIterator()
		{
			return new Iterator<>()
			{
				{
					buffer = ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
				}

				@Override
				public boolean hasNext()
				{
					return hasNextBytes();
				}

				@Override
				public ByteBuffer next()
				{
					buffer.clear();
					encodeNextBytes();
					return buffer;
				}

			};
		}

		public String nextAsText() throws UMBException
		{
			return iterator().next().toString();
		}

		public String toText() throws UMBException
		{
			StringJoiner sj = new StringJoiner(",", "[", "]");
			while (hasNextBytes()) {
				sj.add(nextAsText());
			}
			return sj.toString();
		}
	}

	/**
	 * A UMB data file to be stored containing an array of booleans.
	 */
	static class BooleanArray extends Array
	{
		protected BitSet booleanValues;
		protected long[] booleanLongs;
		protected int posn;

		public BooleanArray(BitSet booleanValues, long size, String name)
		{
			this.booleanValues = booleanValues;
			booleanLongs = booleanValues.toLongArray();
			this.size = size;
			this.name = name;
			posn = 0;
		}

		@Override
		public long totalBytes()
		{
			return 8 * (((size - 1)/ 64) + 1);
		}

		@Override
		public Iterator<Boolean> iterator()
		{
			return new Iterator<>()
			{
				@Override
				public boolean hasNext()
				{
					return posn < size;
				}

				@Override
				public Boolean next()
				{
					return booleanValues.get(posn++);
				}
			};
		}

		@Override
		public int numBytes()
		{
			return Long.BYTES; // 8
		}

		@Override
		public boolean hasNextBytes()
		{
			return posn < size;
		}

		@Override
		public void encodeNextBytes()
		{
			while (posn < size && buffer.remaining() >= numBytes()) {
				int posn2 = posn / 64;
				buffer.putLong(booleanLongs.length > posn2 ? booleanLongs[posn2] : 0);
				posn += 64;
			}
		}

		@Override
		public String toText()
		{
			StringJoiner sj = new StringJoiner("", "[", "]");
			iterator().forEachRemaining(b -> sj.add(b ? "1" : "0"));
			return sj.toString();
		}
	}

	/**
	 * A UMB data file to be stored containing an array of ints.
	 */
	static class IntArray extends Array
	{
		protected PrimitiveIterator.OfInt intValues;

		public IntArray(PrimitiveIterator.OfInt intValues, long size, String name)
		{
			this.intValues = intValues;
			this.size = size;
			this.name = name;
		}

		@Override
		public PrimitiveIterator.OfInt iterator()
		{
			return intValues;
		}

		@Override
		public int numBytes()
		{
			return Integer.BYTES; // 4
		}

		@Override
		public boolean hasNextBytes()
		{
			return intValues.hasNext();
		}

		@Override
		public void encodeNextBytes()
		{
			while (intValues.hasNext() && buffer.remaining() >= numBytes()) {
				buffer.putInt(intValues.nextInt());
			}
		}
	}

	/**
	 * A UMB data file to be stored containing an array of longs.
	 */
	static class LongArray extends Array
	{
		protected PrimitiveIterator.OfLong longValues;

		public LongArray(PrimitiveIterator.OfLong longValues, long size, String name)
		{
			this.longValues = longValues;
			this.size = size;
			this.name = name;
		}

		@Override
		public PrimitiveIterator.OfLong iterator()
		{
			return longValues;
		}

		@Override
		public int numBytes()
		{
			return Long.BYTES; // 8
		}

		@Override
		public boolean hasNextBytes()
		{
			return longValues.hasNext();
		}

		@Override
		public void encodeNextBytes()
		{
			while (longValues.hasNext() && buffer.remaining() >= numBytes()) {
				buffer.putLong(longValues.nextLong());
			}
		}
	}

	/**
	 * A UMB data file to be stored containing an array of BigIntegers.
	 */
	static class BigIntegerArray extends Array
	{
		protected Iterator<BigInteger> bigIntegerValues;
		protected int intSize;

		public BigIntegerArray(Iterator<BigInteger> bigIntegerValues, int numLongs, long size, String name)
		{
			this.bigIntegerValues = bigIntegerValues;
			this.intSize = numLongs;
			this.size = size;
			this.name = name;
		}

		@Override
		public Iterator<BigInteger> iterator()
		{
			return bigIntegerValues;
		}

		@Override
		public int numBytes()
		{
			return intSize * Long.BYTES;
		}

		@Override
		public boolean hasNextBytes()
		{
			return bigIntegerValues.hasNext();
		}

		@Override
		public void encodeNextBytes()
		{
			while (bigIntegerValues.hasNext() && buffer.remaining() >= numBytes()) {
				BigInteger b = bigIntegerValues.next();
				for (int i = 0; i < intSize; i++) {
					buffer.putLong(b.longValue());
					b = b.shiftRight(64);
				}
			}
		}
	}

	/**
	 * A UMB data file to be stored containing an array of doubles.
	 */
	class DoubleArray extends Array
	{
		protected PrimitiveIterator.OfDouble doubleValues;

		public DoubleArray(PrimitiveIterator.OfDouble doubleValues, long size, String name)
		{
			this.doubleValues = doubleValues;
			this.size = size;
			this.name = name;
		}

		@Override
		public PrimitiveIterator.OfDouble iterator()
		{
			return doubleValues;
		}

		@Override
		public int numBytes()
		{
			return Double.BYTES; // 8
		}

		@Override
		public boolean hasNextBytes()
		{
			return doubleValues.hasNext();
		}

		public String nextAsText()
		{
			return formatDouble(doubleValues.nextDouble());
		}

		@Override
		public void encodeNextBytes()
		{
			while (doubleValues.hasNext() && buffer.remaining() >= numBytes()) {
				buffer.putDouble(doubleValues.nextDouble());
			}
		}
	}

	/**
	 * A UMB data file to be stored containing an array of (fixed size) bit strings.
	 */
	class BitStringArray extends Array
	{
		protected Iterator<UMBBitString> bitStrings;
		protected UMBBitPacking bitPacking; // optional
		protected int numBytes;

		public BitStringArray(Iterator<UMBBitString> bitStrings, int numBytes, long size, String name)
		{
			this.bitStrings = bitStrings;
			this.bitPacking = null;
			this.numBytes = numBytes;
			this.size = size;
			this.name = name;
		}

		public BitStringArray(Iterator<UMBBitString> bitStrings, UMBBitPacking bitPacking, long size, String name)
		{
			this.bitStrings = bitStrings;
			this.bitPacking = bitPacking;
			this.numBytes = bitPacking.getTotalNumBytes();
			this.size = size;
			this.name = name;
		}

		@Override
		public Iterator<UMBBitString> iterator()
		{
			return bitStrings;
		}

		@Override
		public int numBytes()
		{
			return numBytes;
		}

		@Override
		public boolean hasNextBytes()
		{
			return bitStrings.hasNext();
		}

		public String nextAsText() throws UMBException
		{
			UMBBitString bitString = bitStrings.next();
			//return bitPacking == null ? bitString.toString() : bitPacking.formatBitString(bitString);
			return bitPacking == null ? bitString.toString() : bitPacking.decodeBitString(bitString);
		}

		@Override
		public void encodeNextBytes()
		{
			while (bitStrings.hasNext() && buffer.remaining() >= numBytes()) {
				buffer.put(bitStrings.next().bytes);
			}
		}
	}

	/**
	 * A UMB data file containing a list of strings
	 */
	static class StringListFile extends UMBDataFile
	{
		protected List<String> strings;
		protected int totalBytes;
		protected byte[] stringBytes;

		public StringListFile(List<String> strings, String name)
		{
			this.name = name;
			this.strings = strings;
			int numStrings = strings.size();
			byte[][] stringsAsBytes = new byte[numStrings][];
			totalBytes = 0;
			for (int i = 0; i < numStrings; i++) {
				stringsAsBytes[i] = strings.get(i).getBytes(StandardCharsets.UTF_8);
				totalBytes += stringsAsBytes[i].length;
			}
			stringBytes = new byte[totalBytes];
			int count = 0;
			for (int i = 0; i < numStrings; i++) {
				System.arraycopy(stringsAsBytes[i], 0, stringBytes, count, stringsAsBytes[i].length);
				count += stringsAsBytes[i].length;
			}
		}

		@Override
		public long totalBytes()
		{
			return totalBytes;
		}

		@Override
		public Iterator<ByteBuffer> byteIterator()
		{
			return new Iterator<>()
			{
				boolean hasNext = true;
				{
					buffer = ByteBuffer.wrap(stringBytes);
					buffer.position(totalBytes);
				}

				@Override
				public boolean hasNext()
				{
					return hasNext;
				}

				@Override
				public ByteBuffer next()
				{
					hasNext = false;
					return buffer;
				}
			};
		}

		@Override
		public String toText()
		{
			return "[" + String.join(",", strings) + "]";
		}
	}

	// Utility methods

	public final static int FORMAT_DOUBLE_PRECISION = 14;

	/**
	 * Format a double as a string for use in the textual version of UMB
	 * @param d Double to format
	 */
	public String formatDouble(double d)
	{
		return formatDouble(d, FORMAT_DOUBLE_PRECISION);
	}

	/**
	 * Format a double as a string for use in the textual version of UMB
	 * @param d Double to format
	 * @param precision Precision (nymber of significant figures)
	 */
	public String formatDouble(double d, int precision)
	{
		// Format as either decimal or scientific notation, depending on precision
		String result = String.format((Locale) null, "%." + precision + "g", d);
		// Remove trailing zeros (keep one if of form x.000...)
		result = result.replaceFirst("(\\.[0-9]*?)0+(e|$)", "$1$2");
		return result.replaceFirst("\\.(e|$)", ".0$1");
	}
}
