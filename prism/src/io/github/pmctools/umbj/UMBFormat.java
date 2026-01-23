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

import java.util.List;
import java.util.function.Predicate;

/**
 * Class storing conventions about how/where entities are stored in a UMB file.
 */
public class UMBFormat
{
	/** Filename for (JSON) index */
	public static final String INDEX_FILE = "index.json";

	/** File extension for binary files in the archive */
	public static final String BIN_FILE_EXT = ".bin";

	// Core model information

	public static final String STATE_CHOICE_OFFSETS_FILE = "state-to-choices" + BIN_FILE_EXT;
	public static final String STATE_PLAYERS = "state-to-player" + BIN_FILE_EXT;
	public static final String INITIAL_STATES_FILE = "state-is-initial" + BIN_FILE_EXT;
	public static final String MARKOVIAN_STATES_FILE = "state-is-markovian" + BIN_FILE_EXT;
	public static final String STATE_EXIT_RATES_FILE = "state-to-exit-rate" + BIN_FILE_EXT;
	public static final String CHOICE_BRANCH_OFFSETS_FILE = "choice-to-branches" + BIN_FILE_EXT;
	public static final String BRANCH_TARGETS_FILE = "branch-to-target" + BIN_FILE_EXT;
	public static final String BRANCH_PROBABILITIES_FILE = "branch-to-probability" + BIN_FILE_EXT;
	public static final String ACTIONS_FOLDER = "actions";
	public static final String OBSERVATIONS_FOLDER = "observations";
	public static final String OBSERVATIONS_FILE = "values" + BIN_FILE_EXT;

	// String list storage info

	public static final String STRING_OFFSETS_FILE = "string-mapping" + BIN_FILE_EXT;
	public static final String STRINGS_FILE = "strings" + BIN_FILE_EXT;

	// Annotations

	/** Location for annotations (folder in zip) */
	public static final String ANNOTATIONS_FOLDER = "annotations";

	/** Filename for storing an annotation's values */
	public static final String ANNOTATION_VALUES_FILE = "values" + BIN_FILE_EXT;

	/** Filename for storing a stochastic annotation's distribution mapping */
	public static final String ANNOTATION_DISTRIBUTION_FILE = "distribution-mapping" + BIN_FILE_EXT;

	/** Filename for storing a stochastic annotation's distribution mapping */
	public static final String ANNOTATION_PROBABILITIES_FILE = "probabilities" + BIN_FILE_EXT;

	// Subfolders for built-in annotation groups

	public static final String AP_ANNOTATIONS_GROUP = "aps";
	public static final String REWARD_ANNOTATIONS_GROUP = "rewards";

	// Valuations

	/** Location for valuations (folder in zip) */
	public static final String VALUATIONS_FOLDER = "valuations";

	/** Filename for storing valuation data (variable values) */
	public static final String VALUATIONS_FILE = "valuations" + BIN_FILE_EXT;

	/** Filename for storing valuation classes */
	public static final String VALUATION_CLASSES = "valuation-to-class" + BIN_FILE_EXT;

	// Allowable compression formats

	public enum CompressionFormat
	{
		GZIP,
		XZ;
		public String extension() {
			switch (this) {
				case GZIP: return "gz";
				case XZ: return "xz";
				default: throw new IllegalStateException("Unknown compression format: " + this);
			}
		}
	}

	/** Allowable compression formats (strict) */
	public static final List<CompressionFormat> ALLOWED_COMPRESSION_FORMATS = List.of(CompressionFormat.GZIP, CompressionFormat.XZ);

	/** Default compression format */
	public static final CompressionFormat DEFAULT_COMPRESSION_FORMAT = CompressionFormat.GZIP;

	/**
	 * Get the filename for the offsets mapping string indices to string data within some folder
	 */
	public static String stringOffsetsFile(String folderName)
	{
		return folderName + "/" + STRING_OFFSETS_FILE;
	}

	/**
	 * Get the filename for the string data within some folder
	 */
	public static String stringsFile(String folderName)
	{
		return folderName + "/" + STRINGS_FILE;
	}

	/**
	 * Get the folder name for observations
	 */
	public static String observationsFolder(UMBIndex.UMBEntity entity)
	{
		return OBSERVATIONS_FOLDER + "/" + entity;
	}

	/**
	 * Get the filename for observations
	 */
	public static String observationsFile(UMBIndex.UMBEntity entity)
	{
		return observationsFolder(entity) + "/" + OBSERVATIONS_FILE;
	}

	/**
	 * Get the folder name for an annotation
	 */
	public static String annotationFolder(String group, String id, UMBIndex.UMBEntity entity)
	{
		return ANNOTATIONS_FOLDER + "/" + group + "/" + id + "/" + entity;
	}

	/**
	 * Get the filename for an annotation
	 */
	public static String annotationFile(String group, String id, UMBIndex.UMBEntity entity)
	{
		return annotationFolder(group, id, entity) + "/" + ANNOTATION_VALUES_FILE;
	}

	/**
	 * Get the filename for a stochastic annotation's distribution mapping
	 */
	public static String annotationDistributionFile(String group, String id, UMBIndex.UMBEntity entity)
	{
		return annotationFolder(group, id, entity) + "/" + ANNOTATION_DISTRIBUTION_FILE;
	}

	/**
	 * Get the filename for a stochastic annotation's probabilities
	 */
	public static String annotationProbabilitiesFile(String group, String id, UMBIndex.UMBEntity entity)
	{
		return annotationFolder(group, id, entity) + "/" + ANNOTATION_PROBABILITIES_FILE;
	}

	/**
	 * Get the folder name for the valuations for some entity
	 */
	public static String valuationsFolder(UMBIndex.UMBEntity entity)
	{
		return VALUATIONS_FOLDER + "/" + entity;
	}

	/**
	 * Get the filename for the valuation data for some entity
	 */
	public static String valuationsFile(UMBIndex.UMBEntity entity)
	{
		return valuationsFolder(entity) + "/" + VALUATIONS_FILE;
	}

	/**
	 * Get the filename for the valuation classes for some entity
	 */
	public static String valuationClassesFile(UMBIndex.UMBEntity entity)
	{
		return valuationsFolder(entity) + "/" + VALUATION_CLASSES;
	}

	// Utility functions

	/**
	 * Check whether a string represents a valid ID.
	 * @param id Proposed ID
	 */
	public static boolean isValidID(String id)
	{
		return id.matches("[a-z0-9_-]+");
	}

	/**
	 * Convert a string to a valid ID.
	 * @param s String to convert
	 */
	public static String toValidID(String s)
	{
		return s.toLowerCase().replaceAll("[^a-z0-9_-]", "_").replace("^[0-9]", "_");
	}

	/**
	 * Convert a string to a unique valid ID.
	 * @param s String to convert
	 * @param idExists Predicate defining what IDs already exist
	 */
	public static String toValidUniqueID(String s, Predicate<String> idExists)
	{
		String id = toValidID(s);
		while (idExists.test(id)) {
			id += "_";
		}
		return id;
	}
}
