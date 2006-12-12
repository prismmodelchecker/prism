//==============================================================================
//
//	Copyright (c) 2002-2006, Andrew Hinton, Dave Parker, Mark Kattenbelt
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
import userinterface.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import userinterface.model.*;
import userinterface.properties.computation.*;
import prism.*;
import parser.*;
import java.io.*;
import java.util.*;
import userinterface.util.*;
import userinterface.simulator.*;
import userinterface.simulator.networking.*;
import java.awt.datatransfer.*;
/**
 *  Properties panel designed to handle constants as well as properties.
 *  Also designed to handle experiments.
 *  @author  ug60axh
 */
public class GUIMultiProperties extends GUIPlugin implements MouseListener, ListSelectionListener, PrismSettingsListener
{
	//CONSTANTS
	public static final int CONTINUE = 0;
	public static final int CANCEL   = 1;
	
	public static final int WARN_INVALID_PROPS = 1;
	public static final int NEVER_INVALID_PROPS = 2;
	
	//ATTRIBUTES
	
	// current model (gets updated only by event listening to GUIModel)
	private ModulesFile parsedModel;
	private Model builtModel;
	private Values lastBuildValues;
	
	//state
	private boolean modified;
	private boolean modifiedSinceBuild;
	private boolean computing;
	private boolean verifyAfterReceiveParseNotification,
	verifyAfterReceiveBuildNotification,
	experimentAfterReceiveParseNotification,
	simulateAfterReceiveParseNotification;
	private PropertiesFile parsedProperties;
	private ArrayList propertiesToBeVerified;
	private File activeFile;
	private Values lastPFConstants;
	private Values lastSimulateValues;
	
	//gui
	private GUIPrismFileFilter propsFilter[];
	private GUIPrismFileFilter textFilter[];
	private JMenu propMenu;
	private JPopupMenu propertiesPopup, constantsPopup, labelsPopup, experimentPopup;
	private GUIExperimentTable experiments;
	private GUIGraphHandler graphHandler;
	private JScrollPane expScroller;
	private JLabel fileLabel;
	private Vector clipboardVector;
	
	private Action newProps, openProps, saveProps, savePropsAs, insertProps,
	verifyAll, verifySelected, cutAction, copyAction, pasteAction, deleteAction,
	newProperty, editProperty, selectAllAction, newConstant,
	removeConstant, newLabel, removeLabel, newExperiment, deleteExperiment, stopExperiment,
	viewResults, plotResults, exportResults, simulate, details;
	
	//current properties
	private GUIPropertiesList propList;
	private GUIPropConstantList consTable;
	private GUIPropLabelList labTable;
	
	// Cached copies of settings
	private Font displayFontFast;
	private Color backgroundFast, warningFast;
	
	//CONSTRUCTORS
	
	/** Creates a new instance of GUIMultiProperties */
	public GUIMultiProperties(GUIPrism pr, GUISimulator simulator)
	{
		super(pr);
		simulator.setGUIProb(this); // link required
		initComponents();
		a_newList();
		setBuiltModel(null);
		setParsedModel(null);
		//options = new GUIPropertiesOptions(this);
	}
	
	public void takeCLArgs(String args[])
	{
		// disabled for now - need to sort out so this doesn't happen until model is fully parsed
		// 		if (args.length > 1) {
		// 			Thread t = new LoadPropertiesThread(this, parsedModel, new File(args[1]));
		// 			t.setPriority(Thread.NORM_PRIORITY);
		// 			t.start();
		// 		}
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
		return getPrism().getSettings().getInteger(PrismSettings.PROPERTIES_ADDITION_STRATEGY)+1; //note the correction
	}
	
	/* UPDATE METHODS */
	
	public void repaintList()
	{
		propList.repaint();
	}
	
	protected void verifyAfterBuild()
	{
		verifyAfterReceiveBuildNotification = false;
		propList.setEnabled(true);
		consTable.setEnabled(true);
		labTable.setEnabled(true);
		if(builtModel != null && parsedProperties != null && propertiesToBeVerified != null)
		{
			Thread t = new ModelCheckThread(this, builtModel, parsedProperties, propertiesToBeVerified, lastBuildValues, lastPFConstants);
			t.setPriority(Thread.NORM_PRIORITY);
			t.start();
		}
	}
	
	protected void verifyAfterParse()
	{
		verifyAfterReceiveParseNotification = false;
		ArrayList validGUIProperties;
		Values buildValues;
		UndefinedConstants uCon;
		try
		{
			parsedProperties = getPrism().parsePropertiesString(parsedModel, getLabelsString()+"\n"+getConstantsString()+"\n"+propList.getValidSelectedString());
			validGUIProperties = propList.getValidSelectedProperties();
		}
		catch(ParseException e)
		{
			error(e.getShortMessage());
			return;
		}
		catch(PrismException e)
		{
			error(e.getMessage());
			return;
		}
		
		//find out any undefined constants
		try
		{
			uCon = new UndefinedConstants(parsedModel, parsedProperties);
			if(uCon.getMFNumUndefined() + uCon.getPFNumUndefined() > 0)
			{
				int result = GUIConstantsPicker.defineConstantsWithDialog(this.getGUI(), uCon, lastBuildValues, lastPFConstants);
				if(result != GUIConstantsPicker.VALUES_DONE)
					return;
			}
			buildValues = uCon.getMFConstantValues();
			lastPFConstants = uCon.getPFConstantValues();
			parsedProperties.setUndefinedConstants(lastPFConstants);
		}
		catch(PrismException e)
		{
			error(e.getMessage());
			return;
		}
		
		verifyAfterReceiveBuildNotification = true;
		propertiesToBeVerified = validGUIProperties;
		propList.setEnabled(false);
		consTable.setEnabled(false);
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.REQUEST_MODEL_BUILD, buildValues));
	}
	
	public void simulateAfterParse()
	{
		simulateAfterReceiveParseNotification = false;
		ArrayList validGUIProperties, simulatableGUIProperties;
		UndefinedConstants uCon;
		try
		{
			parsedProperties = getPrism().parsePropertiesString(parsedModel, getLabelsString()+"\n"+getConstantsString()+"\n"+propList.getValidSelectedString());
			validGUIProperties = propList.getValidSelectedProperties();
			if (validGUIProperties.size() == 0) {
				error("None of the selected properties are suitable for simulation");
				return;
			}
		}
		catch(ParseException e)
		{
			error(e.getShortMessage());
			return;
		}
		catch(PrismException e)
		{
			error(e.getMessage());
			return;
		}
		
		// See which of the selected properties are ok for simulation
		simulatableGUIProperties = new ArrayList();
		for(int i = 0; i < validGUIProperties.size(); i++)
		{
			GUIProperty guiP = (GUIProperty)validGUIProperties.get(i);
			try
			{
				getPrism().checkPropertyForSimulation(parsedModel, parsedProperties, guiP.getPCTLProperty());
				simulatableGUIProperties.add(guiP);
			}
			catch(PrismException e)
			{
				// do nothing
			}
		}
		if (simulatableGUIProperties.size() == 0) {
			error("None of the selected properties are suitable for simulation");
			return;
		}
		if (simulatableGUIProperties.size() < validGUIProperties.size()) {
			int q = questionYesNo("Warning", "Warning: Some of the selected properties are not suitable for simulation. Continue?");
			if (q != 0) return;
		}
		
		//find out any undefined constants
		try
		{
			uCon = new UndefinedConstants(parsedModel, parsedProperties);
			if(uCon.getMFNumUndefined() + uCon.getPFNumUndefined() > 0)
			{
				int result = GUIConstantsPicker.defineConstantsWithDialog(this.getGUI(), uCon, lastSimulateValues, lastPFConstants);
				if(result != GUIConstantsPicker.VALUES_DONE)
					return;
			}
			
			lastSimulateValues = uCon.getMFConstantValues();
			lastPFConstants = uCon.getPFConstantValues();
			parsedModel.setUndefinedConstants(lastSimulateValues);
			parsedProperties.setUndefinedConstants(lastPFConstants);
			
			SimulationInformation info = GUISimulationPicker.defineSimulationWithDialog(this.getGUI(), parsedModel.getInitialValues(), parsedModel);
			if(info == null) return;
			Values initialState = info.getInitialState();
			
			int noIterations = info.getNoIterations();
			int maxPathLength = info.getMaxPathLength();
			
			if(parsedModel != null && parsedProperties != null && validGUIProperties != null)
			{
				if(info.isDistributed())
				{
					new GUISimulatorDistributionDialog(getGUI(), getPrism().getSimulator(), true).show(this, parsedModel, parsedProperties, validGUIProperties,  info);
				}
				else
				{
					Thread t = new SimulateModelCheckThread(this, parsedModel, parsedProperties, validGUIProperties, lastSimulateValues, lastPFConstants, initialState, noIterations, maxPathLength, info);
					t.setPriority(Thread.NORM_PRIORITY);
					t.start();
				}
			}
		}
		catch(PrismException e)
		{
			error(e.getMessage());
			return;
		}
	}
	
	public void experimentAfterParse()
	{
		experimentAfterReceiveParseNotification = false;
		GUIProperty gp = propList.getProperty(propList.getSelectedIndex());
		int type;
		
		try
		{
			// parse property to be used for experiment
			parsedProperties = getPrism().parsePropertiesString(parsedModel, getLabelsString()+"\n"+getConstantsString()+"\n"+gp.getPropString());
			if (parsedProperties.getNumProperties() <= 0)
			{
				error("There are no properties selected");
				return;
			}
			if (parsedProperties.getNumProperties() > 1)
			{
				error("Experiments can only be created for a single property");
				return;
			}
			
			// check the type of the property
			type = parsedProperties.getProperty(0).getType();
		}
		catch(ParseException e)
		{
			error(e.getShortMessage());
			return;
		}
		catch(PrismException e)
		{
			error(e.getMessage());
			return;
		}
		
		// sort out undefined constants
		UndefinedConstants uCon = new UndefinedConstants(parsedModel, parsedProperties);
		boolean showGraphDialog = false;
		boolean useSimulation = false;
		if(uCon.getMFNumUndefined()+uCon.getPFNumUndefined() == 0)
		{
			error("Cannot create an experiment because there are no constants with undefined values");
			return;
		}
		int result = GUIExperimentPicker.defineConstantsWithDialog(this.getGUI(), uCon, type==Expression.INT || type==Expression.DOUBLE, gp.isValidForSimulation());
		if(result == GUIExperimentPicker.VALUES_DONE_SHOW_GRAPH || result == GUIExperimentPicker.VALUES_DONE_SHOW_GRAPH_AND_SIMULATE)
		{
			showGraphDialog = true;
		}
		else if(result == GUIExperimentPicker.CANCELLED) return;
		if(result == GUIExperimentPicker.VALUES_DONE_SIMULATE || result == GUIExperimentPicker.VALUES_DONE_SHOW_GRAPH_AND_SIMULATE)
		{
			useSimulation = true;
		}
		
		//if we are using simulation, make sure the property is ok
		if(useSimulation)
		{
			try
			{
				getPrism().checkPropertyForSimulation(parsedModel, parsedProperties, gp.getPCTLProperty());
			}
			catch(PrismException e)
			{
				error("Property is not suitable for simulation: " + e.getMessage());
				return;
			}
		}
		
		// make sure we can actually create a graph, i.e. that there is >1 result
		if (showGraphDialog) if (uCon.getRangingConstants().size() == 0)
		{
			message("Cannot create a graph since there is only a single result.");
			showGraphDialog = false;
		}
		
		// Use these values to create a new experiment
		int i = experiments.newExperiment(parsedProperties, uCon, parsedModel, useSimulation);
		
		// start the experiment, via the graph dialog if appropriate
		if(showGraphDialog)
		{
			GUIGraphPicker ggp = new GUIGraphPicker(getGUI(), this, experiments.getExperiment(i).getResults(), graphHandler);
			ggp.startGraphExperiment(experiments, i);
		}
		else
		{
			experiments.startExperiment(i);
		}
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
		if(index < 0)
		{
			if(pctl!=null)
			{
				propList.addProperty(pctl, comment);
				setModified(true);
			}
		}
		else
		{
			GUIProperty gp = propList.getProperty(index);
			gp.setBeingEdited(false);
			if(pctl != null)
			{
				gp.setPropString(pctl, parsedModel, getConstantsString(), getLabelsString());
				gp.setComment(comment);
				setModified(true);
			}
			// Force repaint because we modified the GUIProperty directly
			repaintList();
		}
		updateCommentLabel();
	}
	
	public void cancelProperty(String id)
	{
		int index = propList.getIndexOf(id);
		if(index >= 0)
		{
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
	}
	
	protected void setActiveFile(File f)
	{
		activeFile = f;
		setActiveFileLabel();
	}
	
	protected void setActiveFileLabel()
	{
		fileLabel.setText("Properties list: " + ((activeFile == null)?"<Untitled>":activeFile.getPath()) + (modified?"*":""));
	}
	
	protected void setParsedModel(ModulesFile m)
	{
		parsedModel = m;
		consTable.validateConstants();
		propList.validateProperties();
		doEnables();
	}
	
	protected void setBuiltModel(Model m)
	{
		builtModel = m;
		doEnables();
	}
	
	protected void doEnables()
	{
		// properties panel
		setEnabled(true);
		setTabEnabled(true);
		// properties menu
		newProps.setEnabled			(!computing);
		openProps.setEnabled		(!computing);
		insertProps.setEnabled		(!computing);
		saveProps.setEnabled		(!computing);
		savePropsAs.setEnabled		(!computing);
		verifyAll.setEnabled		(!computing && parsedModel != null && propList.getNumValidProperties() > 0);
		simulate.setEnabled			(!computing && parsedModel != null && propList.getValidSimulatableSelectedProperties().size() > 0);
		verifySelected.setEnabled	(!computing && parsedModel != null && propList.getValidSelectedProperties().size() > 0);
		details.setEnabled			(!computing && parsedModel != null && propList.getValidSelectedProperties().size() > 0);
		// properties list
		propList.setEnabled 		(!computing);
		newProperty.setEnabled		(!computing);
		editProperty.setEnabled 	(!computing && propList.getSelectedProperties().size() > 0);
		cutAction.setEnabled		(!computing);
		copyAction.setEnabled		(!computing);
		pasteAction.setEnabled		(!computing);
		deleteAction.setEnabled 	(!computing);
		selectAllAction.setEnabled	(!computing);
		// constants list
		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		// label list
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);
		
		// newExperiment: enabled if there is exactly one prop selected and it is valid
		newExperiment.setEnabled(propList.getNumSelectedProperties() == 1 && propList.getValidSelectedProperties().size() == 1);
		// deleteExperiments: enabled if one or more experiments selected
		deleteExperiment.setEnabled(experiments.getSelectedRowCount() > 0);
		// viewResults: enabled if exactly one experiment is selected
		viewResults.setEnabled(experiments.getSelectedRowCount() == 1);
		// plotResults: enabled if exactly one experiment is selected and its type is int/double
		if (experiments.getSelectedRowCount() == 1)
		{
			GUIExperiment exp = experiments.getExperiment(experiments.getSelectedRow());
			int t=0;
			try
			{
				t = exp.getPropertyType();
				plotResults.setEnabled(t == Expression.INT || t == Expression.DOUBLE);
			}
			catch (PrismException e)
			{ plotResults.setEnabled(false); }
		}
		else
		{
			plotResults.setEnabled(false);
		}
		// exportResults: enabled if exactly one experiment is selected
		exportResults.setEnabled(experiments.getSelectedRowCount() == 1);
	}
	
	public int doModificationCheck()
	{
		if(modified)
		{
			if(activeFile == null)
			{
				String[] selection =
				{"Yes", "No", "Cancel"};
				int selectionNo = -1;
				selectionNo = optionPane("Properties list has been modified.\nDo you wish to save it?", "Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, selection, selection[0]);
				switch(selectionNo)
				{
					case 0: return a_save();
					case 1: return CONTINUE;
					case 2: return CANCEL;
					default: return CANCEL;
				}
			}
			else
			{
				String[] selection =
				{"Yes", "No", "Save As...", "Cancel"};
				int selectionNo = -1;
				selectionNo = optionPane("Properties list has been modified.\nDo you wish to save it?", "Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, selection, selection[0]);
				switch(selectionNo)
				{
					case 0: return a_save();
					case 1: return CONTINUE;
					case 2: return a_saveAs();
					case 3: return CANCEL;
					default: return CANCEL;
				}
			}
		}
		return CONTINUE;
	}
	
	private void updateCommentLabel()
	{
		ArrayList selectedProps = propList.getSelectedProperties();
		if(selectedProps.size() == 1)
		{
			GUIProperty p = (GUIProperty)selectedProps.get(0);
			comLabel.setText(p.getComment());
		}
		else
		{
			comLabel.setText("");
		}
	}
	
	//ACTION METHODS
	
	public void a_newList()
	{
		if (doModificationCheck() != CONTINUE) return;
		
		propList.deleteAll();
		consTable.newList();
		labTable.newList();
		setModified(false);
		modifiedSinceBuild = true;
		setActiveFile(null);
		doEnables();
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.PROPERTIES_LIST_CHANGED));
	}
	
	public void a_open()
	{
		if (doModificationCheck() != CONTINUE) return;
		
		if (showOpenFileDialog(propsFilter, propsFilter[0]) == JFileChooser.APPROVE_OPTION)
		{
			File file = getChooserFile();
			if (file == null)
			{
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
		if(activeFile == null)
		{
			return a_saveAs();
		}
		else
		{
			// only allow save if all props valid (can't reopen file otherwise)
			if (propList.getNumProperties() != propList.getNumValidProperties())
			{
				message("Cannot save properties list: some properties are invalid");
				return CANCEL;
			}
			else
			{
				// do save
				try
				{
					setTaskBarText("Saving properties...");
					PrintWriter out = new PrintWriter(new FileWriter(activeFile));
					out.print(propList.toFileString(activeFile, consTable, labTable));
					out.flush();
					out.close();
				}
				catch(IOException e)
				{
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
		if (propList.getNumProperties() != propList.getNumValidProperties())
		{
			message("Cannot save properties list: some properties are invalid");
			return CANCEL;
		}
		if (!consTable.isValid())
		{
			message("Cannot save properties list: some constants are invalid");
			return CANCEL;
		}
		if (showSaveFileDialog(propsFilter, propsFilter[0]) == JFileChooser.APPROVE_OPTION)
		{
			File file = getChooserFile();
			// do save
			try
			{
				setTaskBarText("Saving properties...");
				PrintWriter out = new PrintWriter(new FileWriter(file));
				out.print(propList.toFileString(file, consTable, labTable));
				out.flush();
				out.close();
			}
			catch(IOException e)
			{
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
		if (showOpenFileDialog(propsFilter, propsFilter[0]) == JFileChooser.APPROVE_OPTION)
		{
			File file = getChooserFile();
			if (file == null)
			{
				error("No file selected");
				return;
			}
			Thread t = new LoadPropertiesThread(this, parsedModel, file, true);
			t.setPriority(Thread.NORM_PRIORITY);
			t.start();
		}
		else
		{
			return;
		}
	}
	
	public void a_verifyAll()
	{
		propList.selectAll();
		a_verifySelected();
	}
	
	public void a_simulateSelected()
	{
		consTable.correctEditors();
		labTable.correctEditors();
		// Bail out if there are no valid properties to simulate
		// (probably never occurs- action is disabled in this case)
		ArrayList guiPropsTmp = propList.getValidSimulatableSelectedProperties();
		if(guiPropsTmp.size() == 0)
		{
			error("None of the selected properties are suitable for simulation");
			return;
		}
		// Request a parse
		simulateAfterReceiveParseNotification = true;
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.REQUEST_MODEL_PARSE));
	}
	
	
	public void a_detailSelected()
	{
		int[] selected = propList.getSelectedIndices();
		
		for(int i = 0; i < selected.length; i++)
		{
			GUIProperty gp = propList.getProperty(selected[i]);
			if(!gp.isBeingEdited())
			{
				gp.setBeingEdited(true);
				// Force repaint because we modified the GUIProperty directly
				repaintList();
				new GUIPropertyResultDialog(getGUI(), this, gp).show();
			}
		}
	}
	
	public void a_verifySelected()
	{
		consTable.correctEditors();
		labTable.correctEditors();
		// Bail out if there are no valid properties to verify
		// (probably never occurs- action is disabled in this case)
		ArrayList guiPropsTmp = propList.getValidSelectedProperties();
		if(guiPropsTmp.size() == 0)
		{
			error("None of the selected properties are suitable for verification. The model was not built");
			return;
		}
		// Request a parse
		verifyAfterReceiveParseNotification = true;
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.REQUEST_MODEL_PARSE));
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
		
		if(contents !=null)
		{
			if (contents.isDataFlavorSupported(getGUIClipboardPropertiesDataFlavor()))
			{
				try
				{
					GUIClipboardProperties gcp = (GUIClipboardProperties)contents.getTransferData(getGUIClipboardPropertiesDataFlavor());
					ArrayList listOfProperties = gcp.getProperties();
					for (int i = 0; i < listOfProperties.size(); i++)
					{
						GUIProperty property = (GUIProperty) listOfProperties.get(i);
						propList.addProperty(property.getPropString(), property.getComment());
						setModified(true);
					}
				}
				catch(UnsupportedFlavorException e) {}
				catch(IOException e) {}
			}
			else
			{
				try
				{
					String text = (String)contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
					propList.pastePropertiesString(text);
					setModified(true);
				}
				catch(UnsupportedFlavorException e) {}
				catch(IOException e) {}
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
		
		for(int i = 0; i < selected.length; i++)
		{
			GUIProperty gp = propList.getProperty(selected[i]);
			if(!gp.isBeingEdited())
			{
				gp.setBeingEdited(true);
				// Force repaint because we modified the GUIProperty directly
				repaintList();
				GUIPropertyEditor ed = new GUIPropertyEditor(this, parsedModel, gp, getInvalidPropertyStrategy());
				ed.show();
			}
		}
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
		while(consTable.getSelectedRowCount() > 0)
		{
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
		while(labTable.getSelectedRowCount() > 0)
		{
			labTable.removeLabel(labTable.getSelectedRow());
		}
	}
	
	public void a_newExperiment()
	{
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
		
		// get experiment
		if (experiments.getSelectedRowCount() != 1) return;
		exp = experiments.getExperiment(experiments.getSelectedRow());
		new GUIResultsTable(this.getGUI(), this, exp).show();
	}
	
	public void a_plotResults()
	{
		GUIExperiment exp;
		int t;
		
		// get experiment
		if (experiments.getSelectedRowCount() != 1) return;
		exp = experiments.getExperiment(experiments.getSelectedRow());
		// check its type
		try
		{
			t = exp.getPropertyType();
		}
		catch (PrismException e)
		{
			t = 0;
		}
		if (!(t == Expression.INT || t == Expression.DOUBLE))
		{
			message("Can only plot results if the property is of type int or double");
			return;
		}
		// make sure we can actually create a graph, i.e. that there is >1 result
		if (exp.getRangingConstants().size() == 0)
		{
			message("Cannot create a graph since there is only a single result.");
			return;
		}
		// launch dialog, plot series
		GUIGraphPicker ggp = new GUIGraphPicker(getGUI(), this, exp.getResults(), graphHandler);
		ggp.pickNewSeries();
	}
	
	public void a_exportResults()
	{
		GUIExperiment exp;
		
		// get experiment
		if (experiments.getSelectedRowCount() != 1) return;
		exp = experiments.getExperiment(experiments.getSelectedRow());
		// get filename to save
		if (showSaveFileDialog(textFilter, textFilter[0]) == JFileChooser.APPROVE_OPTION)
		{
			File file = getChooserFile();
			Thread t = new ExportResultsThread(this, exp, file);
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
		if(e instanceof GUIModelEvent)
		{
			GUIModelEvent me = (GUIModelEvent)e;
			if(me.getID() == me.NEW_MODEL)
			{
				//New Model
				setBuiltModel(null);
				setParsedModel(null);
				//newList();
			}
			else if(me.getID() == GUIModelEvent.MODEL_BUILT)
			{
				setBuiltModel(me.getModel());
				if(me.getBuildValues() != null)lastBuildValues = me.getBuildValues();
				modifiedSinceBuild = false;
				if(verifyAfterReceiveBuildNotification)verifyAfterBuild();
			}
			else if(me.getID() == GUIModelEvent.MODEL_PARSED)
			{
				setParsedModel(me.getModulesFile());
				if(verifyAfterReceiveParseNotification)verifyAfterParse();
				if(experimentAfterReceiveParseNotification)experimentAfterParse();
				if(simulateAfterReceiveParseNotification)simulateAfterParse();
			}
			else if(me.getID() == GUIModelEvent.MODEL_PARSE_FAILED)
			{
				verifyAfterReceiveParseNotification = false;
				experimentAfterReceiveParseNotification = false;
				simulateAfterReceiveParseNotification = false;
			}
			else if(me.getID() == GUIModelEvent.MODEL_BUILD_FAILED)
			{
				verifyAfterReceiveBuildNotification = false;
			}
			else if(me.getID() == GUIModelEvent.MODIFIED_SINCE_SAVE)
			{
				//setBuiltModel(null);
				modifiedSinceBuild = true;
			}
			else if(me.getID() == GUIModelEvent.NEW_LOAD_NOT_RELOAD_MODEL)
			{
				if (getPrism().getSettings().getBoolean(PrismSettings.PROPERTIES_CLEAR_LIST_ON_LOAD))
				{
					a_newList();
				}
			}
		}
		else if(e instanceof GUIComputationEvent)
		{
			if(e.getID() == GUIComputationEvent.COMPUTATION_START)
			{
				setComputing(true);
			}
			else if(e.getID() == GUIComputationEvent.COMPUTATION_DONE)
			{
				setComputing(false);
			}
			else if(e.getID() == GUIComputationEvent.COMPUTATION_ERROR)
			{
				setComputing(false);
			}
		}
		else if(e instanceof GUIClipboardEvent)
		{
			GUIClipboardEvent ce = (GUIClipboardEvent)e;
					
			if(ce.getComponent() == this || ce.getComponent() == propList)
			{
				if(!computing)
				{
					if(ce.getID() == ce.CUT)
					{a_cut();}
					else if(ce.getID() == ce.COPY)
					{a_copy();}
					else if(ce.getID() == ce.PASTE)
					{a_paste();}
					else if(ce.getID() == ce.DELETE)
					{a_delete();}
					else if(ce.getID() == ce.SELECT_ALL)
					{a_selectAll();}
				}
			}
			else
			{
			}
		}
		else if(e instanceof GUIPropertiesEvent)
		{
			GUIPropertiesEvent pr = (GUIPropertiesEvent)e;
			if(pr.getID() == GUIPropertiesEvent.EXPERIMENT_START)
			{
				stopExperiment.setEnabled(true);
			}
			else if(pr.getID() == GUIPropertiesEvent.EXPERIMENT_END)
			{
				stopExperiment.setEnabled(false);
			}
		}
		else if (e instanceof GUIExitEvent)
		{
			if(e.getID() == GUIExitEvent.REQUEST_EXIT)
			{
				if (doModificationCheck() != CONTINUE)
				{
					notifyEventListeners(new GUIExitEvent(GUIExitEvent.CANCEL_EXIT));
					return true;
				}
			}
		}
		return false;
	}
	
	//METHODS TO IMPLEMENT MouseListner INTERFACE
	
	public void mouseClicked(MouseEvent e)
	{
		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);
		if(!computing)
		{
			if(e.getClickCount() >= 2)
			{
				if(e.getSource() == propList)
				{
					int index = propList.locationToIndex(e.getPoint());
					
					if(index != -1)
					{
						propList.setSelectedIndex(index);
					}
					a_editProperty();
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
		if(!computing)
		{			
			if(e.isPopupTrigger() && e.getSource() == propList)
			{
				
				
				int index = propList.locationToIndex(e.getPoint());
				// if there are no properties selected, select the one under the popup
				if(propList.isSelectionEmpty())
				{
					if(index != -1)
					{
						propList.setSelectedIndex(index);
					}
				}
				else
				{
					// if the property under the popup is not already selected, select just that one
					int[] sel = propList.getSelectedIndices();
					boolean valid = false;
					for(int i = 0; i < sel.length; i++)
					{
						if(sel[i] == index)
						{
							valid = true;
							break;
						}
					}
					if(!valid)
					{
						propList.setSelectedIndex(index);
					}
				}
				// disable certain actions if any of the selected propeties are currently being edited
				int[] sel = propList.getSelectedIndices();
				boolean showDeleters = true;
				for(int i = 0; i < sel.length; i++)
				{
					if(propList.getProperty(sel[i]).isBeingEdited())
					{
						showDeleters = false;
						break;
					}
				}
				
				
				
				cutAction.setEnabled(true);
				deleteAction.setEnabled(true);
				verifySelected.setEnabled(propList.getValidSelectedProperties().size() > 0);
				simulate.setEnabled(propList.getValidSimulatableSelectedProperties().size() > 0);
				details.setEnabled(propList.getValidSelectedProperties().size() > 0);
				editProperty.setEnabled(propList.getSelectedProperties().size() > 0);
				
				newExperiment.setEnabled(propList.getNumSelectedProperties() == 1 && propList.getValidSelectedProperties().size() == 1);
				
				if(showDeleters == false)
				{
					cutAction.setEnabled(false);
					deleteAction.setEnabled(false);
					simulate.setEnabled(false);
					verifySelected.setEnabled(false);
					details.setEnabled(false);
					editProperty.setEnabled(false);
					newExperiment.setEnabled(false);
				}
				
				propertiesPopup.show(e.getComponent(), e.getX(), e.getY());
			}
			else if(e.isPopupTrigger() && (e.getSource() == consTable || e.getSource() == constantsScroll))
			{				
				constantsPopup.show(e.getComponent(), e.getX(), e.getY());				
			}
			else if(e.isPopupTrigger() && ( e.getSource() == labTable || e.getSource() == labelsScroll))
			{				
				labelsPopup.show(e.getComponent(), e.getX(), e.getY());
			}
			else if(e.isPopupTrigger() && ( e.getSource() == experiments || e.getSource() == expScroller))
			{				
				doEnables();
				this.experimentPopup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
		
		
	}
	
	public void mouseReleased(MouseEvent e)
	{
		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);
		if(!computing)
		{
			if(e.isPopupTrigger() && e.getSource() == propList)
			{
				int index = propList.locationToIndex(e.getPoint());
				// if there are no properties selected, select the one under the popup
				if(propList.isSelectionEmpty())
				{
					if(index != -1)
					{
						propList.setSelectedIndex(index);
					}
				}
				else
				{
					// if the property under the popup is not already selected, select just that one
					int[] sel = propList.getSelectedIndices();
					boolean valid = false;
					for(int i = 0; i < sel.length; i++)
					{
						if(sel[i] == index)
						{
							valid = true;
							break;
						}
					}
					if(!valid)
					{
						propList.setSelectedIndex(index);
					}
				}
				// disable certain actions if any of the selected propeties are currently being edited
				int[] sel = propList.getSelectedIndices();
				boolean showDeleters = true;
				for(int i = 0; i < sel.length; i++)
				{
					if(propList.getProperty(sel[i]).isBeingEdited())
					{
						showDeleters = false;
						break;
					}
				}
				cutAction.setEnabled(true);
				deleteAction.setEnabled(true);
				verifySelected.setEnabled(propList.getValidSelectedProperties().size() > 0);
				simulate.setEnabled(propList.getValidSimulatableSelectedProperties().size() > 0);
				details.setEnabled(propList.getValidSelectedProperties().size() > 0);
				editProperty.setEnabled(propList.getSelectedProperties().size() > 0);
				newExperiment.setEnabled(propList.getNumSelectedProperties() == 1 && propList.getValidSelectedProperties().size() == 1);
							
				if(showDeleters == false)
				{
					cutAction.setEnabled(false);
					deleteAction.setEnabled(false);
					simulate.setEnabled(false);
					verifySelected.setEnabled(false);
					details.setEnabled(false);
					editProperty.setEnabled(false);
					newExperiment.setEnabled(false);
				}
				
				propertiesPopup.show(e.getComponent(), e.getX(), e.getY());
			}
			else if(e.isPopupTrigger() && (e.getSource() == consTable || e.getSource() == constantsScroll))
			{
				constantsPopup.show(e.getComponent(), e.getX(), e.getY());
				
			}
			else if(e.isPopupTrigger() && ( e.getSource() == labTable || e.getSource() == labelsScroll))
			{
				labelsPopup.show(e.getComponent(), e.getX(), e.getY());
			}
			else if(e.isPopupTrigger() && ( e.getSource() == experiments || e.getSource() == expScroller))
			{
				doEnables();
				this.experimentPopup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	
	//METHODS TO IMPLEMENT ListSelectionListener INTERFACE
	public void valueChanged(ListSelectionEvent e)
	{
		ArrayList selectedProps = propList.getSelectedProperties();
		
		// disable certain actions if any of the selected propeties are currently being edited
		boolean showDeleters = true;
		for(int i = 0; i < selectedProps.size(); i++)
		{
			if(((GUIProperty)selectedProps.get(i)).isBeingEdited())
			{
				showDeleters = false;
				break;
			}
		}
		cutAction.setEnabled(true);
		deleteAction.setEnabled(true);
		verifySelected.setEnabled(propList.getValidSelectedProperties().size() > 0);
		simulate.setEnabled(propList.getValidSimulatableSelectedProperties().size() > 0);
		details.setEnabled(propList.getValidSelectedProperties().size() > 0);
		editProperty.setEnabled(propList.getSelectedProperties().size() > 0);
		if(showDeleters == false)
		{
			cutAction.setEnabled(false);
			deleteAction.setEnabled(false);
			simulate.setEnabled(false);
			verifySelected.setEnabled(false);
			details.setEnabled(false);
			editProperty.setEnabled(false);
		}
		
		updateCommentLabel();
		
		//Now do the one for the constants table
		
		removeConstant.setEnabled(consTable.getSelectedRowCount() > 0);
		removeLabel.setEnabled(labTable.getSelectedRowCount() > 0);
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
				left.setMinimumSize(new java.awt.Dimension(10,10));
				JSplitPane leftSc = new JSplitPane();
				{
					JPanel topLeft = new JPanel();
					{
						JScrollPane propScroll = new JScrollPane();
						{
							propList = new GUIPropertiesList(getPrism(), this);
							propList.addListSelectionListener(this);
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
						{
							consTable = new GUIPropConstantList(this);
							consTable.setBackground(Color.white);
							consTable.addMouseListener(this);
							constantsScroll.setViewportView(consTable);
							constantsScroll.addMouseListener(this);
							constantsScroll.setBorder(new TitledBorder("Constants"));
						}
						labelsScroll = new JScrollPane();
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
					JPanel bottomRight = new JPanel(new GridLayout(1,1));
					{
						graphHandler = new GUIGraphHandler(this.getGUI(), this);
						bottomRight.add(graphHandler);
						
						bottomRight.setPreferredSize(new Dimension(300,300));
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
			fileLabel = new JLabel();
			{
				fileLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
				fileLabel.setBorder(new javax.swing.border.EtchedBorder());
				fileLabel.setMinimumSize(new java.awt.Dimension(40, 25));
			}
			
			//progress = new JProgressBar(0, 100);
			topPanel.setLayout(new BorderLayout());
			//topPanel.add(progress, BorderLayout.WEST);
			topPanel.add(fileLabel,BorderLayout.CENTER);
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
			//JMenu ver = new JMenu("Model checking");
			//ver.setMnemonic('M');
			//ver.setIcon(GUIPrism.getIconFromImage("smallVerify.gif"));
			propMenu.add(verifySelected);
			propMenu.add(simulate);
			propMenu.add(newExperiment);
			//ver.add(verifyAll);
			//propMenu.add(ver);
			propMenu.setMnemonic('P');
		}
		createPopups();
		//file filters
		propsFilter = new GUIPrismFileFilter[1];
		propsFilter[0] = new GUIPrismFileFilter("PRISM properties (*.pctl, *.csl)");
		propsFilter[0].addExtension("pctl");
		propsFilter[0].addExtension("csl");
		textFilter = new GUIPrismFileFilter[1];
		textFilter[0] = new GUIPrismFileFilter("Plain text files (*.txt)");
		textFilter[0].addExtension("txt");
	}
	
	private void createPopups()
	{
		propertiesPopup = new JPopupMenu();
		
		propertiesPopup.add(editProperty);		
		propertiesPopup.add(newProperty);
		propertiesPopup.add(new JSeparator());
		propertiesPopup.add(verifySelected);
		//propertiesPopup.add(verifyAll);
		propertiesPopup.add(simulate);
		//experiment
		propertiesPopup.add(newExperiment);
		propertiesPopup.add(details);
		propertiesPopup.add(new JSeparator());
		propertiesPopup.add(cutAction);
		propertiesPopup.add(copyAction);
		propertiesPopup.add(pasteAction);
		propertiesPopup.add(deleteAction);
		propertiesPopup.add(new JSeparator());
		propertiesPopup.add(selectAllAction);
		
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
		experimentPopup.add(exportResults);
		
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
		newProps.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallNew.gif"));
		newProps.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		
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
		openProps.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallOpen.gif"));
		openProps.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		
		saveProps = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_save();
			}
		};
		saveProps.putValue(Action.LONG_DESCRIPTION, "Saves all properties to the current active properties list, if non exists the user is prompted with a dialog.");
		// saveProps.putValue(Action.SHORT_DESCRIPTION, "Save properties list");
		saveProps.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		saveProps.putValue(Action.NAME, "Save properties list");
		saveProps.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSave.gif"));
		saveProps.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		
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
		savePropsAs.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSaveAs.gif"));
		
		insertProps = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_insert();
			}
		};
		insertProps.putValue(Action.LONG_DESCRIPTION, "Inserts properties from user selected file into the properties list.  The active property file remains the same however.");
		//insertProps.putValue(Action.SHORT_DESCRIPTION, "Insert properties list");
		insertProps.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
		insertProps.putValue(Action.NAME, "Insert properties list...");
		insertProps.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallAdd.gif"));
		
		verifyAll = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_verifyAll();
			}
		};
		verifyAll.putValue(Action.LONG_DESCRIPTION, "Selects all listed properties, and model checks them against the model that is built.  If there is no built model, the parsed model is automatically built.  If the parsed model has changed since the last build, the user is prompted as to whether they wish to re-build the model.  If the model text has been modified since the last build, the user is asked whether they want to re-parse and re-build.");
		//verifyAll.putValue(Action.SHORT_DESCRIPTION, "Verify All Properties");
		verifyAll.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
		verifyAll.putValue(Action.NAME, "Verify all");
		verifyAll.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallVerifyAll.gif"));
		//verifyAll.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
		
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
		simulate.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSimulate.gif"));
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
		details.putValue(Action.NAME, "Show Details");
		details.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDetails.gif"));
		
		verifySelected = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_verifySelected();
			}
		};
		verifySelected.putValue(Action.LONG_DESCRIPTION, "Model checks the selected properties against the model that is built.  If there is no built model, the parsed model is automatically built.  If the parsed model has changed since the last build, the user is prompted as to whether they wish to re-build the model.  If the model text has been modified since the last build, the user is asked whether they want to re-parse and re-build.");
		//verifySelected.putValue(Action.SHORT_DESCRIPTION, "Verify Selected Properties");
		verifySelected.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_V));
		verifySelected.putValue(Action.NAME, "Verify");
		verifySelected.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallVerify.gif"));
		verifySelected.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		
		cutAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_cut();
			}
		};
		cutAction.putValue(Action.LONG_DESCRIPTION, "Copys the selected properties to the clipboard and then deletes them.");
		//cutAction.putValue(Action.SHORT_DESCRIPTION, "Cut");
		cutAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
		cutAction.putValue(Action.NAME, "Cut");
		cutAction.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCut.gif"));
		//cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		
		copyAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_copy();
			}
		};
		copyAction.putValue(Action.LONG_DESCRIPTION, "Copys the selected properties to the clipboard.");
		copyAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
		copyAction.putValue(Action.NAME, "Copy");
		copyAction.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCopy.gif"));
		
		pasteAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_paste();
			}
		};
		pasteAction.putValue(Action.LONG_DESCRIPTION, "Pastes the properties on the clipboard to the properties list");
		pasteAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		pasteAction.putValue(Action.NAME, "Paste");
		pasteAction.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPaste.gif"));
		
		deleteAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_delete();
			}
		};
		deleteAction.putValue(Action.LONG_DESCRIPTION, "Deletes the selected properties.");
		deleteAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		deleteAction.putValue(Action.NAME, "Delete");
		deleteAction.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.gif"));
		
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
		newProperty.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallAdd.gif"));
		
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
		editProperty.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallEdit.gif"));
		
		selectAllAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_selectAll();
			}
		};
		selectAllAction.putValue(Action.LONG_DESCRIPTION, "Selects all properties in the properties list");
		selectAllAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
		selectAllAction.putValue(Action.NAME, "Select all");
		selectAllAction.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSelectAll.gif"));
		
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
		newConstant.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallAdd.gif"));
		
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
		removeConstant.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.gif"));
		
		
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
		newLabel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallAdd.gif"));
		
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
		removeLabel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.gif"));
		
		
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
		newExperiment.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallNew.gif"));
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
		deleteExperiment.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.gif"));
		
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
		viewResults.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallView.gif"));
		
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
		plotResults.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallLineGraph.gif"));
		
		exportResults = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportResults();
			}
		};
		exportResults.putValue(Action.LONG_DESCRIPTION, "Export the results of this experiment to a file");
		exportResults.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
		exportResults.putValue(Action.NAME, "Export results");
		exportResults.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.gif"));
		
		stopExperiment = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_stopExperiment();
			}
		};
		stopExperiment.putValue(Action.LONG_DESCRIPTION, "Stops the Experiment that is currently running");
		stopExperiment.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallHalt.gif"));
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
		private ArrayList listOfProperties;
		private StringSelection stringRepresentation;
		
		public GUIClipboardProperties(ArrayList listOfProperties)
		{
			this.listOfProperties = listOfProperties;
			String tmpString = "";
			for(int i = 0 ; i < listOfProperties.size(); i++)
			{
				GUIProperty gp = (GUIProperty)listOfProperties.get(i);
				if (gp.getComment().trim().length() > 0)
				{					
					tmpString += "//" + gp.getComment() + "\n";
				}
				tmpString += gp.getPropString();
				if(i != listOfProperties.size() -1 ) tmpString += "\n";
			}	
			
			stringRepresentation = new StringSelection(tmpString);
		}
		
		
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
		{
			if (flavor.getRepresentationClass() == this.getClass())
			{	return this;	}
			else
			{	return this.stringRepresentation.getTransferData(flavor);	}
		}
	    
		public DataFlavor[] getTransferDataFlavors()
		{	
			DataFlavor[] stringFlavors = stringRepresentation.getTransferDataFlavors();
					
			DataFlavor[] allFlavors = new DataFlavor[stringFlavors.length + 1];
			allFlavors[0] = GUIMultiProperties.getGUIClipboardPropertiesDataFlavor();
			
			for (int i = 0; i < stringFlavors.length; i++)
			{	allFlavors[i+1] = stringFlavors[i];	}
			
			return allFlavors;
		}
		
		public boolean	isDataFlavorSupported(DataFlavor flavor)
		{
			return (stringRepresentation.isDataFlavorSupported(flavor) || flavor.equals(GUIMultiProperties.getGUIClipboardPropertiesDataFlavor()));	
		}		
		
		public ArrayList getProperties()
		{
			return listOfProperties;
		}
		
	}
}
