//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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
import parser.*;
import prism.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.util.*;
import java.awt.event.*;

/**
 *  GUISimulationPicker is a dialog to collect the initial state, approximation,
 *  confidence, number of samples and path length for simulation techniques.
 *
 *  (Some of this code was modified from GUIConstantsPicker)
 */
public class GUISimulationPicker extends javax.swing.JDialog implements KeyListener
{
	
	//STATICS
	
	public static final int NO_VALUES = 0;
	public static final int VALUES_DONE = 1;
	public static final int CANCELLED = 2;
	
	public static SimulationInformation lastSimulationInformation = null;
	
	//ATTRIBUTES
	
	private GUIPrism gui;
	private ModulesFile mf;
	private SimulationInformation information;
	
	private boolean cancelled = true;
	
	private JTable initValuesTable;
	private DefineValuesTable initValuesModel;
	private Action okAction;
	private Action cancelAction;
	
	// Variables declaration - do not modify//GEN-BEGIN:variables
	javax.swing.JTextField approximationField;
	javax.swing.JComboBox automaticCalculateCombo;
	private javax.swing.JButton cancelButton;
	javax.swing.JTextField confidenceField;
	private javax.swing.JCheckBox distributedCheck;
	javax.swing.JTextField iterationsField;
	private javax.swing.JLabel jLabel2;
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
	// End of variables declaration//GEN-END:variables
	
	/** Creates new form GUIConstantsPicker */
	public GUISimulationPicker(GUIPrism parent, Values defaultInitial, ModulesFile mf) { this(parent, defaultInitial, mf, null); }
	public GUISimulationPicker(GUIPrism parent, Values defaultInitial, ModulesFile mf, String titleExtra)
	{
		super(parent, "Simulation Parameters"+((titleExtra != null)?(" - "+titleExtra):""), true);
		
		this.gui = parent;
		this.mf = mf;
		
		// if there is no existing simulation information
		// or if the user wants to use the default values regardless
		// then use defaults...
		PrismSettings settings = parent.getPrism().getSettings();
		if(lastSimulationInformation == null || settings.getString(PrismSettings.SIMULATOR_FIELD_CHOICE).equals("Always use defaults"))
		{
			double dappr = settings.getDouble(PrismSettings.SIMULATOR_DEFAULT_APPROX);
			double dconf = settings.getDouble(PrismSettings.SIMULATOR_DEFAULT_CONFIDENCE);
			int dsamples = settings.getInteger(PrismSettings.SIMULATOR_DEFAULT_NUM_SAMPLES);
			int dpathlen = settings.getInteger(PrismSettings.SIMULATOR_DEFAULT_MAX_PATH);
			lastSimulationInformation = new SimulationInformation(null, dappr, dconf, dsamples, dpathlen);
		}
		
		// create "initial state" table
		initValuesModel = new DefineValuesTable();
		initValuesTable = new JTable();
		initValuesTable.setModel(initValuesModel);
		initValuesTable.setSelectionMode(DefaultListSelectionModel.SINGLE_INTERVAL_SELECTION);
		initValuesTable.setCellSelectionEnabled(true);
		
		// set up simulation information based on previous info (or defaults - see above)
		information = lastSimulationInformation;
		// set initial state as passed in to constructor
		information.setInitialState(defaultInitial);
		
		// initialise
		initComponents();
		this.getRootPane().setDefaultButton(okayButton);
		
		initTable();
		initValues();
		approximationField.addKeyListener(this);
		confidenceField.addKeyListener(this);
		iterationsField.addKeyListener(this);
		doEnablesAndCalculations();
		setResizable(false);
		
		pack();
		setLocationRelativeTo(getParent()); // centre
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
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
		jLabel4 = new javax.swing.JLabel();
		jLabel5 = new javax.swing.JLabel();
		jLabel6 = new javax.swing.JLabel();
		jLabel7 = new javax.swing.JLabel();
		jPanel11 = new javax.swing.JPanel();
		jPanel12 = new javax.swing.JPanel();
		automaticCalculateCombo = new javax.swing.JComboBox();
		jPanel13 = new javax.swing.JPanel();
		approximationField = new javax.swing.JTextField();
		confidenceField = new javax.swing.JTextField();
		iterationsField = new javax.swing.JTextField();
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

		jPanel1.setBorder(new javax.swing.border.TitledBorder("Initial State"));
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

		topPanel.setLayout(new java.awt.BorderLayout());

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		jPanel1.add(topPanel, gridBagConstraints);

		jPanel7.add(jPanel1, java.awt.BorderLayout.CENTER);

		jPanel10.setLayout(new javax.swing.BoxLayout(jPanel10, javax.swing.BoxLayout.Y_AXIS));

		jPanel8.setLayout(new java.awt.GridBagLayout());

		jPanel8.setBorder(new javax.swing.border.TitledBorder("Sampling Parameters"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		jPanel8.add(jPanel9, gridBagConstraints);

		jLabel4.setText("Automatically Calculate:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel8.add(jLabel4, gridBagConstraints);

		jLabel5.setText("Approximation:");
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

		jLabel7.setText("Number of Samples:");
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

		automaticCalculateCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Number of samples", "Approximation", "Confidence" }));
		automaticCalculateCombo.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				automaticCalculateComboActionPerformed(evt);
			}
		});

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

		approximationField.setPreferredSize(new java.awt.Dimension(65, 20));
		approximationField.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				approximationFieldActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		jPanel8.add(approximationField, gridBagConstraints);

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

		iterationsField.setPreferredSize(new java.awt.Dimension(60, 20));
		iterationsField.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				iterationsFieldActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		jPanel8.add(iterationsField, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 9;
		jPanel8.add(jPanel14, gridBagConstraints);

		jPanel10.add(jPanel8);

		jPanel15.setLayout(new java.awt.GridBagLayout());

		jPanel15.setBorder(new javax.swing.border.TitledBorder("Path Parameters"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		jPanel15.add(jPanel16, gridBagConstraints);

		jLabel2.setText("Maximum Path Length:");
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
	
	private void iterationsFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_iterationsFieldActionPerformed
	{//GEN-HEADEREND:event_iterationsFieldActionPerformed
		doEnablesAndCalculations();
	}//GEN-LAST:event_iterationsFieldActionPerformed
	
	private void confidenceFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_confidenceFieldActionPerformed
	{//GEN-HEADEREND:event_confidenceFieldActionPerformed
		doEnablesAndCalculations();
	}//GEN-LAST:event_confidenceFieldActionPerformed
	
	private void approximationFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_approximationFieldActionPerformed
	{//GEN-HEADEREND:event_approximationFieldActionPerformed
		doEnablesAndCalculations();
	}//GEN-LAST:event_approximationFieldActionPerformed
	
	private void automaticCalculateComboActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_automaticCalculateComboActionPerformed
	{//GEN-HEADEREND:event_automaticCalculateComboActionPerformed
		doEnablesAndCalculations();
	}//GEN-LAST:event_automaticCalculateComboActionPerformed
	
	private void doEnablesAndCalculations()
	{
		double approx, confid;
		int iterat;
		
		switch(automaticCalculateCombo.getSelectedIndex())
		{
			case 0://auto iterations
				iterationsField.setEnabled(false);
				approximationField.setEnabled(true);
				confidenceField.setEnabled(true);
				try
				{
					approx = Double.parseDouble(approximationField.getText());
					confid = Double.parseDouble(confidenceField.getText());
					if (approx > 0 && confid > 0 && confid < 1) {
						iterat = GUIPrism.getGUI().getPrism().computeSimulationNumSamples(approx, confid);
						iterationsField.setText(""+iterat);
					}
				}
				catch(NumberFormatException e)
				{
					// if something goes wrong, don't update text field
				}
				break;
			case 1://auto approximation
				iterationsField.setEnabled(true);
				approximationField.setEnabled(false);
				confidenceField.setEnabled(true);
				try
				{
					confid = Double.parseDouble(confidenceField.getText());
					iterat = Integer.parseInt(iterationsField.getText());
					if (confid > 0 && confid < 1 && iterat > 0) {
						approx = GUIPrism.getGUI().getPrism().computeSimulationApproximation(confid, iterat);
						approximationField.setText(""+approx);
					}
				}
				catch(NumberFormatException e)
				{
					// if something goes wrong, don't update text field
				}
				break;
			case 2://auto confidence
				iterationsField.setEnabled(true);
				approximationField.setEnabled(true);
				confidenceField.setEnabled(false);
				try
				{
					approx = Double.parseDouble(approximationField.getText());
					iterat = Integer.parseInt(iterationsField.getText());
					if (approx > 0 && iterat > 0) {
						confid = GUIPrism.getGUI().getPrism().computeSimulationConfidence(approx, iterat);
						confidenceField.setText(""+confid);
					}
				}
				catch(NumberFormatException e)
				{
					// if something goes wrong, don't update text field
				}
				break;
		}
	}
	
	private void initTable()
	{
		JScrollPane sp = new JScrollPane();
		
		sp.setViewportView(initValuesTable);
		topPanel.add(sp);
		
		topPanel.setPreferredSize(new Dimension(300,300));
	}
	
	private void initValues()
	{
		
		Value v;
		if(information.getInitialState() == null)
		{
			/*
			int n,i,j,n2;
			Declaration decl;
			Module module;
			
			// first add all globals
			n = mf.getNumGlobals();
			for (i = 0; i < n; i++)
			{
				decl = mf.getGlobal(i);
				v = new Value(decl.getName(), decl.getType(), "");
				initValuesModel.addValue(v);
			}
			// then add all module variables
			n = mf.getNumModules();
			for (i = 0; i < n; i++)
			{
				module = mf.getModule(i);
				n2 = module.getNumDeclarations();
				for (j = 0; j < n2; j++)
				{
					decl = module.getDeclaration(j);
					v = new Value(decl.getName(), decl.getType(), "");
					initValuesModel.addValue(v);
				}
			}
			*/
			initValuesTable.setEnabled(false);
		}
		else
		{
			for(int i = 0; i < information.getInitialState().getNumValues(); i++)
			{
				try
				{
					if(information.getInitialState().getType(i) == Expression.BOOLEAN)
						v = new Value(information.getInitialState().getName(i), Expression.BOOLEAN, new Boolean(information.getInitialState().getBooleanValue(i)));
					else
						v = new Value(information.getInitialState().getName(i), Expression.INT, new Integer(information.getInitialState().getIntValue(i)));
					
					initValuesModel.addValue(v);
				}
				catch(Exception e)
				{}
			}
		}
		
		// populate parameter text boxes
		approximationField.setText(""+information.getApprox());
		confidenceField.setText(""+information.getConfidence());
		iterationsField.setText(""+information.getNoIterations());
		// note: do this after above "setText"s because it triggers a doEnablesAndCalculations()
		automaticCalculateCombo.setSelectedIndex(information.getAutoIndex());
		
		pathLengthField.setText(""+information.getMaxPathLength());
		//distributedCheck.setSelected(information.isDistributed());
	}
	
	/** Call this static method to construct a new GUIValuesPicker to define
	 *  initialState.  If you don't want any default values, then pass in null for
	 *  initDefaults
	 */
	public static SimulationInformation defineSimulationWithDialog(GUIPrism parent, Values initDefaults, ModulesFile mf)
	{ return defineSimulationWithDialog(parent, initDefaults, mf, null); }
	public static SimulationInformation defineSimulationWithDialog(GUIPrism parent, Values initDefaults, ModulesFile mf, String titleExtra)
	{
		return new GUISimulationPicker(parent, initDefaults, mf, titleExtra).defineValues();
	}
	
	public SimulationInformation defineValues()
	{
		show();
		if(cancelled) return null;
		else return information;
	}
	
	private void okayButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okayButtonActionPerformed
	{//GEN-HEADEREND:event_okayButtonActionPerformed
		int i, n;
		Value c;
		
		doEnablesAndCalculations();
		
		if (initValuesTable.getCellEditor() != null) initValuesTable.getCellEditor().stopCellEditing();
			
		String parameter = "";
		try
		{
			// check simulation parameters ok
			parameter = "approximation";
			double approx = Double.parseDouble(approximationField.getText());
			if (approx <= 0) throw new NumberFormatException();
			parameter = "confidence";
			double confidence = Double.parseDouble(confidenceField.getText());
			if (confidence <= 0 || confidence >= 1) throw new NumberFormatException();
			parameter = "samples";
			int noIterations = Integer.parseInt(iterationsField.getText());
			if (noIterations <= 0) throw new NumberFormatException();
			parameter = "max path length";
			int maxPathLength = Integer.parseInt(pathLengthField.getText());
			if (maxPathLength <= 0) throw new NumberFormatException();
			
			// check each value of initial state
			Values newInitState = new Values();
			for(i = 0; i < initValuesModel.getNumValues(); i++)
			{
				parameter = initValuesModel.getValue(i).name;
				if(initValuesModel.getValue(i).type == Expression.INT)
					newInitState.addValue(initValuesModel.getValue(i).name, new Integer(initValuesModel.getValue(i).value.toString()));
				else
					newInitState.addValue(initValuesModel.getValue(i).name, new Boolean(initValuesModel.getValue(i).value.toString()));
			}
			information.setInitialState(newInitState);
			
			information.setApprox(approx);
			information.setConfidence(confidence);
			information.setNoIterations(noIterations);
			information.setMaxPathLength(maxPathLength);
			
			information.setAutoIndex(automaticCalculateCombo.getSelectedIndex());
			//information.setDistributed(distributedCheck.isSelected());
			
			cancelled = false;
			
			lastSimulationInformation = information;
			dispose();
		}
		catch(NumberFormatException e)
		{
			gui.errorDialog("Invalid number value entered for "+parameter+" parameter");
		}
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
		doEnablesAndCalculations();
	}
	
	public void keyTyped(KeyEvent e)
	{
		
	}

	class DefineValuesTable extends AbstractTableModel
	{
		ArrayList values;
		
		public DefineValuesTable()
		{
			values = new ArrayList();
		}
		
		public void addValue(Value v)
		{
			values.add(v);
			fireTableRowsInserted(values.size()-1, values.size()-1);
		}
		
		public int getNumValues()
		{
			return values.size();
		}
		
		public Value getValue(int i)
		{
			return (Value)values.get(i);
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
			
			Value v = (Value)values.get(rowIndex);
			switch(columnIndex)
			{
				case 0: return v.name;
				case 1:
					{
					switch(v.type)
					{
						case Expression.INT: return "int";
						case Expression.DOUBLE: return "double";
						case Expression.BOOLEAN: return "boolean";
						default: return "";
					}
				}
				case 2: return v.value.toString();
				default: return "";
			}
		}
		
		public String getColumnName(int columnIndex)
		{
			switch(columnIndex)
			{
				case 0: return "Name";
				case 1: return "Type";
				case 2: return "Value";
				default: return "";
			}
		}
			
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			if(columnIndex == 2) return true;
			else return false;
		}
			
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			if(columnIndex == 2)
			{
				Value v = (Value)values.get(rowIndex);
				String s = (String)aValue;
				v.value = s;
				fireTableCellUpdated(rowIndex, columnIndex);
			}
		}
			
		public String toString()
		{
			String str = "";
			for(int i = 0 ; i < values.size(); i++)
			{
				Value c = (Value)values.get(i);
				str+=c.toString();
				if(i!= values.size()-1) str+=",";
			}
			return str;
		}
		
	}
	
	class Value
	{
		String name;
		int type;
		Object value;
		
		public Value(String name, int type, Object value)
		{
			this.name = name;
			this.type = type;
			this.value = value;
		}
		
		public String toString()
		{
			return name+"="+value.toString();
		}
	}
}
