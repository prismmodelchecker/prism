//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package userinterface.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.table.AbstractTableModel;

import parser.ast.ModulesFile;
import prism.ModelInfo;
import simulator.PathFullInfo;
import userinterface.simulator.SimulationView.ActionValue;
import userinterface.simulator.SimulationView.Observ;
import userinterface.simulator.SimulationView.RewardStructureColumn;
import userinterface.simulator.SimulationView.RewardStructureValue;
import userinterface.simulator.SimulationView.TimeValue;
import userinterface.simulator.SimulationView.Variable;
import userinterface.simulator.SimulationView.VariableValue;
import userinterface.util.GUIGroupedTableModel;

public class GUISimulatorPathTableModel extends AbstractTableModel implements GUIGroupedTableModel, Observer
{
	private static final long serialVersionUID = 1L;

	enum PathTableModelGroupType {
		STEP, TIME, VARIABLES, OBSERVABLES, REWARDS
	};
	
	enum GUISimulatorPathTableModelColumn {
		ACTION, STEP, TIME_CUMUL, TIME, VARIABLE, OBSERVABLE, REWARD
	};
	
	class PathTableModelGroup {
		public PathTableModelGroupType type;
		public Object info;
		public int lastCol;
		public PathTableModelGroup(PathTableModelGroupType type, Object info, int lastCol)
		{
			this.type = type;
			this.info = info;
			this.lastCol = lastCol;
		}
	}
	
	class PathTableModelColumn {
		public GUISimulatorPathTableModelColumn type;
		public Object info;
		public PathTableModelColumn(GUISimulatorPathTableModelColumn type, Object info)
		{
			this.type = type;
			this.info = info;
		}
	}
	
	private GUISimulator simulator;
	private SimulationView view;
	private List<PathTableModelGroup> visibleGroups;
	private List<PathTableModelColumn> visibleColumns;

	private boolean pathActive;
	private ModulesFile parsedModel;
	private ModelInfo modelInfo;
	private PathFullInfo path; 

	private RewardStructureValue rewardStructureValue;
	private VariableValue variableValue;
	private TimeValue timeValue;
	private ActionValue actionValue;

	public GUISimulatorPathTableModel(GUISimulator simulator, SimulationView view)
	{
		this.simulator = simulator;
		this.view = view;
		this.view.addObserver(this);
		visibleGroups = new ArrayList<>();
		visibleColumns = new ArrayList<>();
		rewardStructureValue = view.new RewardStructureValue(null, null);
		variableValue = view.new VariableValue(null, null);
	}

	public void setPathActive(boolean pathActive)
	{
		this.pathActive = pathActive;
	}

	public void setPath(PathFullInfo path)
	{
		this.path = path;
	}

	public void setParsedModel(ModulesFile parsedModel)
	{
		this.parsedModel = parsedModel;
	}

	public void setModelInfo(ModelInfo modelInfo)
	{
		this.modelInfo = modelInfo;
	}

	public boolean canShowTime()
	{
		return modelInfo.getModelType().continuousTime();
	}

	@Override
	public int getGroupCount()
	{
		if (!pathActive) {
			return 0;
		} else {
			return visibleGroups.size();
		}
	}

	@Override
	public void update(Observable o, Object arg)
	{
		if (o == view) {
			setVisibleColumnsAndGroups();
			fireTableStructureChanged();

			//Sort out the minimum widths for each column
			simulator.sortOutColumnSizes();
		}
	}

	@Override
	public String getGroupName(int groupIndex)
	{
		if (!pathActive) {
			return "";
		} else {
			switch (visibleGroups.get(groupIndex).type) {
			case STEP:
				return "Step";
			case TIME:
				return "Time";
			case VARIABLES:
				int module = (Integer) visibleGroups.get(groupIndex).info;
				return module == -1 ? "Globals" : modelInfo.getModuleName(module);
			case OBSERVABLES:
				return "Observables";
			case REWARDS:
				return "Rewards";
			default:
				return "";
			}
		}
	}

	@Override
	public String getGroupToolTip(int groupIndex)
	{
		if (!pathActive) {
			return "";
		} else {
			switch (visibleGroups.get(groupIndex).type) {
			case STEP:
				return null;
			case TIME:
				return null;
			case VARIABLES:
				int module = (Integer) visibleGroups.get(groupIndex).info;
				return module == -1 ? "Global variables" : "Variables of module " + modelInfo.getModuleName(module);
			case OBSERVABLES:
				return null;
			case REWARDS:
				return "State, transition and cumulative rewards";
			default:
				return "";
			}
		}
	}

	@Override
	public int getLastColumnOfGroup(int groupIndex)
	{
		if (!pathActive) {
			return 0;
		} else {
			return visibleGroups.get(groupIndex).lastCol;
		}
	}

	@Override
	public int getColumnCount()
	{
		if (!pathActive) {
			return 0;
		} else {
			return visibleColumns.size();
		}
	}

	@Override
	public int getRowCount()
	{
		// Return current path size if there is an active path.
		return (pathActive ? (int) path.size() + 1 : 0);
	}

	public boolean shouldColourRow(int row)
	{
		int selection = simulator.getStateLabelList().getSelectedIndex();
		if (selection != -1) {
			GUISimLabelList.SimLabel label = (GUISimLabelList.SimLabel) simulator.getStateLabelList().getModel().getElementAt(selection);
			if (row == getRowCount() - 1) {
				if (label.getResult() == 1)
					return true;
			} else {
				if (label.getResult(row) == 1)
					return true;
			}
		}

		return false;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		if (pathActive) {
			switch (visibleColumns.get(columnIndex).type) {
			case ACTION:
				return modelInfo.getActionStringDescription();
			case STEP:
				return "#";
			case TIME_CUMUL:
				return "Time (+)";
			case TIME:
				return "Time";
			case VARIABLE:
				Variable var = (Variable) visibleColumns.get(columnIndex).info;
				return var.toString();
			case OBSERVABLE:
				Observ obs = (Observ) visibleColumns.get(columnIndex).info;
				return obs.toString();
			case REWARD:
				RewardStructureColumn rewardColumn = (RewardStructureColumn) visibleColumns.get(columnIndex).info;
				return rewardColumn.getColumnName();
			default:
				return "";
			}
		}
		return "";
	}

	@Override
	public String getColumnToolTip(int columnIndex)
	{
		if (pathActive) {
			switch (visibleColumns.get(columnIndex).type) {
			case ACTION:
				return "Module name or [action] label";
			case STEP:
				return "Index of state in path";
			case TIME_CUMUL:
				return "Cumulative time";
			case TIME:
				return "Time spent in state";
			case VARIABLE:
				Variable var = (Variable) visibleColumns.get(columnIndex).info;
				return "Value of variable " + var.toString();
			case OBSERVABLE:
				Observ obs = (Observ) visibleColumns.get(columnIndex).info;
				return "Value of observable " + obs.toString();
			case REWARD:
				RewardStructureColumn rewardColumn = (RewardStructureColumn) visibleColumns.get(columnIndex).info;
				String rewardName = rewardColumn.getRewardStructure().getColumnName();
				if (rewardColumn.isStateReward()) {
					return "State reward of reward structure " + rewardName;
				}
				if (rewardColumn.isTransitionReward()) {
					return "Transition reward of reward structure " + rewardName;
				}
				if (rewardColumn.isCumulativeReward()) {
					return "Cumulative reward of reward structure " + rewardName;
				}
			default:
				return "";
			}
		}
		return "";
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (pathActive) {
			switch (visibleColumns.get(columnIndex).type) {
			case ACTION:
				// The action column
				actionValue = view.new ActionValue(rowIndex == 0 ? "" : path.getActionString(rowIndex - 1));
				actionValue.setActionValueUnknown(false);
				return actionValue;
			case STEP:
				// The step column
				return "" + rowIndex;
			case TIME_CUMUL:
				// Cumulative time column
				timeValue = view.new TimeValue(path.getCumulativeTime(rowIndex), true);
				timeValue.setTimeValueUnknown(rowIndex > path.size()); // Never unknown
				return timeValue;
			case TIME:
				// Time column
				timeValue = view.new TimeValue(path.getTime(rowIndex), false);
				timeValue.setTimeValueUnknown(rowIndex >= path.size());
				return timeValue;
			case VARIABLE:
				// A variable column
				Variable var = (Variable) visibleColumns.get(columnIndex).info;
				Object result = path.getState(rowIndex).varValues[var.getIndex()];
				variableValue.setVariable(var);
				variableValue.setValue(result);
				variableValue.setChanged(rowIndex == 0 || !path.getState(rowIndex - 1).varValues[var.getIndex()].equals(result));
				return variableValue;
			case OBSERVABLE:
				// An observable column
				Observ obs = (Observ) visibleColumns.get(columnIndex).info;
				Object resultO = path.getObservation(rowIndex).varValues[obs.getIndex()];
				variableValue.setVariable(obs);
				variableValue.setValue(resultO);
				variableValue.setChanged(rowIndex == 0 || !path.getObservation(rowIndex - 1).varValues[obs.getIndex()].equals(resultO));
				return variableValue;
			case REWARD:
				// A reward column
				RewardStructureColumn rewardColumn = (RewardStructureColumn) visibleColumns.get(columnIndex).info;
				rewardStructureValue.setRewardStructureColumn(rewardColumn);
				rewardStructureValue.setRewardValueUnknown(false);
				// A state reward column
				if (rewardColumn.isStateReward()) {
					double value = path.getStateReward(rowIndex, rewardColumn.getRewardStructure().getIndex());
					rewardStructureValue.setChanged(rowIndex == 0 || value != path.getStateReward(rowIndex - 1, rewardColumn.getRewardStructure().getIndex()));
					rewardStructureValue.setRewardValue(value);
					rewardStructureValue.setRewardValueUnknown(rowIndex > path.size()); // Never unknown
				}
				// A transition reward column
				else if (rewardColumn.isTransitionReward()) {
					double value = path.getTransitionReward(rowIndex, rewardColumn.getRewardStructure().getIndex());
					rewardStructureValue.setChanged(rowIndex == 0 || value != path.getTransitionReward(rowIndex - 1, rewardColumn.getRewardStructure().getIndex()));
					rewardStructureValue.setRewardValue(value);
					rewardStructureValue.setRewardValueUnknown(rowIndex >= path.size());
				}
				// A cumulative reward column
				else {
					double value = path.getCumulativeReward(rowIndex, rewardColumn.getRewardStructure().getIndex());
					rewardStructureValue.setChanged(rowIndex == 0 || value != (path.getCumulativeReward(rowIndex - 1, rewardColumn.getRewardStructure().getIndex())));
					rewardStructureValue.setRewardValue(value);
					rewardStructureValue.setRewardValueUnknown(rowIndex > path.size()); // Never unknown
				}
				return rewardStructureValue;
			default:
				return "";
			}
		}
		return "";
	}

	/** 
	 * Method is called when a new path is created.
	 * The structure of the path may be for a different model etc.
	 */
	public void restartPathTable()
	{
		view.refreshToDefaultView(pathActive, parsedModel);
		// NB: since we observe view, the above will trigger update(),
		// which calls setVisibleColumns() etc.
	}

	/** 
	 * Method is called whenever a path is modified.
	 */
	public void updatePathTable()
	{
		setVisibleColumnsAndGroups();
		fireTableDataChanged();
	}

	/**
	 * Set up the info about table columns/groups
	 */
	public void setVisibleColumnsAndGroups()
	{
		visibleColumns.clear();
		visibleGroups.clear();
		if (pathActive) {
			// Step
			if (view.showActions() || view.showSteps()) {
				if (view.showActions()) {
					visibleColumns.add(new PathTableModelColumn(GUISimulatorPathTableModelColumn.ACTION, null));
				}
				if (view.showSteps()) {
					visibleColumns.add(new PathTableModelColumn(GUISimulatorPathTableModelColumn.STEP, null));
				}
				visibleGroups.add(new PathTableModelGroup(PathTableModelGroupType.STEP, null, visibleColumns.size() - 1));
			}
			// Time
			if (canShowTime() && (view.showTime() || view.showCumulativeTime())) {
				if (view.showCumulativeTime()) {
					visibleColumns.add(new PathTableModelColumn(GUISimulatorPathTableModelColumn.TIME_CUMUL, null));
				}
				if (view.showTime()) {
					visibleColumns.add(new PathTableModelColumn(GUISimulatorPathTableModelColumn.TIME, null));
				}
				visibleGroups.add(new PathTableModelGroup(PathTableModelGroupType.TIME, null, visibleColumns.size() - 1));
			}
			// Variables
			if (view.getVisibleVariables().size() > 0) {
				int numVars = view.getVisibleVariables().size();
				for (int i = 0; i < numVars; i++) {
					Variable v = view.getVisibleVariables().get(i);
					visibleColumns.add(new PathTableModelColumn(GUISimulatorPathTableModelColumn.VARIABLE, v));
					// If module changes between vars (or this is last var), put these ones in a column group
					if ((i == numVars - 1) || (v.getModuleIndex() != view.getVisibleVariables().get(i + 1).getModuleIndex())) {
						visibleGroups.add(new PathTableModelGroup(PathTableModelGroupType.VARIABLES, v.getModuleIndex(), visibleColumns.size() - 1));
					}
				}
			}
			// Variables
			if (view.getVisibleObservables().size() > 0) {
				for (Observ o : view.getVisibleObservables()) {
					visibleColumns.add(new PathTableModelColumn(GUISimulatorPathTableModelColumn.OBSERVABLE, o));
				}
				visibleGroups.add(new PathTableModelGroup(PathTableModelGroupType.OBSERVABLES, null, visibleColumns.size() - 1));
			}
			// Rewards
			if (view.getVisibleRewardColumns().size() > 0) {
				for (RewardStructureColumn rsc : view.getVisibleRewardColumns()) {
					visibleColumns.add(new PathTableModelColumn(GUISimulatorPathTableModelColumn.REWARD, rsc));
				}
				visibleGroups.add(new PathTableModelGroup(PathTableModelGroupType.REWARDS, null, visibleColumns.size() - 1));
			}
		}
	}
	
	public boolean isPathLooping()
	{
		return path.isLooping();
	}

	public int getLoopStart()
	{
		return (int) path.loopStart();
	}

	public int getLoopEnd()
	{
		return (int) path.loopEnd();
	}

	public SimulationView getView()
	{
		return view;
	}

	public void setView(SimulationView view)
	{
		this.view.deleteObserver(this);
		this.view = view;
		this.view.addObserver(this);
	}
}
