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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import parser.ast.ModulesFile;
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
		ALL_STEPS, CHANGES, SNAPSHOTS;
		public String toString()
		{
			switch (this) {
			case ALL_STEPS:
				return "All steps";
			case CHANGES:
				return "Changes";
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

	// GUI objects
	private final JPanel contentPanel = new JPanel();
	private JTextField textFieldTime;
	private JTextField textFieldInterval;
	private JLabel lblInterval;
	private JComboBox comboBoxShow;
	private JComboBox comboBoxSimulate;
	private JButton okButton;
	private JButton cancelButton;

	/**
	 * Show "Path Plot Details" dialog, return settings as a simpath string.
	 * Returns null if the dialog was cancelled.
	 */
	public static String getPathPlotSettings(GUIPrism parent, ModulesFile modulesFile)
	{
		GUIPathPlotDialog dialog = getInstance(parent, modulesFile);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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

	/**
	 * Create the dialog.
	 */
	public GUIPathPlotDialog(GUIPrism parent, ModulesFile modulesFile)
	{
		super(parent, "Path Plot Details", true);
		this.gui = parent;
		this.modulesFile = modulesFile;
		setBounds(100, 100, 450, 154);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[] { 20, 0, 0, 0, 60, 0 };
		gbl_contentPanel.rowHeights = new int[] { 20, 0, 0, 0, 0 };
		gbl_contentPanel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_contentPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblSimulate = new JLabel("Simulate:");
			GridBagConstraints gbc_lblSimulate = new GridBagConstraints();
			gbc_lblSimulate.insets = new Insets(0, 0, 5, 5);
			gbc_lblSimulate.anchor = GridBagConstraints.EAST;
			gbc_lblSimulate.gridx = 1;
			gbc_lblSimulate.gridy = 1;
			contentPanel.add(lblSimulate, gbc_lblSimulate);
		}
		{
			comboBoxSimulate = new JComboBox();
			comboBoxSimulate.setModel(new DefaultComboBoxModel(SimulateChoice.values()));
			GridBagConstraints gbc_comboBoxSimulate = new GridBagConstraints();
			gbc_comboBoxSimulate.fill = GridBagConstraints.HORIZONTAL;
			gbc_comboBoxSimulate.insets = new Insets(0, 0, 5, 5);
			gbc_comboBoxSimulate.gridx = 2;
			gbc_comboBoxSimulate.gridy = 1;
			contentPanel.add(comboBoxSimulate, gbc_comboBoxSimulate);
		}
		{
			textFieldTime = new JTextField();
			GridBagConstraints gbc_textFieldTime = new GridBagConstraints();
			gbc_textFieldTime.fill = GridBagConstraints.HORIZONTAL;
			gbc_textFieldTime.insets = new Insets(0, 0, 5, 5);
			gbc_textFieldTime.gridx = 3;
			gbc_textFieldTime.gridy = 1;
			contentPanel.add(textFieldTime, gbc_textFieldTime);
			textFieldTime.setColumns(5);
		}
		{
			JLabel lblShow = new JLabel("Show:");
			GridBagConstraints gbc_lblShow = new GridBagConstraints();
			gbc_lblShow.anchor = GridBagConstraints.EAST;
			gbc_lblShow.insets = new Insets(0, 0, 5, 5);
			gbc_lblShow.gridx = 1;
			gbc_lblShow.gridy = 2;
			contentPanel.add(lblShow, gbc_lblShow);
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
			gbc_comboBoxShow.fill = GridBagConstraints.HORIZONTAL;
			gbc_comboBoxShow.insets = new Insets(0, 0, 5, 5);
			gbc_comboBoxShow.gridx = 2;
			gbc_comboBoxShow.gridy = 2;
			contentPanel.add(comboBoxShow, gbc_comboBoxShow);
		}
		{
			lblInterval = new JLabel("of interval");
			GridBagConstraints gbc_lblInterval = new GridBagConstraints();
			gbc_lblInterval.insets = new Insets(0, 0, 5, 5);
			gbc_lblInterval.gridx = 3;
			gbc_lblInterval.gridy = 2;
			contentPanel.add(lblInterval, gbc_lblInterval);
		}
		{
			textFieldInterval = new JTextField();
			GridBagConstraints gbc_textFieldInterval = new GridBagConstraints();
			gbc_textFieldInterval.insets = new Insets(0, 0, 5, 0);
			gbc_textFieldInterval.fill = GridBagConstraints.HORIZONTAL;
			gbc_textFieldInterval.gridx = 4;
			gbc_textFieldInterval.gridy = 2;
			contentPanel.add(textFieldInterval, gbc_textFieldInterval);
			textFieldInterval.setColumns(5);
		}
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

		// defaults for new creation
		if (modulesFile.getModelType().continuousTime()) {
			comboBoxSimulate.setSelectedItem(SimulateChoice.TIME);
			textFieldTime.setText("100.0");
			textFieldInterval.setText("1.0");
		} else {
			textFieldTime.setText("100");
			textFieldInterval.setText("");
		}
		comboBoxShow.setSelectedItem(ShowChoice.CHANGES);

		this.getRootPane().setDefaultButton(okButton);
		setLocationRelativeTo(getParent()); // centre
		doEnables();
		cancelled = true;
	}

	public boolean wasCancelled()
	{
		return cancelled;
	}

	public String getSimPathString()
	{
		return simPathString;
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
	}

	public void comboBoxShowActionPerformed(ActionEvent e)
	{
		doEnables();
	}

	public void okButtonActionPerformed(ActionEvent event)
	{
		// Validate inputs and build simpath string 
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
		if ((ShowChoice) comboBoxShow.getSelectedItem() == ShowChoice.CHANGES) {
			simPathString += ",changes=true";
		}

		cancelled = false;
		dispose();
	}

	public void cancelButtonActionPerformed(ActionEvent e)
	{
		cancelled = true;
		dispose();
	}
}
