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

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import parser.Values;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;
import prism.PrismSettingsListener;
import prism.PrismUtils;
import prism.UndefinedConstants;
import simulator.PathFullInfo;
import simulator.SimulatorEngine;
import simulator.networking.SimulatorNetworkHandler;
import userinterface.GUIConstantsPicker;
import userinterface.GUIPlugin;
import userinterface.GUIPrism;
import userinterface.OptionsPanel;
import userinterface.graph.Graph;
import userinterface.model.GUIModelEvent;
import userinterface.model.GUIMultiModel;
import userinterface.properties.GUIMultiProperties;
import userinterface.properties.GUIPropertiesEvent;
import userinterface.properties.GUIPropertiesList;
import userinterface.properties.GUIProperty;
import userinterface.simulator.networking.GUINetworkEditor;
import userinterface.util.GUIComputationEvent;
import userinterface.util.GUIEvent;
import userinterface.util.GUIExitEvent;

@SuppressWarnings("serial")
public class GUISimulator extends GUIPlugin implements MouseListener, ListSelectionListener, PrismSettingsListener
{
	private static final long serialVersionUID = 1L;

	// Links to other parts of the GUI
	private GUIMultiProperties guiProp;
	private GUIMultiModel guiMultiModel;

	/** Underlying simulator */
	private SimulatorEngine engine;

	// GUI components
	private UpdateTableModel updateTableModel;
	private GUISimulatorPathTableModel pathTableModel;

	// Menus/actions/etc.
	private JMenu simulatorMenu;
	private JPopupMenu pathPopupMenu;
	private FileFilter textFilter;
	private Action randomExploration, backtrack, backtrackToHere, removeToHere, newPath, newPathFromState, newPathPlot, newPathPlotFromState, resetPath,
			exportPath, plotPath, configureView;

	//Current State
	private boolean pathActive;
	private ModulesFile parsedModel;
	private boolean newPathAfterReceiveParseNotification, newPathPlotAfterReceiveParseNotification;
	private boolean chooseInitialState;

	private Values lastConstants, lastPropertyConstants, lastInitialState;
	private boolean computing;

	// Config/options
	private boolean displayStyleFast;
	private boolean displayPathLoops;
	private SimulationView view;

	/**
	 * Creates a new instance of GUISimulator
	 */
	public GUISimulator(GUIPrism gui)
	{
		super(gui, true);
		this.engine = gui.getPrism().getSimulator();

		view = new SimulationView(this, gui.getPrism().getSettings());
		pathTableModel = new GUISimulatorPathTableModel(this, view);

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

		view.refreshToDefaultView(pathActive, parsedModel);

		setPathActive(false);
		doEnables();

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
							"These are updates from earlier in the path.\nSelect the last state in the path table to continue exploration");
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

		textFilter = new FileNameExtensionFilter("Plain text files (*.txt)", "txt");

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

	public JList getStateLabelList()
	{
		return stateLabelList;
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

	/**
	 * Update all fields in the "Path information" box.
	 */
	private void updatePathInfoAll(UndefinedConstants uCon)
	{
		modelTypeLabel.setText(parsedModel == null ? "Unknown" : parsedModel.getTypeString());
		String constantsString = uCon == null ? "" : uCon.getDefinedConstantsString();
		definedConstantsLabel.setText((constantsString.length() == 0) ? "None" : constantsString);
		pathLengthLabel.setText(pathActive ? "" + engine.getPathSize() : "0");
		totalTimeLabel.setText(pathActive ? formatDouble(engine.getTotalTimeForPath()) : "0");
	}

	/**
	 * Update path length/time fields in the "Path information" box.
	 */
	private void updatePathInfo()
	{
		pathLengthLabel.setText(pathActive ? "" + engine.getPathSize() : "0");
		totalTimeLabel.setText(pathActive ? formatDouble(engine.getTotalTimeForPath()) : "0");
	}

	/**
	 * Repaint state label and path formulae lists.
	 */
	private void repaintLists()
	{
		stateLabelList.repaint();
		pathFormulaeList.repaint();
	}

	/**
	 * React to a new model being loaded into the GUI.
	 */
	public void a_clearModel()
	{
		// Blank out path table
		tableScroll.setViewportView(pathTablePlaceHolder);
		// Update model/path/tables/lists
		setPathActive(false);
		setParsedModel(null);
		pathTableModel.restartPathTable();
		updateTableModel.restartUpdatesTable();
		((GUISimLabelList) stateLabelList).clearLabels();
		((GUISimPathFormulaeList) pathFormulaeList).clearList();
		// Update display
		repaintLists();
		updatePathInfoAll(null);
		doEnables();
	}

	public void a_loadModulesFile(ModulesFile mf)
	{
		// Update model/path/tables/lists
		setPathActive(false);
		setParsedModel(mf);
		pathTableModel.restartPathTable();
		updateTableModel.restartUpdatesTable();
		((GUISimLabelList) stateLabelList).clearLabels();
		((GUISimPathFormulaeList) pathFormulaeList).clearList();
		// Update display
		repaintLists();
		updatePathInfoAll(null);
		doEnables();
		// Populate controls based on model type
		typeExploreCombo.removeAllItems();
		typeExploreCombo.addItem("Steps");
		typeExploreCombo.addItem("Up to step");
		if (mf != null && mf.getModelType().continuousTime()) {
			typeExploreCombo.addItem("Time");
			typeExploreCombo.addItem("Up to time");
		}
		typeBacktrackCombo.setEnabled(false);
		typeBacktrackCombo.removeAllItems();
		typeBacktrackCombo.addItem("Steps");
		typeBacktrackCombo.addItem("Back to step");
		if (mf != null && mf.getModelType().continuousTime()) {
			typeBacktrackCombo.addItem("Time");
			typeBacktrackCombo.addItem("Back to time");
		}
	}

	public void a_clearPath()
	{
		// Update path/tables/lists
		setPathActive(false);
		pathTableModel.restartPathTable();
		updateTableModel.restartUpdatesTable();
		((GUISimLabelList) stateLabelList).clearLabels();
		((GUISimPathFormulaeList) pathFormulaeList).clearList();
		// Update display
		repaintLists();
		updatePathInfoAll(null);
		doEnables();
	}

	public void a_newPath(boolean chooseInitialState)
	{
		// Request a parse
		newPathAfterReceiveParseNotification = true;
		this.chooseInitialState = chooseInitialState;
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.REQUEST_MODEL_PARSE));
	}

	public void newPathAfterParse()
	{
		newPathAfterReceiveParseNotification = false;
		Values initialState;
		try {
			// Check model is simulate-able
			// (bail out now else causes problems below)
			engine.checkModelForSimulation(parsedModel);

			// get properties constants/labels
			PropertiesFile pf;
			try {
				pf = getPrism().parsePropertiesString(parsedModel, guiProp.getConstantsString().toString() + guiProp.getLabelsString());
			} catch (PrismLangException e) {
				// ignore properties if they don't parse
				pf = null; //if any problems
			}

			// if necessary, get values for undefined constants from user
			// (for now, just get constants needed for properties file labels)
			// TODO: find a way to also get constants for any (path) props we need
			//       (for path formulae display)
			UndefinedConstants uCon = new UndefinedConstants(parsedModel, pf, true);
			if (uCon.getMFNumUndefined() + uCon.getPFNumUndefined() > 0) {
				int result = GUIConstantsPicker.defineConstantsWithDialog(getGUI(), uCon, lastConstants, lastPropertyConstants);
				if (result != GUIConstantsPicker.VALUES_DONE)
					return;
			}
			// remember constant values for next time
			lastConstants = uCon.getMFConstantValues();
			lastPropertyConstants = uCon.getPFConstantValues();
			// store constants (currently, compute non-exact for simulation)
			parsedModel.setUndefinedConstants(lastConstants, false);
			pf.setSomeUndefinedConstants(lastPropertyConstants, false);

			// check here for possibility of multiple initial states
			// (not supported yet) to avoid problems below
			if (parsedModel.getInitialStates() != null) {
				throw new PrismException("The simulator does not yet handle models with multiple initial states");
			}

			// do we need to ask for an initial state for simulation?
			// no: just use default/random
			if (!chooseInitialState) {
				initialState = null;
			}
			// yes: user chooses 
			else {
				initialState = a_chooseInitialState();
				// if user clicked cancel from dialog, bail out
				if (initialState == null) {
					return;
				}
			}

			// Insert path table
			tableScroll.setViewportView(pathTable);

			displayPathLoops = true;

			// Create a new path in the simulator and add labels/properties 
			engine.createNewPath(parsedModel);
			engine.initialisePath(initialState == null ? null : new parser.State(initialState, parsedModel));
			// Update model/path/tables/lists
			setPathActive(true);
			pathTableModel.setPath(engine.getPathFull());
			pathTableModel.restartPathTable();
			pathTable.getSelectionModel().setSelectionInterval(0, 0);
			updateTableModel.restartUpdatesTable();
			repopulateFormulae(pf);
			// Update display
			repaintLists();
			updatePathInfoAll(uCon);
			doEnables();

			// store initial state for next time
			lastInitialState = initialState;

			if (getPrism().getSettings().getBoolean(PrismSettings.SIMULATOR_NEW_PATH_ASK_VIEW)) {
				new GUIViewDialog(getGUI(), pathTableModel.getView(), pathTableModel);
			}

		} catch (PrismException e) {
			this.error(e.getMessage());
			if (e instanceof PrismLangException) {
				guiMultiModel.getHandler().modelParseFailed((PrismLangException) e, false);
				guiMultiModel.tabToFront();
			}
		}
	}

	public Values a_chooseInitialState() throws PrismLangException
	{
		// first, pick default values for chooser dialog

		// default initial state if none specified previously
		if (lastInitialState == null) {
			lastInitialState = new Values(parsedModel.getDefaultInitialState(), parsedModel);
		}
		// otherwise, check previously used state for validity
		else {
			boolean match = true;
			int i, n;
			n = parsedModel.getNumVars();
			if (lastInitialState.getNumValues() != n) {
				match = false;
			} else {
				for (i = 0; i < n; i++) {
					if (!lastInitialState.contains(parsedModel.getVarName(i))) {
						match = false;
						break;
					} else {
						int index = lastInitialState.getIndexOf(parsedModel.getVarName(i));
						if (!lastInitialState.getType(index).equals(parsedModel.getVarType(i))) {
							match = false;
							break;
						}
					}
				}
			}
			// if there's a problem, just use the default
			if (!match) {
				lastInitialState = new Values(parsedModel.getDefaultInitialState(), parsedModel);
			}
		}

		Values initialState = null;
		initialState = GUIInitialStatePicker.defineInitalValuesWithDialog(getGUI(), lastInitialState, parsedModel);
		return initialState;
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
			engine.automaticTransitions(noSteps, displayPathLoops);

			// Update model/path/tables/lists
			pathTableModel.updatePathTable();
			int height = (int) pathTable.getPreferredSize().getHeight();
			int width = (int) pathTable.getPreferredSize().getWidth();
			pathTable.scrollRectToVisible(new Rectangle(0, height - 10, width, height));
			updateTableModel.updateUpdatesTable();
			// Update display
			repaintLists();
			updatePathInfo();
			setComputing(false);

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
			// Update model/path/tables/lists
			pathTableModel.updatePathTable();
			int height = (int) pathTable.getPreferredSize().getHeight();
			int width = (int) pathTable.getPreferredSize().getWidth();
			pathTable.scrollRectToVisible(new Rectangle(0, height - 10, width, height));
			updateTableModel.updateUpdatesTable();
			// Update display
			repaintLists();
			updatePathInfo();
			setComputing(false);

		} catch (PrismException e) {
			this.error(e.getMessage());
		}
	}

	/** Backtrack to a certain time. */
	public void a_backTrack(double time) throws PrismException
	{
		try {
			setComputing(true);
			engine.backtrackTo(time);
			// Update model/path/tables/lists
			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();
			// Update display
			repaintLists();
			updatePathInfo();
			setComputing(false);
		} catch (PrismException e) {
			this.error(e.getMessage());
		}
	}

	/** Backtrack to a certain step. */
	public void a_backTrack(int step)
	{
		try {
			setComputing(true);
			engine.backtrackTo(step);
			// Update model/path/tables/lists
			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();
			// Update display
			repaintLists();
			updatePathInfo();
			setComputing(false);
		} catch (PrismException e) {
			this.error(e.getMessage());
		}
	}

	/** Backtrack to the start of the path. */
	public void a_restartPath()
	{
		try {
			setComputing(true);
			engine.backtrackTo(0);
			// Update model/path/tables/lists
			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();
			// Update display
			repaintLists();
			updatePathInfo();
			setComputing(false);
		} catch (PrismException e) {
			this.error(e.getMessage());
		}
	}

	/** Remove the prefix of the current path up to the given path step. */
	public void a_removePreceding(int step) throws PrismException
	{
		setComputing(true);
		engine.removePrecedingStates(step);
		// Update model/path/tables/lists
		pathTableModel.updatePathTable();
		updateTableModel.updateUpdatesTable();
		// Update display
		repaintLists();
		updatePathInfo();
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
			if (parsedModel.getModelType().continuousTime()) {
				if (!autoTimeCheck.isSelected()) {
					time = GUITimeDialog.askTime(this.getGUI(), this);
					if (time < 0.0d) // dialog cancelled
						return;
				}
			}

			setComputing(true);
			if (parsedModel.getModelType().continuousTime() && time != -1) {
				engine.manualTransition(currentUpdatesTable.getSelectedRow(), time);
			} else {
				engine.manualTransition(currentUpdatesTable.getSelectedRow());
			}
			// Update model/path/tables/lists
			pathTableModel.updatePathTable();
			int height = (int) pathTable.getPreferredSize().getHeight();
			int width = (int) pathTable.getPreferredSize().getWidth();
			pathTable.scrollRectToVisible(new Rectangle(0, height - 10, width, height));
			updateTableModel.updateUpdatesTable();
			// Update display
			repaintLists();
			updatePathInfo();
			setComputing(false);

		} catch (NumberFormatException e) {
			this.error("The Auto update \'no. steps\' parameter is invalid.\nIt must be a positive integer representing a step in the path table");
			setComputing(false);
		} catch (PrismException e) {
			this.error(e.getMessage());
			setComputing(false);
		}
	}

	public void a_loadPath(PathFullInfo pathNew)
	{
		try {
			// get properties constants/labels
			PropertiesFile pf;
			try {
				pf = getPrism().parsePropertiesString(parsedModel, guiProp.getConstantsString().toString() + guiProp.getLabelsString());
			} catch (PrismLangException e) {
				// ignore properties if they don't parse
				pf = null; //if any problems
			}
			// Insert path table
			tableScroll.setViewportView(pathTable);

			displayPathLoops = true;

			// Load new path into the simulator 
			engine.loadPath(parsedModel, pathNew);
			// Update model/path/tables/lists
			setPathActive(true);
			pathTableModel.setPath(engine.getPathFull());
			pathTableModel.restartPathTable();
			int last = pathTable.getRowCount() - 1;
			pathTable.getSelectionModel().setSelectionInterval(last, last);
			updateTableModel.restartUpdatesTable();
			repopulateFormulae(pf);
			// Update display
			repaintLists();
			updatePathInfoAll(null);
			doEnables();

		} catch (PrismException e) {
			this.error(e.getMessage());
			setComputing(false);
		}
	}

	public void a_exportPath()
	{
		try {
			if (showSaveFileDialog(textFilter) != JFileChooser.APPROVE_OPTION)
				return;
			setComputing(true);
			engine.exportPath(getChooserFile());
			setComputing(false);
		} catch (PrismException e) {
			error(e.getMessage());
		}
	}

	public void a_plotPath()
	{
		try {
			setComputing(true);
			guiProp.tabToFront();
			Graph graphModel = new Graph();
			guiProp.getGraphHandler().addGraph(graphModel);
			engine.plotPath(graphModel);
			setComputing(false);
		} catch (PrismException e) {
			error(e.getMessage());
		}
	}

	public void a_newPathPlot(boolean chooseInitialState)
	{
		// Request a parse
		newPathPlotAfterReceiveParseNotification = true;
		this.chooseInitialState = chooseInitialState;
		notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.REQUEST_MODEL_PARSE));
	}

	public void newPathPlotAfterParse()
	{
		newPathPlotAfterReceiveParseNotification = false;
		Values initialState;
		try {
			// Check model is simulate-able
			// (bail out now else causes problems below)
			engine.checkModelForSimulation(parsedModel);

			// if necessary, get values for undefined constants from user
			UndefinedConstants uCon = new UndefinedConstants(parsedModel, null);
			if (uCon.getMFNumUndefined() > 0) {
				int result = GUIConstantsPicker.defineConstantsWithDialog(getGUI(), uCon, lastConstants, lastPropertyConstants);
				if (result != GUIConstantsPicker.VALUES_DONE)
					return;
			}
			// remember constant values for next time
			lastConstants = uCon.getMFConstantValues();
			// store constants (currently, compute non-exact for simulation)
			parsedModel.setUndefinedConstants(lastConstants, false);

			// do we need to ask for an initial state for simulation?
			// no: just use default/random
			if (!chooseInitialState) {
				initialState = null;
			}
			// yes: user chooses 
			else {
				initialState = a_chooseInitialState();
				// if user clicked cancel from dialog, bail out
				if (initialState == null) {
					return;
				}
			}

			// Get path details from dialog
			GUIPathPlotDialog pathPlotDialog = GUIPathPlotDialog.showDialog(getGUI(), parsedModel);
			if (pathPlotDialog == null)
				return;
			String simPathDetails = pathPlotDialog.getSimPathString();
			if (simPathDetails == null)
				return;
			long maxPathLength = pathPlotDialog.getMaxPathLength();

			// Create a new path in the simulator and plot it 
			a_clearPath();
			setComputing(true);
			guiProp.tabToFront();
			Graph graphModel = new Graph();
			guiProp.getGraphHandler().addGraph(graphModel);
			getPrism().getMainLog().resetNumberOfWarnings();
			parser.State initialStateObject = initialState == null ? null : new parser.State(initialState, parsedModel);
			new SimPathPlotThread(this, engine, parsedModel, initialStateObject, simPathDetails, maxPathLength, graphModel).start();
			setComputing(false);

			// store initial state for next time
			lastInitialState = initialState;

		} catch (PrismException e) {
			this.error(e.getMessage());
			if (e instanceof PrismLangException) {
				guiMultiModel.getHandler().modelParseFailed((PrismLangException) e, false);
				guiMultiModel.tabToFront();
			}
		}
	}

	public void a_configureView()
	{
		new GUIViewDialog(getGUI(), pathTableModel.getView(), pathTableModel);
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

		// Path formulae
		// TODO: Currently, path formulae containing undefined property constants
		// are ignored. It would be better if the user is queried
		// for any necessary undefined constants, too.
		GUISimPathFormulaeList thePathFormulaeList = (GUISimPathFormulaeList) pathFormulaeList;
		thePathFormulaeList.clearList();
		if (pathActive) {
			// Go through the property list from the Properties tab of GUI
			GUIPropertiesList gpl = guiProp.getPropList();
			for (int i = 0; i < gpl.getNumProperties(); i++) {
				GUIProperty gp = gpl.getProperty(i);

				// obtain constants in property
				Vector<String> propertyConstants = gp.getProperty().getAllConstants();
				boolean allConstantsDefined = true;
				for (String propertyConstant : propertyConstants) {
					if (!parsedModel.isDefinedConstant(propertyConstant) &&
					    !propertiesFile.isDefinedConstant(propertyConstant)) {
						// we found one that has not been defined in the model
						// or the property file
						allConstantsDefined = false;
						break;
					}
				}

				// If the property has no unresolved constants
				// and is simulate-able...
				if (allConstantsDefined &&
				    gp.isValidForSimulation()) {
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

	@Override
	public boolean processGUIEvent(GUIEvent e)
	{
		if (e instanceof GUIModelEvent) {
			GUIModelEvent me = (GUIModelEvent) e;
			if (me.getID() == GUIModelEvent.NEW_MODEL) {
				a_clearModel();
			} else if (me.getID() == GUIModelEvent.MODEL_PARSED) {
				a_loadModulesFile(me.getModulesFile());
				doEnables();
				if (newPathAfterReceiveParseNotification)
					newPathAfterParse();
				if (newPathPlotAfterReceiveParseNotification)
					newPathPlotAfterParse();
			} else if (me.getID() == GUIModelEvent.MODEL_PARSE_FAILED) {
				newPathAfterReceiveParseNotification = false;
				newPathPlotAfterReceiveParseNotification = false;
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

	protected void doEnables()
	{
		newPath.setEnabled(parsedModel != null && !computing);
		newPathFromState.setEnabled(parsedModel != null && !computing);
		newPathPlot.setEnabled(parsedModel != null && !computing);
		newPathPlotFromState.setEnabled(parsedModel != null && !computing);
		resetPath.setEnabled(pathActive && !computing);
		exportPath.setEnabled(pathActive && !computing);
		plotPath.setEnabled(pathActive && !computing);
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
		currentUpdatesTable.setToolTipText(currentUpdatesTable.isEnabled() ? "Double click on an update to manually execute it" : null);
		autoTimeCheck.setEnabled(pathActive && parsedModel != null && parsedModel.getModelType().continuousTime());

		//resetPathButton.setEnabled(pathActive && !computing);
		//exportPathButton.setEnabled(pathActive && !computing);
		//configureViewButton.setEnabled(pathActive && !computing);

		//newPath.setEnabled(parsedModel != null && !computing);
		//newPathFromState.setEnabled(parsedModel != null && !computing);
		//newPathButton.setEnabled(parsedModel != null && !computing);

		modelType.setEnabled(parsedModel != null);
		modelTypeLabel.setEnabled(parsedModel != null);

		totalTime.setEnabled(pathActive && parsedModel != null && parsedModel.getModelType().continuousTime());
		totalTimeLabel.setEnabled(pathActive && parsedModel != null && parsedModel.getModelType().continuousTime());

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
						noSteps = (int) engine.getPathSize();
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
				a_newPath(false);
			}
		};

		newPath.putValue(Action.LONG_DESCRIPTION, "Creates a new path.");
		newPath.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		newPath.putValue(Action.NAME, "New path");
		newPath.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallStates.png"));
		newPath.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));

		newPathFromState = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				GUISimulator.this.tabToFront();
				a_newPath(true);
			}
		};

		newPathFromState.putValue(Action.LONG_DESCRIPTION, "Creates a new path from a chosen state.");
		//newPathFromState.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		newPathFromState.putValue(Action.NAME, "New path from state");
		newPathFromState.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallStates.png"));

		newPathPlot = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_newPathPlot(false);
			}
		};
		newPathPlot.putValue(Action.LONG_DESCRIPTION, "Creates and plots a new path.");
		//newPathPlot.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		newPathPlot.putValue(Action.NAME, "Plot new path");
		newPathPlot.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileGraph.png"));
		newPathPlot.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F8, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		newPathPlotFromState = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_newPathPlot(true);
			}
		};
		newPathPlotFromState.putValue(Action.LONG_DESCRIPTION, "Creates and plots a new path from a chosen state.");
		//newPathPlotFromState.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		newPathPlotFromState.putValue(Action.NAME, "Plot new path from state");
		newPathPlotFromState.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileGraph.png"));

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

		plotPath = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_plotPath();
			}
		};
		plotPath.putValue(Action.LONG_DESCRIPTION, "Plots the path on a graph.");
		plotPath.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		plotPath.putValue(Action.NAME, "Plot path");
		plotPath.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileGraph.png"));

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
		pathPopupMenu.add(newPathFromState);
		pathPopupMenu.add(newPathPlot);
		pathPopupMenu.add(newPathPlotFromState);
		pathPopupMenu.addSeparator();
		pathPopupMenu.add(resetPath);
		pathPopupMenu.add(exportPath);
		pathPopupMenu.add(plotPath);
		pathPopupMenu.addSeparator();
		pathPopupMenu.add(randomExploration);
		pathPopupMenu.add(backtrack);
		pathPopupMenu.add(backtrackToHere);
		pathPopupMenu.add(removeToHere);
		pathPopupMenu.addSeparator();
		pathPopupMenu.add(configureView);

		simulatorMenu = new JMenu("Simulator");
		simulatorMenu.add(newPath);
		simulatorMenu.add(newPathFromState);
		simulatorMenu.add(newPathPlot);
		simulatorMenu.add(newPathPlotFromState);
		simulatorMenu.addSeparator();
		simulatorMenu.add(resetPath);
		simulatorMenu.add(exportPath);
		simulatorMenu.add(plotPath);
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
					throw new PrismException(msg);
				}
			}

		} catch (PrismException se) {
			this.error(se.getMessage());
		}
	}//GEN-LAST:event_randomExplorationButtonActionPerformed

	private void newPathButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_newPathButtonActionPerformed
	{//GEN-HEADEREND:event_newPathButtonActionPerformed
		a_newPath(false);
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
		pathTableModel.setPathActive(pathActive);
	}

	/**
	 * Setter for property parsedModel.
	 * @param pathActive New value of property pathActive.
	 */
	public void setParsedModel(ModulesFile parsedModel)
	{
		this.parsedModel = parsedModel;
		pathTableModel.setParsedModel(parsedModel);
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
	}

	public void mouseReleased(MouseEvent e)
	{
		doPopupDetection(e);
	}

	public void doPopupDetection(MouseEvent e)
	{
		if (!computing) {
			if (e.getClickCount() == 2 && (e.getSource() == pathTablePlaceHolder || e.getSource() == tableScroll)) {
				if (newPath.isEnabled())
					a_newPath(false);
			} else if (e.isPopupTrigger()
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
	 * Getter for property displayPathLoops.
	 * @return Value of property displayPathLoops.
	 */
	public boolean isDisplayPathLoops()
	{
		return displayPathLoops;
	}

	/**
	 * Setter for property displayStyle.
	 * @param b New value of property displayStyle.
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
			try {
				return pathActive ? engine.getNumTransitions() : 0;
			} catch (PrismException e) {
				return 0;
			}
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
				case 1:
					return parsedModel == null ? "Probability" : parsedModel.getModelType().probabilityOrRate();
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
			currentUpdatesTable.setToolTipText("Double click on an update to manually execute it");

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
				currentUpdatesTable.setToolTipText(null);

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
			try {
				return engine.getChoiceIndexOfTransition(row);
			} catch (PrismException e) {
				return -1;
			}
		}
	}

	public void notifySettings(PrismSettings settings)
	{
		displayStyleFast = settings.getInteger(PrismSettings.SIMULATOR_RENDER_ALL_VALUES) == 0;

		/*if (displayStyleFast != view.isChangeRenderer()) {
			String[] answers = { "Yes", "No" };
			if (GUISimulator.this.question("You have changed the default rendering style of paths. Do you wish \nto reflect this in your current trace?",
					answers, 0) == 0) {
				view.setRenderer(displayStyleFast);
			}
		}*/
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
