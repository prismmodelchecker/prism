//==============================================================================
//
//	Copyright (c) 2024-
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

import explicit.Model;

import explicit.PartiallyObservableModel;
import explicit.rewards.Rewards;
import io.github.pmctools.umbj.*;
import parser.EvaluateContext;
import parser.State;
import parser.VarList;
import parser.ast.DeclarationBool;
import parser.ast.DeclarationDoubleUnbounded;
import parser.ast.DeclarationInt;
import parser.ast.DeclarationIntUnbounded;
import parser.ast.DeclarationType;
import parser.type.Type;
import prism.Evaluator;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import prism.PrismUtils;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class to manage export of built models to the UMB file format.
 */
public class UMBExporter<Value> extends ModelExporter<Value>
{
	/**
	 * Construct a UMBExporter with default export options.
	 */
	public UMBExporter()
	{
		super();
	}

	/**
	 * Construct a UMBExporter with the specified export options.
	 */
	public UMBExporter(ModelExportOptions modelExportOptions)
	{
		super(modelExportOptions);
	}

	@Override
	public void exportModel(Model<Value> model, PrismLog out) throws PrismException
	{
		// Export to PrismLog only for text mode
		if (!modelExportOptions.getBinaryAsText()) {
			throw new PrismException("Export in UMB binary format must be to a file");
		}
		// Load all model info into a UMBWriter, then export
		UMBWriter umbWriter = createUMBWriter(model);
		try {
			StringBuffer sb = new StringBuffer();
			umbWriter.exportAsText(sb);
			out.print(sb.toString());
		} catch (UMBException e) {
			throw new PrismException(e.getMessage());
		}
	}

	@Override
	public void exportModel(Model<Value> model, File fileOut) throws PrismException
	{
		// Text mode: write to a PrismLog
		if (modelExportOptions.getBinaryAsText()) {
			try (PrismFileLog out = new PrismFileLog(fileOut.getPath())) {
				exportModel(model, out);
			}
		}
		// Otherwise export in binary mode
		// Load all model info into a UMBWriter, then export
		UMBWriter umbWriter = createUMBWriter(model);
		try {
			ModelExportOptions.CompressionFormat compressionDefault = ModelExportOptions.CompressionFormat.fromUMB(UMBFormat.DEFAULT_COMPRESSION_FORMAT);
			UMBFormat.CompressionFormat compressionFormat = modelExportOptions.getCompressionFormat(compressionDefault).toUMB();
			umbWriter.export(fileOut, modelExportOptions.getZipped(), compressionFormat);
		} catch (UMBException e) {
			throw new PrismException(e.getMessage());
		}
	}

	/**
	 * Crate a {@link UMBWriter} to export the specified model to UMB format.
	 * @param model The model
	 */
	private UMBWriter createUMBWriter(Model<Value> model) throws PrismException
	{
		// Get some model info
		setEvaluator(model.getEvaluator());
		Evaluator<Value> evalRewards = getRewardEvaluator();
		ModelType modelType = model.getModelType();
		int numStates = model.getNumStates();
		boolean showActions = modelExportOptions.getShowActions();

		// Omit action export if missing or all null
		List<Object> actions = model.getActions();
		if (actions.isEmpty() || actions.size() == 1 && actions.get(0) == null) {
			showActions = false;
		}

		// Check for currently unsupported cases
		if (modelType.uncertain() && !modelType.intervals()) {
			throw new PrismNotSupportedException(modelType + "s cannot yet be exported to UMB");
		}
		if (model.getEvaluator().isSymbolic()) {
			throw new PrismNotSupportedException("Parametric models cannot yet be exported to UMB");
		}

		// Create a ModelAccess object to access the model data in a uniform way
		ModelAccess<Value> modelAccess = ModelAccess.wrap(model);

		try {

			// Create the writer and build the index
			UMBWriter umbWriter = new UMBWriter();
			buildIndex(modelAccess, umbWriter.getUmbIndex());

			// Add core transition info
			if (modelType.nondeterministic()) {
				umbWriter.addStateChoiceOffsets(modelAccess.getStateChoiceOffsets());
				if (modelType.isProbabilistic()) {
					umbWriter.addChoiceBranchOffsets(modelAccess.getChoiceTransitionOffsets());
				}
			} else {
				umbWriter.addChoiceBranchOffsets(modelAccess.getStateTransitionOffsets());
			}
			if (model.getModelType().isProbabilistic()) {
				umbWriter.addBranchProbabilities(modelAccess.getTransitionProbabilitiesAsPrimitives());
				if (model.getModelType() == ModelType.CTMC) {
					umbWriter.addExitRates(modelAccess.getExitRatesAsPrimitives());
				}
			}
			umbWriter.addBranchTargets(modelAccess.getTransitionSuccessors());

			// Add initial states info
			umbWriter.addInitialStates(modelAccess.getInitialStates());

			// Add action labelling info
			if (showActions) {
				if (modelType.nondeterministic()) {
					if (model.getActions().size() > 1) {
						umbWriter.addChoiceActions(modelAccess.getChoiceActionIndices(), modelAccess.getActionStrings());
					} else {
						umbWriter.addSingleChoiceAction(modelAccess.getActionStrings().get(0));
					}
				} else {
					if (model.getActions().size() > 1) {
						umbWriter.addBranchActions(modelAccess.getTransitionActionIndices(), modelAccess.getActionStrings());
					} else {
						umbWriter.addSingleBranchAction(modelAccess.getActionStrings().get(0));
					}
				}
			}

			// Add observation info
			if (modelType.partiallyObservable()) {
				umbWriter.addStateObservations(modelAccess.getStateObservations());
			}

			// Add label info
			boolean showLabels = modelExportOptions.getShowLabels();
			if (showLabels) {
				int numLabels = getNumLabels();
				for (int i = 0; i < numLabels; i++) {
					umbWriter.addStateAP(getLabelName(i), getLabel(i));
				}
			}

			// Add reward info
			boolean showRewards = modelExportOptions.getShowRewards();
			if (showRewards) {
				int numRewards = getNumRewards();
				for (int r = 0; r < numRewards; r++) {
					Rewards<Value> reward = getReward(r);
					String id = umbWriter.addRewards(getRewardName(r), evalRewards.exact());
					if (reward.hasStateRewards()) {
						umbWriter.addStateRewardsByID(id, modelAccess.getStateRewardsAsPrimitives(getReward(r)));
					}
					if (reward.hasTransitionRewards()) {
						if (modelType.nondeterministic()) {
							umbWriter.addChoiceRewardsByID(id, modelAccess.getTransitionRewardsAsPrimitives(getReward(r)));
						} else {
							umbWriter.addBranchRewardsByID(id, modelAccess.getTransitionRewardsAsPrimitives(getReward(r)));
						}
					}
					// If there are no rewards, add some dummy zero state rewards
					if (!(reward.hasStateRewards() || reward.hasTransitionRewards())) {
						umbWriter.addStateRewardsByID(id, Collections.nCopies(numStates, 0.0).iterator());
					}
				}
			}

			// Add variable info
			boolean showStates = modelExportOptions.getShowStates();
			if (showStates && modelInfo != null && model.getStatesList() != null) {
				VarList varList = modelInfo.createVarList();
				storeVarInfo(varList, model.getStatesList(), UMBIndex.UMBEntity.STATES, umbWriter);
			}

			// Add observable info
			boolean showObservations = modelExportOptions.getShowObservations();
			if (showObservations && modelInfo != null && modelInfo.getModelType().partiallyObservable()) {
				// Create a VarList for the observables
				VarList obsVarList = new VarList();
				obsVarList.setEvaluateContext(modelInfo.getEvaluateContext());
				int numObservables = modelInfo.getNumObservables();
				for (int i = 0; i < numObservables; i++) {
					String obsName = modelInfo.getObservableName(i);
					Type obsType = modelInfo.getObservableType(i);
					DeclarationType obsDecl = obsType.defaultDeclarationType();
					obsVarList.addVar(obsName, obsDecl, -1);
				}
				// Store observables
				storeVarInfo(obsVarList, ((PartiallyObservableModel<Value>) model).getObservationsList(), UMBIndex.UMBEntity.OBSERVATIONS, umbWriter);
			}

			return umbWriter;

		} catch (UMBException e) {
			throw new PrismException("UMB import problem: " + e.getMessage());
		}
	}

	/**
	 * Build the index for the UMB file, storing model type and stats.
	 * @param model The model
	 * @param umbIndex The UMB index to build
	 */
	private void buildIndex(ModelAccess<Value> model, UMBIndex umbIndex)
	{
		umbIndex.fileData.tool = Prism.getToolName();
		umbIndex.fileData.toolVersion = Prism.getVersion();
		storeModelTypeInIndex(model, umbIndex);
		storeModelStatsInIndex(model, umbIndex);
	}

	/**
	 * Store info about the model type in the index for the UMB file.
	 * @param model The model
	 * @param umbIndex The UMB index to build
	 */
	private void storeModelTypeInIndex(ModelAccess<Value> model, UMBIndex umbIndex)
	{
		ModelType modelType = model.getModelType();
		umbIndex.setTime(modelType.continuousTime() ? UMBIndex.Time.STOCHASTIC : UMBIndex.Time.DISCRETE);
		umbIndex.setNumPlayers(model.getNumPlayers());
		umbIndex.setNumObservations(model.getNumObservations());
		if (model.getNumObservations() > 0) {
			umbIndex.setObservationsApplyTo(UMBIndex.UMBEntity.STATES);
		}
		boolean rational = model.getEvaluator().exact();
		if (modelType.isProbabilistic()) {
			umbIndex.setBranchProbabilityType(UMBType.contNum(rational, modelType.intervals()));
			if (!modelType.choicesSumToOne()) {
				umbIndex.setExitRateType(UMBType.contNum(rational, modelType.intervals()));
			}
		}
	}

	/**
	 * Store stats about the model in the index for the UMB file.
	 * @param model The model
	 * @param umbIndex The UMB index to build
	 */
	private void storeModelStatsInIndex(ModelAccess<Value> model, UMBIndex umbIndex)
	{
		umbIndex.setNumStates(model.getNumStates());
		umbIndex.setNumInitialStates(model.getNumInitialStates());
		umbIndex.setNumChoices(model.getNumChoices());
		umbIndex.setNumBranches(model.getNumTransitions());
		if (modelExportOptions.getShowActions()) {
			List<Object> actions = model.getActions();
			int numActions = actions.size();
			// Treat a single 'null' action as no actions
			if (numActions == 1 && actions.get(0) == null) {
				numActions = 0;
			}
			if (model.getModelType().nondeterministic()) {
				umbIndex.setNumChoiceActions(numActions);
				umbIndex.setNumBranchActions(0);
			} else {
				umbIndex.setNumChoiceActions(0);
				umbIndex.setNumBranchActions(numActions);
			}
		} else {
			umbIndex.setNumChoiceActions(0);
			umbIndex.setNumBranchActions(0);
		}
	}

	/**
	 * Extract variable/observable info from a VarList and State list and store in UMBWriter.
	 * @param varList Variable/observable info
	 * @param statesList List of states/observations
	 * @param umbWriter The UMBWriter to store info in
	 */
	private void storeVarInfo(VarList varList, List<State> statesList, UMBIndex.UMBEntity entity, UMBWriter umbWriter) throws PrismException
	{
		try {
			int numVars = varList.getNumVars();

			// Create bit-packing for variable/observable values, store metadata in index
			boolean storeOffsets = false;
			UMBBitPacking bitPacking = new UMBBitPacking();
			for (int i = 0; i < numVars; i++) {
				DeclarationType varDecl = varList.getDeclarationType(i);
				UMBType varTypeUMB;
				if (varDecl instanceof DeclarationBool) {
					varTypeUMB = UMBType.create(UMBType.Type.BOOL, 1);
				} else if (varDecl instanceof DeclarationInt) {
					if (storeOffsets) {
						varTypeUMB = UMBType.create(UMBType.Type.INT, varList.getRangeLogTwo(i)); // TODO
					} else {
						int varLow = varList.getLow(i);
						int varHigh = varList.getHigh(i);
						if (varLow < 0) {
							int varMaxAbs = Math.abs(varLow);
							if (varHigh > 0) {
								varMaxAbs = Math.max(varMaxAbs, varHigh + 1);
							}
							varTypeUMB = UMBType.create(UMBType.Type.INT, (int) Math.ceil(PrismUtils.log2(varMaxAbs)) + 1);
						} else {
							varTypeUMB = UMBType.create(UMBType.Type.UINT, (int) Math.ceil(PrismUtils.log2(varHigh + 1)));
						}
					}
				} else if (varDecl instanceof DeclarationIntUnbounded) {
					varTypeUMB = UMBType.create(UMBType.Type.INT, 32);
				} else if (varDecl instanceof DeclarationDoubleUnbounded) {
					varTypeUMB = UMBType.create(UMBType.Type.DOUBLE, 64);
				} else {
					throw new PrismException("Unsupported variable type in UMB export: " + varDecl);
				}
				bitPacking.addVariable(varList.getName(i), varTypeUMB);
			}
			bitPacking.padToByteBoundary();
			umbWriter.addValuationDescription(entity, true, bitPacking);

			// Build an iterator to supply the bit-packed variable/observable values, add data
			Iterator<UMBBitString> iter = statesList.stream()
					.map(s -> {
						UMBBitString bitString = bitPacking.newBitString();
						Object v = null;
						try {
							for (int i = 0; i < numVars; i++) {
								UMBType.Type varTypeUMB = bitPacking.getVariable(i).getType().type;
								v = s.varValues[i];
								v = varList.getType(i).castValueTo(v, EvaluateContext.EvalMode.FP);
								switch (varTypeUMB) {
									case BOOL:
										bitPacking.setBooleanVariableValue(bitString, i, (boolean) v);
										break;
									case INT:
										bitPacking.setIntVariableValue(bitString, i, (int) v);
										break;
									case UINT:
										bitPacking.setUIntVariableValue(bitString, i, (int) v);
										break;
									case DOUBLE:
										bitPacking.setDoubleVariableValue(bitString, i, (double) v);
										break;
									default:
										throw new PrismException("Unsupported variable type in UMB export: " + varTypeUMB);
								}
							}
						} catch (ClassCastException e) {
							throw new RuntimeException("Was not expecting data as " + v.getClass().getSimpleName());
						} catch (UMBException | PrismException e) {
							throw new RuntimeException(e);
						}
						return bitString;
					})
					.iterator();
			umbWriter.addValuations(entity, iter, bitPacking);
	} catch (UMBException | RuntimeException e) {
			throw new PrismException("UMB export problem: " + e.getMessage());
		}
	}
}
