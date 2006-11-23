//==============================================================================
//
//	Copyright (c) 2004-2005, Andrew Hinton
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
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import simulator.*;
import simulator.networking.*;
import parser.*;
import prism.*;
import userinterface.*;
import userinterface.util.*;
import userinterface.model.*;
import userinterface.properties.*;
import userinterface.simulator.networking.*;
import java.awt.event.*;
import javax.swing.event.*;

/**
 *
 * @author  Andrew Hinton
 */
public class GUISimulator extends GUIPlugin implements MouseListener, ListSelectionListener, PrismSettingsListener
{
	
	//ATTRIBUTES
	private GUIPrism gui; //reference to the gui
	private GUIMultiProperties guiProp; //reference to the properties information
	private SimulatorEngine engine;
	private GUIPrismFileFilter[] txtFilter;
	private JPopupMenu pathPopup;
	
	//Current State
	private ModulesFile mf;
	private boolean pathActive;
	private boolean engineBuilt;
	
	private GUISimulator.PathTableModel pathTableModel;
	private GUISimulator.UpdateTableModel updateTableModel;
	
	private Values lastConstants, lastPropertyConstants, lastInitialState;
	private boolean computing;
	
	private boolean displayStyleFast;
	private boolean displayPathLoops;
	
	//Actions
	private Action backtrackToHere, removeToHere, newPath, resetPath, exportPath, configureView;
	
	/** Creates a new instance of GUISimulator */
	public GUISimulator(GUIPrism gui)
	{
		super(gui, true);
		pathTableModel = new PathTableModel(this);
		updateTableModel = new UpdateTableModel();
		
		this.gui = gui;
		this.engine = gui.getPrism().getSimulator();
		initComponents();
		initPopups();
		
		newPathButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallNewPath.gif")));
		resetPathButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallResetPath.gif")));
		exportPathButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallExport.gif")));
		
		autoUpdateButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallAutomaticUpdate.gif")));
		manualUpdateField.setIcon(new ImageIcon(this.getClass().getResource("/images/smallManualUpdate.gif")));
		
		backtrackButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallBacktrack.gif")));
		removePrecedingButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallRemovePreceding.gif")));
		
		pathTable.getSelectionModel().addListSelectionListener(this);
		
		pathTable.addMouseListener(this);
		pathTable.getTableHeader().addMouseListener(this);
		pathTable.getParent().addMouseListener(this);
		
		pathTable.getTableHeader().setReorderingAllowed(true);
		
		pathTable.addComponentListener(new ComponentAdapter()
		{			
			public void componentResized(ComponentEvent e)	{
				sortOutColumnSizes();
			}
		});
		
		
		pathActive = false;
		engineBuilt = false;
		doEnables();
		
		//verifyAllPropertiesAtOnce = true;
		//options = new GUISimulatorOptions(this);
		
		currentUpdatesTable.setModel(updateTableModel);		
		currentUpdatesTable.addMouseListener(new MouseAdapter() 
		{			
			public void mouseClicked(MouseEvent e) 
			{
				if (e.getClickCount() == 2 && currentUpdatesTable.isEnabled())
				{
					a_manualUpdate();
				}
			}
		});
		
		pathTable.setModel(pathTableModel);
		
		lastConstants = null;
		
		lastPropertyConstants = null;
		
		lastInitialState = null;
		
		tableScroll.setRowHeaderView(((GUISimulatorPathTable)pathTable).getPathRowHeader());
		updatesScroll.setRowHeaderView(((GUISimulatorUpdatesTable)currentUpdatesTable).getUpdateRowHeader());
		
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
		totalTimeLabel.setText("0.0");
		pathLengthLabel.setText("0");
		totalRewardLabel.setText("");
		
		txtFilter = new GUIPrismFileFilter[1];
		txtFilter[0] = new GUIPrismFileFilter("Text files (*.txt)");
		txtFilter[0].addExtension("txt");
		
		displayStyleFast = true;
		displayPathLoops = true;
		
		GUINetworkEditor netEdit = new GUINetworkEditor(getGUI(), new SimulatorNetworkHandler()); 
		
		getPrism().getSettings().setFileSelector(PrismSettings.SIMULATOR_NETWORK_FILE, netEdit );
		
		autoTimeCheck.setSelected(true);
	}
	
	public void setGUIProb(GUIMultiProperties guiProp)
	{
		this.guiProp = guiProp;
	}
	
	public String getTotalRewardLabelString()
	{
		int i, n;
		String s;
		n = mf.getNumRewardStructs();
		s = "<html>";
		for (i = 0; i < n; i++) {
			s += engine.getTotalPathReward(i);
			if (i<n-1) s+= ",<br>";
		}
		s += "</html>";
		return s;
	}
	
	public String getTotalStateRewardLabelString()
	{
		int i, n;
		String s;
		n = mf.getNumRewardStructs();
		s = "<html>";
		for (i = 0; i < n; i++) {
			s += engine.getTotalStateReward(i);
			if (i<n-1) s+= ",<br>";
		}
		s += "</html>";
		return s;
	}
	
	public String getTotalTransitionRewardLabelString()
	{
		int i, n;
		String s;
		n = mf.getNumRewardStructs();
		s = "<html>";
		for (i = 0; i < n; i++) {
			s += engine.getTotalTransitionReward(i);
			if (i<n-1) s+= ",<br>";
		}
		s += "</html>";
		return s;
	}
	
	public void a_clearModel()
	{
		try
		{
			//System.ouy.println("guisimulator 1");
			if(engineBuilt)
			{
				//System.ouy.println("guisimulator 2");
				engine.deallocateEngine();
				engineBuilt = false;
				//System.ouy.println("guisimulator 3");
			}
			//System.ouy.println("guisimulator 5");
			pathActive = false;
			pathTableModel.restartPathTable();
			//System.ouy.println("guisimulator 6");
			
			
			((GUISimLabelFormulaeList)stateLabelList).clearLabels();
			((GUISimPathFormulaeList)pathFormulaeList).clearList();
		}
		catch(SimulatorException e)
		{
			message("Simulator error: ", e.getMessage());
		}
	}
	
	public void a_loadModulesFile(ModulesFile mf)
	{
		this.mf = mf;
		try
		{
			//System.ouy.println("guisimulator 7");
			if(engineBuilt)
			{
				//System.ouy.println("guisimulator 8");
				engine.deallocateEngine();
				//System.ouy.println("guisimulator 9");
				engineBuilt = false;
				//System.ouy.println("guisimulator 10");
			}
			//System.ouy.println("guisimulator 11");
			pathActive = false;
			pathTableModel.restartPathTable();
			//System.ouy.println("guisimulator 12");
			updateTableModel.restartUpdatesTable();
			//System.ouy.println("guisimulator 13");
			//engineBuilt=true;
			
			
			//pathActive = false;
			
			if(mf != null)
				modelTypeLabel.setText(""+mf.getTypeString());
			else
				modelTypeLabel.setText("Unknown");
			
			((GUISimLabelFormulaeList)stateLabelList).clearLabels();
			((GUISimPathFormulaeList)pathFormulaeList).clearList();
			stateLabelList.repaint();
			pathFormulaeList.repaint();
			doEnables();
		}
		catch(SimulatorException e)
		{
			message("Simulator error: ", e.getMessage());
		}
	}
	
	
	public void a_newPath()
	{
		Values initialState;
		try
		{
			//pathTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			
			// get properties constants/labels
			PropertiesFile pf;
			try
			{
				pf = getPrism().parsePropertiesString(mf, guiProp.getConstantsString().toString()+guiProp.getLabelString());
			}
			catch(ParseException e)
			{
				// ignore properties if they don't parse
				pf = null; //if any problems
			}
			
			// if necessary, get values for undefined constants from user
			UndefinedConstants uCon = new UndefinedConstants(mf, pf);
			if(uCon.getMFNumUndefined()+uCon.getPFNumUndefined() > 0)
			{
				int result = GUIConstantsPicker.defineConstantsWithDialog(gui, uCon, lastConstants, lastPropertyConstants);
				if(result != GUIConstantsPicker.VALUES_DONE)
					return;
			}
			// remember constant values for next time
			lastConstants = uCon.getMFConstantValues();
			lastPropertyConstants = uCon.getPFConstantValues();
			// store constants
			mf.setUndefinedConstants(lastConstants);
			pf.setUndefinedConstants(lastPropertyConstants);
			
			// now determine the initial state for simulation
			
			// first select a default state (the initial state) which may or may not end up being used
			Values defaultInitialState = new Values();
			defaultInitialState.addValues(mf.getInitialValues());
			
			// if required, we prompt the user for an initial state
			if(isAskForInitialState())
			{
				// we will pass in lastInitialState to the dialog
				// but first make sure it is ok, i.e.
				// (a) it is non-null
				if(lastInitialState == null) {
					lastInitialState = defaultInitialState;
				}
				// (b) var names/types are correct
				else
				{
					boolean match = true;
					int i, n;
					n = defaultInitialState.getNumValues();
					for(i = 0; i < n; i++)
					{
						if(!lastInitialState.contains(defaultInitialState.getName(i)))
						{
							match = false;
							break;
						}
						else
						{
							int index = lastInitialState.getIndexOf(defaultInitialState.getName(i));
							if(lastInitialState.getType(index) != defaultInitialState.getType(i))
							{
								match = false;
								break;
							}
						}
					}
					if(!match) // if there's a problem, just use the default
					{
						lastInitialState = defaultInitialState;
					}
				}
				
				initialState = GUIInitialStatePicker.defineInitalValuesWithDialog(getGUI(), lastInitialState, mf);
				// if user clicked cancel from dialog...
				if (initialState == null) {
					return;
				}
			}
			// if we don't need to ask the user at all, just use the default
			else
			{
				initialState = defaultInitialState;
			}
			
			displayPathLoops = true;
			engine.startNewPath(mf, pf, initialState);
			//engine.setPropertyConstants(lastPropertyConstants);
			
			engineBuilt = true;
			pathActive = true;
			
			totalTimeLabel.setText(""+engine.getTotalPathTime());
			pathLengthLabel.setText(""+(engine.getPathSize()-1));
			totalRewardLabel.setText(getTotalRewardLabelString());
			stateRewardsLabel.setText(getTotalStateRewardLabelString());
			transitionRewardsLabel.setText(getTotalTransitionRewardLabelString());
			definedConstantsLabel.setText(uCon.getDefinedConstantsString());
			
			doEnables();
			pathTableModel.restartPathTable();
			updateTableModel.restartUpdatesTable();
			
			pathTable.getSelectionModel().setSelectionInterval(pathTable.getRowCount()-1, pathTable.getRowCount()-1);
			
			repopulateFormulae();
			stateLabelList.repaint();
			pathFormulaeList.repaint();
					
			// store inital state for next time
			lastInitialState = initialState;
			
			if (getPrism().getSettings().getBoolean(PrismSettings.SIMULATOR_NEW_PATH_ASK_VIEW))
			{
				new GUIViewDialog(gui, pathTableModel, mf);
			}
			
			
		}
		catch(PrismException e)
		{
			this.error(e.getMessage());
		}
		catch(SimulatorException e)
		{
			this.error(e.getMessage());
		}
	}
	
	public void a_autoStep()
	{
		try
		{
			int noSteps = Integer.parseInt(autoUpdateField.getText());
			if(noSteps <= 0) throw new SimulatorException("The No. Steps parameter is invalid.\nIt must be a positive integer");
			
			if (displayPathLoops && pathTableModel.isPathLooping()) {
				if (questionYesNo("A loop in the path has been detected. Do you wish to disable loop detection and extend the path?") == 0) {
					displayPathLoops = false;
					pathTable.repaint();
				}
				else return;
			}
			
			setComputing(true);
			if(isOldUpdate())
			{
				engine.finishedWithOldUpdates();
			}
			engine.automaticChoices(noSteps, displayPathLoops);
			
			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();
			pathTable.scrollRectToVisible(new Rectangle(0, (int)pathTable.getPreferredSize().getHeight() - 10, (int)pathTable.getPreferredSize().getWidth(), (int)pathTable.getPreferredSize().getHeight()) );
			
			totalTimeLabel.setText(""+engine.getTotalPathTime());
			pathLengthLabel.setText(""+(engine.getPathSize()-1));
			totalRewardLabel.setText(getTotalRewardLabelString());
			stateRewardsLabel.setText(getTotalStateRewardLabelString());
			transitionRewardsLabel.setText(getTotalTransitionRewardLabelString());
			
			stateLabelList.repaint();
			pathFormulaeList.repaint();
			setComputing(false);
		}
		catch(NumberFormatException e)
		{
			this.error("The No. Steps parameter is invalid.\nIt must be a positive integer");
		}
		catch(SimulatorException e)
		{
			this.error(e.getMessage());
		}
	}
	
	
	public void a_backTrack()
	{
		try
		{
			int toStep = Integer.parseInt(backTrackStepField.getText());
			if(toStep < 0 || toStep >= pathTableModel.getRowCount()) throw new SimulatorException("The Backtrack \'to step\' parameter is invalid.\nIt must be a positive integer");
			a_backTrack(toStep);
			
		}
		catch(NumberFormatException e)
		{
			this.error("The Backtrack \'to step\' parameter is invalid.\nIt must be a positive integer representing a step in the path table");
		}
		catch(SimulatorException e)
		{
			this.error(e.getMessage());
		}
	}
	
	public void a_backTrack(int step) throws SimulatorException
	{
		setComputing(true);
		
		engine.backtrack(step);
		
		pathTableModel.updatePathTable();
		updateTableModel.updateUpdatesTable();
		
		totalTimeLabel.setText(""+engine.getTotalPathTime());
		pathLengthLabel.setText(""+(engine.getPathSize()-1));
		totalRewardLabel.setText(getTotalRewardLabelString());
		stateRewardsLabel.setText(getTotalStateRewardLabelString());
		transitionRewardsLabel.setText(getTotalTransitionRewardLabelString());
		stateLabelList.repaint();
		pathFormulaeList.repaint();
		setComputing(false);
	}
	
	public void a_restartPath()
	{
		try
		{
			setComputing(true);
			
			engine.backtrack(0);
			
			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();
			
			totalTimeLabel.setText(""+engine.getTotalPathTime());
			pathLengthLabel.setText(""+(engine.getPathSize()-1));
			totalRewardLabel.setText(getTotalRewardLabelString());
			stateRewardsLabel.setText(getTotalStateRewardLabelString());
			transitionRewardsLabel.setText(getTotalTransitionRewardLabelString());
			stateLabelList.repaint();
			pathFormulaeList.repaint();
			setComputing(false);
		}
		catch(SimulatorException e)
		{
			this.error(e.getMessage());
		}
	}
	
	public void a_exportPath()
	{
		try
		{
			if (showSaveFileDialog(txtFilter, txtFilter[0]) != JFileChooser.APPROVE_OPTION) return;
			setComputing(true);
			
			engine.exportPath(getChooserFile());
			
			setComputing(false);
		}
		catch(SimulatorException e)
		{
			error(e.getMessage());
		}
	}
	
	public void a_removePreceding()
	{
		try
		{
			int toStep = Integer.parseInt(removePrecedingField.getText());
			if(toStep < 0 || toStep >= pathTableModel.getRowCount()) throw new SimulatorException("The Remove \'from step\' parameter is invalid.\nIt must be a positive integer");
			
			a_removePreceding(toStep);
			
		}
		catch(NumberFormatException e)
		{
			this.error("The Remove \'from step\' parameter is invalid.\nIt must be a positive integer representing a step in the path table");
		}
		catch(SimulatorException e)
		{
			this.error(e.getMessage());
		}
	}
	
	public void a_removePreceding(int step) throws SimulatorException
	{
		setComputing(true);
		
		engine.removePrecedingStates(step);
		
		pathTableModel.updatePathTable();
		updateTableModel.updateUpdatesTable();
		
		totalTimeLabel.setText(""+engine.getTotalPathTime());
		pathLengthLabel.setText(""+(engine.getPathSize()-1));
		totalRewardLabel.setText(getTotalRewardLabelString());
		stateRewardsLabel.setText(getTotalStateRewardLabelString());
		transitionRewardsLabel.setText(getTotalTransitionRewardLabelString());
		stateLabelList.repaint();
		pathFormulaeList.repaint();
		setComputing(false);
	}
	
	public void a_manualUpdate()
	{
		try
		{
			if (displayPathLoops && pathTableModel.isPathLooping()) {
				if (questionYesNo("A loop in the path has been detected. Do you wish to disable loop detection and extend the path?") == 0) {
					displayPathLoops = false;
					pathTable.repaint();
				}
				else return;
			}
			
			double time = 0.0;
			if(mf.getType() == ModulesFile.STOCHASTIC)
			{
				if(autoTimeCheck.isSelected())
					time = -1;
				else time = Double.parseDouble(stateTimeField.getText());
				
				setComputing(true);
				
				if(currentUpdatesTable.getSelectedRow() == -1)
					throw new SimulatorException("No current update is selected");
				engine.manualUpdate(currentUpdatesTable.getSelectedRow(), time);
				
				//System.out.println("path table height before = "+pathTable.getHeight());
				
				
				pathTableModel.updatePathTable();
				updateTableModel.updateUpdatesTable();
				
				//System.out.println("path table height after = "+pathTable.getHeight());
				
				pathTable.scrollRectToVisible(new Rectangle(0, pathTable.getHeight() - 10, pathTable.getWidth(), pathTable.getHeight()) );
				
				totalTimeLabel.setText(""+engine.getTotalPathTime());
				pathLengthLabel.setText(""+(engine.getPathSize()-1));
				totalRewardLabel.setText(getTotalRewardLabelString());
				stateRewardsLabel.setText(getTotalStateRewardLabelString());
				transitionRewardsLabel.setText(getTotalTransitionRewardLabelString());
				
				setComputing(false);
				
			}
			else
			{
				
				setComputing(true);
				if(currentUpdatesTable.getSelectedRow() == -1)
					throw new SimulatorException("No current update is selected");
				engine.manualUpdate(currentUpdatesTable.getSelectedRow());
				
				pathTableModel.updatePathTable();
				updateTableModel.updateUpdatesTable();
				
				totalTimeLabel.setText(""+engine.getTotalPathTime());
				pathLengthLabel.setText(""+(engine.getPathSize()-1));
				totalRewardLabel.setText(getTotalRewardLabelString());
				stateRewardsLabel.setText(getTotalStateRewardLabelString());
				transitionRewardsLabel.setText(getTotalTransitionRewardLabelString());
				
				pathTable.scrollRectToVisible(new Rectangle(0, (int)pathTable.getPreferredSize().getHeight() - 10, (int)pathTable.getPreferredSize().getWidth(), (int)pathTable.getPreferredSize().getHeight()) );
				
				
				setComputing(false);
			}
			stateLabelList.repaint();
			pathFormulaeList.repaint();
		}
		catch(NumberFormatException e)
		{
			this.error("The Auto update \'no. steps\' parameter is invalid.\nIt must be a positive integer representing a step in the path table");
			setComputing(false);
		}
		catch(SimulatorException e)
		{
			this.error(e.getMessage());
			setComputing(false);
		}
	}
	
	public void a_configureView()
	{
		new GUIViewDialog(gui, pathTableModel, mf);
	}
	
	public void repopulateFormulae()
	{
		((GUISimPathFormulaeList)pathFormulaeList).clearList();
		if(pathActive)
		{
			
			GUIPropertiesList gpl = guiProp.getPropList();
			
			for(int i = 0; i < gpl.getNumProperties(); i++)
			{
				GUIProperty gp = gpl.getProperty(i);
				if(gp.isValidForSimulation())
				{
					PCTLFormula pctl = gp.getPCTLProperty();
					if(pctl instanceof PCTLProb)
					{
						PCTLProb prob = (PCTLProb)pctl;
						//System.out.println(prob.getOperand().toString());
						
						((GUISimPathFormulaeList)pathFormulaeList).addProbFormula(prob);
					}
					else if(pctl instanceof PCTLReward)
					{
						PCTLReward rew = (PCTLReward)pctl;
						//System.out.println(rew.getOperand().toString());
						//pathFormulaeList.add(prob.getOperand().toString());
						((GUISimPathFormulaeList)pathFormulaeList).addRewardFormula(rew);
					}
					
				}
			}
		}
		
		((GUISimLabelFormulaeList)stateLabelList).clearLabels();
		
		if(pathActive)
		{
			
			((GUISimLabelFormulaeList)stateLabelList).addDeadlockAndInit();
			
			GUIPropLabelList labelList = guiProp.getLabTable();
			
			
			//System.out.println("labelList = "+labelList.getLabelsString());
			
			//System.out.println("labelList.getNumLabels() = "+labelList.getNumLabels());
			
			for(int i = 0; i < labelList.getNumLabels(); i++)
			{
				GUILabel gl = labelList.getLabel(i);
				
				if(mf != null)
					((GUISimLabelFormulaeList)stateLabelList).addLabel(gl, mf);
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
		return null;
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
		if(e instanceof GUIModelEvent)
		{
			GUIModelEvent me = (GUIModelEvent)e;
			if(me.getID() == me.NEW_MODEL)
			{
				//New Model
				
				
				
				a_clearModel();
				
				
				doEnables();
				//newList();
			}
			else if(!ignoreNextParse && me.getID() == GUIModelEvent.MODEL_PARSED)
			{
				
				a_loadModulesFile(me.getModulesFile());
				
				doEnables();
				
			}
			else if(ignoreNextParse)
			{
				ignoreNextParse = false;
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
		else if(e instanceof GUIPropertiesEvent)
		{
			if(e.getID() == GUIPropertiesEvent.PROPERTIES_LIST_CHANGED)
			{
				//repopulateFormulae();
			}
		}
		else if (e instanceof GUIExitEvent)
		{
			if(e.getID() == GUIExitEvent.REQUEST_EXIT)
			{
				
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
	
	public void doEnables()
	{
		
		autoUpdateButton.setEnabled(pathActive && !computing);
		autoUpdateField.setEnabled(pathActive);
		backtrackButton.setEnabled(pathActive && !computing);
		backTrackStepField.setEnabled(pathActive);
		removePrecedingButton.setEnabled(pathActive && !computing);
		removePrecedingField.setEnabled(pathActive);
		manualUpdateField.setEnabled(pathActive && !computing && !updateTableModel.oldUpdate);
		stateTimeField.setEnabled(pathActive && mf != null && mf.getType() == ModulesFile.STOCHASTIC && !autoTimeCheck.isSelected());
		autoTimeCheck.setEnabled(pathActive && mf != null && mf.getType() == ModulesFile.STOCHASTIC);
		jLabel2.setEnabled(pathActive);
		jLabel1.setEnabled(pathActive);
		jLabel3.setEnabled(pathActive);
		jLabel4.setEnabled(pathActive);
		
		resetPathButton.setEnabled(pathActive && !computing);
		exportPathButton.setEnabled(pathActive && !computing);
		
		newPathButton.setEnabled(mf != null && !computing);
		
		currentUpdatesTable.setEnabled(pathActive);
		
		modelType.setEnabled(mf != null);
		modelTypeLabel.setEnabled(mf != null);
		
		totalTime.setEnabled(pathActive && mf != null && mf.getType() == ModulesFile.STOCHASTIC);
		totalTimeLabel.setEnabled(pathActive && mf != null && mf.getType() == ModulesFile.STOCHASTIC);
		
		pathLength.setEnabled(pathActive);
		pathLengthLabel.setEnabled(pathActive);
				
		totalRewards.setEnabled(pathActive);
		totalRewardLabel.setEnabled(pathActive);
		
		stateRewards.setEnabled(pathActive);
		stateRewardsLabel.setEnabled(pathActive);
		
		transitionRewards.setEnabled(pathActive);
		transitionRewardsLabel.setEnabled(pathActive);
		
		definedConstants.setEnabled(pathActive);
		definedConstantsLabel.setEnabled(pathActive);
		
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        horizontalSplit = new javax.swing.JSplitPane();
        leftPanel = new javax.swing.JPanel();
        verticalSplit = new javax.swing.JSplitPane();
        topSplit = new javax.swing.JPanel();
        topLeftPanel = new javax.swing.JPanel();
        pathExplorationPanel = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        manualUpdateField = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        stateTimeField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        autoTimeCheck = new javax.swing.JCheckBox();
        jPanel13 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        updatesScroll = new javax.swing.JScrollPane();
        currentUpdatesTable = new javax.swing.JTable();
        currentUpdatesTable = new GUISimulatorUpdatesTable(updateTableModel, this);
        jPanel5 = new javax.swing.JPanel();
        jPanel23 = new javax.swing.JPanel();
        autoUpdateButton = new javax.swing.JButton();
        jPanel17 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel19 = new javax.swing.JPanel();
        autoUpdateField = new javax.swing.JTextField();
        jPanel52 = new javax.swing.JPanel();
        pathModificationPanel = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        jPanel18 = new javax.swing.JPanel();
        backtrackButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jPanel20 = new javax.swing.JPanel();
        jPanel21 = new javax.swing.JPanel();
        removePrecedingButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        backTrackStepField = new javax.swing.JTextField();
        removePrecedingField = new javax.swing.JTextField();
        jPanel22 = new javax.swing.JPanel();
        jPanel25 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel27 = new javax.swing.JPanel();
        jPanel28 = new javax.swing.JPanel();
        jSplitPane3 = new javax.swing.JSplitPane();
        jPanel26 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        pathFormulaeList = new javax.swing.JList();
        pathFormulaeList = new GUISimPathFormulaeList(this);
        jPanel53 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        stateLabelList = new javax.swing.JList();
        stateLabelList = new GUISimLabelFormulaeList(this);
        jLabel7 = new javax.swing.JLabel();
        rightPanel = new javax.swing.JPanel();
        innerRightPanel = new javax.swing.JPanel();
        topRightPanel = new javax.swing.JPanel();
        informationPanel = new javax.swing.JPanel();
        innerInformationPanel = new javax.swing.JPanel();
        topLabels = new javax.swing.JPanel();
        modelType = new javax.swing.JLabel();
        pathLength = new javax.swing.JLabel();
        totalTime = new javax.swing.JLabel();
        topValues = new javax.swing.JPanel();
        modelTypeLabel = new javax.swing.JLabel();
        pathLengthLabel = new javax.swing.JLabel();
        totalTimeLabel = new javax.swing.JLabel();
        middleLabels = new javax.swing.JPanel();
        stateRewards = new javax.swing.JLabel();
        transitionRewards = new javax.swing.JLabel();
        totalRewards = new javax.swing.JLabel();
        middleValues = new javax.swing.JPanel();
        stateRewardsLabel = new javax.swing.JLabel();
        transitionRewardsLabel = new javax.swing.JLabel();
        totalRewardLabel = new javax.swing.JLabel();
        bottomLabels = new javax.swing.JPanel();
        definedConstants = new javax.swing.JLabel();
        bottomValues = new javax.swing.JPanel();
        definedConstantsLabel = new javax.swing.JLabel();
        buttonPanel = new javax.swing.JPanel();
        innerButtonPanel = new javax.swing.JPanel();
        newPathButton = new javax.swing.JButton();
        resetPathButton = new javax.swing.JButton();
        exportPathButton = new javax.swing.JButton();
        tablePanel = new javax.swing.JPanel();
        tableScroll = new javax.swing.JScrollPane();
        pathTable = new javax.swing.JTable();
        pathTable = new GUISimulatorPathTable(pathTableModel, engine);

        setLayout(new java.awt.BorderLayout());

        horizontalSplit.setDividerLocation(302);
        leftPanel.setLayout(new java.awt.BorderLayout());

        leftPanel.setMinimumSize(new java.awt.Dimension(300, 210));
        leftPanel.setPreferredSize(new java.awt.Dimension(302, 591));
        verticalSplit.setDividerLocation(400);
        verticalSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        verticalSplit.setResizeWeight(0.5);
        topSplit.setLayout(new java.awt.BorderLayout());

        topSplit.setMinimumSize(new java.awt.Dimension(302, 227));
        topSplit.setPreferredSize(new java.awt.Dimension(302, 554));
        topLeftPanel.setLayout(new java.awt.BorderLayout());

        pathExplorationPanel.setLayout(new java.awt.GridBagLayout());

        pathExplorationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Exploration"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        pathExplorationPanel.add(jPanel7, gridBagConstraints);

        manualUpdateField.setIcon(new javax.swing.ImageIcon(""));
        manualUpdateField.setText("Manual Update");
        manualUpdateField.setToolTipText("Perform the selected update");
        manualUpdateField.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        manualUpdateField.setMaximumSize(new java.awt.Dimension(112, 25));
        manualUpdateField.setPreferredSize(new java.awt.Dimension(112, 25));
        manualUpdateField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manualUpdateFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        pathExplorationPanel.add(manualUpdateField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        pathExplorationPanel.add(jPanel8, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        pathExplorationPanel.add(jPanel9, gridBagConstraints);

        stateTimeField.setText("1.0");
        stateTimeField.setToolTipText("Enter the time spent in the current state");
        stateTimeField.setEnabled(false);
        stateTimeField.setPreferredSize(new java.awt.Dimension(60, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pathExplorationPanel.add(stateTimeField, gridBagConstraints);

        jLabel1.setText("State time:");
        jLabel1.setMinimumSize(new java.awt.Dimension(57, 15));
        jLabel1.setPreferredSize(new java.awt.Dimension(57, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pathExplorationPanel.add(jLabel1, gridBagConstraints);

        jPanel10.setMinimumSize(new java.awt.Dimension(10, 5));
        jPanel10.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        pathExplorationPanel.add(jPanel10, gridBagConstraints);

        autoTimeCheck.setText("Auto");
        autoTimeCheck.setToolTipText("Automatically sample time from a negative exponential distribution");
        autoTimeCheck.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                autoTimeCheckStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pathExplorationPanel.add(autoTimeCheck, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        pathExplorationPanel.add(jPanel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        pathExplorationPanel.add(jPanel15, gridBagConstraints);

        updatesScroll.setPreferredSize(new java.awt.Dimension(100, 100));
        currentUpdatesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        updatesScroll.setViewportView(currentUpdatesTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pathExplorationPanel.add(updatesScroll, gridBagConstraints);

        jPanel5.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        pathExplorationPanel.add(jPanel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 16;
        pathExplorationPanel.add(jPanel23, gridBagConstraints);

        autoUpdateButton.setIcon(new javax.swing.ImageIcon(""));
        autoUpdateButton.setText("Auto Update");
        autoUpdateButton.setToolTipText("Make a number of automatic updates");
        autoUpdateButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        autoUpdateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoUpdateButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        pathExplorationPanel.add(autoUpdateButton, gridBagConstraints);

        jPanel17.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        pathExplorationPanel.add(jPanel17, gridBagConstraints);

        jLabel2.setText("No. Steps:");
        jLabel2.setPreferredSize(new java.awt.Dimension(57, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pathExplorationPanel.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        pathExplorationPanel.add(jPanel19, gridBagConstraints);

        autoUpdateField.setText("1");
        autoUpdateField.setToolTipText("Enter the number of automatic steps");
        autoUpdateField.setPreferredSize(new java.awt.Dimension(60, 19));
        autoUpdateField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoUpdateFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        pathExplorationPanel.add(autoUpdateField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 0;
        pathExplorationPanel.add(jPanel52, gridBagConstraints);

        topLeftPanel.add(pathExplorationPanel, java.awt.BorderLayout.CENTER);

        pathModificationPanel.setLayout(new java.awt.GridBagLayout());

        pathModificationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Path Modification"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        pathModificationPanel.add(jPanel14, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        pathModificationPanel.add(jPanel16, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        pathModificationPanel.add(jPanel18, gridBagConstraints);

        backtrackButton.setIcon(new javax.swing.ImageIcon(""));
        backtrackButton.setText("Backtrack");
        backtrackButton.setToolTipText("Backtrack to the given step.");
        backtrackButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        backtrackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backtrackButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        pathModificationPanel.add(backtrackButton, gridBagConstraints);

        jLabel3.setText("To Step:");
        jLabel3.setPreferredSize(new java.awt.Dimension(57, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pathModificationPanel.add(jLabel3, gridBagConstraints);

        jPanel20.setNextFocusableComponent(jPanel9);
        jPanel20.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        pathModificationPanel.add(jPanel20, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pathModificationPanel.add(jPanel21, gridBagConstraints);

        removePrecedingButton.setIcon(new javax.swing.ImageIcon(""));
        removePrecedingButton.setText("Remove");
        removePrecedingButton.setToolTipText("Remove all before the given step");
        removePrecedingButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        removePrecedingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removePrecedingButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pathModificationPanel.add(removePrecedingButton, gridBagConstraints);

        jLabel4.setText("Before:");
        jLabel4.setPreferredSize(new java.awt.Dimension(57, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pathModificationPanel.add(jLabel4, gridBagConstraints);

        backTrackStepField.setText("0");
        backTrackStepField.setToolTipText("Enter the step to backtrack to");
        backTrackStepField.setPreferredSize(new java.awt.Dimension(60, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        pathModificationPanel.add(backTrackStepField, gridBagConstraints);

        removePrecedingField.setText("0");
        removePrecedingField.setToolTipText("Enter the step to become the first step");
        removePrecedingField.setPreferredSize(new java.awt.Dimension(60, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        pathModificationPanel.add(removePrecedingField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        pathModificationPanel.add(jPanel22, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        pathModificationPanel.add(jPanel25, gridBagConstraints);

        topLeftPanel.add(pathModificationPanel, java.awt.BorderLayout.SOUTH);

        topSplit.add(topLeftPanel, java.awt.BorderLayout.CENTER);

        verticalSplit.setLeftComponent(topSplit);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Formulae"));
        jPanel4.setMinimumSize(new java.awt.Dimension(302, 35));
        jPanel4.setPreferredSize(new java.awt.Dimension(302, 35));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel4.add(jPanel27, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        jPanel4.add(jPanel28, gridBagConstraints);

        jSplitPane3.setBorder(null);
        jSplitPane3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane3.setResizeWeight(0.5);
        jPanel26.setLayout(new java.awt.GridBagLayout());

        jLabel8.setText("Path formulae:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel26.add(jLabel8, gridBagConstraints);

        jScrollPane3.setViewportView(pathFormulaeList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel26.add(jScrollPane3, gridBagConstraints);

        jSplitPane3.setLeftComponent(jPanel26);

        jPanel53.setLayout(new java.awt.GridBagLayout());

        jScrollPane4.setViewportView(stateLabelList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel53.add(jScrollPane4, gridBagConstraints);

        jLabel7.setText("State labels:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel53.add(jLabel7, gridBagConstraints);

        jSplitPane3.setRightComponent(jPanel53);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel4.add(jSplitPane3, gridBagConstraints);

        verticalSplit.setRightComponent(jPanel4);

        leftPanel.add(verticalSplit, java.awt.BorderLayout.CENTER);

        horizontalSplit.setLeftComponent(leftPanel);

        rightPanel.setLayout(new java.awt.BorderLayout());

        rightPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Simulation Path"));
        innerRightPanel.setLayout(new java.awt.BorderLayout(0, 10));

        innerRightPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topRightPanel.setLayout(new java.awt.BorderLayout(10, 10));

        informationPanel.setLayout(new java.awt.BorderLayout());

        informationPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        innerInformationPanel.setLayout(new javax.swing.BoxLayout(innerInformationPanel, javax.swing.BoxLayout.Y_AXIS));

        innerInformationPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topLabels.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

        topLabels.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 0, 5));
        modelType.setText("Model Type:");
        modelType.setFont(this.getFont().deriveFont(Font.BOLD));
        topLabels.add(modelType);

        pathLength.setText("Path Length:");
        pathLength.setFont(this.getFont().deriveFont(Font.BOLD));
        topLabels.add(pathLength);

        totalTime.setText("Total Time:");
        totalTime.setFont(this.getFont().deriveFont(Font.BOLD));
        topLabels.add(totalTime);

        innerInformationPanel.add(topLabels);

        topValues.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

        topValues.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));
        modelTypeLabel.setText("Unknown");
        modelTypeLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        topValues.add(modelTypeLabel);

        pathLengthLabel.setText("0");
        pathLengthLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        topValues.add(pathLengthLabel);

        totalTimeLabel.setText("0.0");
        totalTimeLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        topValues.add(totalTimeLabel);

        innerInformationPanel.add(topValues);

        middleLabels.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

        middleLabels.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 0, 5));
        stateRewards.setText("State Rewards:");
        stateRewards.setFont(this.getFont().deriveFont(Font.BOLD));
        middleLabels.add(stateRewards);

        transitionRewards.setText("Transition Rewards:");
        transitionRewards.setFont(this.getFont().deriveFont(Font.BOLD));
        middleLabels.add(transitionRewards);

        totalRewards.setText("Total Reward:");
        totalRewards.setFont(this.getFont().deriveFont(Font.BOLD));
        middleLabels.add(totalRewards);

        innerInformationPanel.add(middleLabels);

        middleValues.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

        middleValues.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));
        stateRewardsLabel.setText("0.0");
        stateRewardsLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        middleValues.add(stateRewardsLabel);

        transitionRewardsLabel.setText("0.0");
        transitionRewardsLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        middleValues.add(transitionRewardsLabel);

        totalRewardLabel.setText("0.0");
        totalRewardLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        middleValues.add(totalRewardLabel);

        innerInformationPanel.add(middleValues);

        bottomLabels.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

        bottomLabels.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 0, 5));
        definedConstants.setText("Defined Constants:");
        definedConstants.setFont(this.getFont().deriveFont(Font.BOLD));
        bottomLabels.add(definedConstants);

        innerInformationPanel.add(bottomLabels);

        bottomValues.setLayout(new java.awt.GridLayout(1, 3, 5, 0));

        bottomValues.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));
        definedConstantsLabel.setText("Unknown");
        definedConstantsLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        bottomValues.add(definedConstantsLabel);

        innerInformationPanel.add(bottomValues);

        informationPanel.add(innerInformationPanel, java.awt.BorderLayout.CENTER);

        topRightPanel.add(informationPanel, java.awt.BorderLayout.CENTER);

        buttonPanel.setLayout(new java.awt.BorderLayout());

        innerButtonPanel.setLayout(new java.awt.GridLayout(3, 1, 5, 10));

        newPathButton.setIcon(new javax.swing.ImageIcon(""));
        newPathButton.setText("New Path");
        newPathButton.setToolTipText("New Path");
        newPathButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        newPathButton.setPreferredSize(new java.awt.Dimension(119, 28));
        newPathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newPathButtonActionPerformed(evt);
            }
        });

        innerButtonPanel.add(newPathButton);

        resetPathButton.setIcon(new javax.swing.ImageIcon(""));
        resetPathButton.setText("Reset Path");
        resetPathButton.setToolTipText("Reset Path");
        resetPathButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        resetPathButton.setPreferredSize(new java.awt.Dimension(119, 28));
        resetPathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetPathButtonActionPerformed(evt);
            }
        });

        innerButtonPanel.add(resetPathButton);

        exportPathButton.setIcon(new javax.swing.ImageIcon(""));
        exportPathButton.setText("Export Path");
        exportPathButton.setToolTipText("Export Path");
        exportPathButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        exportPathButton.setPreferredSize(new java.awt.Dimension(119, 28));
        exportPathButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPathButtonActionPerformed(evt);
            }
        });

        innerButtonPanel.add(exportPathButton);

        buttonPanel.add(innerButtonPanel, java.awt.BorderLayout.NORTH);

        topRightPanel.add(buttonPanel, java.awt.BorderLayout.WEST);

        innerRightPanel.add(topRightPanel, java.awt.BorderLayout.NORTH);

        tablePanel.setLayout(new java.awt.BorderLayout());

        pathTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tableScroll.setViewportView(pathTable);

        tablePanel.add(tableScroll, java.awt.BorderLayout.CENTER);

        innerRightPanel.add(tablePanel, java.awt.BorderLayout.CENTER);

        rightPanel.add(innerRightPanel, java.awt.BorderLayout.CENTER);

        horizontalSplit.setRightComponent(rightPanel);

        add(horizontalSplit, java.awt.BorderLayout.CENTER);

    }// </editor-fold>//GEN-END:initComponents

    private void autoTimeCheckStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_autoTimeCheckStateChanged
        
    	this.stateTimeField.setEnabled(!autoTimeCheck.isSelected());
        
    }//GEN-LAST:event_autoTimeCheckStateChanged
	
	private void initPopups()
	{				
		newPath = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_newPath();
			}
		};
		
		newPath.putValue(Action.LONG_DESCRIPTION, "Creates a new path.");
		newPath.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		newPath.putValue(Action.NAME, "New Path");
		newPath.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallNewPath.gif"));
		
		resetPath = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_restartPath();
			}
		};
		
		resetPath.putValue(Action.LONG_DESCRIPTION, "Resets the path.");
		resetPath.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		resetPath.putValue(Action.NAME, "Reset Path");
		resetPath.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallResetPath.gif"));
		
		exportPath = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				a_exportPath();
			}
		};
		
		exportPath.putValue(Action.LONG_DESCRIPTION, "Exports the path.");
		exportPath.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
		exportPath.putValue(Action.NAME, "Export Path");
		exportPath.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallExport.gif"));			
		
		backtrackToHere = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					a_backTrack(pathTable.getSelectedRow());
				}
				catch(SimulatorException ex)
				{
					error(ex.getMessage());
				}
			}
		};
		backtrackToHere.putValue(Action.LONG_DESCRIPTION, "Backtracks the path to the selected state.");
		backtrackToHere.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_B));
		backtrackToHere.putValue(Action.NAME, "Backtrack to here");
		backtrackToHere.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallBacktrack.gif"));
		
		removeToHere = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				
				try
				{
					a_removePreceding(pathTable.getSelectedRow());
				}
				catch(SimulatorException ex)
				{
					error(ex.getMessage());
				}
			}
		};		
		removeToHere.putValue(Action.LONG_DESCRIPTION, "Removes states preceding the selected state from the path.");
		removeToHere.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		removeToHere.putValue(Action.NAME, "Remove preceding steps");
		removeToHere.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallRemovePreceding.gif"));
		
		configureView = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{				
				a_configureView();				
			}
		};		
		configureView.putValue(Action.LONG_DESCRIPTION, "Configures the view.");
		configureView.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
		configureView.putValue(Action.NAME, "Configure View");
		configureView.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallFind.gif"));
		
		pathPopup = new JPopupMenu();
		pathPopup.add(newPath);
		pathPopup.add(resetPath);
		pathPopup.add(exportPath);		
		pathPopup.addSeparator();
		pathPopup.add(backtrackToHere);
		pathPopup.add(removeToHere);
		pathPopup.addSeparator();
		pathPopup.add(configureView);
	}
	
	
	private void exportPathButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_exportPathButtonActionPerformed
	{//GEN-HEADEREND:event_exportPathButtonActionPerformed
		a_exportPath();
	}//GEN-LAST:event_exportPathButtonActionPerformed
	
	private void resetPathButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_resetPathButtonActionPerformed
	{//GEN-HEADEREND:event_resetPathButtonActionPerformed
		a_restartPath();
	}//GEN-LAST:event_resetPathButtonActionPerformed
	
	private void backtrackButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_backtrackButtonActionPerformed
	{//GEN-HEADEREND:event_backtrackButtonActionPerformed
		a_backTrack();
	}//GEN-LAST:event_backtrackButtonActionPerformed
	
	private void autoUpdateFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_autoUpdateFieldActionPerformed
	{//GEN-HEADEREND:event_autoUpdateFieldActionPerformed
		
	}//GEN-LAST:event_autoUpdateFieldActionPerformed
	
	private void autoUpdateButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_autoUpdateButtonActionPerformed
	{//GEN-HEADEREND:event_autoUpdateButtonActionPerformed
		a_autoStep();
	}//GEN-LAST:event_autoUpdateButtonActionPerformed
	
	private void newPathButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_newPathButtonActionPerformed
	{//GEN-HEADEREND:event_newPathButtonActionPerformed
		a_newPath();
	}//GEN-LAST:event_newPathButtonActionPerformed
	
	private void removePrecedingButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_removePrecedingButtonActionPerformed
	{//GEN-HEADEREND:event_removePrecedingButtonActionPerformed
		a_removePreceding();
	}//GEN-LAST:event_removePrecedingButtonActionPerformed
	
	private void manualUpdateFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_manualUpdateFieldActionPerformed
	{//GEN-HEADEREND:event_manualUpdateFieldActionPerformed
		a_manualUpdate();
	}//GEN-LAST:event_manualUpdateFieldActionPerformed
	
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
		
		if (e.getSource() == pathTable)
		{
			int row = pathTable.getSelectedRow();
			backTrackStepField.setText("" + row);
			removePrecedingField.setText("" + row);		
		}
	}
	
	public void mouseReleased(MouseEvent e)
	{
		doPopupDetection(e);
	}
	
	public void doPopupDetection(MouseEvent e)
	{
		if(!computing)
		{
			if(e.isPopupTrigger() && (e.getSource() == pathTable || e.getSource() == pathTable.getTableHeader() || e.getSource() == pathTable.getParent()))
			{
				backtrackToHere.setEnabled(!(e.getSource() == pathTable.getTableHeader() || e.getSource() == pathTable.getParent()));
				removeToHere.setEnabled(!(e.getSource() == pathTable.getTableHeader() || e.getSource() == pathTable.getParent()));
				
				newPath.setEnabled(newPathButton.isEnabled());
				resetPath.setEnabled(resetPathButton.isEnabled());
				exportPath.setEnabled(exportPathButton.isEnabled());
				configureView.setEnabled(pathActive);
				
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
				
				
				pathPopup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	
	public void insertTableColumn(TableColumn column)
	{
		int i = 1;
		int j = 1;
		
		String text = (String) column.getHeaderValue();
										 	
		while (j <= pathTable.getColumnCount() && i < pathTableModel.getColumnCount())
		{
			if (j == pathTable.getColumnCount())
			{
				// Found the location
				pathTable.addColumn(column);	
				
				break;
			}
			else
			{									
				String tcj = pathTable.getColumnName(j);
				String tci = pathTableModel.getColumnName(i);
				
				if (tci.equals(text))
				{
					// Found the location
					pathTable.addColumn(column);
					
					// Move to location
					if (j != pathTable.getColumnCount()-1)
						pathTable.moveColumn(pathTable.getColumnCount()-1, j);
					break;
				}
				else
				{
					i++; 
					
					if (tci.equals(tcj)) { j++; }											
				}
			}
		}
	}
	
	public void showColumn(int i )
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
		
		getPrism().getSettings().set(PrismSettings.SIMULATOR_RENDER_ALL_VALUES, new Integer(displayStyle?0:1));
		
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
		return mf;
	}
	
	
	/**
	 *  Captures
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if(pathTable.getSelectedRowCount() == 0)
		{
			pathTable.getSelectionModel().setSelectionInterval(pathTable.getRowCount()-1, pathTable.getRowCount()-1);
			updateTableModel.updateUpdatesTable();
			
		}
		
		
		
		updateTableModel.updateUpdatesTable(pathTable.getSelectedRow());
		stateLabelList.repaint();
		pathFormulaeList.repaint();
		
	}
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JCheckBox autoTimeCheck;
    javax.swing.JButton autoUpdateButton;
    javax.swing.JTextField autoUpdateField;
    javax.swing.JTextField backTrackStepField;
    javax.swing.JButton backtrackButton;
    private javax.swing.JPanel bottomLabels;
    private javax.swing.JPanel bottomValues;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JPanel buttonPanel;
    javax.swing.JTable currentUpdatesTable;
    private javax.swing.JLabel definedConstants;
    private javax.swing.JLabel definedConstantsLabel;
    javax.swing.JButton exportPathButton;
    private javax.swing.JSplitPane horizontalSplit;
    private javax.swing.JPanel informationPanel;
    private javax.swing.JPanel innerButtonPanel;
    private javax.swing.JPanel innerInformationPanel;
    private javax.swing.JPanel innerRightPanel;
    javax.swing.JLabel jLabel1;
    javax.swing.JLabel jLabel2;
    javax.swing.JLabel jLabel3;
    javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel52;
    private javax.swing.JPanel jPanel53;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JPanel leftPanel;
    javax.swing.JButton manualUpdateField;
    private javax.swing.JPanel middleLabels;
    private javax.swing.JPanel middleValues;
    private javax.swing.JLabel modelType;
    private javax.swing.JLabel modelTypeLabel;
    javax.swing.JButton newPathButton;
    private javax.swing.JPanel pathExplorationPanel;
    javax.swing.JList pathFormulaeList;
    private javax.swing.JLabel pathLength;
    private javax.swing.JLabel pathLengthLabel;
    private javax.swing.JPanel pathModificationPanel;
    private javax.swing.JTable pathTable;
    javax.swing.JButton removePrecedingButton;
    javax.swing.JTextField removePrecedingField;
    javax.swing.JButton resetPathButton;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JList stateLabelList;
    private javax.swing.JLabel stateRewards;
    private javax.swing.JLabel stateRewardsLabel;
    javax.swing.JTextField stateTimeField;
    private javax.swing.JPanel tablePanel;
    private javax.swing.JScrollPane tableScroll;
    private javax.swing.JPanel topLabels;
    private javax.swing.JPanel topLeftPanel;
    private javax.swing.JPanel topRightPanel;
    private javax.swing.JPanel topSplit;
    private javax.swing.JPanel topValues;
    private javax.swing.JLabel totalRewardLabel;
    private javax.swing.JLabel totalRewards;
    private javax.swing.JLabel totalTime;
    private javax.swing.JLabel totalTimeLabel;
    private javax.swing.JLabel transitionRewards;
    private javax.swing.JLabel transitionRewardsLabel;
    private javax.swing.JScrollPane updatesScroll;
    private javax.swing.JSplitPane verticalSplit;
    // End of variables declaration//GEN-END:variables
	
    
    /**
     * @author mxk
     * Represents a  in the model.
     */
    public class Variable 
	{
		private int index;
		private String name;
		private int type;
		
		public Variable(int index, String name, int type)
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
		
		public int getType() 
		{
			return type;
		}
		
		public String toString()
		{
			return name;
		}
		
		public boolean equals(Object o)
		{
			return (o instanceof Variable && ((Variable)o).getIndex() == index);
		}
	}	
    
	class PathTableModel extends AbstractTableModel implements GUIGroupedTableModel
	{		
		private boolean SHOW_STEP = true;
				
		ArrayList visibleVariables;
		ArrayList hiddenVariables;
		
		GUISimulator simulator;
		
		public PathTableModel(GUISimulator simulator)
		{	
			this.simulator = simulator;
		}
		
		public SimulatorEngine getEngine()
		{
			return engine;
		}
		
		public ArrayList getVisibleVariables()
		{
			return visibleVariables;
		}
		
		public ArrayList getHiddenVariables()
		{
			return hiddenVariables;
		}
		
		public void setVisibility(ArrayList visibleVariables, ArrayList hiddenVariables)
		{
			this.visibleVariables = visibleVariables;
			this.hiddenVariables = hiddenVariables;
			
			if (pathActive) 
			{	fireTableStructureChanged(); }
		}
		
		public int getGroupCount() 
		{
			if(!pathActive)
			{
				return 0;				
			}
			else
			{
				int groupCount = 0;
				
				if (SHOW_STEP) 
				{ groupCount++;	}
				
				if (this.visibleVariables.size() > 0)
				{ groupCount++;	}
				
				if (mf.getType() == ModulesFile.STOCHASTIC) 
				{ groupCount++;	} 
				
				// Add state and transitions rewards for each reward structure.
				if ((2*mf.getNumRewardStructs()) > 0)
				{ groupCount++;	} 
				
				return groupCount;				
			}			
        }
		
		public String getGroupName(int groupIndex) 
		{
			int groupCount = 0;
			
			if (SHOW_STEP) 
			{ 					
				if (groupCount == groupIndex)
				{	return "Step";	}
				
				groupCount++;
			}
			
			if (this.visibleVariables.size() > 0)
			{ 
				if (groupCount == groupIndex)
				{	return "Variables";	}
				
				groupCount++;
			}
			
			if (mf.getType() == ModulesFile.STOCHASTIC) 
			{	
				if (groupCount == groupIndex)
				{	return "Time";	}
				
				groupCount++;				
			} 
			
			// Add state and transitions rewards for each reward structure.
			if ((2*mf.getNumRewardStructs()) > 0)
			{
				if (groupCount == groupIndex)
				{	return "Rewards";	}
				
				groupCount++;				
			}
			
			return "";
        }

		public int getLastColumnOfGroup(int groupIndex) 
		{
			int stepStart = 0;
			int varStart = (SHOW_STEP) ? stepStart + 1 : stepStart;
			int timeStart = varStart + visibleVariables.size();
			int rewardStart = timeStart + 1;
			
			int groupCount = 0;
			
			if (SHOW_STEP) 
			{ 
				if (groupCount == groupIndex)
				{	return stepStart;	}
				
				groupCount++;				
			}
			
			if (this.visibleVariables.size() > 0)
			{ 
				if (groupCount == groupIndex)
				{	return varStart + visibleVariables.size() -1; }
				
				groupCount++;				
			}
			
			if (mf.getType() == ModulesFile.STOCHASTIC) 
			{	
				if (groupCount == groupIndex)
				{	return timeStart;	}
				
				groupCount++;
			} 
			
			// Add state and transitions rewards for each reward structure.
			if ((2*mf.getNumRewardStructs()) > 0)
			{
				if (groupCount == groupIndex)
				{	return rewardStart + 2*mf.getNumRewardStructs() - 1;	}
				
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
			if (!pathActive) 
			{	return 0;	}
			else 
			{
				// At least we have the step count and s.
				int colCount = (SHOW_STEP) ? 1 : 0;
				
				colCount += this.visibleVariables.size();
				
				// Add state and transitions rewards for each reward structure.
				colCount += (2*mf.getNumRewardStructs());
				
				// Add a column for time if CTMC.
				if (mf.getType() == ModulesFile.STOCHASTIC) 
				{	colCount += 1; 		}
				
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
			return (pathActive ? SimulatorEngine.getPathSize() : 0);			
		}
		
		public boolean shouldColourRow(int row)
		{
			int selection = stateLabelList.getSelectedIndex();
			if(selection != -1)
			{
				GUISimLabelFormulaeList.SimLabel label = (GUISimLabelFormulaeList.SimLabel)stateLabelList.getModel().getElementAt(selection);
				if(row == getRowCount()-1)
				{
					if(label.getResult() == 1)
						return true;
				}
				else
				{
					if(label.getResult(row) == 1)
						return true;
				}
			}
			
			return false;
		}
		
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			try
			{
				if(pathActive)
				{
					int stepStart = 0;
					int varStart = (SHOW_STEP) ? stepStart + 1 : stepStart;
					int timeStart = varStart + visibleVariables.size();
					
					// The step column
					if (stepStart <= columnIndex && columnIndex < varStart)
					{
						return "" + rowIndex;						
					}
					// A variable column
					else if (varStart <= columnIndex && columnIndex < timeStart)
					{						
						int varIndex = ((Variable)visibleVariables.get(columnIndex - varStart)).getIndex();
						
						int type = engine.getVariableType(varIndex);
						int result = SimulatorEngine.getPathData(varIndex, rowIndex);
						
						if(type == Expression.BOOLEAN)
						{	return new Boolean(result != 0); }						
						else if (type == Expression.INT)
						{	return new Integer(result);	}
					}
					
					
					// todo: tidy rest up.
					if (mf == null) return null;
					if(mf.getType() == ModulesFile.STOCHASTIC)
					{
						int n = visibleVariables.size();
						if(columnIndex == timeStart) //where time should be
						{
							return new Double(SimulatorEngine.getTimeSpentInPathState(rowIndex));
						}
						else if(columnIndex > timeStart) //rewards
						{
							int i = columnIndex-(n+2);
							if (i%2 == 0) return new Double(SimulatorEngine.getStateRewardOfPathState(rowIndex,i/2));
							else return new Double(SimulatorEngine.getTransitionRewardOfPathState(rowIndex,i/2));
						}
					}
					else
					{
						int n = visibleVariables.size();
						if(columnIndex > n)
						{
							int i = columnIndex-(n+1);
							if (i%2 == 0) return new Double(SimulatorEngine.getStateRewardOfPathState(rowIndex,i/2));
							else return new Double(SimulatorEngine.getTransitionRewardOfPathState(rowIndex,i/2));
						}
					}
					return null;
				}
				else return null;
			}
			catch(SimulatorException e)
			{
				//System.out.println("Catching a simulator exception");
				return null;
			}
		}
		
		public String getColumnName(int columnIndex)
		{
			try
			{
				if(pathActive)
				{
					int stepStart = 0;
					int varStart = (SHOW_STEP) ? stepStart + 1 : stepStart;
					int timeStart = varStart + visibleVariables.size();
					
					// The step column
					if (stepStart <= columnIndex && columnIndex < varStart)
					{
						return "#";						
					}
					// A variable column
					else if (varStart <= columnIndex && columnIndex < timeStart)
					{						
						return ((Variable)visibleVariables.get(columnIndex - varStart)).getName();						
					}
					
					
					if (mf == null) return null;
					if (mf.getType() == ModulesFile.STOCHASTIC)
					{
						int n = visibleVariables.size();
						if(columnIndex == n+1) //where time should be
						{
							return "Time";
						}
						else if(columnIndex > n+1) //rewards
						{
							int i = columnIndex-(n+2);
							return "" + ((i%2 == 0)?"Sta.":"Tra.") + " Rew. "+((i/2)+1);
						}
					}
					else
					{
						int n = visibleVariables.size();
						if(columnIndex > n) //rewards
						{
							int i = columnIndex-(n+1);
							return "" + ((i%2 == 0)?"Sta.":"Tra.") + " Rew. "+((i/2)+1);
						}
					}
					return engine.getVariableName(columnIndex-1);
				}
				else return "";
			}
			catch(SimulatorException e)
			{
				return "";
			}
		}
		
		/** 
		 * Method is called when a new path is created.
		 * The structure of the path may be for a different model etc.
		 */
		public void restartPathTable()
		{						
			visibleVariables = new ArrayList();
			hiddenVariables = new ArrayList();
			
			if (pathActive)
			{
				try
				{
					for (int i = 0; i < engine.getNumVariables(); i++)
					{
							visibleVariables.add(new Variable(i, engine.getVariableName(i), engine.getVariableType(i))); }
					}
				catch (SimulatorException e) {}
			}
			
			fireTableStructureChanged();
			
			//Sort out the minimum widths for each column
			sortOutColumnSizes();
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
	}
	
	public void sortOutColumnSizes()
	{
		int prevWidth = pathTable.getWidth();
		
		TableColumnModel cm = pathTable.getColumnModel();
		
		for(int i = 0; i < cm.getColumnCount(); i++)
		{
			TableColumn tc = cm.getColumn(i);
			
			tc.setMinWidth(50);
		}
		
		if(50*cm.getColumnCount() > tableScroll.getWidth())
		{
			pathTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			
		}
		else
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
	
	public void notifySettings(PrismSettings settings)
	{
		displayStyleFast = settings.getInteger(PrismSettings.SIMULATOR_RENDER_ALL_VALUES) == 0;
		if(displayStyleFast)
		{
			((GUISimulatorPathTable)pathTable).switchToChangeRenderer();
		}
		else
		{
			((GUISimulatorPathTable)pathTable).switchToBoringRenderer();
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
			if(pathActive)
				return 3;
			else return 0;
		}
		
		public int getRowCount()
		{
			
			if(pathActive)
			{
				return engine.getNumUpdates();
			}
			else return 0;
			
		}
		
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			String s;
			if(pathActive)
			{
				switch(columnIndex)
				{
					case 0:
						s = engine.getActionLabelOfUpdate(rowIndex);
						if (!s.equals("[]")) return s;
						else return engine.getModuleNameOfUpdate(rowIndex);
					case 1: return ""+engine.getProbabilityOfUpdate(rowIndex);
					case 2: return engine.getAssignmentDescriptionOfUpdate(rowIndex);
					default: return "";
				}
			}
			else return "";
			
		}
		
		public String getColumnName(int column)
		{
			if(pathActive)
			{
				switch(column)
				{
					case 0: return "Action";
					case 1:
					{
						if(mf != null && mf.getType() == ModulesFile.STOCHASTIC)
							return "Rate";
						else return "Prob.";
						
					}
					case 2: return "Update";
					default: return "";
				}
			}
			else return "";
		}
		
		public void updateUpdatesTable()
		{
			if(isOldUpdate())
			{
				engine.finishedWithOldUpdates();
			}
			oldUpdate = false;
			oldStep = -1;
			doEnables();
			fireTableDataChanged();
			
			currentUpdatesTable.setEnabled(true);
			
			if(getRowCount() > 0)
			{
				currentUpdatesTable.getSelectionModel().setSelectionInterval(0,0);
			}
		}
		
		public void updateUpdatesTable(int oldStep)
		{
			if(oldStep == pathTable.getRowCount()-1) // if current state selected
			{
				updateUpdatesTable();
			}
			else
			{
				this.oldStep = oldStep;
				oldUpdate = true;
				doEnables();
				engine.calculateOldUpdates(oldStep);
				fireTableDataChanged();
				
				currentUpdatesTable.setEnabled(false);
				
				if(getRowCount() > 0)
				{
					int selectThis = engine.getChosenIndexOfOldUpdate(oldStep);
					currentUpdatesTable.getSelectionModel().setSelectionInterval(selectThis,selectThis);
				}
			}
		}
		
		public void restartUpdatesTable()
		{
			fireTableStructureChanged();
			if(getRowCount() > 0)
			{
				oldUpdate = false;
				oldStep = -1;
				currentUpdatesTable.getSelectionModel().setSelectionInterval(0,0);
			}
		}
		
		public int getProbabilityDistributionOf(int row)
		{
			return engine.getDistributionIndexOfUpdate(row);
		}
	}
}
