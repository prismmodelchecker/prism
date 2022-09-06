//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package userinterface.simulator.networking;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.table.*;
import java.util.regex.*;
import java.awt.*;

import userinterface.*;
import userinterface.properties.*;
import simulator.*;
import simulator.networking.*;
import parser.*;
import parser.ast.*;
import prism.*;
import settings.*;

public class GUISimulatorDistributionDialog extends javax.swing.JDialog implements Observer
{
	private SimulatorNetworkHandler network;
	private File netFile;
	private SimulatorEngine simulator;
	
	//The job to be done
	private boolean isExperiment;
	private GUIMultiProperties properties;
	private ModulesFile modulesFile;
	private PropertiesFile propertiesFile;
	private ArrayList props;
	private SimulationInformation info;
	private GUIExperiment expr;
	private UndefinedConstants undefinedConstants;
	private Thread experimentThread;
	private Expression experimentFormula;
	private boolean cancelled = false;
	
	private ArrayList propertyValues = null;
	
	//GUI for GUIProperties
	private JTable propertyTable;
	private PropertyResultsTable propResultsModel;
	
	
	/** Creates new form GUISimulatorDistributionDialog */
	public GUISimulatorDistributionDialog(java.awt.Frame parent, SimulatorEngine simulator, boolean modal)
	{
		super(parent, modal);
		setTitle("PRISM Distributed Simulator");
		
		this.simulator = simulator;
		
		initComponents();
		setLocationRelativeTo(getParent()); // centre
		
		
		SummaryEditorKit kit = new SummaryEditorKit();
		summaryText.setEditorKitForContentType("text/summary", kit);
		summaryText.setContentType("text/summary");
		summaryText.getDocument().putProperty( PlainDocument.tabSizeAttribute, new Integer(2) );
		summaryText.setSelectionColor(Color.white);
		summaryText.setEnabled(false);
		
		TableColumn col = hostsTable.getColumnModel().getColumn(3);
		col.setCellRenderer(new ProgressBarRenderer());
		
	}
	
	public void show(GUIExperiment expr, Thread exprThread, ModulesFile modulesFile, PropertiesFile properties, UndefinedConstants undefinedConstants, Expression propertyToCheck, SimulationInformation info) throws PrismException
	{
		this.isExperiment = true;
		this.modulesFile = modulesFile;
		this.propertiesFile = properties;
		this.experimentFormula = propertyToCheck;
		this.expr = expr;
		this.info = info;
		this.experimentThread = exprThread;
		this.undefinedConstants = undefinedConstants;
		//setup the interface
		PrismSettings settings = GUIPrism.getGUI().getPrism().getSettings();
		netFile = settings.getFile(PrismSettings.SIMULATOR_NETWORK_FILE);
		loadNetworkProfile(netFile);
		doHostTable();
		
		//write a summary for the summary box:
		StringBuffer summary = new StringBuffer("");
		summary.append("Experiment for property:\n\t"+propertyToCheck.toString()+"\n");
		
		summary.append("Defined Model Constants:\n\t"+undefinedConstants.getMFConstantValues()+"\n");
		summary.append("Property Constant Ranges:\n\t"+expr.getPFDefinedConstantsString()+"\n");
		summary.append("Total simulator iterations:\n\t"+info.getNumSamples()+"\n");
		summary.append("Simulator max. path length:\n\t"+info.getMaxPathLength()+"\n");
		summary.append("Initial state:\n\t"+info.getInitialState().toString()+"\n");
		summaryText.setText(summary.toString());
		
		JLabel label = new JLabel("No Results Display for Experiments");
		JPanel dummyPanel = new JPanel(new GridBagLayout());
		label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		label.setEnabled(false);
		dummyPanel.add(label);
		this.resultsScroller.setViewportView(dummyPanel);
		
		network.addObserver(this);
		
		//show the interface
		super.show();
		
		
		if(!cancelled)
		{
			ResultsCollection rc = expr.getResults();
			SimulatorResultsFile resultsFile = network.getResultsFile();
			if(propertyValues != null)
			{
				for(int i = 0; i < undefinedConstants.getNumPropertyIterations(); i++)
				{
					Values mcs = undefinedConstants.getMFConstantValues();
					Values pcs = (Values)propertyValues.get(i);
					double res = resultsFile.getResult(i);
					Object result = (res < 0.0)?null:new Double(res);
					rc.setResult(mcs, pcs, result);
					
				}
			}
		}
	}
	
	public void show(GUIMultiProperties properties, ModulesFile modulesFile, PropertiesFile propertiesFile, ArrayList props, SimulationInformation info) throws PrismException
	{
		this.isExperiment = false;
		this.properties = properties;
		this.modulesFile =modulesFile;
		this.propertiesFile = propertiesFile;
		this.props = props;
		this.info = info;
		
		//setup the interface
		PrismSettings settings = GUIPrism.getGUI().getPrism().getSettings();
		netFile = settings.getFile(PrismSettings.SIMULATOR_NETWORK_FILE);
		loadNetworkProfile(netFile);
		doHostTable();
		//write a summary for the summary box
		StringBuffer summary = new StringBuffer("");
		summary.append("Verifying "+props.size()+ ((props.size() == 1)?(" property:\n"):(" properties:\n")));
		String constantsStr = "";
		for(int i = 0; i < props.size(); i++)
		{
			if(props.get(i) instanceof GUIProperty)
			{
				GUIProperty form = (GUIProperty)props.get(i);
				summary.append("\t"+form.getProperty().toString()+"\n");
				constantsStr = form.getConstantsString();
			}
			else
			{
				summary.append("form"+props.get(i).getClass().toString());
			}
		}
		//summary.append("Defined Constants:\n\t"+constantsStr+"\n");
		summary.append("Total simulator iterations:\n\t"+info.getNumSamples()+"\n");
		summary.append("Simulator max. path length:\n\t"+info.getMaxPathLength()+"\n");
		summary.append("Initial state:\n\t"+info.getInitialState().toString()+"\n");
		summaryText.setText(summary.toString());
		
		propResultsModel = new PropertyResultsTable();
		
		propertyTable = new JTable(propResultsModel);
		
		resultsScroller.setViewportView(propertyTable);
		
		//show the interface
		super.show();
		
		if(!cancelled)
		{
			for(int i = 0; i < props.size(); i++)
			{
				GUIProperty guiProp = (GUIProperty)props.get(i);
				double result = network.getResultsFile().getResult(i);
				if(result >= 0.0)
				{
					guiProp.setResult(new Result(result));
					guiProp.setMethodString("Distributed Simulation");
					guiProp.setConstants(undefinedConstants.getMFConstantValues(), undefinedConstants.getPFConstantValues());
				}
			}
		}
	}
	
	private void loadNetworkProfile(File file) throws PrismException
	{
		if(network != null)network.deleteObserver(this);
		network = new SimulatorNetworkHandler();
		network.addObserver(this);
		network.getResultsFile().addObserver(this);
		if(file!=null)
		{
			try
			{
				network.loadNetworkFromXML(file);
			}
			catch(PrismException e)
			{
				try
				{
					network.setName("No Profile Loaded");
				}
				catch(SettingException ee)
				{
					//do nothing
				}
			}
		}
		else
		{
			try
			{
				network.setName("No Profile Loaded");
			}
			catch(SettingException ee)
			{
				//do nothing
			}
		}
		networkProfileField.setText(network.getName());
	}
	
	private void doHostTable()
	{
		hostsTable.setModel(network);
		hostsTable.repaint();
		hostsTable.setFont(new Font("monospaced", Font.PLAIN, 12));
		TableColumn col = hostsTable.getColumnModel().getColumn(3);
		col.setCellRenderer(new ProgressBarRenderer());
	}
	
	private void doEnables()
	{
		int state = network.getState();
		
		editButton.setEnabled(state != SimulatorNetworkHandler.RUNNING);
		testButton.setEnabled(state != SimulatorNetworkHandler.RUNNING);
		feedbackIterationsProgressRadio.setEnabled(state != SimulatorNetworkHandler.RUNNING);
		feedbackResultsProgressRadio.setEnabled(state != SimulatorNetworkHandler.RUNNING);
		feedbackWhenFinishedRadio.setEnabled(state != SimulatorNetworkHandler.RUNNING);
		feedbackResultsWhenFinishedRadio.setEnabled(state != SimulatorNetworkHandler.RUNNING);
		
		startButton.setEnabled(state != SimulatorNetworkHandler.RUNNING);
		stopButton.setEnabled(state == SimulatorNetworkHandler.RUNNING);
		cancelButton.setEnabled(state != SimulatorNetworkHandler.RUNNING);
		doneButton.setEnabled(network.countIterationsDone() > 0 && state != SimulatorNetworkHandler.RUNNING);
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jPanel3 = new javax.swing.JPanel();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        doneButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel12 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        networkProfileField = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        editButton = new javax.swing.JButton();
        testButton = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        summaryPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        summaryText = new javax.swing.JEditorPane();
        jPanel9 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        feedbackIterationsProgressRadio = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        feedbackWhenFinishedRadio = new javax.swing.JRadioButton();
        jPanel10 = new javax.swing.JPanel();
        feedbackResultsProgressRadio = new javax.swing.JRadioButton();
        jPanel11 = new javax.swing.JPanel();
        resultsScroller = new javax.swing.JScrollPane();
        feedbackResultsWhenFinishedRadio = new javax.swing.JRadioButton();
        jPanel13 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jPanel15 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        hostsTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        startButton.setText("Start Networking");
        startButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                startButtonActionPerformed(evt);
            }
        });

        jPanel3.add(startButton);

        stopButton.setText("Stop Networking");
        stopButton.setPreferredSize(new java.awt.Dimension(140, 25));
        stopButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                stopButtonActionPerformed(evt);
            }
        });

        jPanel3.add(stopButton);

        doneButton.setText("Done");
        doneButton.setPreferredSize(new java.awt.Dimension(75, 25));
        doneButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                doneButtonActionPerformed(evt);
            }
        });

        jPanel3.add(doneButton);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelButtonActionPerformed(evt);
            }
        });

        jPanel3.add(cancelButton);

        getContentPane().add(jPanel3, java.awt.BorderLayout.SOUTH);

        jSplitPane1.setDividerLocation(290);
        jSplitPane1.setDividerSize(5);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setPreferredSize(new java.awt.Dimension(900, 650));
        jPanel12.setLayout(new javax.swing.BoxLayout(jPanel12, javax.swing.BoxLayout.X_AXIS));

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Network Profile:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        jPanel2.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel2.add(jPanel4, gridBagConstraints);

        networkProfileField.setEditable(false);
        networkProfileField.setMinimumSize(new java.awt.Dimension(150, 19));
        networkProfileField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel2.add(networkProfileField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel2.add(jPanel6, gridBagConstraints);

        editButton.setText("Change");
        editButton.setMinimumSize(new java.awt.Dimension(82, 19));
        editButton.setPreferredSize(new java.awt.Dimension(82, 19));
        editButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                editButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        jPanel2.add(editButton, gridBagConstraints);

        testButton.setText("Test");
        testButton.setMaximumSize(new java.awt.Dimension(78, 25));
        testButton.setMinimumSize(new java.awt.Dimension(78, 19));
        testButton.setPreferredSize(new java.awt.Dimension(78, 19));
        testButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                testButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        jPanel2.add(testButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        jPanel2.add(jPanel7, gridBagConstraints);

        jPanel8.setLayout(new java.awt.BorderLayout());

        jPanel8.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), "Summary"));
        jPanel8.setPreferredSize(new java.awt.Dimension(300, 110));
        summaryPanel.setLayout(new java.awt.BorderLayout());

        summaryPanel.setBackground(new java.awt.Color(255, 255, 255));
        summaryPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        summaryText.setEditable(false);
        summaryText.setMinimumSize(new java.awt.Dimension(100, 21));
        summaryText.setPreferredSize(new java.awt.Dimension(100, 21));
        jScrollPane2.setViewportView(summaryText);

        summaryPanel.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jPanel8.add(summaryPanel, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(jPanel8, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        jPanel2.add(jPanel9, gridBagConstraints);

        jPanel12.add(jPanel2);

        jPanel14.setLayout(new java.awt.GridBagLayout());

        feedbackIterationsProgressRadio.setSelected(true);
        feedbackIterationsProgressRadio.setText("Progressively");
        buttonGroup1.add(feedbackIterationsProgressRadio);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel14.add(feedbackIterationsProgressRadio, gridBagConstraints);

        jLabel2.setText("Hosts Update:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 1;
        jPanel14.add(jLabel2, gridBagConstraints);

        jPanel5.setPreferredSize(new java.awt.Dimension(30, 10));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 0;
        jPanel14.add(jPanel5, gridBagConstraints);

        feedbackWhenFinishedRadio.setText("When Finished");
        buttonGroup1.add(feedbackWhenFinishedRadio);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel14.add(feedbackWhenFinishedRadio, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        jPanel14.add(jPanel10, gridBagConstraints);

        feedbackResultsProgressRadio.setSelected(true);
        feedbackResultsProgressRadio.setText("Progressively");
        buttonGroup2.add(feedbackResultsProgressRadio);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 13;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel14.add(feedbackResultsProgressRadio, gridBagConstraints);

        jPanel11.setLayout(new java.awt.BorderLayout());

        jPanel11.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), "Results"));
        resultsScroller.setMaximumSize(null);
        resultsScroller.setPreferredSize(null);
        jPanel11.add(resultsScroller, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel14.add(jPanel11, gridBagConstraints);

        feedbackResultsWhenFinishedRadio.setText("When Finished");
        buttonGroup2.add(feedbackResultsWhenFinishedRadio);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 13;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel14.add(feedbackResultsWhenFinishedRadio, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 12;
        gridBagConstraints.gridy = 0;
        jPanel14.add(jPanel13, gridBagConstraints);

        jLabel3.setText("Results Update:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 11;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel14.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 15;
        gridBagConstraints.gridy = 5;
        jPanel14.add(jPanel15, gridBagConstraints);

        jPanel12.add(jPanel14);

        jSplitPane1.setTopComponent(jPanel12);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(new javax.swing.border.TitledBorder("SSH Hosts"));
        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));
        hostsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String []
            {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        hostsTable.setGridColor(new java.awt.Color(204, 204, 204));
        jScrollPane1.setViewportView(hostsTable);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jSplitPane1.setRightComponent(jPanel1);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        pack();
    }//GEN-END:initComponents
	
	private void testButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_testButtonActionPerformed
	{//GEN-HEADEREND:event_testButtonActionPerformed
		network.doTesting();
	}//GEN-LAST:event_testButtonActionPerformed
	
	private void editButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_editButtonActionPerformed
	{//GEN-HEADEREND:event_editButtonActionPerformed
		try
		{
			GUINetworkEditor edit = new GUINetworkEditor(null, network);
			hostsTable.repaint();
			netFile = edit.getFile(null, netFile);
			loadNetworkProfile(netFile);
			doHostTable();
		}
		catch(PrismException e)
		{
			//TODO something
		}
	}//GEN-LAST:event_editButtonActionPerformed
	
	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelButtonActionPerformed
	{//GEN-HEADEREND:event_cancelButtonActionPerformed
		cancelled = true;
		hide();
	}//GEN-LAST:event_cancelButtonActionPerformed
	
	private void doneButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_doneButtonActionPerformed
	{//GEN-HEADEREND:event_doneButtonActionPerformed
		if(network.countIterationsDone() < info.getNumSamples())
		{
			String[] selection =
			{"Yes", "No", "Cancel"};
			int selectionNo = -1;
			selectionNo = JOptionPane.showOptionDialog(this, "Not enough iterations have been performed.\nDo you want to use the available data?\n(This may decrease the accuracy of the results)", "Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, selection, selection[0]);
			switch(selectionNo)
			{
				case 0: hide(); break;
				case 1: 
				{
					cancelled = true;
					hide();
				}
				case 2: //do nothing
			}
		}
	}//GEN-LAST:event_doneButtonActionPerformed
	
	private void startButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_startButtonActionPerformed
	{//GEN-HEADEREND:event_startButtonActionPerformed
		
		//First step is to build and export the simulator engine binary to
		//do the work.
		if(!isExperiment)
		{
			try
			{
				ArrayList propFormulae = new ArrayList();
				for(int i = 0 ; i < props.size(); i++)
				{
					GUIProperty guiProp = (GUIProperty)props.get(i);
					propFormulae.add(guiProp.getProperty());
				}
				System.out.println("doing 1");
				if(modulesFile == null) System.out.println("modulesfile is null");
				if(propertiesFile == null) System.out.println("propertiesfile is null");
				if(props == null) System.out.println("props is null");
				if(info == null) System.out.println("info is null");
				
				File binfile = File.createTempFile("binary"+System.currentTimeMillis(),".bin");
				System.out.println("This is the file path: "+binfile.getPath());
				//TODO: re-enable?
				//simulator.exportBinaryForMultipleProperties(modulesFile, propertiesFile, propFormulae, info.getInitialState(), binfile.getPath());
				System.out.println("doing 2");
				
				network.doNetworking(info.getNumSamples(), info.getMaxPathLength(), binfile, feedbackIterationsProgressRadio.isSelected(), feedbackResultsProgressRadio.isSelected());
				
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.out.println("ewrewoioewrrew" +e.getMessage());//TODO show dialog
			}
		}
		else
		{
			try
			{
				File binfile = File.createTempFile("binary"+System.currentTimeMillis(),".bin");
				
				System.out.println("This is the file path: "+binfile.getPath());
				//TODO: re-enable?
				//propertyValues = simulator.exportBinaryForExperiment(expr, experimentThread, modulesFile, propertiesFile, undefinedConstants, experimentFormula, info.getInitialState(), binfile.getPath());
				
				network.doNetworking(info.getNumSamples(), info.getMaxPathLength(), binfile, feedbackIterationsProgressRadio.isSelected(), feedbackResultsProgressRadio.isSelected(), undefinedConstants.getMFConstantValues(), propertyValues, expr.getResults());
				
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.out.println("ERROR 2 "+e.getMessage());
				//TODO show dialog
			}
		}
		
		
	}//GEN-LAST:event_startButtonActionPerformed
	
	private void stopButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_stopButtonActionPerformed
	{//GEN-HEADEREND:event_stopButtonActionPerformed
		try
		{
			network.stopNetworking();
		}
		catch(PrismException eeee)
		{
			//TODO show a dialog
		}
	}//GEN-LAST:event_stopButtonActionPerformed
	
	public void update(Observable o, Object arg)
	{
		doEnables();
		
		if(propResultsModel != null)propResultsModel.updateTable();
	}
	
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton doneButton;
    private javax.swing.JButton editButton;
    private javax.swing.JRadioButton feedbackIterationsProgressRadio;
    private javax.swing.JRadioButton feedbackResultsProgressRadio;
    private javax.swing.JRadioButton feedbackResultsWhenFinishedRadio;
    private javax.swing.JRadioButton feedbackWhenFinishedRadio;
    private javax.swing.JTable hostsTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextField networkProfileField;
    private javax.swing.JScrollPane resultsScroller;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JPanel summaryPanel;
    private javax.swing.JEditorPane summaryText;
    private javax.swing.JButton testButton;
    // End of variables declaration//GEN-END:variables
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	class ProgressBarRenderer implements TableCellRenderer
	{
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			//System.out.println("called getTableCelelsdikfjlinesoginweosgnoesignseoigneosi gn");
			if(value instanceof JProgressBar)
			{
				JProgressBar b = new JProgressBar();
				if(isSelected)
				{
					b.setBackground(hostsTable.getSelectionBackground());
				}
				else
				{
					b.setBackground(hostsTable.getBackground());
				}
				return (JProgressBar)value;
			}
			else return new JPanel();
		}
	}
	
	
	
	
	
	
	class SummaryEditorKit extends DefaultEditorKit
	{
		
		private SummaryContext preferences;
		/** Creates a new instance of PrismEditorKit */
		public SummaryEditorKit()
		{
			super();
		}
		
		public SummaryContext getStylePreferences()
		{
			if (preferences == null)
			{
				preferences = new SummaryContext();
			}
			return preferences;
		}
		
		public void setStylePreferences(SummaryContext prefs)
		{
			preferences = prefs;
		}
		
		public String getContentType()
		{
			return "text/summary";
		}
		
		public Document createDefaultDocument()
		{
			return new PlainDocument();
		}
		
		public final ViewFactory getViewFactory()
		{
			return getStylePreferences();
		}
		
		
	}
	
	class SummaryContext extends StyleContext implements ViewFactory
	{
		
		
		
		public static final String COMMENT_D = "Single Line Comment";
		
		/** Creates a new instance of PrismContext */
		public SummaryContext()
		{
			super();
		}
		
		public View create(Element elem)
		{
			return new PepaView(elem);
		}
		
		
	}
	
	userinterface.model.Style PLAIN_S = new userinterface.model.Style(Color.black, Font.PLAIN);
	userinterface.model.Style BOLD_S = new userinterface.model.Style(Color.black, Font.BOLD);
	
	class PepaView extends PlainView
	{
		
		private Matcher match;
		private Pattern pattern;
		public PepaView(Element elem)
		{
			super(elem);
			
			pattern = Pattern.compile("%.*");
		}
		
		public void paint(Graphics g, Shape a)
		{
			super.paint(g, a);
		}
		
		
		protected int drawUnselectedText(Graphics g, int x, int y,int p0, int p1) throws BadLocationException
		{
			int stLine = findStartOfLine(p0, getDocument());
			int enLine = findEndOfLine(Math.max(p0, p1-1), getDocument());
			//System.out.println("p0 = "+p0+", p1 = "+p1+", st = "+stLine+", enLine = "+enLine+".");
			try
			{
				g.setColor(Color.green);
				Document doc = getDocument();
				Segment segment = getLineBuffer();
				
				
				//String s = doc.getText(p0, p1-p0);
				String s = doc.getText(stLine, enLine-stLine);
				userinterface.model.Style[] styles = highlight(s, (p0-stLine), Math.max(0,(p1-p0-1)));
				int currStart = 0;
				int currEnd = 0;
				Color last = null;
				String fname = summaryText.getFont().getName();
				int fsize = summaryText.getFont().getSize();
				
				for(int curr = 0; curr < styles.length; curr++)
				{
					
					userinterface.model.Style c = styles[curr];
					
					g.setColor(c.c);
					g.setFont(new Font(fname, c.style, fsize));
					Segment segm = getLineBuffer();
					doc.getText(p0+curr, 1, segm);
					x = Utilities.drawTabbedText(segm, x, y, g, this, p0+curr);
					
				}
				g.setColor(Color.black);
				g.setFont(new Font(fname, Font.PLAIN, fsize));
			}
			catch(BadLocationException ex)
			{
				//System.out.println("ex = "+ex);
				//ex.printStackTrace();
			}
			return x;
		}
		
		protected int drawSelectedText(Graphics g, int x, int y,int p0, int p1) throws BadLocationException
		{
			int stLine = findStartOfLine(p0, getDocument());
			int enLine = findEndOfLine(Math.max(p0, p1-1), getDocument());
			//System.out.println("p0 = "+p0+", p1 = "+p1+", st = "+stLine+", enLine = "+enLine+".");
			try
			{
				g.setColor(Color.green);
				Document doc = getDocument();
				Segment segment = getLineBuffer();
				
				
				//String s = doc.getText(p0, p1-p0);
				String s = doc.getText(stLine, enLine-stLine);
				userinterface.model.Style[] styles = highlight(s, (p0-stLine), Math.max(0,(p1-p0-1)));
				int currStart = 0;
				int currEnd = 0;
				Color last = null;
				String fname = summaryText.getFont().getName();
				int fsize = summaryText.getFont().getSize();
				
				for(int curr = 0; curr < styles.length; curr++)
				{
					
					userinterface.model.Style c = styles[curr];
					
					g.setColor(c.c);
					g.setFont(new Font(fname, c.style, fsize));
					Segment segm = getLineBuffer();
					doc.getText(p0+curr, 1, segm);
					x = Utilities.drawTabbedText(segm, x, y, g, this, p0+curr);
					
				}
				g.setColor(Color.black);
				g.setFont(new Font(fname, Font.PLAIN, fsize));
			}
			catch(BadLocationException ex)
			{
				//System.out.println("ex = "+ex);
				//ex.printStackTrace();
			}
			return x;
		}
		
		private synchronized userinterface.model.Style[] highlight(String s, int offset, int length)
		{
			//System.out.println("s = *"+s+"*");
			userinterface.model.Style[] styles = new userinterface.model.Style[s.length()];
			for(int i = 0; i < styles.length; i++)
				styles[i] = PLAIN_S;
			
			int index = s.indexOf(':');
			
			if(index != -1 && !s.startsWith("\t"))
			{
				for(int i = 0; i <= index; i++)
				{
					styles[i] = BOLD_S;
				}
			}
			
			//System.out.println("styles.length = "+styles.length);
			//System.out.println("ret.length = "+length);
			//System.out.println("offset = "+offset);
			userinterface.model.Style[]ret = new userinterface.model.Style[length];
			for(int i = 0; i < ret.length; i++)
			{
				ret[i] = styles[i+offset];
			}
			
			return ret;
		}
		
		
		private synchronized int findStartOfLine(int p0, Document d)
		{
			int index = p0;
			String s = "";
			try
			{
				s = d.getText(index, 1);
			}
			catch(BadLocationException e)
			{
				return 0;
			}
			index--;
			if(!(!s.equals("\n") && index >= -1)) index--;//botch of the century, an alternative good code
			while(!s.equals("\n") && index >= -1)
			{
				try
				{
					s = d.getText(index, 1);
				}
				catch(BadLocationException e)
				{
					return 0;
				}
				index--;
			}
			index+=2;
			return index;
		}
		
		private synchronized int findEndOfLine(int p1, Document d)
		{
			int index = p1;
			String s = "";
			try
			{
				s = d.getText(index, 1);
			}
			catch(BadLocationException e)
			{
				return d.getLength();
			}
			index++;
			while(!s.equals("\n") && index <= d.getLength())
			{
				try
				{
					s = d.getText(index, 1);
				}
				catch(BadLocationException e)
				{
					return d.getLength()-1;
				}
				index++;
			}
			index--;
			return index;
		}
		
		
		
		
		
		
		
	}
	
	class PropertyResultsTable extends AbstractTableModel
	{
		String[][]data;
		public PropertyResultsTable()
		{
			super();
			data = new String[props.size()][4];
			for(int i = 0; i < props.size(); i++)
			{
				data[i][0] = ((GUIProperty)props.get(i)).getPropString();
				data[i][1] = "";
				data[i][2] = "";
				data[i][3] = "";
			}
		}
		
		public int getColumnCount()
		{
			return 4;
		}
		
		public int getRowCount()
		{
			if(props != null)
				return props.size();
			else return 0;
		}
		
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			return data[rowIndex][columnIndex];
		}
		
		public void updateTable()
		{
			SimulatorResultsFile srf = network.getResultsFile();
			for(int i = 0 ; i < props.size(); i++)
			{
				data[i][1] = (srf.getSum(i)<0)?"":""+srf.getSum(i);
				data[i][2] = (srf.getIterations(i)<0)?"":""+srf.getIterations(i);
				data[i][3] = (srf.getResult(i)<0)?"?":""+srf.getResult(i);
			}
			
			fireTableDataChanged();
		}
		
		public String getColumnName(int column)
		{
			switch(column)
			{
				case 0: return "Property";
				case 1: return "Sum";
				case 2: return "No. Samples";
				case 3: return "Result";
				default: return "";
			}
		}
		
	}
	
}
