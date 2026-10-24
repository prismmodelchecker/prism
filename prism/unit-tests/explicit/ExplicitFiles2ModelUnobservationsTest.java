//==============================================================================
//
//	Copyright (c) 2026-
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

package explicit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import io.ExplicitModelImporter;
import io.PrismExplicitImporter;
import io.UMBExporter;
import io.UMBImporter;
import parser.State;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismDevNullLog;
import prism.PrismException;

/**
 * Tests for the unobservations of POMDPs built by {@link ExplicitFiles2Model},
 * from explicit (.tra/.sta/.obs) files and from UMB files.
 *
 * As for POMDPs built from PRISM models, the unobservation of a state should
 * comprise the values of its non-observable variables (so beliefs, stored over
 * unobservations, are small), provided that, with observations, these identify
 * states uniquely; otherwise, each state should be its own unobservation.
 */
public class ExplicitFiles2ModelUnobservationsTest
{
	// A POMDP with observable variable x and non-observable variable h,
	// where values of h are repeated across observations (x=0, x=1), so that
	// there are 2 unobservations (h=0, h=1) for 4 states. Files were exported
	// by PRISM from:
	//   pomdp
	//   observables x endobservables
	//   module m
	//     x : [0..1] init 0;
	//     h : [0..1] init 0;
	//     [a] x=0 & h=0 -> 0.5 : (h'=1) + 0.5 : (x'=1);
	//     [a] x=0 & h=1 -> (x'=1);
	//     [a] x=1 -> (h'=1-h);
	//   endmodule

	private static final String TRA = String.join("\n",
			"# Transitions (POMDP)",
			"4 4 5 2",
			"- - 0 - 0",
			"0 0 1 0.5 0 a",
			"0 0 2 0.5 1 a",
			"1 0 3 1 1 a",
			"2 0 3 1 1 a",
			"3 0 2 1 1 a",
			"");

	private static final String STA = String.join("\n",
			"# States",
			"(x,h)",
			"0:(0,0)",
			"1:(0,1)",
			"2:(1,0)",
			"3:(1,1)",
			"");

	private static final String OBS = String.join("\n",
			"# Observations",
			"(x)",
			"0:(0)",
			"1:(1)",
			"");

	// As OBS, but with the observable named h, so that h is (wrongly) taken to
	// be observable and x not: then states 0 and 1 have the same observation (0)
	// and the same unobservation (x=0), so states are not identified uniquely
	private static final String OBS_COLLIDING = OBS.replace("(x)", "(h)");

	@TempDir
	Path tempDir;

	@Test
	void explicitImportUsesNonObservableVariables() throws Exception
	{
		POMDP<Double> pomdp = importExplicit(OBS);
		checkCompactUnobservations(pomdp);
	}

	@Test
	void umbImportUsesNonObservableVariables() throws Exception
	{
		POMDP<Double> pomdp = importUMB(importExplicit(OBS), OBS);
		checkCompactUnobservations(pomdp);
	}

	@Test
	void explicitImportFallsBackOnCollision() throws Exception
	{
		POMDP<Double> pomdp = importExplicit(OBS_COLLIDING);
		checkStateUnobservations(pomdp);
	}

	@Test
	void umbImportFallsBackOnCollision() throws Exception
	{
		POMDP<Double> pomdp = importUMB(importExplicit(OBS_COLLIDING), OBS_COLLIDING);
		checkStateUnobservations(pomdp);
	}

	/**
	 * Check that unobservations are the values of h (rather than one per state).
	 */
	private static void checkCompactUnobservations(POMDP<Double> pomdp)
	{
		assertEquals(4, pomdp.getNumStates());
		assertEquals(2, pomdp.getNumObservations());
		assertEquals(2, pomdp.getNumUnobservations());
		List<State> statesList = pomdp.getStatesList();
		List<State> unobsList = pomdp.getUnobservationsList();
		for (int s = 0; s < pomdp.getNumStates(); s++) {
			// Unobservation of a state is the value of h
			State unobs = unobsList.get(pomdp.getUnobservation(s));
			assertEquals(1, unobs.varValues.length);
			assertEquals(statesList.get(s).varValues[1], unobs.varValues[0]);
		}
		checkUniqueAndBeliefsPreserved(pomdp);
	}

	/**
	 * Check that each state is its own unobservation.
	 */
	private static void checkStateUnobservations(POMDP<Double> pomdp)
	{
		assertEquals(4, pomdp.getNumStates());
		assertEquals(4, pomdp.getNumUnobservations());
		List<State> statesList = pomdp.getStatesList();
		List<State> unobsList = pomdp.getUnobservationsList();
		for (int s = 0; s < pomdp.getNumStates(); s++) {
			assertEquals(s, pomdp.getUnobservation(s));
			assertEquals(statesList.get(s), unobsList.get(s));
		}
		checkUniqueAndBeliefsPreserved(pomdp);
	}

	/**
	 * Check that (observation, unobservation) pairs identify states uniquely,
	 * and that converting a point belief for each state back to a distribution
	 * over states gives that state.
	 */
	private static void checkUniqueAndBeliefsPreserved(POMDP<Double> pomdp)
	{
		int numStates = pomdp.getNumStates();
		Set<List<Integer>> pairs = new HashSet<>();
		for (int s = 0; s < numStates; s++) {
			assertTrue(pairs.add(List.of(pomdp.getObservation(s), pomdp.getUnobservation(s))), "Duplicate (observation, unobservation) for state " + s);
			double[] dist = Belief.pointDistribution(s, pomdp).toDistributionOverStates(pomdp);
			for (int s2 = 0; s2 < numStates; s2++) {
				assertEquals(s2 == s ? 1.0 : 0.0, dist[s2], "Belief for state " + s + " has probability " + dist[s2] + " for state " + s2);
			}
		}
	}

	/**
	 * Import the POMDP from explicit files, using the specified observations file contents.
	 */
	private POMDP<Double> importExplicit(String obs) throws IOException, PrismException
	{
		PrismExplicitImporter importer = new PrismExplicitImporter((ModelType) null);
		importer.setTransFile(writeFile("model.tra", TRA));
		importer.setStatesFile(writeFile("model.sta", STA));
		importer.setObservationsFile(writeFile("model.obs", obs));
		return build(importer);
	}

	/**
	 * Export the POMDP to a UMB file and import it again
	 * (the observations file contents used to import the POMDP are needed
	 * to provide the model info for export).
	 */
	private POMDP<Double> importUMB(POMDP<Double> pomdp, String obs) throws IOException, PrismException
	{
		PrismExplicitImporter infoImporter = new PrismExplicitImporter((ModelType) null);
		infoImporter.setTransFile(writeFile("info.tra", TRA));
		infoImporter.setStatesFile(writeFile("info.sta", STA));
		infoImporter.setObservationsFile(writeFile("info.obs", obs));
		UMBExporter<Double> exporter = new UMBExporter<>();
		exporter.setModelInfo(infoImporter.getModelInfo());
		File umbFile = tempDir.resolve("model.umb").toFile();
		exporter.exportModel(pomdp, umbFile);
		return build(new UMBImporter(umbFile));
	}

	@SuppressWarnings("unchecked")
	private static POMDP<Double> build(ExplicitModelImporter importer) throws PrismException
	{
		PrismComponent parent = new PrismComponent();
		parent.setLog(new PrismDevNullLog());
		Model<Double> model = new ExplicitFiles2Model(parent).build(importer, Evaluator.forDouble());
		assertTrue(model instanceof POMDP, "Imported model is not a POMDP");
		return (POMDP<Double>) model;
	}

	private File writeFile(String name, String contents) throws IOException
	{
		Path path = tempDir.resolve(name);
		Files.writeString(path, contents);
		return path.toFile();
	}
}
