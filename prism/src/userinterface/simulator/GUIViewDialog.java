//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

import prism.*;

import javax.swing.*;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import userinterface.*;
import userinterface.simulator.SimulationView.Variable;
import userinterface.simulator.SimulationView.*;

public class GUIViewDialog extends JDialog implements KeyListener
{
	private static final long serialVersionUID = 1L;

	//ATTRIBUTES    
	private boolean askOption;

	private GUIPrism gui;

	private SimulationView view;

	private VariableListModel visibleVariableListModel;
	private VariableListModel hiddenVariableListModel;

	private RewardListModel visibleRewardListModel;
	private RewardListModel hiddenRewardListModel;

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JPanel allPanel;
	private javax.swing.JPanel bottomPanel;
	private javax.swing.JPanel boxPanel;
	private javax.swing.JPanel buttonPanel;
	private javax.swing.JButton cancelButton;
	private javax.swing.JPanel centerRewardColumn;
	private javax.swing.JPanel centerRewardPanel;
	private javax.swing.JPanel centerVariableColumn;
	private javax.swing.JPanel centerVariablePanel;
	private javax.swing.JRadioButton changeRenderingButton;
	private javax.swing.JLabel hiddenLabel;
	private javax.swing.JList hiddenRewardList;
	private javax.swing.JScrollPane hiddenRewardScrollList;
	private javax.swing.JList hiddenVariableList;
	private javax.swing.JScrollPane hiddenVariableScrollList;
	private javax.swing.JPanel innerPathStylePanel;
	private javax.swing.JPanel innerTimePanel;
	private javax.swing.JPanel leftRewardColumn;
	private javax.swing.JPanel leftRewardPanel;
	private javax.swing.JPanel leftVariableColumn;
	private javax.swing.JPanel leftVariablePanel;
	private javax.swing.JButton makeRewardHiddenButton;
	private javax.swing.JButton makeRewardVisibleButton;
	private javax.swing.JButton makeVariableHiddenButton;
	private javax.swing.JButton makeVariableVisibleButton;
	private javax.swing.JButton okayButton;
	private javax.swing.JCheckBox optionCheckBox;
	private javax.swing.JPanel otherTabPanel;
	private javax.swing.ButtonGroup pathStyle;
	private javax.swing.JPanel pathStylePanel;
	private javax.swing.JRadioButton renderAllButton;
	private javax.swing.JPanel rewardPanel;
	private javax.swing.JPanel rewardTabPanel;
	private javax.swing.JPanel rightRewardColumn;
	private javax.swing.JPanel rightRewardPanel;
	private javax.swing.JPanel rightVariableColumn;
	private javax.swing.JPanel rightVariablePanel;
	private javax.swing.JButton selectAllHiddenRewardsButton;
	private javax.swing.JButton selectAllHiddenVariablesButton;
	private javax.swing.JButton selectAllVisibleRewardsButton;
	private javax.swing.JButton selectAllVisibleVariablesButton;
	private javax.swing.JCheckBox showCumulativeTimeCheckBox;
	private javax.swing.JCheckBox showTimeCheckBox;
	private javax.swing.JPanel topInnerTimePanel;
	private javax.swing.JPanel variablePanel;
	private javax.swing.JTabbedPane variableTabPane;
	private javax.swing.JPanel variableTabPanel;
	private javax.swing.JLabel visibleLabel;
	private javax.swing.JList visibleRewardList;
	private javax.swing.JScrollPane visibleRewardScrollList;
	private javax.swing.JList visibleVariableList;
	private javax.swing.JScrollPane visibleVariableScrollList;

	// End of variables declaration//GEN-END:variables

	/**
	 * Creates new form GUIViewDialog
	 */
	public GUIViewDialog(GUIPrism parent, SimulationView view, GUISimulatorPathTableModel pathTableModel)
	{
		super(parent, "Configure View for Simulation", true);

		this.gui = parent;
		this.view = view;

		//initialise
		initComponents();

		this.getRootPane().setDefaultButton(okayButton);

		super.setBounds(new Rectangle(550, 300));
		setResizable(true);
		setLocationRelativeTo(getParent()); // centre

		this.askOption = gui.getPrism().getSettings().getBoolean(PrismSettings.SIMULATOR_NEW_PATH_ASK_VIEW);
		optionCheckBox.setSelected(this.askOption);

		showTimeCheckBox.setSelected(view.showTime());
		showCumulativeTimeCheckBox.setSelected(view.showCumulativeTime());

		visibleVariableListModel = new VariableListModel(view.getVisibleVariables());
		hiddenVariableListModel = new VariableListModel(view.getHiddenVariables());

		visibleVariableList.setModel(visibleVariableListModel);
		hiddenVariableList.setModel(hiddenVariableListModel);

		ArrayList<RewardListItem> visibleRewardColumn = new ArrayList<RewardListItem>();
		ArrayList<RewardListItem> hiddenRewardColumn = new ArrayList<RewardListItem>();

		for (RewardStructure reward : view.getRewards()) {
			hiddenRewardColumn.add(new RewardListItem(reward, false));
			hiddenRewardColumn.add(new RewardListItem(reward, true));
		}

		for (RewardStructureColumn rewardColumn : view.getVisibleRewardColumns()) {
			for (RewardListItem rewardListItem : hiddenRewardColumn) {
				if (rewardColumn.getRewardStructure().equals(rewardListItem.getRewardStructure())
						&& rewardColumn.isCumulativeReward() == rewardListItem.isCumulative()) {
					visibleRewardColumn.add(rewardListItem);
					hiddenRewardColumn.remove(rewardListItem);

					break;
				}
			}
		}

		visibleRewardListModel = new RewardListModel(visibleRewardColumn);
		hiddenRewardListModel = new RewardListModel(hiddenRewardColumn);

		visibleRewardList.setModel(visibleRewardListModel);
		hiddenRewardList.setModel(hiddenRewardListModel);

		showCumulativeTimeCheckBox.setEnabled(pathTableModel.canShowTime());
		showTimeCheckBox.setEnabled(pathTableModel.canShowTime());

		pathStyle.add(renderAllButton);
		pathStyle.add(changeRenderingButton);

		renderAllButton.setSelected(!view.isChangeRenderer());
		changeRenderingButton.setSelected(view.isChangeRenderer());

		makeVariableHiddenButton.setIcon(GUIPrism.getIconFromImage("smallArrowRight.png"));
		makeRewardHiddenButton.setIcon(GUIPrism.getIconFromImage("smallArrowRight.png"));

		makeVariableVisibleButton.setIcon(GUIPrism.getIconFromImage("smallArrowLeft.png"));
		makeRewardVisibleButton.setIcon(GUIPrism.getIconFromImage("smallArrowLeft.png"));

		this.setVisible(true);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
	private void initComponents()
	{
		java.awt.GridBagConstraints gridBagConstraints;

		visibleLabel = new javax.swing.JLabel();
		hiddenLabel = new javax.swing.JLabel();
		pathStyle = new javax.swing.ButtonGroup();
		allPanel = new javax.swing.JPanel();
		bottomPanel = new javax.swing.JPanel();
		buttonPanel = new javax.swing.JPanel();
		okayButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();
		optionCheckBox = new javax.swing.JCheckBox();
		variableTabPane = new javax.swing.JTabbedPane();
		variableTabPanel = new javax.swing.JPanel();
		variablePanel = new javax.swing.JPanel();
		leftVariableColumn = new javax.swing.JPanel();
		leftVariablePanel = new javax.swing.JPanel();
		visibleVariableScrollList = new javax.swing.JScrollPane();
		visibleVariableList = new javax.swing.JList();
		selectAllVisibleVariablesButton = new javax.swing.JButton();
		centerVariableColumn = new javax.swing.JPanel();
		centerVariablePanel = new javax.swing.JPanel();
		makeVariableVisibleButton = new javax.swing.JButton();
		makeVariableHiddenButton = new javax.swing.JButton();
		rightVariableColumn = new javax.swing.JPanel();
		rightVariablePanel = new javax.swing.JPanel();
		hiddenVariableScrollList = new javax.swing.JScrollPane();
		hiddenVariableList = new javax.swing.JList();
		selectAllHiddenVariablesButton = new javax.swing.JButton();
		rewardTabPanel = new javax.swing.JPanel();
		rewardPanel = new javax.swing.JPanel();
		leftRewardColumn = new javax.swing.JPanel();
		leftRewardPanel = new javax.swing.JPanel();
		visibleRewardScrollList = new javax.swing.JScrollPane();
		visibleRewardList = new javax.swing.JList();
		selectAllVisibleRewardsButton = new javax.swing.JButton();
		centerRewardColumn = new javax.swing.JPanel();
		centerRewardPanel = new javax.swing.JPanel();
		makeRewardVisibleButton = new javax.swing.JButton();
		makeRewardHiddenButton = new javax.swing.JButton();
		rightRewardColumn = new javax.swing.JPanel();
		rightRewardPanel = new javax.swing.JPanel();
		hiddenRewardScrollList = new javax.swing.JScrollPane();
		hiddenRewardList = new javax.swing.JList();
		selectAllHiddenRewardsButton = new javax.swing.JButton();
		otherTabPanel = new javax.swing.JPanel();
		boxPanel = new javax.swing.JPanel();
		innerTimePanel = new javax.swing.JPanel();
		topInnerTimePanel = new javax.swing.JPanel();
		showTimeCheckBox = new javax.swing.JCheckBox();
		showCumulativeTimeCheckBox = new javax.swing.JCheckBox();
		pathStylePanel = new javax.swing.JPanel();
		innerPathStylePanel = new javax.swing.JPanel();
		changeRenderingButton = new javax.swing.JRadioButton();
		renderAllButton = new javax.swing.JRadioButton();

		visibleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		visibleLabel.setText("Visible Variables");
		hiddenLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		hiddenLabel.setText("Hidden Variables");

		addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(java.awt.event.WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		allPanel.setLayout(new java.awt.BorderLayout());

		allPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		bottomPanel.setLayout(new java.awt.BorderLayout());

		buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

		okayButton.setText("Okay");
		okayButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				okayButtonActionPerformed(evt);
			}
		});

		buttonPanel.add(okayButton);

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				cancelButtonActionPerformed(evt);
			}
		});

		buttonPanel.add(cancelButton);

		bottomPanel.add(buttonPanel, java.awt.BorderLayout.EAST);

		optionCheckBox.setText("Always prompt for view configuration on path creation");
		bottomPanel.add(optionCheckBox, java.awt.BorderLayout.WEST);
		optionCheckBox.getAccessibleContext().setAccessibleName("optionCheckBox");

		allPanel.add(bottomPanel, java.awt.BorderLayout.SOUTH);

		getContentPane().add(allPanel, java.awt.BorderLayout.SOUTH);

		variableTabPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		variableTabPanel.setLayout(new java.awt.BorderLayout());

		variablePanel.setLayout(new java.awt.GridBagLayout());

		variablePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		leftVariableColumn.setLayout(new java.awt.BorderLayout());

		leftVariableColumn.setBorder(javax.swing.BorderFactory.createTitledBorder("Visible variables"));
		leftVariablePanel.setLayout(new java.awt.BorderLayout(0, 5));

		leftVariablePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		visibleVariableScrollList.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		visibleVariableScrollList.setViewportView(visibleVariableList);

		leftVariablePanel.add(visibleVariableScrollList, java.awt.BorderLayout.CENTER);

		selectAllVisibleVariablesButton.setText("Select All");
		selectAllVisibleVariablesButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				selectAllVisibleVariablesButtonActionPerformed(evt);
			}
		});

		leftVariablePanel.add(selectAllVisibleVariablesButton, java.awt.BorderLayout.SOUTH);

		leftVariableColumn.add(leftVariablePanel, java.awt.BorderLayout.CENTER);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		variablePanel.add(leftVariableColumn, gridBagConstraints);

		centerVariableColumn.setLayout(new java.awt.BorderLayout());

		centerVariablePanel.setLayout(new java.awt.GridBagLayout());

		makeVariableVisibleButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				makeVariableVisibleButtonActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.ipadx = 5;
		gridBagConstraints.ipady = 5;
		gridBagConstraints.insets = new java.awt.Insets(3, 10, 3, 10);
		centerVariablePanel.add(makeVariableVisibleButton, gridBagConstraints);

		makeVariableHiddenButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				makeVariableHiddenButtonActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.ipadx = 5;
		gridBagConstraints.ipady = 5;
		gridBagConstraints.insets = new java.awt.Insets(3, 10, 3, 10);
		centerVariablePanel.add(makeVariableHiddenButton, gridBagConstraints);

		centerVariableColumn.add(centerVariablePanel, java.awt.BorderLayout.CENTER);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
		gridBagConstraints.weighty = 1.0;
		variablePanel.add(centerVariableColumn, gridBagConstraints);

		rightVariableColumn.setLayout(new java.awt.BorderLayout());

		rightVariableColumn.setBorder(javax.swing.BorderFactory.createTitledBorder("Hidden variables"));
		rightVariablePanel.setLayout(new java.awt.BorderLayout(0, 5));

		rightVariablePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		hiddenVariableScrollList.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		hiddenVariableScrollList.setViewportView(hiddenVariableList);

		rightVariablePanel.add(hiddenVariableScrollList, java.awt.BorderLayout.CENTER);

		selectAllHiddenVariablesButton.setText("Select All");
		selectAllHiddenVariablesButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				selectAllHiddenVariablesButtonActionPerformed(evt);
			}
		});

		rightVariablePanel.add(selectAllHiddenVariablesButton, java.awt.BorderLayout.SOUTH);

		rightVariableColumn.add(rightVariablePanel, java.awt.BorderLayout.CENTER);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		variablePanel.add(rightVariableColumn, gridBagConstraints);

		variableTabPanel.add(variablePanel, java.awt.BorderLayout.CENTER);

		variableTabPane.addTab("Variable visibility", variableTabPanel);

		rewardTabPanel.setLayout(new java.awt.BorderLayout());

		rewardPanel.setLayout(new java.awt.GridBagLayout());

		rewardPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		leftRewardColumn.setLayout(new java.awt.BorderLayout());

		leftRewardColumn.setBorder(javax.swing.BorderFactory.createTitledBorder("Visible reward structures"));
		leftRewardPanel.setLayout(new java.awt.BorderLayout(0, 5));

		leftRewardPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		visibleRewardScrollList.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		visibleRewardScrollList.setViewportView(visibleRewardList);

		leftRewardPanel.add(visibleRewardScrollList, java.awt.BorderLayout.CENTER);

		selectAllVisibleRewardsButton.setText("Select All");
		selectAllVisibleRewardsButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				selectAllVisibleRewardsButtonActionPerformed(evt);
			}
		});

		leftRewardPanel.add(selectAllVisibleRewardsButton, java.awt.BorderLayout.SOUTH);

		leftRewardColumn.add(leftRewardPanel, java.awt.BorderLayout.CENTER);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		rewardPanel.add(leftRewardColumn, gridBagConstraints);

		centerRewardColumn.setLayout(new java.awt.BorderLayout());

		centerRewardPanel.setLayout(new java.awt.GridBagLayout());

		makeRewardVisibleButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				makeRewardVisibleButtonActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.ipadx = 5;
		gridBagConstraints.ipady = 5;
		gridBagConstraints.insets = new java.awt.Insets(3, 10, 3, 10);
		centerRewardPanel.add(makeRewardVisibleButton, gridBagConstraints);

		makeRewardHiddenButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				makeRewardHiddenButtonActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.ipadx = 5;
		gridBagConstraints.ipady = 5;
		gridBagConstraints.insets = new java.awt.Insets(3, 10, 3, 10);
		centerRewardPanel.add(makeRewardHiddenButton, gridBagConstraints);

		centerRewardColumn.add(centerRewardPanel, java.awt.BorderLayout.CENTER);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
		gridBagConstraints.weighty = 1.0;
		rewardPanel.add(centerRewardColumn, gridBagConstraints);

		rightRewardColumn.setLayout(new java.awt.BorderLayout());

		rightRewardColumn.setBorder(javax.swing.BorderFactory.createTitledBorder("Hidden reward structures"));
		rightRewardPanel.setLayout(new java.awt.BorderLayout(0, 5));

		rightRewardPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		hiddenRewardScrollList.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		hiddenRewardScrollList.setViewportView(hiddenRewardList);

		rightRewardPanel.add(hiddenRewardScrollList, java.awt.BorderLayout.CENTER);

		selectAllHiddenRewardsButton.setText("Select All");
		selectAllHiddenRewardsButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				selectAllHiddenRewardsButtonActionPerformed(evt);
			}
		});

		rightRewardPanel.add(selectAllHiddenRewardsButton, java.awt.BorderLayout.SOUTH);

		rightRewardColumn.add(rightRewardPanel, java.awt.BorderLayout.CENTER);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		rewardPanel.add(rightRewardColumn, gridBagConstraints);

		rewardTabPanel.add(rewardPanel, java.awt.BorderLayout.CENTER);

		variableTabPane.addTab("Reward visibility", rewardTabPanel);

		otherTabPanel.setLayout(new java.awt.BorderLayout());

		boxPanel.setLayout(new javax.swing.BoxLayout(boxPanel, javax.swing.BoxLayout.Y_AXIS));

		boxPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		innerTimePanel.setLayout(new java.awt.BorderLayout());

		innerTimePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Time properties"));
		topInnerTimePanel.setLayout(new java.awt.GridLayout(2, 1, 5, 5));

		topInnerTimePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		showTimeCheckBox.setText("Show the time spent in states");
		showTimeCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		showTimeCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
		topInnerTimePanel.add(showTimeCheckBox);
		showTimeCheckBox.getAccessibleContext().setAccessibleName("");

		showCumulativeTimeCheckBox.setText("Show the cumulative time");
		showCumulativeTimeCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		showCumulativeTimeCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
		topInnerTimePanel.add(showCumulativeTimeCheckBox);

		innerTimePanel.add(topInnerTimePanel, java.awt.BorderLayout.NORTH);

		boxPanel.add(innerTimePanel);

		pathStylePanel.setLayout(new java.awt.BorderLayout());

		pathStylePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Path style"));
		innerPathStylePanel.setLayout(new java.awt.GridLayout(2, 1, 5, 5));

		innerPathStylePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		changeRenderingButton.setText("Render changes");
		changeRenderingButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		changeRenderingButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
		innerPathStylePanel.add(changeRenderingButton);

		renderAllButton.setText("Render all values");
		renderAllButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		renderAllButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
		innerPathStylePanel.add(renderAllButton);

		pathStylePanel.add(innerPathStylePanel, java.awt.BorderLayout.NORTH);

		boxPanel.add(pathStylePanel);

		otherTabPanel.add(boxPanel, java.awt.BorderLayout.NORTH);

		variableTabPane.addTab("Other", otherTabPanel);

		getContentPane().add(variableTabPane, java.awt.BorderLayout.CENTER);

	}// </editor-fold>//GEN-END:initComponents

	private void selectAllHiddenRewardsButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_selectAllHiddenRewardsButtonActionPerformed
		int[] indices = new int[hiddenRewardListModel.getSize()];
		for (int i = 0; i < indices.length; i++)
			indices[i] = i;

		hiddenRewardList.setSelectedIndices(indices);
	}//GEN-LAST:event_selectAllHiddenRewardsButtonActionPerformed

	private void makeRewardHiddenButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_makeRewardHiddenButtonActionPerformed
		int[] indices = visibleRewardList.getSelectedIndices();

		for (int i = indices.length - 1; i >= 0; i--) {
			RewardListItem rew = (RewardListItem) visibleRewardListModel.get(indices[i]);

			visibleRewardListModel.removeReward(rew);
			hiddenRewardListModel.addReward(rew);
		}
	}//GEN-LAST:event_makeRewardHiddenButtonActionPerformed

	private void makeRewardVisibleButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_makeRewardVisibleButtonActionPerformed
		int[] indices = hiddenRewardList.getSelectedIndices();
		for (int i = indices.length - 1; i >= 0; i--) {
			RewardListItem rew = (RewardListItem) hiddenRewardListModel.get(indices[i]);
			hiddenRewardListModel.removeReward(rew);
			visibleRewardListModel.addReward(rew);
		}
	}//GEN-LAST:event_makeRewardVisibleButtonActionPerformed

	private void selectAllVisibleRewardsButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_selectAllVisibleRewardsButtonActionPerformed
		int[] indices = new int[visibleRewardListModel.getSize()];
		for (int i = 0; i < indices.length; i++)
			indices[i] = i;
		visibleRewardList.setSelectedIndices(indices);
	}//GEN-LAST:event_selectAllVisibleRewardsButtonActionPerformed

	private void selectAllVisibleVariablesButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_selectAllVisibleVariablesButtonActionPerformed
		int[] indices = new int[visibleVariableListModel.getSize()];
		for (int i = 0; i < indices.length; i++)
			indices[i] = i;
		visibleVariableList.setSelectedIndices(indices);
	}//GEN-LAST:event_selectAllVisibleVariablesButtonActionPerformed

	private void selectAllHiddenVariablesButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_selectAllHiddenVariablesButtonActionPerformed
		int[] indices = new int[hiddenVariableListModel.getSize()];
		for (int i = 0; i < indices.length; i++)
			indices[i] = i;

		hiddenVariableList.setSelectedIndices(indices);

	}//GEN-LAST:event_selectAllHiddenVariablesButtonActionPerformed

	private void makeVariableVisibleButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_makeVariableVisibleButtonActionPerformed
		int[] indices = hiddenVariableList.getSelectedIndices();

		for (int i = indices.length - 1; i >= 0; i--) {
			Variable var = (Variable) hiddenVariableListModel.get(indices[i]);

			hiddenVariableListModel.removeVariable(var);
			visibleVariableListModel.addVariable(var);
		}
	}//GEN-LAST:event_makeVariableVisibleButtonActionPerformed

	private void makeVariableHiddenButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_makeVariableHiddenButtonActionPerformed
		int[] indices = visibleVariableList.getSelectedIndices();

		for (int i = indices.length - 1; i >= 0; i--) {
			Variable var = (Variable) visibleVariableListModel.get(indices[i]);

			visibleVariableListModel.removeVariable(var);
			hiddenVariableListModel.addVariable(var);
		}
	}//GEN-LAST:event_makeVariableHiddenButtonActionPerformed

	private void okayButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okayButtonActionPerformed
	{//GEN-HEADEREND:event_okayButtonActionPerformed

		if (optionCheckBox.isSelected() != this.askOption) {
			this.askOption = !this.askOption;

			try {
				gui.getPrism().getSettings().set(PrismSettings.SIMULATOR_NEW_PATH_ASK_VIEW, this.askOption);
			} catch (PrismException e) {
			}
		}

		view.showTime(showTimeCheckBox.isSelected());
		view.showCumulativeTime(showCumulativeTimeCheckBox.isSelected());
		view.setVariableVisibility(visibleVariableListModel.getVariables(), hiddenVariableListModel.getVariables());
		view.setRenderer(changeRenderingButton.isSelected());
		view.setVisibleRewardListItems(visibleRewardListModel.getRewards());

		dispose();
	}//GEN-LAST:event_okayButtonActionPerformed

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelButtonActionPerformed
	{//GEN-HEADEREND:event_cancelButtonActionPerformed
		dispose();
	}//GEN-LAST:event_cancelButtonActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	public void keyPressed(KeyEvent e)
	{
	}

	public void keyReleased(KeyEvent e)
	{

	}

	public void keyTyped(KeyEvent e)
	{

	}

	class VariableListModel extends DefaultListModel
	{
		private static final long serialVersionUID = 1L;

		public VariableListModel(ArrayList<Variable> variables)
		{
			super();
			for (int i = 0; i < variables.size(); i++) {
				super.add(i, variables.get(i));
			}
		}

		public void removeVariable(Variable variable)
		{
			for (int i = 0; i < super.getSize(); i++) {
				Variable var = (Variable) super.getElementAt(i);
				if (var.equals(variable)) {
					super.remove(i);
				}
			}
		}

		public void addVariable(Variable variable)
		{
			int i = 0;

			while (i < super.getSize() && ((Variable) super.getElementAt(i)).getIndex() < variable.getIndex()) {
				i++;
			}

			super.add(i, variable);
		}

		public ArrayList<Variable> getVariables()
		{
			ArrayList<Variable> list = new ArrayList<Variable>();
			for (int i = 0; i < super.getSize(); i++) {
				list.add((Variable) super.getElementAt(i));
			}

			return list;
		}
	}

	class RewardListItem
	{
		private RewardStructure rewardStructure;
		private boolean isCumulative;

		public RewardListItem(RewardStructure rewardStructure, boolean isCumulative)
		{
			this.rewardStructure = rewardStructure;
			this.isCumulative = isCumulative;
		}

		public String toString()
		{
			String res = rewardStructure.toString();
			if (isCumulative)
				return res + " (cumulative)";
			else {
				if (!rewardStructure.isStateEmpty() && !rewardStructure.isTransitionEmpty())
					return res + " (state and transition)";
				else if (!rewardStructure.isStateEmpty())
					return res + " (state)";
				else if (!rewardStructure.isTransitionEmpty())
					return res + " (transition)";
				else
					return res + " (empty)";
			}
		}

		public boolean isCumulative()
		{
			return isCumulative;
		}

		public void setCumulative(boolean isCumulative)
		{
			this.isCumulative = isCumulative;
		}

		public RewardStructure getRewardStructure()
		{
			return rewardStructure;
		}

		public void setRewardStructure(RewardStructure rewardStructure)
		{
			this.rewardStructure = rewardStructure;
		}

		public boolean equals(Object obj)
		{
			if (obj instanceof RewardListItem) {
				RewardListItem item = (RewardListItem) obj;
				return (item.getRewardStructure().equals(this.rewardStructure) && item.isCumulative() == isCumulative);
			}

			return false;
		}
	}

	class RewardListModel extends DefaultListModel
	{
		private static final long serialVersionUID = 1L;

		public RewardListModel(ArrayList<RewardListItem> rewardListItems)
		{
			for (int i = 0; i < rewardListItems.size(); i++) {
				super.add(i, rewardListItems.get(i));

			}
		}

		public void removeReward(RewardListItem reward)
		{
			for (int i = 0; i < super.getSize(); i++) {
				RewardListItem rew = (RewardListItem) super.getElementAt(i);
				if (rew.equals(reward)) {
					super.remove(i);
				}
			}
		}

		public void addReward(RewardListItem reward)
		{
			int i = 0;

			while (i < super.getSize() && ((RewardListItem) super.getElementAt(i)).getRewardStructure().getIndex() < reward.getRewardStructure().getIndex()) {
				i++;
			}

			if (i < super.getSize() && ((RewardListItem) super.getElementAt(i)).getRewardStructure().getIndex() == reward.getRewardStructure().getIndex()
					&& reward.isCumulative()) {
				i++;
			}

			super.add(i, reward);
		}

		public ArrayList<RewardListItem> getRewards()
		{
			ArrayList<RewardListItem> list = new ArrayList<RewardListItem>();
			for (int i = 0; i < super.getSize(); i++) {
				list.add((RewardListItem) super.getElementAt(i));
			}

			return list;
		}
	}
}
