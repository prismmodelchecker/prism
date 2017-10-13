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

package userinterface.model;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import parser.Values;
import parser.ast.ModulesFile;
import prism.Model;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismModelListener;
import prism.PrismSettings;
import prism.UndefinedConstants;
import userinterface.GUIConstantsPicker;
import userinterface.GUIPlugin;
import userinterface.model.computation.BuildModelThread;
import userinterface.model.computation.ComputeSteadyStateThread;
import userinterface.model.computation.ComputeTransientThread;
import userinterface.model.computation.ExportBuiltModelThread;
import userinterface.model.computation.LoadPEPAModelThread;
import userinterface.model.computation.LoadPRISMModelThread;
import userinterface.model.computation.ParseModelThread;
import userinterface.model.pepaModel.GUIPepaModelEditor;
import userinterface.util.GUIUndoManager;

@SuppressWarnings("serial")
public class GUIMultiModelHandler extends JPanel implements PrismModelListener
{
	//Constants
	public static final int PRISM_MODE = 1;
	public static final int PEPA_MODE = 2;

	// export entity types
	public static final int TRANS_EXPORT = 1;
	public static final int STATE_REWARDS_EXPORT = 2;
	public static final int TRANS_REWARDS_EXPORT = 3;
	public static final int STATES_EXPORT = 4;
	public static final int LABELS_EXPORT = 5;

	private GUIMultiModel theModel;
	private GUIMultiModelTree tree;
	private GUIModelEditor editor;
	private Prism prism;

	// State
	private int currentMode;
	private boolean modified;
	private boolean modifiedSinceParse;
	private File activeFile;
	private ModulesFile parsedModel;
	private Values lastMFConstants = null;
	private PrismException lastBuildError = null;
	private boolean busy = false;

	// Options
	private boolean autoParse;

	private Font prismEditorFontFast;
	private Color prismEditorColourFast;
	private Color prismEditorBGColourFast;

	private Style prismEditorNumericFast = Style.defaultStyle();
	private Style prismEditorVariableFast = Style.defaultStyle();
	private Style prismEditorKeywordFast = Style.defaultStyle();
	private Style prismEditorCommentFast = Style.defaultStyle();

	private Font pepaEditorFontFast;
	private Color pepaEditorColourFast;
	private Color pepaEditorBGColourFast;

	private Style pepaEditorCommentFast = Style.defaultStyle();

	// Modification Parse updater
	private WaitParseThread waiter;
	private boolean parsing = false;
	private boolean parseAfterParse = false;

	private String lastError;

	private boolean buildAfterReceiveParseNotification = false;
	private boolean exportAfterReceiveParseNotification = false;
	private boolean computeSSAfterReceiveParseNotification = false;
	private boolean computeTransientAfterReceiveParseNotification = false;
	private int exportEntity = 0;
	private int exportType = Prism.EXPORT_PLAIN;
	private File exportFile = null;
	private double transientTime;

	// GUI
	private JSplitPane splitter;
	private JPanel leftHandSide, treeAndBuild;
	private JLabel builtNoStates, builtNoInitStates, builtNoTransitions;

	/** Creates a new instance of GUIMultiModelHandler */
	public GUIMultiModelHandler(GUIMultiModel theModel)
	{
		super();
		this.theModel = theModel;
		prism = theModel.getPrism();
		prism.addModelListener(this);

		int parseDelay = theModel.getPrism().getSettings().getInteger(PrismSettings.MODEL_PARSE_DELAY);
		waiter = new WaitParseThread(parseDelay, this);
		editor = new GUITextModelEditor("", this);
		tree = new GUIMultiModelTree(this);
		splitter = new JSplitPane();

		initComponents();
		newPRISMModel();
		//splitter.setDividerLocation(0);
		//splitter.setLastDividerLocation(200);

		notifySettings(theModel.getPrism().getSettings());

		splitter.setBorder(null);

		setBorder(new EmptyBorder(5, 5, 5, 5));
	}

	// Initialisation of GUI components
	private void initComponents()
	{
		treeAndBuild = new JPanel();
		JPanel topLeft = new JPanel();
		topLeft.setLayout(new BorderLayout());
		topLeft.add(tree, BorderLayout.CENTER);
		JPanel innerBottomLeft = new JPanel(new BorderLayout());
		innerBottomLeft.setBorder(new CompoundBorder(new EmptyBorder(5, 0, 0, 0), new TitledBorder("Built Model")));
		//JToolBar bar = new JToolBar();
		//bar.setFloatable(false);
		//JButton but = new JButton(buildModel);
		//but.setText("Build Model");
		//bar.add(but);
		//bar.add(buildModel);
		JPanel buildPane = new JPanel(new GridLayout(3, 2, 5, 5));

		JLabel statesLabel = new JLabel("States:");
		statesLabel.setFont(statesLabel.getFont().deriveFont(Font.BOLD));
		statesLabel.setHorizontalAlignment(JLabel.RIGHT);

		builtNoStates = new JLabel("...");

		JLabel initStatesLabel = new JLabel("Initial states:");
		initStatesLabel.setFont(initStatesLabel.getFont().deriveFont(Font.BOLD));
		initStatesLabel.setHorizontalAlignment(JLabel.RIGHT);

		builtNoInitStates = new JLabel("...");

		JLabel transLabel = new JLabel("Transitions:");
		transLabel.setFont(transLabel.getFont().deriveFont(Font.BOLD));
		transLabel.setHorizontalAlignment(JLabel.RIGHT);

		builtNoTransitions = new JLabel("...");

		buildPane.add(statesLabel);
		buildPane.add(builtNoStates);
		buildPane.add(initStatesLabel);
		buildPane.add(builtNoInitStates);
		buildPane.add(transLabel);
		buildPane.add(builtNoTransitions);

		//buildPane.setPreferredSize(new Dimension(250, 40));
		//buildPane.setMaximumSize(new Dimension(2000, 40));
		buildPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		//bottomLeft.add(bar, BorderLayout.NORTH);
		innerBottomLeft.add(buildPane, BorderLayout.CENTER);
		treeAndBuild.setLayout(new BorderLayout());
		treeAndBuild.add(topLeft, BorderLayout.CENTER);
		treeAndBuild.add(innerBottomLeft, BorderLayout.SOUTH);
		treeAndBuild.setBorder(new EmptyBorder(0, 0, 0, 5));
		splitter.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

		leftHandSide = new JPanel();
		leftHandSide.setLayout(new BorderLayout());
		leftHandSide.add(treeAndBuild, BorderLayout.CENTER);

		splitter.setLeftComponent(leftHandSide);
		splitter.setRightComponent(editor);
		splitter.setDividerLocation(0.5);
		splitter.setOneTouchExpandable(true);
		setLayout(new BorderLayout());
		add(splitter, BorderLayout.CENTER);
	}

	// New model...

	public void newPRISMModel()
	{
		activeFile = null;
		modified = false;
		modifiedSinceParse = false;
		parsedModel = null;
		updateBuiltModelDisplay();
		if (currentMode == PRISM_MODE) {
			editor.newModel();
		} else {
			editor = new GUITextModelEditor("", this);
			editor.newModel();
			splitter.setRightComponent(editor);
		}
		tree.newTree(false);
		tree.update(parsedModel);
		currentMode = PRISM_MODE;
		theModel.doEnables();
		lastError = "";
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.NEW_MODEL));
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.NEW_LOAD_NOT_RELOAD_MODEL));
	}

	public void newPEPAModel()
	{
		activeFile = null;
		modified = false;
		modifiedSinceParse = false;
		parsedModel = null;
		updateBuiltModelDisplay();
		if (currentMode == PEPA_MODE) {
			editor.newModel();
		} else {
			//editor = new GUIPepaModelEditor(this);
			editor.newModel();
			//splitter.setRightComponent(editor);
		}
		tree.newTree(false);
		tree.update(parsedModel);
		currentMode = PEPA_MODE;
		theModel.doEnables();
		lastError = "";
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.NEW_MODEL));
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.NEW_LOAD_NOT_RELOAD_MODEL));
	}

	// Conversions... (not used)

	public void convertViewToPRISM()
	{
		theModel.doEnables();
	}

	public void convertViewToPEPA()//dummy dummy dummy
	{
		theModel.doEnables();
	}

	// Load model...

	public void loadModel(File f)
	{
		loadModel(f, true);
	}

	public void loadModel(File f, boolean inBackground)
	{
		// guess model type based on extension
		String name = f.getName();
		if (name.endsWith("pm") | name.endsWith("nm") | name.endsWith("sm"))
			loadPRISMModel(f, inBackground);
		else if (name.endsWith("pepa"))
			loadPEPAModel(f, inBackground);
		else
			loadPRISMModel(f, inBackground);
	}

	public void loadPRISMModel(File f)
	{
		loadPRISMModel(f, true);
	}

	public void loadPRISMModel(File f, boolean inBackground)
	{
		lastError = "";
		Thread t = new LoadPRISMModelThread(this, editor, f, false);
		t.start();
		if (!inBackground)
			try {
				t.join();
			} catch (InterruptedException e) {
			}
		theModel.doEnables();
	}

	public synchronized void prismModelLoaded(GUITextModelEditor edit, File f, boolean replaceEditor)
	{
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.NEW_MODEL));
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.NEW_LOAD_NOT_RELOAD_MODEL));
		activeFile = f;
		modified = false;
		modifiedSinceParse = false;
		parsedModel = null;
		updateBuiltModelDisplay();
		if (replaceEditor) {
			editor = edit;
			splitter.setRightComponent(editor);
		}
		tree.newTree(false);
		tree.update(parsedModel);
		tree.makeNotUpToDate();

		currentMode = PRISM_MODE;

		updateAutoParse();
		lastError = "";
		new ParseModelThread(this, editor.getParseText(), false, isAutoParse()).start();
		tree.startParsing();
		parsing = true;
		theModel.doEnables();
		theModel.tabToFront();
	}

	public void loadPEPAModel(File f)
	{
		loadPEPAModel(f, true);
	}

	public void loadPEPAModel(File f, boolean inBackground)
	{
		lastError = "";
		Thread t = new LoadPEPAModelThread(this, editor, f, false);
		t.start();
		if (!inBackground)
			try {
				t.join();
			} catch (InterruptedException e) {
			}
		theModel.doEnables();
	}

	public synchronized void pepaModelLoaded(GUIPepaModelEditor edit, File f, boolean replaceEditor)
	{
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.NEW_MODEL));
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.NEW_LOAD_NOT_RELOAD_MODEL));
		activeFile = f;
		modified = false;
		modifiedSinceParse = false;
		parsedModel = null;
		updateBuiltModelDisplay();
		if (replaceEditor) {
			editor = edit;
			splitter.setRightComponent(editor);
		}
		tree.newTree(false);
		tree.update(parsedModel);
		tree.makeNotUpToDate();
		currentMode = PEPA_MODE;

		updateAutoParse();
		lastError = "";
		new ParseModelThread(this, editor.getParseText(), true, isAutoParse()).start();
		tree.startParsing();
		theModel.doEnables();
		theModel.tabToFront();
	}

	// Reload model...

	public void reloadActiveFile()
	{
		if (activeFile != null) {
			if (currentMode == PRISM_MODE) {
				new LoadPRISMModelThread(this, editor, activeFile, true).start();
			} else if (currentMode == PEPA_MODE) {
				new LoadPEPAModelThread(this, editor, activeFile, true).start();
			}
		}
		theModel.doEnables();
	}

	public synchronized void prismModelReLoaded(File f)
	{
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.NEW_MODEL));
		activeFile = f;
		modified = false;
		parsedModel = null;
		modifiedSinceParse = false;
		updateBuiltModelDisplay();
		currentMode = PRISM_MODE;
		updateAutoParse();
		if (!parsing) {
			parsing = true;
			tree.makeNotUpToDate();

			lastError = "";
			new ParseModelThread(this, editor.getParseText(), false, isAutoParse()).start();
			tree.startParsing();
		} else {
			parseAfterParse = true;
		}
		theModel.doEnables();
		theModel.tabToFront();
	}

	public synchronized void pepaModelReLoaded(File f)
	{
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.NEW_MODEL));
		activeFile = f;
		modified = false;
		parsedModel = null;
		modifiedSinceParse = false;
		updateBuiltModelDisplay();
		currentMode = PEPA_MODE;
		updateAutoParse();
		if (!parsing) {
			parsing = true;
			tree.makeNotUpToDate();

			lastError = "";
			new ParseModelThread(this, editor.getParseText(), true, isAutoParse()).start();
			tree.startParsing();
		} else {
			parseAfterParse = true;
		}
		theModel.doEnables();
		theModel.tabToFront();
	}

	// Save model...

	public int saveToActiveFile()
	{
		return saveToFile(activeFile);
	}

	public int saveToFile(File f)
	{
		try {
			theModel.setTaskBarText("Saving model...");
			if (currentMode == PRISM_MODE)
				((GUITextModelEditor) editor).write(new FileWriter(f));
			else
				((GUIPepaModelEditor) editor).write(new FileWriter(f));
		} catch (IOException e) {
			theModel.setTaskBarText("Saving model... error.");
			theModel.error("Could not save to file \"" + f + "\"");
			return GUIMultiModel.CANCEL;
		} catch (ClassCastException e) {
			theModel.setTaskBarText("Saving model... error.");
			theModel.error("Could not save to file \"" + f + "\"");
			return GUIMultiModel.CANCEL;
		}
		theModel.setTaskBarText("Saving model... done.");
		if (currentMode == PRISM_MODE)
			prismFileWasSaved(f);
		else
			pepaFileWasSaved(f);
		return GUIMultiModel.CONTINUE;
	}

	public void prismFileWasSaved(File f)
	{
		//possibly to handle switching
		activeFile = f;
		modified = false;
		tree.update(parsedModel);
		theModel.doEnables();
	}

	public void pepaFileWasSaved(File f)
	{
		//possibly to handle switching
		activeFile = f;
		modified = false;
		tree.update(parsedModel);
		theModel.doEnables();
	}

	// Parse model...

	public void requestParse(boolean force)
	{
		// only do a parse if we need to (or have been asked to)
		if (modifiedSinceParse || parsedModel == null || force) {
			if (!parsing) {
				lastError = "";
				tree.makeNotUpToDate();
				new ParseModelThread(this, editor.getParseText(), currentMode == PEPA_MODE, false).start();
				tree.startParsing();
				parsing = true;
			} else {
				parseAfterParse = true;
			}
			theModel.doEnables();
		}
		// otherwise use last successful parse
		else {
			modelParsedSuccessful(parsedModel);
		}
	}

	public synchronized void modelParsedSuccessful(ModulesFile m)
	{
		tree.stopParsing();
		parsing = false;
		parsedModel = m;
		modifiedSinceParse = false;
		lastError = "Parse Successful";

		editor.modelParseSuccessful();

		if (parseAfterParse) {
			parseAfterParse = false;
			tree.makeNotUpToDate();
			//start a new parse thread
			if (isAutoParse()) {
				if (waiter != null) {
					waiter.interrupt();
				}
				int parseDelay = theModel.getPrism().getSettings().getInteger(PrismSettings.MODEL_PARSE_DELAY);
				waiter = new WaitParseThread(parseDelay, this);
				waiter.start();
				//Funky thread waiting stuff
			}
		} else if (buildAfterReceiveParseNotification) {
			buildAfterParse();
		} else if (exportAfterReceiveParseNotification) {
			exportAfterParse();
		} else if (computeSSAfterReceiveParseNotification) {
			computeSteadyStateAfterParse();
		} else if (computeTransientAfterReceiveParseNotification) {
			computeTransientAfterParse();
		} else {
			tree.update(parsedModel);
		}
		tree.repaint();
		theModel.doEnables();
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.MODEL_PARSED, parsedModel));
	}

	public synchronized void modelParseFailed(PrismException parserError, boolean background)
	{
		lastError = parserError.getMessage();

		if (parserError instanceof PrismLangException)
			editor.modelParseFailed((PrismLangException) parserError, background);

		tree.stopParsing();
		parsing = false;
		tree.lastParseFailed();
		if (parseAfterParse) {
			parseAfterParse = false;
			tree.makeNotUpToDate();
			//start a new parse thread
			if (isAutoParse()) {
				if (waiter != null) {
					waiter.interrupt();
				}
				int parseDelay = theModel.getPrism().getSettings().getInteger(PrismSettings.MODEL_PARSE_DELAY);
				waiter = new WaitParseThread(parseDelay, this);
				waiter.start();
				//Funky thread waiting stuff
			}
		} else {
			buildAfterReceiveParseNotification = false;
			exportAfterReceiveParseNotification = false;
			computeSSAfterReceiveParseNotification = false;
			computeTransientAfterReceiveParseNotification = false;
		}
		tree.repaint();
		theModel.doEnables();
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.MODEL_PARSE_FAILED));
	}

	// Build model...

	public void forceBuild()
	{
		// set flag
		buildAfterReceiveParseNotification = true;
		// do a parse if necessary
		requestParse(false);
	}

	private void buildAfterParse()
	{
		UndefinedConstants unC;

		// switch off flag
		buildAfterReceiveParseNotification = false;
		// get values for undefined consts
		unC = new UndefinedConstants(parsedModel, null);
		if (unC.getMFNumUndefined() > 0) {
			int result = GUIConstantsPicker.defineConstantsWithDialog(theModel.getGUI(), unC, lastMFConstants, null);
			if (result != GUIConstantsPicker.VALUES_DONE)
				return;
			lastMFConstants = unC.getMFConstantValues();
		}
		try {
			// currently, don't evaluate constants exactly
			prism.setPRISMModelConstants(unC.getMFConstantValues(), false);
		} catch (PrismException e) {
			theModel.error(e.getMessage());
			return;
		}
		new BuildModelThread(this).start();
	}

	public synchronized void notifyModelBuildSuccessful()
	{
		// Deal with a model build success
		// Put this in an invokeLater(...) so can be called from another thread
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				editor.modelParseSuccessful();
				updateBuiltModelDisplay();
				theModel.doEnables();
				theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.MODEL_BUILT, lastMFConstants));
			}
		});
	}

	private void updateBuiltModelDisplay()
	{
		builtNoStates.setText("?");
		builtNoInitStates.setText("?");
		builtNoTransitions.setText("?");
		if (!prism.getExplicit()) {
			Model m = prism.getBuiltModel();
			if (m != null) {
				builtNoStates.setText("" + m.getNumStatesString());
				builtNoInitStates.setText("" + m.getNumStartStates());
				builtNoTransitions.setText("" + m.getNumTransitionsString());
			}
		} else {
			explicit.Model m = prism.getBuiltModelExplicit();
			if (m != null) {
				builtNoStates.setText("" + m.getNumStates());
				builtNoInitStates.setText("" + m.getNumInitialStates());
				builtNoTransitions.setText("" + m.getNumTransitions());
			}
		}
	}

	public synchronized void notifyModelBuildFailed(PrismException e)
	{
		// Deal with a model build failure
		// Put this in an invokeLater(...) so can be called from another thread
		lastBuildError = e;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (lastBuildError != null && lastBuildError instanceof PrismLangException) {
					editor.modelParseFailed((PrismLangException) lastBuildError, false);
					// Bring model tab to front, but only if not busy e.g. with experiment
					if (!theModel.getComputing()) {
						theModel.tabToFront();
					}
				}
				updateBuiltModelDisplay();
				theModel.doEnables();
				theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.MODEL_BUILD_FAILED));
			}
		});
	}

	// Export model...

	public void export(int entity, int type, File f)
	{
		// set flags/store info
		exportAfterReceiveParseNotification = true;
		exportEntity = entity;
		exportType = type;
		exportFile = f;
		// do a parse if necessary
		requestParse(false);
	}

	private void exportAfterParse()
	{
		UndefinedConstants unC;

		// switch off flag
		exportAfterReceiveParseNotification = false;
		// get values for undefined consts
		unC = new UndefinedConstants(parsedModel, null);
		if (unC.getMFNumUndefined() > 0) {
			int result = GUIConstantsPicker.defineConstantsWithDialog(theModel.getGUI(), unC, lastMFConstants, null);
			if (result != GUIConstantsPicker.VALUES_DONE)
				return;
			lastMFConstants = unC.getMFConstantValues();
		}
		try {
			// currently, don't evaluate constants exactly
			prism.setPRISMModelConstants(unC.getMFConstantValues(), false);
		} catch (PrismException e) {
			theModel.error(e.getMessage());
			return;
		}
		// if export is being done to log, switch view to log
		if (exportFile == null)
			theModel.logToFront();
		new ExportBuiltModelThread(this, exportEntity, exportType, exportFile).start();
	}

	// Compute steady-state...

	public void computeSteadyState(int type, File f)
	{
		// set flags/store info
		computeSSAfterReceiveParseNotification = true;
		exportType = type;
		exportFile = f;
		// do a parse if necessary
		requestParse(false);
	}

	private void computeSteadyStateAfterParse()
	{
		UndefinedConstants unC;

		// switch off flag
		computeSSAfterReceiveParseNotification = false;
		// get values for undefined consts
		unC = new UndefinedConstants(parsedModel, null);
		if (unC.getMFNumUndefined() > 0) {
			int result = GUIConstantsPicker.defineConstantsWithDialog(theModel.getGUI(), unC, lastMFConstants, null);
			if (result != GUIConstantsPicker.VALUES_DONE)
				return;
			lastMFConstants = unC.getMFConstantValues();
		}
		try {
			// for steady-state, currently don't evaluate constants exactly
			prism.setPRISMModelConstants(unC.getMFConstantValues(), false);
		} catch (PrismException e) {
			theModel.error(e.getMessage());
			return;
		}
		// if the results are going to the log, switch view to there
		if (exportFile == null)
			theModel.logToFront();
		new ComputeSteadyStateThread(this, exportType, exportFile).start();
	}

	// Compute transient probabilities...

	public void computeTransient(double time, int type, File f)
	{
		computeTransientAfterReceiveParseNotification = true;
		transientTime = time;
		exportType = type;
		exportFile = f;
		// do a parse if necessary
		requestParse(false);
	}

	private void computeTransientAfterParse()
	{
		UndefinedConstants unC;

		// switch off flag
		computeTransientAfterReceiveParseNotification = false;
		// get values for undefined consts
		unC = new UndefinedConstants(parsedModel, null);
		if (unC.getMFNumUndefined() > 0) {
			int result = GUIConstantsPicker.defineConstantsWithDialog(theModel.getGUI(), unC, lastMFConstants, null);
			if (result != GUIConstantsPicker.VALUES_DONE)
				return;
			lastMFConstants = unC.getMFConstantValues();
		}
		try {
			// for transient computation, currently don't evaluate constants exactly
			prism.setPRISMModelConstants(unC.getMFConstantValues(), false);
		} catch (PrismException e) {
			theModel.error(e.getMessage());
			return;
		}
		// if the results are going to the log, switch view to there
		if (exportFile == null)
			theModel.logToFront();
		new ComputeTransientThread(this, transientTime, exportType, exportFile).start();
	}

	public void requestViewModel()
	{
		if (parsedModel != null) {
			theModel.showModel(parsedModel.toString());
		}
		theModel.doEnables();
	}

	public void hasModified(boolean attemptReparse)
	{
		modified = true;
		if (isBusy()) {
			theModel.doEnables();
			return;
		}

		tree.makeNotUpToDate();
		theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.MODIFIED_SINCE_SAVE));
		modifiedSinceParse = true;

		if (!parsing) {
			if (isAutoParse() && attemptReparse) {
				if (waiter != null) {
					waiter.interrupt();
				}
				int parseDelay = theModel.getPrism().getSettings().getInteger(PrismSettings.MODEL_PARSE_DELAY);
				waiter = new WaitParseThread(parseDelay, this);
				waiter.start();
				//Funky thread waiting stuff
			}
		} else {
			parseAfterParse = true;
		}
		theModel.doEnables();
	}

	public void undo()
	{
		editor.undo();
	}

	public void redo()
	{
		editor.redo();
	}

	public void cut()
	{
		editor.cut();
	}

	public void copy()
	{
		editor.copy();
	}

	public void paste()
	{
		editor.paste();
	}

	public void delete()
	{
		editor.delete();
	}

	public void selectAll()
	{
		editor.selectAll();
	}

	//Access methods

	public synchronized int getModelMode()
	{
		return currentMode;
	}

	public synchronized boolean hasActiveFile()
	{
		return activeFile != null;
	}

	public synchronized boolean modified()
	{
		return modified;
	}

	public synchronized String getActiveFileName()
	{
		if (hasActiveFile()) {
			return activeFile.getPath();
		} else
			return "<Untitled>";
	}

	public synchronized String getShortActiveFileName()
	{
		if (hasActiveFile()) {
			return activeFile.getName();
		} else
			return "<Untitled>";
	}

	/**
	 * Is auto-parsing currently enabled?
	 */
	public synchronized boolean isAutoParse()
	{
		return autoParse;
	}

	/**
	 * Update whether auto-parsing is currently enabled,
	 * based on current PRISM settings (and the current model size).
	 */
	public synchronized void updateAutoParse()
	{
		// Store existing status
		boolean autoParseOld = autoParse;
		
		// Is auto-parse switched on?
		autoParse = theModel.getPrism().getSettings().getBoolean(PrismSettings.MODEL_AUTO_PARSE);

		// Should we disable auto parsing? (if the model is too big and that option is not disabled)
		if (isSwitchOnLarge() && autoParse) {
			if (currentMode == PRISM_MODE || currentMode == PEPA_MODE) {
				// "too big" == 25000 chars = 500 lines at 50 chars per line
				if (editor.getParseText().length() > 25000) {
					autoParse = false;
				}
			}
		}

		// If the flag has just been switched ON, do a parse...
		if (!autoParseOld && autoParse) {
			tree.makeNotUpToDate();
			theModel.notifyEventListeners(new GUIModelEvent(GUIModelEvent.MODIFIED_SINCE_SAVE));
			if (!parsing) {
				if (isAutoParse()) {
					if (waiter != null) {
						waiter.interrupt();
					}
					int parseDelay = theModel.getPrism().getSettings().getInteger(PrismSettings.MODEL_PARSE_DELAY);
					waiter = new WaitParseThread(parseDelay, this);
					waiter.start();
				}
			} else {
				parseAfterParse = true;
			}
			theModel.doEnables();
		}
	}

	/**
	 * Should auto=parsing be disabled for large models?
	 */
	public synchronized boolean isSwitchOnLarge()
	{
		return theModel.getPrism().getSettings().getBoolean(PrismSettings.MODEL_AUTO_MANUAL);
	}

	public synchronized ModelType getParsedModelType()
	{
		if (parsedModel != null) {
			return parsedModel.getModelType();
		} else
			return ModelType.MDP;
	}

	public synchronized String getParseErrorMessage()
	{
		return lastError;
	}

	//Access to the top level plugin for communication with the rest of the gui
	public GUIPlugin getGUIPlugin()
	{
		return theModel;
	}

	public int getParseState()
	{
		return tree.getParseSynchState();
	}

	public GUIMultiModelTree getTree()
	{
		return tree;
	}

	/**
	 * Getter for property busy.
	 * @return Value of property busy.
	 */
	public boolean isBusy()
	{
		return busy;
	}

	/**
	 * Setter for property busy.
	 * @param busy New value of property busy.
	 */
	public void setBusy(boolean busy)
	{
		this.busy = busy;
	}

	public void notifySettings(PrismSettings settings)
	{
		updateAutoParse();
		
		prismEditorFontFast = settings.getFontColorPair(PrismSettings.MODEL_PRISM_EDITOR_FONT).f;
		if (editor instanceof GUITextModelEditor)
			((GUITextModelEditor) editor).setEditorFont(prismEditorFontFast);
		prismEditorColourFast = settings.getFontColorPair(PrismSettings.MODEL_PRISM_EDITOR_FONT).c;

		prismEditorBGColourFast = settings.getColor(PrismSettings.MODEL_PRISM_EDITOR_BG_COLOUR);
		if (editor instanceof GUITextModelEditor)
			((GUITextModelEditor) editor).setEditorBackground(prismEditorBGColourFast);
		int stt;
		switch (settings.getInteger(PrismSettings.MODEL_PRISM_EDITOR_NUMERIC_STYLE)) {
		case 0:
			stt = Font.PLAIN;
			break;
		case 1:
			stt = Font.ITALIC;
			break;
		case 2:
			stt = Font.BOLD;
			break;
		default:
			stt = Font.BOLD | Font.ITALIC;
			break;
		}
		prismEditorNumericFast = new Style(settings.getColor(PrismSettings.MODEL_PRISM_EDITOR_NUMERIC_COLOUR), stt);

		switch (settings.getInteger(PrismSettings.MODEL_PRISM_EDITOR_IDENTIFIER_STYLE)) {
		case 0:
			stt = Font.PLAIN;
			break;
		case 1:
			stt = Font.ITALIC;
			break;
		case 2:
			stt = Font.BOLD;
			break;
		default:
			stt = Font.BOLD | Font.ITALIC;
			break;
		}
		prismEditorVariableFast = new Style(settings.getColor(PrismSettings.MODEL_PRISM_EDITOR_IDENTIFIER_COLOUR), stt);
		switch (settings.getInteger(PrismSettings.MODEL_PRISM_EDITOR_KEYWORD_STYLE)) {
		case 0:
			stt = Font.PLAIN;
			break;
		case 1:
			stt = Font.ITALIC;
			break;
		case 2:
			stt = Font.BOLD;
			break;
		default:
			stt = Font.BOLD | Font.ITALIC;
			break;
		}
		prismEditorKeywordFast = new Style(settings.getColor(PrismSettings.MODEL_PRISM_EDITOR_KEYWORD_COLOUR), stt);
		switch (settings.getInteger(PrismSettings.MODEL_PRISM_EDITOR_COMMENT_STYLE)) {
		case 0:
			stt = Font.PLAIN;
			break;
		case 1:
			stt = Font.ITALIC;
			break;
		case 2:
			stt = Font.BOLD;
			break;
		default:
			stt = Font.BOLD | Font.ITALIC;
			break;
		}
		prismEditorCommentFast = new Style(settings.getColor(PrismSettings.MODEL_PRISM_EDITOR_COMMENT_COLOUR), stt);

		pepaEditorFontFast = settings.getFontColorPair(PrismSettings.MODEL_PEPA_EDITOR_FONT).f;
		if (editor instanceof GUIPepaModelEditor)
			((GUIPepaModelEditor) editor).setEditorFont(pepaEditorFontFast);
		pepaEditorColourFast = settings.getColor(PrismSettings.MODEL_PEPA_EDITOR_COMMENT_COLOUR);
		pepaEditorBGColourFast = settings.getColor(PrismSettings.MODEL_PEPA_EDITOR_BG_COLOUR);
		if (editor instanceof GUIPepaModelEditor)
			((GUIPepaModelEditor) editor).setEditorBackground(pepaEditorBGColourFast);
		pepaEditorCommentFast = new Style(settings.getColor(PrismSettings.MODEL_PEPA_EDITOR_COMMENT_COLOUR),
				settings.getInteger(PrismSettings.MODEL_PEPA_EDITOR_COMMENT_STYLE));
	}

	/**
	 * Getter for property prismEditorFontFast.
	 * @return Value of property prismEditorFontFast.
	 */
	public java.awt.Font getPrismEditorFontFast()
	{
		return prismEditorFontFast;
	}

	/**
	 * Getter for property prismEditorColourFast.
	 * @return Value of property prismEditorColourFast.
	 */
	public java.awt.Color getPrismEditorColourFast()
	{
		return prismEditorColourFast;
	}

	/**
	 * Getter for property prismEditorBGColourFast.
	 * @return Value of property prismEditorBGColourFast.
	 */
	public java.awt.Color getPrismEditorBGColourFast()
	{
		return prismEditorBGColourFast;
	}

	/**
	 * Getter for property pepaEditorFontFast.
	 * @return Value of property pepaEditorFontFast.
	 */
	public java.awt.Font getPepaEditorFontFast()
	{
		return pepaEditorFontFast;
	}

	/**
	 * Getter for property pepaEditorColourFast.
	 * @return Value of property pepaEditorColourFast.
	 */
	public java.awt.Color getPepaEditorColourFast()
	{
		return pepaEditorColourFast;
	}

	/**
	 * Getter for property pepaEditorBGColourFast.
	 * @return Value of property pepaEditorBGColourFast.
	 */
	public java.awt.Color getPepaEditorBGColourFast()
	{
		return pepaEditorBGColourFast;
	}

	/**
	 * Getter for property prismEditorNumericFast.
	 * @return Value of property prismEditorNumericFast.
	 */
	public userinterface.model.Style getPrismEditorNumericFast()
	{
		return prismEditorNumericFast;
	}

	/**
	 * Getter for property prismEditorVariableFast.
	 * @return Value of property prismEditorVariableFast.
	 */
	public userinterface.model.Style getPrismEditorVariableFast()
	{
		return prismEditorVariableFast;
	}

	/**
	 * Getter for property prismEditorKeywordFast.
	 * @return Value of property prismEditorKeywordFast.
	 */
	public userinterface.model.Style getPrismEditorKeywordFast()
	{
		return prismEditorKeywordFast;
	}

	/**
	 * Getter for property prismEditorCommentFast.
	 * @return Value of property prismEditorCommentFast.
	 */
	public userinterface.model.Style getPrismEditorCommentFast()
	{
		return prismEditorCommentFast;
	}

	/**
	 * Getter for property pepaEditorCommentFast.
	 * @return Value of property pepaEditorCommentFast.
	 */
	public userinterface.model.Style getPepaEditorCommentFast()
	{
		return pepaEditorCommentFast;
	}

	class WaitParseThread extends Thread
	{
		int time;
		GUIMultiModelHandler handler;
		ParseModelThread parseThread;

		public WaitParseThread(int time, GUIMultiModelHandler handler)
		{
			this.time = time;
			this.handler = handler;
		}

		public void run()
		{
			try {
				sleep(time);
				parseThread = new ParseModelThread(handler, editor.getParseText(), currentMode == PEPA_MODE, isAutoParse());
				parsing = true;
				tree.startParsing();
				parseThread.start();
			} catch (InterruptedException e) {
			}
		}
	}

	public GUIUndoManager getUndoManager()
	{
		return editor.getUndoManager();
	}

	public boolean canDoClipBoardAction(Action action)
	{
		return editor.canDoClipBoardAction(action);
	}

	public void jumpToError()
	{
		if (editor != null && editor instanceof GUITextModelEditor)
			((GUITextModelEditor) editor).jumpToError();
	}

}
