//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package userinterface.properties;

import java.util.Vector;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;
import org.jfree.data.xy.*;

import userinterface.*;
import userinterface.graph.Graph;
import userinterface.graph.GraphResultListener;
import prism.*;
import parser.*;
import parser.type.TypeInterval;

public class GUIGraphPicker extends javax.swing.JDialog
{
	private GUIPrism gui;
	private GUIPlugin plugin;

	private GUIExperiment experiment;
	private GUIGraphHandler graphHandler;
	private ResultsCollection resultsCollection;

	private GraphConstantPickerList pickerList;

	private String ranger;
	private DefinedConstant rangingConstant;

	private Values otherValues;
	private Vector<DefinedConstant> multiSeries;

	private userinterface.graph.Graph graphModel;
	private boolean graphCancelled;

	private static final int MAX_NUM_SERIES_BEFORE_QUERY = 11;

	/** Creates new form GUIGraphPicker 
	 * 
	 * @param parent The parent.
	 * @param plugin The GUIPlugin (GUIMultiProperties)
	 * @param experiment The experiment for which to plot a graph.
	 * @param graphHandler The graph handler in which to display the graph.
	 * @param resultsKnown If true, simply plot existing results (experiment has been done). 
	 * If false, attach listeners to the results such that plot is made when results become available.
	 */
	public GUIGraphPicker(GUIPrism parent, GUIPlugin plugin, GUIExperiment experiment, GUIGraphHandler graphHandler, boolean resultsKnown)
	{
		super(parent, true);
		setTitle("New Graph Series");

		this.gui = parent;
		this.plugin = plugin;

		this.experiment = experiment;
		this.graphHandler = graphHandler;
		this.resultsCollection = experiment.getResults();

		// graphCancelled will be set explicitly to false when the OK button is pressed
		// (this means if the user closes the dialog, this counts as a cancel)
		this.graphCancelled = true;

		this.multiSeries = new Vector<DefinedConstant>();

		initComponents();
		setResizable(false);

		init();
		setLocationRelativeTo(getParent()); // centre
		getRootPane().setDefaultButton(lineOkayButton);

		/* Wait untill OK or Cancel is pressed. */
		setVisible(true);

		/* If OK was pressed. */
		if (!graphCancelled) {
			/* Collect series keys. */
			Vector<Graph.SeriesKey> seriesKeys = new Vector<Graph.SeriesKey>();

			/* Collect series Values */
			ArrayList<Values> seriesValues = new ArrayList<Values>();

			/* Add single constant values to each serie */
			seriesValues.add(otherValues);

			for (int i = 0; i < multiSeries.size(); i++) {
				ArrayList<Values> temp = (ArrayList<Values>) seriesValues.clone();
				seriesValues.clear();

				// For each of the possible value in the range
				for (int j = 0; j < multiSeries.get(i).getNumSteps(); j++) {
					// Clone the list
					ArrayList copy = (ArrayList<Values>) temp.clone();

					// For each element in the list
					for (int k = 0; k < copy.size(); k++) {
						Values v = new Values();
						Values cp = (Values) copy.get(k);
						v.addValues(cp);
						v.addValue(multiSeries.get(i).getName(), multiSeries.get(i).getValue(j));
						seriesValues.add(v);
					}
				}
			}

			/* Do all series settings. */
			for (int serie = 0; serie < seriesValues.size(); serie++) //each combination of series
			{
				Values values = seriesValues.get(serie);
				String seriesName = (seriesValues.size() > 1) ? values.toString() : seriesNameField.getText();
				// For properties that return an interval, we add a pair of series
				// (the pair is stored as a linked list)
				if (experiment.getPropertyType() instanceof TypeInterval) {
					Graph.SeriesKey key = graphModel.addSeries(seriesName + " (min)");
					key.next = graphModel.addSeries(seriesName + " (max)");
					seriesKeys.add(key);
				} else {
					seriesKeys.add(graphModel.addSeries(seriesName));
				}
			}

			/* If there are results already, then lets render them! */
			if (resultsKnown && resultsCollection.getCurrentIteration() > 0) {
				for (int series = 0; series < seriesValues.size(); series++) //each combination of series
				{
					Values values = seriesValues.get(series);
					Graph.SeriesKey seriesKey = seriesKeys.get(series);

					/** Range over x-axis. */
					for (int i = 0; i < rangingConstant.getNumSteps(); i++) {
						Object value = rangingConstant.getValue(i);

						/** Values used in the one experiment for this series. */
						Values useThis = new Values();
						useThis.addValues(values);
						useThis.addValue(ranger, value);

						/** Get this particular result. **/
						try {
							Object result = resultsCollection.getResult(useThis);

							double x = 0, y = 0;
							boolean validX = true;

							if (value instanceof Double) {
								x = ((Double) value).doubleValue();
							} else if (value instanceof Integer) {
								x = ((Integer) value).intValue();
							} else {
								validX = false;
							}

							// Add point to graph (if of valid type)
							if (validX) {
								if (result instanceof Double) {
									y = ((Double) result).doubleValue();
									graphModel.addPointToSeries(seriesKey, new XYDataItem(x, y));
								} else if (result instanceof Integer) {
									y = ((Integer) result).intValue();
									graphModel.addPointToSeries(seriesKey, new XYDataItem(x, y));
								} else if (result instanceof Interval) {
									Interval interval = (Interval) result;
									if (interval.lower instanceof Double) {
										y = ((Double) interval.lower).doubleValue();
										graphModel.addPointToSeries(seriesKey, new XYDataItem(x, y));
										y = ((Double) interval.upper).doubleValue();
										graphModel.addPointToSeries(seriesKey.next, new XYDataItem(x, y));
									} else if (result instanceof Integer) {
										y = ((Integer) interval.lower).intValue();
										graphModel.addPointToSeries(seriesKey, new XYDataItem(x, y));
										y = ((Integer) interval.upper).intValue();
										graphModel.addPointToSeries(seriesKey.next, new XYDataItem(x, y));
									}
								}
							}
						} catch (PrismException pe) {
							// No result found. 
						}
					}
				}
			} else if (!resultsKnown && resultsCollection.getCurrentIteration() == 0) {
				for (int series = 0; series < seriesValues.size(); series++) //each combination of series
				{
					Values values = seriesValues.get(series);
					Graph.SeriesKey seriesKey = seriesKeys.get(series);

					GraphResultListener listener = new GraphResultListener(graphModel, seriesKey, ranger, values);
					resultsCollection.addResultListener(listener);
				}
			}
		}
	}

	/** According to what is stored in 'rc', set up the table to pick the constants
	 */
	private void init()
	{
		// set up "define other constants" table
		// create header
		GraphConstantHeader header = new GraphConstantHeader();
		constantTablePanel.add(header, BorderLayout.NORTH);
		// create scroller
		JScrollPane scroller = new JScrollPane();
		constantTablePanel.add(scroller, BorderLayout.CENTER);
		// create picker list
		pickerList = new GraphConstantPickerList();
		scroller.setViewportView(pickerList);

		// for each ranging constant in rc, add:
		// (1) a row in the picker list
		// (2) an item in the "x axis" drop down menu
		for (int i = 0; i < resultsCollection.getRangingConstants().size(); i++) {
			DefinedConstant dc = (DefinedConstant) resultsCollection.getRangingConstants().get(i);
			pickerList.addConstant(new GraphConstantLine(dc, this));
			this.selectAxisConstantCombo.addItem(dc.getName());
		}

		// select the default constant for the x axis
		// (first property if there is one, if not first model one)
		if (selectAxisConstantCombo.getItemCount() > 0) {
			if (resultsCollection.getNumPropertyRangingConstants() > 0)
				selectAxisConstantCombo.setSelectedIndex(resultsCollection.getNumModelRangingConstants());
			else
				selectAxisConstantCombo.setSelectedIndex(0);
		}
		// and disable it in the picker list
		pickerList.disableLine(0);

		// if there is only one ranging constant, disable controls
		if (resultsCollection.getRangingConstants().size() == 1) {
			selectAxisConstantCombo.setEnabled(false);
			pickerList.setEnabled(false);
			header.setEnabled(false);
			this.middleLabel.setEnabled(false);
			this.topComboLabel.setEnabled(false);
		}

		// default graph option is "new graph"
		this.newGraphRadio.setSelected(true);

		// add existing graphs to choose from
		for (int i = 0; i < graphHandler.getNumModels(); i++) {
			existingGraphCombo.addItem(graphHandler.getGraphName(i));
		}
		// default to latest one
		if (existingGraphCombo.getItemCount() > 0) {
			existingGraphCombo.setSelectedIndex(existingGraphCombo.getItemCount() - 1);
		}
		// if there are no graphs, disable control
		else {
			existingGraphCombo.setEnabled(false);
			this.existingGraphRadio.setEnabled(false);
		}

		// create a default series name
		resetAutoSeriesName();

		// other enables/disables
		doEnables();

		pack();
	}

	public void doEnables()
	{
		this.existingGraphCombo.setEnabled(this.existingGraphRadio.isSelected());
	}

	// create a default series name
	public void resetAutoSeriesName()
	{
		DefinedConstant temp;
		Object value;

		if (selectAxisConstantCombo.getSelectedItem() == null) {
			return;
		}

		// see which constant is on x axis
		ranger = selectAxisConstantCombo.getSelectedItem().toString();
		// init arrays
		otherValues = new Values();
		multiSeries = new Vector<DefinedConstant>();
		// go through constants in picker list
		for (int j = 0; j < pickerList.getNumConstants(); j++) {
			// get constant
			temp = pickerList.getConstantLine(j).getDC();
			// ignore constant for x-axis
			if (temp.getName().equals(ranger))
				continue;
			// get value
			value = pickerList.getConstantLine(j).getSelectedValue();
			// if we find any constants selected "All Series", clear name, disable and bail out
			if (value instanceof String) {
				this.seriesNameLabel.setEnabled(false);
				this.seriesNameField.setText("");
				this.seriesNameField.setEnabled(false);
				return;
			}
			// we add other constants to a list
			else {
				otherValues.addValue(temp.getName(), value);
			}
		}
		// use values object string for name
		if (otherValues.getNumValues() != 0) {
			this.seriesNameField.setText(otherValues.toString());
		} else {
			this.seriesNameField.setText("New Series");
		}
		this.seriesNameLabel.setEnabled(true);
		this.seriesNameField.setEnabled(true);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	private void initComponents()
	{
		java.awt.GridBagConstraints gridBagConstraints;

		buttonGroup1 = new javax.swing.ButtonGroup();
		jTabbedPane1 = new javax.swing.JTabbedPane();
		jPanel1 = new javax.swing.JPanel();
		jPanel3 = new javax.swing.JPanel();
		jPanel5 = new javax.swing.JPanel();
		topComboLabel = new javax.swing.JLabel();
		jPanel6 = new javax.swing.JPanel();
		selectAxisConstantCombo = new javax.swing.JComboBox();
		jPanel7 = new javax.swing.JPanel();
		middleLabel = new javax.swing.JLabel();
		constantTablePanel = new javax.swing.JPanel();
		jPanel9 = new javax.swing.JPanel();
		jPanel10 = new javax.swing.JPanel();
		jLabel3 = new javax.swing.JLabel();
		newGraphRadio = new javax.swing.JRadioButton();
		existingGraphRadio = new javax.swing.JRadioButton();
		jPanel11 = new javax.swing.JPanel();
		existingGraphCombo = new javax.swing.JComboBox();
		jPanel12 = new javax.swing.JPanel();
		seriesNameLabel = new javax.swing.JLabel();
		seriesNameField = new javax.swing.JTextField();
		jPanel4 = new javax.swing.JPanel();
		lineOkayButton = new javax.swing.JButton();
		lineCancelButton = new javax.swing.JButton();
		jPanel2 = new javax.swing.JPanel();

		addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(java.awt.event.WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.LEFT);
		jPanel1.setLayout(new java.awt.BorderLayout());

		jPanel1.setBorder(new javax.swing.border.TitledBorder("Line Graph"));
		jPanel1.setFocusable(false);
		jPanel1.setEnabled(false);
		jPanel3.setLayout(new java.awt.GridBagLayout());

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		jPanel3.add(jPanel5, gridBagConstraints);

		topComboLabel.setText("Select x axis constant:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel3.add(topComboLabel, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		jPanel3.add(jPanel6, gridBagConstraints);

		selectAxisConstantCombo.setPreferredSize(new java.awt.Dimension(100, 24));
		selectAxisConstantCombo.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				selectAxisConstantComboActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		jPanel3.add(selectAxisConstantCombo, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		jPanel3.add(jPanel7, gridBagConstraints);

		middleLabel.setText("Define other constants:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel3.add(middleLabel, gridBagConstraints);

		constantTablePanel.setLayout(new java.awt.BorderLayout());

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.gridwidth = 3;
		gridBagConstraints.gridheight = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		jPanel3.add(constantTablePanel, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 6;
		gridBagConstraints.gridy = 0;
		jPanel3.add(jPanel9, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 6;
		jPanel3.add(jPanel10, gridBagConstraints);

		jLabel3.setText("Add Series to:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel3.add(jLabel3, gridBagConstraints);

		newGraphRadio.setText("New Graph");
		buttonGroup1.add(newGraphRadio);
		newGraphRadio.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				newGraphRadioActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel3.add(newGraphRadio, gridBagConstraints);

		existingGraphRadio.setText("Existing Graph");
		buttonGroup1.add(existingGraphRadio);
		existingGraphRadio.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				existingGraphRadioActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel3.add(existingGraphRadio, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 4;
		gridBagConstraints.gridy = 0;
		jPanel3.add(jPanel11, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 5;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel3.add(existingGraphCombo, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 9;
		jPanel3.add(jPanel12, gridBagConstraints);

		seriesNameLabel.setText("Series name:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 10;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel3.add(seriesNameLabel, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 10;
		gridBagConstraints.gridwidth = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		jPanel3.add(seriesNameField, gridBagConstraints);

		jPanel1.add(jPanel3, java.awt.BorderLayout.CENTER);

		jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

		lineOkayButton.setText("Okay");
		lineOkayButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				lineOkayButtonActionPerformed(evt);
			}
		});

		jPanel4.add(lineOkayButton);

		lineCancelButton.setText("Cancel");
		lineCancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				lineCancelButtonActionPerformed(evt);
			}
		});

		jPanel4.add(lineCancelButton);

		jPanel1.add(jPanel4, java.awt.BorderLayout.SOUTH);

		//jTabbedPane1.addTab("", GUIPrism.getIconFromImage("lineGraph.png"), jPanel1);

		jPanel2.setBorder(new javax.swing.border.TitledBorder("Bar Graph"));
		jPanel2.setEnabled(false);
		//jTabbedPane1.addTab("", GUIPrism.getIconFromImage("barGraph.png"), jPanel2);

		getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

		pack();
	}

	public boolean isGraphCancelled()
	{
		return graphCancelled;
	}

	private void lineCancelButtonActionPerformed(java.awt.event.ActionEvent evt)
	{
		graphCancelled = true;
		setVisible(false);
	}

	private void lineOkayButtonActionPerformed(java.awt.event.ActionEvent evt)
	{
		int numSeries = 1;

		// see which constant is on x axis
		ranger = selectAxisConstantCombo.getSelectedItem().toString();

		// init arrays
		otherValues = new Values();
		multiSeries = new Vector<DefinedConstant>();

		// go through all constants in picker list
		for (int j = 0; j < pickerList.getNumConstants(); j++) {
			// get constant
			DefinedConstant tmpConstant = pickerList.getConstantLine(j).getDC();
			// if its the constant for the x-axis, store info about the constant
			if (tmpConstant.getName().equals(ranger)) {
				rangingConstant = tmpConstant;
			}
			// otherwise store info about the selected values
			else {
				// Is this constant just a value, or does it have a range?
				Object value = pickerList.getConstantLine(j).getSelectedValue();
				if (value instanceof String) {
					/* Yes, calculate the numSeries. */
					multiSeries.add(pickerList.getConstantLine(j).getDC());
					numSeries *= tmpConstant.getNumSteps();
				} else {
					/* No, just the one. */
					otherValues.addValue(tmpConstant.getName(), value);
				}
			}
		}

		//sort out which one to add it to
		if (rangingConstant == null)
			return;

		// if there are a lot of series, check if this is what the user really wanted
		if (numSeries > MAX_NUM_SERIES_BEFORE_QUERY) {
			String[] choices = { "Yes", "No" };
			int choice = -1;
			choice = plugin.optionPane("Warning: This will plot " + numSeries + " series.\nAre you sure you want to continue?", "Question",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, choices, choices[0]);
			if (choice != 0)
				return;
		}

		if (newGraphRadio.isSelected()) {
			/* Make new graph. */
			graphModel = new Graph();
			graphHandler.addGraph(graphModel);

			graphModel.getYAxisSettings().setHeading(resultsCollection.getResultName());
			graphModel.getXAxisSettings().setHeading(ranger);
		} else {
			/* Add to an existing graph. */
			graphModel = graphHandler.getModel(existingGraphCombo.getSelectedItem().toString());
			if (!ranger.equals(graphModel.getXAxisSettings().getHeading())) //FIXME: must do this better in future
				if (!roughExists(ranger, graphModel.getXAxisSettings().getHeading()))
					graphModel.getXAxisSettings().setHeading(graphModel.getXAxisSettings().getHeading() + ", " + ranger);
		}

		graphCancelled = false;
		setVisible(false);
	}

	private void existingGraphRadioActionPerformed(java.awt.event.ActionEvent evt)
	{
		doEnables();
	}

	private void newGraphRadioActionPerformed(java.awt.event.ActionEvent evt)
	{
		doEnables();
	}

	private void selectAxisConstantComboActionPerformed(java.awt.event.ActionEvent evt)
	{
		pickerList.disableLine(selectAxisConstantCombo.getSelectedIndex());
		resetAutoSeriesName();
	}

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)
	{
		setVisible(false);
		dispose();
	}

	// Variables declaration - do not modify
	private javax.swing.ButtonGroup buttonGroup1;
	private javax.swing.JPanel constantTablePanel;
	private javax.swing.JComboBox existingGraphCombo;
	private javax.swing.JRadioButton existingGraphRadio;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel10;
	private javax.swing.JPanel jPanel11;
	private javax.swing.JPanel jPanel12;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JPanel jPanel6;
	private javax.swing.JPanel jPanel7;
	private javax.swing.JPanel jPanel9;
	private javax.swing.JTabbedPane jTabbedPane1;
	private javax.swing.JButton lineCancelButton;
	private javax.swing.JButton lineOkayButton;
	private javax.swing.JLabel middleLabel;
	private javax.swing.JRadioButton newGraphRadio;
	private javax.swing.JComboBox selectAxisConstantCombo;
	private javax.swing.JTextField seriesNameField;
	private javax.swing.JLabel seriesNameLabel;
	private javax.swing.JLabel topComboLabel;

	// End of variables declaration

	public static int factorial(int i)
	{
		if (i < 0)
			return 1;
		if (i == 0)
			return 1;
		else
			return i * factorial(i - 1);
	}

	public static boolean roughExists(String test, String inThis)
	{
		int i = inThis.indexOf(test);
		if (i == -1)
			return false;
		if (!((i == 0) || (inThis.charAt(i - 1) == ' ')))
			return false;
		if (!((inThis.length() == i + 1) || (inThis.charAt(i + 1) == ',')))
			return false;
		return true;
	}
}
