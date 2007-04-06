//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

package parser;

import java.util.Vector;

import prism.PrismException;

// class to store representation of parsed properties file

public class PropertiesFile
{
	// associated ModulesFile (for constants, ...)
	private ModulesFile modulesFile;
	
	// labels (atomic propositions)
	private LabelList labelList;
	
	// constants
	private ConstantList constantList;
	
	// list of properties
	// (and their associated comments)
	private Vector properties;
	private Vector comments;
	
	// list of all identifiers used
	private Vector allIdentsUsed;
	
	// actual values of constants
	private Values constantValues;
	
	// constructor
	
	public PropertiesFile(ModulesFile mf)
	{
		// initialise
		modulesFile = mf;
		labelList = null;
		constantList = null;
		properties = new Vector();
		comments = new Vector();
		allIdentsUsed = new Vector();
		constantValues = null;
	}
	
	// set up methods - these are called by the parser to create a PropertiesFile object
	
	public void setLabelList(LabelList ll) { labelList = ll; }
	
	public void setConstantList(ConstantList cl) { constantList = cl; }
	
	public void addProperty(PCTLFormula f, String c)
	{
		properties.addElement(f);
		comments.addElement(c);
	}
	
	public void setProperty(int i, PCTLFormula f) { properties.setElementAt(f, i); }
	
	// accessor methods

	public LabelList getLabelList() { return labelList; }
	
	public ConstantList getConstantList() { return constantList; }
	
	public int getNumProperties() { return properties.size(); }
	
	public PCTLFormula getProperty(int i) { return (PCTLFormula)properties.elementAt(i); }
	
	public String getPropertyComment(int i) { return (String)comments.elementAt(i); }
	
	// method to tidy up (called after parsing)
	
	public void tidyUp() throws PrismException
	{
		// check label identifiers
		checkLabelIdents();
		
		// check constant identifiers
		checkConstantIdents();
		// find all instances of constants
		// (i.e. locate idents which are constants)
		findAllConstants();
		// check constants for cyclic dependencies
		constantList.findCycles();
		
		// find all instances of variables
		// (i.e. locate idents which are variables)
		findAllVars();
		
		// check everything is ok
		// (including type checking)
		check();
	}

	// check label identifiers
	// also check reference to these identifiers in properties
	
	private void checkLabelIdents() throws PrismException
	{
		int i, n;
		String s;
		Vector labelIdents;
		
		// go thru labels
		n = labelList.size();
		labelIdents = new Vector();
		for (i = 0; i < n; i++) {
			s = labelList.getLabelName(i);
			// see if ident has been used already for a label
			if (labelIdents.contains(s)) {
				throw new PrismException("Duplicated label name \"" + s + "\"");
			}
			else {
				labelIdents.addElement(s);
			}
		}
		
		// now go thru properties and check that any PCTLLabel objects refer only to existing labels
		n = properties.size();
		for (i = 0; i < n; i++) {
			getProperty(i).checkLabelIdents(labelList);
		}
	}

	// check constant identifiers
	
	private void checkConstantIdents() throws PrismException
	{
		int i, n;
		String s;
		Vector mfIdents, constIdents;
		
		// get idents used in modules file
		mfIdents = modulesFile.getAllIdentsUsed();
		// create vector to store new idents
		constIdents = new Vector();
		
		// go thru constants
		n = constantList.size();
		for (i = 0; i < n; i++) {
			s = constantList.getConstantName(i);
			// see if ident has been used elsewhere
			if (mfIdents.contains(s)) {
				throw new PrismException("Identifier \"" + s + "\" already used in model file");
			}
			else if (constIdents.contains(s)) {
				throw new PrismException("Duplicated identifier \"" + s + "\"");
			}
			else {
				constIdents.addElement(s);
			}
		}
	}

	// find all constants (i.e. locate idents which are constants)
	
	private void findAllConstants() throws PrismException
	{
		int i, n;
		
		// note: we have to look for both constants defined here
		//       and those defined in the modules file
		
		// look in labels
		labelList.findAllConstants(modulesFile.getConstantList());
		labelList.findAllConstants(constantList);
		// look in constants
		constantList.findAllConstants(modulesFile.getConstantList());
		constantList.findAllConstants(constantList);
		// look in properties
		n = properties.size();
		for (i = 0; i < n; i++) {
			setProperty(i, getProperty(i).findAllConstants(modulesFile.getConstantList()));
			setProperty(i, getProperty(i).findAllConstants(constantList));
		}
	}

	// find all variables (i.e. locate idents which are variables)
	
	private void findAllVars() throws PrismException
	{
		int i, n;
		
		// nb: we even check in places where there shouldn't be vars
		//     eg. in constant definitions etc.
		
		// look in labels
		labelList.findAllVars(modulesFile.getVarNames(), modulesFile.getVarTypes());
		// look in constants
		constantList.findAllVars(modulesFile.getVarNames(), modulesFile.getVarTypes());
		// look in properties
		n = properties.size();
		for (i = 0; i < n; i++) {
			setProperty(i, getProperty(i).findAllVars(modulesFile.getVarNames(), modulesFile.getVarTypes()));
		}
	}
		
	// check everything is ok
	
	private void check() throws PrismException
	{
		PCTLFormula f;
		int i, n;
		
		// check labels
		labelList.check();
		
		// check constants
		constantList.check();
		
		// check properties
		n = properties.size();
		for (i = 0; i < n; i++) {
			f = getProperty(i);
			// check property ok
			f.check();
			// check type (nb: will never be a problem but forces type checking throughout)
			if (!(f.getType() == Expression.BOOLEAN || f.getType() == Expression.DOUBLE)) {
				throw new PrismException("Typing error in formula \"" + f + "\"");
			}
		}
	}
	
	// get undefined constants
	
	public Vector getUndefinedConstants()
	{
		return constantList.getUndefinedConstants();
	}
	
	// set values for undefined constants and evaluate all constants
	// always need to call this, even when there are no undefined constants
	// (if this is the case, someValues can be null)
	
	public void setUndefinedConstants(Values someValues) throws PrismException
	{
		// might need values for ModulesFile constants too
		constantValues = constantList.evaluateConstants(someValues, modulesFile.getConstantValues());
	}
	
	// get all constant values
	
	public Values getConstantValues()
	{
		return constantValues;
	}

	// convert to string
	
	public String toString()
	{
		String s = "", tmp, tmp2;
		int i, j, n;
		
		tmp = "" + labelList;
		if (tmp.length() > 0) tmp += "\n";
		s += tmp;
		
		tmp = "" + constantList;
		if (tmp.length() > 0) tmp += "\n";
		s += tmp;
		
		for (i = 0; i < properties.size(); i++) {
			// add comment (if any)
			tmp = getPropertyComment(i);
			if (tmp != null) {
				if (tmp.length() > 0) {
					s += PrismParser.slashCommentBlock(tmp);
				}
			}
			s += getProperty(i) + "\n";
			if (i < properties.size()-1) s += "\n";
		}
		
		return s;
	}

	// convert to string
	// which gives parse trees for each prop
	
	public String toTreeString()
	{
		String s = "", tmp;
		int i, n;
		
		tmp = "" + labelList.toTreeString();
		if (tmp.length() > 0) tmp += "\n";
		s += tmp;
		
		tmp = "" + constantList;
		if (tmp.length() > 0) tmp += "\n";
		s += tmp;
		
		for (i = 0; i < properties.size(); i++) {
			tmp = getPropertyComment(i);
			if (tmp != null) {
				if (tmp.length() > 0) {
					s += PrismParser.slashCommentBlock(tmp);
				}
			}
			s += getProperty(i).toTreeString(0) + "\n";
			if (i < properties.size()-1) s += "\n";
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
