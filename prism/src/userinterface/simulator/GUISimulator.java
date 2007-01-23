//==============================================================================
//
//	Copyright (c) 2004-2006, Andrew Hinton, Mark Kattenbelt
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
import userinterface.simulator.GUIViewDialog.RewardListItem;
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
	private JMenu simulatorMenu;
	private JPopupMenu pathPopupMenu;
	
	
	//Current State
	private ModulesFile parsedModel;
	private boolean pathActive;
	private boolean engineBuilt;
	
	private PathTableModel pathTableModel;
	private UpdateTableModel updateTableModel;
	
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
		pathTableModel = new PathTableModel(new SimulationView());
		updateTableModel = new UpdateTableModel();
		
		this.gui = gui;
		this.engine = gui.getPrism().getSimulator();
		initComponents();
		initPopups();
		
		doEnables();
		
		horizontalSplit.setDividerLocation((int)leftExplorePanel.getPreferredSize().getHeight() + 11);
		
		//newPathButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallNewPath.gif")));
		//resetPathButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallResetPath.gif")));
		//exportPathButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallExport.gif")));
		//configureViewButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallFind.gif")));
		
		randomExplorationButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallAutomaticUpdate.gif")));
		backtrackButton.setIcon(new ImageIcon(this.getClass().getResource("/images/smallBacktracking.gif")));
				
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
					currentUpdatesTable.requestFocus();
				}
			}
		});
		
		currentUpdatesTable.addKeyListener(new KeyAdapter() 
		{						
			public void keyPressed(KeyEvent e) 
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER && currentUpdatesTable.isEnabled())
				{
					a_manualUpdate();
					currentUpdatesTable.requestFocus();
				}
					
			}
		});
		
		pathTable.setModel(pathTableModel);
		
		lastConstants = null;
		
		lastPropertyConstants = null;
		
		lastInitialState = null;
		
		tableScroll.setRowHeaderView(((GUISimulatorPathTable)pathTable).getPathRowHeader());
		manualUpdateTableScrollPane.setRowHeaderView(((GUISimulatorUpdatesTable)currentUpdatesTable).getUpdateRowHeader());
		
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
		totalTimeLabel.setText(PrismUtils.formatDouble(this.getPrism().getSettings(), 0.0));
		pathLengthLabel.setText("0");		
		
		txtFilter = new GUIPrismFileFilter[1];
		txtFilter[0] = new GUIPrismFileFilter("Text files (*.txt)");
		txtFilter[0].addExtension("txt");
		
		displayStyleFast = true;
		displayPathLoops = true;
		
		GUINetworkEditor netEdit = new GUINetworkEditor(getGUI(), new SimulatorNetworkHandler()); 
		
		getPrism().getSettings().setFileSelector(PrismSettings.SIMULATOR_NETWORK_FILE, netEdit );
		
		autoTimeCheck.setSelected(true);
		currentUpdatesTable.requestFocus();
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
		n = parsedModel.getNumRewardStructs();
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
		n = parsedModel.getNumRewardStructs();
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
		this.parsedModel = mf;
		try
		{
			if(engineBuilt)
			{
				engine.deallocateEngine();
				engineBuilt = false;				
			}
			pathActive = false;
			pathTableModel.restartPathTable();
			updateTableModel.restartUpdatesTable();
						
			if(mf != null)
				modelTypeLabel.setText(""+mf.getTypeString());
			else
				modelTypeLabel.setText("Unknown");
			
			((GUISimLabelFormulaeList)stateLabelList).clearLabels();
			((GUISimPathFormulaeList)pathFormulaeList).clearList();
			stateLabelList.repaint();
			pathFormulaeList.repaint();
			doEnables();
			
			typeExploreCombo.removeAllItems();
			typeExploreCombo.addItem("Steps");
			typeExploreCombo.addItem("Up to step");
			
			if (mf != null && mf.getType() == ModulesFile.STOCHASTIC)
			{
				typeExploreCombo.addItem("Time");
				typeExploreCombo.addItem("Up to time");
			}
					
			typeBacktrackCombo.setEnabled(pathActive);
			typeBacktrackCombo.removeAllItems();
			
			typeBacktrackCombo.addItem("Steps");
			typeBacktrackCombo.addItem("Back to step");
			
			if (mf != null && mf.getType() == ModulesFile.STOCHASTIC)
			{
				typeBacktrackCombo.addItem("Time");
				typeBacktrackCombo.addItem("Back to time");
			}
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
			// get properties constants/labels
			PropertiesFile pf;
			try
			{
				pf = getPrism().parsePropertiesString(parsedModel, guiProp.getConstantsString().toString()+guiProp.getLabelsString());
			}
			catch(ParseException e)
			{
				// ignore properties if they don't parse
				pf = null; //if any problems
			}
			
			// if necessary, get values for undefined constants from user
			UndefinedConstants uCon = new UndefinedConstants(parsedModel, pf);
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
			parsedModel.setUndefinedConstants(lastConstants);
			pf.setUndefinedConstants(lastPropertyConstants);
			
			// now determine the initial state for simulation
			
			// first select a default state (the initial state) which may or may not end up being used
			Values defaultInitialState = new Values();
			defaultInitialState.addValues(parsedModel.getInitialValues());
			
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
					if (lastInitialState.getNumValues() != defaultInitialState.getNumValues())
					{
						match = false;
					}
					else
					{
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
					}
					if(!match) // if there's a problem, just use the default
					{
						lastInitialState = defaultInitialState;
					}
				}
				
				initialState = GUIInitialStatePicker.defineInitalValuesWithDialog(getGUI(), lastInitialState, parsedModel);
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
			engine.startNewPath(parsedModel, pf, initialState);
			//engine.setPropertyConstants(lastPropertyConstants);
			
			engineBuilt = true;
			pathActive = true;
			
			totalTimeLabel.setText(PrismUtils.formatDouble(this.getPrism().getSettings(), engine.getTotalPathTime()));
			pathLengthLabel.setText(""+(engine.getPathSize()-1));
			definedConstantsLabel.setText((uCon.getDefinedConstantsString().length() == 0) ? "None" : uCon.getDefinedConstantsString());
			
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
				new GUIViewDialog(gui, pathTableModel.getView());
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
	
	/** Explore a number of steps. */
	public void a_autoStep(int noSteps)
	{		
		try
		{		
			int oldPathSize = engine.getPathSize();
			
			if (displayPathLoops && pathTableModel.isPathLooping())
			{
				if (questionYesNo("The current path contains a deterministic loop. Do you wish to disable the detection of deterministic loops and extend the path anyway?") == 0) 
				{
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
			
			totalTimeLabel.setText(PrismUtils.formatDouble(this.getPrism().getSettings(), engine.getTotalPathTime()));
			pathLengthLabel.setText(""+(engine.getPathSize()-1));
			
			stateLabelList.repaint();
			pathFormulaeList.repaint();
			setComputing(false);
			
			int newPathSize = engine.getPathSize();
			
			if (displayPathLoops && pathTableModel.isPathLooping() && (newPathSize - oldPathSize) < noSteps)
			{
				message("Exploration has stopped early because a deterministic loop has been detected.");
			}			
		}		
		catch(SimulatorException e)
		{
			this.error(e.getMessage());
		}
	}	
	
	/** Explore an amount of time. */
	public void a_autoStep(double time)
	{		
		try
		{	
			double oldPathTime = engine.getTotalPathTime();
			
			if (displayPathLoops && pathTableModel.isPathLooping())
			{
				if (questionYesNo("The current path contains a deterministic loop. Do you wish to disable the detection of deterministic loops and extend the path anyway?") == 0) 
				{
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
			
			engine.automaticChoices(time, displayPathLoops);		
												
			pathTableModel.updatePathTable();
			updateTableModel.updateUpdatesTable();
			pathTable.scrollRectToVisible(new Rectangle(0, (int)pathTable.getPreferredSize().getHeight() - 10, (int)pathTable.getPreferredSize().getWidth(), (int)pathTable.getPreferredSize().getHeight()) );
			
			totalTimeLabel.setText(PrismUtils.formatDouble(this.getPrism().getSettings(), engine.getTotalPathTime()));
			pathLengthLabel.setText(""+(engine.getPathSize()-1));
			
			stateLabelList.repaint();
			pathFormulaeList.repaint();
			setComputing(false);
			
			double newPathTime = engine.getTotalPathTime();
			
			if (displayPathLoops && pathTableModel.isPathLooping() && (newPathTime - oldPathTime) < time)
			{
				message("Exploration has stopped early because a deterministic loop has been detected.");
			}			
		}		
		catch(SimulatorException e)
		{
			this.error(e.getMessage());
		}
	}	
	
	/** Backtracks to a certain time. */	
	public void a_backTrack(double time) throws SimulatorException
	{
		setComputing(true);
		
		engine.backtrack(time);
		
		pathTableModel.updatePathTable();
		updateTableModel.updateUpdatesTable();
		
		totalTimeLabel.setText(PrismUtils.formatDouble(this.getPrism().getSettings(), engine.getTotalPathTime()));
		pathLengthLabel.setText(""+(engine.getPathSize()-1));
		stateLabelList.repaint();
		pathFormulaeList.repaint();
		
		setComputing(false);
	}
	
	/** Backtracks to a certain step. */	
	public void a_backTrack(int step) throws SimulatorException
	{
		setComputing(true);
		
		engine.backtrack(step);
		
		pathTableModel.updatePathTable();
		updateTableModel.updateUpdatesTable();
		
		totalTimeLabel.setText(PrismUtils.formatDouble(this.getPrism().getSettings(), engine.getTotalPathTime()));
		pathLengthLabel.setText(""+(engine.getPathSize()-1));
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
			
			totalTimeLabel.setText(PrismUtils.formatDouble(this.getPrism().getSettings(), engine.getTotalPathTime()));
			pathLengthLabel.setText(""+(engine.getPathSize()-1));
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
	
	public void a_removePreceding(int step) throws SimulatorException
	{
		setComputing(true);
		
		engine.removePrecedingStates(step);
		
		pathTableModel.updatePathTable();
		updateTableModel.updateUpdatesTable();
		
		totalTimeLabel.setText(PrismUtils.formatDouble(this.getPrism().getSettings(), engine.getTotalPathTime()));
		pathLengthLabel.setText(""+(engine.getPathSize()-1));
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
			
			double time = -1;
			if(parsedModel.getType() == ModulesFile.STOCHASTIC)
			{				
				if (!autoTimeCheck.isSelected())
				{
					time = GUITimeDialog.askTime(this.getGUI(), this);
					if (time < 0.0d) // dialog cancelled
						return;					
				}
				
					//Double.parseDouble(stateTimeField.getText());
				
				setComputing(true);
				
				if(currentUpdatesTable.getSelectedRow() == -1)
					throw new SimulatorException("No current update is selected");
				engine.manualUpdate(currentUpdatesTable.getSelectedRow(), time);
				
				//System.out.println("path table height before = "+pathTable.getHeight());
								
				pathTableModel.updatePathTable();
				updateTableModel.updateUpdatesTable();
				
				//System.out.println("path table height after = "+pathTable.getHeight());
				
				pathTable.scrollRectToVisible(new Rectangle(0, pathTable.getHeight() - 10, pathTable.getWidth(), pathTable.getHeight()) );
				
				totalTimeLabel.setText(PrismUtils.formatDouble(this.getPrism().getSettings(), engine.getTotalPathTime()));
				pathLengthLabel.setText(""+(engine.getPathSize()-1));
				
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
		new GUIViewDialog(gui, pathTableModel.getView());
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
				
				if(parsedModel != null) {
					if (gl.isParseable()) {
						try {
							PropertiesFile pf = getPrism().parsePropertiesString(parsedModel, guiProp.getConstantsString().toString()+"\n"+gl.toString());
							((GUISimLabelFormulaeList)stateLabelList).addLabel(pf.getLabelList().getLabelName(0), pf.getLabelList().getLabel(0), parsedModel);
						}
						catch (ParseException e) {}
						catch (PrismException e) {}
					}
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
	
	protected void doEnables()
	{	
		newPath.setEnabled(parsedModel != null && !computing);
		resetPath.setEnabled(pathActive && !computing);
		exportPath.setEnabled(pathActive && !computing);
		configureView.setEnabled(pathActive && !computing);
		
		randomExplorationButton.setEnabled(pathActive && !computing);
		backtrackButton.setEnabled(pathActive && !computing);
		
		inputExploreField.setEnabled(pathActive);		
		inputBacktrackField.setEnabled(pathActive);
		
		typeExploreCombo.setEnabled(pathActive);				
		typeBacktrackCombo.setEnabled(pathActive);
		
		currentUpdatesTable.setEnabled(pathActive && !computing);
		autoTimeCheck.setEnabled(pathActive && parsedModel != null && parsedModel.getType() == ModulesFile.STOCHASTIC);
		
		//resetPathButton.setEnabled(pathActive && !computing);
		//exportPathButton.setEnabled(pathActive && !computing);
		//configureViewButton.setEnabled(pathActive && !computing);
		
		//newPath.setEnabled(parsedModel != null && !computing);
		//newPathButton.setEnabled(parsedModel != null && !computing);
		
		currentUpdatesTable.setEnabled(pathActive);
		
		modelType.setEnabled(parsedModel != null);
		modelTypeLabel.setEnabled(parsedModel != null);
		
		totalTime.setEnabled(pathActive && parsedModel != null && parsedModel.getType() == ModulesFile.STOCHASTIC);
		totalTimeLabel.setEnabled(pathActive && parsedModel != null && parsedModel.getType() == ModulesFile.STOCHASTIC);
		
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
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        innerButtonPanel = new javax.swing.JPanel();
        newPathButton = new javax.swing.JButton();
        resetPathButton = new javax.swing.JButton();
        exportPathButton = new javax.swing.JButton();
        configureViewButton = new javax.swing.JButton();
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
        stateLabelList = new GUISimLabelFormulaeList(this);
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
        pathTable = new javax.swing.JTable();
        pathTable = new GUISimulatorPathTable(this, pathTableModel, engine);

        innerButtonPanel.setLayout(new java.awt.GridLayout(2, 2, 10, 10));

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

        configureViewButton.setIcon(new javax.swing.ImageIcon(""));
        configureViewButton.setToolTipText("Export Path");
        configureViewButton.setActionCommand("Configure View");
        configureViewButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        configureViewButton.setLabel("Configure View");
        configureViewButton.setPreferredSize(new java.awt.Dimension(119, 28));
        configureViewButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configureViewButtonActionPerformed(evt);
            }
        });

        innerButtonPanel.add(configureViewButton);

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
        tabbedPane.setMinimumSize(new java.awt.Dimension(226, 0));
        tabbedPane.setPreferredSize(new java.awt.Dimension(285, 50));
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
        randomExplorationButton.setLabel("Random Exploration");
        randomExplorationButton.setMaximumSize(new java.awt.Dimension(220, 23));
        randomExplorationButton.setMinimumSize(new java.awt.Dimension(50, 23));
        randomExplorationButton.setPreferredSize(new java.awt.Dimension(160, 23));
        randomExplorationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
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
        inputExploreField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
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
        backtrackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
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
        inputBacktrackField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
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
        currentUpdatesTable.setToolTipText("Double click on an update to manually execute the update");
        manualUpdateTableScrollPane.setViewportView(currentUpdatesTable);

        innerManualUpdatesPanel.add(manualUpdateTableScrollPane, java.awt.BorderLayout.CENTER);

        autoTimeCheckPanel.setLayout(new java.awt.BorderLayout());

        autoTimeCheckPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 0, 0, 0));
        autoTimeCheck.setText("Generate time automatically");
        autoTimeCheck.setToolTipText("When not selected, you will be promted to enter the time spent in state manually");
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
        pathTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tableScroll.setViewportView(pathTable);

        bottomPanel.add(tableScroll, java.awt.BorderLayout.CENTER);

        outerBottomPanel.add(bottomPanel, java.awt.BorderLayout.CENTER);

        horizontalSplit.setBottomComponent(outerBottomPanel);

        allPanel.add(horizontalSplit, java.awt.BorderLayout.CENTER);

        add(allPanel, java.awt.BorderLayout.CENTER);

    }// </editor-fold>//GEN-END:initComponents

    private void inputBacktrackFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputBacktrackFieldActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_inputBacktrackFieldActionPerformed

    private void backtrackButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backtrackButtonActionPerformed

    	try
    	{        	
        	/* Update some given number of steps. */
			if (typeBacktrackCombo.getSelectedIndex() == 0)
			{
				int noSteps;
				
				try
				{
					if (inputBacktrackField.getText().trim().length() == 0)
						throw new NumberFormatException();
					
					noSteps = Integer.parseInt(inputBacktrackField.getText().trim());
					
					if (noSteps >= engine.getPathSize()) 
						noSteps = engine.getPathSize() - 1;
					
					if (noSteps < 0)
						throw new NumberFormatException();
					else if (noSteps == 0)
						return;				

					a_backTrack(engine.getPathSize() - noSteps - 1);
				}
				catch (NumberFormatException nfe)
				{
					throw new SimulatorException("The \"Steps\" parameter is invalid, it must be a positive integer smaller than the current path length (which is "+engine.getPathSize()+")");
				}
			}
			else if (typeBacktrackCombo.getSelectedIndex() == 1)
			{
				int toState;
				
				try
				{
					if (inputBacktrackField.getText().trim().length() == 0)
						throw new NumberFormatException();
					
					toState = Integer.parseInt(inputBacktrackField.getText().trim());
					
					if (toState < 0 || toState >= engine.getPathSize())
						throw new NumberFormatException();
					
					a_backTrack(toState);
				}
				catch (NumberFormatException nfe)
				{
					throw new SimulatorException("The \"Steps\" parameter is invalid, it must be a positive integer smaller than the current path length (which is "+engine.getPathSize()+")");
				}
			}	
			else if (typeBacktrackCombo.getSelectedIndex() == 2)
			{		
				double time;
				
				try
				{
					if (inputBacktrackField.getText().trim().length() == 0)
						throw new NumberFormatException();
					
					time = Double.parseDouble(inputBacktrackField.getText().trim());
										
					if (time < 0) 
						throw new NumberFormatException();
					
					if  (time >= engine.getTotalPathTime())
						a_backTrack(0.0d);
					else
						a_backTrack(engine.getTotalPathTime() - time);
				}
				catch (NumberFormatException nfe)
				{
					throw new SimulatorException("The \"Time\" parameter is invalid, it must be a positive double smaller than the cumulative path time (which is "+engine.getTotalPathTime()+")");
				}
			}	
			else if (typeBacktrackCombo.getSelectedIndex() == 3)
			{
				double time;
				
				try
				{
					if (inputBacktrackField.getText().trim().length() == 0)
						throw new NumberFormatException();
					
					time = Double.parseDouble(inputBacktrackField.getText().trim());
					
					if (time < 0 || time >= engine.getTotalPathTime()) 
						throw new NumberFormatException();
					
					a_backTrack(time);
				}
				catch (NumberFormatException nfe)
				{
					throw new SimulatorException("The \"Back to time\" parameter is invalid, it must be a positive double smaller than the cumulative path time (which is "+engine.getTotalPathTime()+")");
				}
			}	
    	}
    	catch (SimulatorException se)
    	{
    		this.error(se.getMessage());
    	}
    }//GEN-LAST:event_backtrackButtonActionPerformed

    private void configureViewButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configureViewButtonActionPerformed
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
		
		pathPopupMenu = new JPopupMenu();
		pathPopupMenu.add(newPath);
		pathPopupMenu.add(resetPath);
		pathPopupMenu.add(exportPath);		
		pathPopupMenu.addSeparator();
		pathPopupMenu.add(backtrackToHere);
		pathPopupMenu.add(removeToHere);
		pathPopupMenu.addSeparator();
		pathPopupMenu.add(configureView);
		
		simulatorMenu = new JMenu("Simulator");
		simulatorMenu.add(newPath);
		simulatorMenu.add(resetPath);
		simulatorMenu.add(exportPath);
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
		
    private void inputExploreFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputExploreFieldActionPerformed
	
    }//GEN-LAST:event_inputExploreFieldActionPerformed

    private void randomExplorationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_randomExplorationButtonActionPerformed

    	try
    	{        	
        	/* Update some given number of steps. */
			if (typeExploreCombo.getSelectedIndex() == 0)
			{
				int noSteps = 1;
				
				try
				{
					if (inputExploreField.getText().trim().length() == 0)
						throw new NumberFormatException();
					
					noSteps = Integer.parseInt(inputExploreField.getText().trim());
					
					if (noSteps <= 0) 
						throw new NumberFormatException();

					a_autoStep(noSteps);
				}
				catch (NumberFormatException nfe)
				{
					throw new SimulatorException("The \"Steps\" parameter is invalid, it must be a positive integer.");
				}
			}
			/* Update upto some state. */
			else if (typeExploreCombo.getSelectedIndex() == 1)
			{
				int uptoState;
				
				try
				{
					if (inputExploreField.getText().trim().length() == 0)
						throw new NumberFormatException();
					
					uptoState = Integer.parseInt(inputExploreField.getText().trim());
					
					if (uptoState < engine.getPathSize()) 
						throw new NumberFormatException();

					a_autoStep(uptoState - engine.getPathSize() + 1);
				}
				catch (NumberFormatException nfe)
				{
					throw new SimulatorException("The \"Up to state\" parameter is invalid, it must be a positive integer larger than the index of the current state (which is "+ (engine.getPathSize()-1)+")");
				}
			}
			else if (typeExploreCombo.getSelectedIndex() == 2)
			{
				double time;
				
				try
				{
					if (inputExploreField.getText().trim().length() == 0)
						throw new NumberFormatException();
					
					time = Double.parseDouble(inputExploreField.getText().trim());
					
					if (time < 0.0d) 
						throw new NumberFormatException();

					a_autoStep(time);
				}
				catch (NumberFormatException nfe)
				{
					throw new SimulatorException("The \"Time\" parameter is invalid, it must be a positive double");
				}
			}
			else if (typeExploreCombo.getSelectedIndex() == 3)
			{
				double time;
				double currentTime = engine.getTotalPathTime();
				
				try
				{
					if (inputExploreField.getText().trim().length() == 0)
						throw new NumberFormatException();
					
					time = Double.parseDouble(inputExploreField.getText().trim());
					
					if (time <= currentTime) 
						throw new NumberFormatException();

					a_autoStep(time-currentTime);
				}
				catch (NumberFormatException nfe)
				{
					throw new SimulatorException("The \"Time\" parameter is invalid, it must be a positive double larger than the cumulative path time (which is currently " + currentTime+")");
				}
			}
			
    	}
    	catch (SimulatorException se)
    	{
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
		
		if (e.getSource() == pathTable)
		{
			int row = pathTable.getSelectedRow();			
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
		return parsedModel;
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
			if (name == null)
			{
				return "" + (index + 1);
			}
			else
			{
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
			if (name != null)
			{
				return "" + (index + 1) + ": \"" + name + "\"";
			}
			else 
			{
				return "" + (index + 1) + ": <unnamed>"; 
			}
		}		
		
		public boolean equals(Object o)
		{
			return (o instanceof RewardStructure && ((RewardStructure)o).getIndex() == index && ((RewardStructure)o).isCumulative() == isCumulative());
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
    		switch (type)
    		{
    			case (STATE_REWARD) :
    				return rewardStructure.getColumnName();
    			case (TRANSITION_REWARD) :
    				return "[ " + rewardStructure.getColumnName() + " ]";
    			case (CUMULATIVE_REWARD) :
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
		private boolean showTime;
		private boolean showCumulativeTime;
				    	
    	public SimulationView()
    	{    		
    		this.visibleVariables = new ArrayList();
    		this.hiddenVariables = new ArrayList();
    		
    		this.visibleRewardColumns = new ArrayList();    		
    		this.rewards = new ArrayList();
    		
    		refreshToDefaultView();
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
		
		public boolean showTime() 
		{
        	return showTime && parsedModel.getType() == ModulesFile.STOCHASTIC;
        }
		
		public boolean showCumulativeTime()
		{
			return showCumulativeTime && parsedModel.getType() == ModulesFile.STOCHASTIC;
		}
		
		public boolean canShowTime()
		{
			return parsedModel.getType() == ModulesFile.STOCHASTIC;
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
			
			for (Object obj : visibleRewardListItems)
			{
				GUIViewDialog.RewardListItem item = (GUIViewDialog.RewardListItem)obj;
				if (item.isCumulative())
					visibleRewardColumns.add(new RewardStructureColumn(item.getRewardStructure(), GUISimulator.RewardStructureColumn.CUMULATIVE_REWARD));
				else
				{
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
		
		public void refreshToDefaultView()
		{
			visibleVariables.clear();
			hiddenVariables.clear();			
			visibleRewardColumns.clear();
			
			rewards.clear();
			
			if (pathActive)
			{
				try
				{
					stepsVisible = true;
					showTime = parsedModel.getType() == ModulesFile.STOCHASTIC;
					showCumulativeTime = parsedModel.getType() == ModulesFile.STOCHASTIC;;
					
					for (int i = 0; i < engine.getNumVariables(); i++)
					{
						visibleVariables.add(new Variable(i, engine.getVariableName(i), engine.getVariableType(i))); 
					}
									
					for (int r = 0; r < parsedModel.getNumRewardStructs(); r++)
					{
						parser.RewardStruct rewardStruct = parsedModel.getRewardStruct(r);
						String rewardName = rewardStruct.getName();
						
						if (rewardName.trim().length() == 0)
						{	rewardName = null; }
						
						RewardStructure rewardStructure = new RewardStructure(r, rewardName, parsedModel.getRewardStruct(r).getNumStateItems() == 0,  parsedModel.getRewardStruct(r).getNumTransItems() == 0);
						
						if (!rewardStructure.isStateEmpty() || !rewardStructure.isTransitionEmpty())		
							rewards.add(rewardStructure);
						
						if (!rewardStructure.isStateEmpty())
							visibleRewardColumns.add(new RewardStructureColumn(rewardStructure, RewardStructureColumn.STATE_REWARD));
						
						if (!rewardStructure.isTransitionEmpty())
							visibleRewardColumns.add(new RewardStructureColumn(rewardStructure, RewardStructureColumn.TRANSITION_REWARD));						
					}				
				}
				catch (SimulatorException e) {}
			}		
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
				
		public PathTableModel(SimulationView view)
		{	
			this.view = view;
			this.view.addObserver(this);
			
			rewardStructureValue = new RewardStructureValue(null , null);			
			variableValue = new VariableValue(null, null);			
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
				
				if (view.showSteps()) 
				{ groupCount++;	}
				
				if (view.showTime() || view.showCumulativeTime()) 
				{ groupCount++;	} 
				
				if (view.getVisibleVariables().size() > 0)
				{ groupCount++;	}				
				
				if (view.getVisibleRewardColumns().size() > 0)
				{ groupCount++;	} 
				
				return groupCount;				
			}			
        }
				
		public void update(Observable o, Object arg) 
		{	       
			if (o == view)
			{
				fireTableStructureChanged();
			
				//Sort out the minimum widths for each column
				sortOutColumnSizes();
			}
        }

		public String getGroupName(int groupIndex) 
		{
			if (!pathActive)
			{
				return "";
			}
			else
			{
				int groupCount = 0;
				
				if (view.showSteps()) 
				{ 					
					if (groupCount == groupIndex)
					{	return "Step";	}
					
					groupCount++;
				}
				
				if (view.showTime() || view.showCumulativeTime()) 
				{	
					if (groupCount == groupIndex) 
					{	return "Time";	}
					
					groupCount++;				
				}
				
				if (view.getVisibleVariables().size() > 0)
				{ 
					if (groupCount == groupIndex)
					{	return "Variables";	}
					
					groupCount++;
				}
				
				// Add state and transitions rewards for each reward structure.
				if (view.getVisibleRewardColumns().size() > 0)
					{
					if (groupCount == groupIndex)
					{	return "Rewards";	}
					
					groupCount++;				
				}
				
				return "Undefined Group";
			}		
        }		

		public String getGroupToolTip(int groupIndex) 
		{
			int groupCount = 0;
			
			if (view.showSteps()) 
			{ 					
				if (groupCount == groupIndex)
				{	return null;	}
				
				groupCount++;
			}
			
			if (view.showTime() || view.showCumulativeTime()) 
			{	
				if (groupCount == groupIndex) 
				{	return null;	}
				
				groupCount++;				
			}
			
			if (view.getVisibleVariables().size() > 0)
			{ 
				if (groupCount == groupIndex)
				{	return "Columns in this group represent variables of this model";	}
				
				groupCount++;
			}
			
			// Add state and transitions rewards for each reward structure.
			if (view.getVisibleRewardColumns().size() > 0)
				{
				if (groupCount == groupIndex)
				{	return "Columns in this group represent the state, transition and cumulative rewards of this model";	}
				
				groupCount++;				
			}
			
			return null;
		}

		public int getLastColumnOfGroup(int groupIndex) 
		{
			int stepStart = 0;
			int timeStart = stepStart + (view.showSteps() ? 1 : 0); 
			int varStart = timeStart + (view.showTime() ? 1 : 0) + (view.showCumulativeTime() ? 1 : 0);
			int rewardStart =  varStart + view.getVisibleVariables().size();
			
			int groupCount = 0;
						
			if (view.showSteps()) 
			{ 
				if (groupCount == groupIndex)
				{	return stepStart;	}
				
				groupCount++;				
			}
			
			if (view.showTime() || view.showCumulativeTime()) 
			{	
				if (groupCount == groupIndex)
				{	
					if (view.showTime() && view.showCumulativeTime())
						return timeStart + 1;
					else
						return timeStart;
				}
				
				groupCount++;
			} 
			
			if (view.getVisibleVariables().size() > 0)
			{ 
				if (groupCount == groupIndex)
				{	return varStart + view.getVisibleVariables().size() -1; }
				
				groupCount++;				
			}
			
			// Add state and transitions rewards for each reward structure.
			if (view.getVisibleRewardColumns().size() > 0)
			{
				if (groupCount == groupIndex)
				{	return rewardStart + view.getVisibleRewardColumns().size() - 1;	}
				
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
				int colCount = 0;
				
				colCount += (view.showSteps() ? 1 : 0);
				colCount += (view.showTime() ? 1 : 0) + (view.showCumulativeTime() ? 1 : 0);				
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
		
		public String getColumnName(int columnIndex)
		{			
			if(pathActive)
			{
				int stepStart = 0;
				int timeStart = stepStart + (view.showSteps() ? 1 : 0);
				int cumulativeTimeStart = timeStart + (view.showTime() ? 1 : 0);
				int varStart = cumulativeTimeStart + (view.showCumulativeTime() ? 1 : 0); 
				int rewardStart = varStart + view.getVisibleVariables().size();
				
				// The step column
				if (stepStart <= columnIndex && columnIndex < timeStart)
				{
					return "#";						
				}
				else if (timeStart <= columnIndex && columnIndex < cumulativeTimeStart)
				{
					return "Time";
				}
				else if (cumulativeTimeStart <= columnIndex && columnIndex < varStart)
				{
					return "Time (+)";
				}
				// A variable column
				else if (varStart <= columnIndex && columnIndex < rewardStart)
				{						
					return ((Variable)view.getVisibleVariables().get(columnIndex - varStart)).toString();						
				}
				
				else if (rewardStart <= columnIndex)
				{	
					return ((RewardStructureColumn)view.getVisibleRewardColumns().get(columnIndex - rewardStart)).getColumnName();					
				}
			}				
			return "Undefined Column";			
		}
		
		
		
		public String getColumnToolTip(int columnIndex) 
		{			
			if(pathActive)
			{
				int stepStart = 0;
				int timeStart = stepStart + (view.showSteps() ? 1 : 0);
				int cumulativeTimeStart = timeStart + (view.showTime() ? 1 : 0);
				int varStart = cumulativeTimeStart + (view.showCumulativeTime() ? 1 : 0); 
				int rewardStart = varStart + view.getVisibleVariables().size();
				
				// The step column
				if (stepStart <= columnIndex && columnIndex < timeStart)
				{
					return "The states of this path";						
				}
				else if (timeStart <= columnIndex && columnIndex < cumulativeTimeStart)
				{
					return "The time spent in a state";
				}
				else if (cumulativeTimeStart <= columnIndex && columnIndex < varStart)
				{
					return "The cumulative time spent in states";
				}
				// A variable column
				else if (varStart <= columnIndex && columnIndex < rewardStart)
				{						
					return "The values of variable \"" + ((Variable)view.getVisibleVariables().get(columnIndex - varStart)).toString() + "\"";						
				}
				
				else if (rewardStart <= columnIndex)
				{	
					RewardStructureColumn column = ((RewardStructureColumn)view.getVisibleRewardColumns().get(columnIndex - rewardStart));
					String rewardName = column.getRewardStructure().getColumnName();
					
					if (column.isStateReward())
						return "The state reward of reward structure " + rewardName;
					if (column.isTransitionReward())
						return "The transition reward of reward structure " + rewardName;
					if (column.isCumulativeReward())
						return "The cumulative reward of reward structure " + rewardName;								
				}
			}				
			return "Undefined Column";
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			if(pathActive)
			{
				int stepStart = 0;
				int timeStart = stepStart + (view.showSteps() ? 1 : 0);
				int cumulativeTimeStart = timeStart + (view.showTime() ? 1 : 0);
				int varStart = cumulativeTimeStart + (view.showCumulativeTime() ? 1 : 0); 
				int rewardStart = varStart + view.getVisibleVariables().size();
				
				// The step column
				if (stepStart <= columnIndex && columnIndex < timeStart)
				{
					return "" + rowIndex;						
				}
				
				else if (timeStart <= columnIndex && columnIndex < cumulativeTimeStart)					
				{					
					timeValue = new TimeValue(SimulatorEngine.getTimeSpentInPathState(rowIndex), false);					
					timeValue.setTimeValueUnknown(rowIndex >= SimulatorEngine.getPathSize() - 1);
					
					return timeValue;
				}					
				else if (cumulativeTimeStart <= columnIndex && columnIndex < varStart)
				{
					timeValue = new TimeValue(SimulatorEngine.getCumulativeTimeSpentInPathState(rowIndex), true);					
					timeValue.setTimeValueUnknown(rowIndex >= SimulatorEngine.getPathSize() - 1);
					
					return timeValue;					
				}
//					 A variable column
				else if (varStart <= columnIndex && columnIndex < rewardStart)
				{					
					Variable var = ((Variable)view.getVisibleVariables().get(columnIndex - varStart));
											
					int type = var.getType();
					int result = SimulatorEngine.getPathData(var.getIndex(), rowIndex);
					
					variableValue.setVariable(var);										
					variableValue.setValue((type == Expression.BOOLEAN) ? (new Boolean(result != 0)) : (new Integer(result)));
					variableValue.setChanged(rowIndex == 0 || SimulatorEngine.getPathData(var.getIndex(), rowIndex-1) != result);
					
					return variableValue;
				}
				else if (rewardStart <= columnIndex)
				{		
					RewardStructureColumn rewardColumn = (RewardStructureColumn)view.getVisibleRewardColumns().get(columnIndex - rewardStart);
					
					rewardStructureValue.setRewardStructureColumn(rewardColumn);
					rewardStructureValue.setRewardValueUnknown(false);
										
					if (rewardColumn.isStateReward())
					{					
						double value = SimulatorEngine.getStateRewardOfPathState(rowIndex, rewardColumn.getRewardStructure().getIndex());
						rewardStructureValue.setChanged(rowIndex == 0 || value != SimulatorEngine.getStateRewardOfPathState(rowIndex-1, rewardColumn.getRewardStructure().getIndex()));											
						rewardStructureValue.setRewardValue(new Double(value));					
						
						rewardStructureValue.setRewardValueUnknown(rowIndex > SimulatorEngine.getPathSize() - 1);
					}
					else if (rewardColumn.isTransitionReward())
					{					
						double value = SimulatorEngine.getTransitionRewardOfPathState(rowIndex, rewardColumn.getRewardStructure().getIndex());
						rewardStructureValue.setChanged(rowIndex == 0 || value != SimulatorEngine.getTransitionRewardOfPathState(rowIndex-1, rewardColumn.getRewardStructure().getIndex()));											
						rewardStructureValue.setRewardValue(new Double(value));	
						
						rewardStructureValue.setRewardValueUnknown(rowIndex >= SimulatorEngine.getPathSize() - 1);
					}
					else
					{					
						double value = SimulatorEngine.getTotalStateRewardOfPathState(rowIndex, rewardColumn.getRewardStructure().getIndex()) + SimulatorEngine.getTotalTransitionRewardOfPathState(rowIndex, rewardColumn.getRewardStructure().getIndex());
						rewardStructureValue.setChanged(rowIndex == 0 || value != (SimulatorEngine.getTotalStateRewardOfPathState(rowIndex-1, rewardColumn.getRewardStructure().getIndex()) + SimulatorEngine.getTotalTransitionRewardOfPathState(rowIndex -1, rewardColumn.getRewardStructure().getIndex())));											
						rewardStructureValue.setRewardValue(new Double(value));		
						
						rewardStructureValue.setRewardValueUnknown(rowIndex >= SimulatorEngine.getPathSize() - 1);
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
						if(parsedModel != null && parsedModel.getType() == ModulesFile.STOCHASTIC)
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
