//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
//	* Mateusz Ujma <mateusz.ujma@cs.ox.ac.uk> (University of Oxford)
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.data.xy.XYDataItem;

import parser.Values;
import parser.ast.Expression;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import parser.type.Type;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import parser.type.TypeInterval;
import prism.Prism;
import prism.PrismException;
import prism.PrismSettings;
import prism.PrismSettingsListener;
import prism.TileList;
import prism.UndefinedConstants;
import userinterface.GUIClipboardEvent;
import userinterface.GUIConstantsPicker;
import userinterface.GUIPlugin;
import userinterface.GUIPrism;
import userinterface.GUISimulationPicker;
import userinterface.OptionsPanel;
import userinterface.SimulationInformation;
import userinterface.graph.Graph;
import userinterface.graph.Graph.SeriesKey;
import userinterface.model.GUIModelEvent;
import userinterface.model.GUIMultiModelHandler;
import userinterface.model.computation.ExportBuiltModelThread;
import userinterface.properties.computation.ExportResultsThread;
import userinterface.properties.computation.LoadPropertiesThread;
import userinterface.properties.computation.ModelCheckThread;
import userinterface.properties.computation.SimulateModelCheckThread;
import userinterface.simulator.GUISimulator;
import userinterface.util.GUIComputationEvent;
import userinterface.util.GUIEvent;
import userinterface.util.GUIExitEvent;

/**
 *  Properties tab of the PRISM GUI.
 */
@SuppressWarnings("serial")
public class GUIMultiProperties extends GUIPlugin implements MouseListener, ListSelectionListener, PrismSettingsListener, ContainerListener
{
	//CONSTANTS
	public static final int CONTINUE = 0;
	public static final int CANCEL = 1;

	public static final int WARN_INVALID_PROPS = 1;
	public static final int NEVER_INVALID_PROPS = 2;

	// ATTRIBUTES

	// Current model (gets updated only by event listening to GUIModel)
	private ModulesFile parsedModel;
	// Constants for model (updated by events or locally)
	private Values mfConstants;

	// State
	private boolean modified;
	private boolean computing;
	private boolean verifyAfterReceiveParseNotification, experimentAfterReceiveParseNotification, simulateAfterReceiveParseNotification, exportLabelsAfterReceiveParseNotification;
	private PropertiesFile parsedProperties;
	private ArrayList<GUIProperty> propertiesToBeVerified;
	private File activeFile;
	private Values pfConstants;
	private String argsPropertiesFile;
	private int exportType = Prism.EXPORT_PLAIN;
	private File exportFile = null;

	// GUI
	private FileFilter propsFilter;
	private Map<String,FileFilter> labFilters;
	private FileFilter textFilter;
	private FileFilter csvFilter;
	private FileFilter matlabFilter;
	private JMenu propMenu;
	private JPopupMenu propertiesPopup, constantsPopup, labelsPopup, experimentPopup;
	private GUIExperimentTable experiments;
	private GUIGraphHandler graphHandler;
	private JScrollPane expScroller;
	private JTextField fileTextField;
	private Action newProps, openProps, saveProps, savePropsAs, insertProps, verifySelected, newProperty, editProperty, newConstant, removeConstant, newLabel,
			removeLabel, newExperiment, deleteExperiment, stopExperiment, parametric, viewResults, plotResults, exportResultsListText, exportResultsListCSV,
			exportResultsMatrixText, exportResultsMatrixCSV, simulate, details, exportLabelsPlain, exportLabelsMatlab;;

	// Current properties
	private GUIPropertiesList propList;
	private GUIPropConstantList consTable;
	private GUIPropLabelList labTable;

	// Cached copies of settings
	private Font displayFontFast;
	private Color backgroundFast, warningFast;

	// The simulator
	private GUISimulator simulator;

	// CONSTRUCTORS

	/** Creates a new instance of GUIMultiProperties */
	public GUIMultiProperties(GUIPrism pr, GUISimulator simulator)
	{
		super(pr);
		this.simulator = simulator;
		simulator.setGUIProb(this); // link required
		initComponents();
		a_newList();
		setParsedModel(null);
		doEnables();
		//options = new GUIPropertiesOptions(this);
	}

	public void takeCLArgs(String args[])
	{
		if (args.length > 1) {
			argsPropertiesFile = args[1];
		}
	}

	//ACCESS METHODS

	public ModulesFile getParsedModel()
	{
		return parsedModel;
	}

	public String getConstantsString()
	{
		return consTable.getValidConstantsString();
	}

	public String getLabelsString()
	{
		return labTable.getValidLabelsString();
	}

	public int getNumConstants()
	{
		return consTable.getRowCount();
	}

	public int getNumLabels()
	{
		return labTable.getRowCount();
	}

	public Font getListFont()
	{
		return displayFontFast;
	}

	public Color getWarningColor()
	{
		return warningFast;
	}

	public Color getSelectionColor()
	{
		return backgroundFast;
	}

	public int getInvalidPropertyStrategy()
	{
		return getPrism().getSettings().getInteger(PrismSettings.PROPERTIES_ADDITION_STRATEGY) + 1; //note the correction
	}

	public GUIGraphHandler getGraphHandler()
	{
		return graphHandler;
	}

	/* UPDATE METHODS */

	public void repaintList()
	{
		propList.repaint();
	}

	protected void verifyAfterParse()
	{
		ArrayList<GUIProperty> validGUIProperties;
		UndefinedConstants uCon;

		verifyAfterReceiveParseNotification = false;

		try {
			// are we in exact mode?
			boolean exact = getPrism().getSettings().getBoolean(PrismSettings.PRISM_EXACT_ENABLED);

			// Get valid/selected properties
			String propertiesString = getLabelsString() + "\n" + getConstantsString() + "\n" + propList.getValidSelectedAndReferencedString();
			// Get PropertiesFile for valid/selected properties
			parsedProperties = getPrism().parsePropertiesString(parsedModel, propertiesString);
			// And get list of corresponding GUIProperty objects
			validGUIProperties = propList.getValidSelectedProperties();
			// Query user for undefined constant values (if required)
			int n = parsedProperties.getNumProperties();
			ArrayList<Property> validProperties = new ArrayList<Property>(n);
			for (int i = 0; i < n; i++)
				validProperties.add(parsedProperties.getPropertyObject(i));
			uCon = new UndefinedConstants(parsedModel, parsedProperties, validProperties);
			uCon.setExactMode(exact);
			if (uCon.getMFNumUndefined() + uCon.getPFNumUndefined() > 0) {
				// Use previous constant values as defaults in dialog
				int result = GUIConstantsPicker.defineConstantsWithDialog(this.getGUI(), uCon, mfConstants, pfConstants);
				if (result != GUIConstantsPicker.VALUES_DONE)
					return;
			}
			// Store model/property constants
			mfConstants = uCon.getMFConstantValues();
			pfConstants = uCon.getPFConstantValues();
			getPrism().setPRISMModelConstants(mfConstants, exact);
			parsedProperties.setSomeUndefinedConstants(pfConstants, exact);
			// Store properties to be verified
			propertiesToBeVerified = validGUIProperties;
			for (GUIProperty gp : propertiesToBeVerified)
				gp.setConstants(mfConstants, pfConstants);
			// Start model checking
			Thread t = new ModelCheckThread(this, parsedProperties, propertiesToBeVerified);
			t.setPriority(Thread.NORM_PRIORITY);
			t.start();
		} catch (PrismException e) {
			error(e.getMessage());
			return;
		}
	}

	public void simulateAfterParse()
	{
		simulateAfterReceiveParseNotification = false;
		ArrayList<GUIProperty> validGUIProperties, simulatableGUIProperties;
		ArrayList<Expression> simulatableExprs;
		UndefinedConstants uCon;
		try {
			parsedProperties = getPrism().parsePropertiesString(parsedModel,
					getLabelsString() + "\n" + getConstantsString() + "\n" + propList.getValidSelectedAndReferencedString());
			validGUIProperties = propList.getValidSelectedProperties();
			if (validGUIProperties.size() == 0) {
				error("None of the selected properties are suitable for simulation");
				return;
			}
		} catch (PrismException e) {
			error(e.getMessage());
			return;
		}

		// See which of the (valid) selected properties are ok for simulation
		// Also store a list of the expression themselves
		simulatableGUIProperties = new ArrayList<GUIProperty>();
		ArrayList<Property> simulatableProperties = new ArrayList<Property>();
		simulatableExprs = new ArrayList<Expression>();
		for (int i = 0; i < validGUIProperties.size(); i++) {
			GUIProperty guiP = validGUIProperties.get(i);
			if (getPrism().isPropertyOKForSimulation(guiP.getProperty())) {
				simulatableGUIProperties.add(guiP);
				simulatableProperties.add(parsedProperties.getPropertyObject(i));
				simulatableExprs.add(guiP.getProperty());
			}
		}
		if (simulatableGUIProperties.size() == 0) {
			error("None of the selected properties are suitable for simulation");
			return;
		}
		if (simulatableGUIProperties.size() < validGUIProperties.size()) {
			int q = questionYesNo("Warning", "Warning: Some of the selected properties are not suitable for simulation. Continue?");
			if (q != 0)
				return;
		}

		//find out any undefined constants
		try {
			uCon = new UndefinedConstants(parsedModel, parsedProperties, simulatableProperties);
			if (uCon.getMFNumUndefined() + uCon.getPFNumUndefined() > 0) {
				// Use previous constant values as defaults in dialog
				int result = GUIConstantsPicker.defineConstantsWithDialog(this.getGUI(), uCon, mfConstants, pfConstants);
				if (result != GUIConstantsPicker.VALUES_DONE)
					return;
			}

			// Store model/property constants
			mfConstants = uCon.getMFConstantValues();
			pfConstants = uCon.getPFConstantValues();
			// currently, evaluate constants non-exact for simulation
			getPrism().setPRISMModelConstants(mfConstants, false);
			parsedProperties.setSomeUndefinedConstants(pfConstants, false);
			for (GUIProperty gp : simulatableGUIProperties)
				gp.setConstants(mfConstants, pfConstants);

			// Store properties to be verified
			propertiesToBeVerified = validGUIProperties;
			for (GUIProperty gp : propertiesToBeVerified)
				gp.setConstants(mfConstants, pfConstants);

			// Get simulation info with dialog
			SimulationInformation info = GUISimulationPicker.defineSimulationWithDialog(this.getGUI(), simulatableExprs, parsedModel, null);

			// If user cancelled simulation, quit 
			if (info == null)
				return;

			if (parsedModel != null && parsedProperties != null) {
				Thread t = new SimulateModelCheckThread(this, parsedProperties, simulatableGUIProperties, pfConstants, info);
				t.setPriority(Thread.NORM_PRIORITY);
				t.start();
			}
		} catch (PrismException e) {
			error(e.getMessage());
			return;
		}
	}

	public void experimentAfterParse()
	{
		experimentAfterReceiveParseNotification = false;
		GUIProperty gp = propList.getProperty(propList.getSelectedIndex());
		Type type;

		try {
			/*//get referenced named properties
			String namedString = "";
			//Add named properties
			for (GUIProperty namedProp : this.propList.getAllNamedProperties()) {
				if (gp.getReferencedNames().contains(namedProp.getName())) {
					namedString += "\"" + namedProp.getName() + "\" : " + namedProp.getPropString() + "\n";
				}
			}*/

			// parse property to be used for experiment
			parsedProperties = getPrism().parsePropertiesString(parsedModel,
					getLabelsString() + "\n" + getConstantsString() + "\n" + propList.getValidSelectedAndReferencedString());
			if (parsedProperties.getNumProperties() <= 0) {
				error("There are no properties selected");
				return;
			}
			if (propList.getNumSelectedProperties() > 1) {
				error("Experiments can only be created for a single property");
				return;
			}

			// check the type of the property
			int index = parsedProperties.getNumProperties() - 1;
			type = parsedProperties.getProperty(index).getType();
		} catch (PrismException e) {
			error(e.getMessage());
			return;
		}

		//get Property objects for sorting out undefined constants
		ArrayList<Property> props = new ArrayList<Property>();
		for (int i = 0; i < parsedProperties.getNumProperties(); i++) {
			props.add(parsedProperties.getPropertyObject(i));
		}

		// sort out undefined constants
		UndefinedConstants uCon = new UndefinedConstants(parsedModel, parsedProperties, props);
		uCon.setExactMode(getPrism().getSettings().getBoolean(PrismSettings.PRISM_EXACT_ENABLED));
		boolean showGraphDialog = false;
		boolean useSimulation = false;
		if (uCon.getMFNumUndefined() + uCon.getPFNumUndefined() == 0) {
			error("Cannot create an experiment because there are no constants with undefined values");
			return;
		}
		boolean offerGraph = type instanceof TypeInt || type instanceof TypeDouble || type instanceof TypeInterval;
		int result = GUIExperimentPicker.defineConstantsWithDialog(this.getGUI(), uCon, offerGraph, gp.isValidForSimulation());
		if (result == GUIExperimentPicker.VALUES_DONE_SHOW_GRAPH || result == GUIExperimentPicker.VALUES_DONE_SHOW_GRAPH_AND_SIMULATE) {
			showGraphDialog = true;
		} else if (result == GUIExperimentPicker.CANCELLED)
			return;
		if (result == GUIExperimentPicker.VALUES_DONE_SIMULATE || result == GUIExperimentPicker.VALUES_DONE_SHOW_GRAPH_AND_SIMULATE) {
			useSimulation = true;
		}

		//if we are using simulation, make sure the property is ok
		if (useSimulation) {
			try {
				getPrism().checkPropertyForSimulation(gp.getProperty());
			} catch (PrismException e) {
				error("Property is not suitable for simulation: " + e.getMessage());
				return;
			}
		}

		// make sure we can actually create a graph, i.e. that there is >1 result
		if (showGraphDialog)
			if (uCon.getRangingConstants().size() == 0) {
				message("Cannot create a graph since there is only a single result.");
				showGraphDialog = false;
			}

		// Use these values to create a new experiment
		int i = experiments.newExperiment(parsedProperties, uCon, useSimulation);
		boolean notCancelled = true;
		// start the experiment, via the graph dialog if appropriate
		if (showGraphDialog) {
			GUIGraphPicker ggp = new GUIGraphPicker(getGUI(), this, experiments.getExperiment(i), graphHandler, false);
			if (ggp.isGraphCancelled()) {
				if (questionYesNo("Do you want to cancel the experiment completely?", 0) == 0)
					notCancelled = false;
			}
		}

		if (notCancelled)
			experiments.startExperiment(i);
		//else
		//	experiments.removeExperiment(i);
	}

	public void propertyLoadSuccessful(PropertiesFile pf, File f)
	{
		// note: add constants/labels first to stop property parse errors
		consTable.newList();
		consTable.addPropertiesFile(pf);
		labTable.newList();
		labTable.addPropertiesFile(pf);
		propList.deleteAll();
		propList.addPropertiesFile(pf);
		tabToFront();
		setModified(false);
		setActiveFile(f);
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.PROPERTIES_LIST_CHANGED));
	}

	public void propertyInsertSuccessful(PropertiesFile pf)
	{
		propList.addPropertiesFile(pf);
		consTable.addPropertiesFile(pf);
		labTable.addPropertiesFile(pf);
		tabToFront();
		setModified(true);
	}

	public void propertySaveSuccessful(File f)
	{
		setActiveFile(f);
		setModified(false);
	}

	public void changeProperty(String pctl, String comment, String id)
	{
		int index = propList.getIndexOf(id);
		if (index < 0) {
			if (pctl != null) {
				propList.addProperty(pctl, comment);
				setModified(true);
			}
		} else {
			GUIProperty gp = propList.getProperty(index);
			gp.setBeingEdited(false);
			if (pctl != null) {
				if (pctl.matches("\"[^\"]*\"[ ]*:.*")) {
					//the string contains property name
					int start = pctl.indexOf('"') + 1;
					int end = pctl.indexOf('"', start);
					String name = pctl.substring(start, end);
					int colon = pctl.indexOf(':') + 1;
					pctl = pctl.substring(colon).trim();
					gp.setPropStringAndName(pctl, name, parsedModel, getConstantsString(), getLabelsString());
				} else {
					gp.setPropStringAndName(pctl, null, parsedModel, getConstantsString(), getLabelsString());
				}

				gp.setComment(comment);
				setModified(true);
			}
			// Force repaint because we modified the GUIProperty directly
			repaintList();
		}
		selectionChangeHandler.notifyListeners(new GUIEvent(1));
		updateCommentLabel();
	}

	public void cancelProperty(String id)
	{
		int index = propList.getIndexOf(id);
		if (index >= 0) {
			GUIProperty gp = propList.getProperty(index);
			gp.setBeingEdited(false);
			// Force repaint because we modified the GUIProperty directly
			repaintList();
		}
	}

	/** Called by the constant list to let us know something changed there */

	public void constantListChanged()
	{
		labTable.validateLabels();
		//maybe some constants became valid/invalid, so validate them
		//before re-validating properties.
		consTable.validateConstants();
		propList.validateProperties();
		setModified(true);
	}

	/** Called by the label list to let us know something changed there */

	public void labelListChanged()
	{
		propList.validateProperties();
		setModified(true);
	}

	protected void setModified(boolean mod)
	{
		modified = mod;
		setActiveFileLabel();
		if (modified) {
			doEnables();
			notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.PROPERTIES_LIST_CHANGED));
		}
	}

	protected void setComputing(boolean com)
	{
		computing = com;
		doEnables();
		selectionChangeHandler.notifyListeners(new GUIEvent(1));
	}

	protected void setActiveFile(File f)
	{
		activeFile = f;
		setActiveFileLabel();
	}

	protected void setActiveFileLabel()
	{
		fileTextField.setText("Properties list: " + ((activeFile == null) ? "<Untitled>" : activeFile.getPath()) + (modified ? "*" : ""));
	}

	protected void setParsedModel(ModulesFile m)
	{
		parsedModel = m;
		consTable.validateConstants();
		propList.validateProperties();
		doEnables();
	}

	protected void doEnables()
	{
		// properties panel
		setEnabled(true);
		setTabEnabled(true);
		// properties menu
		newProps.setEnabled(!computing);
		openProps.setEnabled(!computing);
		insertProps.setEnabled(!computing);
		saveProps.setEnabled(!computing);
		savePropsAs.setEnabled(!computing);
		simulate.setEnabled(!computing && parsedModel != null && propList.existsValidSimulatableSelectedProperties());
		verifySelected.setEnabled(!computing && parsedModel != null && propList.existsValidSelectedProperties());
		exportLabelsPlain.setEnabled(!computing && parsedModel != null);
		exportLabelsMatlab.setEnabled(!computing && parsedModel != null);
		details.setEnabled(!computing && parsedModel != null && propList.existsValidSelectedProperties());
		// properties list
		propList.setEnabled(!computing);
		newProperty.setEnabled(!computing);
		editProperty.setEnabled(!computing && propList.getSelectedProperties().size() > 0);
		// constants list
		consTable.setEnabled(!computing);
		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		// label list
		labTable.setEnabled(!computing);
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);

		// newExperiment: enabled if there is exactly one prop selected and it is valid
		newExperiment.setEnabled(propList.getNumSelectedProperties() == 1 && propList.getValidSelectedProperties().size() == 1);
		// parametric: enabled if there is exactly one prop selected and it is valid
		parametric.setEnabled(propList.getNumSelectedProperties() == 1 && propList.getValidSelectedProperties().size() == 1);
		// deleteExperiments: enabled if one or more experiments selected
		deleteExperiment.setEnabled(experiments.getSelectedRowCount() > 0);
		// viewResults: enabled if at least one experiment is selected
		viewResults.setEnabled(experiments.getSelectedRowCount() > 0);
		// plotResults: enabled if exactly one experiment is selected and its type is int/double
		if (experiments.getSelectedRowCount() == 1) {
			GUIExperiment exp = experiments.getExperiment(experiments.getSelectedRow());
			Type type = exp.getPropertyType();
			plotResults.setEnabled(type instanceof TypeInt || type instanceof TypeDouble || type instanceof TypeInterval);
		} else {
			plotResults.setEnabled(false);
		}
		// exportResults: enabled if at least one experiment is selected
		exportResultsListText.setEnabled(experiments.getSelectedRowCount() > 0);
		exportResultsListCSV.setEnabled(experiments.getSelectedRowCount() > 0);
		exportResultsMatrixText.setEnabled(experiments.getSelectedRowCount() > 0);
		exportResultsMatrixCSV.setEnabled(experiments.getSelectedRowCount() > 0);
	}

	public int doModificationCheck()
	{
		if (modified) {
			if (activeFile == null) {
				String[] selection = { "Yes", "No", "Cancel" };
				int selectionNo = -1;
				selectionNo = optionPane("Properties list has been modified.\nDo you wish to save it?", "Question", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, selection, selection[0]);
				switch (selectionNo) {
				case 0:
					return a_save();
				case 1:
					return CONTINUE;
				case 2:
					return CANCEL;
				default:
					return CANCEL;
				}
			} else {
				String[] selection = { "Yes", "No", "Save As...", "Cancel" };
				int selectionNo = -1;
				selectionNo = optionPane("Properties list has been modified.\nDo you wish to save it?", "Question", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, selection, selection[0]);
				switch (selectionNo) {
				case 0:
					return a_save();
				case 1:
					return CONTINUE;
				case 2:
					return a_saveAs();
				case 3:
					return CANCEL;
				default:
					return CANCEL;
				}
			}
		}
		return CONTINUE;
	}

	private void updateCommentLabel()
	{
		ArrayList<GUIProperty> selectedProps = propList.getSelectedProperties();
		if (selectedProps.size() == 1) {
			GUIProperty p = selectedProps.get(0);
			comLabel.setText(p.getComment());
		} else {
			comLabel.setText("");
		}
	}

	//ACTION METHODS

	public void a_newList()
	{
		if (doModificationCheck() != CONTINUE)
			return;

		propList.deleteAll();
		consTable.newList();
		labTable.newList();
		tabToFront();
		setModified(false);
		setActiveFile(null);
		doEnables();
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.PROPERTIES_LIST_CHANGED));
	}

	public void a_open()
	{
		if (doModificationCheck() != CONTINUE)
			return;

		if (showOpenFileDialog(propsFilter) == JFileChooser.APPROVE_OPTION) {
			File file = getChooserFile();
			if (file == null) {
				error("No file selected");
				return;
			}
			Thread t = new LoadPropertiesThread(this, parsedModel, file);
			t.setPriority(Thread.NORM_PRIORITY);
			t.start();
		}
	}

	// Save properties
	// NB: This used to be in a thread but I have taken it out because:
	//	 (a) We sometimes need to know whether save was successful before continuing
	//	 (b) When saving before clearing, clear can occur too early

	public int a_save()
	{
		if (activeFile == null) {
			return a_saveAs();
		} else {
			// only allow save if all props valid (can't reopen file otherwise)
			if (!propList.allPropertiesAreValid()) {
				message("Cannot save properties list: some properties are invalid");
				return CANCEL;
			} else {
				// do save
				try {
					setTaskBarText("Saving properties...");
					PrintWriter out = new PrintWriter(new FileWriter(activeFile));
					out.print(propList.toFileString(activeFile, consTable, labTable));
					out.flush();
					out.close();
				} catch (IOException e) {
					setTaskBarText("Saving properties... error.");
					error("Could not save to file \"" + activeFile + "\"");
					return CANCEL;
				}
				setTaskBarText("Saving properties... done.");
				propertySaveSuccessful(activeFile);
				return CONTINUE;
			}
		}
	}

	// Save properties as
	// NB: This used to be in a thread but I have taken it out because:
	//	 (a) We sometimes need to know whether save was successful before continuing
	//	 (b) When saving before clearing, clear can occur too early

	public int a_saveAs()
	{
		// only allow save if all props valid (can't reopen file otherwise)
		if (!propList.allPropertiesAreValid()) {
			message("Cannot save properties list: some properties are invalid");
			return CANCEL;
		}
		if (!consTable.isConstantListValid()) {
			message("Cannot save properties list: some constants are invalid");
			return CANCEL;
		}
		if (showSaveFileDialog(propsFilter) == JFileChooser.APPROVE_OPTION) {
			File file = getChooserFile();
			// do save
			try {
				setTaskBarText("Saving properties...");
				PrintWriter out = new PrintWriter(new FileWriter(file));
				out.print(propList.toFileString(file, consTable, labTable));
				out.flush();
				out.close();
			} catch (IOException e) {
				setTaskBarText("Saving properties... error.");
				error("Could not save to file \"" + file + "\"");
				return CANCEL;
			}
			setTaskBarText("Saving properties... done.");
			propertySaveSuccessful(file);
			return CONTINUE;
		}
		return CANCEL;
	}

	public void a_insert()
	{
		if (showOpenFileDialog(propsFilter) == JFileChooser.APPROVE_OPTION) {
			File file = getChooserFile();
			if (file == null) {
				error("No file selected");
				return;
			}
			Thread t = new LoadPropertiesThread(this, parsedModel, file, true);
			t.setPriority(Thread.NORM_PRIORITY);
			t.start();
		} else {
			return;
		}
	}

	public void a_simulateSelected()
	{
		consTable.correctEditors();
		labTable.correctEditors();
		// Bail out if there are no valid properties to simulate
		// (probably never occurs - action is disabled in this case)
		if (!propList.existsValidSimulatableSelectedProperties()) {
			error("None of the selected properties are suitable for simulation");
			return;
		}
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Request a parse
		simulateAfterReceiveParseNotification = true;
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.REQUEST_MODEL_PARSE));
	}

	public void a_detailSelected()
	{
		int[] selected = propList.getSelectedIndices();

		for (int i = 0; i < selected.length; i++) {
			GUIProperty gp = propList.getProperty(selected[i]);
			if (!gp.isBeingEdited()) {
				gp.setBeingEdited(true);
				// Force repaint because we modified the GUIProperty directly
				repaintList();
				new GUIPropertyResultDialog(getGUI(), this, gp).display();
			}
		}
	}

	public void a_verifySelected()
	{
		consTable.correctEditors();
		labTable.correctEditors();
		// Bail out if there are no valid properties to verify
		// (probably never occurs - action is disabled in this case)
		if (!propList.existsValidSelectedProperties()) {
			error("None of the selected properties are suitable for verification. The model was not built");
			return;
		}
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Request a parse
		verifyAfterReceiveParseNotification = true;
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.REQUEST_MODEL_PARSE));
	}

	public void a_verifyDone()
	{
		int[] selected = propList.getSelectedIndices();

		// Display result dialogs
		for (int i = 0; i < selected.length; i++) {
			GUIProperty gp = propList.getProperty(selected[i]);
			if (!gp.isBeingEdited()) {
				gp.setBeingEdited(true);
				// Force repaint because we modified the GUIProperty directly
				repaintList();
				new GUIPropertyResultDialog(getGUI(), this, gp).display();
			}
		}

		// How to plot a parametric result...
		/*if (selected.length == 1) {
			GUIProperty gp = propList.getProperty(selected[0]);
			if (gp.getResult().getResult() instanceof RegionValues) {
				Graph graph = new Graph("Param");
				SeriesKey sk = graph.addSeries("param");
				RegionValues vals = (RegionValues) gp.getResult().getResult();
				param.Function f = vals.getResult(0).getInitStateValueAsFunction();
				int n = 100;
				for (int i = 0; i < n; i++) {
					BigRational br = f.evaluate(new param.Point(new BigRational[] {new BigRational(i, n)}));
					XYDataItem di = new XYDataItem(((double)i)/n, br.doubleValue());
					graph.addPointToSeries(sk, di);
				}
				this.getGraphHandler().addGraph(graph);
			}
		}*/
		
		// For a single property with a displayable counterexample, offer to do show it
		if (selected.length == 1) {
			GUIProperty gp = propList.getProperty(selected[0]);
			Object cex = gp.getResult().getCounterexample();
			if (cex != null && cex instanceof simulator.PathFullInfo) {
				String qu = "Do you want to view a witness/counterexample for the property in the simulator?";
				if (questionYesNo("Question", qu, 0) == 0) {
					simulator.a_loadPath((simulator.PathFullInfo) cex);
					simulator.tabToFront();
				}
			}
		}

		//print Pareto curves for all available TileLists, then clear the storage
		synchronized (TileList.getStoredTileLists()) {
			for (int i = 0; i < TileList.getStoredTileLists().size(); i++) {
				TileList tl = TileList.getStoredTileLists().get(i);
				if (tl != null && tl.getDimension() == 2) {
					Graph graph = new Graph(TileList.getStoredFormulas().get(i).toString());
					graph.getXAxisSettings().setHeading(TileList.getStoredFormulasX().get(i).toString());
					graph.getYAxisSettings().setHeading(TileList.getStoredFormulasY().get(i).toString());
					SeriesKey sk = graph.addSeries("Pareto curve");

					//Get points in tilelist and sort them. This is required for the graph to show them right
					List<prism.Point> l = tl.getPoints();
					Comparator<prism.Point> c = new Comparator<prism.Point>()
					{
						public int compare(prism.Point o1, prism.Point o2)
						{
							if (o1.getCoord(0) == o2.getCoord(0))
								return Double.compare(o1.getCoord(1), o2.getCoord(1));
							else
								return Double.compare(o1.getCoord(0), o2.getCoord(0));
						};
					};

					Collections.sort(l, c);

					for (prism.Point p : l) {
						prism.Point pReal = p.toRealProperties(tl.getOpsAndBoundsList());
						XYDataItem di = new XYDataItem(pReal.getCoord(0), pReal.getCoord(1));
						graph.addPointToSeries(sk, di);
					}
					this.getGraphHandler().addGraph(graph);
				}
			}

			TileList.clearStoredTileLists();
		}
	}

	public void a_cut()
	{
		java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		//java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(propList.getClipboardString());
		//clipboard.setContents(selection, null);
		clipboard.setContents(new GUIClipboardProperties(propList.getSelectedProperties()), null);
		a_delete();
	}

	public void a_copy()
	{
		java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		//java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(propList.getClipboardString());
		//clipboard.setContents(selection, null);
		clipboard.setContents(new GUIClipboardProperties(propList.getSelectedProperties()), null);
	}

	public void a_paste()
	{
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clipboard.getContents(null);

		if (contents != null) {
			if (contents.isDataFlavorSupported(getGUIClipboardPropertiesDataFlavor())) {
				try {
					GUIClipboardProperties gcp = (GUIClipboardProperties) contents.getTransferData(getGUIClipboardPropertiesDataFlavor());
					ArrayList<GUIProperty> listOfProperties = gcp.getProperties();
					for (int i = 0; i < listOfProperties.size(); i++) {
						GUIProperty property = (GUIProperty) listOfProperties.get(i);
						propList.addProperty(property.getName(), property.getPropString(), property.getComment());
						setModified(true);
					}
				} catch (UnsupportedFlavorException e) {
				} catch (IOException e) {
				}
			} else {
				try {
					String text = (String) contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
					propList.pastePropertiesString(text);
					setModified(true);
				} catch (UnsupportedFlavorException e) {
				} catch (IOException e) {
				}
			}
		}
	}

	public void a_delete()
	{
		propList.deleteSelected();
		setModified(true);
	}

	public void a_deleteAll()
	{
		propList.deleteAll();
		setModified(true);
	}

	public void a_newProperty()
	{
		GUIPropertyEditor ed = new GUIPropertyEditor(this, parsedModel, getInvalidPropertyStrategy());
		ed.show();
	}

	public void a_editProperty()
	{
		int[] selected = propList.getSelectedIndices();

		for (int i = 0; i < selected.length; i++) {
			GUIProperty gp = propList.getProperty(selected[i]);
			if (!gp.isBeingEdited()) {
				gp.setBeingEdited(true);
				// Force repaint because we modified the GUIProperty directly
				repaintList();
				GUIPropertyEditor ed = new GUIPropertyEditor(this, parsedModel, gp, getInvalidPropertyStrategy());
				ed.show();
			}
		}

		selectionChangeHandler.notifyListeners(new GUIEvent(1));
	}

	public void a_selectAll()
	{
		propList.selectAll();
	}

	public void a_addConstant()
	{
		consTable.addNewConstant();
	}

	public void a_removeSelectedConstants()
	{
		// Note: Unlike for prop list, this is safe because constants can always be deleted
		//       (not the case properties - e.g. if they are being edited)
		while (consTable.getSelectedRowCount() > 0) {
			consTable.removeConstant(consTable.getSelectedRow());//for now
		}
	}

	public void a_addLabel()
	{
		labTable.addNewLabel();
	}

	public void a_removeSelectedLabels()
	{
		// Note: Unlike for prop list, this is safe because constants can always be deleted
		//       (not the case properties - e.g. if they are being edited)
		while (labTable.getSelectedRowCount() > 0) {
			labTable.removeLabel(labTable.getSelectedRow());
		}
	}

	public void a_exportLabels(int exportType)
	{
		int res = JFileChooser.CANCEL_OPTION;

		// pop up dialog to select file
		switch (exportType) {
		case Prism.EXPORT_MATLAB:
			res = showSaveFileDialog(matlabFilter);
			break;
		default:
			res = showSaveFileDialog(labFilters.values(), labFilters.get("lab"));
			break;
		}
		if (res != JFileChooser.APPROVE_OPTION)
			return;
		consTable.correctEditors();
		labTable.correctEditors();
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Set flag, store export info
		exportLabelsAfterReceiveParseNotification = true;
		this.exportType = exportType;
		this.exportFile = getChooserFile();
		// Request a parse
		exportLabelsAfterReceiveParseNotification = true;
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.REQUEST_MODEL_PARSE));
	}
	
	public void exportLabelsAfterParse()
	{
		// Switch off flag
		exportLabelsAfterReceiveParseNotification = false;
		try {
			// Parse labels/constants
			parsedProperties = getPrism().parsePropertiesString(parsedModel, getLabelsString() + "\n" + getConstantsString());
			// Query user for undefined constant values (if required)
			UndefinedConstants uCon = new UndefinedConstants(parsedModel, parsedProperties, true);
			if (uCon.getMFNumUndefined() + uCon.getPFNumUndefined() > 0) {
				// Use previous constant values as defaults in dialog
				int result = GUIConstantsPicker.defineConstantsWithDialog(this.getGUI(), uCon, mfConstants, pfConstants);
				if (result != GUIConstantsPicker.VALUES_DONE)
					return;
			}
			// Store model/property constants
			mfConstants = uCon.getMFConstantValues();
			pfConstants = uCon.getPFConstantValues();
			// currently, evaluate constants non-exact for model building
			getPrism().setPRISMModelConstants(mfConstants, false);
			parsedProperties.setSomeUndefinedConstants(pfConstants, false);
			// If export is being done to log, switch view to log
			if (exportFile == null)
				logToFront();
			// Start export 
			ExportBuiltModelThread ebmt = new ExportBuiltModelThread(this, GUIMultiModelHandler.LABELS_EXPORT, exportType, exportFile);
			ebmt.setPropertiesFile(parsedProperties);
			ebmt.start();
		} catch (PrismException e) {
			error(e.getMessage());
			return;
		}
	}
	
	public void a_newExperiment()
	{
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Start expt
		experimentAfterReceiveParseNotification = true;
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.REQUEST_MODEL_PARSE));
	}

	public void a_stopExperiment()
	{
		experiments.stop();
	}

	public void a_deleteExperiment()
	{
		experiments.deleteSelected();
	}

	public void a_viewResults()
	{
		GUIExperiment exp;
		int i, n, inds[];

		// get selected experiments
		n = experiments.getSelectedRowCount();
		if (n < 1)
			return;
		inds = experiments.getSelectedRows();
		// show results dialog for reach one
		for (i = 0; i < n; i++) {
			exp = experiments.getExperiment(inds[i]);
			new GUIResultsTable(this.getGUI(), this, exp).show();
		}
	}

	public void a_plotResults()
	{
		GUIExperiment exp;
		Type type;

		// get experiment
		if (experiments.getSelectedRowCount() != 1)
			return;
		exp = experiments.getExperiment(experiments.getSelectedRow());
		// check its type
		type = exp.getPropertyType();
		if (!(type instanceof TypeInt || type instanceof TypeDouble || type instanceof TypeInterval)) {
			message("Can only plot results if the property is of type int or double");
			return;
		}
		// make sure we can actually create a graph, i.e. that there is >1 result
		if (exp.getRangingConstants().size() == 0) {
			message("Cannot create a graph since there is only a single result.");
			return;
		}

		// launch dialog, plot series (modal)
		new GUIGraphPicker(getGUI(), this, exp, graphHandler, true);
	}

	public void a_exportResults(boolean exportMatrix, String sep)
	{
		GUIExperiment exps[];
		int i, n, inds[];

		// get selected experiments
		n = experiments.getSelectedRowCount();
		if (n < 1)
			return;
		exps = new GUIExperiment[n];
		inds = experiments.getSelectedRows();
		for (i = 0; i < n; i++)
			exps[i] = experiments.getExperiment(inds[i]);
		// get filename to save
		if (showSaveFileDialog(sep.equals(", ") ? csvFilter : textFilter) == JFileChooser.APPROVE_OPTION) {
			File file = getChooserFile();
			Thread t = new ExportResultsThread(this, exps, file, exportMatrix, sep);
			t.setPriority(Thread.NORM_PRIORITY);
			t.start();
		}
	}

	//METHODS TO IMPLEMENT GUIPlugin INTERFACE

	public boolean displaysTab()
	{
		return true;
	}

	public JMenu getMenu()
	{
		return propMenu;
	}

	public OptionsPanel getOptions()
	{
		return null;
	}

	public String getTabText()
	{
		return "Properties";
	}

	public JToolBar getToolBar()
	{
		return null;
	}

	public String getXMLIDTag()
	{
		return "";
	}

	public Object getXMLSaveTree()
	{
		return null;
	}

	public void loadXML(Object c)
	{
	}

	// if return value is true, event should not be passed on to any more listeners

	public boolean processGUIEvent(GUIEvent e)
	{
		if (e instanceof GUIModelEvent) {
			GUIModelEvent me = (GUIModelEvent) e;
			if (me.getID() == GUIModelEvent.NEW_MODEL) {
				//New Model
				setParsedModel(null);
				doEnables();
				//newList();
			} else if (me.getID() == GUIModelEvent.MODEL_BUILT) {
				if (me.getBuildValues() != null)
					mfConstants = me.getBuildValues();
				doEnables();
			} else if (me.getID() == GUIModelEvent.MODEL_PARSED) {
				setParsedModel(me.getModulesFile());
				checkForPropertiesToLoad();
				if (verifyAfterReceiveParseNotification)
					verifyAfterParse();
				if (experimentAfterReceiveParseNotification)
					experimentAfterParse();
				if (simulateAfterReceiveParseNotification)
					simulateAfterParse();
				if (exportLabelsAfterReceiveParseNotification)
					exportLabelsAfterParse();
			} else if (me.getID() == GUIModelEvent.MODEL_PARSE_FAILED) {
				argsPropertiesFile = null;
				verifyAfterReceiveParseNotification = false;
				experimentAfterReceiveParseNotification = false;
				simulateAfterReceiveParseNotification = false;
				exportLabelsAfterReceiveParseNotification = false;
			} else if (me.getID() == GUIModelEvent.NEW_LOAD_NOT_RELOAD_MODEL) {
				if (getPrism().getSettings().getBoolean(PrismSettings.PROPERTIES_CLEAR_LIST_ON_LOAD)) {
					a_newList();
				}
			}
		} else if (e instanceof GUIComputationEvent) {
			if (e.getID() == GUIComputationEvent.COMPUTATION_START) {
				setComputing(true);
			} else if (e.getID() == GUIComputationEvent.COMPUTATION_DONE) {
				setComputing(false);
			} else if (e.getID() == GUIComputationEvent.COMPUTATION_ERROR) {
				setComputing(false);
			}
		} else if (e instanceof GUIClipboardEvent && super.getGUI().getFocussedPlugin() == this) {
			GUIClipboardEvent ce = (GUIClipboardEvent) e;

			if (ce.getComponent() == this || ce.getComponent() == propList) {
				if (!computing) {
					if (ce.getID() == GUIClipboardEvent.CUT) {
						a_cut();
					} else if (ce.getID() == GUIClipboardEvent.COPY) {
						a_copy();
					} else if (ce.getID() == GUIClipboardEvent.PASTE) {
						a_paste();
					} else if (ce.getID() == GUIClipboardEvent.DELETE) {
						a_delete();
					} else if (ce.getID() == GUIClipboardEvent.SELECT_ALL) {
						a_selectAll();
					}
				}
			} else {
			}
		} else if (e instanceof GUIPropertiesEvent) {
			GUIPropertiesEvent pr = (GUIPropertiesEvent) e;
			if (pr.getID() == GUIPropertiesEvent.EXPERIMENT_START) {
				stopExperiment.setEnabled(true);
			} else if (pr.getID() == GUIPropertiesEvent.EXPERIMENT_END) {
				stopExperiment.setEnabled(false);
			} else if (pr.getID() == GUIPropertiesEvent.VERIFY_END) {
				a_verifyDone();
			}
		} else if (e instanceof GUIExitEvent) {
			if (e.getID() == GUIExitEvent.REQUEST_EXIT) {
				if (doModificationCheck() != CONTINUE) {
					notifyEventListeners(new GUIExitEvent(GUIExitEvent.CANCEL_EXIT));
					return true;
				}
			}
		}
		return false;
	}

	private void checkForPropertiesToLoad()
	{
		if (argsPropertiesFile != null) {
			Thread t = new LoadPropertiesThread(this, parsedModel, new File(argsPropertiesFile));
			t.setPriority(Thread.NORM_PRIORITY);
			t.start();
			//we clear the variable to avoid loading property file every time a model is parsed.
			argsPropertiesFile = null;
		}
	}

	//METHODS TO IMPLEMENT MouseListner INTERFACE

	public void mouseClicked(MouseEvent e)
	{
		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);
		if (!computing) {
			if (e.getClickCount() >= 2) {
				if (e.getSource() == propList) {
					int index = propList.locationToIndex(e.getPoint());

					if (index != -1) {
						// locationToIndex provides the index to the "nearest"
						// property. We check here if the cursor is actually on
						// the property.
						Rectangle bounds = propList.getCellBounds(index, index);
						if (bounds != null && bounds.contains(e.getPoint())) {
							// Cursor is on the property: select and open editor
							propList.setSelectedIndex(index);
							a_editProperty();
						} else {
							// Cursor is not on the property: open new property editor
							a_newProperty();
						}
					} else {
						// there is no property yet, open new property editor
						a_newProperty();
					}
				} else if (e.getSource() == consTable || e.getSource() == constantsScroll) {
					if (consTable.rowAtPoint(e.getPoint()) == -1) {
						// double-click, not an existing row -> add a new constant
						a_addConstant();
					}
				} else if (e.getSource() == labTable || e.getSource() == labelsScroll) {
					if (labTable.rowAtPoint(e.getPoint()) == -1) {
						// double-click, not an existing row -> add a new label
						a_addLabel();
					}
				}
			}
		}
	}

	public void mouseEntered(MouseEvent e)
	{
		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);
	}

	public void mouseExited(MouseEvent e)
	{
		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);
	}

	public void mousePressed(MouseEvent e)
	{
		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);
		if (!computing) {
			if (e.isPopupTrigger() && e.getSource() == propList) {
				int index = propList.locationToIndex(e.getPoint());
				// if there are no properties selected, select the one under the popup
				if (propList.isSelectionEmpty()) {
					if (index != -1) {
						propList.setSelectedIndex(index);
					}
				} else {
					// if the property under the popup is not already selected, select just that one
					int[] sel = propList.getSelectedIndices();
					boolean valid = false;
					for (int i = 0; i < sel.length; i++) {
						if (sel[i] == index) {
							valid = true;
							break;
						}
					}
					if (!valid) {
						propList.setSelectedIndex(index);
					}
				}

				// disable certain actions if any of the selected propeties are currently being edited
				int[] sel = propList.getSelectedIndices();
				boolean showDeleters = true;
				for (int i = 0; i < sel.length; i++) {
					if (propList.getProperty(sel[i]).isBeingEdited()) {
						showDeleters = false;
						break;
					}
				}

				verifySelected.setEnabled(propList.existsValidSelectedProperties());
				simulate.setEnabled(propList.existsValidSimulatableSelectedProperties());
				details.setEnabled(propList.existsValidSelectedProperties());
				editProperty.setEnabled(propList.getSelectedProperties().size() > 0);

				newExperiment.setEnabled(propList.getNumSelectedProperties() == 1 && propList.getValidSelectedProperties().size() == 1);
				parametric.setEnabled(propList.getNumSelectedProperties() == 1 && propList.getValidSelectedProperties().size() == 1);

				if (showDeleters == false) {
					simulate.setEnabled(false);
					verifySelected.setEnabled(false);
					details.setEnabled(false);
					editProperty.setEnabled(false);
					newExperiment.setEnabled(false);
					parametric.setEnabled(false);
				}

				propertiesPopup.show(e.getComponent(), e.getX(), e.getY());
			} else if (e.isPopupTrigger() && (e.getSource() == consTable || e.getSource() == constantsScroll)) {
				constantsPopup.show(e.getComponent(), e.getX(), e.getY());
			} else if (e.isPopupTrigger() && (e.getSource() == labTable || e.getSource() == labelsScroll)) {
				labelsPopup.show(e.getComponent(), e.getX(), e.getY());
			} else if (e.isPopupTrigger() && (e.getSource() == experiments || e.getSource() == expScroller)) {
				doEnables();
				this.experimentPopup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	@Override
	public boolean canDoClipBoardAction(Action action)
	{
		if (computing)
			return false;

		// disable certain actions if any of the selected propeties are currently being edited
		int[] sel = propList.getSelectedIndices();
		boolean showDeleters = true;
		for (int i = 0; i < sel.length; i++) {
			if (propList.getProperty(sel[i]).isBeingEdited()) {
				showDeleters = false;
				break;
			}
		}

		if (action == GUIPrism.getClipboardPlugin().getPasteAction()) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			return (clipboard.getContents(null) != null);
		} else if (action == GUIPrism.getClipboardPlugin().getCutAction() || action == GUIPrism.getClipboardPlugin().getDeleteAction()) {
			if (!showDeleters)
				return false;
			return (propList.getSelectedProperties().size() > 0);
		} else if (action == GUIPrism.getClipboardPlugin().getCopyAction()) {
			return (propList.getSelectedProperties().size() > 0);
		} else if (action == GUIPrism.getClipboardPlugin().getSelectAllAction()) {
			return true;
		}

		return false;
	}

	public void mouseReleased(MouseEvent e)
	{
		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);
		if (!computing) {
			if (e.isPopupTrigger() && e.getSource() == propList) {
				int index = propList.locationToIndex(e.getPoint());
				// if there are no properties selected, select the one under the popup
				if (propList.isSelectionEmpty()) {
					if (index != -1) {
						propList.setSelectedIndex(index);
					}
				} else {
					// if the property under the popup is not already selected, select just that one
					int[] sel = propList.getSelectedIndices();
					boolean valid = false;
					for (int i = 0; i < sel.length; i++) {
						if (sel[i] == index) {
							valid = true;
							break;
						}
					}
					if (!valid) {
						propList.setSelectedIndex(index);
					}
				}
				// disable certain actions if any of the selected propeties are currently being edited
				int[] sel = propList.getSelectedIndices();
				boolean showDeleters = true;
				for (int i = 0; i < sel.length; i++) {
					if (propList.getProperty(sel[i]).isBeingEdited()) {
						showDeleters = false;
						break;
					}
				}
				verifySelected.setEnabled(propList.existsValidSelectedProperties());
				simulate.setEnabled(propList.existsValidSimulatableSelectedProperties());
				details.setEnabled(propList.existsValidSelectedProperties());
				editProperty.setEnabled(propList.getSelectedProperties().size() > 0);
				newExperiment.setEnabled(propList.getNumSelectedProperties() == 1 && propList.getValidSelectedProperties().size() == 1);
				parametric.setEnabled(propList.getNumSelectedProperties() == 1 && propList.getValidSelectedProperties().size() == 1);

				if (showDeleters == false) {
					simulate.setEnabled(false);
					verifySelected.setEnabled(false);
					details.setEnabled(false);
					editProperty.setEnabled(false);
					newExperiment.setEnabled(false);
					parametric.setEnabled(false);
				}

				propertiesPopup.show(e.getComponent(), e.getX(), e.getY());
			} else if (e.isPopupTrigger() && (e.getSource() == consTable || e.getSource() == constantsScroll)) {
				constantsPopup.show(e.getComponent(), e.getX(), e.getY());

			} else if (e.isPopupTrigger() && (e.getSource() == labTable || e.getSource() == labelsScroll)) {
				labelsPopup.show(e.getComponent(), e.getX(), e.getY());
			} else if (e.isPopupTrigger() && (e.getSource() == experiments || e.getSource() == expScroller)) {
				doEnables();
				this.experimentPopup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	public void componentAdded(ContainerEvent e)
	{
		// notify GUIClipboard
		selectionChangeHandler.notifyListeners(new GUIEvent(1));
	}

	public void componentRemoved(ContainerEvent e)
	{
		// notify GUIClipboard
		selectionChangeHandler.notifyListeners(new GUIEvent(1));
	}

	//METHODS TO IMPLEMENT ListSelectionListener INTERFACE
	public void valueChanged(ListSelectionEvent e)
	{
		ArrayList<GUIProperty> selectedProps = propList.getSelectedProperties();

		// disable certain actions if any of the selected properties are currently being edited
		boolean showDeleters = true;
		for (int i = 0; i < selectedProps.size(); i++) {
			if (((GUIProperty) selectedProps.get(i)).isBeingEdited()) {
				showDeleters = false;
				break;
			}
		}

		verifySelected.setEnabled(propList.existsValidSelectedProperties());
		simulate.setEnabled(propList.existsValidSimulatableSelectedProperties());
		details.setEnabled(propList.existsValidSelectedProperties());
		editProperty.setEnabled(propList.getSelectedProperties().size() > 0);

		if (showDeleters == false) {
			simulate.setEnabled(false);
			verifySelected.setEnabled(false);
			details.setEnabled(false);
			editProperty.setEnabled(false);
		}

		updateCommentLabel();

		//Now do the one for the constants table

		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);

		// notify GUIClipboard
		selectionChangeHandler.notifyListeners(new GUIEvent(1));
	}

	//CONSTRUCTOR HELPER METHODS
	JScrollPane constantsScroll, labelsScroll;
	JTextArea comLabel;

	private void initComponents()
	{
		setupActions();
		//panel
		JSplitPane mainSplit = new JSplitPane();
		{
			JPanel left = new JPanel();
			{
				left.setMinimumSize(new java.awt.Dimension(10, 10));
				JSplitPane leftSc = new JSplitPane();
				{
					JPanel topLeft = new JPanel();
					{
						JScrollPane propScroll = new JScrollPane();
						{
							propList = new GUIPropertiesList(getPrism(), this);
							propList.addListSelectionListener(this);
							propList.addContainerListener(this);
							propList.setToolTipText("Double-click or right-click here to create a new property");
							propScroll.setViewportView(propList);
						}
						JScrollPane comScroll = new JScrollPane();
						comLabel = new JTextArea();
						{
							comLabel.setRows(2);
							comLabel.setEditable(false);
							//comLabel.setPreferredSize(new Dimension(300, 20));
							//comLabel.setMinimumSize(new Dimension(10,10));
						}
						comScroll.setViewportView(comLabel);
						topLeft.setLayout(new BorderLayout());
						topLeft.add(propScroll, BorderLayout.CENTER);
						topLeft.add(comScroll, BorderLayout.SOUTH);
						topLeft.setBorder(new TitledBorder("Properties"));
					}
					JSplitPane bottomLeft = new JSplitPane();
					{
						constantsScroll = new JScrollPane();
						constantsScroll.setToolTipText("Double-click or right-click here to create a new constant");
						{
							consTable = new GUIPropConstantList(this);
							consTable.setBackground(Color.white);
							consTable.addMouseListener(this);
							constantsScroll.setViewportView(consTable);
							constantsScroll.addMouseListener(this);
							constantsScroll.setBorder(new TitledBorder("Constants"));
						}
						labelsScroll = new JScrollPane();
						labelsScroll.setToolTipText("Double-click or right-click here to create a new label");
						{
							labTable = new GUIPropLabelList(this);
							labTable.setBackground(Color.white);
							labTable.addMouseListener(this);
							labelsScroll.setViewportView(labTable);
							labelsScroll.addMouseListener(this);
							labelsScroll.setBorder(new TitledBorder("Labels"));
						}
						bottomLeft.setOrientation(JSplitPane.VERTICAL_SPLIT);
						bottomLeft.setTopComponent(constantsScroll);
						bottomLeft.setBottomComponent(labelsScroll);
						bottomLeft.setDividerLocation(0.5);
						bottomLeft.setDividerSize(5);
						bottomLeft.setResizeWeight(0.5);
					}
					leftSc.setOrientation(JSplitPane.VERTICAL_SPLIT);
					leftSc.setTopComponent(topLeft);
					leftSc.setBottomComponent(bottomLeft);
					leftSc.setDividerLocation(0.5);
					leftSc.setDividerSize(5);
					leftSc.setResizeWeight(0.5);

				}
				left.setLayout(new BorderLayout());
				left.add(leftSc, BorderLayout.CENTER);
			}
			JPanel right = new JPanel();
			{
				JSplitPane rightSplit = new JSplitPane();
				{
					JPanel topRight = new JPanel();
					{
						JToolBar stopTool = new JToolBar();
						{
							JButton b = new JButton(stopExperiment);
							b.setToolTipText("Stop Current Experiment");
							stopTool.add(b);

							stopTool.setFloatable(false);
						}
						expScroller = new JScrollPane();
						{
							experiments = new GUIExperimentTable(this);
							experiments.addMouseListener(this);
							expScroller.addMouseListener(this);
							expScroller.setViewportView(experiments);
						}
						topRight.setLayout(new BorderLayout());
						topRight.add(stopTool, BorderLayout.NORTH);
						topRight.add(expScroller, BorderLayout.CENTER);
					}
					JPanel bottomRight = new JPanel(new GridLayout(1, 1));
					{
						graphHandler = new GUIGraphHandler(this.getGUI(), this, true);
						bottomRight.add(graphHandler);

						bottomRight.setPreferredSize(new Dimension(300, 300));
						//graphHandler.addGraph();
						//graphHandler.addGraph();
					}
					rightSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
					rightSplit.setTopComponent(topRight);
					rightSplit.setBottomComponent(bottomRight);
					rightSplit.setDividerLocation(0.5);
					rightSplit.setDividerSize(5);
					rightSplit.setResizeWeight(0.5);
				}

				right.setLayout(new BorderLayout());
				right.setBorder(new TitledBorder("Experiments"));
				right.add(rightSplit, BorderLayout.CENTER);
			}

			mainSplit.setLeftComponent(left);
			mainSplit.setRightComponent(right);
			mainSplit.setDividerLocation(0.5);
			mainSplit.setOneTouchExpandable(true);
			mainSplit.setResizeWeight(0.5);
		}

		JPanel topPanel = new JPanel();
		{
			fileTextField = new JTextField();
			{
				fileTextField.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
				fileTextField.setBorder(new javax.swing.border.EtchedBorder());
				fileTextField.setMinimumSize(new java.awt.Dimension(40, 25));
				fileTextField.setEditable(false);
				fileTextField.setBackground(null);
			}

			//progress = new JProgressBar(0, 100);
			topPanel.setLayout(new BorderLayout());
			//topPanel.add(progress, BorderLayout.WEST);
			topPanel.add(fileTextField, BorderLayout.CENTER);
		}
		setLayout(new BorderLayout());
		add(mainSplit, BorderLayout.CENTER);
		add(topPanel, BorderLayout.NORTH);
		//menu
		propMenu = new JMenu("Properties");
		{
			//JSplitter split = new JSeparator();
			propMenu.add(newProps);
			propMenu.add(new JSeparator());
			propMenu.add(openProps);
			propMenu.add(insertProps);
			propMenu.add(new JSeparator());
			propMenu.add(saveProps);
			propMenu.add(savePropsAs);
			propMenu.add(new JSeparator());
			propMenu.add(verifySelected);
			propMenu.add(simulate);
			propMenu.add(newExperiment);
			//propMenu.add(parametric);
			JMenu exportlabelsMenu = new JMenu("Export labels");
			exportlabelsMenu.setMnemonic('E');
			exportlabelsMenu.setIcon(GUIPrism.getIconFromImage("smallExport.png"));
			exportlabelsMenu.add(exportLabelsPlain);
			exportlabelsMenu.add(exportLabelsMatlab);
			propMenu.add(exportlabelsMenu);
			propMenu.setMnemonic('P');
		}
		createPopups();
		//file filters
		propsFilter = new FileNameExtensionFilter("PRISM properties (*.props, *.pctl, *.csl)", "props", "pctl", "csl");
		labFilters = new HashMap<String,FileFilter>();
		labFilters.put("lab", new FileNameExtensionFilter("Label files (*.lab)", "lab"));
		labFilters.put("txt", new FileNameExtensionFilter("Plain text files (*.txt)", "txt"));
		textFilter =  new FileNameExtensionFilter("Plain text files (*.txt)", "txt");
		csvFilter =  new FileNameExtensionFilter("Comma-separated values (*.csv)", "csv");
		matlabFilter = new FileNameExtensionFilter("Matlab files (*.m)", "m");
	}

	private void createPopups()
	{
		propertiesPopup = new JPopupMenu();

		propertiesPopup.add(editProperty);
		propertiesPopup.add(newProperty);
		propertiesPopup.add(new JSeparator());
		propertiesPopup.add(verifySelected);
		propertiesPopup.add(simulate);
		propertiesPopup.add(newExperiment);
		//propertiesPopup.add(parametric);
		propertiesPopup.add(details);
		propertiesPopup.add(new JSeparator());
		propertiesPopup.add(GUIPrism.getClipboardPlugin().getCutAction());
		propertiesPopup.add(GUIPrism.getClipboardPlugin().getCopyAction());
		propertiesPopup.add(GUIPrism.getClipboardPlugin().getPasteAction());
		propertiesPopup.add(GUIPrism.getClipboardPlugin().getDeleteAction());
		propertiesPopup.add(new JSeparator());
		propertiesPopup.add(GUIPrism.getClipboardPlugin().getSelectAllAction());

		constantsPopup = new JPopupMenu();

		constantsPopup.add(newConstant);
		constantsPopup.add(removeConstant);

		labelsPopup = new JPopupMenu();
		labelsPopup.add(newLabel);
		labelsPopup.add(removeLabel);

		consTable.addMouseListener(this);
		propList.addMouseListener(this);

		experimentPopup = new JPopupMenu();

		experimentPopup.add(newExperiment);
		experimentPopup.add(deleteExperiment);
		experimentPopup.add(new JSeparator());
		experimentPopup.add(viewResults);
		experimentPopup.add(plotResults);
		JMenu exportResultsMenu = new JMenu("Export results");
		exportResultsMenu.setMnemonic('E');
		exportResultsMenu.setIcon(GUIPrism.getIconFromImage("smallExport.png"));
		exportResultsMenu.add(exportResultsListText);
		exportResultsMenu.add(exportResultsListCSV);
		exportResultsMenu.add(exportResultsMatrixText);
		exportResultsMenu.add(exportResultsMatrixCSV);
		experimentPopup.add(exportResultsMenu);
	}

	private void setupActions()
	{

		newProps = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_newList();
			}
		};
		newProps.putValue(Action.LONG_DESCRIPTION, "Clears the current properties list, and the current active properties list.");
		//newProps.putValue(Action.SHORT_DESCRIPTION, "New properties list");
		newProps.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		newProps.putValue(Action.NAME, "New properties list");
		newProps.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallNew.png"));
		newProps.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK));

		openProps = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_open();
			}
		};
		openProps.putValue(Action.LONG_DESCRIPTION, "Opens a properties list, checking that it is valid according to the current parsed model");
		//openProps.putValue(Action.SHORT_DESCRIPTION, "Open properties list");
		openProps.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		openProps.putValue(Action.NAME, "Open properties list...");
		openProps.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallOpen.png"));
		openProps.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK));

		saveProps = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_save();
			}
		};
		saveProps.putValue(Action.LONG_DESCRIPTION,
				"Saves all properties to the current active properties list, if non exists the user is prompted with a dialog.");
		// saveProps.putValue(Action.SHORT_DESCRIPTION, "Save properties list");
		saveProps.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		saveProps.putValue(Action.NAME, "Save properties list");
		saveProps.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSave.png"));
		saveProps.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK));

		savePropsAs = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_saveAs();
			}
		};
		savePropsAs.putValue(Action.LONG_DESCRIPTION, "Saves all properties to a new file selected by the user from a dialog.");
		//savePropsAs.putValue(Action.SHORT_DESCRIPTION, "Save properties list As...");
		savePropsAs.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
		savePropsAs.putValue(Action.NAME, "Save properties list as...");
		savePropsAs.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSaveAs.png"));

		insertProps = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_insert();
			}
		};
		insertProps.putValue(Action.LONG_DESCRIPTION,
				"Inserts properties from user selected file into the properties list.  The active property file remains the same however.");
		//insertProps.putValue(Action.SHORT_DESCRIPTION, "Insert properties list");
		insertProps.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
		insertProps.putValue(Action.NAME, "Insert properties list...");
		insertProps.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallAdd.png"));

		simulate = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_simulateSelected();
			}
		};
		simulate.putValue(Action.LONG_DESCRIPTION, "Calls the PRISM simulator to approximately model check the selected properties against the parsed model.");
		simulate.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		simulate.putValue(Action.NAME, "Simulate");
		simulate.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSimulate.png"));
		simulate.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));

		details = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_detailSelected();
			}
		};
		details.putValue(Action.LONG_DESCRIPTION, "Shows the details for the currently selected properties in a dialog box.");
		details.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		details.putValue(Action.NAME, "Show details");
		details.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDetails.png"));

		verifySelected = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_verifySelected();
			}
		};
		verifySelected
				.putValue(
						Action.LONG_DESCRIPTION,
						"Model checks the selected properties against the model that is built.  If there is no built model, the parsed model is automatically built.  If the parsed model has changed since the last build, the user is prompted as to whether they wish to re-build the model.  If the model text has been modified since the last build, the user is asked whether they want to re-parse and re-build.");
		//verifySelected.putValue(Action.SHORT_DESCRIPTION, "Verify Selected Properties");
		verifySelected.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_V));
		verifySelected.putValue(Action.NAME, "Verify");
		verifySelected.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallTick.png"));
		verifySelected.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

		newProperty = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_newProperty();
			}
		};
		newProperty.putValue(Action.LONG_DESCRIPTION, "Brings up a dialog to add a new property to the list.");
		newProperty.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		newProperty.putValue(Action.NAME, "Add");
		newProperty.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallAdd.png"));

		editProperty = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_editProperty();
			}
		};
		editProperty.putValue(Action.LONG_DESCRIPTION, "Brings up a dialog to edit a selected property.");
		editProperty.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
		editProperty.putValue(Action.NAME, "Edit");
		editProperty.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallEdit.png"));

		newConstant = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_addConstant();
			}
		};
		newConstant.putValue(Action.LONG_DESCRIPTION, "Adds a new constant to the constants list");
		newConstant.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		newConstant.putValue(Action.NAME, "Add constant");
		newConstant.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallAdd.png"));

		removeConstant = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_removeSelectedConstants();
			}
		};
		removeConstant.putValue(Action.LONG_DESCRIPTION, "Deletes selected constants");
		removeConstant.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		removeConstant.putValue(Action.NAME, "Delete constant");
		removeConstant.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.png"));

		newLabel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_addLabel();
			}
		};
		newLabel.putValue(Action.LONG_DESCRIPTION, "Adds a new Label to the Labels list");
		newLabel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		newLabel.putValue(Action.NAME, "Add label");
		newLabel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallAdd.png"));

		removeLabel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_removeSelectedLabels();
			}
		};
		removeLabel.putValue(Action.LONG_DESCRIPTION, "Deletes selected Labels");
		removeLabel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		removeLabel.putValue(Action.NAME, "Delete label");
		removeLabel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.png"));

		newExperiment = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_newExperiment();
			}
		};
		newExperiment.putValue(Action.LONG_DESCRIPTION, "Creates a new experiment");
		newExperiment.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		newExperiment.putValue(Action.NAME, "New experiment");
		newExperiment.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExperiment.png"));
		newExperiment.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));

		deleteExperiment = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_deleteExperiment();
			}
		};
		deleteExperiment.putValue(Action.LONG_DESCRIPTION, "Deletes the selected experiment");
		deleteExperiment.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		deleteExperiment.putValue(Action.NAME, "Delete experiment");
		deleteExperiment.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.png"));

		viewResults = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_viewResults();
			}
		};
		viewResults.putValue(Action.LONG_DESCRIPTION, "View the results of this experiment");
		viewResults.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_V));
		viewResults.putValue(Action.NAME, "View results");
		viewResults.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallView.png"));

		plotResults = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_plotResults();
			}
		};
		plotResults.putValue(Action.LONG_DESCRIPTION, "Plot the results of this experiment in a graph series");
		plotResults.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		plotResults.putValue(Action.NAME, "Plot results");
		plotResults.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileGraph.png"));

		parametric = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_newExperiment();
			}
		};
		parametric.putValue(Action.LONG_DESCRIPTION, "Perform parametric model checking");
		parametric.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		parametric.putValue(Action.NAME, "Parametric");
		parametric.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileGraph.png"));

		exportResultsListText = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportResults(false, "\t");
			}
		};
		exportResultsListText.putValue(Action.LONG_DESCRIPTION, "Export the results of this experiment to a text file");
		exportResultsListText.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
		exportResultsListText.putValue(Action.NAME, "List (text)");
		exportResultsListText.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportResultsListCSV = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportResults(false, ", ");
			}
		};
		exportResultsListCSV.putValue(Action.LONG_DESCRIPTION, "Export the results of this experiment to a CSV file");
		exportResultsListCSV.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
		exportResultsListCSV.putValue(Action.NAME, "List (CSV)");
		exportResultsListCSV.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallMatrix.png"));

		exportResultsMatrixText = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportResults(true, "\t");
			}
		};
		exportResultsMatrixText.putValue(Action.LONG_DESCRIPTION, "Export the results of this experiment to a file in matrix form");
		exportResultsMatrixText.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportResultsMatrixText.putValue(Action.NAME, "Matrix (text)");
		exportResultsMatrixText.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportResultsMatrixCSV = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportResults(true, ", ");
			}
		};
		exportResultsMatrixCSV.putValue(Action.LONG_DESCRIPTION, "Export the results of this experiment to a file in matrix form");
		exportResultsMatrixCSV.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportResultsMatrixCSV.putValue(Action.NAME, "Matrix (CSV)");
		exportResultsMatrixCSV.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallMatrix.png"));

		exportLabelsPlain = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportLabels(Prism.EXPORT_PLAIN);
			}
		};
		exportLabelsPlain.putValue(Action.LONG_DESCRIPTION, "Exports the model and property file's labels and their satisfying states to a plain text file");
		exportLabelsPlain.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		exportLabelsPlain.putValue(Action.NAME, "Plain text file");
		exportLabelsPlain.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportLabelsMatlab = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportLabels(Prism.EXPORT_MATLAB);
			}
		};
		exportLabelsMatlab.putValue(Action.LONG_DESCRIPTION, "Exports the model and property file's labels and their satisfying states to a Matlab file");
		exportLabelsMatlab.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportLabelsMatlab.putValue(Action.NAME, "Matlab file");
		exportLabelsMatlab.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileMatlab.png"));
		
		stopExperiment = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_stopExperiment();
			}
		};
		stopExperiment.putValue(Action.LONG_DESCRIPTION, "Stops the Experiment that is currently running");
		stopExperiment.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallStop.png"));
		stopExperiment.setEnabled(false);
	}

	/**
	 * Getter for property propList.
	 * @return Value of property propList.
	 */
	public userinterface.properties.GUIPropertiesList getPropList()
	{
		return propList;
	}

	/**
	 * Getter for property consTable.
	 * @return Value of property consTable.
	 */
	public userinterface.properties.GUIPropConstantList getConsTable()
	{
		return consTable;
	}

	/**
	 * Getter for property labTable.
	 * @return Value of property labTable.
	 */
	public userinterface.properties.GUIPropLabelList getLabTable()
	{
		return labTable;
	}

	public void notifySettings(PrismSettings settings)
	{
		displayFontFast = settings.getFontColorPair(PrismSettings.PROPERTIES_FONT).f;
		setFont(displayFontFast);
		backgroundFast = new Color(202, 225, 255);
		warningFast = settings.getColor(PrismSettings.PROPERTIES_WARNING_COLOUR);
		repaint();
	}

	public static DataFlavor getGUIClipboardPropertiesDataFlavor()
	{
		return new DataFlavor(GUIClipboardProperties.class, "PRISM Property List");
	}

	/**
	 * A class that allows sets of properties to be put on the clipboard.
	 */
	public class GUIClipboardProperties implements Transferable
	{
		private ArrayList<GUIProperty> listOfProperties;
		private StringSelection stringRepresentation;

		public GUIClipboardProperties(ArrayList<GUIProperty> listOfProperties)
		{
			this.listOfProperties = listOfProperties;
			String tmpString = "";
			for (int i = 0; i < listOfProperties.size(); i++) {
				GUIProperty gp = listOfProperties.get(i);
				if (gp.getComment().trim().length() > 0) {
					tmpString += "//" + gp.getComment() + "\n";
				}
				tmpString += gp.getPropString();
				if (i != listOfProperties.size() - 1)
					tmpString += "\n";
			}

			stringRepresentation = new StringSelection(tmpString);
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
		{
			if (flavor.getRepresentationClass() == this.getClass()) {
				return this;
			} else {
				return this.stringRepresentation.getTransferData(flavor);
			}
		}

		public DataFlavor[] getTransferDataFlavors()
		{
			DataFlavor[] stringFlavors = stringRepresentation.getTransferDataFlavors();

			DataFlavor[] allFlavors = new DataFlavor[stringFlavors.length + 1];
			allFlavors[0] = GUIMultiProperties.getGUIClipboardPropertiesDataFlavor();

			for (int i = 0; i < stringFlavors.length; i++) {
				allFlavors[i + 1] = stringFlavors[i];
			}

			return allFlavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			return (stringRepresentation.isDataFlavorSupported(flavor) || flavor.equals(GUIMultiProperties.getGUIClipboardPropertiesDataFlavor()));
		}

		public ArrayList<GUIProperty> getProperties()
		{
			return listOfProperties;
		}
	}
}
