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

import io.github.pmctools.umbj.UMBType.Type;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Representation of the (JSON) index for a UMB file.
 */
public class UMBIndex
{
	// Index data, stored in a form that allows JSON import/export via GSON

	/** Major version of the UMB format used for this file */
	public Integer formatVersion = UMBVersion.MAJOR;
	/** Minor version of the UMB format used for this file */
	public Integer formatRevision = UMBVersion.MINOR;
	/** Model metadata */
	public ModelData modelData = new ModelData();
	/** File metadata */
	public FileData fileData = new FileData();
	/** Transition system details */
	public TransitionSystem transitionSystem = new TransitionSystem();
	/** Annotations, arranged by group and then by annotation ID, in ordered maps */
	public LinkedHashMap<String, LinkedHashMap<String, Annotation>> annotations = new LinkedHashMap<>();
	/** Valuation descriptions, arranged by entity type */
	public LinkedHashMap<UMBEntity, ValuationDescription> valuations = new LinkedHashMap<>();

	// Further info about annotations

	/** Names of groups into which annotations are organised */
	public transient List<String> annotationGroups = new ArrayList<>();
	/** Map from annotation aliases to IDs for each annotation group */
	public transient Map<String, Map<String, String>> annotationAliasMaps = new LinkedHashMap<>();

	// Fake annotation objects for some data

	/** Annotation object for actions (stored in a similar way to annotations) */
	public transient Annotation actionsAnnotation = createStandaloneAnnotation(UMBFormat.ACTIONS_FOLDER, UMBType.create(Type.STRING));
	/** Annotation object for observations (stored in a similar way to annotations) */
	public transient Annotation observationsAnnotation = createStandaloneAnnotation(UMBFormat.OBSERVATIONS_FOLDER, UMBType.create(Type.INT));

	// Enums

	/** UMB file entities which can be annotated/indexed */
	public enum UMBEntity implements UMBField
	{
		STATES, CHOICES, BRANCHES, OBSERVATIONS, PLAYERS;
		@Override
		public String toString()
		{
			switch (this) {
				case STATES: return "states";
				case CHOICES: return "choices";
				case BRANCHES: return "branches";
				case OBSERVATIONS: return "observations";
				case PLAYERS: return "players";
				default: return "?";
			}
		}
	}

	/** Notions of time */
	public enum Time implements UMBField
	{
		DISCRETE, STOCHASTIC, URGENT_STOCHASTIC
	}

	/** Common model types */
	public enum ModelType implements UMBField
	{
		DTMC, CTMC, MDP, POMDP, CTMDP, MA, TSG, LTS, TG,
		IDTMC, IMDP, IPOMDP, ITSG
	}

	// Index contents

	/** Model metadata */
	public static class ModelData
	{
		/** (Short) name of the model */
		public String name;
		/** Version info for the model */
		public String version;
		/** Model author(s) */
		public List<String> authors;
		/** Model description */
		public String description;
		/** Additional comments about the model */
		public String comment;
		/** DOI of the paper where the model was introduced/used/described */
		public String doi;
		/** URL pointing to more information about the model */
		public String url;

		/**
		 * Check this object is valid; throw an exception if not.
		 */
		public void validate() throws UMBException
		{
			// All optional: nothing to do
		}
	}

	/** File metadata */
	public static class FileData
	{
		/** The tool used to create this file */
		public String tool;
		/** Version of the tool used to create this file */
		public String toolVersion;
		/** Date of file creation */
		public Long creationDate = Instant.now().getEpochSecond();
		/** Tool parameters (e.g. string or list of command-line arguments) used */
		public Object parameters;

		/**
		 * Check this object is valid; throw an exception if not.
		 */
		public void validate() throws UMBException
		{
			// All optional: nothing to do
		}
	}

	/** Transition system details */
	public static class TransitionSystem
	{
		/** Notion of time used */
		public Time time;
		/** Number of players */
		@SerializedName("#players")
		public Integer numPlayers;
		/** Number of states */
		@SerializedName("#states")
		public Long numStates;
		/** Number of initial states */
		@SerializedName("#initial-states")
		public Long numInitialStates;
		/** Number of choices */
		@SerializedName("#choices")
		public Long numChoices;
		/** Number of choice actions */
		@SerializedName("#choice-actions")
		public Integer numChoiceActions;
		/** Number of branches */
		@SerializedName("#branches")
		public Long numBranches;
		/** Number of branch actions */
		@SerializedName("#branch-actions")
		public Integer numBranchActions;
		/** Number of observations */
		@SerializedName("#observations")
		public Integer numObservations;
		/** Observation style */
		public UMBEntity observationsApplyTo;
		/** Type of branch probabilities */
		public UMBType branchProbabilityType;
		/** Type of exit rates */
        public UMBType exitRateType;
		/** Type of probabilities for stochastic observations */
		public UMBType observationProbabilityType;
		/** Names of players (for games) */
		public List<String> playerNames;

		/**
		 * Check this object is valid; throw an exception if not.
		 */
		public void validate() throws UMBException
		{
			checkFieldExists(time, "time");
			checkFieldExists(numPlayers, "numPlayers");
			if (numPlayers < 0) {
				throw new UMBException("Number of players must be non-negative");
			}
			checkFieldExists(numStates, "numStates");
			if (numStates < 0) {
				throw new UMBException("Number of states must be at least 1");
			}
			checkFieldExists(numInitialStates, "numInitialStates");
			if (numInitialStates < 0) {
				throw new UMBException("Number of initial states must be non-negative");
			}
			checkFieldExists(numChoices, "numChoices");
			if (numChoices < 0) {
				throw new UMBException("Number of choices must be non-negative");
			}
			checkFieldExists(numChoiceActions, "numChoiceActions");
			if (numChoiceActions < 0) {
				throw new UMBException("Number of choice actions must be non-negative");
			}
			checkFieldExists(numBranches, "numBranches");
			if (numBranches < 0) {
				throw new UMBException("Number of branches must be non-negative");
			}
			checkFieldExists(numBranchActions, "numBranchActions");
			if (numBranchActions < 0) {
				throw new UMBException("Number of branch actions must be non-negative");
			}
			checkFieldExists(numObservations, "numObservations");
			if (numObservations < 0) {
				throw new UMBException("Number of observations must be non-negative");
			}
			if (numObservations > 0) {
				checkFieldExists(observationsApplyTo, "observationsApplyTo");
				if (!EnumSet.of(UMBEntity.STATES, UMBEntity.BRANCHES).contains(observationsApplyTo)) {
					throw new UMBException("Invalid value \" + observationsApplyTo" + "\" for " + fieldNameToUMB("observationsApplyTo"));
				}
			}
			if (branchProbabilityType != null) {
				if (!branchProbabilityType.type.isContinuousNumeric()) {
					throw new UMBException("Branch probability type must be a continuous numeric type");
				}
				if (!branchProbabilityType.isDefaultSize()) {
					throw new UMBException("Branch probability type must have default size");
				}
			}
			checkFieldExistsIff(exitRateType, "exitRateType", time == Time.STOCHASTIC || time == Time.URGENT_STOCHASTIC);
			if (exitRateType != null) {
				if (!exitRateType.type.isContinuousNumeric()) {
					throw new UMBException("Exit rate type must be a continuous numeric type");
				}
				if (!exitRateType.isDefaultSize()) {
					throw new UMBException("Exit rate type must have default size");
				}
			}
			if (observationProbabilityType != null) {
				if (!observationProbabilityType.type.isContinuousNumeric()) {
					throw new UMBException("Observation probability type must be a continuous numeric type");
				}
				if (!observationProbabilityType.isDefaultSize()) {
					throw new UMBException("Observation probability type must have default size");
				}
			}
			if (numPlayers > 1) {
				checkFieldExists(playerNames, "playerNames");
				if (playerNames.size() != numPlayers) {
					throw new UMBException("Player name list does not match number of players");
				}
				// check names are unique
				if (!playerNames.stream().allMatch(new HashSet<>()::add)) {
					throw new UMBException("Player names must be unique");
				}
			} else {
				if (playerNames != null) {
					throw new UMBException("Player names should be omitted in non-game models");
				}
			}
		}
	}

	/** Representation of a model annotation */
	public static class Annotation
	{
		/** Group ID (duplicated for convenience; same as map key) */
		public transient String group;
		/** ID (duplicated for convenience; same as map key) */
		public transient String id;
		/** Alias (name) */
		public String alias;
		/** Description */
		public String description;
		/** List of entities to which this annotation applies */
		public List<UMBEntity> appliesTo = new ArrayList<>();
		/** Type of values stored */
		public UMBType type;
		/** For string annotations, the number of strings */
		@SerializedName("#strings")
		public Integer numStrings;
		/** For stochastic annotations, the type for probability values */
		public UMBType probabilityType;
		/** For stochastic annotations, the sum of the supports of the distributions */
		@SerializedName("#probabilities")
		public Integer numProbabilities;

		/**
		 * Add an entity to which this annotation applies.
		 * @param entity The entity to add
		 */
		public void addAppliesTo(UMBEntity entity)
		{
			appliesTo.add(entity);
		}

		/**
		 * Check this object is valid; throw an exception if not.
		 */
		public void validate() throws UMBException
		{
			checkFieldExists(appliesTo, "appliesTo");
			if (appliesTo.isEmpty()) {
				throw new UMBException("Annotation \"" + id + "\" in group \"" + group + "\" is empty");
			}
			checkFieldExists(type, "type");
			checkFieldExistsIff(numStrings, "numStrings", type.type == Type.STRING);
			if (type.type == Type.STRING) {
				if (numStrings <= 0) {
					throw new UMBException("Number of strings must be positive");
				}
			}
			checkFieldExistsIff(numProbabilities, "#probabilities", probabilityType != null);
			if (probabilityType != null) {
				if (numProbabilities <= 0) {
					throw new UMBException("Number of probabilities must be positive");
				}
			}
		}

		/**
		 * Get the "name" of this annotation, i.e., the alias if present or the ID if not.
		 */
		public String getName()
		{
			return alias == null ? id : alias;
		}

		/**
		 * Check whether this annotation applies to the specified entity.
		 * @param entity The entity to check
		 */
		public boolean appliesTo(UMBEntity entity)
		{
			return appliesTo.contains(entity);
		}

		/**
		 * Get the type of the values stored in the annotation
		 */
		public UMBType getType()
		{
			return type;
		}

		/**
		 * Get the number of strings (only valid for string annotations)
		 */
		public int getNumStrings()
		{
			return numStrings;
		}

		/**
		 * Get the type for probability values (for stochastic annotations)
		 */
		public UMBType getProbabilityType()
		{
			return probabilityType;
		}

		/**
		 * Get the name of the folder to store this annotation, for the specified entity.
		 * @param entity The entity
		 */
		public String getFolderName(UMBEntity entity)
		{
			return UMBFormat.annotationFolder(group, id, entity);
		}

		/**
		 * Get the name of the file to store this annotation, for the specified entity.
		 * @param entity The entity
		 */
		public String getFilename(UMBEntity entity)
		{
			return UMBFormat.annotationFile(group, id, entity);
		}

		/**
		 * Get the filename for the distribution mapping if this is a stochastic annotation, for the specified entity.
		 * @param entity The entity
		 */
		public String getDistributionFilename(UMBEntity entity)
		{
			return UMBFormat.annotationDistributionFile(group, id, entity);
		}

		/**
		 * Get the filename for the probabilities if this is a stochastic annotation, for the specified entity.
		 * @param entity The entity
		 */
		public String getProbabilitiesFilename(UMBEntity entity)
		{
			return UMBFormat.annotationProbabilitiesFile(group, id, entity);
		}
	}

	/**
	 * Annotation object for data stored in the same style as an annotation,
	 * but without the usual conventions regarding group, id, folder, etc.
	 */
	public static class StandaloneAnnotation extends Annotation
	{
		/** Folder where data is stored */
		public String folderName;

		@Override
		public String getFolderName(UMBEntity entity)
		{
			return folderName + "/" + entity;
		}

		@Override
		public String getFilename(UMBEntity entity)
		{
			return getFolderName(entity) + "/" + UMBFormat.ANNOTATION_VALUES_FILE;
		}
	}

	/** Info about valuations (for e.g. states, observations) */
	public static class ValuationDescription
	{
		/** If true, valuations are unique within their entity */
		public Boolean unique;
		/** If there are any string variables, the number of strings */
		@SerializedName("#strings")
		public Integer numStrings;
		/** Descriptions for each class of valuations for an entity */
		public List<ValuationClassDescription> classes = new ArrayList<>();

		public ValuationDescription(boolean unique)
		{
			this.unique = unique;
		}
	}

	/** Info about a class of valuation (for e.g. states, observations) */
	public static class ValuationClassDescription
	{
		/** List of variables/padding making up each state valuation */
		public List<ValuationVariable> variables = new ArrayList<>();

		/**
		 * Get the total size of the variables/padding (in bits).
		 */
		public int numBits()
		{
			int numBits = 0;
			for (ValuationVariable variable : variables) {
				numBits += variable.numBits();
			}
			return numBits;
		}
	}

	/** Info about a valuation variable/padding */
	public static class ValuationVariable
	{
		// For variables
		/** Variable name */
		public String name;
		/** Is the variable optional */
		public Boolean isOptional;
		/** Variable type (including size) */
		public UMBType type;
		// For padding
		/** Amount of padding (number of bits) */
		public Integer padding;

		/**
		 * Is this a variable (as opposed to padding)?
		 */
		public boolean isVariable()
		{
			return padding == null;
		}

		/**
		 * Is this padding (as opposed to a variable)?
		 */
		public boolean isPadding()
		{
			return padding != null;
		}

		/**
		 * Get the size of this variable/padding (in bits).
		 */
		public int numBits()
		{
			if (padding != null) {
				return padding;
			} else if (type.size != null) {
				return type.size;
			} else {
				return 0;
			}
		}
	}

	/**
	 * Perform validation of this object
	 */
	public void validate() throws UMBException
	{
		checkFieldExists(formatVersion, "formatVersion");
		checkFieldExists(formatRevision, "formatVersion");
		if (modelData != null) {
			modelData.validate();
		}
		if (fileData != null) {
			fileData.validate();
		}
		checkFieldExists(transitionSystem, "transitionSystem");
		transitionSystem.validate();
		if (annotations != null) {
			for (Map.Entry<String, LinkedHashMap<String, Annotation>> entry : annotations.entrySet()) {
				validateAnnotations(entry.getKey(), entry.getValue());
			}
		}
		if (valuations != null) {
			for (Map.Entry<UMBEntity, ValuationDescription> entry : valuations.entrySet()) {
				checkFieldExists(entry.getValue().unique, "valuations." + entry.getKey() + ".unique");
				for (ValuationClassDescription valuationClassDescription : entry.getValue().classes) {
					validateValuationDescription(valuationClassDescription, "valuations.classes." + entry.getKey());
				}
			}
		}
	}

	/**
	 * Perform validation of the annotations
	 */
	public void validateAnnotations(String group, Map<String, Annotation> annotations) throws UMBException
	{
		for (Map.Entry<String, Annotation> entry : annotations.entrySet()) {
			if ("".equals(entry.getKey())) {
				throw new UMBException("Empty annotation ID in group \"" + group + "\"");
			}
			if (!UMBFormat.isValidID(entry.getKey())) {
				throw new UMBException("\"" + entry.getKey() + "\" is not a valid annotation ID in group \"" + group + "\"");
			}
			Annotation a = entry.getValue();
			a.validate();
		}
	}

	/**
	 * Perform validation of a valuation description
	 */
	public void validateValuationDescription(ValuationClassDescription valuationDescr, String fieldName) throws UMBException
	{
		checkFieldExists(valuationDescr.variables, fieldName + ".variables");
		for (ValuationVariable var : valuationDescr.variables) {
			validateValuationVariable(var);
		}
		// TODO: If strict and (valuations.numBits() % 8 != 0)
	}

	/**
	 * Perform validation of an individual valuation variable
	 */
	public void validateValuationVariable(ValuationVariable var) throws UMBException
	{
		// Should either be padding or a named variable
		if (var.padding != null) {
			if (var.name != null || var.type != null) {
				throw new UMBException("Malformed variable/padding in state valuation metadata");
			}
		} else {
			if (var.name == null) {
				throw new UMBException("Unnamed variable in state valuation metadata");
			}
			if (var.type == null) {
				throw new UMBException("Untyped variable in state valuation metadata");
			}
			if (var.type.size == null) {
				throw new UMBException("Only fixed size variables are currently supported for state valuations");
			}
		}
	}

	/**
	 * Create an empty UMBIndex.
	 */
	public UMBIndex()
	{
		// Default no-argument constructor needed for JSON deserialisation
	}

	// Setters

	/**
	 * Set the notion of time used for the model.
	 * @param time Notion of time
	 */
	public void setTime(Time time)
	{
		transitionSystem.time = time;
	}

	/**
	 * Set the number of players in the model.
	 * @param numPlayers The number of players
	 */
	public void setNumPlayers(int numPlayers)
	{
		transitionSystem.numPlayers = numPlayers;
	}

	/**
	 * Set the number of states in the model.
	 * @param numStates The number of states
	 */
	public void setNumStates(long numStates)
	{
		transitionSystem.numStates = numStates;
	}

	/**
	 * Set the number of initial states in the model.
	 * @param numInitialStates The number of initial states
	 */
	public void setNumInitialStates(long numInitialStates)
	{
		transitionSystem.numInitialStates = numInitialStates;
	}

	/**
	 * Set the number of choices in the model.
	 * @param numChoices The number of choices
	 */
	public void setNumChoices(long numChoices)
	{
		transitionSystem.numChoices = numChoices;
	}

	/**
	 * Set the number of branches in the model.
	 * @param numBranches The number of branches
	 */
	public void setNumBranches(long numBranches)
	{
		transitionSystem.numBranches = numBranches;
	}

	/**
	 * Set the number of choice actions in the model.
	 * @param numChoiceActions The number of choice actions
	 */
	public void setNumChoiceActions(int numChoiceActions)
	{
		transitionSystem.numChoiceActions = numChoiceActions;
	}

	/**
	 * Set the number of branch actions in the model.
	 * @param numBranchActions The number of choice actions
	 */
	public void setNumBranchActions(int numBranchActions)
	{
		transitionSystem.numBranchActions = numBranchActions;
	}

	/**
	 * Set the number of observations in the model.
	 * @param numObservations The number of observations
	 */
	public void setNumObservations(int numObservations)
	{
		transitionSystem.numObservations = numObservations;
	}

	/**
	 * Set the entity (e.g. states/branches) observations are attached to.
	 * @param observationsApplyTo The entity
	 */
	public void setObservationsApplyTo(UMBEntity observationsApplyTo)
	{
		transitionSystem.observationsApplyTo = observationsApplyTo;
	}

	/**
	 * Set the type of branch probabilities used in the model.
	 * @param branchProbabilityType The type of branch probabilities
	 */
	public void setBranchProbabilityType(UMBType branchProbabilityType)
	{
		transitionSystem.branchProbabilityType = branchProbabilityType;
	}

	/**
	 * Set the type of exit rates used in the model.
	 * @param exitRateType The type of exit rates
	 */
	public void setExitRateType(UMBType exitRateType)
	{
		transitionSystem.exitRateType = exitRateType;
	}

	/**
	 * Set the type of probabilities for stochastic observations in the model.
	 * @param observationProbabilityType The type of observation probabilities
	 */
	public void setObservationProbabilityType(UMBType observationProbabilityType)
	{
		transitionSystem.observationProbabilityType = observationProbabilityType;
	}

	/**
	 * Set the names of players in the model (for games
	 * @param playerNames The list of player names
	 */
	public void setPlayerNames(List<String> playerNames)
	{
		transitionSystem.playerNames = new ArrayList<>(playerNames);
	}

	/**
	 * Convenience method to set model metadata for a range of common models,
	 * i.e., those that are included in the {@link ModelType} enum.
	 * For some models (games, POMDPs), further configuration will be needed
	 * since fields are just set to defaults (players = 2, observations = 1).
	 * @param modelType The model type
	 * @param rational Are probabilities rationals (as opposed to doubles)?
	 */
	public void setModelType(ModelType modelType, boolean rational) throws UMBException
	{
		switch (modelType) {
			case DTMC:
			case IDTMC:
				setTime(Time.DISCRETE);
				setNumPlayers(0);
				setBranchProbabilityType(rational ? UMBType.create(Type.RATIONAL) : UMBType.create(Type.DOUBLE));
				setExitRateType(null);
				break;
			case CTMC:
				setTime(Time.STOCHASTIC);
				setNumPlayers(0);
				setBranchProbabilityType(rational ? UMBType.create(Type.RATIONAL) : UMBType.create(Type.DOUBLE));
				setExitRateType(rational ? UMBType.create(Type.RATIONAL) : UMBType.create(Type.DOUBLE));
				break;
			case MDP:
			case POMDP:
				setTime(Time.DISCRETE);
				setNumPlayers(1);
				setBranchProbabilityType(rational ? UMBType.create(Type.RATIONAL) : UMBType.create(Type.DOUBLE));
				setExitRateType(null);
				break;
			case IMDP:
			case IPOMDP:
				setTime(Time.DISCRETE);
				setNumPlayers(1);
				setBranchProbabilityType(rational ? UMBType.create(Type.RATIONAL_INTERVAL) : UMBType.create(Type.DOUBLE_INTERVAL));
				setExitRateType(null);
				break;
			case CTMDP:
				setTime(Time.STOCHASTIC);
				setNumPlayers(1);
				setBranchProbabilityType(rational ? UMBType.create(Type.RATIONAL) : UMBType.create(Type.DOUBLE));
				setExitRateType(rational ? UMBType.create(Type.RATIONAL) : UMBType.create(Type.DOUBLE));
				break;
			case MA:
				setTime(Time.URGENT_STOCHASTIC);
				setNumPlayers(1);
				setBranchProbabilityType(rational ? UMBType.create(Type.RATIONAL) : UMBType.create(Type.DOUBLE));
				setExitRateType(rational ? UMBType.create(Type.RATIONAL) : UMBType.create(Type.DOUBLE));
				break;
			case TSG:
			case ITSG:
				setTime(Time.DISCRETE);
				setNumPlayers(2);
				setBranchProbabilityType(null);
				setExitRateType(null);
				break;
			case LTS:
				setTime(Time.DISCRETE);
				setNumPlayers(1);
				setBranchProbabilityType(null);
				setExitRateType(null);
				break;
			case TG:
				setTime(Time.DISCRETE);
				setNumPlayers(2);
				setBranchProbabilityType(null);
				setExitRateType(null);
				break;
			default:
				throw new UMBException("Unsupported model type \"" + modelType + "\"");
		}
		if (EnumSet.of(ModelType.POMDP, ModelType.IPOMDP).contains(modelType)) {
			setNumObservations(1);
			setObservationsApplyTo(UMBEntity.STATES);
		} else {
			setNumObservations(0);
		}
	}

	/**
	 * Add a new annotation, for now without any data attached.
	 * If an alias is provided, there should not already exist
	 * an annotation in the same group with the same alias.
	 * @param group The ID of the group
	 * @param alias Optional alias (name) for the annotation ("" or null if not needed)
	 * @param type The type of the values to be stored in the annotation
	 */
	public Annotation addAnnotation(String group, String alias, UMBType type) throws UMBException
	{
		// Check type
		if (!type.type.isRational() && !type.isDefaultSize()) {
			throw new UMBException("Annotation type must have default size unless it is rational");
		}
		// Check if group exists; create if not
		if (!annotationGroups.contains(group)) {
			if (!UMBFormat.isValidID(group)) {
				throw new UMBException("Invalid group ID \"" + group + "\"");
			}
			annotationGroups.add(group);
			annotationAliasMaps.put(group, new LinkedHashMap<>());
			annotations.put(group, new LinkedHashMap<>());
		}
		// If alias is present, check it does not already exist
		if (alias != null && !alias.isEmpty()) {
			Map<String, String> nameMap = annotationAliasMaps.get(group);
			if (nameMap.containsKey(alias)) {
				throw new UMBException("Duplicate alias \"" + alias + "\" in group \"" + group + "\"");
			}
		}
		// Create/store annotation
		// (ID is based on alias if present; if not, just use integer, 1-indexed indices)
		LinkedHashMap<String, Annotation> grpAnnotations = annotations.get(group);
		Annotation annotation = new Annotation();
		annotation.group = group;
		String id = Integer.toString(grpAnnotations.size() + 1);
		if (alias != null && !alias.isEmpty()) {
			id = UMBFormat.toValidUniqueID(alias, grpAnnotations::containsKey);
			annotation.alias = alias;
			annotationAliasMaps.get(group).put(alias, id);
		}
		annotation.id = id;
		annotation.type = type;
		grpAnnotations.put(id, annotation);
		return annotation;
	}

	/**
	 * Create a standalone annotation object without adding it to the index.
	 */
	public static Annotation createStandaloneAnnotation(String folderName, UMBType type)
	{
		StandaloneAnnotation annotation = new StandaloneAnnotation();
		annotation.type = type;
		annotation.folderName = folderName;
		return annotation;
	}

	/**
	 * Add a new description for (a single class of) valuations to be attached to some model entity,
	 * extracted from a {@link UMBBitPacking} object.
	 * @param entity The entity to which the valuations apply
	 * @param unique Whether valuations are unique within the entity
	 * @param bitPacking Definition of valuation contents
	 */
	public void addSingleValuationDescription(UMBEntity entity, boolean unique, UMBBitPacking bitPacking) throws UMBException
	{
		addValuationDescriptions(entity, unique);
		addValuationDescriptionClass(entity, bitPacking);
	}

	/**
	 * Specify that valuations are to be attached to some model entity,
	 * and whether they are unique, without adding any descriptions.
	 * This is done subsequently via calls to {@link #addValuationDescriptionClass(UMBEntity, UMBBitPacking)}.
	 * extracted from a list of {@link UMBBitPacking} objects.
	 * @param entity The entity to which the valuations apply
	 * @param unique Whether valuations are unique within the entity
	 */
	public void addValuationDescriptions(UMBEntity entity, boolean unique) throws UMBException
	{
		if (valuations.containsKey(entity)) {
			throw new UMBException("Valuations have already been added for " + entity + "");
		} else {
			valuations.put(entity, new ValuationDescription(unique));
		}
	}

	/**
	 * Add a new description for a class of valuations to be attached to some model entity,
	 * extracted from a {@link UMBBitPacking} object.
	 * Returns the index of the class within those that exist for the entity.
	 * @param entity The entity to which the valuations apply
	 * @param bitPacking Definition of valuation contents
	 */
	public int addValuationDescriptionClass(UMBEntity entity, UMBBitPacking bitPacking) throws UMBException
	{
		ValuationDescription valuationDescr = valuations.get(entity);
		valuationDescr.classes.add(bitPacking.toValuationClassDescription());
		return valuationDescr.classes.size() - 1;
	}

	// Getters

	/**
	 * Get the notion of time used for the model.
	 */
	public Time getTime()
	{
		return transitionSystem.time;
	}

	/**
	 * Get the number of players in the model.
	 */
	public int getNumPlayers()
	{
		return transitionSystem.numPlayers;
	}

	/**
	 * Get the number of states in the model.
	 */
	public long getNumStates()
	{
		return transitionSystem.numStates;
	}

	/**
	 * Get the number of initial states in the model.
	 */
	public long getNumInitialStates()
	{
		return transitionSystem.numInitialStates;
	}

	/**
	 * Get the number of choices in the model.
	 */
	public long getNumChoices()
	{
		return transitionSystem.numChoices;
	}

	/**
	 * Get the number of branches in the model.
	 */
	public long getNumBranches()
	{
		return transitionSystem.numBranches;
	}

	/**
	 * Get the number of choice actions in the model.
	 */
	public int getNumChoiceActions()
	{
		return transitionSystem.numChoiceActions;
	}

	/**
	 * Get the number of branch actions in the model.
	 */
	public int getNumBranchActions()
	{
		return transitionSystem.numBranchActions;
	}

	/**
	 * Get the number of observations in the model.
	 */
	public int getNumObservations()
	{
		return transitionSystem.numObservations;
	}

	/**
	 * Get the entity (e.g. states/branches) observations are attached to.
	 */
	public UMBEntity getObservationsApplyTo()
	{
		return transitionSystem.observationsApplyTo;
	}

	/**
	 * Get the type of branch probabilities used in the model.
	 */
	public UMBType getBranchProbabilityType()
	{
		return transitionSystem.branchProbabilityType;
	}

	/**
	 * Get the type of exit rates used in the model.
	 */
	public UMBType getExitRateType()
	{
		return transitionSystem.exitRateType;
	}

	/**
	 * Get the type of probabilities for stochastic observations in the model.
	 */
	public UMBType getObservationProbabilityType()
	{
		return transitionSystem.observationProbabilityType;
	}

	/**
	 * Get the names of the players in the model (for games).
	 * Only present if {@link #getNumPlayers()} is greater than one.
	 */
	public List<String> getPlayerNames()
	{
		return transitionSystem.playerNames;
	}

	/**
	 * Get the number of the specified entity (states, choices, etc.).
	 */
	public long getEntityCount(UMBEntity entity) throws UMBException
	{
		switch (entity) {
			case STATES:
				return getNumStates();
			case CHOICES:
				return getNumChoices();
			case BRANCHES:
				return getNumBranches();
			case OBSERVATIONS:
				return getNumObservations();
			case PLAYERS:
				return getNumPlayers();
			default:
				throw new UMBException("Unsupported entity \"" + entity + "\"");
		}
	}

	/**
	 * Convenience method to get the model type, for a range of common models,
	 * i.e., those that are included in the {@link ModelType} enum.
	 * Returns null if the model type is not one of the common types.
	 */
	public ModelType getModelType()
	{
		boolean prob = transitionSystem.branchProbabilityType != null;
		int numPlayers = getNumPlayers();
		boolean pObs = getNumObservations() > 0;
		Time time = getTime();
		boolean intv = transitionSystem.branchProbabilityType != null && transitionSystem.branchProbabilityType.type.isInterval();

		ModelType modelType = null;
		// Probabilistic models
		if (prob) {
			if (numPlayers == 0) {
				if (pObs) {
					return null;
				}
				switch (time) {
					case DISCRETE:
						modelType = ModelType.DTMC;
						break;
					case STOCHASTIC:
						modelType = ModelType.CTMC;
						break;
					case URGENT_STOCHASTIC:
						return null;
				}
			} else if (numPlayers == 1) {
				switch (time) {
					case DISCRETE:
						modelType = pObs ? ModelType.POMDP : ModelType.MDP;
						break;
					case STOCHASTIC:
						modelType = pObs ? null: ModelType.CTMDP;
						break;
					case URGENT_STOCHASTIC:
						modelType = pObs ? null: ModelType.MA;
						break;
				}
			} else {
				if (pObs) {
					return null;
				}
				switch (time) {
					case DISCRETE:
						modelType = ModelType.TSG;
						break;
					case STOCHASTIC:
					case URGENT_STOCHASTIC:
						return null;
				}
			}
		}
		// Non-probabilistic models
		else {
			if (pObs) {
				return null;
			}
			// Ignore time/intervals
			if (numPlayers == 0) {
				return null;
			} else if (numPlayers == 1) {
				return ModelType.LTS;
			} else {
				return ModelType.TG;
			}
		}
		// If still unknown, give up
		if (modelType == null) {
			return null;
		}
		// Deal with interval variants
		if (!intv) {
			return modelType;
		} else {
			switch (modelType) {
				case DTMC:
					return ModelType.IDTMC;
				case MDP:
					return ModelType.IMDP;
				case POMDP:
					return ModelType.IPOMDP;
				case TSG:
					return ModelType.ITSG;
				default:
					return null;
			}
		}
	}

	// Methods to get info about annotations

	/**
	 * Get a stored model annotation
	 * @param group The ID of the group containing the annotation
	 * @param id The ID of the annotation within the group
	 */
	public Annotation getAnnotation(String group, String id) throws UMBException
	{
		LinkedHashMap<String, Annotation> grpAnnotations = annotations.get(group);
		if (grpAnnotations == null) {
			throw new UMBException("Unknown annotation group ID \"" + group + "\"");
		}
		Annotation annotation = grpAnnotations.get(id);
		if (annotation == null) {
			throw new UMBException("Unknown annotation ID \"" + id + "\" in group \"" + group + "\"");
		}
		return annotation;
	}

	/**
	 * See if a stored model annotation with a given alias exists.
	 * @param group The ID of the group containing the annotation
	 * @param alias The alias of the annotation
	 */
	public boolean annotationWithAliasExists(String group, String alias)
	{
		Map<String, String> aliasMap = annotationAliasMaps.get(group);
		if (aliasMap == null) {
			return false;
		}
		return aliasMap.containsKey(alias);
	}

	/**
	 * Get a stored model annotation by its alias.
	 * Throws an exception if no such annotation exists.
	 * @param group The ID of the group containing the annotation
	 * @param alias The alias of the annotation
	 */
	public String getAnnotationIdForAlias(String group, String alias) throws UMBException
	{
		Map<String, String> aliasMap = annotationAliasMaps.get(group);
		if (aliasMap == null) {
			throw new UMBException("Annotation group \"" + group + "\" does not exist");
		}
		String id = aliasMap.get(alias);
		if (id == null) {
			throw new UMBException("No annotation with alias \"" + alias + "\" in group \"" + group + "\" exists");
		}
		return id;
	}

	// Methods to get info about AP annotations

	/**
	 * Get all AP annotations, as an ordered map from ID to {@link Annotation}.
	 * This is guaranteed to return a non-null map, but it may be empty.
	 */
	public LinkedHashMap<String, Annotation> getAPAnnotations()
	{
		return annotations.getOrDefault(UMBFormat.AP_ANNOTATIONS_GROUP, new LinkedHashMap<>());
	}

	/**
	 * Get all AP annotations as a list.
	 */
	public List<Annotation> getAPAnnotationsList()
	{
		return new ArrayList<>(getAPAnnotations().values());
	}

	/**
	 * Does this UMB file have any AP annotations?
	 */
	public boolean hasAPAnnotations()
	{
		return !getAPAnnotations().isEmpty();
	}

	/**
	 * Get the number of AP annotations.
	 */
	public int getNumAPAnnotations()
	{
		return getAPAnnotations().size();
	}

	/**
	 * Get the {@code i}th AP annotation.
	 */
	public Annotation getAPAnnotation(int i)
	{
		return getAPAnnotationsList().get(i);
	}

	/**
	 * Get an AP annotation by its ID
	 * Throws an exception if no such annotation exists.
	 */
	public Annotation getAPAnnotationByID(String apID) throws UMBException
	{
		Annotation annotation = getAPAnnotations().get(apID);
		if (annotation == null) {
			throw new UMBException("Unknown AP annotation ID \"" + apID + "\"");
		}
		return annotation;
	}

	/**
	 * Does this UMB file have an AP annotation with the specified alias?
	 */
	public boolean hasAPAnnotationWithAlias(String apAlias)
	{
		return annotationWithAliasExists(UMBFormat.AP_ANNOTATIONS_GROUP, apAlias);
	}

	/**
	 * Get an AP annotation by its alias.
	 * Throws an exception if no such annotation exists.
	 */
	public Annotation getAPAnnotationByAlias(String apAlias) throws UMBException
	{
		String apID = getAnnotationIdForAlias(UMBFormat.AP_ANNOTATIONS_GROUP, apAlias);
		return getAPAnnotationByID(apID);
	}

	/**
	 * Get the names of all AP annotations (name is alias if present or ID if not)
	 */
	public List<String> getAPNames()
	{
		return getAPAnnotationsList().stream()
				.map(Annotation::getName)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	// Methods to get info about reward annotations

	/**
	 * Get all reward annotations, as an ordered map from ID to {@link Annotation}.
	 * This is guaranteed to return a non-null map, but it may be empty.
	 */
	public LinkedHashMap<String, Annotation> getRewardAnnotations()
	{
		return annotations.getOrDefault(UMBFormat.REWARD_ANNOTATIONS_GROUP, new LinkedHashMap<>());
	}

	/**
	 * Get all reward annotations as a list.
	 */
	public List<Annotation> getRewardAnnotationsList()
	{
		return new ArrayList<>(getRewardAnnotations().values());
	}

	/**
	 * Does this UMB file have any reward annotations?
	 */
	public boolean hasRewardAnnotations()
	{
		return !getRewardAnnotations().isEmpty();
	}

	/**
	 * Get the number of reward annotations.
	 */
	public int getNumRewardAnnotations()
	{
		return getRewardAnnotations().size();
	}

	/**
	 * Get the {@code i}th reward annotation.
	 */
	public Annotation getRewardAnnotation(int i)
	{
		return getRewardAnnotationsList().get(i);
	}

	/**
	 * Get a reward annotation by its ID
	 * Throws an exception if no such annotation exists.
	 */
	public Annotation getRewardAnnotationByID(String rewardID) throws UMBException
	{
		Annotation annotation = getRewardAnnotations().get(rewardID);
		if (annotation == null) {
			throw new UMBException("Unknown reward annotation ID \"" + rewardID + "\"");
		}
		return annotation;
	}

	/**
	 * Does this UMB file have a reward annotation with the specified alias?
	 */
	public boolean hasRewardAnnotationWithAlias(String rewardAlias)
	{
		return annotationWithAliasExists(UMBFormat.REWARD_ANNOTATIONS_GROUP, rewardAlias);
	}

	/**
	 * Get a reward annotation by its alias.
	 * Throws an exception if no such annotation exists.
	 */
	public Annotation getRewardAnnotationByAlias(String rewardAlias) throws UMBException
	{
		String rewardID = getAnnotationIdForAlias(UMBFormat.REWARD_ANNOTATIONS_GROUP, rewardAlias);
		return getRewardAnnotationByID(rewardID);
	}

	/**
	 * Get the names of all reward annotations (name is alias if present or ID if not)
	 */
	public List<String> getRewardNames()
	{
		return getRewardAnnotationsList().stream()
				.map(Annotation::getName)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public boolean hasStateRewards(int i)
	{
		return getRewardAnnotation(i).appliesTo(UMBEntity.STATES);
	}

	public boolean hasChoiceRewards(int i)
	{
		return getRewardAnnotation(i).appliesTo(UMBEntity.CHOICES);
	}

	public boolean hasBranchRewards(int i)
	{
		return getRewardAnnotation(i).appliesTo(UMBEntity.BRANCHES);
	}

	// Methods to get info about valuations

	/**
	 * Does this UMB file have any valuations for the specified entity?
	 */
	public boolean hasValuations(UMBEntity entity)
	{
		return valuations != null && valuations.containsKey(entity) && !valuations.get(entity).classes.isEmpty();
	}

	/**
	 * Does this UMB file have unique valuations for the specified entity?
	 */
	public boolean areValuationsUnique(UMBEntity entity)
	{
		return valuations != null && valuations.containsKey(entity) && valuations.get(entity).unique;
	}

	/**
	 * Get a {@link UMBBitPacking} object describing the valuations for the specified entity.
	 * If there is more than one class of valuation for the entity, it returns the first one.
	 * Use {@link #getValuationBitPacking(UMBEntity, int)} to get others.
	 * Throws an exception if no such metadata is present.
	 */
	public UMBBitPacking getValuationBitPacking(UMBEntity entity) throws UMBException
	{
		return getValuationBitPacking(entity, 0);
	}

	/**
	 * Get a {@link UMBBitPacking} object for the {@code i}th class of valuation for the specified entity.
	 * Throws an exception if no such metadata is present.
	 */
	public UMBBitPacking getValuationBitPacking(UMBEntity entity, int i) throws UMBException
	{
		if (valuations == null || !valuations.containsKey(entity) || valuations.get(entity).classes.size() <= i) {
			throw new UMBException("No valuation metadata present");
		}
		return new UMBBitPacking(valuations.get(entity).classes.get(i));
	}

	/**
	 * Get the number of classes of valuations for the specified entity.
	 * Throws an exception if no such metadata is present.
	 */
	public int getNumValuationClasses(UMBEntity entity) throws UMBException
	{
		if (valuations == null || !valuations.containsKey(entity)) {
			throw new UMBException("No valuation metadata present");
		}
		return valuations.get(entity).classes.size();
	}

	// Validation methods

	/**
	 * Check whether a field exists (is non-null) and throw an exception if not.
	 * @param field The field, as stored in {@link UMBIndex}
	 * @param fieldName The name of the field, as stored in {@link UMBIndex} (not JSON/UMB)
	 */
	private static void checkFieldExists(Object field, String fieldName) throws UMBException
	{
		checkFieldExistsIff(field, fieldName, true);
	}

	/**
	 * Check whether a field exists (is non-null) iff {@code condition} is true and throw an exception if not.
	 * @param field The field, as stored in {@link UMBIndex}
	 * @param fieldName The name of the field, as stored in {@link UMBIndex} (not JSON/UMB)
	 * @param condition The condition
	 */
	private static void checkFieldExistsIff(Object field, String fieldName, boolean condition) throws UMBException
	{
		if (condition && field == null) {
			throw new UMBException("Required field \"" + fieldNameToUMB(fieldName) + "\" is missing");
		}
		if (!condition && field != null) {
			throw new UMBException("Field \"" + fieldNameToUMB(fieldName) + "\" should not be present");
		}
	}

	// Import/export from/to JSON

	/**
	 * Convert this index to JSON format.
	 */
	public String toJSON()
	{
		return gsonBuilder().toJson(this);
	}

	/**
	 * Parse an index from JSON format.
	 */
	public static UMBIndex fromJSON(String json) throws UMBException
	{
		try {
			UMBIndex umbIndex = gsonBuilder().fromJson(json, UMBIndex.class);
			umbIndex.buildAnnotationInfo();
			return umbIndex;
		} catch (JsonParseException e) {
			throw new UMBException(e.getMessage());
		}
	}

	/**
	 * Build derived info {@code annotationGroups} and {@code annotationAliasMaps}
	 * from the list of annotations in {@code annotations}.
	 */
	private void buildAnnotationInfo()
	{
		annotationGroups = new ArrayList<>();
		annotationAliasMaps = new LinkedHashMap<>();
		annotationGroups.addAll(annotations.keySet());
		for (Map.Entry<String, LinkedHashMap<String, Annotation>> entry2 : annotations.entrySet()) {
			String group = entry2.getKey();
			Map<String, String> aliasMap = new LinkedHashMap<>();
			annotationAliasMaps.put(group, aliasMap);
			for (Map.Entry<String, Annotation> entry : entry2.getValue().entrySet()) {
				entry.getValue().group = group;
				entry.getValue().id = entry.getKey();
				if (entry.getValue().alias != null) {
					aliasMap.put(entry.getValue().alias, entry.getKey());
				}
			}
		}
	}

	/**
	 * Configure a Gson object for UMB import/export.
	 */
	private static Gson gsonBuilder()
	{
		return new GsonBuilder()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
			.registerTypeHierarchyAdapter(UMBField.class, new EnumSerializer<>())
			.registerTypeHierarchyAdapter(UMBField.class, new EnumDeserializer<>())
			.setPrettyPrinting()
			.create();
	}

	/**
	 * Customised enum behaviour for UMB field (de)serialisation.
	 */
	interface UMBField
	{
		default String description()
		{
			return toUMB(((Enum) this).name());
		}

		/**
		 * Convert to UMB-style field values (lower case, hyphenated)
		 */
		static String toUMB(String value)
		{
			return value.toLowerCase().replace("_", "-");
		}
	}

	/**
	 * Convert the name of a field, as stored in {@link UMBIndex} to its name in UMB JSON,
	 * i.e., converting (possibly capitalised) camel case to lower case hyphenated.
	 * Actual serialisation is done with {@link FieldNamingPolicy#LOWER_CASE_WITH_DASHES}
	 * but this should match closely enough for error reporting etc.
	 */
	public static String fieldNameToUMB(String field)
	{
		return field.replaceAll("([a-z])([A-Z])", "$1-$2").replaceAll("^([A-Z])", "$1").toLowerCase();
	}

	/**
	 * Custom JSON serializer for enums, following style of UMB specification
	 */
	static class EnumSerializer<T extends Enum<T>> implements JsonSerializer<T>
	{
		@Override
		public JsonElement serialize(T t, java.lang.reflect.Type type, JsonSerializationContext jsonSerializationContext)
		{
			// Format for UMB (lower case, hyphenated)
			return new JsonPrimitive(UMBField.toUMB(t.name()));
		}
	}

	/**
	 * Custom JSON deserializer for enums, following style of UMB specification
	 */
	static class EnumDeserializer<T extends Enum<T>> implements JsonDeserializer<T>
	{
		@Override
		public T deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context)
		{
			String fieldValue = json.getAsString();
			try {
				// Look up enum, ignoring UMB field style conversion (lower case, hyphenated))
				// And throw an exception if lookup fails (GSON default is to just store null)
				if (!json.isJsonPrimitive()) {
					throw new IllegalArgumentException();
				}
				for (T enumConstant : ((Class<T>) typeOfT).getEnumConstants()) {
					if (UMBField.toUMB(enumConstant.name()).equals(UMBField.toUMB(fieldValue))) {
						return enumConstant;
					}
				}
				throw new IllegalArgumentException();
			} catch (IllegalArgumentException e) {
				String fieldName = fieldNameToUMB(((Class<T>) typeOfT).getSimpleName());
				throw new JsonParseException("Invalid value \"" + fieldValue + "\" for " + fieldName, e);
			}
		}
	}
}
