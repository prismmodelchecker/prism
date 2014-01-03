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

import java.util.*;
import javax.swing.table.AbstractTableModel;

import simulator.PathFullInfo;
import userinterface.simulator.SimulationView.*;
import userinterface.util.GUIGroupedTableModel;
import parser.ast.*;

public class GUISimulatorPathTableModel extends AbstractTableModel implements GUIGroupedTableModel, Observer
{
	private static final long serialVersionUID = 1L;

	private GUISimulator simulator;
	private SimulationView view;

	private boolean pathActive;
	private ModulesFile parsedModel;
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

	public boolean canShowTime()
	{
		return parsedModel.getModelType().continuousTime();
	}

	public int getGroupCount()
	{
		if (!pathActive) {
			return 0;
		} else {
			int groupCount = 0;

			if (view.showActions() || view.showSteps()) {
				groupCount++;
			}

			if (canShowTime() && (view.showTime() || view.showCumulativeTime())) {
				groupCount++;
			}

			ArrayList<Variable> vars = view.getVisibleVariables();
			Set<String> varNames = new HashSet<String>();

			for (Variable variable : vars) {
				varNames.add(variable.getName());
			}

			for (int g = 0; g < parsedModel.getNumGlobals(); g++) {
				if (varNames.contains(parsedModel.getGlobal(g).getName())) {
					groupCount++;
					break;
				}
			}

			for (int m = 0; m < parsedModel.getNumModules(); m++) {
				Module module = parsedModel.getModule(m);
				for (int v = 0; v < module.getNumDeclarations(); v++) {
					if (varNames.contains(module.getDeclaration(v).getName())) {
						groupCount++;
						break;
					}
				}
			}

			if (view.getVisibleRewardColumns().size() > 0) {
				groupCount++;
			}

			return groupCount;
		}
	}

	public void update(Observable o, Object arg)
	{
		if (o == view) {
			fireTableStructureChanged();

			//Sort out the minimum widths for each column
			simulator.sortOutColumnSizes();
		}
	}

	public String getGroupName(int groupIndex)
	{
		if (!pathActive) {
			return "";
		} else {
			int groupCount = 0;

			if (view.showActions() || view.showSteps()) {
				if (groupCount == groupIndex) {
					return "Step";
				}

				groupCount++;
			}

			if (canShowTime() && (view.showTime() || view.showCumulativeTime())) {
				if (groupCount == groupIndex) {
					return "Time";
				}

				groupCount++;
			}

			if (view.getVisibleVariables().size() > 0) {
				ArrayList<Variable> vars = view.getVisibleVariables();
				Set<String> varNames = new HashSet<String>();

				for (Variable variable : vars) {
					varNames.add(variable.getName());
				}

				for (int g = 0; g < parsedModel.getNumGlobals(); g++) {
					if (varNames.contains(parsedModel.getGlobal(g).getName())) {
						if (groupCount == groupIndex) {
							return "Globals";
						}

						groupCount++;
						break;
					}
				}

				for (int m = 0; m < parsedModel.getNumModules(); m++) {
					Module module = parsedModel.getModule(m);
					for (int v = 0; v < module.getNumDeclarations(); v++) {
						if (varNames.contains(module.getDeclaration(v).getName())) {
							if (groupCount == groupIndex) {
								return "" + parsedModel.getModuleName(m) + "";
							}

							groupCount++;
							break;
						}
					}
				}
			}

			// Add state and transitions rewards for each reward structure.
			if (view.getVisibleRewardColumns().size() > 0) {
				if (groupCount == groupIndex) {
					return "Rewards";
				}

				groupCount++;
			}

			return "Undefined Group";
		}
	}

	public String getGroupToolTip(int groupIndex)
	{
		ArrayList<Variable> vars = view.getVisibleVariables();
		Set<String> varNames = new HashSet<String>();

		for (Variable variable : vars) {
			varNames.add(variable.getName());
		}

		int groupCount = 0;

		if (view.showActions() || view.showSteps()) {
			if (groupCount == groupIndex) {
				return null;
			}

			groupCount++;
		}

		if (canShowTime() && (view.showTime() || view.showCumulativeTime())) {
			if (groupCount == groupIndex) {
				return null;
			}

			groupCount++;
		}

		for (int g = 0; g < parsedModel.getNumGlobals(); g++) {
			if (varNames.contains(parsedModel.getGlobal(g).getName())) {
				if (groupCount == groupIndex) {
					return "Global variables";
				}

				groupCount++;
				break;
			}
		}

		for (int m = 0; m < parsedModel.getNumModules(); m++) {
			Module module = parsedModel.getModule(m);
			for (int v = 0; v < module.getNumDeclarations(); v++) {
				if (varNames.contains(module.getDeclaration(v).getName())) {
					if (groupCount == groupIndex) {
						return "Variables of module \"" + parsedModel.getModuleName(m) + "\"";
					}

					groupCount++;
					break;
				}
			}
		}

		// Add state and transitions rewards for each reward structure.
		if (view.getVisibleRewardColumns().size() > 0) {
			if (groupCount == groupIndex) {
				return "State, transition and cumulative rewards";
			}

			groupCount++;
		}

		return null;
	}

	public int getLastColumnOfGroup(int groupIndex)
	{
		int stepStart = 0;
		int timeStart = stepStart + (view.showActions() ? 1 : 0) + (view.showSteps() ? 1 : 0);
		int varStart = timeStart + (canShowTime() && view.showCumulativeTime() ? 1 : 0) + (canShowTime() && view.showTime() ? 1 : 0);
		int rewardStart = varStart + view.getVisibleVariables().size();

		int groupCount = 0;

		if (view.showActions() || view.showSteps()) {
			if (groupCount == groupIndex) {
				if (view.showActions() && view.showSteps())
					return stepStart + 1;
				else
					return stepStart;
			}

			groupCount++;
		}

		if (canShowTime() && (view.showCumulativeTime() || view.showTime())) {
			if (groupCount == groupIndex) {
				if (view.showCumulativeTime() && view.showTime())
					return timeStart + 1;
				else
					return timeStart;
			}

			groupCount++;
		}

		if (view.getVisibleVariables().size() > 0) {
			int visVarCount = 0;

			ArrayList<Variable> vars = view.getVisibleVariables();
			Set<String> varNames = new HashSet<String>();

			for (Variable variable : vars) {
				varNames.add(variable.getName());
			}

			boolean atLeastOneGlobal = false;

			for (int g = 0; g < parsedModel.getNumGlobals(); g++) {
				boolean contained = varNames.contains(parsedModel.getGlobal(g).getName());

				if (!atLeastOneGlobal && contained) {
					atLeastOneGlobal = true;
				}

				if (contained)
					visVarCount++;
			}

			if (atLeastOneGlobal && groupCount == groupIndex) {
				return varStart + visVarCount - 1;
			}

			if (atLeastOneGlobal) {
				groupCount++;
			}

			for (int m = 0; m < parsedModel.getNumModules(); m++) {
				Module module = parsedModel.getModule(m);
				boolean atLeastOne = false;

				for (int v = 0; v < module.getNumDeclarations(); v++) {
					boolean contained = varNames.contains(module.getDeclaration(v).getName());
					if (!atLeastOne && contained) {
						atLeastOne = true;
					}

					if (contained)
						visVarCount++;
				}

				if (atLeastOne && groupCount == groupIndex) {
					return varStart + visVarCount - 1;
				}

				if (atLeastOne) {
					groupCount++;
				}
			}
		}

		// Add state and transitions rewards for each reward structure.
		if (view.getVisibleRewardColumns().size() > 0) {
			if (groupCount == groupIndex) {
				return rewardStart + view.getVisibleRewardColumns().size() - 1;
			}

			groupCount++;
		}

		return 0;
	}

	/**
	 * Returns the number of columns.
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount()
	{
		if (!pathActive) {
			return 0;
		} else {
			int colCount = 0;

			colCount += (view.showActions() ? 1 : 0);
			colCount += (view.showSteps() ? 1 : 0);
			colCount += (canShowTime() && view.showCumulativeTime() ? 1 : 0) + (canShowTime() && view.showTime() ? 1 : 0);
			colCount += view.getVisibleVariables().size();
			colCount += view.getVisibleRewardColumns().size();

			return colCount;
		}
	}

	/**
	 * Returns the number of rows.
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
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

	public String getColumnName(int columnIndex)
	{
		if (pathActive) {
			int actionStart = 0;
			int stepStart = actionStart + (view.showActions() ? 1 : 0);
			int cumulativeTimeStart = stepStart + (view.showSteps() ? 1 : 0);
			int timeStart = cumulativeTimeStart + (canShowTime() && view.showCumulativeTime() ? 1 : 0);
			int varStart = timeStart + (canShowTime() && view.showTime() ? 1 : 0);
			int rewardStart = varStart + view.getVisibleVariables().size();

			// The step column
			if (actionStart <= columnIndex && columnIndex < stepStart) {
				return "Action";
			} else if (stepStart <= columnIndex && columnIndex < cumulativeTimeStart) {
				return "#";
			} else if (cumulativeTimeStart <= columnIndex && columnIndex < timeStart) {
				return "Time (+)";
			} else if (timeStart <= columnIndex && columnIndex < varStart) {
				return "Time";
			}
			// A variable column
			else if (varStart <= columnIndex && columnIndex < rewardStart) {
				return ((Variable) view.getVisibleVariables().get(columnIndex - varStart)).toString();
			}

			else if (rewardStart <= columnIndex) {
				return ((RewardStructureColumn) view.getVisibleRewardColumns().get(columnIndex - rewardStart)).getColumnName();
			}
		}
		return "Undefined Column";
	}

	public String getColumnToolTip(int columnIndex)
	{
		if (pathActive) {
			int actionStart = 0;
			int stepStart = actionStart + (view.showActions() ? 1 : 0);
			int cumulativeTimeStart = stepStart + (view.showSteps() ? 1 : 0);
			int timeStart = cumulativeTimeStart + (canShowTime() && view.showCumulativeTime() ? 1 : 0);
			int varStart = timeStart + (canShowTime() && view.showTime() ? 1 : 0);
			int rewardStart = varStart + view.getVisibleVariables().size();

			// The step column
			if (actionStart <= columnIndex && columnIndex < stepStart) {
				return "Module name or [action] label";
			} else if (stepStart <= columnIndex && columnIndex < cumulativeTimeStart) {
				return "Index of state in path";
			} else if (cumulativeTimeStart <= columnIndex && columnIndex < timeStart) {
				return "Cumulative time";
			} else if (timeStart <= columnIndex && columnIndex < varStart) {
				return "Time spent in state";
			}
			// A variable column
			else if (varStart <= columnIndex && columnIndex < rewardStart) {
				return "Values of variable \"" + ((Variable) view.getVisibleVariables().get(columnIndex - varStart)).toString() + "\"";
			}

			else if (rewardStart <= columnIndex) {
				RewardStructureColumn column = ((RewardStructureColumn) view.getVisibleRewardColumns().get(columnIndex - rewardStart));
				String rewardName = column.getRewardStructure().getColumnName();

				if (column.isStateReward())
					return "State reward of reward structure " + rewardName;
				if (column.isTransitionReward())
					return "Transition reward of reward structure " + rewardName;
				if (column.isCumulativeReward())
					return "Cumulative reward of reward structure " + rewardName;
			}
		}
		return "Undefined Column";
	}

	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (pathActive) {
			int actionStart = 0;
			int stepStart = actionStart + (view.showActions() ? 1 : 0);
			int cumulativeTimeStart = stepStart + (view.showSteps() ? 1 : 0);
			int timeStart = cumulativeTimeStart + (canShowTime() && view.showCumulativeTime() ? 1 : 0);
			int varStart = timeStart + (canShowTime() && view.showTime() ? 1 : 0);
			int rewardStart = varStart + view.getVisibleVariables().size();

			// The action column
			if (actionStart <= columnIndex && columnIndex < stepStart) {
				actionValue = view.new ActionValue(rowIndex == 0 ? "" : path.getModuleOrAction(rowIndex - 1));
				actionValue.setActionValueUnknown(false);
				return actionValue;
			}
			// The step column
			else if (stepStart <= columnIndex && columnIndex < cumulativeTimeStart) {
				return "" + rowIndex;
			}
			// Cumulative time column
			else if (cumulativeTimeStart <= columnIndex && columnIndex < timeStart) {
				timeValue = view.new TimeValue(path.getCumulativeTime(rowIndex), true);
				timeValue.setTimeValueUnknown(rowIndex > path.size()); // Never unknown
				return timeValue;
			}
			// Time column
			else if (timeStart <= columnIndex && columnIndex < varStart) {
				timeValue = view.new TimeValue(path.getTime(rowIndex), false);
				timeValue.setTimeValueUnknown(rowIndex >= path.size());
				return timeValue;
			}
			// A variable column
			else if (varStart <= columnIndex && columnIndex < rewardStart) {
				Variable var = view.getVisibleVariables().get(columnIndex - varStart);
				Object result = path.getState(rowIndex).varValues[var.getIndex()];
				variableValue.setVariable(var);
				variableValue.setValue(result);
				variableValue.setChanged(rowIndex == 0 || !path.getState(rowIndex - 1).varValues[var.getIndex()].equals(result));
				return variableValue;
			}
			// A reward column
			else if (rewardStart <= columnIndex) {
				RewardStructureColumn rewardColumn = (RewardStructureColumn) view.getVisibleRewardColumns().get(columnIndex - rewardStart);
				rewardStructureValue.setRewardStructureColumn(rewardColumn);
				rewardStructureValue.setRewardValueUnknown(false);
				// A state reward column
				if (rewardColumn.isStateReward()) {
					double value = path.getStateReward(rowIndex, rewardColumn.getRewardStructure().getIndex());
					rewardStructureValue.setChanged(rowIndex == 0
							|| value != path.getStateReward(rowIndex - 1, rewardColumn.getRewardStructure().getIndex()));
					rewardStructureValue.setRewardValue(new Double(value));
					rewardStructureValue.setRewardValueUnknown(rowIndex > path.size()); // Never unknown
				}
				// A transition reward column
				else if (rewardColumn.isTransitionReward()) {
					double value = path.getTransitionReward(rowIndex, rewardColumn.getRewardStructure().getIndex());
					rewardStructureValue.setChanged(rowIndex == 0
							|| value != path.getTransitionReward(rowIndex - 1, rewardColumn.getRewardStructure().getIndex()));
					rewardStructureValue.setRewardValue(new Double(value));
					rewardStructureValue.setRewardValueUnknown(rowIndex >= path.size());
				}
				// A cumulative reward column
				else {
					double value = path.getCumulativeReward(rowIndex, rewardColumn.getRewardStructure().getIndex());
					rewardStructureValue.setChanged(rowIndex == 0
							|| value != (path.getCumulativeReward(rowIndex - 1, rewardColumn.getRewardStructure().getIndex())));
					rewardStructureValue.setRewardValue(new Double(value));
					rewardStructureValue.setRewardValueUnknown(rowIndex > path.size()); // Never unknown
				}
				return rewardStructureValue;
			}
		}

		return "Undefined value";
	}

	/** 
	 * Method is called when a new path is created.
	 * The structure of the path may be for a different model etc.
	 */
	public void restartPathTable()
	{
		view.refreshToDefaultView(pathActive, parsedModel);
	}

	/** 
	 * Method is called whenever a path is modified.
	 */
	public void updatePathTable()
	{
		fireTableDataChanged();
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
