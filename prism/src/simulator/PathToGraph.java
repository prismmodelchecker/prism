//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package simulator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.jfree.data.xy.XYDataItem;

import parser.State;
import parser.ast.ModulesFile;
import parser.type.TypeDouble;
import prism.PrismException;
import userinterface.graph.Graph;
import userinterface.graph.Graph.SeriesKey;

/**
 * Class to display a simulation path in text form, sending to a PrismLog.
 */
public class PathToGraph extends PathDisplayer
{
	/** Graph on which to plot path */
	private Graph graphModel = null;
	private List<SeriesKey> varSeriesKeys = null;
	private List<SeriesKey> rewardSeriesKeys = null;

	// Model info
	private ModulesFile modulesFile;
	private int numVars;
	private int numRewardStructs;

	// Displayer state
	private double lastTime;
	private State lastState;
	private double[] lastStateRewards;
	/** For each variable/reward, whether we skipped plotting a point last time */
	private BitSet skippedVars = new BitSet();
	private BitSet skippedRewards = new BitSet();

	/**
	 * Construct a {@link PathToGraph} object
	 * @param graphModel Graph on which to plot path
	 * @param modulesFile Model associated with path
	 */
	public PathToGraph(Graph graphModel, ModulesFile modulesFile)
	{
		this.graphModel = graphModel;
		this.modulesFile = modulesFile;

		// Get model info
		numVars = modulesFile.getNumVars();
		numRewardStructs = modulesFile.getNumRewardStructs();
	}

	// Display methods

	@Override
	public void startDisplay(State initialState, double[] initialStateRewards)
	{
		// Configure axes
		graphModel.getXAxisSettings().setHeading("Time");
		graphModel.getYAxisSettings().setHeading("Value");

		// Create series
		varSeriesKeys = new ArrayList<Graph.SeriesKey>();
		if (varsToShow == null) {
			for (int j = 0; j < numVars; j++) {
				varSeriesKeys.add(graphModel.addSeries(modulesFile.getVarName(j)));
			}
		} else {
			for (int j = 0; j < numVars; j++) {
				if (varsToShow != null && varsToShow.contains(j))
					varSeriesKeys.add(graphModel.addSeries(modulesFile.getVarName(j)));
				else
					varSeriesKeys.add(null);
			}
		}
		if (showRewards) {
			rewardSeriesKeys = new ArrayList<Graph.SeriesKey>();
			for (int j = 0; j < numRewardStructs; j++) {
				rewardSeriesKeys.add(graphModel.addSeries(modulesFile.getRewardStruct(j).getName()));
			}
		}

		// Display initial state
		lastState = new State(initialState.varValues.length);
		if (showRewards)
			lastStateRewards = explicit.Utils.cloneDoubleArray(initialStateRewards);
		displayState(0.0, initialState, initialStateRewards, true);
	}

	@Override
	public void displayStep(double timeSpent, double timeCumul, Object action, double probability, double[] transitionRewards, long newStateIndex, State newState, double[] newStateRewards)
	{
		displayState(timeCumul, newState, newStateRewards, !showChangesOnly);
	}

	@Override
	public void displaySnapshot(double timeCumul, long newStateIndex, State newState, double[] newStateRewards)
	{
		displayState(timeCumul, newState, newStateRewards, !showChangesOnly);
	}

	private void displayState(double time, State state, double[] stateRewards, boolean force)
	{
		if (varsToShow == null) {
			for (int j = 0; j < numVars; j++) {
				Object val = state.varValues[j];
				boolean plot = force || !val.equals(lastState.varValues[j]);
				if (plot) {
					if (skippedVars.get(j))
						addPoint(varSeriesKeys.get(j), lastTime, lastState.varValues[j]);
					addPoint(varSeriesKeys.get(j), time, val);
				}
				skippedVars.set(j, !plot);
			}
		} else {
			for (int j : varsToShow) {
				Object val = state.varValues[j];
				boolean plot = force || !val.equals(lastState.varValues[j]);
				if (plot) {
					if (skippedVars.get(j))
						addPoint(varSeriesKeys.get(j), lastTime, lastState.varValues[j]);
					addPoint(varSeriesKeys.get(j), time, val);
				}
				skippedVars.set(j, !plot);
			}
		}
		if (showRewards) {
			for (int j = 0; j < numRewardStructs; j++) {
				double d = stateRewards[j];
				boolean plot = force || lastStateRewards[j] != stateRewards[j];
				if (plot) {
					if (skippedRewards.get(j))
						graphModel.addPointToSeries(rewardSeriesKeys.get(j), new XYDataItem(lastTime, lastStateRewards[j]));
					graphModel.addPointToSeries(rewardSeriesKeys.get(j), new XYDataItem(time, d));
				}
				skippedRewards.set(j, !plot);
			}
		}
		lastTime = time;
		lastState.copy(state);
		if (showRewards) {
			explicit.Utils.copyDoubleArray(stateRewards, lastStateRewards);
		}
	}

	/**
	 * Add a point to a given graph series. X-value is {@code x}.
	 * Y-value is {@code value}, which will mapped to a double as appropriate.
	 */
	private void addPoint(SeriesKey seriesKey, double x, Object val)
	{
		double d;
		try {
			d = TypeDouble.getInstance().castValueTo(val).doubleValue();
			graphModel.addPointToSeries(seriesKey, new XYDataItem(x, d));
		} catch (PrismException e) {
			if (val instanceof Boolean) {
				d = ((Boolean) val).booleanValue() ? 1.0 : 0.0;
				graphModel.addPointToSeries(seriesKey, new XYDataItem(x, d));
			}
		}
	}
	
	@Override
	public void endDisplay()
	{
		// Always display last points to ensure complete plot lines
		// (it's OK to overwrite points)
		displayState(lastTime, lastState, lastStateRewards, true);
	}
}
