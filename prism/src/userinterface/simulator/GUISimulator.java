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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import simulator.*;
import simulator.networking.*;
import parser.*;
import parser.ast.*;
import parser.type.*;
import prism.*;
import userinterface.*;
import userinterface.util.*;
import userinterface.model.*;
import userinterface.properties.*;
import userinterface.simulator.networking.*;

public class GUISimulator extends GUIPlugin implements MouseListener, ListSelectionListener, PrismSettingsListener
{
	//ATTRIBUTES
	private GUIPrism gui; //reference to the gui
	private GUIMultiProperties guiProp; //reference to the properties information
	private GUIMultiModel guiMultiModel; //reference to the model plugin
	private SimulatorEngine engine;
	private GUIPrismFileFilter[] txtFilter;
	private JMenu simulatorMenu;
	private JPopupMenu pathPopupMenu;

	//Current State
	private ModulesFile parsedModel;
	private boolean pathActive;

	private PathTableModel pathTableModel;
	private UpdateTableModel updateTableModel;

	private Values lastConstants, lastPropertyConstants, lastInitialState;
	private boolean computing;

	private boolean displayStyleFast;
	private boolean displayPathLoops;
	private SimulationView view;

	//Actions
	private Action randomExploration, backtrack, backtrackToHere, removeToHere, newPath, resetPath, exportPath, configureView;

	/** Creates a new instance of GUISimulator */
	public GUISimulator(GUIPrism gui)
	{
		super(gui, true);
		this.gui = gui;
		this.engine = gui.getPrism().getSimulator();

		view = new SimulationView();
		pathTableModel = new PathTableModel(view);

		updateTableModel = new UpdateTableModel();

		initComponents();
		initPopups();

		doEnables();

		horizontalSplit.setDividerLocation((int) leftExplorePanel.getPreferredSize().getHeight() + 11);

		randomExplorationButton.setIcon(GUIPrism.getIconFromImage("smallPlayerFwd.png"));
		backtrackButton.setIcon(GUIPrism.getIconFromImage("smallPlayerRew.png"));

		pathTable.getSelectionModel().addListSelectionListener(this);

		pathTable.addMouseListener(this);
		pathTable.getTableHeader().addMouseListener(this);
		tableScroll.addMouseListener(this);

		pathTable.getTableHeader().setReorderingAllowed(true);

		pathTable.addComponentListener(new ComponentAdapter()
		{
			public void componentResized(ComponentEvent e)
			{
				sortOutColumnSizes();
			}
		});

		pathTablePlaceHolder.addMouseListener(this);

		view.refreshToDefaultView();

		pathActive = false;
		doEnables();

		//verifyAllPropertiesAtOnce = true;
		//options = new GUISimulatorOptions(this);

		currentUpdatesTable.setModel(updateTableModel);
		currentUpdatesTable.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2 && currentUpdatesTable.isEnabled()) {
					a_manualUpdate();
					currentUpdatesTable.requestFocus();
				} else if (e.getClickCount() == 2 && !currentUpdatesTable.isEnabled()) {
					GUISimulator.this.warning("Simulation",
							"You cannot continue exploration from the state that is current selected.\nSelect the last state in the path table to continue");
				}
			}
		});

		currentUpdatesTable.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER && currentUpdatesTable.isEnabled()) {
					a_manualUpdate();
					currentUpdatesTable.requestFocus();
				}

			}
		});

		pathTable.setModel(pathTableModel);

		lastConstants = null;

		lastPropertyConstants = null;

		lastInitialState = null;

		tableScroll.setRowHeaderView(((GUISimulatorPathTable) pathTable).getPathLoopIndicator());
		manualUpdateTableScrollPane.setRowHeaderView(((GUISimulatorUpdatesTable) currentUpdatesTable).getUpdateRowHeader());

		tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		stateLabelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		stateLabelList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				pathTable.repaint();
			}
		});

		modelTypeLabel.setText("Unknown");
		totalTimeLabel.setText(formatDouble(0.0));
		pathLengthLabel.setText("0");

		txtFilter = new GUIPrismFileFilter[1];
		txtFilter[0] = new GUIPrismFileFilter("Text files (*.txt)");
		txtFilter[0].addExtension("txt");

		displayStyleFast = true;
		displayPathLoops = true;

		GUINetworkEditor netEdit = new GUINetworkEditor(getGUI(), new SimulatorNetworkHandler());

		getPrism().getSettings().setFileSelector(PrismSettings.SIMULATOR_NETWORK_FILE, netEdit);

		autoTimeCheck.setSelected(true);
		currentUpdatesTable.requestFocus();

		manualUpdateTableScrollPane.setToolTipText("Double-click or right-click below to create a new path");
	}

	public void setGUIMultiModel(GUIMultiModel guiMultiModel)
	{
		this.guiMultiModel = guiMultiModel;
	}

	public void setGUIProb(GUIMultiProperties guiProp)
	{
		this.guiProp = guiProp;
	}

	public String getTotalRewardLabelString()
	{
		int i, n;
		String s;
		n = parsedModel.getNumRewardStructs();
		s = "<html>";
		for (i = 0; i < n; i++) {
			s += engine.getTotalCumulativeRewardForPath(i);
			if (i < n - 1)
				s += ",<br>";
		}
		s += "</html>";
		return s;
	}

	public void a_clearModel()
	{
		tableScroll.setViewportView(pathTablePlaceHolder);

		pathActive = false;
		pathTableModel.restartPathTable();

		((GUISimLabelList) stateLabelList).clearLabels();
		((GUISimPathFormulaeList) pathFormulaeList).clearList();
	}

	public void a_loadModulesFile(ModulesFile mf)
	{
		this.parsedModel = mf;
		pathActive = false;

		pathTableModel.restartPathTable();
		updateTableModel.restartUpdatesTable();

		if (mf != null)
			modelTypeLabel.setText("" + mf.getTypeString());
		else
			modelTypeLabel.setText("Unknown");

		((GUISimLabelList) stateLabelList).clearLabels();
		((GUISimPathFormulaeList) pathFormulaeList).clearList();
		stateLabelList.repaint();
		pathFormulaeList.repaint();
		doEnables();

		typeExploreCombo.removeAllItems();
		typeExploreCombo.addItem("Steps");
		typeExploreCombo.addItem("Up to step");

		if (mf != null && mf.getModelType() == ModelType.CTMC) {
			typeExploreCombo.addItem("Time");
			typeExploreCombo.addItem("Up to time");
		}

		typeBacktrackCombo.setEnabled(pathActive);
		typeBacktrackCombo.removeAllItems();

		typeBacktrackCombo.addItem("Steps");
		typeBacktrackCombo.addItem("Back to step");

		if (mf != null && mf.getModelType() == ModelType.CTMC) {
			typeBacktrackCombo.addItem("Time");
			typeBacktrackCombo.addItem("Back to time");
		}
	}

	public void a_newPath()
	{
		Values initialState;
		try {
			// get properties constants/labels
			PropertiesFile pf;
			try {
				pf = getPrism().parsePropertiesString(parsedModel, guiProp.getConstantsString().toString() + guiProp.getLabelsString());
			} catch (PrismLangException e) {
				// ignore properties if they don't parse
				pf = null; //if any problems
			}

			// if necessary, get values for undefined constants from user
			// TODO: only get necessary property constants (pf can decide this)
			UndefinedConstants uCon = new UndefinedConstants(parsedModel, pf);
			if (uCon.getMFNumUndefined() + uCon.getPFNumUndefined() > 0) {
				int result = GUIConstantsPicker.defineConstantsWithDialog(gui, uCon, lastConstants, lastPropertyConstants);
				if (result != GUIConstantsPicker.VALUES_DONE)
					return;
			}
			// remember constant values for next time
			lastConstants = uCon.getMFConstantValues();
			lastPropertyConstants = uCon.getPFConstantValues();
			// store constants
			parsedModel.setUndefinedConstants(lastConstants);
			pf.setUndefinedConstants(lastPropertyConstants);

			// now determine the initial state for simulation

			// first select a default state (the initial state) which may or may not end up being used
			Values defaultInitialState = new Values();
			defaultInitialState.addValues(parsedModel.getInitialValues());

			boolean modelChanged = false;

			// we will pass in lastInitialState to the dialog
			// but first make sure it is ok, i.e.
			// (a) it is non-null
			if (lastInitialState == null) {
				lastInitialState = defaultInitialState;
				modelChanged = true;
			}
			// (b) var names/types are correct
			else {
				boolean match = true;
				int i, n;
				n = defaultInitialState.getNumValues();
				if (lastInitialState.getNumValues() != defaultInitialState.getNumValues()) {
					match = false;
				} else {
					for (i = 0; i < n; i++) {
						if (!lastInitialState.contains(defaultInitialState.getName(i))) {
							match = false;
							break;
						} else {
							int index = lastInitialState.getIndexOf(defaultInitialState.getName(i));
							if (lastInitialState.getType(index) != defaultInitialState.getType(i)) {
								match = false;
								break;
							}
						}
					}
				}
				if (!match) // if there's a problem, just use the default
				{
					lastInitialState = defaultInitialState;
					modelChanged = true;
				}
			}

			// if required, we prompt the user for an initial state
			if (isAskForInitialState()) {
				initialState = GUIInitialStatePicker.defineInitalValuesWithDialog(getGUI(), lastInitialState, parsedModel);
				// if user clicked cancel from dialog...

				if (initialState == null) {
					return;
				}
			} else {
				initialState = lastInitialState;
			}

			tableScroll.setViewportView(pathTable);

			displayPathLoops = true;

			// Create a new path in the simulator and add labels/properties 
			engine.createNewPath(parsedModel);
			pathActive = true;
			repopulateFormulae(pf);
			engine.initialisePath(new State(initialState));

			totalTimeLabel.setText(formatDouble(engine.getTotalTimeForPath()));
			pathLengthLabel.setText("" + engine.getPathSize());
			definedConstantsLabel.setText((uCon.getDefinedConstantsString().length() == 0) ? "None" : uCon.getDefinedConstantsString());

			doEnables();

			pathTableModel.restartPathTable();

			updateTableModel.restartUpdatesTable();

			pathTable.getSelectionModel().setSelectionInterval(pathTable.getRowCount() - 1, pathTable.getRowCount() - 1);

			stateLabelList.repaint();
			pathFormulaeList.repaint();

			// store inital state for next time
			lastInitialState = initialState;

			if (getPrism().getSettings().getBoolean(PrismSettings.SIMULATOR_NEW_PATH_ASK_VIEW)) {
				new GUIViewDialog(gui, pathTableModel.getView());
			}

		} catch (PrismException e) {
			this.error(e.getMessage());
			if (e instanceof PrismLangException) {
				guiMultiModel.getHandler().modelParseFailed((PrismLangException) e, false);
				guiMultiModel.tabToFront();
			}
		}
	}

	/** Explore a number of steps. */
	public void a_autoStep(int noSteps)
	{
		try {
			if (displayPathLoops && pathTableModel.isPathLooping()) {
				if (questionYesNo("The current path contains a deterministic loop. \nDo you wish to disable detection of such loops and extend the path anyway?") == 0) {
					displayPathLoops = false;
					pathTable.repaint();
				} else
					return;
			}

			setComputing(true);

			if (isOldUpdate()) {
				engine.computeTransitionsForCurrentState();
			}

			int noStepsTaken = engine.automaticTransitions(noSteps, displayPathLoops);

			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();
			pathTable.scrollRectToVisible(new Rectangle(0, (int) pathTable.getPreferredSize().getHeight() - 10, (int) pathTable.getPreferredSize().getWidth(),
					(int) pathTable.getPreferredSize().getHeight()));

			totalTimeLabel.setText(formatDouble(engine.getTotalTimeForPath()));
			pathLengthLabel.setText("" + engine.getPathSize());

			stateLabelList.repaint();
			pathFormulaeList.repaint();
			setComputing(false);

			//if (displayPathLoops && pathTableModel.isPathLooping() && (noStepsTaken < noSteps) {
			//	message("Exploration has stopped early because a deterministic loop has been detected.");
			//}
		} catch (PrismException e) {
			this.error(e.getMessage());
			guiMultiModel.getHandler().modelParseFailed((PrismLangException) e, false);
			guiMultiModel.tabToFront();
		}
	}

	/** Explore an amount of time. */
	public void a_autoStep(double time)
	{
		try {
			double oldPathTime = engine.getTotalTimeForPath();

			if (displayPathLoops && pathTableModel.isPathLooping()) {
				if (questionYesNo("The current path contains a deterministic loop. \nDo you wish to disable detection of such loops and extend the path anyway?") == 0) {
					displayPathLoops = false;
					pathTable.repaint();
				} else
					return;
			}

			setComputing(true);

			if (isOldUpdate()) {
				engine.computeTransitionsForCurrentState();
			}

			engine.automaticTransitions(time, displayPathLoops);

			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();
			pathTable.scrollRectToVisible(new Rectangle(0, (int) pathTable.getPreferredSize().getHeight() - 10, (int) pathTable.getPreferredSize().getWidth(),
					(int) pathTable.getPreferredSize().getHeight()));

			totalTimeLabel.setText(formatDouble(engine.getTotalTimeForPath()));
			pathLengthLabel.setText("" + engine.getPathSize());

			stateLabelList.repaint();
			pathFormulaeList.repaint();
			setComputing(false);

			//if (displayPathLoops && pathTableModel.isPathLooping() && (engine.getTotalTimeForPath() - oldPathTime) < time) {
			//	message("Exploration has stopped early because a deterministic loop has been detected.");
			//}
		} catch (PrismException e) {
			this.error(e.getMessage());
		}
	}

	/** Backtracks to a certain time. */
	public void a_backTrack(double time) throws PrismException
	{
		try {
			setComputing(true);

			engine.backtrackTo(time);

			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();

			totalTimeLabel.setText(formatDouble(engine.getTotalTimeForPath()));
			pathLengthLabel.setText("" + engine.getPathSize());
			stateLabelList.repaint();
			pathFormulaeList.repaint();

			setComputing(false);
		} catch (PrismException e) {
			this.error(e.getMessage());
		}
	}

	/** Backtracks to a certain step. */
	public void a_backTrack(int step)
	{
		try {
			setComputing(true);

			engine.backtrackTo(step);

			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();

			totalTimeLabel.setText(formatDouble(engine.getTotalTimeForPath()));
			pathLengthLabel.setText("" + engine.getPathSize());
			stateLabelList.repaint();
			pathFormulaeList.repaint();

			setComputing(false);
		} catch (PrismException e) {
			this.error(e.getMessage());
		}
	}

	public void a_restartPath()
	{
		try {
			setComputing(true);
			// Instruct simulator to go back to step 0
			engine.backtrackTo(0);

			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();

			totalTimeLabel.setText(formatDouble(engine.getTotalTimeForPath()));
			pathLengthLabel.setText("" + engine.getPathSize());
			stateLabelList.repaint();
			pathFormulaeList.repaint();
			setComputing(false);
		} catch (PrismException e) {
			this.error(e.getMessage());
		}
	}

	public void a_exportPath()
	{
		try {
			if (showSaveFileDialog(txtFilter, txtFilter[0]) != JFileChooser.APPROVE_OPTION)
				return;
			setComputing(true);

			engine.exportPath(getChooserFile());

			setComputing(false);
		} catch (PrismException e) {
			error(e.getMessage());
		}
	}

	public void a_removePreceding(int step) throws PrismException
	{
		setComputing(true);

		engine.removePrecedingStates(step);

		pathTableModel.updatePathTable();
		updateTableModel.updateUpdatesTable();

		totalTimeLabel.setText(formatDouble(engine.getTotalTimeForPath()));
		pathLengthLabel.setText("" + engine.getPathSize());
		stateLabelList.repaint();
		pathFormulaeList.repaint();
		setComputing(false);
	}

	public void a_manualUpdate()
	{
		try {
			if (currentUpdatesTable.getSelectedRow() == -1)
				throw new PrismException("No current update is selected");

			if (displayPathLoops && pathTableModel.isPathLooping()) {
				if (questionYesNo("The current path contains a deterministic loop. \nDo you wish to disable detection of such loops and extend the path anyway?") == 0) {
					displayPathLoops = false;
					pathTable.repaint();
				} else
					return;
			}

			double time = -1;
			if (parsedModel.getModelType() == ModelType.CTMC) {
				if (!autoTimeCheck.isSelected()) {
					time = GUITimeDialog.askTime(this.getGUI(), this);
					if (time < 0.0d) // dialog cancelled
						return;
				}

				//Double.parseDouble(stateTimeField.getText());

				setComputing(true);
				if (time == -1) {
					engine.manualTransition(currentUpdatesTable.getSelectedRow());
				} else {
					engine.manualTransition(currentUpdatesTable.getSelectedRow(), time);
				}

				pathTableModel.updatePathTable();
				updateTableModel.updateUpdatesTable();

				pathTable.scrollRectToVisible(new Rectangle(0, pathTable.getHeight() - 10, pathTable.getWidth(), pathTable.getHeight()));

				totalTimeLabel.setText(formatDouble(engine.getTotalTimeForPath()));
				pathLengthLabel.setText("" + engine.getPathSize());

				setComputing(false);

			} else {

				setComputing(true);

				engine.manualTransition(currentUpdatesTable.getSelectedRow());

				pathTableModel.updatePathTable();
				updateTableModel.updateUpdatesTable();

				totalTimeLabel.setText("" + engine.getTotalTimeForPath());
				pathLengthLabel.setText("" + engine.getPathSize());

				pathTable.scrollRectToVisible(new Rectangle(0, (int) pathTable.getPreferredSize().getHeight() - 10, (int) pathTable.getPreferredSize()
						.getWidth(), (int) pathTable.getPreferredSize().getHeight()));

				setComputing(false);
			}
			stateLabelList.repaint();
			pathFormulaeList.repaint();
		} catch (NumberFormatException e) {
			this.error("The Auto update \'no. steps\' parameter is invalid.\nIt must be a positive integer representing a step in the path table");
			setComputing(false);
		} catch (PrismException e) {
			this.error(e.getMessage());
			setComputing(false);
		}
	}

	public void a_configureView()
	{
		new GUIViewDialog(gui, pathTableModel.getView());
	}

	/**
	 * Re-populate lists of labels and path formulas.
	 * Labels are taken from current model and passed in properties file. 
	 * Path formulas are taken from the passed in properties file. 
	 */
	private void repopulateFormulae(PropertiesFile propertiesFile)
	{
		// Labels
		GUISimLabelList theStateLabelList = (GUISimLabelList) stateLabelList;
		theStateLabelList.clearLabels();
		if (pathActive) {
			// Add the default labels: "init" and "deadlock"
			theStateLabelList.addDeadlockAndInit();
			if (parsedModel != null) {
				// Add labels from model
				LabelList labelList1 = parsedModel.getLabelList();
				for (int i = 0; i < labelList1.size(); i++) {
					theStateLabelList.addModelLabel(labelList1.getLabelName(i), labelList1.getLabel(i));
				}
				// Add labels from properties file
				LabelList labelList2 = propertiesFile.getLabelList();
				for (int i = 0; i < labelList2.size(); i++) {
					theStateLabelList.addPropertyLabel(labelList2.getLabelName(i), labelList2.getLabel(i), propertiesFile);
				}
			}
		}

		// TODO: fix and re-enable this (note: should use passed in properties file)
		// Path formulas
		GUISimPathFormulaeList thePathFormulaeList = (GUISimPathFormulaeList) pathFormulaeList;
		thePathFormulaeList.clearList();
		if (pathActive) {
			// Go through the property list from the Properties tab of GUI
			GUIPropertiesList gpl = guiProp.getPropList();
			for (int i = 0; i < gpl.getNumProperties(); i++) {
				GUIProperty gp = gpl.getProperty(i);
				// For properties which are simulate-able...
				if (gp.isValidForSimulation()) {
					// Add them to the list
					thePathFormulaeList.addProperty(gp.getProperty(), propertiesFile);
				}
			}
		}
	}

	//METHODS TO IMPLEMENT THE GUIPLUGIN INTERFACE

	public boolean displaysTab()
	{
		return true;
	}

	public javax.swing.JMenu getMenu()
	{
		return simulatorMenu;
	}

	public OptionsPanel getOptions()
	{
		return null;
	}

	public String getTabText()
	{
		return "Simulator";
	}

	public javax.swing.JToolBar getToolBar()
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

	private boolean ignoreNextParse;

	public void ignoreNextParse()
	{
		ignoreNextParse = true;
	}

	public boolean processGUIEvent(userinterface.util.GUIEvent e)
	{
		if (e instanceof GUIModelEvent) {
			GUIModelEvent me = (GUIModelEvent) e;
			if (me.getID() == me.NEW_MODEL) {
				//New Model

				a_clearModel();

				doEnables();
				//newList();
			} else if (!ignoreNextParse && me.getID() == GUIModelEvent.MODEL_PARSED) {

				a_loadModulesFile(me.getModulesFile());

				doEnables();

			} else if (ignoreNextParse) {
				ignoreNextParse = false;
			}

		} else if (e instanceof GUIComputationEvent) {
			if (e.getID() == GUIComputationEvent.COMPUTATION_START) {
				setComputing(true);
			} else if (e.getID() == GUIComputationEvent.COMPUTATION_DONE) {
				setComputing(false);
			} else if (e.getID() == GUIComputationEvent.COMPUTATION_ERROR) {
				setComputing(false);
			}

		} else if (e instanceof GUIPropertiesEvent) {
			if (e.getID() == GUIPropertiesEvent.PROPERTIES_LIST_CHANGED) {
				//repopulateFormulae();
			}
		} else if (e instanceof GUIExitEvent) {
			if (e.getID() == GUIExitEvent.REQUEST_EXIT) {

			}
		}
		return false;
	}

	public void takeCLArgs(String[] args)
	{
	}

	//ACCESS METHODS

	/**
	 * Getter for property verifyAllPropertiesAtOnce.
	 * @return Value of property verifyAllPropertiesAtOnce.
	 */
	public boolean isVerifyAllPropertiesAtOnce()
	{
		return getPrism().getSettings().getBoolean(PrismSettings.SIMULATOR_SIMULTANEOUS);
		//return verifyAllPropertiesAtOnce;
	}

	//UPDATE METHODS

	/**
	 * Setter for property verifyAllPropertiesAtOnce.
	 * @param verifyAllPropertiesAtOnce New value of property verifyAllPropertiesAtOnce.
	 */
	public void setVerifyAllPropertiesAtOnce(boolean verifyAllPropertiesAtOnce) throws PrismException
	{
		getPrism().getSettings().set(PrismSettings.SIMULATOR_SIMULTANEOUS, verifyAllPropertiesAtOnce);
		//this.verifyAllPropertiesAtOnce = verifyAllPropertiesAtOnce;
	}

	protected void doEnables()
	{
		newPath.setEnabled(parsedModel != null && !computing);
		resetPath.setEnabled(pathActive && !computing);
		exportPath.setEnabled(pathActive && !computing);
		randomExploration.setEnabled(pathActive && !computing);
		backtrack.setEnabled(pathActive && !computing);
		configureView.setEnabled(pathActive && !computing);

		randomExplorationButton.setEnabled(pathActive && !computing);
		backtrackButton.setEnabled(pathActive && !computing);

		inputExploreField.setEnabled(pathActive);
		inputBacktrackField.setEnabled(pathActive);

		typeExploreCombo.setEnabled(pathActive);
		typeBacktrackCombo.setEnabled(pathActive);

		currentUpdatesTable.setEnabled(pathActive && !computing);
		autoTimeCheck.setEnabled(pathActive && parsedModel != null && parsedModel.getModelType() == ModelType.CTMC);

		//resetPathButton.setEnabled(pathActive && !computing);
		//exportPathButton.setEnabled(pathActive && !computing);
		//configureViewButton.setEnabled(pathActive && !computing);

		//newPath.setEnabled(parsedModel != null && !computing);
		//newPathButton.setEnabled(parsedModel != null && !computing);

		currentUpdatesTable.setEnabled(pathActive);

		modelType.setEnabled(parsedModel != null);
		modelTypeLabel.setEnabled(parsedModel != null);

		totalTime.setEnabled(pathActive && parsedModel != null && parsedModel.getModelType() == ModelType.CTMC);
		totalTimeLabel.setEnabled(pathActive && parsedModel != null && parsedModel.getModelType() == ModelType.CTMC);

		pathLength.setEnabled(pathActive);
		pathLengthLabel.setEnabled(pathActive);

		definedConstants.setEnabled(pathActive);
		definedConstantsLabel.setEnabled(pathActive);

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

		buttonGroup1 = new javax.swing.ButtonGroup();
		innerButtonPanel = new javax.swing.JPanel();
		newPathButton = new javax.swing.JButton();
		resetPathButton = new javax.swing.JButton();
		exportPathButton = new javax.swing.JButton();
		configureViewButton = new javax.swing.JButton();
		pathTable = new javax.swing.JTable();
		pathTable = new GUISimulatorPathTable(this, pathTableModel, engine);

		jPanel2 = new javax.swing.JPanel();
		jSplitPane1 = new javax.swing.JSplitPane();
		jPanel3 = new javax.swing.JPanel();
		jPanel4 = new javax.swing.JPanel();
		allPanel = new javax.swing.JPanel();
		horizontalSplit = new javax.swing.JSplitPane();
		topPanel = new javax.swing.JPanel();
		topSplit = new javax.swing.JSplitPane();
		tabbedPane = new javax.swing.JTabbedPane();
		outerStateLabelPanel = new javax.swing.JPanel();
		stateLabelScrollPane = new javax.swing.JScrollPane();
		stateLabelList = new javax.swing.JList();
		stateLabelList = new GUISimLabelList(this);
		outerPathFormulaePanel = new javax.swing.JPanel();
		pathFormulaeScrollPane = new javax.swing.JScrollPane();
		pathFormulaeList = new javax.swing.JList();
		pathFormulaeList = new GUISimPathFormulaeList(this);
		informationPanel = new javax.swing.JPanel();
		innerInformationPanel = new javax.swing.JPanel();
		topLabels = new javax.swing.JPanel();
		modelType = new javax.swing.JLabel();
		definedConstants = new javax.swing.JLabel();
		topValues = new javax.swing.JPanel();
		modelTypeLabel = new javax.swing.JLabel();
		definedConstantsLabel = new javax.swing.JLabel();
		bottomLabels = new javax.swing.JPanel();
		pathLength = new javax.swing.JLabel();
		totalTime = new javax.swing.JLabel();
		bottomValues = new javax.swing.JPanel();
		pathLengthLabel = new javax.swing.JLabel();
		totalTimeLabel = new javax.swing.JLabel();
		outerTopLeftPanel = new javax.swing.JPanel();
		topLeftPanel = new javax.swing.JPanel();
		innerTopLeftPanel = new javax.swing.JPanel();
		outerLeftExplorePanel = new javax.swing.JPanel();
		leftExplorePanel = new javax.swing.JPanel();
		automaticExplorationPanel = new javax.swing.JPanel();
		innerAutomaticExplorationPanel = new javax.swing.JPanel();
		randomExplorationButton = new javax.swing.JButton();
		noStepsExplorePanel = new javax.swing.JPanel();
		typeExploreCombo = new javax.swing.JComboBox();
		inputExploreField = new javax.swing.JTextField();
		backtrackPanel = new javax.swing.JPanel();
		innerBacktrackPanel = new javax.swing.JPanel();
		backtrackButton = new javax.swing.JButton();
		noStepsBacktrackPanel = new javax.swing.JPanel();
		typeBacktrackCombo = new javax.swing.JComboBox();
		inputBacktrackField = new javax.swing.JTextField();
		manualUpdatesPanel = new javax.swing.JPanel();
		innerManualUpdatesPanel = new javax.swing.JPanel();
		manualUpdateTableScrollPane = new javax.swing.JScrollPane();
		currentUpdatesTable = new javax.swing.JTable();
		currentUpdatesTable = new GUISimulatorUpdatesTable(updateTableModel, this);
		autoTimeCheckPanel = new javax.swing.JPanel();
		autoTimeCheck = new javax.swing.JCheckBox();
		outerBottomPanel = new javax.swing.JPanel();
		bottomPanel = new javax.swing.JPanel();
		tableScroll = new javax.swing.JScrollPane();
		pathTablePlaceHolder = new javax.swing.JPanel();

		innerButtonPanel.setLayout(new java.awt.GridLayout(2, 2, 10, 10));

		newPathButton.setIcon(new javax.swing.ImageIcon(""));
		newPathButton.setText("New path");
		newPathButton.setToolTipText("New path");
		newPathButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		newPathButton.setPreferredSize(new java.awt.Dimension(119, 28));
		newPathButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				newPathButtonActionPerformed(evt);
			}
		});

		innerButtonPanel.add(newPathButton);

		resetPathButton.setIcon(new javax.swing.ImageIcon(""));
		resetPathButton.setText("Reset path");
		resetPathButton.setToolTipText("Reset path");
		resetPathButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		resetPathButton.setPreferredSize(new java.awt.Dimension(119, 28));
		resetPathButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				resetPathButtonActionPerformed(evt);
			}
		});

		innerButtonPanel.add(resetPathButton);

		exportPathButton.setIcon(new javax.swing.ImageIcon(""));
		exportPathButton.setText("Export path");
		exportPathButton.setToolTipText("Export path");
		exportPathButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		exportPathButton.setPreferredSize(new java.awt.Dimension(119, 28));
		exportPathButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				exportPathButtonActionPerformed(evt);
			}
		});

		innerButtonPanel.add(exportPathButton);

		configureViewButton.setIcon(new javax.swing.ImageIcon(""));
		configureViewButton.setToolTipText("Export path");
		configureViewButton.setActionCommand("Configure view");
		configureViewButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		configureViewButton.setText("Configure view");
		configureViewButton.setPreferredSize(new java.awt.Dimension(119, 28));
		configureViewButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				configureViewButtonActionPerformed(evt);
			}
		});

		innerButtonPanel.add(configureViewButton);

		pathTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null },
				{ null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
		jSplitPane1.setLeftComponent(jPanel3);

		jSplitPane1.setRightComponent(jPanel4);

		jPanel2.add(jSplitPane1);

		setLayout(new java.awt.BorderLayout());

		allPanel.setLayout(new java.awt.BorderLayout());

		allPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		horizontalSplit.setDividerLocation(211);
		horizontalSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
		horizontalSplit.setMinimumSize(new java.awt.Dimension(0, 0));
		horizontalSplit.setOneTouchExpandable(true);
		topPanel.setLayout(new java.awt.BorderLayout());

		topPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		topPanel.setMinimumSize(new java.awt.Dimension(300, 10));
		topPanel.setPreferredSize(new java.awt.Dimension(302, 591));
		topSplit.setBorder(null);
		topSplit.setDividerLocation(600);
		topSplit.setResizeWeight(0.75);
		topSplit.setContinuousLayout(true);
		topSplit.setDoubleBuffered(true);
		topSplit.setMinimumSize(new java.awt.Dimension(0, 0));
		topSplit.setOneTouchExpandable(true);
		topSplit.setPreferredSize(new java.awt.Dimension(0, 0));
		tabbedPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
		tabbedPane.setMinimumSize(new java.awt.Dimension(0, 0));
		tabbedPane.setPreferredSize(new java.awt.Dimension(0, 50));
		outerStateLabelPanel.setLayout(new java.awt.BorderLayout());

		outerStateLabelPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		outerStateLabelPanel.setMinimumSize(new java.awt.Dimension(34, 0));
		stateLabelScrollPane.setMinimumSize(new java.awt.Dimension(24, 0));
		stateLabelScrollPane.setViewportView(stateLabelList);

		outerStateLabelPanel.add(stateLabelScrollPane, java.awt.BorderLayout.CENTER);

		tabbedPane.addTab("State labels", outerStateLabelPanel);

		outerPathFormulaePanel.setLayout(new java.awt.BorderLayout());

		outerPathFormulaePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		outerPathFormulaePanel.setMinimumSize(new java.awt.Dimension(34, 0));
		pathFormulaeScrollPane.setMinimumSize(new java.awt.Dimension(24, 0));
		pathFormulaeScrollPane.setViewportView(pathFormulaeList);

		outerPathFormulaePanel.add(pathFormulaeScrollPane, java.awt.BorderLayout.CENTER);

		tabbedPane.addTab("Path formulae", outerPathFormulaePanel);

		informationPanel.setLayout(new java.awt.BorderLayout());

		informationPanel.setMinimumSize(new java.awt.Dimension(211, 0));
		innerInformationPanel.setLayout(new javax.swing.BoxLayout(innerInformationPanel, javax.swing.BoxLayout.Y_AXIS));

		innerInformationPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		innerInformationPanel.setMinimumSize(new java.awt.Dimension(211, 0));
		topLabels.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

		topLabels.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 0, 5));
		topLabels.setMinimumSize(new java.awt.Dimension(201, 0));
		modelType.setText("Model Type:");
		modelType.setFont(this.getFont().deriveFont(Font.BOLD));
		topLabels.add(modelType);

		definedConstants.setText("Defined Constants:");
		definedConstants.setFont(this.getFont().deriveFont(Font.BOLD));
		topLabels.add(definedConstants);

		innerInformationPanel.add(topLabels);

		topValues.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

		topValues.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));
		modelTypeLabel.setText("Unknown");
		modelTypeLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
		topValues.add(modelTypeLabel);

		definedConstantsLabel.setText("Unknown");
		definedConstantsLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
		topValues.add(definedConstantsLabel);

		innerInformationPanel.add(topValues);

		bottomLabels.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

		bottomLabels.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 0, 5));
		pathLength.setText("Path Length:");
		pathLength.setFont(this.getFont().deriveFont(Font.BOLD));
		bottomLabels.add(pathLength);

		totalTime.setText("Total Time:");
		totalTime.setFont(this.getFont().deriveFont(Font.BOLD));
		bottomLabels.add(totalTime);

		innerInformationPanel.add(bottomLabels);

		bottomValues.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

		bottomValues.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));
		pathLengthLabel.setText("0");
		pathLengthLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
		bottomValues.add(pathLengthLabel);

		totalTimeLabel.setText("0.0");
		totalTimeLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
		bottomValues.add(totalTimeLabel);

		innerInformationPanel.add(bottomValues);

		informationPanel.add(innerInformationPanel, java.awt.BorderLayout.NORTH);

		tabbedPane.addTab("Path information", informationPanel);

		topSplit.setRightComponent(tabbedPane);

		outerTopLeftPanel.setLayout(new java.awt.BorderLayout());

		topLeftPanel.setLayout(new java.awt.BorderLayout());

		topLeftPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 10));
		topLeftPanel.setMinimumSize(new java.awt.Dimension(0, 0));
		topLeftPanel.setPreferredSize(new java.awt.Dimension(0, 0));
		innerTopLeftPanel.setLayout(new java.awt.BorderLayout(5, 5));

		innerTopLeftPanel.setMinimumSize(new java.awt.Dimension(50, 0));
		innerTopLeftPanel.setPreferredSize(new java.awt.Dimension(302, 50));
		outerLeftExplorePanel.setLayout(new java.awt.BorderLayout());

		outerLeftExplorePanel.setMinimumSize(new java.awt.Dimension(129, 0));
		leftExplorePanel.setLayout(new javax.swing.BoxLayout(leftExplorePanel, javax.swing.BoxLayout.Y_AXIS));

		leftExplorePanel.setMinimumSize(new java.awt.Dimension(129, 0));
		automaticExplorationPanel.setLayout(new java.awt.BorderLayout());

		automaticExplorationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Automatic exploration"));
		automaticExplorationPanel.setMinimumSize(new java.awt.Dimension(129, 0));
		innerAutomaticExplorationPanel.setLayout(new java.awt.GridLayout(2, 1, 5, 5));

		innerAutomaticExplorationPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		innerAutomaticExplorationPanel.setMinimumSize(new java.awt.Dimension(117, 0));
		randomExplorationButton.setIcon(new javax.swing.ImageIcon(""));
		randomExplorationButton.setToolTipText("Make a number of random automatic updates");
		randomExplorationButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		randomExplorationButton.setText("Simulate");
		randomExplorationButton.setMaximumSize(new java.awt.Dimension(220, 23));
		randomExplorationButton.setMinimumSize(new java.awt.Dimension(50, 23));
		randomExplorationButton.setPreferredSize(new java.awt.Dimension(160, 23));
		randomExplorationButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				randomExplorationButtonActionPerformed(evt);
			}
		});

		innerAutomaticExplorationPanel.add(randomExplorationButton);

		noStepsExplorePanel.setLayout(new java.awt.GridBagLayout());

		noStepsExplorePanel.setMinimumSize(new java.awt.Dimension(107, 0));
		typeExploreCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Num. steps", "Upto state", "Max. time" }));
		typeExploreCombo.setToolTipText("");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 2.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
		noStepsExplorePanel.add(typeExploreCombo, gridBagConstraints);

		inputExploreField.setText("1");
		inputExploreField.setToolTipText("");
		inputExploreField.setPreferredSize(new java.awt.Dimension(60, 19));
		inputExploreField.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				inputExploreFieldActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.25;
		noStepsExplorePanel.add(inputExploreField, gridBagConstraints);

		innerAutomaticExplorationPanel.add(noStepsExplorePanel);

		automaticExplorationPanel.add(innerAutomaticExplorationPanel, java.awt.BorderLayout.NORTH);

		leftExplorePanel.add(automaticExplorationPanel);

		backtrackPanel.setLayout(new java.awt.BorderLayout());

		backtrackPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Backtracking"));
		backtrackPanel.setMinimumSize(new java.awt.Dimension(129, 0));
		innerBacktrackPanel.setLayout(new java.awt.GridLayout(2, 1, 5, 5));

		innerBacktrackPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		innerBacktrackPanel.setMinimumSize(new java.awt.Dimension(117, 0));
		backtrackButton.setIcon(new javax.swing.ImageIcon(""));
		backtrackButton.setText("Backtrack");
		backtrackButton.setToolTipText("Backtrack to a certain state in your path");
		backtrackButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		backtrackButton.setMaximumSize(new java.awt.Dimension(220, 23));
		backtrackButton.setMinimumSize(new java.awt.Dimension(50, 23));
		backtrackButton.setPreferredSize(new java.awt.Dimension(160, 23));
		backtrackButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				backtrackButtonActionPerformed(evt);
			}
		});

		innerBacktrackPanel.add(backtrackButton);

		noStepsBacktrackPanel.setLayout(new java.awt.GridBagLayout());

		noStepsBacktrackPanel.setMinimumSize(new java.awt.Dimension(107, 0));
		typeBacktrackCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Num. steps", "To state" }));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 2.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
		noStepsBacktrackPanel.add(typeBacktrackCombo, gridBagConstraints);

		inputBacktrackField.setText("1");
		inputBacktrackField.setToolTipText("");
		inputBacktrackField.setPreferredSize(new java.awt.Dimension(60, 19));
		inputBacktrackField.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				inputBacktrackFieldActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.25;
		noStepsBacktrackPanel.add(inputBacktrackField, gridBagConstraints);

		innerBacktrackPanel.add(noStepsBacktrackPanel);

		backtrackPanel.add(innerBacktrackPanel, java.awt.BorderLayout.CENTER);

		leftExplorePanel.add(backtrackPanel);

		outerLeftExplorePanel.add(leftExplorePanel, java.awt.BorderLayout.NORTH);

		innerTopLeftPanel.add(outerLeftExplorePanel, java.awt.BorderLayout.WEST);

		manualUpdatesPanel.setLayout(new java.awt.BorderLayout());

		manualUpdatesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Manual exploration"));
		manualUpdatesPanel.setPreferredSize(new java.awt.Dimension(60, 60));
		innerManualUpdatesPanel.setLayout(new java.awt.BorderLayout());

		innerManualUpdatesPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		manualUpdateTableScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		currentUpdatesTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null },
				{ null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
		currentUpdatesTable.setToolTipText("Double click on an update to manually execute the update");
		manualUpdateTableScrollPane.setViewportView(currentUpdatesTable);

		innerManualUpdatesPanel.add(manualUpdateTableScrollPane, java.awt.BorderLayout.CENTER);

		autoTimeCheckPanel.setLayout(new java.awt.BorderLayout());

		autoTimeCheckPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 0, 0, 0));
		autoTimeCheck.setText("Generate time automatically");
		autoTimeCheck.setToolTipText("When not selected, you will be prompted to manually enter the time spent in states");
		autoTimeCheck.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		autoTimeCheck.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		autoTimeCheck.setMargin(new java.awt.Insets(0, 0, 0, 0));
		autoTimeCheckPanel.add(autoTimeCheck, java.awt.BorderLayout.EAST);

		innerManualUpdatesPanel.add(autoTimeCheckPanel, java.awt.BorderLayout.SOUTH);

		manualUpdatesPanel.add(innerManualUpdatesPanel, java.awt.BorderLayout.CENTER);

		innerTopLeftPanel.add(manualUpdatesPanel, java.awt.BorderLayout.CENTER);

		topLeftPanel.add(innerTopLeftPanel, java.awt.BorderLayout.CENTER);

		outerTopLeftPanel.add(topLeftPanel, java.awt.BorderLayout.CENTER);

		topSplit.setLeftComponent(outerTopLeftPanel);

		topPanel.add(topSplit, java.awt.BorderLayout.CENTER);

		horizontalSplit.setLeftComponent(topPanel);

		outerBottomPanel.setLayout(new java.awt.BorderLayout());

		outerBottomPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		bottomPanel.setLayout(new java.awt.BorderLayout());

		bottomPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Path"));
		bottomPanel.setMinimumSize(new java.awt.Dimension(42, 0));
		tableScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		pathTablePlaceHolder.setToolTipText("Double-click or right-click to create a new path");
		tableScroll.setViewportView(pathTablePlaceHolder);

		bottomPanel.add(tableScroll, java.awt.BorderLayout.CENTER);

		outerBottomPanel.add(bottomPanel, java.awt.BorderLayout.CENTER);

		horizontalSplit.setBottomComponent(outerBottomPanel);

		allPanel.add(horizontalSplit, java.awt.BorderLayout.CENTER);

		add(allPanel, java.awt.BorderLayout.CENTER);

	}// </editor-fold>//GEN-END:initComponents

	/** Override set font to set pass on to path table and header (which may not be visible yet) */
	public void setFont(Font font)
	{
		super.setFont(font);
		if (pathTable != null)
			pathTable.setFont(font);
	}

	private void inputBacktrackFieldActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_inputBacktrackFieldActionPerformed
		// TODO add your handling code here:
	}//GEN-LAST:event_inputBacktrackFieldActionPerformed

	private void backtrackButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_backtrackButtonActionPerformed

		try {
			// Backtrack a specified number of steps
			if (typeBacktrackCombo.getSelectedIndex() == 0) {
				try {
					int noSteps = Integer.parseInt(inputBacktrackField.getText().trim());
					// Number must be >=0
					if (noSteps < 0)
						throw new NumberFormatException();
					// If number is too big, just set as max possible (i.e. path size)
					if (noSteps > engine.getPathSize())
						noSteps = engine.getPathSize();
					// If number is 0, nothing to do
					else if (noSteps == 0)
						return;
					// Do backtrack
					a_backTrack(engine.getPathSize() - noSteps);
				} catch (NumberFormatException nfe) {
					String msg = "The \"" + typeBacktrackCombo.getSelectedItem() + "\" parameter is invalid: ";
					msg += "it should be a positive integer";
					throw new PrismException(msg);
				}
			}
			// Backtrack to a specified step index
			else if (typeBacktrackCombo.getSelectedIndex() == 1) {
				try {
					int toState = Integer.parseInt(inputBacktrackField.getText().trim());
					// Number must be a valid state index
					if (toState < 0 || toState > engine.getPathSize())
						throw new NumberFormatException();
					// Do backtrack
					a_backTrack(toState);
				} catch (NumberFormatException nfe) {
					String msg = "The \"" + typeBacktrackCombo.getSelectedItem() + "\" parameter is invalid: ";
					msg += "it should be between 0 and " + (engine.getPathSize() - 1);
					throw new PrismException(msg);
				}
			}
			// Backtrack for a specific amount of time
			else if (typeBacktrackCombo.getSelectedIndex() == 2) {
				try {
					double time = Double.parseDouble(inputBacktrackField.getText().trim());
					// Time must be >=0
					if (time < 0)
						throw new NumberFormatException();
					// If time is too big, just set as max possible (i.e. total path time)
					if (time >= engine.getTotalTimeForPath())
						time = engine.getTotalTimeForPath();
					// Do backtrack
					a_backTrack(engine.getTotalTimeForPath() - time);
				} catch (NumberFormatException nfe) {
					String msg = "The \"" + typeBacktrackCombo.getSelectedItem() + "\" parameter is invalid: ";
					msg += "it should be a positive double";
					throw new PrismException(msg);
				}
			}
			// Backtrack to a specified point in time
			else if (typeBacktrackCombo.getSelectedIndex() == 3) {
				try {
					double time = Double.parseDouble(inputBacktrackField.getText().trim());
					// Must be a valid time point in path
					if (time < 0 || time >= engine.getTotalTimeForPath())
						throw new NumberFormatException();
					// Do backtrack
					a_backTrack(time);
				} catch (NumberFormatException nfe) {
					String msg = "The \"" + typeBacktrackCombo.getSelectedItem() + "\" parameter is invalid: ";
					msg += "it should be between 0 and " + (engine.getTotalTimeForPath());
					throw new PrismException(msg);
				}
			}
		} catch (PrismException se) {
			this.error(se.getMessage());
		}
	}//GEN-LAST:event_backtrackButtonActionPerformed

	private void configureViewButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_configureViewButtonActionPerformed
		a_configureView();
	}//GEN-LAST:event_configureViewButtonActionPerformed

	private void initPopups()
	{
		newPath = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				GUISimulator.this.tabToFront();
				a_newPath();
			}
		};

		newPath.putValue(Action.LONG_DESCRIPTION, "Creates a new path.");
		newPath.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		newPath.putValue(Action.NAME, "New path");
		newPath.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallStates.png"));
		newPath.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));

		resetPath = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_restartPath();
			}
		};

		resetPath.putValue(Action.LONG_DESCRIPTION, "Resets the path.");
		resetPath.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		resetPath.putValue(Action.NAME, "Reset path");
		resetPath.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPlayerStart.png"));
		resetPath.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F8, InputEvent.SHIFT_DOWN_MASK));

		exportPath = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportPath();
			}
		};

		exportPath.putValue(Action.LONG_DESCRIPTION, "Exports the path.");
		exportPath.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
		exportPath.putValue(Action.NAME, "Export path");
		exportPath.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.png"));

		randomExploration = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				randomExplorationButtonActionPerformed(e);
			}
		};
		randomExploration.putValue(Action.LONG_DESCRIPTION, "Extends the path by simulating.");
		randomExploration.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		randomExploration.putValue(Action.NAME, "Simulate");
		randomExploration.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPlayerFwd.png"));
		randomExploration.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));

		backtrack = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				backtrackButtonActionPerformed(e);
			}
		};
		backtrack.putValue(Action.LONG_DESCRIPTION, "Backtracks the path.");
		backtrack.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_K));
		backtrack.putValue(Action.NAME, "Backtrack");
		backtrack.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPlayerRew.png"));
		backtrack.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_DOWN_MASK));

		backtrackToHere = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_backTrack(pathTable.getSelectedRow());
			}
		};
		backtrackToHere.putValue(Action.LONG_DESCRIPTION, "Backtracks the path to the selected state.");
		backtrackToHere.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_B));
		backtrackToHere.putValue(Action.NAME, "Backtrack to here");
		backtrackToHere.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPlayerRew.png"));

		removeToHere = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{

				try {
					a_removePreceding(pathTable.getSelectedRow());
				} catch (PrismException ex) {
					error(ex.getMessage());
				}
			}
		};
		removeToHere.putValue(Action.LONG_DESCRIPTION, "Removes states preceding the selected state from the path.");
		removeToHere.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		removeToHere.putValue(Action.NAME, "Remove preceding steps");
		removeToHere.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallRemove.png"));

		configureView = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_configureView();
			}
		};
		configureView.putValue(Action.LONG_DESCRIPTION, "Configures the view.");
		configureView.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
		configureView.putValue(Action.NAME, "Configure view");
		configureView.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallOptions.png"));

		pathPopupMenu = new JPopupMenu();
		pathPopupMenu.add(newPath);
		pathPopupMenu.add(resetPath);
		pathPopupMenu.add(exportPath);
		pathPopupMenu.addSeparator();
		pathPopupMenu.add(randomExploration);
		pathPopupMenu.add(backtrack);
		pathPopupMenu.add(backtrackToHere);
		pathPopupMenu.add(removeToHere);
		pathPopupMenu.addSeparator();
		pathPopupMenu.add(configureView);

		simulatorMenu = new JMenu("Simulator");
		simulatorMenu.add(newPath);
		simulatorMenu.add(resetPath);
		simulatorMenu.add(exportPath);
		simulatorMenu.addSeparator();
		simulatorMenu.add(randomExploration);
		simulatorMenu.add(backtrack);
		simulatorMenu.addSeparator();
		simulatorMenu.add(configureView);

		simulatorMenu.setMnemonic('S');
	}

	private void exportPathButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_exportPathButtonActionPerformed
	{//GEN-HEADEREND:event_exportPathButtonActionPerformed
		a_exportPath();
	}//GEN-LAST:event_exportPathButtonActionPerformed

	private void resetPathButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_resetPathButtonActionPerformed
	{//GEN-HEADEREND:event_resetPathButtonActionPerformed
		a_restartPath();
	}//GEN-LAST:event_resetPathButtonActionPerformed

	private void inputExploreFieldActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_inputExploreFieldActionPerformed

	}//GEN-LAST:event_inputExploreFieldActionPerformed

	private void randomExplorationButtonActionPerformed(java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_randomExplorationButtonActionPerformed

		try {
			// Simulate a specified number of steps
			if (typeExploreCombo.getSelectedIndex() == 0) {
				try {
					int noSteps = Integer.parseInt(inputExploreField.getText().trim());
					// Number must be >0
					if (noSteps <= 0)
						throw new NumberFormatException();
					// Do simulation
					a_autoStep(noSteps);
				} catch (NumberFormatException nfe) {
					String msg = "The \"" + typeExploreCombo.getSelectedItem() + "\" parameter is invalid: ";
					msg += "it should be a positive integer";
					throw new PrismException(msg);
				}
			}
			// Simulate up until a specified step index 
			else if (typeExploreCombo.getSelectedIndex() == 1) {
				try {
					int uptoState = Integer.parseInt(inputExploreField.getText().trim());
					// Index must exceed current path length
					if (uptoState <= engine.getPathSize())
						throw new NumberFormatException();
					// Do simulation
					a_autoStep(uptoState - engine.getPathSize());
				} catch (NumberFormatException nfe) {
					String msg = "The \"" + typeExploreCombo.getSelectedItem() + "\" parameter is invalid: ";
					msg += "it should be greater than " + engine.getPathSize();
					throw new PrismException(msg);
				}
			}
			// Simulate for a specified amount of time
			else if (typeExploreCombo.getSelectedIndex() == 2) {
				try {
					double time = Double.parseDouble(inputExploreField.getText().trim());
					// Time must be >=0
					if (time < 0.0)
						throw new NumberFormatException();
					// Do simulation
					a_autoStep(time);
				} catch (NumberFormatException nfe) {
					String msg = "The \"" + typeExploreCombo.getSelectedItem() + "\" parameter is invalid: ";
					msg += "it should be a positive double";
					throw new PrismException(msg);
				}
			}
			// Simulate up until a specified point in time
			else if (typeExploreCombo.getSelectedIndex() == 3) {
				double currentTime = engine.getTotalTimeForPath();
				try {
					double time = Double.parseDouble(inputExploreField.getText().trim());
					// Time must exceed current total path time
					if (time <= currentTime)
						throw new NumberFormatException();
					// Do simulation
					a_autoStep(time - currentTime);
				} catch (NumberFormatException nfe) {
					String msg = "The \"" + typeExploreCombo.getSelectedItem() + "\" parameter is invalid: ";
					msg += "it should be greater than " + currentTime;
				}
			}

		} catch (PrismException se) {
			this.error(se.getMessage());
		}
	}//GEN-LAST:event_randomExplorationButtonActionPerformed

	private void newPathButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_newPathButtonActionPerformed
	{//GEN-HEADEREND:event_newPathButtonActionPerformed
		a_newPath();
	}//GEN-LAST:event_newPathButtonActionPerformed

	/**
	 * Getter for property pathActive.
	 * @return Value of property pathActive.
	 */
	public boolean isPathActive()
	{
		return pathActive;
	}

	/**
	 * Setter for property pathActive.
	 * @param pathActive New value of property pathActive.
	 */
	public void setPathActive(boolean pathActive)
	{
		this.pathActive = pathActive;
		doEnables();
	}

	/**
	 * Getter for property computing.
	 * @return Value of property computing.
	 */
	public boolean isComputing()
	{
		return computing;
	}

	/**
	 * Setter for property computing.
	 * @param computing New value of property computing.
	 */
	public void setComputing(boolean computing)
	{
		this.computing = computing;
		doEnables();
	}

	public void mouseClicked(MouseEvent e)
	{

	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
		doPopupDetection(e);

		if (e.getSource() == pathTable) {
			int row = pathTable.getSelectedRow();
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		doPopupDetection(e);
	}

	public void doPopupDetection(MouseEvent e)
	{
		if (!computing) {
			if (e.getClickCount() == 2 && e.getSource() == pathTablePlaceHolder) {
				if (newPath.isEnabled())
					a_newPath();
			}
			if (e.isPopupTrigger()
					&& (e.getSource() == pathTablePlaceHolder || e.getSource() == pathTable || e.getSource() == pathTable.getTableHeader() || e.getSource() == tableScroll)) {
				randomExploration
						.setEnabled(!(e.getSource() == pathTable.getTableHeader() || e.getSource() == pathTablePlaceHolder || e.getSource() == tableScroll));
				backtrack.setEnabled(!(e.getSource() == pathTable.getTableHeader() || e.getSource() == pathTablePlaceHolder || e.getSource() == tableScroll));
				backtrackToHere
						.setEnabled(!(e.getSource() == pathTable.getTableHeader() || e.getSource() == pathTablePlaceHolder || e.getSource() == tableScroll));
				removeToHere
						.setEnabled(!(e.getSource() == pathTable.getTableHeader() || e.getSource() == pathTablePlaceHolder || e.getSource() == tableScroll));

				doEnables();

				int index = pathTable.rowAtPoint(e.getPoint());

				pathTable.getSelectionModel().setSelectionInterval(index, index);

				/*
				showColumnMenu.removeAll();
				
				//Now populate showColumnMenu with all columns
				for(int i = 1; i < pathTableModel.getColumnCount(); i++)
				{
					// Better to use JCheckBoxMenuItems rather than JCheckBox.
					JCheckBoxMenuItem showColumn = new JCheckBoxMenuItem(pathTableModel.getColumnName(i));
					
					// A column is selected if it is present in the view.
					showColumn.setSelected(pathTable.convertColumnIndexToView(i) != -1);
					
					showColumn.addItemListener(new ItemListener() 
					{
						public void itemStateChanged(ItemEvent e)
						{
							JCheckBoxMenuItem showColumn = (JCheckBoxMenuItem)e.getSource();
							
							if (e.getStateChange() == ItemEvent.DESELECTED)
							{													
								TableColumn tc = pathTable.getColumn(showColumn.getText());								
								hiddenColumns.add(tc);								
								pathTable.removeColumn(tc);
							}
							else if (e.getStateChange() == ItemEvent.SELECTED)
							{
								//search for this in hiddenColumns								
								TableColumn tc = null;
								
								for (int i = 0; i < hiddenColumns.size(); i++)
								{
									tc = (TableColumn)hiddenColumns.get(i);
									if(tc.getHeaderValue().equals(showColumn.getText()))
										break;
								}
								if(tc == null) return;
								
								insertTableColumn(tc);
								hiddenColumns.remove(tc);								
							}
						}
					});					
					
					showColumnMenu.add(showColumn);
				}
				
				*/
				/*
				showColumnMenu.addSeparator();
				
				JMenuItem showAll = new JMenuItem("Show all columns");
				
				showAll.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						for(int i = hiddenColumns.size()-1; i>=0; i--)
						{
							TableColumn tc = (TableColumn) hiddenColumns.get(i);
							insertTableColumn(tc);
							hiddenColumns.remove(tc);
						}
						
						sortOutColumnSizes();
					}
				});
				showColumnMenu.add(showAll);
				*/

				pathPopupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	public void insertTableColumn(TableColumn column)
	{
		int i = 1;
		int j = 1;

		String text = (String) column.getHeaderValue();

		while (j <= pathTable.getColumnCount() && i < pathTableModel.getColumnCount()) {
			if (j == pathTable.getColumnCount()) {
				// Found the location
				pathTable.addColumn(column);

				break;
			} else {
				String tcj = pathTable.getColumnName(j);
				String tci = pathTableModel.getColumnName(i);

				if (tci.equals(text)) {
					// Found the location
					pathTable.addColumn(column);

					// Move to location
					if (j != pathTable.getColumnCount() - 1)
						pathTable.moveColumn(pathTable.getColumnCount() - 1, j);
					break;
				} else {
					i++;

					if (tci.equals(tcj)) {
						j++;
					}
				}
			}
		}
	}

	public void showColumn(int i)
	{

	}

	/**
	 * Getter for property displayStyle.
	 * @return Value of property displayStyle.
	 */
	public boolean isDisplayStyle()
	{
		return displayStyleFast;
	}

	/**
	 * Setter for property displayStyle.
	 * @param displayStyle New value of property displayStyle.
	 */
	public void setDisplayStyle(boolean displayStyle) throws PrismException
	{

		getPrism().getSettings().set(PrismSettings.SIMULATOR_RENDER_ALL_VALUES, new Integer(displayStyle ? 0 : 1));

	}

	/**
	 * Getter for property askForInitialState.
	 * @return Value of property askForInitialState.
	 */
	public boolean isAskForInitialState()
	{
		return getPrism().getSettings().getBoolean(PrismSettings.SIMULATOR_NEW_PATH_ASK_INITIAL);
	}

	/**
	 * Setter for property askForInitialState.
	 * @param askForInitialState New value of property askForInitialState.
	 */
	public void setAskForInitialState(boolean askForInitialState) throws PrismException
	{
		getPrism().getSettings().set(PrismSettings.SIMULATOR_NEW_PATH_ASK_INITIAL, askForInitialState);
	}

	/**
	 * Getter for property maxPathLength.
	 * @return Value of property maxPathLength.
	 */
	/*public int getMaxPathLength()
	{
			return maxPathLength;
	}*/

	/**
	 * Setter for property maxPathLength.
	 * @param maxPathLength New value of property maxPathLength.
	 */
	/*public void setMaxPathLength(int maxPathLength)
	{
			this.maxPathLength = maxPathLength;
			engine.setMaximumPathLength(maxPathLength);
	}*/

	/**
	 * Getter for property displayPathLoops.
	 * @return Value of property displayPathLoops.
	 */
	public boolean isDisplayPathLoops()
	{
		return displayPathLoops;
	}

	/**
	 * Setter for property displayStyle.
	 * @param displayStyle New value of property displayStyle.
	 */
	public void setDisplayPathLoops(boolean b) throws PrismException
	{
		displayPathLoops = b;
		//getPrism().getSettings().set(PrismSettings.SIMULATOR_????, new Boolean(b));
	}

	public ModulesFile getModulesFile()
	{
		return parsedModel;
	}

	/**
	 * Respond to selections of rows in the path table: update transition list.
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		try {
			// If no rows are selected, make the last (current) state selected and proceed
			if (pathTable.getSelectedRowCount() == 0) {
				int last = pathTable.getRowCount() - 1;
				pathTable.getSelectionModel().setSelectionInterval(last, last);
				updateTableModel.updateUpdatesTable();
			}
			// Update transition table according to reflect currently selected step
			updateTableModel.updateUpdatesTable(pathTable.getSelectedRow());
			stateLabelList.repaint();
			pathFormulaeList.repaint();
		} catch (PrismException ex) {
			this.error(ex.getMessage());
		}
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JPanel allPanel;
	private javax.swing.JCheckBox autoTimeCheck;
	private javax.swing.JPanel autoTimeCheckPanel;
	private javax.swing.JPanel automaticExplorationPanel;
	javax.swing.JButton backtrackButton;
	private javax.swing.JPanel backtrackPanel;
	private javax.swing.JPanel bottomLabels;
	private javax.swing.JPanel bottomPanel;
	private javax.swing.JPanel bottomValues;
	private javax.swing.ButtonGroup buttonGroup1;
	javax.swing.JButton configureViewButton;
	javax.swing.JTable currentUpdatesTable;
	private javax.swing.JLabel definedConstants;
	private javax.swing.JLabel definedConstantsLabel;
	javax.swing.JButton exportPathButton;
	private javax.swing.JSplitPane horizontalSplit;
	private javax.swing.JPanel informationPanel;
	private javax.swing.JPanel innerAutomaticExplorationPanel;
	private javax.swing.JPanel innerBacktrackPanel;
	private javax.swing.JPanel innerButtonPanel;
	private javax.swing.JPanel innerInformationPanel;
	private javax.swing.JPanel innerManualUpdatesPanel;
	private javax.swing.JPanel innerTopLeftPanel;
	javax.swing.JTextField inputBacktrackField;
	javax.swing.JTextField inputExploreField;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JSplitPane jSplitPane1;
	private javax.swing.JPanel leftExplorePanel;
	private javax.swing.JScrollPane manualUpdateTableScrollPane;
	private javax.swing.JPanel manualUpdatesPanel;
	private javax.swing.JLabel modelType;
	private javax.swing.JLabel modelTypeLabel;
	javax.swing.JButton newPathButton;
	private javax.swing.JPanel noStepsBacktrackPanel;
	private javax.swing.JPanel noStepsExplorePanel;
	private javax.swing.JPanel outerBottomPanel;
	private javax.swing.JPanel outerLeftExplorePanel;
	private javax.swing.JPanel outerPathFormulaePanel;
	private javax.swing.JPanel outerStateLabelPanel;
	private javax.swing.JPanel outerTopLeftPanel;
	javax.swing.JList pathFormulaeList;
	private javax.swing.JScrollPane pathFormulaeScrollPane;
	private javax.swing.JLabel pathLength;
	private javax.swing.JLabel pathLengthLabel;
	private javax.swing.JTable pathTable;
	private javax.swing.JPanel pathTablePlaceHolder;
	javax.swing.JButton randomExplorationButton;
	javax.swing.JButton resetPathButton;
	private javax.swing.JList stateLabelList;
	private javax.swing.JScrollPane stateLabelScrollPane;
	private javax.swing.JTabbedPane tabbedPane;
	private javax.swing.JScrollPane tableScroll;
	private javax.swing.JPanel topLabels;
	private javax.swing.JPanel topLeftPanel;
	private javax.swing.JPanel topPanel;
	private javax.swing.JSplitPane topSplit;
	private javax.swing.JPanel topValues;
	private javax.swing.JLabel totalTime;
	private javax.swing.JLabel totalTimeLabel;
	private javax.swing.JComboBox typeBacktrackCombo;
	private javax.swing.JComboBox typeExploreCombo;

	// End of variables declaration//GEN-END:variables

	/**
	 * @author mxk
	 * Represents a  in the model.
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
	 * @author mxk
	 * Represents a  in the model.
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

	public class SimulationView extends Observable
	{
		private ArrayList visibleVariables;
		private ArrayList hiddenVariables;

		private ArrayList visibleRewardColumns;
		private ArrayList rewards;

		private boolean stepsVisible;
		private boolean actionsVisible;
		private boolean showTime;
		private boolean showCumulativeTime;
		private boolean useChangeRenderer;

		private boolean initialRun = true;

		public SimulationView()
		{
			this.visibleVariables = new ArrayList();
			this.hiddenVariables = new ArrayList();

			this.visibleRewardColumns = new ArrayList();
			this.rewards = new ArrayList();

			this.stepsVisible = true;
			this.actionsVisible = true;
			this.showTime = false;
			this.showCumulativeTime = true;

			useChangeRenderer = (gui.getPrism().getSettings().getInteger(PrismSettings.SIMULATOR_RENDER_ALL_VALUES) == 0);

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

		public boolean canShowTime()
		{
			return parsedModel.getModelType() == ModelType.CTMC;
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

		public ArrayList getVisibleVariables()
		{
			return visibleVariables;
		}

		public ArrayList getHiddenVariables()
		{
			return hiddenVariables;
		}

		public void setVariableVisibility(ArrayList visibleVariables, ArrayList hiddenVariables)
		{
			this.visibleVariables = visibleVariables;
			this.hiddenVariables = hiddenVariables;

			this.setChanged();
			this.notifyObservers();
		}

		public ArrayList getVisibleRewardColumns()
		{
			return visibleRewardColumns;
		}

		public void setVisibleRewardListItems(ArrayList visibleRewardListItems)
		{
			ArrayList visibleRewardColumns = new ArrayList();

			for (Object obj : visibleRewardListItems) {
				GUIViewDialog.RewardListItem item = (GUIViewDialog.RewardListItem) obj;
				if (item.isCumulative())
					visibleRewardColumns.add(new RewardStructureColumn(item.getRewardStructure(), GUISimulator.RewardStructureColumn.CUMULATIVE_REWARD));
				else {
					if (!item.getRewardStructure().isStateEmpty())
						visibleRewardColumns.add(new RewardStructureColumn(item.getRewardStructure(), GUISimulator.RewardStructureColumn.STATE_REWARD));
					if (!item.getRewardStructure().isTransitionEmpty())
						visibleRewardColumns.add(new RewardStructureColumn(item.getRewardStructure(), GUISimulator.RewardStructureColumn.TRANSITION_REWARD));
				}
			}

			this.visibleRewardColumns = visibleRewardColumns;

			this.setChanged();
			this.notifyObservers();
		}

		public ArrayList getRewards()
		{
			return this.rewards;
		}

		public SimulatorEngine getEngine()
		{
			return engine;
		}

		public boolean isChangeRenderer()
		{
			return useChangeRenderer;
		}

		public void setRenderer(boolean isChangeRenderer)
		{
			if (useChangeRenderer != isChangeRenderer) {
				useChangeRenderer = isChangeRenderer;

				GUISimulator.this.setRenderer(useChangeRenderer);
			}
		}

		public void refreshToDefaultView()
		{
			// First see if we can get away with using current settings...
			boolean canUseCurrentView = true;

			if (!pathActive)
				canUseCurrentView = false;
			else {
				if (useChangeRenderer != usingChangeRenderer()) {
					GUISimulator.this.setRenderer(useChangeRenderer);
				}

				// Time-wise we have a problem.
				if (parsedModel.getModelType() != ModelType.CTMC && (showTime || showCumulativeTime))
					canUseCurrentView = false;

				// Make a set of all variable names.
				TreeSet<String> allVarNames = new TreeSet<String>();

				for (Object var : visibleVariables)
					allVarNames.add(((Variable) var).getName());
				for (Object var : hiddenVariables)
					allVarNames.add(((Variable) var).getName());

				for (int i = 0; i < engine.getNumVariables(); i++) {
					if (allVarNames.contains(engine.getVariableName(i)))
						allVarNames.remove(engine.getVariableName(i));
					else
						// Cannot use current view if a variable is not there.
						canUseCurrentView = false;
				}

				// Cannot use current view if we have too many variables.
				if (allVarNames.size() > 0)
					canUseCurrentView = false;

				// Make a list of all reward structures
				ArrayList<RewardStructure> allrew = new ArrayList<RewardStructure>();

				for (Object rew : rewards) {
					allrew.add((RewardStructure) rew);
				}

				for (int r = 0; r < parsedModel.getNumRewardStructs(); r++) {
					RewardStruct rewardStruct = parsedModel.getRewardStruct(r);
					String rewardName = rewardStruct.getName();

					boolean hasStates = parsedModel.getRewardStruct(r).getNumStateItems() != 0;
					boolean hasTrans = parsedModel.getRewardStruct(r).getNumTransItems() != 0;

					boolean foundReward = false;

					for (Object rewobj : rewards) {
						RewardStructure rew = (RewardStructure) rewobj;
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
					for (int i = 0; i < engine.getNumVariables(); i++) {
						visibleVariables.add(new Variable(i, engine.getVariableName(i), engine.getVariableType(i)));
					}

					for (int r = 0; r < parsedModel.getNumRewardStructs(); r++) {
						RewardStruct rewardStruct = parsedModel.getRewardStruct(r);
						String rewardName = rewardStruct.getName();

						if (rewardName.trim().length() == 0) {
							rewardName = null;
						}

						RewardStructure rewardStructure = new RewardStructure(r, rewardName, parsedModel.getRewardStruct(r).getNumStateItems() == 0,
								parsedModel.getRewardStruct(r).getNumTransItems() == 0);

						if (!rewardStructure.isStateEmpty() || !rewardStructure.isTransitionEmpty())
							rewards.add(rewardStructure);

						if (!rewardStructure.isStateEmpty())
							visibleRewardColumns.add(new RewardStructureColumn(rewardStructure, RewardStructureColumn.STATE_REWARD));

						if (!rewardStructure.isTransitionEmpty())
							visibleRewardColumns.add(new RewardStructureColumn(rewardStructure, RewardStructureColumn.TRANSITION_REWARD));
					}

				}
			}
			initialRun = false;
			this.setChanged();
			this.notifyObservers();

		}
	}

	class PathTableModel extends AbstractTableModel implements GUIGroupedTableModel, Observer
	{
		private SimulationView view;
		private RewardStructureValue rewardStructureValue;
		private VariableValue variableValue;
		private TimeValue timeValue;
		private ActionValue actionValue;

		public PathTableModel(SimulationView view)
		{
			this.view = view;
			this.view.addObserver(this);

			rewardStructureValue = new RewardStructureValue(null, null);
			variableValue = new VariableValue(null, null);
		}

		public int getGroupCount()
		{
			if (!pathActive) {
				return 0;
			} else {
				int groupCount = 0;

				if (view.showSteps() || view.showActions()) {
					groupCount++;
				}

				if (view.canShowTime() && (view.showTime() || view.showCumulativeTime())) {
					groupCount++;
				}

				ArrayList vars = view.getVisibleVariables();
				Set<String> varNames = new HashSet<String>();

				for (Object var : vars) {
					Variable variable = (Variable) var;
					varNames.add(variable.getName());
				}

				for (int g = 0; g < parsedModel.getNumGlobals(); g++) {
					if (varNames.contains(parsedModel.getGlobal(g).getName())) {
						groupCount++;
						break;
					}
				}

				for (int m = 0; m < parsedModel.getNumModules(); m++) {
					Module module = parsedModel.getModule(m);
					for (int v = 0; v < module.getNumDeclarations(); v++) {
						if (varNames.contains(module.getDeclaration(v).getName())) {
							groupCount++;
							break;
						}
					}
				}

				if (view.getVisibleRewardColumns().size() > 0) {
					groupCount++;
				}

				return groupCount;
			}
		}

		public void update(Observable o, Object arg)
		{
			if (o == view) {
				fireTableStructureChanged();

				//Sort out the minimum widths for each column
				sortOutColumnSizes();
			}
		}

		public String getGroupName(int groupIndex)
		{
			if (!pathActive) {
				return "";
			} else {
				int groupCount = 0;

				if (view.showSteps() || view.showActions()) {
					if (groupCount == groupIndex) {
						return "Step";
					}

					groupCount++;
				}

				if (view.canShowTime() && (view.showTime() || view.showCumulativeTime())) {
					if (groupCount == groupIndex) {
						return "Time";
					}

					groupCount++;
				}

				if (view.getVisibleVariables().size() > 0) {
					ArrayList vars = view.getVisibleVariables();
					Set<String> varNames = new HashSet<String>();

					for (Object var : vars) {
						Variable variable = (Variable) var;
						varNames.add(variable.getName());
					}

					for (int g = 0; g < parsedModel.getNumGlobals(); g++) {
						if (varNames.contains(parsedModel.getGlobal(g).getName())) {
							if (groupCount == groupIndex) {
								return "Globals";
							}

							groupCount++;
							break;
						}
					}

					for (int m = 0; m < parsedModel.getNumModules(); m++) {
						Module module = parsedModel.getModule(m);
						for (int v = 0; v < module.getNumDeclarations(); v++) {
							if (varNames.contains(module.getDeclaration(v).getName())) {
								if (groupCount == groupIndex) {
									return "" + parsedModel.getModuleName(m) + "";
								}

								groupCount++;
								break;
							}
						}
					}
				}

				// Add state and transitions rewards for each reward structure.
				if (view.getVisibleRewardColumns().size() > 0) {
					if (groupCount == groupIndex) {
						return "Rewards";
					}

					groupCount++;
				}

				return "Undefined Group";
			}
		}

		public String getGroupToolTip(int groupIndex)
		{
			ArrayList vars = view.getVisibleVariables();
			Set<String> varNames = new HashSet<String>();

			for (Object var : vars) {
				Variable variable = (Variable) var;
				varNames.add(variable.getName());
			}

			int groupCount = 0;

			if (view.showSteps() || view.showActions()) {
				if (groupCount == groupIndex) {
					return null;
				}

				groupCount++;
			}

			if (view.canShowTime() && (view.showTime() || view.showCumulativeTime())) {
				if (groupCount == groupIndex) {
					return null;
				}

				groupCount++;
			}

			for (int g = 0; g < parsedModel.getNumGlobals(); g++) {
				if (varNames.contains(parsedModel.getGlobal(g).getName())) {
					if (groupCount == groupIndex) {
						return "Global variables";
					}

					groupCount++;
					break;
				}
			}

			for (int m = 0; m < parsedModel.getNumModules(); m++) {
				Module module = parsedModel.getModule(m);
				for (int v = 0; v < module.getNumDeclarations(); v++) {
					if (varNames.contains(module.getDeclaration(v).getName())) {
						if (groupCount == groupIndex) {
							return "Variables of module \"" + parsedModel.getModuleName(m) + "\"";
						}

						groupCount++;
						break;
					}
				}
			}

			// Add state and transitions rewards for each reward structure.
			if (view.getVisibleRewardColumns().size() > 0) {
				if (groupCount == groupIndex) {
					return "State, transition and cumulative rewards";
				}

				groupCount++;
			}

			return null;
		}

		public int getLastColumnOfGroup(int groupIndex)
		{
			int stepStart = 0;
			int timeStart = stepStart + (view.showSteps() ? 1 : 0) + (view.showActions() ? 1 : 0);
			int varStart = timeStart + (view.canShowTime() && view.showTime() ? 1 : 0) + (view.canShowTime() && view.showCumulativeTime() ? 1 : 0);
			int rewardStart = varStart + view.getVisibleVariables().size();

			int groupCount = 0;

			if (view.showSteps() || view.showActions()) {
				if (groupCount == groupIndex) {
					if (view.showSteps() && view.showActions())
						return stepStart + 1;
					else
						return stepStart;
				}

				groupCount++;
			}

			if (view.canShowTime() && (view.showTime() || view.showCumulativeTime())) {
				if (groupCount == groupIndex) {
					if (view.showTime() && view.showCumulativeTime())
						return timeStart + 1;
					else
						return timeStart;
				}

				groupCount++;
			}

			if (view.getVisibleVariables().size() > 0) {
				int visVarCount = 0;

				ArrayList vars = view.getVisibleVariables();
				Set<String> varNames = new HashSet<String>();

				for (Object var : vars) {
					Variable variable = (Variable) var;
					varNames.add(variable.getName());
				}

				boolean atLeastOneGlobal = false;

				for (int g = 0; g < parsedModel.getNumGlobals(); g++) {
					boolean contained = varNames.contains(parsedModel.getGlobal(g).getName());

					if (!atLeastOneGlobal && contained) {
						atLeastOneGlobal = true;
					}

					if (contained)
						visVarCount++;
				}

				if (atLeastOneGlobal && groupCount == groupIndex) {
					return varStart + visVarCount - 1;
				}

				if (atLeastOneGlobal) {
					groupCount++;
				}

				for (int m = 0; m < parsedModel.getNumModules(); m++) {
					Module module = parsedModel.getModule(m);
					boolean atLeastOne = false;

					for (int v = 0; v < module.getNumDeclarations(); v++) {
						boolean contained = varNames.contains(module.getDeclaration(v).getName());
						if (!atLeastOne && contained) {
							atLeastOne = true;
						}

						if (contained)
							visVarCount++;
					}

					if (atLeastOne && groupCount == groupIndex) {
						return varStart + visVarCount - 1;
					}

					if (atLeastOne) {
						groupCount++;
					}
				}
			}

			// Add state and transitions rewards for each reward structure.
			if (view.getVisibleRewardColumns().size() > 0) {
				if (groupCount == groupIndex) {
					return rewardStart + view.getVisibleRewardColumns().size() - 1;
				}

				groupCount++;
			}

			return 0;
		}

		/**
		 * Returns the number of columns.
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount()
		{
			if (!pathActive) {
				return 0;
			} else {
				int colCount = 0;

				colCount += (view.showSteps() ? 1 : 0);
				colCount += (view.showActions() ? 1 : 0);
				colCount += (view.canShowTime() && view.showTime() ? 1 : 0) + (view.canShowTime() && view.showCumulativeTime() ? 1 : 0);
				colCount += view.getVisibleVariables().size();
				colCount += view.getVisibleRewardColumns().size();

				return colCount;
			}
		}

		/**
		 * Returns the number of rows.
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount()
		{
			// Return current path size if there is an active path.
			return (pathActive ? engine.getPathSize() + 1 : 0);
		}

		public boolean shouldColourRow(int row)
		{
			int selection = stateLabelList.getSelectedIndex();
			if (selection != -1) {
				GUISimLabelList.SimLabel label = (GUISimLabelList.SimLabel) stateLabelList.getModel().getElementAt(selection);
				if (row == getRowCount() - 1) {
					if (label.getResult() == 1)
						return true;
				} else {
					if (label.getResult(row) == 1)
						return true;
				}
			}

			return false;
		}

		public String getColumnName(int columnIndex)
		{
			if (pathActive) {
				int stepStart = 0;
				int actionStart = stepStart + (view.showSteps() ? 1 : 0);
				int timeStart = actionStart + (view.showActions() ? 1 : 0);
				int cumulativeTimeStart = timeStart + (view.canShowTime() && view.showTime() ? 1 : 0);
				int varStart = cumulativeTimeStart + (view.canShowTime() && view.showCumulativeTime() ? 1 : 0);
				int rewardStart = varStart + view.getVisibleVariables().size();

				// The step column
				if (stepStart <= columnIndex && columnIndex < actionStart) {
					return "#";
				} else if (actionStart <= columnIndex && columnIndex < timeStart) {
					return "Action";
				} else if (timeStart <= columnIndex && columnIndex < cumulativeTimeStart) {
					return "Time";
				} else if (cumulativeTimeStart <= columnIndex && columnIndex < varStart) {
					return "Time (+)";
				}
				// A variable column
				else if (varStart <= columnIndex && columnIndex < rewardStart) {
					return ((Variable) view.getVisibleVariables().get(columnIndex - varStart)).toString();
				}

				else if (rewardStart <= columnIndex) {
					return ((RewardStructureColumn) view.getVisibleRewardColumns().get(columnIndex - rewardStart)).getColumnName();
				}
			}
			return "Undefined Column";
		}

		public String getColumnToolTip(int columnIndex)
		{
			if (pathActive) {
				int stepStart = 0;
				int actionStart = stepStart + (view.showSteps() ? 1 : 0);
				int timeStart = actionStart + (view.showActions() ? 1 : 0);
				int cumulativeTimeStart = timeStart + (view.canShowTime() && view.showTime() ? 1 : 0);
				int varStart = cumulativeTimeStart + (view.canShowTime() && view.showCumulativeTime() ? 1 : 0);
				int rewardStart = varStart + view.getVisibleVariables().size();

				// The step column
				if (stepStart <= columnIndex && columnIndex < actionStart) {
					return "Index of state in path";
				} else if (actionStart <= columnIndex && columnIndex < timeStart) {
					return "Action label or module name";
				} else if (timeStart <= columnIndex && columnIndex < cumulativeTimeStart) {
					return "Time spent in state";
				} else if (cumulativeTimeStart <= columnIndex && columnIndex < varStart) {
					return "Cumulative time";
				}
				// A variable column
				else if (varStart <= columnIndex && columnIndex < rewardStart) {
					return "Values of variable \"" + ((Variable) view.getVisibleVariables().get(columnIndex - varStart)).toString() + "\"";
				}

				else if (rewardStart <= columnIndex) {
					RewardStructureColumn column = ((RewardStructureColumn) view.getVisibleRewardColumns().get(columnIndex - rewardStart));
					String rewardName = column.getRewardStructure().getColumnName();

					if (column.isStateReward())
						return "State reward of reward structure " + rewardName;
					if (column.isTransitionReward())
						return "Transition reward of reward structure " + rewardName;
					if (column.isCumulativeReward())
						return "Cumulative reward of reward structure " + rewardName;
				}
			}
			return "Undefined Column";
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			if (pathActive) {
				int stepStart = 0;
				int actionStart = stepStart + (view.showSteps() ? 1 : 0);
				int timeStart = actionStart + (view.showActions() ? 1 : 0);
				int cumulativeTimeStart = timeStart + (view.canShowTime() && view.showTime() ? 1 : 0);
				int varStart = cumulativeTimeStart + (view.canShowTime() && view.showCumulativeTime() ? 1 : 0);
				int rewardStart = varStart + view.getVisibleVariables().size();

				// The step column
				if (stepStart <= columnIndex && columnIndex < actionStart) {
					return "" + rowIndex;
				}
				// The action column
				else if (actionStart <= columnIndex && columnIndex < timeStart) {
					actionValue = new ActionValue(engine.getModuleOrActionOfPathStep(rowIndex));
					actionValue.setActionValueUnknown(rowIndex >= engine.getPathSize());
					return actionValue;
				}
				// Time column
				else if (timeStart <= columnIndex && columnIndex < cumulativeTimeStart) {
					timeValue = new TimeValue(engine.getTimeSpentInPathStep(rowIndex), false);
					timeValue.setTimeValueUnknown(rowIndex >= engine.getPathSize());
					return timeValue;
				}
				// Cumulative time column
				else if (cumulativeTimeStart <= columnIndex && columnIndex < varStart) {
					timeValue = new TimeValue(engine.getCumulativeTimeUpToPathStep(rowIndex), true);
					timeValue.setTimeValueUnknown(rowIndex > engine.getPathSize()); // Never unknown
					return timeValue;
				}
				// A variable column
				else if (varStart <= columnIndex && columnIndex < rewardStart) {
					Variable var = ((Variable) view.getVisibleVariables().get(columnIndex - varStart));
					Object result = engine.getVariableValueOfPathStep(rowIndex, var.getIndex());
					variableValue.setVariable(var);
					variableValue.setValue(result);
					variableValue.setChanged(rowIndex == 0 || !engine.getVariableValueOfPathStep(rowIndex - 1, var.getIndex()).equals(result));
					return variableValue;
				}
				// A reward column
				else if (rewardStart <= columnIndex) {
					RewardStructureColumn rewardColumn = (RewardStructureColumn) view.getVisibleRewardColumns().get(columnIndex - rewardStart);
					rewardStructureValue.setRewardStructureColumn(rewardColumn);
					rewardStructureValue.setRewardValueUnknown(false);
					// A state reward column
					if (rewardColumn.isStateReward()) {
						double value = engine.getStateRewardOfPathStep(rowIndex, rewardColumn.getRewardStructure().getIndex());
						rewardStructureValue.setChanged(rowIndex == 0
								|| value != engine.getStateRewardOfPathStep(rowIndex - 1, rewardColumn.getRewardStructure().getIndex()));
						rewardStructureValue.setRewardValue(new Double(value));
						rewardStructureValue.setRewardValueUnknown(rowIndex > engine.getPathSize()); // Never unknown
					}
					// A transition reward column
					else if (rewardColumn.isTransitionReward()) {
						double value = engine.getTransitionRewardOfPathStep(rowIndex, rewardColumn.getRewardStructure().getIndex());
						rewardStructureValue.setChanged(rowIndex == 0
								|| value != engine.getTransitionRewardOfPathStep(rowIndex - 1, rewardColumn.getRewardStructure().getIndex()));
						rewardStructureValue.setRewardValue(new Double(value));
						rewardStructureValue.setRewardValueUnknown(rowIndex >= engine.getPathSize());
					}
					// A cumulative reward column
					else {
						double value = engine.getCumulativeRewardUpToPathStep(rowIndex, rewardColumn.getRewardStructure().getIndex());
						rewardStructureValue.setChanged(rowIndex == 0
								|| value != (engine.getCumulativeRewardUpToPathStep(rowIndex - 1, rewardColumn.getRewardStructure().getIndex())));
						rewardStructureValue.setRewardValue(new Double(value));
						rewardStructureValue.setRewardValueUnknown(rowIndex > engine.getPathSize()); // Never unknown
					}
					return rewardStructureValue;
				}
			}

			return "Undefined value";
		}

		/** 
		 * Method is called when a new path is created.
		 * The structure of the path may be for a different model etc.
		 */
		public void restartPathTable()
		{
			view.refreshToDefaultView();
		}

		/** 
		 * Method is called whenever a path is modified.
		 */
		public void updatePathTable()
		{
			fireTableDataChanged();
		}

		public boolean isPathLooping()
		{
			return engine.isPathLooping();
		}

		public int getLoopStart()
		{
			return engine.loopStart();
		}

		public int getLoopEnd()
		{
			return engine.loopEnd();
		}

		public boolean isDisplayPathLoops()
		{
			return displayPathLoops;
		}

		public SimulationView getView()
		{
			return view;
		}

		public void setView(SimulationView view)
		{
			this.view.deleteObserver(this);
			this.view = view;
			this.view.addObserver(this);
		}
	}

	public void sortOutColumnSizes()
	{
		int prevWidth = pathTable.getWidth();

		TableColumnModel cm = pathTable.getColumnModel();

		for (int i = 0; i < cm.getColumnCount(); i++) {
			TableColumn tc = cm.getColumn(i);

			tc.setMinWidth(50);
		}

		if (50 * cm.getColumnCount() > tableScroll.getWidth()) {
			pathTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		} else
			pathTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
	}

	public boolean isOldUpdate()
	{
		return this.updateTableModel.oldUpdate;
	}

	public int getOldUpdateStep()
	{
		return updateTableModel.oldStep;
	}

	public boolean usingChangeRenderer()
	{
		return ((GUISimulatorPathTable) pathTable).usingChangeRenderer();
	}

	public void setRenderer(boolean displayStyleFast)
	{
		if (displayStyleFast) {
			((GUISimulatorPathTable) pathTable).switchToChangeRenderer();
		} else {
			((GUISimulatorPathTable) pathTable).switchToBoringRenderer();
		}

		//displayPathLoops = settings.getBoolean(PrismSettings.SIMULATOR_???);

		repaint();
	}

	class UpdateTableModel extends AbstractTableModel
	{
		public boolean oldUpdate;
		private int oldStep;

		public UpdateTableModel()
		{
			super();
			oldUpdate = false;
			oldStep = -1;
		}

		public int getColumnCount()
		{
			return pathActive ? 3 : 0;
		}

		public int getRowCount()
		{
			return pathActive ? engine.getNumTransitions() : 0;
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			if (pathActive) {
				try {
					switch (columnIndex) {
					case 0:
						return engine.getTransitionModuleOrAction(rowIndex);
					case 1:
						return "" + engine.getTransitionProbability(rowIndex);
					case 2:
						return engine.getTransitionUpdateString(rowIndex);
					default:
						return "";
					}
				} catch (PrismException e) {
					return "";
				}
			}
			return "";
		}

		public String getColumnName(int column)
		{
			if (pathActive) {
				switch (column) {
				case 0:
					return "Module/[action]";
				case 1: {
					if (parsedModel != null && parsedModel.getModelType() == ModelType.CTMC)
						return "Rate";
					else
						return "Probability";

				}
				case 2:
					return "Update";
				default:
					return "";
				}
			} else
				return "";
		}

		/**
		 * Update the updates table to display the available transitions in the current path step.
		 */
		public void updateUpdatesTable() throws PrismException
		{
			if (isOldUpdate()) {
				engine.computeTransitionsForCurrentState();
			}
			oldUpdate = false;
			oldStep = -1;
			doEnables();
			fireTableDataChanged();

			currentUpdatesTable.setEnabled(true);

			if (getRowCount() > 0) {
				currentUpdatesTable.getSelectionModel().setSelectionInterval(0, 0);
			}
		}

		/**
		 * Update the updates table to display the available transitions in a particular path step.
		 */
		public void updateUpdatesTable(int oldStep) throws PrismException
		{
			if (oldStep == pathTable.getRowCount() - 1) // if current state selected
			{
				updateUpdatesTable();
			} else {
				this.oldStep = oldStep;
				oldUpdate = true;
				doEnables();
				engine.computeTransitionsForStep(oldStep);
				fireTableDataChanged();

				currentUpdatesTable.setEnabled(false);

				if (getRowCount() > 0) {
					int selectThis = engine.getChoiceOfPathStep(oldStep);
					currentUpdatesTable.getSelectionModel().setSelectionInterval(selectThis, selectThis);
				}
			}
		}

		public void restartUpdatesTable()
		{
			fireTableStructureChanged();
			if (getRowCount() > 0) {
				oldUpdate = false;
				oldStep = -1;
				currentUpdatesTable.getSelectionModel().setSelectionInterval(0, 0);
			}
		}

		public int getChoiceIndexOf(int row)
		{
			return engine.getChoiceIndexOfTransition(row);
		}
	}

	public void notifySettings(PrismSettings settings)
	{
		displayStyleFast = settings.getInteger(PrismSettings.SIMULATOR_RENDER_ALL_VALUES) == 0;

		if (displayStyleFast != view.isChangeRenderer()) {
			String[] answers = { "Yes", "No" };
			if (GUISimulator.this.question("You have changed the default rendering style of paths. Do you wish \nto reflect this in your current trace?",
					answers, 0) == 0) {
				view.setRenderer(displayStyleFast);
			}
		}
	}

	/**
	 * Utility function to format floating point numbers.
	 */
	public String formatDouble(double d)
	{
		// getPrism().getSettings().
		return PrismUtils.formatDouble(6, d);
	}
}
