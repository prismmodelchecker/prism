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

package userinterface.simulator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import parser.ast.ModulesFile;
import prism.PrismSettings;
import userinterface.CheckBoxList;
import userinterface.GUIPrism;

@SuppressWarnings("serial")
public class GUIPathPlotDialog extends JDialog
{
	// ComboBox enums

	public enum SimulateChoice {
		STEP, TIME;
		public String toString()
		{
			switch (this) {
			case STEP:
				return "Up to step";
			case TIME:
				return "Up to time";
			default:
				return this.toString();
			}
		}
	};

	public enum ShowChoice {
		ALL_STEPS, SNAPSHOTS;
		public String toString()
		{
			switch (this) {
			case ALL_STEPS:
				return "All steps";
			case SNAPSHOTS:
				return "Snapshots";
			default:
				return this.toString();
			}
		}
	};

	// Static instance
	private static GUIPathPlotDialog instance = null;

	// State
	private GUIPrism gui;
	private ModulesFile modulesFile;
	private boolean cancelled;
	private String simPathString;
	private long maxPathLength;

	// GUI objects
	private final JPanel topPanel = new JPanel();
	private JTextField textFieldTime;
	private JTextField textFieldInterval;
	private JLabel lblInterval;
	private JComboBox comboBoxShow;
	private JComboBox comboBoxSimulate;
	private JButton okButton;
	private JButton cancelButton;
	private JTabbedPane tabbedPane;
	private JPanel varsPanel;
	private CheckBoxList varsCheckList;
	private Vector<JCheckBox> varsCheckBoxes;
	private JPanel varsRadios;
	private JRadioButton rdbtnVarsAll;
	private JRadioButton rdbtnVarsSelected;
	private final ButtonGroup buttonGroupVars = new ButtonGroup();
	private JPanel rewardsPanel;
	private JLabel lblVarsShow;
	private JRadioButton rdbtnVarsNone;
	private JPanel bottomPanel;
	private JPanel mainPanel;
	private JCheckBox chckbxChanges;
	private JPanel rewardsRadios;
	private JLabel lblRewardsShow;
	private JRadioButton rdbtnRewardsAll;
	private JRadioButton rdbtnRewardsNone;
	private final ButtonGroup buttonGroupRewards = new ButtonGroup();
	private JLabel lblMaximumPathLength;
	private JTextField textFieldMaxLen;

	/**
	 * Show "Path Plot Details" dialog, return the dialog.
	 * Returns null if the dialog was cancelled.
	 */
	public static GUIPathPlotDialog showDialog(GUIPrism parent, ModulesFile modulesFile)
	{
		GUIPathPlotDialog dialog = getInstance(parent, modulesFile);
		dialog.setVisible(true);
		return dialog.wasCancelled() ? null : dialog;
	}

	/**
	 * Show "Path Plot Details" dialog, return settings as a simpath string.
	 * Returns null if the dialog was cancelled.
	 */
	public static String getPathPlotSettings(GUIPrism parent, ModulesFile modulesFile)
	{
		GUIPathPlotDialog dialog = getInstance(parent, modulesFile);
		dialog.setVisible(true);
		return dialog.wasCancelled() ? null : dialog.getSimPathString();
	}

	/**
	 * Get static instance, creating if necessary.
	 */
	private static GUIPathPlotDialog getInstance(GUIPrism parent, ModulesFile modulesFile)
	{
		if (instance != null && instance.gui == parent && instance.modulesFile == modulesFile)
			return instance;
		else {
			instance = new GUIPathPlotDialog(parent, modulesFile);
			return instance;
		}
	}

	public boolean wasCancelled()
	{
		return cancelled;
	}

	public String getSimPathString()
	{
		return simPathString;
	}

	public long getMaxPathLength()
	{
		return maxPathLength;
	}

	/**
	 * Create the dialog.
	 */
	public GUIPathPlotDialog(GUIPrism parent, ModulesFile modulesFile)
	{
		super(parent, "Path Plot Details", true);
		this.gui = parent;
		this.modulesFile = modulesFile;
		setBounds(100, 100, 361, 401);
		getContentPane().setLayout(new BorderLayout());
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						okButtonActionPerformed(e);
					}
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						cancelButtonActionPerformed(e);
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		this.getRootPane().setDefaultButton(okButton);
		{
			mainPanel = new JPanel();
			getContentPane().add(mainPanel, BorderLayout.CENTER);
			GridBagLayout gbl_mainPanel = new GridBagLayout();
			gbl_mainPanel.columnWidths = new int[] { 329, 0 };
			gbl_mainPanel.rowHeights = new int[] { 103, 0, 0 };
			gbl_mainPanel.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
			gbl_mainPanel.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
			mainPanel.setLayout(gbl_mainPanel);
			mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			GridBagConstraints gbc_topPanel = new GridBagConstraints();
			gbc_topPanel.insets = new Insets(0, 0, 5, 0);
			gbc_topPanel.gridx = 0;
			gbc_topPanel.gridy = 0;
			mainPanel.add(topPanel, gbc_topPanel);
			GridBagLayout gbl_topPanel = new GridBagLayout();
			gbl_topPanel.columnWidths = new int[] { 50, 0, 0, 60, 0 };
			gbl_topPanel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
			gbl_topPanel.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
			gbl_topPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
			topPanel.setLayout(gbl_topPanel);
			{
				JLabel lblSimulate = new JLabel("Simulate:");
				GridBagConstraints gbc_lblSimulate = new GridBagConstraints();
				gbc_lblSimulate.insets = new Insets(0, 0, 5, 5);
				gbc_lblSimulate.anchor = GridBagConstraints.WEST;
				gbc_lblSimulate.gridx = 0;
				gbc_lblSimulate.gridy = 0;
				topPanel.add(lblSimulate, gbc_lblSimulate);
			}
			{
				comboBoxSimulate = new JComboBox();
				comboBoxSimulate.setModel(new DefaultComboBoxModel(SimulateChoice.values()));
				GridBagConstraints gbc_comboBoxSimulate = new GridBagConstraints();
				gbc_comboBoxSimulate.anchor = GridBagConstraints.WEST;
				gbc_comboBoxSimulate.insets = new Insets(0, 0, 5, 5);
				gbc_comboBoxSimulate.gridx = 1;
				gbc_comboBoxSimulate.gridy = 0;
				topPanel.add(comboBoxSimulate, gbc_comboBoxSimulate);
			}
			{
				textFieldTime = new JTextField();
				GridBagConstraints gbc_textFieldTime = new GridBagConstraints();
				gbc_textFieldTime.fill = GridBagConstraints.HORIZONTAL;
				gbc_textFieldTime.insets = new Insets(0, 0, 5, 5);
				gbc_textFieldTime.gridx = 2;
				gbc_textFieldTime.gridy = 0;
				topPanel.add(textFieldTime, gbc_textFieldTime);
				textFieldTime.setColumns(5);
			}
			{
				JLabel lblShow = new JLabel("Sample:");
				GridBagConstraints gbc_lblShow = new GridBagConstraints();
				gbc_lblShow.anchor = GridBagConstraints.WEST;
				gbc_lblShow.insets = new Insets(0, 0, 5, 5);
				gbc_lblShow.gridx = 0;
				gbc_lblShow.gridy = 1;
				topPanel.add(lblShow, gbc_lblShow);
			}
			{
				comboBoxShow = new JComboBox();
				comboBoxShow.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						comboBoxShowActionPerformed(e);
					}
				});
				comboBoxShow.setModel(new DefaultComboBoxModel(ShowChoice.values()));
				GridBagConstraints gbc_comboBoxShow = new GridBagConstraints();
				gbc_comboBoxShow.anchor = GridBagConstraints.WEST;
				gbc_comboBoxShow.insets = new Insets(0, 0, 5, 5);
				gbc_comboBoxShow.gridx = 1;
				gbc_comboBoxShow.gridy = 1;
				topPanel.add(comboBoxShow, gbc_comboBoxShow);
			}
			{
				lblInterval = new JLabel("of interval");
				GridBagConstraints gbc_lblInterval = new GridBagConstraints();
				gbc_lblInterval.insets = new Insets(0, 0, 5, 5);
				gbc_lblInterval.gridx = 2;
				gbc_lblInterval.gridy = 1;
				topPanel.add(lblInterval, gbc_lblInterval);
			}
			{
				textFieldInterval = new JTextField();
				GridBagConstraints gbc_textFieldInterval = new GridBagConstraints();
				gbc_textFieldInterval.anchor = GridBagConstraints.WEST;
				gbc_textFieldInterval.insets = new Insets(0, 0, 5, 0);
				gbc_textFieldInterval.gridx = 3;
				gbc_textFieldInterval.gridy = 1;
				topPanel.add(textFieldInterval, gbc_textFieldInterval);
				textFieldInterval.setColumns(5);
			}
			{
				chckbxChanges = new JCheckBox("Plot changes only");
				GridBagConstraints gbc_chckbxChanges = new GridBagConstraints();
				gbc_chckbxChanges.anchor = GridBagConstraints.WEST;
				gbc_chckbxChanges.gridwidth = 2;
				gbc_chckbxChanges.insets = new Insets(0, 0, 5, 5);
				gbc_chckbxChanges.gridx = 0;
				gbc_chckbxChanges.gridy = 2;
				topPanel.add(chckbxChanges, gbc_chckbxChanges);
			}
			{
				lblMaximumPathLength = new JLabel("Maximum path length:");
				GridBagConstraints gbc_lblMaximumPathLength = new GridBagConstraints();
				gbc_lblMaximumPathLength.anchor = GridBagConstraints.WEST;
				gbc_lblMaximumPathLength.gridwidth = 2;
				gbc_lblMaximumPathLength.insets = new Insets(0, 0, 0, 5);
				gbc_lblMaximumPathLength.gridx = 0;
				gbc_lblMaximumPathLength.gridy = 3;
				topPanel.add(lblMaximumPathLength, gbc_lblMaximumPathLength);
			}
			{
				textFieldMaxLen = new JTextField();
				GridBagConstraints gbc_textFieldMaxLen = new GridBagConstraints();
				gbc_textFieldMaxLen.anchor = GridBagConstraints.WEST;
				gbc_textFieldMaxLen.gridwidth = 2;
				gbc_textFieldMaxLen.insets = new Insets(0, 0, 0, 5);
				gbc_textFieldMaxLen.gridx = 2;
				gbc_textFieldMaxLen.gridy = 3;
				topPanel.add(textFieldMaxLen, gbc_textFieldMaxLen);
				textFieldMaxLen.setColumns(8);
			}
			{
				bottomPanel = new JPanel();
				GridBagConstraints gbc_bottomPanel = new GridBagConstraints();
				gbc_bottomPanel.fill = GridBagConstraints.VERTICAL;
				gbc_bottomPanel.gridx = 0;
				gbc_bottomPanel.gridy = 1;
				mainPanel.add(bottomPanel, gbc_bottomPanel);
				GridBagLayout gbl_bottomPanel = new GridBagLayout();
				gbl_bottomPanel.columnWidths = new int[] { 20, 0 };
				gbl_bottomPanel.rowHeights = new int[] { 120, 0 };
				gbl_bottomPanel.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
				gbl_bottomPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
				bottomPanel.setLayout(gbl_bottomPanel);
				tabbedPane = new JTabbedPane(JTabbedPane.TOP);
				GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
				gbc_tabbedPane.fill = GridBagConstraints.HORIZONTAL;
				gbc_tabbedPane.gridx = 0;
				gbc_tabbedPane.gridy = 0;
				bottomPanel.add(tabbedPane, gbc_tabbedPane);
				{
					varsPanel = new JPanel();
					tabbedPane.addTab("Variables", null, varsPanel, null);
					GridBagLayout gbl_varsPanel = new GridBagLayout();
					gbl_varsPanel.columnWidths = new int[] { 260, 0 };
					gbl_varsPanel.rowHeights = new int[] { 33, 10, 0 };
					gbl_varsPanel.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
					gbl_varsPanel.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
					varsPanel.setLayout(gbl_varsPanel);
					{
						varsRadios = new JPanel();
						FlowLayout fl_varsRadios = (FlowLayout) varsRadios.getLayout();
						fl_varsRadios.setAlignment(FlowLayout.LEFT);
						GridBagConstraints gbc_varsRadios = new GridBagConstraints();
						gbc_varsRadios.anchor = GridBagConstraints.NORTHWEST;
						gbc_varsRadios.insets = new Insets(0, 0, 5, 0);
						gbc_varsRadios.gridx = 0;
						gbc_varsRadios.gridy = 0;
						varsPanel.add(varsRadios, gbc_varsRadios);
						{
							lblVarsShow = new JLabel("Show:");
							varsRadios.add(lblVarsShow);
						}
						{
							rdbtnVarsAll = new JRadioButton("All");
							buttonGroupVars.add(rdbtnVarsAll);
							rdbtnVarsAll.addActionListener(new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									doEnables();
								}
							});
							varsRadios.add(rdbtnVarsAll);
						}
						{
							rdbtnVarsNone = new JRadioButton("None");
							buttonGroupVars.add(rdbtnVarsNone);
							rdbtnVarsNone.addActionListener(new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									doEnables();
								}
							});
							varsRadios.add(rdbtnVarsNone);
						}
						{
							rdbtnVarsSelected = new JRadioButton("Selected");
							buttonGroupVars.add(rdbtnVarsSelected);
							rdbtnVarsSelected.addActionListener(new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									doEnables();
								}
							});
							varsRadios.add(rdbtnVarsSelected);
						}
					}
					{
						JScrollPane varsScroll = new JScrollPane();
						varsCheckList = new CheckBoxList();
						varsScroll.setViewportView(varsCheckList);
						varsCheckBoxes = new Vector<JCheckBox>();
						varsCheckList.setListData(varsCheckBoxes);
						GridBagConstraints gbc_varsScroll = new GridBagConstraints();
						gbc_varsScroll.anchor = GridBagConstraints.NORTH;
						gbc_varsScroll.fill = GridBagConstraints.HORIZONTAL;
						gbc_varsScroll.gridx = 0;
						gbc_varsScroll.gridy = 1;
						varsPanel.add(varsScroll, gbc_varsScroll);
					}
				}
				{
					rewardsPanel = new JPanel();
					tabbedPane.addTab("Rewards", null, rewardsPanel, null);
					{
						rewardsRadios = new JPanel();
						rewardsPanel.add(rewardsRadios);
						rewardsRadios.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
						{
							lblRewardsShow = new JLabel("Show:");
							rewardsRadios.add(lblRewardsShow);
						}
						{
							rdbtnRewardsAll = new JRadioButton("All");
							buttonGroupRewards.add(rdbtnRewardsAll);
							rewardsRadios.add(rdbtnRewardsAll);
						}
						{
							rdbtnRewardsNone = new JRadioButton("None");
							buttonGroupRewards.add(rdbtnRewardsNone);
							rewardsRadios.add(rdbtnRewardsNone);
						}
					}
				}
			}
		}

		initComponents();
	}

	private void initComponents()
	{
		if (modulesFile.getModelType().continuousTime()) {
			comboBoxSimulate.setSelectedItem(SimulateChoice.TIME);
			comboBoxShow.setSelectedItem(ShowChoice.SNAPSHOTS);
			textFieldTime.setText("10.0");
			textFieldInterval.setText("0.1");
		} else {
			comboBoxSimulate.setSelectedItem(SimulateChoice.STEP);
			comboBoxShow.setSelectedItem(ShowChoice.ALL_STEPS);
			textFieldTime.setText("100");
			textFieldInterval.setText("");
		}
		chckbxChanges.setSelected(true);
		textFieldMaxLen.setText("" + gui.getPrism().getSettings().getLong(PrismSettings.SIMULATOR_DEFAULT_MAX_PATH));
		rdbtnVarsAll.setSelected(true);
		for (int i = 0; i < modulesFile.getNumVars(); i++) {
			varsCheckBoxes.add(new JCheckBox(modulesFile.getVarName(i)));
		}
		//
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(getParent()); // centre
		pack();
		doEnables();
		cancelled = true;
	}

	private void doEnables()
	{
		if ((ShowChoice) comboBoxShow.getSelectedItem() == ShowChoice.SNAPSHOTS) {
			lblInterval.setEnabled(true);
			textFieldInterval.setEnabled(true);
		} else {
			lblInterval.setEnabled(false);
			textFieldInterval.setEnabled(false);
		}
		// Variable list enabled iff "show selected"
		if (rdbtnVarsSelected.isSelected()) {
			varsCheckList.setEnabled(true);
			for (JCheckBox cb : varsCheckBoxes) {
				cb.setEnabled(true);
			}
		} else {
			varsCheckList.setEnabled(false);
			for (JCheckBox cb : varsCheckBoxes) {
				cb.setEnabled(false);
			}
		}
	}

	public void comboBoxShowActionPerformed(ActionEvent e)
	{
		doEnables();
	}

	public void okButtonActionPerformed(ActionEvent event)
	{
		// Validate inputs and build simpath string, store max path len
		simPathString = "";
		switch ((SimulateChoice) comboBoxSimulate.getSelectedItem()) {
		case STEP:
			try {
				int i = Integer.parseInt(textFieldTime.getText());
				if (i < 0)
					throw new NumberFormatException();
				simPathString += i;
			} catch (NumberFormatException e) {
				gui.errorDialog("Invalid number of steps \"" + textFieldTime.getText() + "\"");
				return;
			}
			break;
		case TIME:
			try {
				double d = Double.parseDouble(textFieldTime.getText());
				if (d < 0)
					throw new NumberFormatException();
				simPathString += "time=" + d;
			} catch (NumberFormatException e) {
				gui.errorDialog("Invalid time \"" + textFieldTime.getText() + "\"");
				return;
			}
			break;
		}
		if ((ShowChoice) comboBoxShow.getSelectedItem() == ShowChoice.SNAPSHOTS) {
			try {
				double d = Double.parseDouble(textFieldInterval.getText());
				if (d < 0)
					throw new NumberFormatException();
				simPathString += ",snapshot=" + d;
			} catch (NumberFormatException e) {
				gui.errorDialog("Invalid time interval \"" + textFieldInterval.getText() + "\"");
				return;
			}
		}
		if (chckbxChanges.isSelected()) {
			simPathString += ",changes=true";
		}
		if (rdbtnVarsNone.isSelected()) {
			simPathString += ",vars=()";
		} else if (rdbtnVarsSelected.isSelected()) {
			String s = "";
			for (JCheckBox cb : varsCheckBoxes) {
				if (cb.isSelected()) {
					s += "," + cb.getText();
				}
			}
			simPathString += ",vars=(" + s + ")";
		}
		simPathString += ",rewards=" + rdbtnRewardsAll.isSelected();
		try {
			long l = Long.parseLong(textFieldMaxLen.getText());
			if (l < 0)
				throw new NumberFormatException();
			maxPathLength = l;
		} catch (NumberFormatException e) {
			gui.errorDialog("Invalid maximum path length \"" + textFieldMaxLen.getText() + "\"");
			return;
		}
		
		cancelled = false;
		dispose();
	}

	public void cancelButtonActionPerformed(ActionEvent e)
	{
		cancelled = true;
		dispose();
	}

	// Close when Escape pressed
	protected JRootPane createRootPane()
	{
		ActionListener actionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent actionEvent)
			{
				cancelled = true;
				dispose();
			}
		};
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		JRootPane rootPane = new JRootPane();
		rootPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
		return rootPane;
	}
}
