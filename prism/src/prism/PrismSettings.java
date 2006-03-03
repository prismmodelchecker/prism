//==============================================================================
//
//	Copyright (c) 2002-2005, Dave Parker, Andrew Hinton
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

package prism;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.lang.Comparable;
import javax.swing.*;

import settings.*;


public class PrismSettings implements Observer
{
	
	//Default Constants
	public static final String DEFAULT_STRING = "";
	public static final int DEFAULT_INT = 0;
	public static final double DEFAULT_DOUBLE = 0.0;
	public static final float DEFAULT_FLOAT = 0.0f;
	public static final long DEFAULT_LONG = 0l;
	public static final boolean DEFAULT_BOOLEAN = false;
	public static final Color DEFAULT_COLOUR = Color.white;
	public static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);
	public static final FontColorPair DEFAULT_FONT_COLOUR = new FontColorPair(new Font("monospaced", Font.PLAIN, 12), Color.black);
	public static final File DEFAULT_FILE = null;
	
	//Type Constants
	public static final String STRING_TYPE = "s";
	public static final String INTEGER_TYPE = "i";
	public static final String FLOAT_TYPE = "f";
	public static final String DOUBLE_TYPE = "d";
	public static final String LONG_TYPE = "l";
	public static final String BOOLEAN_TYPE = "b";
	public static final String COLOUR_TYPE = "c";
	//public static final String FONT_TYPE = "fo";	  //remove for now cos we don't have a setting for this yet
	public static final String CHOICE_TYPE = "ch";
	public static final String FONT_COLOUR_TYPE = "fct";
	public static final String FILE_TYPE = "fi";
	
	//Property Constant Keys
	//======================
	
	//Prism
	public static final	String PRISM_ENGINE							= "prism.engine";
	public static final	String PRISM_VERBOSE						= "prism.verbose";
	public static final	String PRISM_FAIRNESS						= "prism.fairness";
	public static final	String PRISM_PRECOMPUTATION					= "prism.precomputation";
	public static final	String PRISM_DO_PROB_CHECKS					= "prism.doProbChecks";
	public static final	String PRISM_COMPACT						= "prism.compact";
	public static final	String PRISM_LIN_EQ_METHOD					= "prism.linEqMethod";//"prism.iterativeMethod";
	public static final	String PRISM_LIN_EQ_METHOD_PARAM			= "prism.linEqMethodParam";//"prism.overRelaxation";
	public static final	String PRISM_TERM_CRIT						= "prism.termCrit";//"prism.termination";
	public static final	String PRISM_TERM_CRIT_PARAM				= "prism.termCritParam";//"prism.terminationEpsilon";
	public static final	String PRISM_MAX_ITERS						= "prism.maxIters";//"prism.maxIterations";
	public static final	String PRISM_CUDD_MAX_MEM					= "prism.cuddMaxMem";
	public static final	String PRISM_CUDD_EPSILON					= "prism.cuddEpsilon";
	public static final	String PRISM_NUM_SB_LEVELS					= "prism.numSBLevels";//"prism.hybridNumLevels";
	public static final	String PRISM_SB_MAX_MEM						= "prism.SBMaxMem";//"prism.hybridMaxMemory";
	public static final	String PRISM_NUM_SOR_LEVELS					= "prism.numSORLevels";//"prism.hybridSORLevels";
	public static final	String PRISM_SOR_MAX_MEM					= "prism.SORMaxMem";//"prism.hybridSORMaxMemory";
	
	//GUI Model
	public static final	String MODEL_AUTO_PARSE						= "model.autoParse";
	public static final	String MODEL_AUTO_MANUAL					= "model.autoManual";
	public static final	String MODEL_PARSE_DELAY					= "model.parseDelay";
	public static final	String MODEL_PRISM_EDITOR_FONT				= "model.prismEditor.font";
	//public static final	String MODEL_PRISM_EDITOR_FONT_COLOUR		= "model.prismEditor.fontColour";
	public static final	String MODEL_PRISM_EDITOR_BG_COLOUR			= "model.prismEditor.bgColour";
	public static final	String MODEL_PRISM_EDITOR_NUMERIC_COLOUR	= "model.prismEditor.numericColour";
	public static final	String MODEL_PRISM_EDITOR_NUMERIC_STYLE		= "model.prismEditor.numericStyle";
	public static final	String MODEL_PRISM_EDITOR_IDENTIFIER_COLOUR	  =	"model.prismEditor.identifierColour";
	public static final	String MODEL_PRISM_EDITOR_IDENTIFIER_STYLE	  =	"model.prismEditor.identifierStyle";
	public static final	String MODEL_PRISM_EDITOR_KEYWORD_COLOUR	= "model.prismEditor.keywordColour";
	public static final	String MODEL_PRISM_EDITOR_KEYWORD_STYLE		= "model.prismEditor.keywordStyle";
	public static final	String MODEL_PRISM_EDITOR_COMMENT_COLOUR	= "model.prismEditor.commentColour";
	public static final	String MODEL_PRISM_EDITOR_COMMENT_STYLE		= "model.prismEditor.commentStyle";
	public static final	String MODEL_PEPA_EDITOR_FONT				= "model.pepaEditor.font";
	//public static final	String MODEL_PEPA_EDITOR_FONT_COLOUR		= "model.pepaEditor.fontColour";
	public static final	String MODEL_PEPA_EDITOR_BG_COLOUR			= "model.pepaEditor.bgColour";
	public static final	String MODEL_PEPA_EDITOR_COMMENT_COLOUR		= "model.pepaEditor.commentColour";
	public static final	String MODEL_PEPA_EDITOR_COMMENT_STYLE		= "model.pepaEditor.commentStyle";
	
	//GUI Properties
	public static final	String PROPERTIES_FONT						= "properties.font";
	public static final	String PROPERTIES_SELECTION_COLOUR			= "properties.selectionColour";
	public static final	String PROPERTIES_WARNING_COLOUR			= "properties.warningColour";
	public static final	String PROPERTIES_ADDITION_STRATEGY			= "properties.additionStategy";
	public static final	String PROPERTIES_CLEAR_LIST_ON_LOAD		= "properties.clearListOnLoad";
	
	//GUI Simulator
	public static final	String SIMULATOR_ENGINE						= "simulator.engine";
	public static final String SIMULATOR_DEFAULT_APPROX				= "simulator.defaultApprox";
	public static final String SIMULATOR_DEFAULT_CONFIDENCE			= "simulator.defaultConfidence";
	public static final String SIMULATOR_DEFAULT_NUM_SAMPLES		= "simulator.defaultNumSamples";
	public static final String SIMULATOR_DEFAULT_MAX_PATH			= "simulator.defaultMaxPath";
	public static final	String SIMULATOR_SIMULTANEOUS				= "simulator.simultaneous";
	public static final String SIMULATOR_FIELD_CHOICE				= "simulator.fieldChoice";
	public static final	String SIMULATOR_NEW_PATH_ASK_INITIAL		= "simulator.newPathAskDefault";
	public static final	String SIMULATOR_RENDER_ALL_VALUES			= "simulator.renderAllValues";
	public static final	String SIMULATOR_APMC_STRATEGY				= "simulator.apmcStrategy";
	public static final String SIMULATOR_NETWORK_FILE				= "simulator.networkFile";
	
	//GUI Log
	public static final	String LOG_FONT								= "log.font";
	public static final	String LOG_SELECTION_COLOUR					= "log.selectionColour";
	public static final	String LOG_BG_COLOUR						= "log.bgColour";
	public static final	String LOG_BUFFER_LENGTH					= "log.bufferLength";
	
	
	//Defaults, types and constaints
	
	public static final String[] propertyOwnerNames =
	{
		"Prism",
		"Model",
		"Properties",
		"Simulator",
		"Log"
	};
	public static final int[] propertyOwnerIDs =
	{
		PropertyConstants.PRISM,
		PropertyConstants.MODEL,
		PropertyConstants.PROPERTIES,
		PropertyConstants.SIMULATOR,
		PropertyConstants.LOG
	};
	
	
	/*
	 *
	 *
	 *	<li> 1 - use with ordering 1: nondet vars form a tree at the top
	 *                                                                      <li> 3 - use with ordering 2: zero for nonexistant bits
	 *
	 *
	 *
	 */
	public static final Object[][][] propertyData =
	{
		{	//Datatype:			Key:									Display name:							Default:																	Constraints:																				Comment:
			//====================================================================================================================================================================================================================================================================================================================================
			{ CHOICE_TYPE,		PRISM_ENGINE,							"engine",								"Hybrid",																	"MTBDD,Sparse,Hybrid",																		"Model checking engine" },
			{ BOOLEAN_TYPE,		PRISM_VERBOSE,							"verbose output?",						new Boolean(false),															"",																							"Display verbose output?" },
			{ BOOLEAN_TYPE,		PRISM_FAIRNESS,							"use fairness?",						new Boolean(false),															"",																							"Use fairness for MDP analysis?" },
			{ BOOLEAN_TYPE,		PRISM_PRECOMPUTATION,					"use precomputation?",					new Boolean(true),															"",																							"Use model checking precomputation algorithms?" },
			{ BOOLEAN_TYPE,		PRISM_DO_PROB_CHECKS,					"do prob checks?",						new Boolean(true),															"",																							"Perform sanity checks on model probabilities/rates?" },
			{ BOOLEAN_TYPE,		PRISM_COMPACT,							"use compact schemes?",					new Boolean(true),															"",																							"Use compact sparse storage schemes?" },
			{ CHOICE_TYPE,		PRISM_LIN_EQ_METHOD,					"iterative method",						"Jacobi",																	"Power,Jacobi,Gauss-Seidel,Backwards Gauss-Seidel,Pseudo-Gauss-Seidel,Backwards Pseudo-Gauss-Seidel,JOR,SOR,Backwards SOR,Pseudo-SOR,Backwards Pseudo-SOR",		"Iterative method for linear equation system solution" },
			{ DOUBLE_TYPE,		PRISM_LIN_EQ_METHOD_PARAM,				"over-relaxation parameter",			new Double(0.9),															"",																							"Over-relaxation parameter (for JOR/SOR/etc.)" },
			{ CHOICE_TYPE,		PRISM_TERM_CRIT,						"termination criteria",					"Relative",																	"Absolute,Relative",																		"Termination criteria (iterative methods)" },
			{ DOUBLE_TYPE,		PRISM_TERM_CRIT_PARAM,					"termination epsilon",					new Double(1.0E-6),															"0.0,",																						"Termination criteria epsilon (iterative methods)" },
			{ INTEGER_TYPE,		PRISM_MAX_ITERS,						"termination max iterations",			new Integer(10000),															"0,",																						"Maximum iterations (iterative methods)" },
			{ INTEGER_TYPE,		PRISM_CUDD_MAX_MEM,						"cudd max memory (KB)",					new Integer(204800),														"0,",																						"CUDD max memory (KB)" },
			{ DOUBLE_TYPE,		PRISM_CUDD_EPSILON,						"cudd epsilon",							new Double(1.0E-15),														"0.0,",																						"CUDD epsilon for terminal cache comparisions" },
			{ INTEGER_TYPE,		PRISM_NUM_SB_LEVELS,					"hybrid num. levels",					new Integer(-1),															"-1,",																						"Number of MTBDD levels ascended when adding sparse matrices to hybrid engine data structures (-1 means use default)" },
			{ INTEGER_TYPE,		PRISM_SB_MAX_MEM,						"hybrid max memory",					new Integer(1024),															"0,",																						"Maximum memory usage when adding sparse matrices to hybrid engine data structures" },
			{ INTEGER_TYPE,		PRISM_NUM_SOR_LEVELS,					"hybrid num. levels (GS/SOR)",			new Integer(-1),															"-1,",																						"Number of MTBDD levels descended for hybrid engine data structures block division (GS/SOR)" },
			{ INTEGER_TYPE,		PRISM_SOR_MAX_MEM,						"hybrid max memory (GS/SOR)",			new Integer(1024),															"0,",																						"Maximum memory usage for hybrid engine data structures block division (GS/SOR)" },
		},
		{
			{ BOOLEAN_TYPE,		MODEL_AUTO_PARSE,						"auto parse?",							new Boolean(true),															"",																							"When set to true, prism models are parsed automatically as they are entered into the text editor." },
			{ BOOLEAN_TYPE,		MODEL_AUTO_MANUAL,						"manual parse for large model?",		new Boolean(true),															"",																							"When set to true, the loading of large PRISM models turns off automatic parsing." },
			{ INTEGER_TYPE,		MODEL_PARSE_DELAY,						"parse delay (ms)",						new Integer(1000),															"0,",																						"After typing has finished, the time delay before an automatic re-parse of the model is performed." },
			{ FONT_COLOUR_TYPE,	MODEL_PRISM_EDITOR_FONT,				"prism editor font",					new FontColorPair(new Font("monospaced", Font.PLAIN, 12), Color.black),		"",																							"The base font (without highlighting) for the text editor when editing PRISM models." },
			{ COLOUR_TYPE,		MODEL_PRISM_EDITOR_BG_COLOUR,			"prism editor background",				new Color(255,255,255),														"",																							"The colour of the background of the text editor when editing PRISM models." },
			{ COLOUR_TYPE,		MODEL_PRISM_EDITOR_NUMERIC_COLOUR,		"prism editor numeric colour",			new Color(0,0,255),															"",																							"The colour to highlight numeric characters of the text editor when editing PRISM models." },
			{ CHOICE_TYPE,		MODEL_PRISM_EDITOR_NUMERIC_STYLE,		"prism editor numeric style",			"Plain",																	"Plain,Italic,Bold,Bold Italic",															"The style to highlight numeric characters of the text editor when editing PRISM models." },
			{ COLOUR_TYPE,		MODEL_PRISM_EDITOR_IDENTIFIER_COLOUR,	"prism editor identifier colour",		new Color(255,0,0),															"",																							"The colour to highlight the characters of identifiers in the text editor when editing PRISM models." },
			{ CHOICE_TYPE,		MODEL_PRISM_EDITOR_IDENTIFIER_STYLE,	"prism editor identifier style",		"Plain",																	"Plain,Italic,Bold,Bold Italic",															"The style to highlight the characters of identifiers in the text editor when editing PRISM models." },
			{ COLOUR_TYPE,		MODEL_PRISM_EDITOR_KEYWORD_COLOUR,		"prism editor keyword colour",			new Color(0,0,0),															"",																							"The colour to highlight the characters of keywords in the text editor when editing PRISM models." },
			{ CHOICE_TYPE,		MODEL_PRISM_EDITOR_KEYWORD_STYLE,		"prism editor keyword style",			"Bold",																		"Plain,Italic,Bold,Bold Italic",															"The style to highlight the characters of keywords in the text editor when editing PRISM models." },
			{ COLOUR_TYPE,		MODEL_PRISM_EDITOR_COMMENT_COLOUR,		"prism editor comment colour",			new Color(0,99,0),															"",																							"The colour to highlight the characters of comments in the text editor when editing PRISM models." },
			{ CHOICE_TYPE,		MODEL_PRISM_EDITOR_COMMENT_STYLE,		"prism editor comment style",			"Italic",																	"Plain,Italic,Bold,Bold Italic",															"The style to highlight the characters of comments in the text editor when editing PRISM models." },
			{ FONT_COLOUR_TYPE,	MODEL_PEPA_EDITOR_FONT,					"pepa editor font",						new FontColorPair(new Font("monospaced", Font.PLAIN, 12), Color.black),		"",																							"The base font (without highlighting) for the text editor when editing PEPA models." },
			{ COLOUR_TYPE,		MODEL_PEPA_EDITOR_BG_COLOUR,			"pepa editor background",				new Color(255,250,240),														"",																							"The colour of the background of the text editor when editing PEPA models." },
			{ COLOUR_TYPE,		MODEL_PEPA_EDITOR_COMMENT_COLOUR,		"pepa editor comment colour",			new Color(0,99,0),															"",																							"The colour to highlight the characters of comments in the text editor when editing PEPA models." },
			{ CHOICE_TYPE,		MODEL_PEPA_EDITOR_COMMENT_STYLE,		"pepa editor comment style",			"Italic",																	"Plain,Italic,Bold,Bold Italic",															"The style to highlight the characters of comments in the text editor when editing PEPA models." }
		},
		{
			{ FONT_COLOUR_TYPE,	PROPERTIES_FONT,						"display font",							new FontColorPair(new Font("monospaced", Font.PLAIN, 12), Color.black),		"",																							"The font for the properties list." },
			{ COLOUR_TYPE,		PROPERTIES_WARNING_COLOUR,				"warning colour",						new Color(255,130,130),														"",																							"The colour to highlight a property when there is an error / problem." },
			{ CHOICE_TYPE,		PROPERTIES_ADDITION_STRATEGY,			"property addition strategy",			"Warn when invalid",														"Warn when invalid,Do not allow invalid",													"How to deal with properties that are invalid." },
			{ BOOLEAN_TYPE,		PROPERTIES_CLEAR_LIST_ON_LOAD,			"clear list when load model?",			new Boolean(true),															"",																							"When set to true, the properties list is cleared whenever a new model is loaded." }
		},
		{
			{ CHOICE_TYPE,		SIMULATOR_ENGINE,						"simulator engine",						"PRISM simulator",															"PRISM simulator,APMC",																		"The simulator engine for use in approximate model checking.  The PRISM simulator supports DTMCs and CTMCs.  APMC supports DTMCs and only runs on UNIX platforms." },
			{ DOUBLE_TYPE,		SIMULATOR_DEFAULT_APPROX,				"default approximation parameter",		new Double(1.0E-2),															"0,1",																							"The default approximation parameter for approximate model checking using the PRISM simulator or APMC." },
			{ DOUBLE_TYPE,		SIMULATOR_DEFAULT_CONFIDENCE,			"default confidence parameter",			new Double(1.0E-10),														"0,1",																						"The default confidence parameter for approximate model checking using the PRISM simulator or APMC." },
			{ INTEGER_TYPE,		SIMULATOR_DEFAULT_NUM_SAMPLES,			"default num. samples",					new Integer(402412),														"1,",																						"The default number of samples for approximate model checking using the PRISM simulator or APMC." },
			{ INTEGER_TYPE,		SIMULATOR_DEFAULT_MAX_PATH,				"default max. path length",				new Integer(10000),															"1,",																					"The default maximum path length for approximate model checking using the PRISM simulator or APMC." },
			{ BOOLEAN_TYPE,		SIMULATOR_SIMULTANEOUS,					"check properties simultaneously?",		new Boolean(true),															"",																							"When set to true, all relevant properties are checked over the same execution paths, meaning only one set of sample paths need be generated.  This feature is only supported by the PRISM simulator." },
			{ CHOICE_TYPE,		SIMULATOR_FIELD_CHOICE,					"values used in dialog",				"Last used values",															"Last used values,Always use defaults",														"This setting allows the choice between whether the values used in the simulation dialog are taken from the defaults every time, or from the last used values." },
			{ BOOLEAN_TYPE,		SIMULATOR_NEW_PATH_ASK_INITIAL,			"ask for initial state?",				new Boolean(true),															"",																							"When set to true, creating a new path in the simulator user interface prompts for an initial state rather than using default values for the model." },
			{ CHOICE_TYPE,		SIMULATOR_RENDER_ALL_VALUES,			"path render style",					"Render changes",															"Render changes,Render all values",															"How the execution path in the simulator user interface should display the different states.  The \'render changes\' option displays a value only if it has changed." },
			{ INTEGER_TYPE,		SIMULATOR_APMC_STRATEGY,				"apmc strategy",						new Integer(0),																"",																							"The strategy used by APMC" },	
			{ FILE_TYPE,		SIMULATOR_NETWORK_FILE,					"network profile",						new File(""),																		"",																					"This file is used to specify the network profile which should be used by the distributed PRISM simulator." }
		},
		{
			{ FONT_COLOUR_TYPE,	LOG_FONT,								"display font",							new FontColorPair(new Font("monospaced", Font.PLAIN, 12), Color.black),		"",																							"The font for the log display." },
			{ COLOUR_TYPE,		LOG_BG_COLOUR,							"background colour",					new Color(255,255,255),														"",																							"The colour of the background of the log display." },
			{ INTEGER_TYPE,		LOG_BUFFER_LENGTH,						"buffer length",						new Integer(10000),															"1,",																						"The length of the buffer for the log display." }
		}
	};
	
	
	
	
	public DefaultSettingOwner[] optionOwners;
	private Hashtable data;
	private boolean modified;
	
	private ArrayList settingsListeners;
	
	public PrismSettings()
	{
		optionOwners = new DefaultSettingOwner[propertyOwnerIDs.length];
		
		int counter = 0;
		
		for(int i = 0; i < propertyOwnerIDs.length; i++)
		{
			optionOwners[i] = new DefaultSettingOwner(propertyOwnerNames[i], propertyOwnerIDs[i]);
			for(int j = 0; j < propertyData[i].length; j++)
			{
				counter++;
				Object[] setting = propertyData[i][j];
				String key = (String)setting[1];
				String display = (String)setting[2];
				Object value = setting[3];
				String constraint = (String)setting[4];
				String comment = (String)setting[5];
				
				Setting set;
				
				if(setting[0].equals(INTEGER_TYPE))
				{
					if(constraint.equals(""))
						set = new IntegerSetting(display, (Integer)value, comment, optionOwners[i], false);
					else
						set = new IntegerSetting(display, (Integer)value, comment, optionOwners[i], false, new RangeConstraint(constraint));
					set.setKey(key);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(DOUBLE_TYPE))
				{
					//DO constraints for this double
					if(constraint.equals(""))
						set = new DoubleSetting(display, (Double)value, comment, optionOwners[i], false);
					else
						set = new DoubleSetting(display, (Double)value, comment, optionOwners[i], false, new RangeConstraint(constraint));
					set.setKey(key);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(BOOLEAN_TYPE))
				{
					//DO constraints for this boolean
					set = new BooleanSetting(display, (Boolean)value, comment, optionOwners[i], false);
					set.setKey(key);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(COLOUR_TYPE))
				{
					//DO constraints for this Color
					set = new ColorSetting(display, (Color)value, comment, optionOwners[i], false);
					set.setKey(key);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(CHOICE_TYPE))
				{
					//DO constraints for this choice
					StringTokenizer tokens = new StringTokenizer(constraint, ",");
					String[] choices = new String[tokens.countTokens()];
					int k = 0;
					while(tokens.hasMoreTokens())
					{
						choices[k++] = tokens.nextToken();
					}
					set = new ChoiceSetting(display, choices, (String)value, comment, optionOwners[i], false);
					set.setKey(key);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(FONT_COLOUR_TYPE))
				{
					//DO constraints for this FontColorPair
					set = new FontColorSetting(display, (FontColorPair)value, comment, optionOwners[i], false);
					set.setKey(key);
					optionOwners[i].addSetting(set);
				}
				else if(setting[0].equals(FILE_TYPE))
				{
					set = new FileSetting(display, (File)value, comment, optionOwners[i], false);
					set.setKey(key);
					optionOwners[i].addSetting(set);
				}
				else
				{
					System.err.println("Fatal error when loading properties: unknown setting type "+setting[0]);
				}
			}
			optionOwners[i].addObserver(this);
		}
		
		modified = false;
		
		//populate a hash table with the keys
		populateHashTable(counter);
		settingsListeners = new ArrayList();
		
	}
	
	private void populateHashTable(int size)
	{
		data = new Hashtable(size);
		
		for(int i = 0; i < optionOwners.length; i++)
		{
			for(int j = 0; j < optionOwners[i].getNumSettings(); j++)
			{
				data.put(optionOwners[i].getSetting(j).getKey(), optionOwners[i].getSetting(j));
			}
		}
	}
	
	private Setting settingFromHash(String key)
	{
		return (Setting)data.get(key);
	}
	
	public void addSettingsListener(PrismSettingsListener listener)
	{
		settingsListeners.add(listener);
	}
	
	public void removeSettingsListener(PrismSettingsListener listener)
	{
		settingsListeners.remove(listener);
	}
	
	public void notifySettingsListeners()
	{
		for(int i = 0; i < settingsListeners.size(); i++)
		{
			PrismSettingsListener listener = (PrismSettingsListener)settingsListeners.get(i);
			listener.notifySettings(this);
		}
	}
	
	public File getLocationForSettingsFile()
	{
		return new File(System.getProperty("user.home")+"/.prism");
	}
	
	public synchronized void saveSettingsFile() throws PrismException
	{
		File file = getLocationForSettingsFile();
		
		try
		{
			FileWriter out = new FileWriter(file);
			
			out.write("# PRISM settings file\n");
			out.write("# (created by version "+Prism.getVersion()+")\n");
			
			for(int i = 0; i < optionOwners.length; i++)
			{
				out.write("\n");
				for(int j = 0; j < optionOwners[i].getNumSettings(); j++)
				{
					Setting set = optionOwners[i].getSetting(j);
					//write the key
					out.write(set.getKey()+"=");
					String value = set.toString();
					while(value.indexOf('\n') != -1) //is multiline string
					{
						out.write(value.substring(0,value.indexOf('\n')) + "\\");
						if(value.substring(value.indexOf('\n')).length() > 0)
							value = value.substring(value.indexOf('\n')+1);
						else
							value = "";
					}
					
					out.write(value+"\n");
					
				}
			}
			
			out.close();
			
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new PrismException("Error exporting properties file: "+e.getMessage());
		}
		
		modified = false;
	}
	
	public synchronized void loadSettingsFile() throws PrismException
	{
		File file = getLocationForSettingsFile();
		
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			String key = "";
			String value;
			int equalsIndex;
			StringBuffer multiline = new StringBuffer();
			boolean inMulti = false;
			
			while(reader.ready())
			{
				line = reader.readLine();
				if(!inMulti)
				{
					if(line.startsWith("#")) continue; //ignore comments
					equalsIndex = line.indexOf('=');
					if(equalsIndex == -1) continue; //is not a valid property line
					else
					{
						key = line.substring(0, equalsIndex);
						value = (equalsIndex==line.length()-1)?"":line.substring(equalsIndex+1);
						if(value.endsWith("\\"))
						{
							inMulti = true;
							multiline = new StringBuffer(value.substring(0, value.length()-1)); // trim the \ off
						}
						else
						{
							Setting set = settingFromHash(key);
							if(set != null)
							{
								try
								{
									Object valObj = set.parseStringValue(value);
									set.setValue(valObj);
								}
								catch(SettingException ee)
								{
									System.err.println("Warning: PRISM setting \""+key+"\" has invalid value \"" + value + "\"");
								}
							}
							else
							{
								System.err.println("Warning: PRISM setting \""+key+"\" is unknown.");
							}
						}
						
					}
				}
				else
				{
					//In multiline
					if(line.endsWith("\\"))
					{
						multiline.append(line.substring(0, line.length()-1));
					}
					else
					{
						Setting set = settingFromHash(key);
						if(set != null)
						{
							try
							{
								Object valObj = set.parseStringValue(multiline.toString() + line);
								set.setValue(valObj);
							}
							catch(SettingException ee)
							{
								System.err.println("Warning: PRISM setting \""+key+"\" has invalid value \"" + multiline.toString() + line + "\"");
							}
						}
						else
						{
							System.err.println("Warning: PRISM setting \""+key+"\" is unknown.");
						}
						
						inMulti = false;
						multiline = null;
					}
				}
			}
			
			
		}
		catch(IOException e)
		{
			throw new PrismException("Error importing properties file: "+e.getMessage());
		}
		
		modified = false;
		
		notifySettingsListeners();
	}
	
	public synchronized void loadDefaults()
	{
		for(int i = 0; i < propertyOwnerIDs.length; i++)
		{
			
			for(int j = 0; j < propertyData[i].length; j++)
			{
				Setting set = settingFromHash(propertyData[i][j][1].toString());
			
				if(set != null)
				{
					try
					{
						set.setValue(propertyData[i][j][3]);
					}
					catch(SettingException e)
					{
						System.err.println("Warning: Error with default value for PRISM setting \""+set.getName()+"\"");
					}
				}
			}
		}
		
		notifySettingsListeners();
	}
	
	public synchronized void set(String key, Object value) throws PrismException
	{
		Setting set = settingFromHash(key);
		if(set == null)throw new PrismException("Property "+key+" is not valid.");
		try
		{
			String oldValueString = set.toString();
			
			if(value instanceof Integer && set instanceof ChoiceSetting)
			{
				int iv = ((Integer)value).intValue();
				((ChoiceSetting)set).setSelectedIndex(iv);
			}
			else
			{
				set.setValue(value);
			}
			
			notifySettingsListeners();
			
			if(!set.toString().equals(oldValueString))modified = true;
		}
		catch(SettingException e)
		{
			throw new PrismException(e.getMessage());
		}
	}
	
	public synchronized void set(String key, String value) throws PrismException
	{
		Setting set = settingFromHash(key);
		for(int i = 0; (i < optionOwners.length) && (set==null); i++)
		{
			set = optionOwners[i].getFromKey(key);
		}
		if(set == null)throw new PrismException("Property "+key+" is not valid.");
		try
		{
			Object obj = set.parseStringValue(value);
			String oldValueString = set.toString();
			
			set.setValue(obj);
			
			notifySettingsListeners();
			
			if(!set.toString().equals(oldValueString))modified = true;
		}
		catch(SettingException e)
		{
			throw new PrismException(e.getMessage());
		}
	}
	
	public synchronized void setFileSelector(String key, FileSelector select)
	{
		Setting set = settingFromHash(key);
		if(set instanceof FileSetting)
		{
			FileSetting fset = (FileSetting)set;
			
			fset.setFileSelector(select);
		}
	}
	
	//The following methods are kept in for legacy purposes
	
	public synchronized void set(String key, int value) throws PrismException
	{
		set(key, new Integer(value));
	}
	
	public synchronized void set(String key, boolean value) throws PrismException
	{
		set(key, new Boolean(value));
	}
	
	public synchronized void set(String key, double value) throws PrismException
	{
		set(key, new Double(value));
	}
	
	public synchronized String getString(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof SingleLineStringSetting)
		{
			return ((SingleLineStringSetting)set).getStringValue();
		}
		else if(set instanceof MultipleLineStringSetting)
		{
			return ((MultipleLineStringSetting)set).getStringValue();
		}
		else if(set instanceof ChoiceSetting)
		{
			return ((ChoiceSetting)set).getStringValue();
		}
		else return DEFAULT_STRING;
	}
	
	public synchronized int getInteger(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof IntegerSetting)
		{
			return ((IntegerSetting)set).getIntegerValue();
		}
		else if(set instanceof ChoiceSetting)
		{
			return ((ChoiceSetting)set).getCurrentIndex();
		}
		else return DEFAULT_INT;
	}
	
	public synchronized double getDouble(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof DoubleSetting)
		{
			return ((DoubleSetting)set).getDoubleValue();
		}
		else return DEFAULT_DOUBLE;
	}
	
	public synchronized boolean getBoolean(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof BooleanSetting)
		{
			return ((BooleanSetting)set).getBooleanValue();
		}
		else return DEFAULT_BOOLEAN;
	}
	
	public synchronized Color getColor(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof ColorSetting)
		{
			return ((ColorSetting)set).getColorValue();
		}
		else return DEFAULT_COLOUR;
	}
	
	public synchronized FontColorPair getFontColorPair(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof FontColorSetting)
		{
			return ((FontColorSetting)set).getFontColorValue();
		}
		else return DEFAULT_FONT_COLOUR;
	}
	
	public synchronized File getFile(String key)
	{
		Setting set = settingFromHash(key);
		if(set instanceof FileSetting)
		{
			return ((FileSetting)set).getFileValue();
		}
		else return DEFAULT_FILE;
	}
	
	public boolean isModified()
	{
		return modified;
	}
	
	public void update(Observable obs, Object obj)
	{
		modified = true;
		notifySettingsListeners();
	}
	
	public static void main(String[]args)
	{
		PrismSettings set = new PrismSettings();
		
		JFrame jf = new JFrame("Prism Settings");
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		ArrayList owners = new ArrayList();
		for(int i = 0; i < set.optionOwners.length; i++)
		{
			owners.add(set.optionOwners[i]);
		}
		
		SettingTable pt = new SettingTable(jf);
		pt.setOwners(owners);
		
		for(int i = 0; i < owners.size(); i++)
		{
			SettingOwner a = (SettingOwner)owners.get(i);
			a.setDisplay(pt);
		}
		
		jf.getContentPane().add(pt);
		jf.getContentPane().setSize(100, 300);
		
		jf.pack();
		jf.show();
	}
}
