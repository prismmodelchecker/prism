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

import parser.ast.*;
import parser.type.Type;
import prism.PrismSettings;
import userinterface.simulator.GUIViewDialog.RewardListItem;

/**
 * Class storing information about the view configuration for a path in the simulator.
 */
public class SimulationView extends Observable
{
	private GUISimulator simulator;

	private ArrayList<Variable> visibleVariables;
	private ArrayList<Variable> hiddenVariables;

	private ArrayList<RewardStructureColumn> visibleRewardColumns;
	private ArrayList<RewardStructure> rewards;

	private boolean stepsVisible;
	private boolean actionsVisible;
	private boolean showTime;
	private boolean showCumulativeTime;
	private boolean useChangeRenderer;

	public SimulationView(GUISimulator simulator, PrismSettings settings)
	{
		this.simulator = simulator;

		this.visibleVariables = new ArrayList<Variable>();
		this.hiddenVariables = new ArrayList<Variable>();

		this.visibleRewardColumns = new ArrayList<RewardStructureColumn>();
		this.rewards = new ArrayList<RewardStructure>();

		this.stepsVisible = true;
		this.actionsVisible = true;
		this.showTime = false;
		this.showCumulativeTime = true;

		useChangeRenderer = (settings.getInteger(PrismSettings.SIMULATOR_RENDER_ALL_VALUES) == 0);

	}

	public boolean showSteps()
	{
		return stepsVisible;
	}

	public void showSteps(boolean stepsVisible)
	{
		this.stepsVisible = stepsVisible;

		this.setChanged();
		this.notifyObservers();
	}

	public boolean showActions()
	{
		return actionsVisible;
	}

	public void showActions(boolean actionsVisible)
	{
		this.actionsVisible = actionsVisible;

		this.setChanged();
		this.notifyObservers();
	}

	public boolean showTime()
	{
		return showTime;
	}

	public boolean showCumulativeTime()
	{
		return showCumulativeTime;
	}

	public void showTime(boolean showTime)
	{
		this.showTime = showTime;

		this.setChanged();
		this.notifyObservers();
	}

	public void showCumulativeTime(boolean showCumulativeTime)
	{
		this.showCumulativeTime = showCumulativeTime;

		this.setChanged();
		this.notifyObservers();
	}

	public ArrayList<Variable> getVisibleVariables()
	{
		return visibleVariables;
	}

	public ArrayList<Variable> getHiddenVariables()
	{
		return hiddenVariables;
	}

	public void setVariableVisibility(ArrayList<Variable> visibleVariables, ArrayList<Variable> hiddenVariables)
	{
		this.visibleVariables = visibleVariables;
		this.hiddenVariables = hiddenVariables;

		this.setChanged();
		this.notifyObservers();
	}

	public ArrayList<RewardStructureColumn> getVisibleRewardColumns()
	{
		return visibleRewardColumns;
	}

	public void setVisibleRewardListItems(ArrayList<RewardListItem> visibleRewardListItems)
	{
		ArrayList<RewardStructureColumn> visibleRewardColumns = new ArrayList<RewardStructureColumn>();

		for (RewardListItem item : visibleRewardListItems) {
			if (item.isCumulative())
				visibleRewardColumns.add(new RewardStructureColumn(item.getRewardStructure(), RewardStructureColumn.CUMULATIVE_REWARD));
			else {
				if (!item.getRewardStructure().isStateEmpty())
					visibleRewardColumns.add(new RewardStructureColumn(item.getRewardStructure(), RewardStructureColumn.STATE_REWARD));
				if (!item.getRewardStructure().isTransitionEmpty())
					visibleRewardColumns.add(new RewardStructureColumn(item.getRewardStructure(), RewardStructureColumn.TRANSITION_REWARD));
			}
		}

		this.visibleRewardColumns = visibleRewardColumns;

		this.setChanged();
		this.notifyObservers();
	}

	public ArrayList<RewardStructure> getRewards()
	{
		return rewards;
	}

	public boolean isChangeRenderer()
	{
		return useChangeRenderer;
	}

	public void setRenderer(boolean isChangeRenderer)
	{
		if (useChangeRenderer != isChangeRenderer) {
			useChangeRenderer = isChangeRenderer;
			simulator.setRenderer(useChangeRenderer);
		}
	}

	public void refreshToDefaultView(boolean pathActive, ModulesFile parsedModel)
	{
		// First see if we can get away with using current settings...
		boolean canUseCurrentView = true;
		if (!pathActive) {
			canUseCurrentView = false;
		} else {
			if (useChangeRenderer != simulator.usingChangeRenderer()) {
				simulator.setRenderer(useChangeRenderer);
			}

			// Time-wise we have a problem.
			if (!parsedModel.getModelType().continuousTime() && (showTime || showCumulativeTime))
				canUseCurrentView = false;

			// Make a set of all variable names.
			TreeSet<String> allVarNames = new TreeSet<String>();

			for (Variable var : visibleVariables)
				allVarNames.add(var.getName());
			for (Variable var : hiddenVariables)
				allVarNames.add(var.getName());

			// Cannot use current view if a variable is not there.
			for (int g = 0; g < parsedModel.getNumGlobals(); g++) {
				if (!allVarNames.remove(parsedModel.getGlobal(g).getName()))
					canUseCurrentView = false;
			}
			for (int m = 0; m < parsedModel.getNumModules(); m++) {
				parser.ast.Module module = parsedModel.getModule(m);
				for (int v = 0; v < module.getNumDeclarations(); v++) {
					if (!allVarNames.remove(module.getDeclaration(v).getName()))
						canUseCurrentView = false;
				}
			}

			// Cannot use current view if we have too many variables.
			if (allVarNames.size() > 0)
				canUseCurrentView = false;

			// Make a list of all reward structures
			ArrayList<RewardStructure> allrew = new ArrayList<RewardStructure>();

			for (RewardStructure rew : rewards) {
				allrew.add(rew);
			}

			for (int r = 0; r < parsedModel.getNumRewardStructs(); r++) {
				RewardStruct rewardStruct = parsedModel.getRewardStruct(r);
				String rewardName = rewardStruct.getName();

				boolean hasStates = parsedModel.getRewardStruct(r).getNumStateItems() != 0;
				boolean hasTrans = parsedModel.getRewardStruct(r).getNumTransItems() != 0;

				boolean foundReward = false;

				for (RewardStructure rew : rewards) {
					if (rew.isStateEmpty() == !hasStates && rew.isTransitionEmpty() == !hasTrans
							&& ((rew.getName() == null && rewardName.equals("")) || (rew.getName() != null && rew.getName().equals(rewardName)))) {
						allrew.remove(rew);
						foundReward = true;
					}
				}

				if (!foundReward)
					canUseCurrentView = false;
			}

			if (allrew.size() > 0)
				canUseCurrentView = false;

		}

		if (!canUseCurrentView && pathActive) {
			visibleVariables.clear();
			hiddenVariables.clear();
			visibleRewardColumns.clear();

			rewards.clear();

			{
				int i = 0;
				for (int g = 0; g < parsedModel.getNumGlobals(); g++) {
					visibleVariables.add(new Variable(i, parsedModel.getGlobal(g).getName(), parsedModel.getGlobal(g).getType()));
					i++;
				}
				for (int m = 0; m < parsedModel.getNumModules(); m++) {
					parser.ast.Module module = parsedModel.getModule(m);
					for (int v = 0; v < module.getNumDeclarations(); v++) {
						visibleVariables.add(new Variable(i, module.getDeclaration(v).getName(), module.getDeclaration(v).getType()));
						i++;
					}
				}

				for (int r = 0; r < parsedModel.getNumRewardStructs(); r++) {
					RewardStruct rewardStruct = parsedModel.getRewardStruct(r);
					String rewardName = rewardStruct.getName();

					if (rewardName.trim().length() == 0) {
						rewardName = null;
					}

					RewardStructure rewardStructure = new RewardStructure(r, rewardName, parsedModel.getRewardStruct(r).getNumStateItems() == 0, parsedModel
							.getRewardStruct(r).getNumTransItems() == 0);

					if (!rewardStructure.isStateEmpty() || !rewardStructure.isTransitionEmpty())
						rewards.add(rewardStructure);

					if (!rewardStructure.isStateEmpty())
						visibleRewardColumns.add(new RewardStructureColumn(rewardStructure, RewardStructureColumn.STATE_REWARD));

					if (!rewardStructure.isTransitionEmpty())
						visibleRewardColumns.add(new RewardStructureColumn(rewardStructure, RewardStructureColumn.TRANSITION_REWARD));
				}

			}
		}
		this.setChanged();
		this.notifyObservers();

	}

	/**
	 * Represents a variable in the model.
	 */
	public class Variable
	{
		private int index;
		private String name;
		private Type type;

		public Variable(int index, String name, Type type)
		{
			this.index = index;
			this.name = name;
			this.type = type;
		}

		public int getIndex()
		{
			return index;
		}

		public String getName()
		{
			return name;
		}

		public Type getType()
		{
			return type;
		}

		public String toString()
		{
			return name;
		}

		public boolean equals(Object o)
		{
			return (o instanceof Variable && ((Variable) o).getIndex() == index);
		}
	}

	public class VariableValue
	{
		private Variable variable;
		private Object value;
		private boolean hasChanged;

		public VariableValue(Variable variable, Object value)
		{
			this.variable = variable;
			this.value = value;
			this.hasChanged = true;
		}

		public Object getValue()
		{
			return value;
		}

		public void setValue(Object value)
		{
			this.value = value;
		}

		public Variable getVariable()
		{
			return variable;
		}

		public void setVariable(Variable variable)
		{
			this.variable = variable;
		}

		public boolean hasChanged()
		{
			return hasChanged;
		}

		public void setChanged(boolean hasChanged)
		{
			this.hasChanged = hasChanged;
		}
	}

	public class ActionValue
	{
		private String value;
		private boolean actionValueUnknown;

		public ActionValue(String value)
		{
			this.value = value;
		}

		public String getValue()
		{
			return value;
		}

		public void setValue(String value)
		{
			this.value = value;
		}

		public void setActionValueUnknown(boolean unknown)
		{
			this.actionValueUnknown = unknown;
		}

		public boolean isActionValueUnknown()
		{
			return this.actionValueUnknown;
		}
	}

	public class TimeValue
	{
		private Double value;
		private boolean timeValueUnknown;
		private boolean isCumulative;

		public TimeValue(Double value, boolean isCumulative)
		{
			this.value = value;
			this.isCumulative = isCumulative;
		}

		public Double getValue()
		{
			return value;
		}

		public void setValue(Double value)
		{
			this.value = value;
		}

		public void setTimeValueUnknown(boolean unknown)
		{
			this.timeValueUnknown = unknown;
		}

		public boolean isTimeValueUnknown()
		{
			return this.timeValueUnknown;
		}

		public boolean isCumulative()
		{
			return isCumulative;
		}

		public void setCumulative(boolean isCumulative)
		{
			this.isCumulative = isCumulative;
		}
	}

	/**
	 * Represents a reward structure in the model.
	 */
	public class RewardStructure
	{
		private int index;
		private String name;

		private boolean stateEmpty;
		private boolean transitionEmpty;

		public RewardStructure(int index, String name, boolean stateEmpty, boolean transitionEmpty)
		{
			this.index = index;
			this.name = name;
			this.stateEmpty = stateEmpty;
			this.transitionEmpty = transitionEmpty;
		}

		public int getIndex()
		{
			return index;
		}

		public String getName()
		{
			return name;
		}

		public String getColumnName()
		{
			if (name == null) {
				return "" + (index + 1);
			} else {
				return "\"" + name + "\"";
			}
		}

		public boolean isStateEmpty()
		{
			return stateEmpty;
		}

		public boolean isTransitionEmpty()
		{
			return transitionEmpty;
		}

		public boolean isCumulative()
		{
			return false;
		}

		public String toString()
		{
			if (name != null) {
				return "" + (index + 1) + ": \"" + name + "\"";
			} else {
				return "" + (index + 1) + ": <unnamed>";
			}
		}

		public boolean equals(Object o)
		{
			return (o instanceof RewardStructure && ((RewardStructure) o).getIndex() == index && ((RewardStructure) o).isCumulative() == isCumulative());
		}
	}

	public class RewardStructureColumn
	{
		public static final int STATE_REWARD = 0;
		public static final int TRANSITION_REWARD = 1;
		public static final int CUMULATIVE_REWARD = 2;

		private RewardStructure rewardStructure;
		private int type;

		public RewardStructureColumn(RewardStructure rewardStructure, int type)
		{
			this.rewardStructure = rewardStructure;
			this.type = type;
		}

		public String getColumnName()
		{
			switch (type) {
			case (STATE_REWARD):
				return rewardStructure.getColumnName();
			case (TRANSITION_REWARD):
				return "[ " + rewardStructure.getColumnName() + " ]";
			case (CUMULATIVE_REWARD):
				return rewardStructure.getColumnName() + " (+)";
			}
			return "";
		}

		public RewardStructure getRewardStructure()
		{
			return rewardStructure;
		}

		public void setRewardStructure(RewardStructure rewardStructure)
		{
			this.rewardStructure = rewardStructure;
		}

		public String toString()
		{
			return getColumnName();
		}

		public boolean isStateReward()
		{
			return this.type == RewardStructureColumn.STATE_REWARD;
		}

		public boolean isTransitionReward()
		{
			return this.type == RewardStructureColumn.TRANSITION_REWARD;
		}

		public boolean isCumulativeReward()
		{
			return this.type == RewardStructureColumn.CUMULATIVE_REWARD;
		}

		public void setStateReward()
		{
			this.type = RewardStructureColumn.STATE_REWARD;
		}

		public void setTransitionReward()
		{
			this.type = RewardStructureColumn.TRANSITION_REWARD;
		}

		public void setCumulativeReward()
		{
			this.type = RewardStructureColumn.CUMULATIVE_REWARD;
		}
	}

	public class RewardStructureValue
	{
		private RewardStructureColumn rewardStructureColumn;
		private Double rewardValue;
		private boolean hasChanged;

		private boolean rewardValueUnknown;

		public RewardStructureValue(RewardStructureColumn rewardStructureColumn, Double rewardValue)
		{
			this.rewardStructureColumn = rewardStructureColumn;
			this.rewardValue = rewardValue;
			this.hasChanged = true;

			this.rewardValueUnknown = false;
		}

		public RewardStructureColumn getRewardStructureColumn()
		{
			return rewardStructureColumn;
		}

		public void setRewardStructureColumn(RewardStructureColumn rewardStructureColumn)
		{
			this.rewardStructureColumn = rewardStructureColumn;
		}

		public Double getRewardValue()
		{
			return rewardValue;
		}

		public void setRewardValue(Double rewardValue)
		{
			this.rewardValue = rewardValue;
		}

		public void setRewardValueUnknown(boolean unknown)
		{
			this.rewardValueUnknown = unknown;
		}

		public boolean isRewardValueUnknown()
		{
			return this.rewardValueUnknown;
		}

		public boolean hasChanged()
		{
			return hasChanged;
		}

		public void setChanged(boolean hasChanged)
		{
			this.hasChanged = hasChanged;
		}
	}
}
