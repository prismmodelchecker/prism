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
	
	private ArrayList hiddenColumns;
	
	
	//private int maxPathLength;
	
	//External state
	
	private boolean computing;
	
	
	//Options
	
	//private boolean verifyAllPropertiesAtOnce;
	//private boolean useAPMCForSimulation;
	//private GUISimulatorOptions options;
	private boolean displayStyleFast;
	//private boolean askForInitialState;
	private boolean displayPathLoops;
	
	//Actions
	private Action backtrackToHere, removeToHere;
	
	private JMenu showColumnMenu;
	
	
	/** Creates a new instance of GUISimulator */
	public GUISimulator(GUIPrism gui)
	{
		super(gui, true);
		pathTableModel = new PathTableModel();
		updateTableModel = new UpdateTableModel();
		
		hiddenColumns = new ArrayList();
		this.gui = gui;
		this.engine = gui.getPrism().getSimulator();
		initComponents();
		initPopups();
		
		pathTable.getSelectionModel().addListSelectionListener(this);
		
		pathTable.addMouseListener(this);
		pathTable.getTableHeader().addMouseListener(this);
		pathTable.getParent().addMouseListener(this);
		
		pathTable.getTableHeader().setReorderingAllowed(false);
		
		pathTable.addComponentListener(new ComponentListener()
		{
			public void componentHidden(ComponentEvent e)
			{
			}
			
			public void componentMoved(ComponentEvent e)
			{
			}
			
			public void componentResized(ComponentEvent e)
			{
				sortOutColumnSizes();
			}
			
			public void componentShown(ComponentEvent e)
			{
			}
			
			
		});
		
		
		pathActive = false;
		engineBuilt = false;
		doEnables();
		
		//useAPMCForSimulation = false;
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
		
		jScrollPane1.setRowHeaderView(((GUISimulatorPathTable)pathTable).getPathRowHeader());
		jScrollPane2.setRowHeaderView(((GUISimulatorUpdatesTable)currentUpdatesTable).getUpdateRowHeader());
		
		jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
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
		pathLengthParameterLabel.setText("0");
		totalRewardLabel.setText("");
		//maximumLengthLabel.setText(""+gui.getPrism().getApmcPathLen());
		
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
			//maximumLengthLabel.setText(""+gui.getPrism().getApmcPathLen());
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
			pathLengthParameterLabel.setText(""+(engine.getPathSize()-1));
			totalRewardLabel.setText(getTotalRewardLabelString());
			stateTotalRewardsLabel.setText(getTotalStateRewardLabelString());
			transitionTotalRewardsLabel.setText(getTotalTransitionRewardLabelString());
			definedConstantsLabel.setText(uCon.getDefinedConstantsString());
			
			doEnables();
			pathTableModel.restartPathTable();
			updateTableModel.restartUpdatesTable();
			
			pathTable.getSelectionModel().setSelectionInterval(pathTable.getRowCount()-1, pathTable.getRowCount()-1);
			
			repopulateFormulae();
			stateLabelList.repaint();
			pathFormulaeList.repaint();
			//pathTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			
			// store inital state for next time
			lastInitialState = initialState;
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
			pathLengthParameterLabel.setText(""+(engine.getPathSize()-1));
			totalRewardLabel.setText(getTotalRewardLabelString());
			stateTotalRewardsLabel.setText(getTotalStateRewardLabelString());
			transitionTotalRewardsLabel.setText(getTotalTransitionRewardLabelString());
			
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
		pathLengthParameterLabel.setText(""+(engine.getPathSize()-1));
		totalRewardLabel.setText(getTotalRewardLabelString());
		stateTotalRewardsLabel.setText(getTotalStateRewardLabelString());
		transitionTotalRewardsLabel.setText(getTotalTransitionRewardLabelString());
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
			pathLengthParameterLabel.setText(""+(engine.getPathSize()-1));
			totalRewardLabel.setText(getTotalRewardLabelString());
			stateTotalRewardsLabel.setText(getTotalStateRewardLabelString());
			transitionTotalRewardsLabel.setText(getTotalTransitionRewardLabelString());
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
		pathLengthParameterLabel.setText(""+(engine.getPathSize()-1));
		totalRewardLabel.setText(getTotalRewardLabelString());
		stateTotalRewardsLabel.setText(getTotalStateRewardLabelString());
		transitionTotalRewardsLabel.setText(getTotalTransitionRewardLabelString());
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
				pathLengthParameterLabel.setText(""+(engine.getPathSize()-1));
				totalRewardLabel.setText(getTotalRewardLabelString());
				stateTotalRewardsLabel.setText(getTotalStateRewardLabelString());
				transitionTotalRewardsLabel.setText(getTotalTransitionRewardLabelString());
				
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
				pathLengthParameterLabel.setText(""+(engine.getPathSize()-1));
				totalRewardLabel.setText(getTotalRewardLabelString());
				stateTotalRewardsLabel.setText(getTotalStateRewardLabelString());
				transitionTotalRewardsLabel.setText(getTotalTransitionRewardLabelString());
				
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
	 * Getter for property useAPMCForSimulation.
	 * @return Value of property useAPMCForSimulation.
	 */
	public boolean isUseAPMCForSimulation()
	{
		return getPrism().getSettings().getString(PrismSettings.SIMULATOR_ENGINE).equals("APMC");
		//return useAPMCForSimulation;
	}
	
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
	 * Setter for property useAPMCForSimulation.
	 * @param useAPMCForSimulation New value of property useAPMCForSimulation.
	 */
	public void setUseAPMCForSimulation(boolean useAPMC) throws PrismException
	{
		getPrism().getSettings().set(PrismSettings.SIMULATOR_ENGINE, new Integer(useAPMC?1:0));
		//this.useAPMCForSimulation = useAPMC;
	}
	
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
		stateTimeField.setEnabled(pathActive && mf != null && mf.getType() == ModulesFile.STOCHASTIC);
		autoTimeCheck.setEnabled(pathActive && mf != null && mf.getType() == ModulesFile.STOCHASTIC);
		jLabel2.setEnabled(pathActive);
		jLabel1.setEnabled(pathActive);
		jLabel3.setEnabled(pathActive);
		jLabel4.setEnabled(pathActive);
		jLabel5.setEnabled(pathActive);
		
		resetPathButton.setEnabled(pathActive && !computing);
		exportPathButton.setEnabled(pathActive && !computing);
		
		newPathButton.setEnabled(mf != null && !computing);
		
		currentUpdatesTable.setEnabled(pathActive);
		
		jLabel6.setEnabled(mf != null);
		modelTypeLabel.setEnabled(mf != null);
		
		totalTimeLabel.setEnabled(pathActive && mf != null && mf.getType() == ModulesFile.STOCHASTIC);
		pathTimeLabel.setEnabled(pathActive && mf != null && mf.getType() == ModulesFile.STOCHASTIC);
		
		pathLengthLabel.setEnabled(pathActive);
		pathLengthParameterLabel.setEnabled(pathActive);
		
		jLabel9.setEnabled(pathActive);
		totalRewardLabel.setEnabled(pathActive);
		
		jLabel13.setEnabled(pathActive);
		stateTotalRewardsLabel.setEnabled(pathActive);
		
		spareLabel.setEnabled(pathActive);
		transitionTotalRewardsLabel.setEnabled(pathActive);
		
		jLabel10.setEnabled(pathActive);
		definedConstantsLabel.setEnabled(pathActive);
		
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
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel3 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
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
        jScrollPane2 = new javax.swing.JScrollPane();
        currentUpdatesTable = new javax.swing.JTable();
        currentUpdatesTable = new GUISimulatorUpdatesTable(updateTableModel, this);
        jLabel5 = new javax.swing.JLabel();
        Font f = jLabel5.getFont();

        jLabel5.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
        jPanel5 = new javax.swing.JPanel();
        jPanel23 = new javax.swing.JPanel();
        autoUpdateButton = new javax.swing.JButton();
        jPanel17 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel19 = new javax.swing.JPanel();
        autoUpdateField = new javax.swing.JTextField();
        jPanel52 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
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
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        pathTable = new javax.swing.JTable();
        pathTable = new GUISimulatorPathTable(pathTableModel, engine);
        jPanel24 = new javax.swing.JPanel();
        newPathButton = new javax.swing.JButton();
        resetPathButton = new javax.swing.JButton();
        exportPathButton = new javax.swing.JButton();
        jPanel29 = new javax.swing.JPanel();
        jPanel31 = new javax.swing.JPanel();
        jPanel32 = new javax.swing.JPanel();
        jPanel33 = new javax.swing.JPanel();
        jPanel34 = new javax.swing.JPanel();
        jPanel35 = new javax.swing.JPanel();
        jPanel36 = new javax.swing.JPanel();
        jPanel54 = new javax.swing.JPanel();
        jPanel30 = new javax.swing.JPanel();
        jPanel39 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jPanel40 = new javax.swing.JPanel();
        modelTypeLabel = new javax.swing.JLabel();
        jPanel41 = new javax.swing.JPanel();
        jPanel42 = new javax.swing.JPanel();
        stateTotalRewardsLabel = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jPanel38 = new javax.swing.JPanel();
        jPanel43 = new javax.swing.JPanel();
        jPanel44 = new javax.swing.JPanel();
        jPanel45 = new javax.swing.JPanel();
        pathLengthLabel = new javax.swing.JLabel();
        pathLengthParameterLabel = new javax.swing.JLabel();
        jPanel46 = new javax.swing.JPanel();
        spareLabel = new javax.swing.JLabel();
        transitionTotalRewardsLabel = new javax.swing.JLabel();
        jPanel47 = new javax.swing.JPanel();
        jPanel48 = new javax.swing.JPanel();
        jPanel49 = new javax.swing.JPanel();
        jPanel50 = new javax.swing.JPanel();
        jPanel51 = new javax.swing.JPanel();
        totalTimeLabel = new javax.swing.JLabel();
        pathTimeLabel = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        totalRewardLabel = new javax.swing.JLabel();
        jPanel55 = new javax.swing.JPanel();
        jPanel56 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jPanel57 = new javax.swing.JPanel();
        definedConstantsLabel = new javax.swing.JLabel();
        jPanel58 = new javax.swing.JPanel();
        jPanel37 = new javax.swing.JPanel();
        jPanel59 = new javax.swing.JPanel();
        jPanel60 = new javax.swing.JPanel();

        setLayout(new java.awt.BorderLayout());

        jSplitPane1.setDividerLocation(302);
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel1.setMinimumSize(new java.awt.Dimension(300, 210));
        jPanel1.setPreferredSize(new java.awt.Dimension(302, 591));
        jSplitPane2.setDividerLocation(400);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane2.setResizeWeight(0.5);
        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel3.setMinimumSize(new java.awt.Dimension(302, 227));
        jPanel3.setPreferredSize(new java.awt.Dimension(302, 554));
        jPanel11.setLayout(new java.awt.BorderLayout());

        jPanel6.setLayout(new java.awt.GridBagLayout());

        jPanel6.setBorder(new javax.swing.border.TitledBorder("Exploration"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        jPanel6.add(jPanel7, gridBagConstraints);

        manualUpdateField.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/smallManualUpdate.gif")));
        manualUpdateField.setText("Do Update");
        manualUpdateField.setToolTipText("Perform the selected update");
        manualUpdateField.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        manualUpdateField.setMaximumSize(new java.awt.Dimension(112, 25));
        manualUpdateField.setPreferredSize(new java.awt.Dimension(112, 25));
        manualUpdateField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                manualUpdateFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel6.add(manualUpdateField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel6.add(jPanel8, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        jPanel6.add(jPanel9, gridBagConstraints);

        stateTimeField.setText("1.0");
        stateTimeField.setToolTipText("Enter the time spent in the current state");
        stateTimeField.setPreferredSize(new java.awt.Dimension(60, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(stateTimeField, gridBagConstraints);

        jLabel1.setText("State time:");
        jLabel1.setMinimumSize(new java.awt.Dimension(57, 15));
        jLabel1.setPreferredSize(new java.awt.Dimension(57, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel1, gridBagConstraints);

        jPanel10.setMinimumSize(new java.awt.Dimension(10, 5));
        jPanel10.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        jPanel6.add(jPanel10, gridBagConstraints);

        autoTimeCheck.setText("Auto");
        autoTimeCheck.setToolTipText("Automatically sample time from a negative exponential distribution");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(autoTimeCheck, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        jPanel6.add(jPanel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel6.add(jPanel15, gridBagConstraints);

        jScrollPane2.setPreferredSize(new java.awt.Dimension(100, 100));
        currentUpdatesTable.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane2.setViewportView(currentUpdatesTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel6.add(jScrollPane2, gridBagConstraints);

        jLabel5.setText("Current Updates:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel5, gridBagConstraints);

        jPanel5.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        jPanel6.add(jPanel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 16;
        jPanel6.add(jPanel23, gridBagConstraints);

        autoUpdateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/smallAutomaticUpdate.gif")));
        autoUpdateButton.setText("Auto Update");
        autoUpdateButton.setToolTipText("Make a number of automatic updates");
        autoUpdateButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        autoUpdateButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                autoUpdateButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel6.add(autoUpdateButton, gridBagConstraints);

        jPanel17.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        jPanel6.add(jPanel17, gridBagConstraints);

        jLabel2.setText("No. Steps:");
        jLabel2.setPreferredSize(new java.awt.Dimension(57, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        jPanel6.add(jPanel19, gridBagConstraints);

        autoUpdateField.setText("1");
        autoUpdateField.setToolTipText("Enter the number of automatic steps");
        autoUpdateField.setPreferredSize(new java.awt.Dimension(60, 19));
        autoUpdateField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                autoUpdateFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel6.add(autoUpdateField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 0;
        jPanel6.add(jPanel52, gridBagConstraints);

        jPanel11.add(jPanel6, java.awt.BorderLayout.CENTER);

        jPanel12.setLayout(new java.awt.GridBagLayout());

        jPanel12.setBorder(new javax.swing.border.TitledBorder("Path Modification"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel12.add(jPanel14, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        jPanel12.add(jPanel16, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel12.add(jPanel18, gridBagConstraints);

        backtrackButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/smallBacktrack.gif")));
        backtrackButton.setText("Backtrack");
        backtrackButton.setToolTipText("Backtrack to the given step.");
        backtrackButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        backtrackButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                backtrackButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel12.add(backtrackButton, gridBagConstraints);

        jLabel3.setText("To Step:");
        jLabel3.setPreferredSize(new java.awt.Dimension(57, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel12.add(jLabel3, gridBagConstraints);

        jPanel20.setNextFocusableComponent(jPanel9);
        jPanel20.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        jPanel12.add(jPanel20, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel12.add(jPanel21, gridBagConstraints);

        removePrecedingButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/smallRemovePreceding.gif")));
        removePrecedingButton.setText("Remove");
        removePrecedingButton.setToolTipText("Remove all before the given step");
        removePrecedingButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        removePrecedingButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                removePrecedingButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel12.add(removePrecedingButton, gridBagConstraints);

        jLabel4.setText("Before:");
        jLabel4.setPreferredSize(new java.awt.Dimension(57, 15));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel12.add(jLabel4, gridBagConstraints);

        backTrackStepField.setText("0");
        backTrackStepField.setToolTipText("Enter the step to backtrack to");
        backTrackStepField.setPreferredSize(new java.awt.Dimension(60, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel12.add(backTrackStepField, gridBagConstraints);

        removePrecedingField.setText("0");
        removePrecedingField.setToolTipText("Enter the step to become the first step");
        removePrecedingField.setPreferredSize(new java.awt.Dimension(60, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel12.add(removePrecedingField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        jPanel12.add(jPanel22, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        jPanel12.add(jPanel25, gridBagConstraints);

        jPanel11.add(jPanel12, java.awt.BorderLayout.SOUTH);

        jPanel3.add(jPanel11, java.awt.BorderLayout.CENTER);

        jSplitPane2.setLeftComponent(jPanel3);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(new javax.swing.border.TitledBorder("Formulae"));
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
        jSplitPane3.setDividerSize(5);
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

        jSplitPane2.setRightComponent(jPanel4);

        jPanel1.add(jSplitPane2, java.awt.BorderLayout.CENTER);

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel2.setBorder(new javax.swing.border.TitledBorder("Simulation Path"));
        pathTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
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
        jScrollPane1.setViewportView(pathTable);

        jPanel2.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel24.setLayout(new java.awt.GridBagLayout());

        newPathButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/smallNewPath.gif")));
        newPathButton.setText("New Path");
        newPathButton.setToolTipText("New Path");
        newPathButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        newPathButton.setPreferredSize(new java.awt.Dimension(119, 28));
        newPathButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                newPathButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel24.add(newPathButton, gridBagConstraints);

        resetPathButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/smallResetPath.gif")));
        resetPathButton.setText("Reset Path");
        resetPathButton.setToolTipText("Reset Path");
        resetPathButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        resetPathButton.setPreferredSize(new java.awt.Dimension(119, 28));
        resetPathButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                resetPathButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        jPanel24.add(resetPathButton, gridBagConstraints);

        exportPathButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/smallExport.gif")));
        exportPathButton.setText("Export Path");
        exportPathButton.setToolTipText("Export Path");
        exportPathButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        exportPathButton.setPreferredSize(new java.awt.Dimension(119, 28));
        exportPathButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                exportPathButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel24.add(exportPathButton, gridBagConstraints);

        jPanel29.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel24.add(jPanel29, gridBagConstraints);

        jPanel31.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        jPanel24.add(jPanel31, gridBagConstraints);

        jPanel32.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        jPanel24.add(jPanel32, gridBagConstraints);

        jPanel33.setPreferredSize(new java.awt.Dimension(10, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        jPanel24.add(jPanel33, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        jPanel24.add(jPanel34, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        jPanel24.add(jPanel35, gridBagConstraints);

        jPanel36.setLayout(new javax.swing.BoxLayout(jPanel36, javax.swing.BoxLayout.Y_AXIS));

        jPanel36.setBorder(new javax.swing.border.EtchedBorder());
        jPanel54.setLayout(new java.awt.GridLayout(1, 3));

        jPanel30.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel30.add(jPanel39, gridBagConstraints);

        jLabel6.setText("Model Type:");
        f = jLabel6.getFont();
        jLabel6.setFont(new java.awt.Font(f.getName(), Font.BOLD, f.getSize()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel30.add(jLabel6, gridBagConstraints);

        jPanel40.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        jPanel30.add(jPanel40, gridBagConstraints);

        modelTypeLabel.setText("Unknown");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel30.add(modelTypeLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanel30.add(jPanel41, gridBagConstraints);

        jPanel42.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        jPanel30.add(jPanel42, gridBagConstraints);

        stateTotalRewardsLabel.setText("0.0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel30.add(stateTotalRewardsLabel, gridBagConstraints);

        jLabel13.setText("State Rewards:");
        f = jLabel13.getFont();
        jLabel13.setFont(new java.awt.Font(f.getName(), Font.BOLD, f.getSize()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel30.add(jLabel13, gridBagConstraints);

        jPanel54.add(jPanel30);

        jPanel38.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel38.add(jPanel43, gridBagConstraints);

        jPanel44.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        jPanel38.add(jPanel44, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanel38.add(jPanel45, gridBagConstraints);

        pathLengthLabel.setText("Path Length:");
        f = pathLengthLabel.getFont();
        pathLengthLabel.setFont(new java.awt.Font(f.getName(), Font.BOLD, f.getSize()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel38.add(pathLengthLabel, gridBagConstraints);

        pathLengthParameterLabel.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel38.add(pathLengthParameterLabel, gridBagConstraints);

        jPanel46.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        jPanel38.add(jPanel46, gridBagConstraints);

        spareLabel.setText("Transition Rewards:");
        f = spareLabel.getFont();
        spareLabel.setFont(new java.awt.Font(f.getName(), Font.BOLD, f.getSize()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel38.add(spareLabel, gridBagConstraints);

        transitionTotalRewardsLabel.setText("0.0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel38.add(transitionTotalRewardsLabel, gridBagConstraints);

        jPanel54.add(jPanel38);

        jPanel47.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel47.add(jPanel48, gridBagConstraints);

        jPanel49.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        jPanel47.add(jPanel49, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanel47.add(jPanel50, gridBagConstraints);

        jPanel51.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        jPanel47.add(jPanel51, gridBagConstraints);

        totalTimeLabel.setText("0.0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel47.add(totalTimeLabel, gridBagConstraints);

        pathTimeLabel.setText("Total Time:");
        f = jLabel6.getFont();
        pathTimeLabel.setFont(new java.awt.Font(f.getName(), Font.BOLD, f.getSize()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel47.add(pathTimeLabel, gridBagConstraints);

        jLabel9.setText("Total Reward:");
        f = jLabel9.getFont();
        jLabel9.setFont(new java.awt.Font(f.getName(), Font.BOLD, f.getSize()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel47.add(jLabel9, gridBagConstraints);

        totalRewardLabel.setText("0.0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel47.add(totalRewardLabel, gridBagConstraints);

        jPanel54.add(jPanel47);

        jPanel36.add(jPanel54);

        jPanel55.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel55.add(jPanel56, gridBagConstraints);

        jLabel10.setText("Defined Constants:");
        f = jLabel10.getFont();
        jLabel10.setFont(new java.awt.Font(f.getName(), Font.BOLD, f.getSize()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel55.add(jLabel10, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        jPanel55.add(jPanel57, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel55.add(definedConstantsLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        jPanel55.add(jPanel58, gridBagConstraints);

        jPanel36.add(jPanel55);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel24.add(jPanel36, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 9;
        jPanel24.add(jPanel37, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 0.5;
        jPanel24.add(jPanel59, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.weighty = 0.5;
        jPanel24.add(jPanel60, gridBagConstraints);

        jPanel2.add(jPanel24, java.awt.BorderLayout.NORTH);

        jSplitPane1.setRightComponent(jPanel2);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);

    }//GEN-END:initComponents
	
	private void initPopups()
	{
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
		
		//item to dynamically add checkboxes for each column
		showColumnMenu = new JMenu("Show Column");
		
		pathPopup = new JPopupMenu();
		
		pathPopup.add(showColumnMenu);
		pathPopup.addSeparator();
		pathPopup.add(backtrackToHere);
		pathPopup.add(removeToHere);
		
		
		
		
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
				
				int index = pathTable.rowAtPoint(e.getPoint());
				
				pathTable.getSelectionModel().setSelectionInterval(index, index);				
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
    private javax.swing.ButtonGroup buttonGroup1;
    javax.swing.JTable currentUpdatesTable;
    private javax.swing.JLabel definedConstantsLabel;
    javax.swing.JButton exportPathButton;
    javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel13;
    javax.swing.JLabel jLabel2;
    javax.swing.JLabel jLabel3;
    javax.swing.JLabel jLabel4;
    javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel31;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel33;
    private javax.swing.JPanel jPanel34;
    private javax.swing.JPanel jPanel35;
    private javax.swing.JPanel jPanel36;
    private javax.swing.JPanel jPanel37;
    private javax.swing.JPanel jPanel38;
    private javax.swing.JPanel jPanel39;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel40;
    private javax.swing.JPanel jPanel41;
    private javax.swing.JPanel jPanel42;
    private javax.swing.JPanel jPanel43;
    private javax.swing.JPanel jPanel44;
    private javax.swing.JPanel jPanel45;
    private javax.swing.JPanel jPanel46;
    private javax.swing.JPanel jPanel47;
    private javax.swing.JPanel jPanel48;
    private javax.swing.JPanel jPanel49;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel50;
    private javax.swing.JPanel jPanel51;
    private javax.swing.JPanel jPanel52;
    private javax.swing.JPanel jPanel53;
    private javax.swing.JPanel jPanel54;
    private javax.swing.JPanel jPanel55;
    private javax.swing.JPanel jPanel56;
    private javax.swing.JPanel jPanel57;
    private javax.swing.JPanel jPanel58;
    private javax.swing.JPanel jPanel59;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel60;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSplitPane jSplitPane3;
    javax.swing.JButton manualUpdateField;
    private javax.swing.JLabel modelTypeLabel;
    javax.swing.JButton newPathButton;
    javax.swing.JList pathFormulaeList;
    private javax.swing.JLabel pathLengthLabel;
    private javax.swing.JLabel pathLengthParameterLabel;
    private javax.swing.JTable pathTable;
    private javax.swing.JLabel pathTimeLabel;
    javax.swing.JButton removePrecedingButton;
    javax.swing.JTextField removePrecedingField;
    javax.swing.JButton resetPathButton;
    private javax.swing.JLabel spareLabel;
    private javax.swing.JList stateLabelList;
    javax.swing.JTextField stateTimeField;
    private javax.swing.JLabel stateTotalRewardsLabel;
    private javax.swing.JLabel totalRewardLabel;
    private javax.swing.JLabel totalTimeLabel;
    private javax.swing.JLabel transitionTotalRewardsLabel;
    // End of variables declaration//GEN-END:variables
	
	class PathTableModel extends AbstractTableModel
	{
		
		public int getColumnCount()
		{
			int colCount;
			if(pathActive)
				colCount = 1+engine.getNumVariables();
			else colCount = 0;
			
			if(mf == null) return colCount;
			if(mf.getType() == ModulesFile.STOCHASTIC)
				colCount +=1;
			
			colCount += (2*mf.getNumRewardStructs());
			
			return colCount;
		}
		
		public int getRowCount()
		{
			
			if(pathActive)
				return engine.getPathSize();
			else return 0;
			
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
					if (columnIndex == 0) return ""+rowIndex;
					if (mf == null) return null;
					if(mf.getType() == ModulesFile.STOCHASTIC)
					{
						int n = engine.getNumVariables();
						if(columnIndex == n+1) //where time should be
						{
							return new Double(engine.getTimeSpentInPathState(rowIndex));
						}
						else if(columnIndex > n+1) //rewards
						{
							int i = columnIndex-(n+2);
							if (i%2 == 0) return new Double(engine.getStateRewardOfPathState(rowIndex,i/2));
							else return new Double(engine.getTransitionRewardOfPathState(rowIndex,i/2));
						}
					}
					else
					{
						int n = engine.getNumVariables();
						if(columnIndex > n)
						{
							int i = columnIndex-(n+1);
							if (i%2 == 0) return new Double(engine.getStateRewardOfPathState(rowIndex,i/2));
							else return new Double(engine.getTransitionRewardOfPathState(rowIndex,i/2));
						}
					}
					int type = engine.getVariableType(columnIndex-1);
					int result = engine.getPathData(columnIndex-1, rowIndex);
					if(type == Expression.BOOLEAN)
					{
						if(result == 0) return new Boolean(false);
						else return new Boolean(true);
					}
					else
						return new Integer(result);
				}
				else return null;
			}
			catch(SimulatorException e)
			{
				//System.out.println("Catching a simulator exception");
				return null;
			}
		}
		
		public String getColumnName(int column)
		{
			try
			{
				if(pathActive)
				{
					if (column == 0) return "Step";
					if (mf == null) return null;
					if (mf.getType() == ModulesFile.STOCHASTIC)
					{
						int n = engine.getNumVariables();
						if(column == n+1) //where time should be
						{
							return "Time";
						}
						else if(column > n+1) //rewards
						{
							int i = column-(n+2);
							return "" + ((i%2 == 0)?"Sta.":"Tra.") + " Rew. "+((i/2)+1);
						}
					}
					else
					{
						int n = engine.getNumVariables();
						if(column > n) //rewards
						{
							int i = column-(n+1);
							return "" + ((i%2 == 0)?"Sta.":"Tra.") + " Rew. "+((i/2)+1);
						}
					}
					return engine.getVariableName(column-1);
				}
				else return "";
			}
			catch(SimulatorException e)
			{
				return "";
			}
		}
		
		public void restartPathTable()
		{
			fireTableStructureChanged();
			//Sort out the minimum widths for each column
			sortOutColumnSizes();
		}
		
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
		
		if(50*cm.getColumnCount() > jScrollPane1.getWidth())
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
