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
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import prism.ModelType;
import prism.Prism;
import prism.PrismSettings;
import prism.PrismSettingsListener;
import userinterface.GUIClipboardEvent;
import userinterface.GUIPlugin;
import userinterface.GUIPrism;
import userinterface.OptionsPanel;
import userinterface.util.GUIComputationEvent;
import userinterface.util.GUIEvent;
import userinterface.util.GUIExitEvent;
import userinterface.util.GUIPrismFileFilter;
import userinterface.util.GUIUndoManager;

@SuppressWarnings("serial")
public class GUIMultiModel extends GUIPlugin implements PrismSettingsListener
{
	//Constants
	public static final boolean GM_ENABLED = false;

	public static final int CONTINUE = 0;
	public static final int CANCEL = 1;

	public static final int FILTER_PRISM_MODEL = 0;
	public static final int FILTER_PEPA_MODEL = 1;
	public static final int FILTER_GRAPHIC_MODEL = 2;

	//GUI
	private JTextField fileTextField;
	private JMenu modelMenu, newMenu, viewMenu, exportMenu, computeMenu, computeExportMenu;
	private JMenu exportStatesMenu, exportTransMenu, exportStateRewardsMenu, exportTransRewardsMenu, exportLabelsMenu, exportSSMenu, exportTrMenu;
	private AbstractAction viewStates, viewTrans, viewStateRewards, viewTransRewards, viewLabels, viewPrismCode, computeSS, computeTr, newPRISMModel,
			newGraphicModel, newPEPAModel, loadModel, reloadModel, saveModel, saveAsModel, parseModel, buildModel, exportStatesPlain, exportStatesMatlab,
			exportTransPlain, exportTransMatlab, exportTransDot, exportTransDotStates, exportTransMRMC, exportStateRewardsPlain, exportStateRewardsMatlab,
			exportStateRewardsMRMC, exportTransRewardsPlain, exportTransRewardsMatlab, exportTransRewardsMRMC, exportLabelsPlain, exportLabelsMatlab,
			exportSSPlain, exportSSMatlab, exportTrPlain, exportTrMatlab;
	private JPopupMenu popup;
	//Contents
	private GUIMultiModelHandler handler;
	private GUIPrismFileFilter modelFilters[];
	private GUIPrismFileFilter textFilter[];
	private GUIPrismFileFilter matlabFilter[];
	private GUIPrismFileFilter dotFilter[];
	//State
	private boolean computing = false;
	private boolean initialised = false;

	/** Creates a new instance of GUIMultiModel */
	public GUIMultiModel(GUIPrism pr)
	{
		super(pr);
		initialised = false;
		initComponents();
		initialised = true;
		doEnables();
	}

	public void takeCLArgs(String args[])
	{
		if (args.length > 0) {
			File f = new File(args[0]);
			// load the specified model file
			handler.loadModel(f, false);
			// set the default directory of the file chooser
			File dir = f.getParentFile();
			if (dir == null)
				dir = new File(".");
			getGUI().getChooser().setCurrentDirectory(dir);
		}
	}

	public GUIMultiModelHandler getHandler()
	{
		return handler;
	}

	public JPopupMenu getPopup()
	{
		return popup;
	}

	public boolean getComputing()
	{
		return computing;
	}
	
	public void doEnables()
	{
		// do nothing if not initialised yet
		if (!initialised)
			return;
		// setup file label
		int mode = handler.getModelMode();
		String s = "";
		switch (mode) {
		case GUIMultiModelHandler.PRISM_MODE:
			s += "PRISM Model File: ";
			break;
		case GUIMultiModelHandler.PEPA_MODE:
			s += "PEPA Model File: ";
			break;
		case GUIMultiModelHandler.GRAPHIC_MODE:
			s += "PRISM Graphic Model File: ";
			break;
		}
		s += handler.getActiveFileName();
		if (handler.modified())
			s += "*";
		fileTextField.setText(s);
		// model menu
		newPRISMModel.setEnabled(!computing);
		newGraphicModel.setEnabled(!computing);
		newPEPAModel.setEnabled(!computing);
		loadModel.setEnabled(!computing);
		reloadModel.setEnabled(!computing && handler.hasActiveFile());
		saveModel.setEnabled(!computing && handler.modified());
		saveAsModel.setEnabled(!computing);
		parseModel.setEnabled(!computing);
		buildModel.setEnabled(!computing);
		viewStates.setEnabled(!computing);
		viewTrans.setEnabled(!computing);
		viewStateRewards.setEnabled(!computing);
		viewTransRewards.setEnabled(!computing);
		viewLabels.setEnabled(!computing);
		viewPrismCode.setEnabled(!computing && handler.getParseState() == GUIMultiModelTree.TREE_SYNCHRONIZED_GOOD);
		computeSS.setEnabled(!computing && (handler.getParsedModelType() == ModelType.CTMC || handler.getParsedModelType() == ModelType.DTMC));
		computeTr.setEnabled(!computing && (handler.getParsedModelType() == ModelType.CTMC || handler.getParsedModelType() == ModelType.DTMC));
		exportStatesPlain.setEnabled(!computing);
		exportStatesMatlab.setEnabled(!computing);
		exportTransPlain.setEnabled(!computing);
		exportTransMatlab.setEnabled(!computing);
		exportTransDot.setEnabled(!computing);
		exportTransDotStates.setEnabled(!computing);
		exportTransMRMC.setEnabled(!computing);
		exportStateRewardsPlain.setEnabled(!computing);
		exportStateRewardsMatlab.setEnabled(!computing);
		exportStateRewardsMRMC.setEnabled(!computing);
		exportTransRewardsPlain.setEnabled(!computing);
		exportTransRewardsMatlab.setEnabled(!computing);
		exportTransRewardsMRMC.setEnabled(!computing);
		exportLabelsPlain.setEnabled(!computing);
		exportLabelsMatlab.setEnabled(!computing);
	}

	public int doModificationCheck()
	{
		if (!handler.modified())
			return CONTINUE;
		if (!handler.hasActiveFile()) {
			String[] selection = { "Yes", "No", "Cancel" };
			int selectionNo = -1;
			selectionNo = optionPane("Model has been modified.\nDo you wish to save it?", "Question", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, selection, selection[0]);
			switch (selectionNo) {
			case 0:
				return a_saveModelAs();
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
			selectionNo = optionPane("Model has been modified.\nDo you wish to save it?", "Question", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, selection, selection[0]);
			switch (selectionNo) {
			case 0:
				return a_saveModel();
			case 1:
				return CONTINUE;
			case 2:
				return a_saveModelAs();
			case 3:
				return CANCEL;
			default:
				return CANCEL;
			}
		}
	}

	public void showModel(String modelString)
	{
		JDialog diag = new JDialog(getGUI(), false);

		diag.setTitle("Parsed PRISM Model");
		GUITextModelEditor edit = new GUITextModelEditor(modelString, handler);
		edit.setEditable(false);
		edit.setBackground(new Color(224, 255, 255));
		/*
		PrismEditorKit kit = new PrismEditorKit();
		edit.setEditorKitForContentType("text/prism", kit);
		edit.setContentType("text/prism");
		edit.setPreferredSize(new Dimension(1000,300));
		edit.setText(modelString);
		edit.setFont(new Font("monospaced", Font.PLAIN, 12));*/

		JScrollPane scro = new JScrollPane();
		scro.setPreferredSize(new Dimension(640, 300));
		scro.setViewportView(edit);
		diag.getContentPane().add(scro);

		diag.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		diag.pack();
		diag.setLocationRelativeTo(getGUI()); // centre
		diag.setVisible(true);
	}

	//Action methods

	protected void a_newPRISMModel()
	{
		int cont = doModificationCheck();
		if (cont == CONTINUE)
			handler.newPRISMModel();
	}

	protected void a_newPEPAModel()
	{
		int cont = doModificationCheck();
		if (cont == CONTINUE)
			handler.newPEPAModel();
	}

	protected void a_newGraphicModel()
	{
		int cont = doModificationCheck();
		if (cont == CONTINUE)
			handler.newGraphicModel();
	}

	protected void a_openModel()
	{
		int cont = doModificationCheck();
		if (cont == CONTINUE) {
			int filterIndex;
			switch (handler.getModelMode()) {
			case GUIMultiModelHandler.PEPA_MODE:
				filterIndex = FILTER_PEPA_MODEL;
				break;
			case GUIMultiModelHandler.GRAPHIC_MODE:
				filterIndex = FILTER_GRAPHIC_MODEL;
				break;
			default:
				filterIndex = FILTER_PRISM_MODEL;
			}
			if (showOpenFileDialog(modelFilters, modelFilters[filterIndex]) == JFileChooser.APPROVE_OPTION) {
				File file = getChooserFile();
				if (file == null) {
					error("No file selected");
					return;
				}
				getPrism().getMainLog().resetNumberOfWarnings();
				handler.loadModel(file);
			}
		}
	}

	protected void a_reloadModel()
	{
		int cont = doModificationCheck();
		if (cont == CONTINUE) {
			getPrism().getMainLog().resetNumberOfWarnings();
			handler.reloadActiveFile();
		}
	}

	protected int a_saveModel()
	{
		if (!handler.hasActiveFile()) {
			return a_saveModelAs();
		} else {
			getPrism().getMainLog().resetNumberOfWarnings();
			return handler.saveToActiveFile();
		}
	}

	protected int a_saveModelAs()
	{
		int mode = handler.getModelMode();
		int filterIndex;
		switch (mode) {
		case GUIMultiModelHandler.PEPA_MODE:
			filterIndex = FILTER_PEPA_MODEL;
			break;
		case GUIMultiModelHandler.GRAPHIC_MODE:
			filterIndex = FILTER_GRAPHIC_MODEL;
			break;
		default:
			filterIndex = FILTER_PRISM_MODEL;
		}
		if (showSaveFileDialog(modelFilters, modelFilters[filterIndex]) != JFileChooser.APPROVE_OPTION) {
			return CANCEL;
		}
		// do save
		return handler.saveToFile(getChooserFile());
	}

	protected void a_refreshParseTree()
	{
		handler.requestParse(true);
	}

	protected void a_build()
	{
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Request build
		handler.forceBuild();
	}

	protected void a_exportBuildAs(int exportEntity, int exportType)
	{
		int res = JFileChooser.CANCEL_OPTION;

		// pop up dialog to select file
		switch (exportType) {
		case Prism.EXPORT_DOT:
			res = showSaveFileDialog(dotFilter, dotFilter[0]);
			break;
		case Prism.EXPORT_DOT_STATES:
			res = showSaveFileDialog(dotFilter, dotFilter[0]);
			break;
		case Prism.EXPORT_MATLAB:
			res = showSaveFileDialog(matlabFilter, matlabFilter[0]);
			break;
		default:
			switch (exportEntity) {
			case GUIMultiModelHandler.STATES_EXPORT:
				res = showSaveFileDialog(textFilter, textFilter[1]);
			case GUIMultiModelHandler.TRANS_EXPORT:
				res = showSaveFileDialog(textFilter, textFilter[2]);
			case GUIMultiModelHandler.LABELS_EXPORT:
				res = showSaveFileDialog(textFilter, textFilter[3]);
			default:
				res = showSaveFileDialog(textFilter, textFilter[0]);
			}
			break;
		}
		if (res != JFileChooser.APPROVE_OPTION)
			return;
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Do export...
		handler.export(exportEntity, exportType, getChooserFile());
	}

	protected void a_viewBuild(int exportEntity, int exportType)
	{
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Do view...
		handler.export(exportEntity, exportType, null);
	}

	// 	protected void a_viewStates()
	// 	{
	// 		handler.requestViewStates();
	// 	}

	protected void a_viewCurrentModelBuild()
	{
		handler.requestViewModel();
	}

	protected void a_exportSteadyState(int exportType)
	{
		// Pop up dialog to select file
		int res = JFileChooser.CANCEL_OPTION;
		switch (exportType) {
		case Prism.EXPORT_MATLAB:
			res = showSaveFileDialog(matlabFilter, matlabFilter[0]);
			break;
		case Prism.EXPORT_PLAIN:
		default:
			res = showSaveFileDialog(textFilter, textFilter[0]);
			break;
		}
		if (res != JFileChooser.APPROVE_OPTION)
			return;
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Do steady-state
		handler.computeSteadyState(exportType, getChooserFile());
	}

	protected void a_computeSteadyState()
	{
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Do steady-state
		handler.computeSteadyState(Prism.EXPORT_PLAIN, null);
	}

	protected void a_exportTransient(int exportType)
	{
		// Get time
		int result = GUITransientTime.requestTime(this.getGUI());
		if (result != GUITransientTime.OK)
			return;
		// Pop up dialog to select file
		int res = JFileChooser.CANCEL_OPTION;
		switch (exportType) {
		case Prism.EXPORT_MATLAB:
			res = showSaveFileDialog(matlabFilter, matlabFilter[0]);
			break;
		case Prism.EXPORT_PLAIN:
		default:
			res = showSaveFileDialog(textFilter, textFilter[0]);
			break;
		}
		if (res != JFileChooser.APPROVE_OPTION)
			return;
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Do transient
		handler.computeTransient(GUITransientTime.getTime(), exportType, getChooserFile());
	}

	protected void a_computeTransient()
	{
		// Reset warnings counter 
		getPrism().getMainLog().resetNumberOfWarnings();
		// Get time
		int result = GUITransientTime.requestTime(this.getGUI());
		if (result != GUITransientTime.OK)
			return;
		// Do transient
		handler.computeTransient(GUITransientTime.getTime(), Prism.EXPORT_PLAIN, null);
	}

	protected void a_convertToPrismTextModel()
	{
		int cont = doModificationCheck();
		if (cont == CONTINUE && (handler.getModelMode() != GUIMultiModelHandler.PRISM_MODE)) {
			String[] selection = { "Yes", "No", "Cancel" };
			int selectionNo = -1;
			selectionNo = optionPane("WARNING: This is a one way operation. Continue?", "Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
					selection, selection[0]);
			switch (selectionNo) {
			case 0:
				handler.convertViewToPRISM();
				break;
			}
		}
	}

	protected void a_convertToPrismGraphicModel()
	{
		int cont = doModificationCheck();
		if (cont == CONTINUE && (handler.getModelMode() != GUIMultiModelHandler.GRAPHIC_MODE)) {
			String[] selection = { "Yes", "No", "Cancel" };
			int selectionNo = -1;
			selectionNo = optionPane("WARNING: This is a one way operation. Continue?", "Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
					selection, selection[0]);
			switch (selectionNo) {
			case 0:
				handler.convertViewToGraphic();
				break;
			}
		}
	}

	protected void a_convertToPepaGraphicModel()
	{
		int cont = doModificationCheck();
		if (cont == CONTINUE && (handler.getModelMode() != GUIMultiModelHandler.PEPA_MODE)) {
			String[] selection = { "Yes", "No", "Cancel" };
			int selectionNo = -1;
			selectionNo = optionPane("WARNING: This is a one way operation. Continue?", "Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
					selection, selection[0]);
			switch (selectionNo) {
			case 0:
				handler.convertViewToPEPA();
				break;
			}
		}
	}

	private void setupActions()
	{
		newPRISMModel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_newPRISMModel();
			}
		};
		newPRISMModel.putValue(Action.LONG_DESCRIPTION, "Removes the current build, and loads a new model editor in PRISM Text Model mode.");
		//newPRISMModel.putValue(Action.SHORT_DESCRIPTION, "New PRISM Text Model");
		newPRISMModel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		newPRISMModel.putValue(Action.NAME, "PRISM model");
		newPRISMModel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFilePrism.png"));
		newPRISMModel.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		newGraphicModel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				//modified check do later
				a_newGraphicModel();
			}
		};
		newGraphicModel.putValue(Action.LONG_DESCRIPTION, "Removes the current build, and loads a new model editor in PRISM Graphic Model mode.");
		//newGraphicModel.putValue(Action.SHORT_DESCRIPTION, "New PRISM Graphic Model");
		newGraphicModel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_G));
		newGraphicModel.putValue(Action.NAME, "Graphical PRISM model");
		//newGraphicModel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileGraphic.png"));

		newPEPAModel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				//modified check do later
				a_newPEPAModel();
			}
		};
		newPEPAModel.putValue(Action.LONG_DESCRIPTION, "Removes the current build, and loads a new model editor in PEPA Text Model mode.");
		//newPEPAModel.putValue(Action.SHORT_DESCRIPTION, "New PEPA Text Model");
		newPEPAModel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
		newPEPAModel.putValue(Action.NAME, "PEPA model");
		newPEPAModel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFilePepa.png"));

		loadModel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				//do actions later
				//System.out.println("realised open action");
				a_openModel();

			}
		};
		loadModel
				.putValue(
						Action.LONG_DESCRIPTION,
						"Brings up a file loading dialogue and loads the file into the editor.  The editor will change mode according to the format of the file.  The loaded file is active for saving.");
		//loadModel.putValue(Action.SHORT_DESCRIPTION, "Open Model");
		loadModel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		loadModel.putValue(Action.NAME, "Open model...");
		//loadModel.putValue(Action.ACTION_COMMAND_KEY,
		loadModel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallOpen.png"));
		loadModel.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		reloadModel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_reloadModel();
			}
		};
		reloadModel.putValue(Action.LONG_DESCRIPTION, "Reloads the current active file.");
		//reloadModel.putValue(Action.SHORT_DESCRIPTION, "Reload Model");
		reloadModel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		reloadModel.putValue(Action.NAME, "Reload model");
		//loadModel.putValue(Action.ACTION_COMMAND_KEY,
		reloadModel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallReload.png"));
		reloadModel.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		saveModel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_saveModel();
			}
		};
		saveModel.putValue(Action.LONG_DESCRIPTION,
				"Brings up a file saving dialogue and saves the current text editor to the active file or to a new file.  The saved file becomes active");
		//saveModel.putValue(Action.SHORT_DESCRIPTION, "Save Model");
		saveModel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		saveModel.putValue(Action.NAME, "Save model");
		saveModel.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		//saveModel.putValue(Action.ACTION_COMMAND_KEY,
		saveModel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSave.png"));

		saveAsModel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_saveModelAs();
			}
		};
		saveAsModel.putValue(Action.LONG_DESCRIPTION,
				"Brings up a file saving dialogue and saves the current text editor to a new file.  The saved file becomes active");
		//saveAsModel.putValue(Action.SHORT_DESCRIPTION, "Save Model As...");
		saveAsModel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
		saveAsModel.putValue(Action.NAME, "Save model as...");
		//saveAsModel.putValue(Action.ACTION_COMMAND_KEY,
		saveAsModel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSaveAs.png"));

		parseModel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_refreshParseTree();
			}
		};
		parseModel.putValue(Action.LONG_DESCRIPTION, "Forces a parse of the model in the editor.  The parsed description is shown in the model tree.");
		//parseModel.putValue(Action.SHORT_DESCRIPTION, "Parse Model");
		parseModel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		parseModel.putValue(Action.NAME, "Parse model");
		parseModel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallParse.png"));
		parseModel.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
		buildModel = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_build();
			}
		};
		buildModel.putValue(Action.LONG_DESCRIPTION, "Builds the model that has been parsed.");
		//buildModel.putValue(Action.SHORT_DESCRIPTION, "Build Model");
		buildModel.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_B));
		buildModel.putValue(Action.NAME, "Build model");
		buildModel.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallBuild.png"));
		buildModel.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));

		exportStatesPlain = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.STATES_EXPORT, Prism.EXPORT_PLAIN);
			}
		};
		exportStatesPlain.putValue(Action.LONG_DESCRIPTION, "Exports the reachable states to a plain text file");
		exportStatesPlain.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		exportStatesPlain.putValue(Action.NAME, "Plain text file");
		exportStatesPlain.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportStatesMatlab = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.STATES_EXPORT, Prism.EXPORT_MATLAB);
			}
		};
		exportStatesMatlab.putValue(Action.LONG_DESCRIPTION, "Exports the reachable states to a Matlab file");
		exportStatesMatlab.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportStatesMatlab.putValue(Action.NAME, "Matlab file");
		exportStatesMatlab.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileMatlab.png"));

		exportTransPlain = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.TRANS_EXPORT, Prism.EXPORT_PLAIN);
			}
		};
		exportTransPlain.putValue(Action.LONG_DESCRIPTION, "Exports the transition matrix to a plain text file");
		exportTransPlain.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		exportTransPlain.putValue(Action.NAME, "Plain text file");
		exportTransPlain.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportTransMatlab = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.TRANS_EXPORT, Prism.EXPORT_MATLAB);
			}
		};
		exportTransMatlab.putValue(Action.LONG_DESCRIPTION, "Exports the transition matrix to a Matlab file");
		exportTransMatlab.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportTransMatlab.putValue(Action.NAME, "Matlab file");
		exportTransMatlab.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileMatlab.png"));

		exportTransDot = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.TRANS_EXPORT, Prism.EXPORT_DOT);
			}
		};
		exportTransDot.putValue(Action.LONG_DESCRIPTION, "Exports the transition matrix graph to a Dot file");
		exportTransDot.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		exportTransDot.putValue(Action.NAME, "Dot file");
		exportTransDot.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileDot.png"));

		exportTransDotStates = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.TRANS_EXPORT, Prism.EXPORT_DOT_STATES);
			}
		};
		exportTransDotStates.putValue(Action.LONG_DESCRIPTION, "Exports the transition matrix graph to a Dot file (with states)");
		exportTransDotStates.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		exportTransDotStates.putValue(Action.NAME, "Dot file (with states)");
		exportTransDotStates.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileDot.png"));

		exportTransMRMC = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.TRANS_EXPORT, Prism.EXPORT_MRMC);
			}
		};
		exportTransMRMC.putValue(Action.LONG_DESCRIPTION, "Exports the transition matrix to a MRMC file");
		exportTransMRMC.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		exportTransMRMC.putValue(Action.NAME, "MRMC file");
		exportTransMRMC.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportStateRewardsPlain = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.STATE_REWARDS_EXPORT, Prism.EXPORT_PLAIN);
			}
		};
		exportStateRewardsPlain.putValue(Action.LONG_DESCRIPTION, "Exports the state rewards vector to a plain text file");
		exportStateRewardsPlain.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		exportStateRewardsPlain.putValue(Action.NAME, "Plain text file");
		exportStateRewardsPlain.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportStateRewardsMatlab = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.STATE_REWARDS_EXPORT, Prism.EXPORT_MATLAB);
			}
		};
		exportStateRewardsMatlab.putValue(Action.LONG_DESCRIPTION, "Exports the state rewards vector to a Matlab file");
		exportStateRewardsMatlab.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportStateRewardsMatlab.putValue(Action.NAME, "Matlab file");
		exportStateRewardsMatlab.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileMatlab.png"));

		exportStateRewardsMRMC = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.STATE_REWARDS_EXPORT, Prism.EXPORT_MRMC);
			}
		};
		exportStateRewardsMRMC.putValue(Action.LONG_DESCRIPTION, "Exports the state rewards vector graph to a MRMC file");
		exportStateRewardsMRMC.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		exportStateRewardsMRMC.putValue(Action.NAME, "MRMC file");
		exportStateRewardsMRMC.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportTransRewardsPlain = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.TRANS_REWARDS_EXPORT, Prism.EXPORT_PLAIN);
			}
		};
		exportTransRewardsPlain.putValue(Action.LONG_DESCRIPTION, "Exports the transition rewards matrix to a plain text file");
		exportTransRewardsPlain.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		exportTransRewardsPlain.putValue(Action.NAME, "Plain text file");
		exportTransRewardsPlain.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportTransRewardsMatlab = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.TRANS_REWARDS_EXPORT, Prism.EXPORT_MATLAB);
			}
		};
		exportTransRewardsMatlab.putValue(Action.LONG_DESCRIPTION, "Exports the transition rewards matrix to a Matlab file");
		exportTransRewardsMatlab.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportTransRewardsMatlab.putValue(Action.NAME, "Matlab file");
		exportTransRewardsMatlab.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileMatlab.png"));

		exportTransRewardsMRMC = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.TRANS_REWARDS_EXPORT, Prism.EXPORT_MRMC);
			}
		};
		exportTransRewardsMRMC.putValue(Action.LONG_DESCRIPTION, "Exports the transition rewards matrix to a MRMC file");
		exportTransRewardsMRMC.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		exportTransRewardsMRMC.putValue(Action.NAME, "MRMC file");
		exportTransRewardsMRMC.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportLabelsPlain = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.LABELS_EXPORT, Prism.EXPORT_PLAIN);
			}
		};
		exportLabelsPlain.putValue(Action.LONG_DESCRIPTION, "Exports the model's labels and their satisfying states to a plain text file");
		exportLabelsPlain.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		exportLabelsPlain.putValue(Action.NAME, "Plain text file");
		exportLabelsPlain.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportLabelsMatlab = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportBuildAs(GUIMultiModelHandler.LABELS_EXPORT, Prism.EXPORT_MATLAB);
			}
		};
		exportLabelsMatlab.putValue(Action.LONG_DESCRIPTION, "Exports the model's labels and their satisfying states to a Matlab file");
		exportLabelsMatlab.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportLabelsMatlab.putValue(Action.NAME, "Matlab file");
		exportLabelsMatlab.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileMatlab.png"));

		computeSS = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_computeSteadyState();
			}
		};
		computeSS.putValue(Action.LONG_DESCRIPTION, "Computes steady-state probabilities for the model");
		//computeSS.putValue(Action.SHORT_DESCRIPTION, "Compute steady-state probabilities");
		computeSS.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		computeSS.putValue(Action.NAME, "Steady-state probabilities");
		computeSS.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSteadyState.png"));
		computeSS.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));

		computeTr = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_computeTransient();
			}
		};
		computeTr.putValue(Action.LONG_DESCRIPTION, "Computes transient probabilities for the model");
		//computeTr.putValue(Action.SHORT_DESCRIPTION, "Compute transient probabilities");
		computeTr.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
		computeTr.putValue(Action.NAME, "Transient probabilities");
		computeTr.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallClockAnim1.png"));
		computeTr.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.CTRL_DOWN_MASK));

		exportSSPlain = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportSteadyState(Prism.EXPORT_PLAIN);
			}
		};
		exportSSPlain.putValue(Action.LONG_DESCRIPTION, "Exports the steady-state probabilities to a plain text file");
		exportSSPlain.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		exportSSPlain.putValue(Action.NAME, "Plain text file");
		exportSSPlain.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportSSMatlab = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportSteadyState(Prism.EXPORT_MATLAB);
			}
		};
		exportSSMatlab.putValue(Action.LONG_DESCRIPTION, "Exports the steady-state probabilities to a Matlab file");
		exportSSMatlab.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportSSMatlab.putValue(Action.NAME, "Matlab file");
		exportSSMatlab.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileMatlab.png"));

		exportTrPlain = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportTransient(Prism.EXPORT_PLAIN);
			}
		};
		exportTrPlain.putValue(Action.LONG_DESCRIPTION, "Exports the transient probabilities to a plain text file");
		exportTrPlain.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		exportTrPlain.putValue(Action.NAME, "Plain text file");
		exportTrPlain.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileText.png"));

		exportTrMatlab = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportTransient(Prism.EXPORT_MATLAB);
			}
		};
		exportTrMatlab.putValue(Action.LONG_DESCRIPTION, "Exports the transient probabilities to a Matlab file");
		exportTrMatlab.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		exportTrMatlab.putValue(Action.NAME, "Matlab file");
		exportTrMatlab.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFileMatlab.png"));

		viewStates = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_viewBuild(GUIMultiModelHandler.STATES_EXPORT, Prism.EXPORT_PLAIN);
			}
		};
		viewStates.putValue(Action.LONG_DESCRIPTION, "Print the reachable states to the log");
		viewStates.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		viewStates.putValue(Action.NAME, "States");
		viewStates.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallStates.png"));

		viewTrans = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_viewBuild(GUIMultiModelHandler.TRANS_EXPORT, Prism.EXPORT_PLAIN);
			}
		};
		viewTrans.putValue(Action.LONG_DESCRIPTION, "Print the transition matrix to the log");
		viewTrans.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
		viewTrans.putValue(Action.NAME, "Transition matrix");
		viewTrans.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallMatrix.png"));

		viewStateRewards = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_viewBuild(GUIMultiModelHandler.STATE_REWARDS_EXPORT, Prism.EXPORT_PLAIN);
			}
		};
		viewStateRewards.putValue(Action.LONG_DESCRIPTION, "Print the state rewards to the log");
		viewStateRewards.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		viewStateRewards.putValue(Action.NAME, "State rewards");
		viewStateRewards.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallStates.png"));

		viewTransRewards = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_viewBuild(GUIMultiModelHandler.TRANS_REWARDS_EXPORT, Prism.EXPORT_PLAIN);
			}
		};
		viewTransRewards.putValue(Action.LONG_DESCRIPTION, "Print the transition rewards to the log");
		viewTransRewards.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
		viewTransRewards.putValue(Action.NAME, "Transition rewards");
		viewTransRewards.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallMatrix.png"));

		viewLabels = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_viewBuild(GUIMultiModelHandler.LABELS_EXPORT, Prism.EXPORT_PLAIN);
			}
		};
		viewLabels.putValue(Action.LONG_DESCRIPTION, "Print the labels and satisfying states to the log");
		viewLabels.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
		viewLabels.putValue(Action.NAME, "Labels");
		viewLabels.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallStates.png"));

		viewPrismCode = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_viewCurrentModelBuild();
			}
		};
		viewPrismCode.putValue(Action.LONG_DESCRIPTION, "This shows the parsed model in a text editor.");
		viewPrismCode.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_V));
		viewPrismCode.putValue(Action.NAME, "Parsed PRISM model");
		viewPrismCode.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFilePrism.png"));
	}

	//Required to be a GUIPlugin:

	public boolean displaysTab()
	{
		return true;
	}

	public JMenu getMenu()
	{
		return modelMenu;
	}

	public String getTabText()
	{
		return "Model";
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
		if (e instanceof userinterface.properties.GUIPropertiesEvent) {
			if (e.getID() == userinterface.properties.GUIPropertiesEvent.REQUEST_MODEL_PARSE) {
				handler.requestParse(false);
			}
		} else if (e instanceof GUIClipboardEvent && super.getGUI().getFocussedPlugin() == this) {
			GUIClipboardEvent ce = (GUIClipboardEvent) e;
			if (ce.getComponent() == this) {
				int id = ce.getID();
				if (id == GUIClipboardEvent.CUT)
					handler.cut();
				else if (id == GUIClipboardEvent.COPY)
					handler.copy();
				else if (id == GUIClipboardEvent.PASTE)
					handler.paste();
				else if (id == GUIClipboardEvent.DELETE)
					handler.delete();
				else if (id == GUIClipboardEvent.SELECT_ALL)
					handler.selectAll();
				else if (id == GUIClipboardEvent.UNDO)
					handler.undo();
				else if (id == GUIClipboardEvent.REDO)
					handler.redo();

			}
		} else if (e instanceof GUIComputationEvent) {
			if (e.getID() == GUIComputationEvent.COMPUTATION_START) {
				computing = true;
				doEnables();
				selectionChangeHandler.notifyListeners(new GUIEvent(1));
			} else if (e.getID() == GUIComputationEvent.COMPUTATION_DONE) {
				computing = false;
				doEnables();
				selectionChangeHandler.notifyListeners(new GUIEvent(1));
			} else if (e.getID() == GUIComputationEvent.COMPUTATION_ERROR) {
				computing = false;
				doEnables();
				selectionChangeHandler.notifyListeners(new GUIEvent(1));
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

	private JMenu initExportMenu()
	{
		JMenu exportMenu = new JMenu("Export");
		exportMenu.setMnemonic('E');
		exportMenu.setIcon(GUIPrism.getIconFromImage("smallExport.png"));
		exportStatesMenu = new JMenu("States");
		exportStatesMenu.setMnemonic('S');
		exportStatesMenu.setIcon(GUIPrism.getIconFromImage("smallStates.png"));
		exportStatesMenu.add(exportStatesPlain);
		exportStatesMenu.add(exportStatesMatlab);
		exportMenu.add(exportStatesMenu);
		exportTransMenu = new JMenu("Transition matrix");
		exportTransMenu.setMnemonic('T');
		exportTransMenu.setIcon(GUIPrism.getIconFromImage("smallMatrix.png"));
		exportTransMenu.add(exportTransPlain);
		exportTransMenu.add(exportTransMatlab);
		exportTransMenu.add(exportTransDot);
		exportTransMenu.add(exportTransDotStates);
		exportTransMenu.add(exportTransMRMC);
		exportMenu.add(exportTransMenu);
		exportStateRewardsMenu = new JMenu("State rewards");
		exportStateRewardsMenu.setMnemonic('R');
		exportStateRewardsMenu.setIcon(GUIPrism.getIconFromImage("smallStates.png"));
		exportStateRewardsMenu.add(exportStateRewardsPlain);
		exportStateRewardsMenu.add(exportStateRewardsMatlab);
		exportStateRewardsMenu.add(exportStateRewardsMRMC);
		exportMenu.add(exportStateRewardsMenu);
		exportTransRewardsMenu = new JMenu("Transition rewards");
		exportTransRewardsMenu.setMnemonic('E');
		exportTransRewardsMenu.setIcon(GUIPrism.getIconFromImage("smallMatrix.png"));
		exportTransRewardsMenu.add(exportTransRewardsPlain);
		exportTransRewardsMenu.add(exportTransRewardsMatlab);
		exportTransRewardsMenu.add(exportTransRewardsMRMC);
		exportMenu.add(exportTransRewardsMenu);
		exportLabelsMenu = new JMenu("Labels");
		exportLabelsMenu.setMnemonic('L');
		exportLabelsMenu.setIcon(GUIPrism.getIconFromImage("smallStates.png"));
		exportLabelsMenu.add(exportLabelsPlain);
		exportLabelsMenu.add(exportLabelsMatlab);
		exportMenu.add(exportLabelsMenu);
		return exportMenu;
	}

	private JMenu initViewMenu()
	{
		JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic('V');
		viewMenu.setIcon(GUIPrism.getIconFromImage("smallView.png"));
		viewMenu.add(viewStates);
		viewMenu.add(viewTrans);
		viewMenu.add(viewStateRewards);
		viewMenu.add(viewTransRewards);
		viewMenu.add(viewLabels);
		viewMenu.add(viewPrismCode);
		return viewMenu;
	}

	private JMenu initComputeMenu()
	{
		JMenu computeMenu = new JMenu("Compute");
		computeMenu.setMnemonic('C');
		computeMenu.setIcon(GUIPrism.getIconFromImage("smallCompute.png"));
		computeMenu.add(computeSS);
		computeMenu.add(computeTr);
		return computeMenu;
	}

	private JMenu initComputeExportMenu()
	{
		JMenu computeExportMenu = new JMenu("Compute/export");
		computeExportMenu.setMnemonic('X');
		computeExportMenu.setIcon(GUIPrism.getIconFromImage("smallCompute.png"));
		exportSSMenu = new JMenu("Steady-state probabilities");
		exportSSMenu.setMnemonic('S');
		exportSSMenu.setIcon(GUIPrism.getIconFromImage("smallSteadyState.png"));
		exportSSMenu.add(exportSSPlain);
		exportSSMenu.add(exportSSMatlab);
		computeExportMenu.add(exportSSMenu);
		exportTrMenu = new JMenu("Transient probabilities");
		exportTrMenu.setMnemonic('A');
		exportTrMenu.setIcon(GUIPrism.getIconFromImage("smallClockAnim1.png"));
		exportTrMenu.add(exportTrPlain);
		exportTrMenu.add(exportTrMatlab);
		computeExportMenu.add(exportTrMenu);
		return computeExportMenu;
	}

	private void initComponents()
	{
		setupActions();

		modelMenu = new JMenu("Model");
		exportMenu = initExportMenu();
		viewMenu = initViewMenu();
		computeMenu = initComputeMenu();
		computeExportMenu = initComputeExportMenu();

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
			handler = new GUIMultiModelHandler(this);
			//topPanel.add(progress, BorderLayout.WEST);
			topPanel.add(fileTextField, BorderLayout.NORTH);
			topPanel.add(handler, BorderLayout.CENTER);
		}

		newMenu = new JMenu("New");
		newMenu.setMnemonic('N');
		newMenu.setIcon(GUIPrism.getIconFromImage("smallNew.png"));
		newMenu.add(newPRISMModel);
		if (GM_ENABLED)
			newMenu.add(newGraphicModel);
		newMenu.add(newPEPAModel);
		modelMenu.add(newMenu);
		modelMenu.add(new JSeparator());
		modelMenu.add(loadModel);
		modelMenu.add(reloadModel);
		modelMenu.add(new JSeparator());
		modelMenu.add(saveModel);
		modelMenu.add(saveAsModel);
		modelMenu.add(new JSeparator());
		modelMenu.setMnemonic(KeyEvent.VK_M);
		modelMenu.add(parseModel);
		modelMenu.add(buildModel);
		modelMenu.add(new JSeparator());

		modelMenu.add(exportMenu);

		modelMenu.add(viewMenu);

		modelMenu.add(computeMenu);

		modelMenu.add(computeExportMenu);

		popup = new JPopupMenu();
		{
			popup.add(parseModel);
			popup.add(buildModel);
			popup.add(viewPrismCode);
		}

		modelFilters = new GUIPrismFileFilter[GM_ENABLED ? 3 : 2];
		modelFilters[FILTER_PRISM_MODEL] = new GUIPrismFileFilter("PRISM models (*.prism, *.pm, *.nm, *.sm)");
		modelFilters[FILTER_PRISM_MODEL].addExtension("prism");
		modelFilters[FILTER_PRISM_MODEL].addExtension("pm");
		modelFilters[FILTER_PRISM_MODEL].addExtension("nm");
		modelFilters[FILTER_PRISM_MODEL].addExtension("sm");
		if (GM_ENABLED)
			modelFilters[FILTER_GRAPHIC_MODEL] = new GUIPrismFileFilter("Graphical PRISM models (*.gm)");
		if (GM_ENABLED)
			modelFilters[FILTER_GRAPHIC_MODEL].addExtension("gm");
		modelFilters[FILTER_PEPA_MODEL] = new GUIPrismFileFilter("PEPA models (*.pepa)");
		modelFilters[FILTER_PEPA_MODEL].addExtension("pepa");
		textFilter = new GUIPrismFileFilter[4];
		textFilter[0] = new GUIPrismFileFilter("Plain text files (*.txt)");
		textFilter[0].addExtension("txt");
		textFilter[1] = new GUIPrismFileFilter("State list files (*.sta)");
		textFilter[1].addExtension("sta");
		textFilter[2] = new GUIPrismFileFilter("Transition matrix files (*.tra)");
		textFilter[2].addExtension("tra");
		textFilter[3] = new GUIPrismFileFilter("Label files (*.lab)");
		textFilter[3].addExtension("lab");
		matlabFilter = new GUIPrismFileFilter[1];
		matlabFilter[0] = new GUIPrismFileFilter("Matlab files (*.m)");
		matlabFilter[0].addExtension("m");
		dotFilter = new GUIPrismFileFilter[1];
		dotFilter[0] = new GUIPrismFileFilter("Dot files (*.dot)");
		dotFilter[0].addExtension("dot");

		setLayout(new BorderLayout());
		add(topPanel, BorderLayout.CENTER);

		doEnables();
	}

	public OptionsPanel getOptions()
	{
		return null;
	}

	public void notifySettings(PrismSettings settings)
	{
		//System.out.println("model notifySettings called");
		handler.notifySettings(settings);

		repaint();
	}

	@Override
	public GUIUndoManager getUndoManager()
	{
		return handler.getUndoManager();
	}

	@Override
	public boolean canDoClipBoardAction(Action action)
	{
		if (computing)
			return false;

		return handler.canDoClipBoardAction(action);
	}

	public AbstractAction getParseModel()
	{
		return parseModel;
	}

	public AbstractAction getBuildModel()
	{
		return buildModel;
	}

	public JMenu getViewMenu()
	{
		return initViewMenu();
	}

	public JMenu getExportMenu()
	{
		return initExportMenu();
	}

	public JMenu getComputeMenu()
	{
		return initComputeMenu();
	}

	public JMenu getComputeExportMenu()
	{
		return initComputeExportMenu();
	}
}
