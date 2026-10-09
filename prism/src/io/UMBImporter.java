//==============================================================================
//
//	Copyright (c) 2025-
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

import common.Interval;
import common.SafeCast;
import io.github.pmctools.umbj.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import parser.EvaluateContext;
import parser.VarList;
import parser.ast.DeclarationBool;
import parser.ast.DeclarationDoubleUnbounded;
import parser.ast.DeclarationInt;
import parser.ast.DeclarationType;
import parser.ast.Expression;
import prism.BasicModelInfo;
import prism.BasicRewardInfo;
import prism.Evaluator;
import prism.ModelInfo;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import prism.RewardInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.IntStream;

/**
 * Class to manage importing models from UMB binary files.
 */
public class UMBImporter extends ExplicitModelImporter
{
	private File umbFile;
	private UMBReader umbReader;
	private UMBIndex umbIndex;

	// Model info extracted from file and then stored in a BasicModelInfo object
	private BasicModelInfo basicModelInfo;

	// Links between model info to UMB file
	private List<String> labelIDs;
	private List<String> rewardIDs;
	private List<String> varIDs;

	// Num states/transitions/etc.
	private int numStates = 0;
	private int numChoices = 0;
	private int numTransitions = 0;
	private int numObservations = 0;

	// Reward info extracted from files and then stored in a BasicRewardInfo object
	private BasicRewardInfo basicRewardInfo;

	public UMBImporter(File umbFile) throws PrismException
	{
		this.umbFile = umbFile;

		try {
			umbReader = new UMBReader(umbFile);
			// Extract index and store model stats
			umbIndex = umbReader.getUMBIndex();
			numStates = SafeCast.toIntExact(umbIndex.getNumStates());
			numChoices = SafeCast.toIntExact(umbIndex.getNumChoices());
			numTransitions = SafeCast.toIntExact(umbIndex.getNumBranches());
			numObservations = SafeCast.toIntExact(umbIndex.getNumObservations());
		} catch (ArithmeticException e) {
			throw new PrismException("UMB model is too large to be imported");
		} catch (UMBException e) {
			throw new PrismException("Error importing from UMB: " + e.getMessage());
		}
	}

	@Override
	public boolean modelIsExact()
	{
		// For now, assume that if transition probabilities are rationals, so is everything else
		return umbIndex.getBranchProbabilityType() != null && umbIndex.getBranchProbabilityType().type.isRational();
	}

	@Override
	public boolean providesStates()
	{
		return umbIndex.hasValuations(UMBIndex.UMBEntity.STATES);
	}

	@Override
	public boolean providesObservations()
	{
		return umbIndex.hasValuations(UMBIndex.UMBEntity.OBSERVATIONS);
	}

	@Override
	public boolean providesLabels()
	{
		return umbIndex.hasAPAnnotations();
	}

	@Override
	public String sourceString()
	{
		return "\"" + umbFile.getName() + "\"";
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
		return numStates;
	}

	@Override
	public int getNumChoices() throws PrismException
	{
		return numChoices;
	}

	@Override
	public int getNumTransitions() throws PrismException
	{
		return numTransitions;
	}

	@Override
	public int getNumObservations() throws PrismException
	{
		return numObservations;
	}

	@Override
	public BitSet getDeadlockStates() throws PrismException
	{
		// TODO
		return new BitSet();
	}

	@Override
	public int getNumDeadlockStates() throws PrismException
	{
		// TODO
		return 0;
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
	 * Build/store model info from the UMB index file.
	 * Can then be accessed via {@link #getModelInfo()}.
	 */
	private void buildModelInfo() throws PrismException
	{
		// Create BasicModelInfo object
		ModelType modelType = getModelTypeFromIndex(umbIndex);
		basicModelInfo = new BasicModelInfo(modelType);
		// Extract/initialise action list
		ArrayList<Object> actionStrings = new ArrayList<>();
		try {
			if (modelType.nondeterministic()) {
				if (umbReader.hasChoiceActionStrings()) {
					umbReader.extractChoiceActionStrings(actionStrings::add);
				} else {
					// No strings provided: use default action strings _1, _2, ...
					IntStream.rangeClosed(1, umbIndex.getNumChoiceActions()).mapToObj(i -> "_" + i).forEach(actionStrings::add);
				}
			} else {
				if (umbReader.hasBranchActionStrings()) {
					umbReader.extractBranchActionStrings(actionStrings::add);
				} else {
					// No strings provided: use default action strings _1, _2, ...
					IntStream.rangeClosed(1, umbIndex.getNumBranchActions()).mapToObj(i -> "_" + i).forEach(actionStrings::add);
				}
			}
			if (actionStrings.isEmpty()) {
				actionStrings.add(null);
			}
		} catch (UMBException e) {
			throw new PrismException("Could not extract actions from UMB file");
		}
		basicModelInfo.setActionList(actionStrings);
		// Add variable info, extracting from UMB file if provided
		VarList varList = basicModelInfo.getVarList();
		if (providesStates()) {
			buildVarInfo(UMBIndex.UMBEntity.STATES, varList);
		} else {
			varList.addVar(defaultVariableName(), defaultVariableDeclarationType(), -1);
		}
		// Add observable info, extracting from UMB file if provided
		if (providesObservations()) {
			VarList obsVarList = new VarList();
			obsVarList.setEvaluateContext(basicModelInfo.getEvaluateContext());
			buildVarInfo(UMBIndex.UMBEntity.OBSERVATIONS, obsVarList);
			for (int i = 0; i < obsVarList.getNumVars(); i++) {
				basicModelInfo.getObservableNames().add(obsVarList.getName(i));
				basicModelInfo.getObservableTypeList().add(obsVarList.getType(i));
			}
		} else {
			basicModelInfo.getObservableNames().add(defaultObservableName());
			basicModelInfo.getObservableTypeList().add(defaultObservableType());
		}
		// Add label info
		// We extract all labels (AP) annotations from the UMB file, ignoring "deadlock" if present
		// IDs are stores in labelIDs and (valid) names go in basicModelInfo
		labelIDs = new ArrayList<>();
		List<String> labelList = basicModelInfo.getLabelNameList();
		for (UMBIndex.Annotation apAnnotation : umbIndex.getAPAnnotationsList()) {
			String apName = apAnnotation.getName();
			if (!apName.equals("deadlock")) {
				labelIDs.add(apAnnotation.id);
				// Get valid, unique label name (usually just the AP annotation alias)
				String labelName = Prism.toIdentifier(apName);
				while (labelList.contains(labelName)) {
					labelName = "_" + labelName;
				}
				labelList.add(labelName);
			}
		}
	}

	/**
	 * Determine ModelType from UMBIndex metadata.
	 */
	private static ModelType getModelTypeFromIndex(UMBIndex umbIndex) throws PrismException
	{
		UMBIndex.ModelType modelType = umbIndex.getModelType();
		if (modelType == null) {
			throw new PrismException("Unsupported model type in UMB file");
		}
		switch (modelType) {
			case DTMC:
				return ModelType.DTMC;
			case CTMC:
				return ModelType.CTMC;
			case MDP:
				return ModelType.MDP;
			case POMDP:
				return ModelType.POMDP;
			case LTS:
				return ModelType.LTS;
			case IDTMC:
				return ModelType.IDTMC;
			case IMDP:
				return ModelType.IMDP;
			case IPOMDP:
				return ModelType.IPOMDP;
			default:
				throw new PrismException("Unsupported model type " + modelType + " in UMB file");
		}
	}

	/**
	 * Build/store reward info from the UMB index file.
	 * Can then be accessed via {@link #getRewardInfo()}.
	 */
	private void buildRewardInfo() throws PrismException
	{
		ModelType modelType = getModelInfo().getModelType();
		basicRewardInfo = new BasicRewardInfo();
		int numRewards = umbIndex.getNumRewardAnnotations();
		for (int r = 0; r < numRewards; r++) {
			basicRewardInfo.addReward(umbIndex.getRewardAnnotation(r).getName());
			basicRewardInfo.setHasStateRewards(r, umbIndex.hasStateRewards(r));
			if (modelType.nondeterministic()) {
				basicRewardInfo.setHasTransitionRewards(r, umbIndex.hasChoiceRewards(r));
			} else {
				basicRewardInfo.setHasTransitionRewards(r, umbIndex.hasBranchRewards(r));
			}
		}
	}

	/**
	 * Extract info about variables/observables for states/observations and store in the provided VarList.
	 * @param entity State or observations?
	 * @param varList Storage for variable/observable info
	 */
	private void buildVarInfo(UMBIndex.UMBEntity entity, VarList varList) throws PrismException
	{
		try {
			if (!umbIndex.hasValuations(entity)) {
				throw new PrismException("Missing UMB valuation data for " + entity);
			} else {
				if (umbIndex.getNumValuationClasses(entity) > 1) {
					throw new PrismException("Import of multiple valuation classes not yet supported");
				}
			}
			UMBBitPacking bitPacking = umbIndex.getValuationBitPacking(entity);
			int numVars = bitPacking.getNumVariables();
			// Compute ranges of all integer variables (in a single pass)
			UMBReader.LongRange[] varLongRanges = umbReader.getValuationLongRanges(entity, bitPacking);
			for (int i = 0; i < numVars; i++) {
				UMBBitPacking.BitPackedVariable var = bitPacking.getVariable(i);
				// Get valid, unique variable name (usually just the provided variable name)
				String varName = Prism.toIdentifier(var.name);
				while (varList.getIndex(varName) != -1) {
					varName = "_" + varName;
				}
				// Determine type, range, etc. of variable
				DeclarationType varDecl = null;
				switch (var.getType().type) {
					case BOOL:
						varDecl = new DeclarationBool();
						break;
					case INT:
					case UINT:
						boolean computeRange = true;
						int varIntMin;
						int varIntMax;
						if (computeRange) {
							UMBReader.LongRange varLongRange = varLongRanges[i];
							long varLongMin = varLongRange.getMin();
							long varLongMax = varLongRange.getMax();
							try {
								varIntMin = SafeCast.toIntExact(varLongMin);
								varIntMax = SafeCast.toIntExact(varLongMax);
							} catch (ArithmeticException e) {
								throw new PrismException("UMB variable " + var.name + " exceeds the range of an int");
							}
						} else {
							// Default to min/max values for (u)ints
							if (var.getType().type == UMBType.Type.INT) {
								if (bitPacking.getVariableSize(i) > 32) {
									throw new PrismException("UMB variable " + var.name + " exceeds the range of an int");
								}
								varIntMin = -(1 << (bitPacking.getVariableSize(i) - 1));
								varIntMax = (1 << (bitPacking.getVariableSize(i) - 1)) -1;
							} else {
								if (bitPacking.getVariableSize(i) >= 32) {
									throw new PrismException("UMB variable " + var.name + " exceeds the range of an int");
								}
								varIntMin = 0;
								varIntMax = (1 << bitPacking.getVariableSize(i)) - 1;
							}
						}
						// Note: we do not yet allow 0-range variables
						if (varIntMin == varIntMax) {
							varIntMax++;
						}
						varDecl = new DeclarationInt(Expression.Int(varIntMin), Expression.Int(varIntMax));
						break;
					case DOUBLE:
						varDecl = new DeclarationDoubleUnbounded();
						break;
					default:
						throw new PrismException("Unknown variable type in UMB index: " + var.getType().type);
				}
				varList.addVar(varName, varDecl, -1);
			}
		} catch (UMBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void extractStates(IOUtils.StateDefnConsumer storeStateDefn) throws PrismException
	{
		// If there is no info, just assume that states comprise a single integer value
		if (!providesStates()) {
			super.extractStates(storeStateDefn);
			return;
		}
		// Otherwise, extract state variable info
		extractValuationDefinitions(UMBIndex.UMBEntity.STATES, storeStateDefn);
	}

	@Override
	public void extractObservationDefinitions(IOUtils.StateDefnConsumer storeObservationDefn) throws PrismException
	{
		// If there is no info, just assume that states comprise a single integer value
		if (!providesObservations()) {
			super.extractObservationDefinitions(storeObservationDefn);
			return;
		}
		// Otherwise, extract observation observable info
		extractValuationDefinitions(UMBIndex.UMBEntity.OBSERVATIONS, storeObservationDefn);
	}

	/**
	 * Extract the values of all variables from the valuations for an entity (states or observations).
	 * @param entity States or observations?
	 * @param storeDefn Consumer for (entity index, variable index, value) triples
	 */
	private void extractValuationDefinitions(UMBIndex.UMBEntity entity, IOUtils.StateDefnConsumer storeDefn) throws PrismException
	{
		try {
			// Look up variable info once, rather than for each valuation
			UMBBitPacking bitPacking = umbIndex.getValuationBitPacking(entity);
			int numVars = bitPacking.getNumVariables();
			UMBType.Type[] types = new UMBType.Type[numVars];
			int[] offsets = new int[numVars];
			int[] sizes = new int[numVars];
			for (int i = 0; i < numVars; i++) {
				types[i] = bitPacking.getVariable(i).getType().type;
				offsets[i] = bitPacking.getVariableOffset(i);
				sizes[i] = bitPacking.getVariableSize(i);
			}
			int[] index = new int[1];
			umbReader.extractValuations(entity, bitString -> {
				try {
					int s = index[0]++;
					for (int i = 0; i < numVars; i++) {
						storeDefn.accept(s, i, getBitStringValue(bitString, types[i], offsets[i], sizes[i], bitPacking, i));
					}
				} catch (UMBException | PrismException e) {
					throw new RuntimeException(e.getMessage());
				}
			});
		} catch (UMBException | RuntimeException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	/**
	 * Get the value of a variable, of the given type, offset and size, from a bit string, as an Object.
	 * The bit packing and variable index are only used for error messages.
	 */
	private static Object getBitStringValue(UMBBitString bitString, UMBType.Type type, int offset, int size, UMBBitPacking bitPacking, int i) throws UMBException
	{
		switch (type) {
			case BOOL:
				return bitString.getBoolean(offset, size);
			case INT:
				try {
					return SafeCast.toIntExact(bitString.getLong(offset, size));
				} catch (ArithmeticException e) {
					throw new UMBException("UMB variable " + bitPacking.getVariable(i).name + " exceeds the range of an int");
				}
			case UINT:
				try {
					return SafeCast.toIntExact(bitString.getULong(offset, size));
				} catch (ArithmeticException e) {
					throw new UMBException("UMB variable " + bitPacking.getVariable(i).name + " exceeds the range of an int");
				}
			case DOUBLE:
				return bitString.getDouble(offset, size);
			default:
				throw new UMBException("Unknown UMB variable type: " + type);
		}
	}

	@Override
	public int computeMaxNumChoices() throws PrismException
	{
		try {
			return SafeCast.toInt(umbReader.extractMaxStateChoiceCount());
		} catch (UMBException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public <Value> void extractMCTransitions(IOUtils.MCTransitionConsumer<Value> storeTransition, Evaluator<Value> eval) throws PrismException
	{
		try {
			// Extract transition info
			IntList choiceTransitionOffsets = new IntArrayList(numChoices + 1);
			umbReader.extractChoiceBranchOffsets(l -> choiceTransitionOffsets.add((int) l));
			IntList transitionSuccessors = new IntArrayList(numTransitions);
			umbReader.extractBranchTargets(l -> transitionSuccessors.add((int) l));
			ModelAccess.ValueListFromPrimitives<Value> valueListFromPrimitives = new ModelAccess.ValueListFromPrimitives<>(eval, numTransitions);
			List<Value> transitionProbabilities = valueListFromPrimitives.valueList;
			umbReader.extractBranchProbabilities(valueListFromPrimitives.primitiveConsumer);

			// For CTMCs, extract exit rates
			List<Value> exitRates = null;
			boolean ctmc = getModelInfo().getModelType() == ModelType.CTMC;
			if (ctmc) {
				valueListFromPrimitives = new ModelAccess.ValueListFromPrimitives<>(eval, numStates);
				exitRates = valueListFromPrimitives.valueList;
				umbReader.extractExitRates(valueListFromPrimitives.primitiveConsumer);
			}

			// Extract action info
			Object firstAction = getModelInfo().getActions().get(0);
			IntList transitionActionIndices = null;
			boolean hasActions = umbReader.hasBranchActionIndices();
			if (hasActions) {
				transitionActionIndices = new IntArrayList(numTransitions);
				umbReader.extractBranchActionIndices(transitionActionIndices::add);
			}

			// Convert sparse storage to transitions and store
			int jLo = 0, jHi = 0;
			for (int s = 0; s < numStates; s++) {
				jLo = jHi;
				jHi = choiceTransitionOffsets.getInt(s + 1);
				for (int j = jLo; j < jHi; j++) {
					Object action = hasActions ? getModelInfo().getActions().get(transitionActionIndices.getInt(j)) : firstAction;
					if (getModelInfo().getModelType() == ModelType.IDTMC) {
						Interval<Double> dIntv = new Interval<>((Double) transitionProbabilities.get(2 * j), (Double) transitionProbabilities.get(2 * j + 1));
						((IOUtils.MCTransitionConsumer<Interval<Double>>) storeTransition).accept(s, transitionSuccessors.getInt(j), dIntv, action);
					} else {
						Value v = transitionProbabilities.get(j);
						if (ctmc) {
							v = eval.multiply(v, exitRates.get(s));
						}
						storeTransition.accept(s, transitionSuccessors.getInt(j), v, action);
					}
				}
			}

			// Transitions now extracted, so the UMB reader no longer needs to hold them
			umbReader.releaseTransitionData();
		} catch (UMBException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public <Value> void extractMDPTransitions(IOUtils.MDPTransitionConsumer<Value> storeTransition, Evaluator<Value> eval) throws PrismException
	{
		try {
			// Extract transition info
			IntList stateChoiceOffsets = new IntArrayList(numStates + 1);
			umbReader.extractStateChoiceOffsets(l -> stateChoiceOffsets.add((int) l));
			IntList choiceTransitionOffsets = new IntArrayList(numChoices + 1);
			umbReader.extractChoiceBranchOffsets(l -> choiceTransitionOffsets.add((int) l));
			IntList transitionSuccessors = new IntArrayList(numTransitions);
			umbReader.extractBranchTargets(l -> transitionSuccessors.add((int) l));
			ModelAccess.ValueListFromPrimitives<Value> valueListFromPrimitives = new ModelAccess.ValueListFromPrimitives<>(eval, numTransitions);
			List<Value> transitionProbabilities = valueListFromPrimitives.valueList;
			umbReader.extractBranchProbabilities(valueListFromPrimitives.primitiveConsumer);

			// Extract action info
			Object firstAction = getModelInfo().getActions().get(0);
			IntList choiceActionIndices = null;
			boolean hasActions = umbReader.hasChoiceActionIndices();
			if (hasActions) {
				choiceActionIndices = new IntArrayList(numChoices);
				umbReader.extractChoiceActionIndices(choiceActionIndices::add);
			}

			// Convert sparse storage to transitions and store
			int iLo = 0, iHi = 0;
			int jLo = 0, jHi = 0;
			for (int s = 0; s < numStates; s++) {
				iLo = iHi;
				iHi = stateChoiceOffsets.getInt(s + 1);
				int iCount = 0;
				for (int i = iLo; i < iHi; i++) {
					jLo = jHi;
					jHi = choiceTransitionOffsets.getInt(i + 1);
					for (int j = jLo; j < jHi; j++) {
						Object action = hasActions ? getModelInfo().getActions().get(choiceActionIndices.getInt(i)) : firstAction;
						if (getModelInfo().getModelType().intervals()) {
							Interval<Value> vIntv = new Interval<>(transitionProbabilities.get(2 * j), transitionProbabilities.get(2 * j + 1));
							((IOUtils.MDPTransitionConsumer<Interval<Value>>) storeTransition).accept(s, iCount, transitionSuccessors.getInt(j), vIntv, action);
						} else {
							Value v = transitionProbabilities.get(j);
							storeTransition.accept(s, iCount, transitionSuccessors.getInt(j), v, action);
						}

					}
					iCount++;
				}
			}

			// Transitions now extracted, so the UMB reader no longer needs to hold them
			umbReader.releaseTransitionData();
		} catch (UMBException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public IOUtils.SparseMCTransitions extractMCTransitionsSparse() throws PrismException
	{
		// We can extract directly from the UMB file for (double-valued) DTMCs
		// (assuming, as for extractMCTransitions, one choice per state)
		UMBType probType = umbIndex.getBranchProbabilityType();
		boolean direct = getModelInfo().getModelType() == ModelType.DTMC && numChoices == numStates
				&& probType != null && probType.type.isDouble() && !probType.type.isInterval();
		if (!direct) {
			return super.extractMCTransitionsSparse();
		}
		try {
			IOUtils.SparseMCTransitions trans = new IOUtils.SparseMCTransitions();
			trans.rowStarts = extractIntArray(numChoices + 1, umbReader::extractChoiceBranchOffsets);
			trans.successors = extractIntArray(numTransitions, umbReader::extractBranchTargets);
			trans.probabilities = extractDoubleArray(numTransitions, umbReader::extractBranchProbabilities);
			trans.actions = extractActions(numTransitions, umbReader.hasBranchActionIndices() ? umbReader::extractBranchActionIndices : null);
			// Transitions now extracted, so the UMB reader no longer needs to hold them
			umbReader.releaseTransitionData();
			return trans;
		} catch (UMBException | RuntimeException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public IOUtils.SparseMDPTransitions extractMDPTransitionsSparse() throws PrismException
	{
		// We can extract directly from the UMB file for (double-valued) MDPs
		UMBType probType = umbIndex.getBranchProbabilityType();
		boolean direct = getModelInfo().getModelType() == ModelType.MDP
				&& probType != null && probType.type.isDouble() && !probType.type.isInterval();
		if (!direct) {
			return super.extractMDPTransitionsSparse();
		}
		try {
			IOUtils.SparseMDPTransitions trans = new IOUtils.SparseMDPTransitions();
			trans.rowStarts = extractIntArray(numStates + 1, umbReader::extractStateChoiceOffsets);
			trans.choiceStarts = extractIntArray(numChoices + 1, umbReader::extractChoiceBranchOffsets);
			trans.successors = extractIntArray(numTransitions, umbReader::extractBranchTargets);
			trans.probabilities = extractDoubleArray(numTransitions, umbReader::extractBranchProbabilities);
			trans.actions = extractActions(numChoices, umbReader.hasChoiceActionIndices() ? umbReader::extractChoiceActionIndices : null);
			// Transitions now extracted, so the UMB reader no longer needs to hold them
			umbReader.releaseTransitionData();
			return trans;
		} catch (UMBException | RuntimeException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public void extractLTSTransitions(IOUtils.LTSTransitionConsumer storeTransition) throws PrismException
	{
		try {
			// Extract transition info
			IntList stateChoiceOffsets = new IntArrayList(numStates + 1);
			umbReader.extractStateChoiceOffsets(l -> stateChoiceOffsets.add((int) l));
			IntList transitionSuccessors = new IntArrayList(numTransitions);
			umbReader.extractBranchTargets(l -> transitionSuccessors.add((int) l));

			// Extract action info
			Object firstAction = getModelInfo().getActions().get(0);
			IntList choiceActionIndices = null;
			boolean hasActions = umbReader.hasChoiceActionIndices();
			if (hasActions) {
				choiceActionIndices = new IntArrayList(numChoices);
				umbReader.extractChoiceActionIndices(choiceActionIndices::add);
			}

			// Convert sparse storage to transitions and store
			int iLo = 0, iHi = 0;
			for (int s = 0; s < numStates; s++) {
				iLo = iHi;
				iHi = stateChoiceOffsets.getInt(s + 1);
				int iCount = 0;
				for (int i = iLo; i < iHi; i++) {
					Object action = hasActions ? getModelInfo().getActions().get(choiceActionIndices.getInt(i)) : firstAction;
					storeTransition.accept(s, iCount, transitionSuccessors.getInt(i), action);
					iCount++;
				}
			}

			// Transitions now extracted, so the UMB reader no longer needs to hold them
			umbReader.releaseTransitionData();
		} catch (UMBException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public void extractLabelsAndInitialStates(BiConsumer<Integer, Integer> storeLabel, Consumer<Integer> storeInit, Consumer<Integer> storeDeadlock) throws PrismException
	{
		try {
			// Extract initial states
			umbReader.extractInitialStates(s -> storeInit.accept(SafeCast.toIntExact(s)));
			// Extract labels
			int numLabels = labelIDs.size();
			for (int i = 0; i < numLabels; i++) {
				int finalI = i;
				umbReader.extractStateAP(labelIDs.get(i), s -> storeLabel.accept(SafeCast.toIntExact(s), finalI));
			}
			// If a "deadlock" AP is stored, use it to store deadlock state info
			if (storeDeadlock != null) {
				String deadlockId = null;
				if (umbIndex.hasAPAnnotationWithAlias("deadlock")) {
					deadlockId = umbIndex.getAPAnnotationByAlias("deadlock").id;
				} else if (umbIndex.getAPAnnotations().get("deadlock") != null) {
					deadlockId = "deadlock";
				}
				if (deadlockId != null) {
					umbReader.extractStateAP(deadlockId, s -> storeDeadlock.accept(SafeCast.toIntExact(s)));
				}
			}
		} catch (UMBException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public void extractObservations(IOUtils.StateIntConsumer storeObservation) throws PrismException
	{
		try {
			// Extract observations from UMB
			IntList stateObservations = new IntArrayList(numStates);
			umbReader.extractStateObservations(l -> stateObservations.add((int) l));
			// Store observations in model
			for (int s = 0; s < numStates; s++) {
				storeObservation.accept(s, stateObservations.getInt(s));
			}
		} catch (UMBException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public <Value> void extractStateRewards(int rewardIndex, BiConsumer<Integer, Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		try {
			if (!basicRewardInfo.rewardStructHasStateRewards(rewardIndex)) {
				return;
			}
			umbReader.extractStateRewards(rewardIndex, ModelAccess.primitivesToValues(eval, new IndexedConsumer<>(storeReward)));
		} catch (UMBException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public <Value> void extractMCTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (!basicRewardInfo.rewardStructHasTransitionRewards(rewardIndex)) {
			return;
		}

		try {
			// Extract transition rewards from UMB
			ModelAccess.ValueListFromPrimitives<Value> valueListFromPrimitives = new ModelAccess.ValueListFromPrimitives<>(eval, numTransitions);
			List<Value> transRewards = valueListFromPrimitives.valueList;
			umbReader.extractBranchRewards(rewardIndex, valueListFromPrimitives.primitiveConsumer);

			// Convert sparse storage to reward list and store
			// If the model has already been built and provided, use this to look for transition indexing
			if (modelLookup != null) {
				ModelAccess<Value> modelAccess = ModelAccess.wrap(modelLookup);
				int iLo = 0, iHi = 0;
				for (int s = 0; s < numStates; s++) {
					iLo = iHi;
					iHi = iLo + modelAccess.getNumTransitions(s, 0);
					for (int i = iLo; i < iHi; i++) {
						Value v = transRewards.get(i);
						if (eval.gt(v, eval.zero())) {
							switch (transitionRewardIndexing) {
								case STATE:
									storeReward.accept(s, modelAccess.getTransitionSuccessor(s, 0, i - iLo), v);
									break;
								case OFFSET:
									storeReward.accept(s, i - iLo, v);
									break;
								default:
									throw new PrismException("Unknown transition reward indexing " + transitionRewardIndexing);
							}
						}
					}
				}
			}
			// If there is no model available, we extract transition info from UMB first
			else {
				IntList stateTransitionOffsets = new IntArrayList(numStates + 1);
				umbReader.extractChoiceBranchOffsets(l -> stateTransitionOffsets.add((int) l));
				IntList transitionSuccessors;
				if (transitionRewardIndexing == TransitionRewardIndexing.STATE) {
					transitionSuccessors = new IntArrayList(numTransitions);
					umbReader.extractBranchTargets(l -> transitionSuccessors.add((int) l));
				} else {
					transitionSuccessors = null;
				}

				int iLo = 0, iHi = 0;
				for (int s = 0; s < numStates; s++) {
					iLo = iHi;
					iHi = stateTransitionOffsets.getInt(s + 1);
					for (int i = iLo; i < iHi; i++) {
						Value v = transRewards.get(i);
						if (eval.gt(v, eval.zero())) {
							switch (transitionRewardIndexing) {
								case STATE:
									storeReward.accept(s, transitionSuccessors.getInt(i), v);
									break;
								case OFFSET:
									storeReward.accept(s, i - iLo, v);
									break;
								default:
									throw new PrismException("Unknown transition reward indexing " + transitionRewardIndexing);
							}
						}
					}
				}
			}
		} catch (UMBException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public <Value> void extractMDPTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException
	{
		if (!basicRewardInfo.rewardStructHasTransitionRewards(rewardIndex)) {
			return;
		}

		try {
			// Extract transition rewards from UMB
			ModelAccess.ValueListFromPrimitives<Value> valueListFromPrimitives = new ModelAccess.ValueListFromPrimitives<>(eval, numChoices);
			List<Value> transRewards = valueListFromPrimitives.valueList;
			umbReader.extractChoiceRewards(rewardIndex, valueListFromPrimitives.primitiveConsumer);

			// Convert sparse storage to reward list and store
			// If the model has already been built and provided, use this to look for transition indexing
			ModelAccess<Value> modelAccess = null;
			IntList stateChoiceOffsets;
			if (modelLookup != null) {
				stateChoiceOffsets = null;
				modelAccess = ModelAccess.wrap(modelLookup);
			}
			// If there is no model available, we extract transition info from UMB first
			else {
				stateChoiceOffsets = new IntArrayList(numStates + 1);
				umbReader.extractStateChoiceOffsets(l -> stateChoiceOffsets.add((int) l));
			}

			// Convert sparse storage to reward list and store
			int iLo = 0, iHi = 0;
			for (int s = 0; s < numStates; s++) {
				iLo = iHi;
				if (modelLookup != null) {
					iHi = iLo + modelAccess.getNumChoices(s);
				} else {
					iHi = stateChoiceOffsets.getInt(s + 1);
				}
				for (int i = iLo; i < iHi; i++) {
					Value v = transRewards.get(i);
					if (eval.gt(v, eval.zero())) {
						storeReward.accept(s, i - iLo, v);
					}
				}
			}
		} catch (UMBException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	@Override
	public void importDone()
	{
		umbReader.releaseData();
	}

	// Utility methods

	/**
	 * Something that extracts (long) values from a UMB file and passes them to a consumer.
	 */
	@FunctionalInterface
	private interface UMBLongExtractor
	{
		void extract(LongConsumer longConsumer) throws UMBException;
	}

	/**
	 * Something that extracts (int) values from a UMB file and passes them to a consumer.
	 */
	@FunctionalInterface
	private interface UMBIntExtractor
	{
		void extract(IntConsumer intConsumer) throws UMBException;
	}

	/**
	 * Something that extracts (double) values from a UMB file and passes them to a consumer.
	 */
	@FunctionalInterface
	private interface UMBDoubleExtractor
	{
		void extract(it.unimi.dsi.fastutil.doubles.DoubleConsumer doubleConsumer) throws UMBException;
	}

	/**
	 * Extract (long) values from a UMB file into a new int array of the specified size.
	 */
	private static int[] extractIntArray(int size, UMBLongExtractor extractor) throws UMBException
	{
		int[] array = new int[size];
		int[] count = new int[1];
		extractor.extract(l -> array[count[0]++] = SafeCast.toIntExact(l));
		if (count[0] != size) {
			throw new UMBException("Expected " + size + " values but found " + count[0]);
		}
		return array;
	}

	/**
	 * Extract (double) values from a UMB file into a new double array of the specified size.
	 */
	private static double[] extractDoubleArray(int size, UMBDoubleExtractor extractor) throws UMBException
	{
		double[] array = new double[size];
		int[] count = new int[1];
		extractor.extract(d -> array[count[0]++] = d);
		if (count[0] != size) {
			throw new UMBException("Expected " + size + " values but found " + count[0]);
		}
		return array;
	}

	/**
	 * Extract action indices from a UMB file and convert them to a new array of action objects,
	 * or, if {@code extractor} is null, create an array storing the first (default) action.
	 */
	private Object[] extractActions(int size, UMBIntExtractor extractor) throws UMBException, PrismException
	{
		List<Object> actionList = getModelInfo().getActions();
		Object[] actions = new Object[size];
		if (extractor == null) {
			Arrays.fill(actions, actionList.get(0));
		} else {
			int[] count = new int[1];
			extractor.extract(i -> actions[count[0]++] = actionList.get(i));
			if (count[0] != size) {
				throw new UMBException("Expected " + size + " actions but found " + count[0]);
			}
		}
		return actions;
	}

	// Utility classes

	/**
	 * Class to map a long consumer to an int consumer.
	 */
	private static class LongConsumerToIntConsumer implements LongConsumer
	{
		IntConsumer out;

		LongConsumerToIntConsumer(IntConsumer out)
		{
			this.out = out;
		}

		@Override
		public void accept(long v)
		{
			out.accept(SafeCast.toIntExact(v));
		}
	}

	/**
	 * Class to add an increasing index to values from a Value consumer.
	 */
	private static class IndexedConsumer<Value> implements Consumer<Value>
	{
		BiConsumer<Integer, Value> out;
		int index;

		IndexedConsumer(BiConsumer<Integer, Value> out)
		{
			this.out = out;
		}

		@Override
		public void accept(Value v)
		{
			out.accept(index++, v);
		}
	}

	/**
	 * Class to add an increasing index to values from a double consumer.
	 */
	private static class IndexedDoubleConsumer implements DoubleConsumer
	{
		BiConsumer<Integer, Double> out;
		int index;

		IndexedDoubleConsumer(BiConsumer<Integer, Double> out)
		{
			this.out = out;
		}

		@Override
		public void accept(double d)
		{
			out.accept(index++, d);
		}
	}
}
