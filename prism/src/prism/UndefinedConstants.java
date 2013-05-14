//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
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

package prism;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import parser.*;
import parser.ast.*;
import parser.type.*;

/**
 * Class to handle the undefined constants in model and/or properties file.
 * This may not be *all* of the constants in the file(s); it may be just
 * the ones that are required to execute model checking or some other task.
 * Really, the main purpose of this class is to manage an "experiment",
 * i.e. to iterate through several different values of one or more constants,
 * but this includes the special case where all undefined constants are given a single value.
 */
public class UndefinedConstants
{
	// info used to construct this object
	// parsed model/properties files
	private ModulesFile modulesFile = null;
	private PropertiesFile propertiesFile = null;
	// optionally, specific properties to get constants from
	private List<Property> props = null;
	// just get constants from labels (in properties file)?
	private boolean justLabels = false;

	// info about constants
	private int mfNumConsts = 0;
	private List<DefinedConstant> mfConsts = null;
	private int pfNumConsts = 0;
	private List<DefinedConstant> pfConsts = null;

	// stuff for iterating through constant values
	private Values mfValues = null;
	private Values pfValues = null;

	// class-wide storage for info from -const switch
	private Vector<String> constSwitchNames;
	private Vector<String> constSwitchLows;
	private Vector<String> constSwitchHighs;
	private Vector<String> constSwitchSteps;

	/**
	 * Construct information about undefined constants from a model and/or properties file.
	 * If either is not required, it can be left as null. All undefined constants,
	 * whether used or not, are assumed to be required to be provided.
	 */
	public UndefinedConstants(ModulesFile modulesFile, PropertiesFile propertiesFile)
	{
		this(modulesFile, propertiesFile, false);
	}

	/**
	 * Construct information about undefined constants from a model and/or properties file.
	 * If either is not required, it can be left as null. If {@code justLabels} is false,
	 * all constants, whether used or not, are assumed to be required to be provided.
	 * If {@code justLabels} is true, only properties file constants that are needed for labels are.
	 */
	public UndefinedConstants(ModulesFile modulesFile, PropertiesFile propertiesFile, boolean justLabels)
	{
		setModulesFile(modulesFile);
		setPropertiesFile(propertiesFile);
		setJustLabels(justLabels);
		initialise();
	}

	/**
	 * Construct information about undefined constants for a specific property.
	 * It is assumed that all undefined constants from the model file are needed,
	 * plus any from the properties file that are use in the property {@code prop}.  
	 */
	public UndefinedConstants(ModulesFile modulesFile, PropertiesFile propertiesFile, Property prop)
	{
		setModulesFile(modulesFile);
		setPropertiesFile(propertiesFile);
		addProperty(prop);
		initialise();
	}

	/**
	 * Construct information about undefined constants for specific properties.
	 * It is assumed that all undefined constants from the model file are needed,
	 * plus any from the properties file that are use in the properties {@code props}.  
	 */
	public UndefinedConstants(ModulesFile modulesFile, PropertiesFile propertiesFile, List<Property> props)
	{
		setModulesFile(modulesFile);
		setPropertiesFile(propertiesFile);
		setProperties(props);
		initialise();
	}

	// Setters

	public void setModulesFile(ModulesFile modulesFile)
	{
		this.modulesFile = modulesFile;
	}

	public void setPropertiesFile(PropertiesFile propertiesFile)
	{
		this.propertiesFile = propertiesFile;
	}

	public void setProperties(List<Property> props)
	{
		this.props = props;
	}

	public void addProperty(Property prop)
	{
		if (props == null)
			props = new ArrayList<Property>();
		props.add(prop);
	}

	public void setJustLabels(boolean justLabels)
	{
		this.justLabels = justLabels;
	}

	/**
	 * Initialise this UndefinedConstants object:
	 * determine which constants are undefined and then set up data structures.
	 */
	public void initialise()
	{
		Vector<String> mfv, pfv;
		// determine which constants are undefined
		mfv = (modulesFile == null) ? new Vector<String>() : modulesFile.getUndefinedConstants();
		if (propertiesFile == null) {
			pfv = new Vector<String>();
		} else {
			if (props == null) {
				if (justLabels) {
					pfv = propertiesFile.getUndefinedConstantsUsedInLabels();
					pfv = orderConstantsByPropertiesFile(pfv, propertiesFile);
				} else {
					pfv = propertiesFile.getUndefinedConstants();
				}
			} else {
				pfv = propertiesFile.getUndefinedConstantsUsedInProperties(props);
				pfv = orderConstantsByPropertiesFile(pfv, propertiesFile);
			}
		}
		// create data structures
		setUpDataStructures(mfv, pfv);
	}

	/**
	 * Create a new copy of a list of constant names, sorted by their occurrence in a PropertiesFile. 
	 */
	private Vector<String> orderConstantsByPropertiesFile(Vector<String> oldList, PropertiesFile propertiesFile)
	{
		Vector<String> newList = new Vector<String>();
		Vector<String> pfList = propertiesFile.getUndefinedConstants();
		for (String s : pfList) {
			if (oldList.contains(s))
				newList.add(s);
		}
		return newList;
	}

	/**
	 * Set up data structures (as required by constructor methods)
	 */
	private void setUpDataStructures(Vector<String> mfv, Vector<String> pfv)
	{
		int i;
		String s;
		// determine how many constants there are
		mfNumConsts = mfv.size();
		pfNumConsts = pfv.size();
		// create storage for info about constant definitions
		mfConsts = new ArrayList<DefinedConstant>(mfNumConsts);
		for (i = 0; i < mfNumConsts; i++) {
			s = (String) mfv.elementAt(i);
			mfConsts.add(new DefinedConstant(s, modulesFile.getConstantList().getConstantType(modulesFile.getConstantList().getConstantIndex(s))));
		}
		pfConsts = new ArrayList<DefinedConstant>(pfNumConsts);
		for (i = 0; i < pfNumConsts; i++) {
			s = (String) pfv.elementAt(i);
			pfConsts.add(new DefinedConstant(s, propertiesFile.getConstantList().getConstantType(propertiesFile.getConstantList().getConstantIndex(s))));
		}
		// initialise storage just created
		clearAllDefinitions();
	}

	/**
	 * Remove a constant. This is used if you decide that you do not want to treat
	 * some constant as undefined once you have created the UndefinedConstants object.
	 * @return whether a constant was actually removed.  
	 */
	public boolean removeConstant(String constName)
	{
		int i = getMFConstIndex(constName);
		if (i != -1) {
			mfNumConsts--;
			mfConsts.remove(i);
			return true;
		} else {
			i = getPFConstIndex(constName);
			if (i != -1) {
				pfNumConsts--;
				pfConsts.remove(i);
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove some constants. This is used if you decide that you do not want to treat
	 * some constants as undefined once you have created the UndefinedConstants object.  
	 * @return how many constants were actually removed.  
	 */
	public int removeConstants(String constNames[])
	{
		int removed = 0, n = constNames.length;
		for (int i = 0; i < n; i++) {
			if (removeConstant(constNames[i]))
				removed++;
		}
		return removed;
	}

	// Accessor methods for info about undefined constants

	public int getMFNumUndefined()
	{
		return mfNumConsts;
	}

	public int getPFNumUndefined()
	{
		return pfNumConsts;
	}

	public String getMFUndefinedName(int i)
	{
		return mfConsts.get(i).getName();
	}

	public String getPFUndefinedName(int i)
	{
		return pfConsts.get(i).getName();
	}

	public Type getMFUndefinedType(int i)
	{
		return mfConsts.get(i).getType();
	}

	public Type getPFUndefinedType(int i)
	{
		return pfConsts.get(i).getType();
	}

	public int getMFConstIndex(String s)
	{
		for (int i = 0; i < mfNumConsts; i++) {
			if (mfConsts.get(i).getName().equals(s))
				return i;
		}
		return -1;
	}

	public int getPFConstIndex(String s)
	{
		for (int i = 0; i < pfNumConsts; i++) {
			if (pfConsts.get(i).getName().equals(s))
				return i;
		}
		return -1;
	}

	/**
	 * Clear definitions of all undefined constants.
	 */
	public void clearAllDefinitions()
	{
		int i;

		// constants from model file
		for (i = 0; i < mfNumConsts; i++) {
			mfConsts.get(i).clear();
			;
		}
		// constants from properties file
		for (i = 0; i < pfNumConsts; i++) {
			pfConsts.get(i).clear();
		}
	}

	/**
	 * Define all undefined constants using the argument to the prism -const command line switch.
	 * (i.e. format is "x=1,y=1:100,z=1:10:100,b=true")
	 */
	public void defineUsingConstSwitch(String constSwitch) throws PrismException
	{
		int i;
		String name;
		boolean dupe;
		boolean useAll = false;

		// clear any previous definitions
		clearAllDefinitions();

		// parse and store info from switch argument
		parseConstSwitch(constSwitch);

		// if there are no undefined consts...
		if (useAll && (mfNumConsts + pfNumConsts == 0)) {
			if (constSwitchNames.size() > 0) {
				throw new PrismException("There are no undefined constants to define");
			}
			return;
		}

		// go thru parts of -const switch one by one
		for (i = 0; i < constSwitchNames.size(); i++) {

			// get name
			name = constSwitchNames.elementAt(i);

			// define constant using info from switch
			dupe = defineConstant(name, constSwitchLows.elementAt(i), constSwitchHighs.elementAt(i), constSwitchSteps.elementAt(i), useAll);

			// check for duplication
			if (dupe) {
				throw new PrismException("Duplicate definitions for undefined constant \"" + name + "\"");
			}
		}

		// check all undefined consts have now been defined
		checkAllDefined();

		// initialise info for iterating
		initialiseIterators();
	}

	/**
	 * Parse -const switch string and store info.
	 */
	private void parseConstSwitch(String constSwitch) throws PrismException
	{
		int i, j;
		String parts[], args[];

		// create storage for info
		constSwitchNames = new Vector<String>();
		constSwitchLows = new Vector<String>();
		constSwitchHighs = new Vector<String>();
		constSwitchSteps = new Vector<String>();

		// if string is null, nothing more to do
		if (constSwitch == null)
			return;

		// split into comma separated parts...
		parts = constSwitch.split(",");

		// ...and treat each separately
		for (i = 0; i < parts.length; i++) {

			// ignore blanks
			if (parts[i].length() == 0)
				continue;

			// split either size of "=", store lhs (name)
			j = parts[i].indexOf('=');
			if (j < 1 || j + 2 > parts[i].length())
				throw new PrismException("Invalid format in definition of undefined constants");
			constSwitchNames.add(parts[i].substring(0, j));

			// check for trailing colon(s)
			// this is a formatting error not detected by split(":") below
			if (parts[i].charAt(parts[i].length() - 1) == ':')
				throw new PrismException("Invalid format in definition of undefined constants");

			// split into colon separated parts
			args = parts[i].substring(j + 1).split(":");
			// simple case - no colons, e.g. x=0
			if (args.length == 1) {
				constSwitchLows.add(args[0]);
				constSwitchHighs.add(null);
				constSwitchSteps.add(null);
			}
			// range, e.g. x=0:10
			else if (args.length == 2) {
				constSwitchLows.add(args[0]);
				constSwitchHighs.add(args[1]);
				constSwitchSteps.add(null);
			}
			// range with step, e.g. x=0:2:10
			else if (args.length == 3) {
				constSwitchLows.add(args[0]);
				constSwitchHighs.add(args[2]);
				constSwitchSteps.add(args[1]);
			}
			// error
			else {
				throw new PrismException("Invalid format in definition of undefined constants");
			}
		}
	}

	/** Define value for a single undefined constant.
	 *  Returns whether or not an existing definition was overwritten.
	 *	Actually just helper method for more general method {@link #defineConstant(String, String, String, String) below}.
	 *  
	 *  The method {@link #initialiseIterators() initialiseIterators} must be called after all constants are defined.
	 *
	 *  @param name The name of the constant.
	 *  @param val The value to be assigned.
	 *  
	 *  @return True if the constant was defined before.
	 */
	public boolean defineConstant(String name, String val) throws PrismException
	{
		return defineConstant(name, val, null, null, false);
	}

	/** Define value for a single undefined constant.
	 *  Returns whether or not an existing definition was overwritten.
	 *
	 *  The method {@link #initialiseIterators() initialiseIterators} must be called after all constants are defined.
	 *	
	 *  @param name The name of the constant.
	 *  @param sl If sh are ss are null, this is the value to be assigned. Otherwise, it is the lower bound for the range.
	 *  @param sh The upper bound for the range.
	 *  @param ss The step for the values. Null means 1.
	 *  @param useAll If true, throw an exception if {@code name} is does not need to be defined
	 *  
	 *  @return True if the constant was defined before.
	 */
	public boolean defineConstant(String name, String sl, String sh, String ss) throws PrismException
	{
		return defineConstant(name, sl, sh, ss, false);
	}

	/** Define value for a single undefined constant.
	 *  Returns whether or not an existing definition was overwritten.
	 *
	 *  The method {@link #initialiseIterators() initialiseIterators} must be called after all constants are defined.
	 *	
	 *  @param name The name of the constant.
	 *  @param sl If sh are ss are null, this is the value to be assigned. Otherwise, it is the lower bound for the range.
	 *  @param sh The upper bound for the range.
	 *  @param ss The step for the values. Null means 1.
	 *  @param useAll If true, throw an exception if {@code name} does not need to be defined
	 *  
	 *  @return True if the constant was defined before.
	 */
	public boolean defineConstant(String name, String sl, String sh, String ss, boolean useAll) throws PrismException
	{
		int index = 0;
		boolean overwrite = false; // did definition exist already?

		// find out if const is in model or properties file (or neither)
		// also check for overwriting
		index = getMFConstIndex(name);
		if (index != -1) {
			// const is in modules file
			overwrite = (mfConsts.get(index).isDefined());
			mfConsts.get(index).define(sl, sh, ss);
		} else {
			index = getPFConstIndex(name);
			if (index != -1) {
				// const is in properties file
				overwrite = (pfConsts.get(index).isDefined());
				pfConsts.get(index).define(sl, sh, ss);
			} else {
				// If we are required to use all supplied const values, check for this
				// (by default we don't care about un-needed or non-existent const values)
				if (useAll)
					throw new PrismException("\"" + name + "\" is not an undefined constant");
				// But if an unused supplied value clashes with a defined constant, we complain
				if (modulesFile != null && modulesFile.isDefinedConstant(name)) {
					throw new PrismException("Constant \"" + name + "\" has already been defined in the model");
				}
				if (propertiesFile != null && propertiesFile.isDefinedConstant(name)) {
					throw new PrismException("Constant \"" + name + "\" has already been defined");
				}
			}
		}

		// return whether or not we overwrote an existing definition
		return overwrite;
	}

	/**
	 * Check that definitions have been provided for all constants.
	 * Throw explanatory exception if not.
	 */
	public void checkAllDefined() throws PrismException
	{
		int i, n;
		String s;
		Vector<String> v = new Vector<String>();

		for (i = 0; i < mfNumConsts; i++) {
			if (mfConsts.get(i).getLow() == null) {
				v.add(mfConsts.get(i).getName());
			}
		}
		for (i = 0; i < pfNumConsts; i++) {
			if (pfConsts.get(i).getLow() == null) {
				v.add(pfConsts.get(i).getName());
			}
		}
		n = v.size();
		if (n > 0) {
			if (n == 1) {
				s = "Undefined constant \"" + v.get(0) + "\" must be defined";
			} else {
				s = "Undefined constants";
				for (i = 0; i < n; i++) {
					if (i > 0 && i < n - 1)
						s += ",";
					else if (i == n - 1)
						s += " and";
					s += " \"" + v.get(i) + "\"";
				}
				s += " must be defined";
			}
			throw new PrismException(s);
		}
	}

	/**
	 * Initialise iterators for stepping through constant values.
	 */
	public void initialiseIterators()
	{
		intialiseModelIterator();
		intialisePropertyIterator();
	}

	/**
	 * Initialise iterator for stepping through model constant values.
	 */
	private void intialiseModelIterator()
	{
		int i;

		// set all consts to lowest values
		for (i = 0; i < mfNumConsts; i++) {
			mfConsts.get(i).setValue(mfConsts.get(i).getLow());
		}
		// create Values object
		mfValues = new Values();
		for (i = 0; i < mfNumConsts; i++) {
			mfValues.addValue(mfConsts.get(i).getName(), mfConsts.get(i).getValue());
		}
	}

	/**
	 * Initialise iterator for stepping through property constant values.
	 */
	private void intialisePropertyIterator()
	{
		int i;

		// set all consts to lowest values
		for (i = 0; i < pfNumConsts; i++) {
			pfConsts.get(i).setValue(pfConsts.get(i).getLow());
		}
		// create Values object
		pfValues = new Values();
		for (i = 0; i < pfNumConsts; i++) {
			pfValues.addValue(pfConsts.get(i).getName(), pfConsts.get(i).getValue());
		}
	}

	// Accessor methods for info about values of defined constants, iterators, etc.

	/**
	 * Get a string showing the values/ranges of all constants. 
	 * (e.g. "x=1,y=1:100,z=1:10:100,b=true")
	 */
	public String getDefinedConstantsString()
	{
		int i;
		String s = "";

		for (i = 0; i < mfNumConsts; i++) {
			s += mfConsts.get(i);
			if (i < mfNumConsts - 1)
				s += ",";
		}
		for (i = 0; i < pfNumConsts; i++) {
			if (i == 0 && mfNumConsts > 0)
				s += ",";
			s += pfConsts.get(i);
			if (i < pfNumConsts - 1)
				s += ",";
		}

		return s;
	}

	/**
	 * Get a string showing the values/ranges of all properties file constants. 
	 * (e.g. "x=1,y=1:100,z=1:10:100,b=true")
	 */
	public String getPFDefinedConstantsString()
	{
		int i;
		String s = "";

		for (i = 0; i < pfNumConsts; i++) {
			s += pfConsts.get(i);
			if (i < pfNumConsts - 1)
				s += ",";
		}

		return s;
	}

	public int getNumModelIterations()
	{
		int i, res;

		res = 1;
		for (i = 0; i < mfNumConsts; i++)
			res *= mfConsts.get(i).getNumSteps();

		return res;
	}

	/**
	 * Get the number of ranging constants (constants with range > 1) from the model file.
	 */
	public int getNumModelRangingConstants()
	{
		int i, res;

		res = 0;
		for (i = 0; i < mfNumConsts; i++)
			if (mfConsts.get(i).getNumSteps() > 1)
				res++;

		return res;
	}

	public int getNumPropertyIterations()
	{
		int i, res;

		res = 1;
		for (i = 0; i < pfNumConsts; i++)
			res *= pfConsts.get(i).getNumSteps();

		return res;
	}

	/**
	 * Get the number of ranging constants (constants with range > 1) from the properties file.
	 */
	public int getNumPropertyRangingConstants()
	{
		int i, res;

		res = 0;
		for (i = 0; i < pfNumConsts; i++)
			if (pfConsts.get(i).getNumSteps() > 1)
				res++;

		return res;
	}

	public int getNumIterations()
	{
		return getNumModelIterations() * getNumPropertyIterations();
	}

	/**
	 * Get a list of DefinedConstant objects: one for each ranging constant,
	 * i.e. each constant that has range > 1.
	 */
	public Vector<DefinedConstant> getRangingConstants()
	{
		int i;
		Vector<DefinedConstant> res;

		res = new Vector<DefinedConstant>();
		for (i = 0; i < mfNumConsts; i++)
			if (mfConsts.get(i).getNumSteps() > 1)
				res.addElement(mfConsts.get(i));
		for (i = 0; i < pfNumConsts; i++)
			if (pfConsts.get(i).getNumSteps() > 1)
				res.addElement(pfConsts.get(i));

		return res;
	}

	public void iterateModel()
	{
		int i, ptr;

		// pointer to which constant we are looking at
		ptr = mfNumConsts - 1;
		// cycle backwards through contants
		while (ptr >= -1) {
			// general case
			if (ptr >= 0) {
				if (!mfConsts.get(ptr).incr()) {
					break;
				} else {
					ptr--;
					continue;
				}
			}
			// special case - have reached last iteration and need to restart
			else {
				for (i = 0; i < mfNumConsts; i++) {
					mfConsts.get(i).setValue(mfConsts.get(i).getLow());
				}
				break;
			}
		}

		// create Values objects
		mfValues = new Values();
		for (i = 0; i < mfNumConsts; i++) {
			mfValues.addValue(mfConsts.get(i).getName(), mfConsts.get(i).getValue());
		}
	}

	public void iterateProperty()
	{
		int i, ptr;

		// pointer to which constant we are looking at
		ptr = pfNumConsts - 1;
		// cycle backwards through contants
		while (ptr >= -1) {
			// general case
			if (ptr >= 0) {
				if (!pfConsts.get(ptr).incr()) {
					break;
				} else {
					ptr--;
					continue;
				}
			}
			// special case - have reached last iteration and need to restart
			else {
				for (i = 0; i < pfNumConsts; i++) {
					pfConsts.get(i).setValue(pfConsts.get(i).getLow());
				}
				break;
			}
		}

		// create Values objects
		pfValues = new Values();
		for (i = 0; i < pfNumConsts; i++) {
			pfValues.addValue(pfConsts.get(i).getName(), pfConsts.get(i).getValue());
		}
	}

	public Values getMFConstantValues()
	{
		return mfValues;
	}

	public Values getPFConstantValues()
	{
		return pfValues;
	}
}
