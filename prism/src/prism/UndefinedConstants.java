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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * <br><br>
 * Constants can also be defined to be parameters (for parametric model checking),
 * see {@link #defineUsingParamSwitch(String, boolean)}. These are then no longer
 * treated as undefined constants to be given values, and are available via {@link #getParameters()}.
 * <br><br>
 * Definitions made from strings (via the {@code define...} methods) are remembered as strings,
 * so the exact mode ({@link #setExactMode(boolean)}) can be changed after constants have been defined.
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
	// computation / evaluation of constants via double or exact arithmetic?
	private boolean exact = false;

	// info about constants
	private int mfNumConsts = 0;
	private List<DefinedConstant> mfConsts = null;
	private int pfNumConsts = 0;
	private List<DefinedConstant> pfConsts = null;
	// constants that are parameters (removed from the above)
	private List<DefinedConstant.Parameter> parameters = new ArrayList<>();
	// strings used to define constants (low, high, step), so they can be redefined, e.g. for exact mode
	private Map<String, String[]> definitionStrings = new LinkedHashMap<>();

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

	/**
	 * Reconstruct information about undefined constants from a ResultCollection.
	 * This information is necessarily incomplete.
	 * Hence, consider the information a mockup and do not use the instance to run an experiment.
	 * The provided property does not need to correspond to the data.
	 * It has only a decorative function. 
	 * @param property a property to associate with the results
	 * @param results the ResultsCollection from which the undefined constants are extracted
	 */
	@SuppressWarnings("rawtypes")
	public UndefinedConstants(Property property, ResultsCollection results)
	{
		setModulesFile(null);
		setPropertiesFile(null);
		setProperties(List.of(property));
		// consider all constants pf constants as we do not know the model file
		Vector<DefinedConstant> constants = results.getRangingConstants();
		mfNumConsts = 0;
		pfNumConsts = constants.size();
		// create storage for info about constant definitions
		mfConsts = new ArrayList<DefinedConstant>(mfNumConsts);
		pfConsts = new ArrayList<DefinedConstant>(pfNumConsts);
		// we can add constants safely as they are immutable except for the temporary value
		pfConsts.addAll(constants);
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
	 * Set whether constants are evaluated exactly (real values as BigRationals) or not (as doubles).
	 * This can be called after constants have been defined: definitions made from strings
	 * are then redefined in the new mode, and any iterators are reinitialised.
	 */
	public void setExactMode(boolean exact) throws PrismException
	{
		if (this.exact == exact) {
			return;
		}
		if (definitionStrings.isEmpty()) {
			this.exact = exact;
			return;
		}
		// Keep the current state, to restore if redefinition fails
		// (e.g. a value that is a valid double but not a valid rational)
		List<DefinedConstant> oldMfConsts = mfConsts == null ? null : new ArrayList<>(mfConsts);
		List<DefinedConstant> oldPfConsts = pfConsts == null ? null : new ArrayList<>(pfConsts);
		Map<String, String[]> oldDefinitionStrings = new LinkedHashMap<>(definitionStrings);
		Values oldMfValues = mfValues;
		Values oldPfValues = pfValues;
		this.exact = exact;
		try {
			// Redefine (from the copy, since redefinition updates the map)
			for (Map.Entry<String, String[]> def : oldDefinitionStrings.entrySet()) {
				String[] strs = def.getValue();
				defineConstant(def.getKey(), strs[0], strs[1], strs[2]);
			}
			if (mfValues != null || pfValues != null) {
				initialiseIterators();
			}
		} catch (PrismException e) {
			this.exact = !exact;
			mfConsts = oldMfConsts;
			pfConsts = oldPfConsts;
			definitionStrings = oldDefinitionStrings;
			mfValues = oldMfValues;
			pfValues = oldPfValues;
			throw e;
		}
	}

	/**
	 * Are constants evaluated exactly (real values as BigRationals)?
	 */
	public boolean getExactMode()
	{
		return exact;
	}

	/**
	 * Initialise this UndefinedConstants object:
	 * determine which constants are undefined and then set up data structures.
	 */
	public void initialise()
	{
		List<String> mfv, pfv;
		// determine which constants are undefined
		mfv = (modulesFile == null) ? new ArrayList<>() : modulesFile.getUndefinedConstants();
		if (propertiesFile == null) {
			pfv = new ArrayList<>();
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
	private List<String> orderConstantsByPropertiesFile(List<String> oldList, PropertiesFile propertiesFile)
	{
		List<String> newList = new ArrayList<>();
		List<String> pfList = propertiesFile.getUndefinedConstants();
		for (String s : pfList) {
			if (oldList.contains(s))
				newList.add(s);
		}
		return newList;
	}

	/**
	 * Set up data structures (as required by constructor methods)
	 */
	private void setUpDataStructures(List<String> mfv, List<String> pfv)
	{
		int i;
		String s;
		// determine how many constants there are
		mfNumConsts = mfv.size();
		pfNumConsts = pfv.size();
		// create storage for info about constant definitions
		mfConsts = new ArrayList<DefinedConstant>(mfNumConsts);
		for (i = 0; i < mfNumConsts; i++) {
			s = mfv.get(i);
			Type type = modulesFile.getConstantList().getConstantType(modulesFile.getConstantList().getConstantIndex(s));
			mfConsts.add(new DefinedConstant.Undefined(s, type));
		}
		pfConsts = new ArrayList<DefinedConstant>(pfNumConsts);
		for (i = 0; i < pfNumConsts; i++) {
			s = pfv.get(i);
			Type type = propertiesFile.getConstantList().getConstantType(propertiesFile.getConstantList().getConstantIndex(s));
			pfConsts.add(new DefinedConstant.Undefined(s, type));
		}
		parameters = new ArrayList<>();
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
		return removeConstants(Arrays.asList(constNames));
	}

	/**
	 * Remove some constants. This is used if you decide that you do not want to treat
	 * some constants as undefined once you have created the UndefinedConstants object.
	 * @return how many constants were actually removed.
	 */
	public int removeConstants(Collection<String> constNames)
	{
		int removed = 0;
		for (String name : constNames) {
			if (removeConstant(name))
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
			mfConsts.set(i, mfConsts.get(i).clear());
			;
		}
		// constants from properties file
		for (i = 0; i < pfNumConsts; i++) {
			pfConsts.set(i, pfConsts.get(i).clear());
		}
		definitionStrings.clear();
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

		// a parameter cannot also be given values
		if (isParameter(name)) {
			throw new PrismException("Constant \"" + name + "\" is a parameter, so cannot also be given a value");
		}

		// find out if const is in model or properties file (or neither)
		// also check for overwriting
		index = getMFConstIndex(name);
		if (index != -1) {
			// const is in modules file
			overwrite = (mfConsts.get(index).isDefined());
			mfConsts.set(index, mfConsts.get(index).define(sl, sh, ss, exact));
			definitionStrings.put(name, new String[] { sl, sh, ss });
		} else {
			index = getPFConstIndex(name);
			if (index != -1) {
				// const is in properties file
				overwrite = (pfConsts.get(index).isDefined());
				pfConsts.set(index, pfConsts.get(index).define(sl, sh, ss, exact));
				definitionStrings.put(name, new String[] { sl, sh, ss });
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
	 * Parse the argument to the prism -param command line switch
	 * (format "p,q=0:1/2", i.e., names with optional ranges, which default to 0:1)
	 * into a list of parameters. Their types are not known here, so are null.
	 */
	public static List<DefinedConstant.Parameter> parseParamSwitch(String paramSwitch) throws PrismException
	{
		List<DefinedConstant.Parameter> params = new ArrayList<>();
		if (paramSwitch == null) {
			return params;
		}
		Set<String> names = new HashSet<>();
		for (String paramDef : paramSwitch.split(",")) {
			// ignore blanks
			if (paramDef.trim().isEmpty()) {
				continue;
			}
			DefinedConstant.Parameter param;
			if (!paramDef.contains("=")) {
				param = new DefinedConstant.Parameter(paramDef.trim(), null, "0", "1");
			} else {
				String[] paramDefSplit = paramDef.split("=", 2);
				String name = paramDefSplit[0].trim();
				String range = paramDefSplit[1].trim();
				String[] upperLower = range.split(":");
				if (upperLower.length != 2) {
					throw new PrismException("Invalid range \"" + range + "\" for parameter " + name);
				}
				param = new DefinedConstant.Parameter(name, null, upperLower[0].trim(), upperLower[1].trim());
			}
			if (!names.add(param.getName())) {
				throw new PrismException("Duplicate definitions for parameter \"" + param.getName() + "\"");
			}
			params.add(param);
		}
		return params;
	}

	/**
	 * Check that none of the parameters in the argument to the prism -param command line switch
	 * are also given values in the argument to the -const switch. Unlike the checks done when
	 * defining constants, this applies to all constants, not just those needed here.
	 */
	public static void checkParamSwitchAgainstConstSwitch(String paramSwitch, String constSwitch) throws PrismException
	{
		if (constSwitch == null) {
			return;
		}
		Set<String> constNames = new HashSet<>();
		for (String constDef : constSwitch.split(",")) {
			int j = constDef.indexOf('=');
			if (j > 0) {
				constNames.add(constDef.substring(0, j).trim());
			}
		}
		for (DefinedConstant.Parameter param : parseParamSwitch(paramSwitch)) {
			if (constNames.contains(param.getName())) {
				throw new PrismException("Constant \"" + param.getName() + "\" is a parameter, so cannot also be given a value");
			}
		}
	}

	/**
	 * Define some constants to be parameters, using the argument to the prism -param command line switch
	 * (see {@link #parseParamSwitch(String)}). These are no longer treated as undefined constants.
	 * @param paramSwitch The switch argument
	 * @param useAll If true, throw an exception if a parameter is not one of the undefined constants
	 */
	public void defineUsingParamSwitch(String paramSwitch, boolean useAll) throws PrismException
	{
		for (DefinedConstant.Parameter param : parseParamSwitch(paramSwitch)) {
			defineParameter(param.getName(), param.getLower(), param.getUpper(), useAll);
		}
	}

	/**
	 * Define a constant to be a parameter, with a range. It is no longer treated as an undefined constant.
	 * @param name The name of the constant
	 * @param lower The lower bound of the range
	 * @param upper The upper bound of the range
	 * @param useAll If true, throw an exception if {@code name} is not one of the undefined constants
	 * @return True if the constant was found (and so is now a parameter)
	 */
	public boolean defineParameter(String name, String lower, String upper, boolean useAll) throws PrismException
	{
		if (isParameter(name)) {
			throw new PrismException("Duplicate definitions for parameter \"" + name + "\"");
		}
		DefinedConstant<?> constant = null;
		int index = getMFConstIndex(name);
		if (index != -1) {
			constant = mfConsts.get(index);
		} else {
			index = getPFConstIndex(name);
			if (index != -1) {
				constant = pfConsts.get(index);
			}
		}
		if (constant == null) {
			if (useAll) {
				throw new PrismException("Parameter \"" + name + "\" is not an undefined constant of the model or properties");
			}
			return false;
		}
		if (definitionStrings.containsKey(name)) {
			throw new PrismException("Constant \"" + name + "\" has been given a value, so cannot also be a parameter");
		}
		removeConstant(name);
		parameters.add(new DefinedConstant.Parameter(name, constant.getType(), lower, upper));
		return true;
	}

	/**
	 * Is the constant {@code name} a parameter?
	 */
	public boolean isParameter(String name)
	{
		return parameters.stream().anyMatch(p -> p.getName().equals(name));
	}

	/**
	 * Get the constants that are parameters, in the order they were defined.
	 */
	public List<DefinedConstant.Parameter> getParameters()
	{
		return new ArrayList<>(parameters);
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
	 * Get the number of ranging constants (constants with range greater than 1) from the model file.
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
	 * Get the number of ranging constants (constants with range greater than 1) from the properties file.
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
	 * i.e. each constant that has range greater than 1.
	 */
	public Vector<DefinedConstant> getRangingConstants()
	{
		Vector<DefinedConstant> res = new Vector<DefinedConstant>();
		for (int i = 0; i < mfNumConsts; i++)
			if (mfConsts.get(i).getNumSteps() > 1)
				res.addElement(mfConsts.get(i));
		for (int i = 0; i < pfNumConsts; i++)
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

	/**
	 * Get the values for non-ranging model constants, i.e. each constant that has range 1.
	 */
	public Values getNonRangingConstantValues()
	{
		Values vals = new Values();
		for (int i = 0; i < mfNumConsts; i++)
			if (mfConsts.get(i).getNumSteps() == 1)
				vals.addValue(mfConsts.get(i).getName(), mfConsts.get(i).getValue());
		for (int i = 0; i < pfNumConsts; i++)
			if (pfConsts.get(i).getNumSteps() == 1)
				vals.addValue(pfConsts.get(i).getName(), pfConsts.get(i).getValue());
		return vals;
	}
}
