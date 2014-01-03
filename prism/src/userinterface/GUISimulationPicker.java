//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
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

package userinterface;

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import parser.*;
import parser.ast.*;
import parser.type.*;
import prism.*;
import simulator.method.*;

/**
 *  GUISimulationPicker is a dialog to collect the details required for
 *  approximate (simulation-based) model checking: initial state, max path length,
 *  simulation method + parameters, etc.
 */
public class GUISimulationPicker extends javax.swing.JDialog implements KeyListener
{
	// Default serial version ID
	private static final long serialVersionUID = 1L;

	//STATICS

	public static final int NO_VALUES = 0;
	public static final int VALUES_DONE = 1;
	public static final int CANCELLED = 2;

	public static SimulationInformation lastSimulationInformation = null;

	//ATTRIBUTES

	private GUIPrism gui;
	private ModulesFile modulesFile;
	private SimulationInformation information;

	private boolean cancelled = true;

	private JTable initValuesTable;
	private DefineValuesTable initValuesModel;

	// Last valid contents of text boxes
	private String lastWidth;
	private String lastConf;
	private String lastNumSamples;

	private boolean atLeastOneRwd;
	private boolean atLeastOneQuant;

	// Variables declaration - do not modify//GEN-BEGIN:variables
	// Note: this code has now been modified manually; form is no longer used.
	javax.swing.JTextField widthField;
	javax.swing.JComboBox selectSimulationMethod;
	javax.swing.JComboBox automaticCalculateCombo;
	private javax.swing.JButton cancelButton;
	javax.swing.JTextField confidenceField;
	javax.swing.JTextField numberToDecide;
	//private javax.swing.JCheckBox distributedCheck;
	javax.swing.JTextField numSamplesField;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabelSSM;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel10;
	private javax.swing.JPanel jPanel11;
	private javax.swing.JPanel jPanel12;
	private javax.swing.JPanel jPanel13;
	private javax.swing.JPanel jPanel14;
	private javax.swing.JPanel jPanel15;
	private javax.swing.JPanel jPanel16;
	private javax.swing.JPanel jPanel17;
	private javax.swing.JPanel jPanel18;
	private javax.swing.JPanel jPanel19;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel20;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JPanel jPanel6;
	private javax.swing.JPanel jPanel7;
	private javax.swing.JPanel jPanel8;
	private javax.swing.JPanel jPanel9;
	private javax.swing.JButton okayButton;
	javax.swing.JTextField pathLengthField;
	javax.swing.JPanel topPanel;
	javax.swing.JCheckBox useDefaultInitialCheck;

	// End of variables declaration//GEN-END:variables

	/**
	 * Create a new GUIConstantsPicker dialog to define info for simulation-based property checking.
	 * @param parent Parent GUI window
	 * @param exprs The properties to be checked (note: constants may not be defined)
	 * @param modulesFile The model that will be simulated (for initial state info)
	 * @param titleExtra Optional string to append to dialog (null if not needed)
	 * @throws PrismException if there is a problem getting initial state info from the model.
	 */
	public GUISimulationPicker(GUIPrism parent, List<Expression> exprs, ModulesFile modulesFile, String titleExtra) throws PrismException
	{
		super(parent, "Simulation Parameters" + ((titleExtra != null) ? (" - " + titleExtra) : ""), true);
		this.gui = parent;
		this.modulesFile = modulesFile;

		// See if there are any reward props in the list to be checked
		atLeastOneRwd = false;
		for (Expression expr : exprs) {
			if (expr instanceof ExpressionReward) {
				atLeastOneRwd = true;
				break;
			}
		}

		// See if there are any quantitative (=?) props in the list to be checked
		atLeastOneQuant = false;
		for (Expression expr : exprs) {
			if (Expression.isQuantitative(expr)) {
				atLeastOneQuant = true;
				break;
			}
		}

		// Create a SimulationInformation object to store info in this dialog,
		// (or re-use last one if user prefers to do that)
		PrismSettings settings = parent.getPrism().getSettings();
		if (lastSimulationInformation == null || settings.getString(PrismSettings.SIMULATOR_FIELD_CHOICE).equals("Always use defaults")) {
			lastSimulationInformation = new SimulationInformation(settings);
			// Pick a default method (CI for quantitative, SPRT for bounded)
			lastSimulationInformation.setMethod(atLeastOneQuant ? SimulationInformation.Method.CI : SimulationInformation.Method.SPRT);
		}

		// Make sure method choice is valid (if remembered)
		if (atLeastOneRwd && lastSimulationInformation.getMethod() == SimulationInformation.Method.APMC) {
			lastSimulationInformation.setMethod(SimulationInformation.Method.CI);
		}
		if (atLeastOneQuant && lastSimulationInformation.getMethod() == SimulationInformation.Method.SPRT) {
			lastSimulationInformation.setMethod(SimulationInformation.Method.CI);
		}

		lastSimulationInformation.setPropReward(atLeastOneRwd);

		lastWidth = "" + lastSimulationInformation.getWidth();
		lastConf = "" + lastSimulationInformation.getConfidence();
		lastNumSamples = "" + lastSimulationInformation.getNumSamples();

		// create "initial state" table
		initValuesModel = new DefineValuesTable();
		initValuesTable = new GreyableJTable();
		initValuesTable.setModel(initValuesModel);
		initValuesTable.setSelectionMode(DefaultListSelectionModel.SINGLE_INTERVAL_SELECTION);
		initValuesTable.setCellSelectionEnabled(true);

		// set up simulation information based on previous info (or defaults - see above)
		information = lastSimulationInformation;

		// currently, we deliberately do not recall the last initial state used
		// since it may no longer be valid due to changes in the model
		information.setInitialState(null);
		
		// initialise
		initComponents();
		this.getRootPane().setDefaultButton(okayButton);

		initTable();
		initValues();
		widthField.addKeyListener(this);
		confidenceField.addKeyListener(this);
		numSamplesField.addKeyListener(this);
		doEnablesAndCalculations(true);
		setResizable(false);

		pack();
		setLocationRelativeTo(getParent()); // centre
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 * Note: this code has now been modified manually; form is no longer used.
	 */
	private void initComponents()//GEN-BEGIN:initComponents
	{
		java.awt.GridBagConstraints gridBagConstraints;

		jPanel6 = new javax.swing.JPanel();
		okayButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();
		jPanel7 = new javax.swing.JPanel();
		jPanel1 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jPanel3 = new javax.swing.JPanel();
		jPanel4 = new javax.swing.JPanel();
		jPanel5 = new javax.swing.JPanel();
		topPanel = new javax.swing.JPanel();
		jPanel10 = new javax.swing.JPanel();
		jPanel8 = new javax.swing.JPanel();
		jPanel9 = new javax.swing.JPanel();
		jLabelSSM = new javax.swing.JLabel();
		jLabel4 = new javax.swing.JLabel();
		jLabel5 = new javax.swing.JLabel();
		jLabel6 = new javax.swing.JLabel();
		jLabel7 = new javax.swing.JLabel();
		jPanel11 = new javax.swing.JPanel();
		jPanel12 = new javax.swing.JPanel();
		selectSimulationMethod = new javax.swing.JComboBox(); // new
		automaticCalculateCombo = new javax.swing.JComboBox();
		jPanel13 = new javax.swing.JPanel();
		widthField = new javax.swing.JTextField();
		confidenceField = new javax.swing.JTextField();
		numberToDecide = new javax.swing.JTextField();
		numSamplesField = new javax.swing.JTextField();
		jPanel14 = new javax.swing.JPanel();
		jPanel15 = new javax.swing.JPanel();
		jPanel16 = new javax.swing.JPanel();
		jLabel2 = new javax.swing.JLabel();
		jPanel17 = new javax.swing.JPanel();
		pathLengthField = new javax.swing.JTextField();
		jPanel18 = new javax.swing.JPanel();
		jPanel19 = new javax.swing.JPanel();
		jPanel20 = new javax.swing.JPanel();
		//distributedCheck = new javax.swing.JCheckBox();
		useDefaultInitialCheck = new javax.swing.JCheckBox();

		addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(java.awt.event.WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

		okayButton.setText("Okay");
		okayButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				okayButtonActionPerformed(evt);
			}
		});

		jPanel6.add(okayButton);

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				cancelButtonActionPerformed(evt);
			}
		});

		jPanel6.add(cancelButton);

		getContentPane().add(jPanel6, java.awt.BorderLayout.SOUTH);

		jPanel7.setLayout(new java.awt.BorderLayout());

		jPanel1.setLayout(new java.awt.GridBagLayout());

		jPanel1.setBorder(new javax.swing.border.TitledBorder("Initial state"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		jPanel1.add(jPanel2, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		jPanel1.add(jPanel3, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		jPanel1.add(jPanel4, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		jPanel1.add(jPanel5, gridBagConstraints);
		
		useDefaultInitialCheck.setText("Use default initial state");
		useDefaultInitialCheck.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				useDefaultInitialCheckActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		jPanel1.add(useDefaultInitialCheck, gridBagConstraints);
		
		topPanel.setLayout(new java.awt.BorderLayout());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		jPanel1.add(topPanel, gridBagConstraints);

		jPanel7.add(jPanel1, java.awt.BorderLayout.CENTER);

		jPanel10.setLayout(new javax.swing.BoxLayout(jPanel10, javax.swing.BoxLayout.Y_AXIS));

		jPanel8.setLayout(new java.awt.GridBagLayout());

		jPanel8.setBorder(new javax.swing.border.TitledBorder("Sampling parameters"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		jPanel8.add(jPanel9, gridBagConstraints);

		jLabelSSM.setText("Simulation method:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel8.add(jLabelSSM, gridBagConstraints);

		jLabel4.setText("Automatically calculate:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel8.add(jLabel4, gridBagConstraints);

		jLabel5.setText("Width:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel8.add(jLabel5, gridBagConstraints);

		jLabel6.setText("Confidence:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel8.add(jLabel6, gridBagConstraints);

		jLabel7.setText("Number of samples:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel8.add(jLabel7, gridBagConstraints);

		jPanel11.setPreferredSize(new java.awt.Dimension(10, 5));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 5;
		jPanel8.add(jPanel11, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 3;
		jPanel8.add(jPanel12, gridBagConstraints);

		// Populate simulation method choices
		Vector<String> methods = new Vector<String>();
		methods.add("CI");
		methods.add("ACI");
		if (!atLeastOneRwd)
			methods.add("APMC");
		if (!atLeastOneQuant)
			methods.add("SPRT");
		selectSimulationMethod.setModel(new javax.swing.DefaultComboBoxModel(methods));

		selectSimulationMethod.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				selectSimulationMethodActionPerformed(evt);
			}
		});

		automaticCalculateCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { }));
		automaticCalculateCombo.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				automaticCalculateComboActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		jPanel8.add(selectSimulationMethod, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		jPanel8.add(automaticCalculateCombo, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 5;
		gridBagConstraints.gridy = 0;
		jPanel8.add(jPanel13, gridBagConstraints);

		widthField.setPreferredSize(new java.awt.Dimension(65, 20));
		widthField.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				widthFieldActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		jPanel8.add(widthField, gridBagConstraints);

		confidenceField.setPreferredSize(new java.awt.Dimension(60, 20));
		confidenceField.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				confidenceFieldActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		jPanel8.add(confidenceField, gridBagConstraints);

		numSamplesField.setPreferredSize(new java.awt.Dimension(60, 20));
		numSamplesField.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				numSamplesFieldActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		jPanel8.add(numSamplesField, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 9;
		jPanel8.add(jPanel14, gridBagConstraints);

		jPanel10.add(jPanel8);

		jPanel15.setLayout(new java.awt.GridBagLayout());

		jPanel15.setBorder(new javax.swing.border.TitledBorder("Path parameters"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		jPanel15.add(jPanel16, gridBagConstraints);

		jLabel2.setText("Maximum path length:");
		jLabel2.setPreferredSize(new java.awt.Dimension(150, 15));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel15.add(jLabel2, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		jPanel15.add(jPanel17, gridBagConstraints);

		pathLengthField.setPreferredSize(new java.awt.Dimension(158, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		jPanel15.add(pathLengthField, gridBagConstraints);

		jPanel18.setPreferredSize(new java.awt.Dimension(118, 10));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = 2;
		jPanel15.add(jPanel18, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 6;
		gridBagConstraints.gridy = 0;
		jPanel15.add(jPanel19, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		jPanel15.add(jPanel20, gridBagConstraints);

		//distributedCheck.setText("Distributed?");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		//jPanel15.add(distributedCheck, gridBagConstraints);

		jPanel10.add(jPanel15);

		jPanel7.add(jPanel10, java.awt.BorderLayout.EAST);

		getContentPane().add(jPanel7, java.awt.BorderLayout.CENTER);

		pack();
	}//GEN-END:initComponents

	private void numSamplesFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_numSamplesFieldActionPerformed
	{//GEN-HEADEREND:event_numSamplesFieldActionPerformed
		doEnablesAndCalculations(false);
	}//GEN-LAST:event_numSamplesFieldActionPerformed

	private void confidenceFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_confidenceFieldActionPerformed
	{//GEN-HEADEREND:event_confidenceFieldActionPerformed
		doEnablesAndCalculations(false);
	}//GEN-LAST:event_confidenceFieldActionPerformed

	private void widthFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_approximationFieldActionPerformed
	{//GEN-HEADEREND:event_widthFieldActionPerformed
		doEnablesAndCalculations(false);
	}//GEN-LAST:event_widthFieldActionPerformed

	private void selectSimulationMethodActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_automaticCalculateComboActionPerformed
	{//GEN-HEADEREND:event_selectSimulationMethodActionPerformed
		doChangeSimulationMethod();
	}//GEN-LAST:event_selectSimulationMethodActionPerformed

	private void automaticCalculateComboActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_automaticCalculateComboActionPerformed
	{//GEN-HEADEREND:event_automaticCalculateComboActionPerformed
		doEnablesAndCalculations(true);
	}//GEN-LAST:event_automaticCalculateComboActionPerformed

	private static boolean isValidDoubleOrEmpty(String s)
	{
		if (s.isEmpty())
			return true;
		try {
			Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	private void doChangeSimulationMethod()
	{
		// Remember choice for "Automatically calculate"
		int automaticCalculateComboIndex = automaticCalculateCombo.getSelectedIndex();
		// Store method, change text labels, and
		// populate "Automatically calculate" choices based on method
		information.setMethodByName((String) selectSimulationMethod.getSelectedItem());
		switch (information.getMethod()) {
		case CI:
			jLabel5.setText("Width:");
			jLabel6.setText("Confidence:");
			automaticCalculateCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Width", "Confidence", "Number of samples" }));
			break;
		case ACI:
			jLabel5.setText("Width:");
			jLabel6.setText("Confidence:");
			automaticCalculateCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Width", "Confidence", "Number of samples" }));
			break;
		case APMC:
			jLabel5.setText("Approximation:");
			jLabel6.setText("Confidence:");
			automaticCalculateCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Approximation", "Confidence", "Number of samples" }));
			break;
		case SPRT:
			jLabel5.setText("Indifference:");
			jLabel6.setText("Type I/II error:");
			automaticCalculateCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Number of samples" }));
			break;
		}
		// Restore choice for "Automatically calculate"
		// (unless empty list, indicated by -1 indexx, which means this the initial setup)
		if (automaticCalculateComboIndex != -1) {
			// If choice invalid due to list size change, reset to 0
			if (automaticCalculateComboIndex > automaticCalculateCombo.getItemCount() - 1)
				automaticCalculateComboIndex = 0;
			automaticCalculateCombo.setSelectedIndex(automaticCalculateComboIndex);
		}
	}

	/**
	 * Update dialog after a change in one of the controls.
	 * Enable/disable text fields as necessary.
	 * Compute new value for unknown fields.
	 * @param autoChange indicates that the "method" or "auto calc" has changed
	 */
	private void doEnablesAndCalculations(boolean autoChange)
	{
		double width, confidence;
		int numSamples;
		
		// Save valid values for later
		if (isValidDoubleOrEmpty(confidenceField.getText()))
			lastConf = confidenceField.getText();
		if (isValidDoubleOrEmpty(numSamplesField.getText()))
			lastNumSamples = numSamplesField.getText();
		if (isValidDoubleOrEmpty(widthField.getText()))
			lastWidth = widthField.getText();

		// Store current unknown parameter
		information.setUnknownByName((String) automaticCalculateCombo.getSelectedItem());

		// Enable all text fields (then disable some below)
		widthField.setEnabled(true);
		confidenceField.setEnabled(true);
		numSamplesField.setEnabled(true);

		switch (information.getMethod()) {

		case CI:
		case ACI:
			switch (information.getUnknown()) {
			case WIDTH:
				widthField.setEnabled(false);
				widthField.setText("Unknown before sim.");
				widthField.setCaretPosition(0);
				// If method changes, display last valid values
				if (autoChange) {
					numSamplesField.setText(lastNumSamples);
					numSamplesField.setCaretPosition(0);
					confidenceField.setText(lastConf);
					confidenceField.setCaretPosition(0);
				}
				break;
			case CONFIDENCE:
				confidenceField.setEnabled(false);
				confidenceField.setText("Unknown before sim.");
				confidenceField.setCaretPosition(0);
				// If method changes, display last valid values
				if (autoChange) {
					widthField.setText(lastWidth);
					widthField.setCaretPosition(0);
					numSamplesField.setText(lastNumSamples);
					numSamplesField.setCaretPosition(0);
				}
				break;
			case NUM_SAMPLES:
				numSamplesField.setEnabled(false);
				numSamplesField.setText("Unknown before sim.");
				numSamplesField.setCaretPosition(0);
				// If method changes, display last valid values
				if (autoChange) {
					widthField.setText(lastWidth);
					widthField.setCaretPosition(0);
					confidenceField.setText(lastConf);
					confidenceField.setCaretPosition(0);
				}
				break;
			}
			break;

		case APMC:
			switch (information.getUnknown()) {
			case WIDTH:
				widthField.setEnabled(false);
				try {
					confidence = Double.parseDouble(confidenceField.getText());
					numSamples = Integer.parseInt(numSamplesField.getText());
					if (confidence > 0 && confidence < 1 && numSamples > 0) {
						widthField.setText("" + (new APMCapproximation(confidence, numSamples)).getMissingParameter());
						widthField.setCaretPosition(0);
					}
				} catch (NumberFormatException e) {
					// Any problems, don't update text field
				} catch (PrismException e) {
					// Any problems, don't update text field
				}
				// If method changes, display last valid values
				if (autoChange) {
					numSamplesField.setText(lastNumSamples);
					numSamplesField.setCaretPosition(0);
					confidenceField.setText(lastConf);
					confidenceField.setCaretPosition(0);
				}
				break;
			case CONFIDENCE:
				confidenceField.setEnabled(false);
				try {
					width = Double.parseDouble(widthField.getText());
					numSamples = Integer.parseInt(numSamplesField.getText());
					if (width > 0 && numSamples > 0) {
						try {
							confidenceField.setText("" + (new APMCconfidence(width, numSamples)).getMissingParameter());
							confidenceField.setCaretPosition(0);
						} catch (PrismException e) {
							// Catch this error to set helpful message
							confidenceField.setText("Incr. approx/samples");
							confidenceField.setCaretPosition(0);
						}
					}
				} catch (NumberFormatException e) {
					// Any problems, don't update text field
				}
				// If method changes, display last valid values
				if (autoChange) {
					widthField.setText(lastWidth);
					widthField.setCaretPosition(0);
					numSamplesField.setText(lastNumSamples);
					numSamplesField.setCaretPosition(0);
				}
				break;
			case NUM_SAMPLES:
				numSamplesField.setEnabled(false);
				try {
					width = Double.parseDouble(widthField.getText());
					confidence = Double.parseDouble(confidenceField.getText());
					if (width > 0 && confidence > 0 && confidence < 1) {
						numSamplesField.setText("" + (new APMCiterations(confidence, width)).getMissingParameter());
						numSamplesField.setCaretPosition(0);
					}
				} catch (NumberFormatException e) {
					// Any problems, don't update text field
				} catch (PrismException e) {
					// Any problems, don't update text field
				}
				// If method changes, display last valid values
				if (autoChange) {
					widthField.setText(lastWidth);
					widthField.setCaretPosition(0);
					confidenceField.setText(lastConf);
					confidenceField.setCaretPosition(0);
				}
				break;
			}
			break;

		case SPRT:
			numSamplesField.setEnabled(false);
			numSamplesField.setText("Unknown before sim.");
			numSamplesField.setCaretPosition(0);
			// If method changes, display last valid values
			if (autoChange) {
				widthField.setText(lastWidth);
				widthField.setCaretPosition(0);
				confidenceField.setText(lastConf);
				confidenceField.setCaretPosition(0);
			}
			break;
		}
	}

	private void initTable()
	{
		JScrollPane sp = new JScrollPane();

		sp.setViewportView(initValuesTable);
		topPanel.add(sp);

		topPanel.setPreferredSize(new Dimension(300, 300));
	}

	/**
	 * Populate dialog using 'information' field.
	 */
	private void initValues()
	{
		// Use default initial state?
		// (currently always the case on initialisation)
		if (information.getInitialState() == null) {
			// Tick box
			useDefaultInitialCheck.setSelected(true);
			// Put variable names in table, but no values
			for (int i = 0; i < modulesFile.getNumVars(); i++) {
				initValuesModel.addValue(new Value(modulesFile.getVarName(i), modulesFile.getVarType(i), null));
			}
			// Disable table
			if (initValuesTable.getCellEditor() != null)
			initValuesTable.getCellEditor().stopCellEditing();
			initValuesTable.getSelectionModel().clearSelection();
			initValuesTable.setEnabled(false);
		} else {
			// Untick box
			useDefaultInitialCheck.setSelected(false);
			// Set up table (from information)
			// Need to add to add some validity checks here if re-enabled
			for (int i = 0; i < modulesFile.getNumVars(); i++) {
				initValuesModel.addValue(new Value(modulesFile.getVarName(i), modulesFile.getVarType(i), information.getInitialState().getValue(i)));
			}
			initValuesTable.setEnabled(true);
		}

		// populate parameter text boxes
		widthField.setText("" + information.getWidth());
		widthField.setCaretPosition(0);
		confidenceField.setText("" + information.getConfidence());
		confidenceField.setCaretPosition(0);
		numSamplesField.setText("" + information.getNumSamples());
		numSamplesField.setCaretPosition(0);
		// note: do this after above "setText"s because it triggers a doEnablesAndCalculations()
		selectSimulationMethod.setSelectedItem(information.getMethodName());
		automaticCalculateCombo.setSelectedItem(information.getUnknownName());

		pathLengthField.setText("" + information.getMaxPathLength());
		//distributedCheck.setSelected(information.isDistributed());
	}

	/**
	 * Create a new GUIConstantsPicker dialog to define info for simulation-based property checking.
	 * @param parent Parent GUI window
	 * @param exprs The properties to be checked (note: constants may not be defined)
	 * @param modulesFile The model that will be simulated (for initial state info)
	 * @param titleExtra Optional string to append to dialog (null if not needed)
	 * @throws PrismException if there is a problem getting initial state info from the model.
	 */
	public static SimulationInformation defineSimulationWithDialog(GUIPrism parent, List<Expression> exprs, ModulesFile modulesFile, String titleExtra)
			throws PrismException
	{
		return new GUISimulationPicker(parent, exprs, modulesFile, titleExtra).defineValues();
	}

	/**
	 * Create a new GUIConstantsPicker dialog to define info for simulation-based property checking.
	 * @param parent Parent GUI window
	 * @param expr The property to be checked (note: constants may not be defined)
	 * @param modulesFile The model that will be simulated (for initial state info)
	 * @param titleExtra Optional string to append to dialog (null if not needed)
	 * @throws PrismException if there is a problem getting initial state info from the model.
	 */
	public static SimulationInformation defineSimulationWithDialog(GUIPrism parent, Expression expr, ModulesFile modulesFile, String titleExtra)
			throws PrismException
	{
		List<Expression> exprs = new ArrayList<Expression>(1);
		exprs.add(expr);
		return new GUISimulationPicker(parent, exprs, modulesFile, titleExtra).defineValues();
	}

	public SimulationInformation defineValues()
	{
		setVisible(true);
		if (cancelled)
			return null;
		else
			return information;
	}

	private void okayButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okayButtonActionPerformed
	{//GEN-HEADEREND:event_okayButtonActionPerformed
		int i;

		doEnablesAndCalculations(false);

		if (initValuesTable.getCellEditor() != null)
			initValuesTable.getCellEditor().stopCellEditing();

		String parameter = "";
		try {
			// Check (editable) simulation parameters are ok, store in SimulationInformation
			if (widthField.isEnabled()) {
				parameter = jLabel5.getText().substring(0, jLabel5.getText().length() - 1);
				double width = Double.parseDouble(widthField.getText());
				if (width <= 0) {
					gui.errorDialog("Parameter \"" + parameter + "\" most be positive");
					return;
				}
				information.setWidth(width);
			}
			if (confidenceField.isEnabled()) {
				parameter = jLabel6.getText().substring(0, jLabel6.getText().length() - 1);
				double confidence = Double.parseDouble(confidenceField.getText());
				if (confidence <= 0 || confidence >= 1) {
					gui.errorDialog("Parameter \"" + parameter + "\" most be between 0 and 1");
					return;
				}
				information.setConfidence(confidence);
			}
			if (numSamplesField.isEnabled()) {
				parameter = "Number of samples";
				int numSamples = Integer.parseInt(numSamplesField.getText());
				if (numSamples <= 0) {
					gui.errorDialog("Parameter \"" + parameter + "\" most be positive");
					return;
				}
				information.setNumSamples(numSamples);
			}
			// Create SimulationMethod object, just to check parameters are ok
			try {
				information.createSimulationMethod().computeMissingParameterBeforeSim();
			} catch (PrismException e) {
				gui.errorDialog(e.getMessage());
				return;
			}
			// Check/store max path length
			parameter = "Maximum path length";
			long maxPathLength = Long.parseLong(pathLengthField.getText());
			if (maxPathLength <= 0) {
				gui.errorDialog("Parameter \"" + parameter + "\" most be positive");
				return;
			}
			information.setMaxPathLength(maxPathLength);
			// Store initial state
			if (useDefaultInitialCheck.isSelected()) {
				information.setInitialState(null);
			} else {
				Values newInitState = new Values();
				for (i = 0; i < initValuesModel.getNumValues(); i++) {
					parameter = initValuesModel.getValue(i).name;
					Object parameterValue = null;
					if (initValuesModel.getValue(i).type instanceof TypeBool) {
						String bool = initValuesModel.getValue(i).value.toString();
						if (!(bool.equals("true") || bool.equals("false")))
							throw new NumberFormatException();
						parameterValue = new Boolean(bool);
					} else if (initValuesModel.getValue(i).type instanceof TypeInt) {
						parameterValue = new Integer(initValuesModel.getValue(i).value.toString());
					} else { 
						throw new NumberFormatException();
					}
					newInitState.addValue(parameter, parameterValue);
				}
				information.setInitialState(newInitState);
			}
			
			//information.setDistributed(distributedCheck.isSelected());
			cancelled = false;
			lastSimulationInformation = information;
			dispose();
		} catch (NumberFormatException e) {
			gui.errorDialog("Invalid value entered for parameter \"" + parameter + "\"");
		}

	}//GEN-LAST:event_okayButtonActionPerformed

	private void useDefaultInitialCheckActionPerformed(java.awt.event.ActionEvent evt)
	{
		// If ticking...
		if (useDefaultInitialCheck.isSelected()) {
			// Clear values in table
			for (int i = 0; i < modulesFile.getNumVars(); i++) {
				initValuesModel.getValue(i).value = null;
			}
			// Disable table
			if (initValuesTable.getCellEditor() != null)
			initValuesTable.getCellEditor().stopCellEditing();
			initValuesTable.getSelectionModel().clearSelection();
			initValuesTable.setEnabled(false);
		}
		// If unticking
		else {
			// Set up table (based on default initial state)
			try {
				State defaultInitialState = modulesFile.getDefaultInitialState();
				if (defaultInitialState == null)
					throw new PrismException("");
				for (int i = 0; i < modulesFile.getNumVars(); i++) {
					initValuesModel.getValue(i).value = defaultInitialState.varValues[i];
				}
			} catch (PrismException e) {
				// In case of error, clear values
				for (int i = 0; i < modulesFile.getNumVars(); i++) {
					initValuesModel.getValue(i).value = null;
				}
			}
			initValuesTable.setEnabled(true);
		}
	}
	
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
		doEnablesAndCalculations(false);
	}

	public void keyTyped(KeyEvent e)
	{

	}

	class DefineValuesTable extends AbstractTableModel
	{
		ArrayList<Value> values;

		public DefineValuesTable()
		{
			values = new ArrayList<Value>();
		}

		public void addValue(Value v)
		{
			values.add(v);
			fireTableRowsInserted(values.size() - 1, values.size() - 1);
		}

		public int getNumValues()
		{
			return values.size();
		}

		public Value getValue(int i)
		{
			return values.get(i);
		}

		public int getColumnCount()
		{
			return 3;
		}

		public int getRowCount()
		{
			return values.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{

			Value v = values.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return v.name;
			case 1:
				return v.type.getTypeString();
			case 2:
				return v.value == null ? "" : v.value.toString();
			default:
				return "";
			}
		}

		public String getColumnName(int columnIndex)
		{
			switch (columnIndex) {
			case 0:
				return "Name";
			case 1:
				return "Type";
			case 2:
				return "Value";
			default:
				return "";
			}
		}

		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			if (columnIndex == 2)
				return true;
			else
				return false;
		}

		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			if (columnIndex == 2) {
				Value v = values.get(rowIndex);
				String s = (String) aValue;
				v.value = s;
				fireTableCellUpdated(rowIndex, columnIndex);
			}
		}

		public String toString()
		{
			String str = "";
			for (int i = 0; i < values.size(); i++) {
				Value c = values.get(i);
				str += c.toString();
				if (i != values.size() - 1)
					str += ",";
			}
			return str;
		}

	}

	class Value
	{
		String name;
		parser.type.Type type;
		Object value;

		public Value(String name, parser.type.Type type, Object value)
		{
			this.name = name;
			this.type = type;
			this.value = value;
		}

		public String toString()
		{
			return name + "=" + value;
		}
	}

	public class GreyableJTable extends JTable
	{
		private static final long serialVersionUID = 1L;
		private TableCellRenderer tableCellRenderer = new TableCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
			{
				Component c = GreyableJTable.super.getCellRenderer(row, column).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				c.setEnabled(table != null && table.isEnabled());
				return c;
			}
		};

		public TableCellRenderer getCellRenderer(int row, int column)
		{
			return tableCellRenderer;
		}
	}
}
