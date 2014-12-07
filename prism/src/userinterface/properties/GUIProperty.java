//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

package userinterface.properties;

import java.util.Vector;

import javax.swing.*;

import userinterface.GUIPrism;
import param.BigRational;
import parser.*;
import parser.ast.*;
import parser.type.TypeVoid;
import prism.*;

/**
 * Encapsulates a property in the list in the GUI "Properties" tab.
 */
public class GUIProperty
{
	//Constants

	/** A property state constant image */
	public static final ImageIcon IMAGE_NOT_DONE = GUIPrism.getIconFromImage("smallFilePrism.png");
	/** A property state constant image */
	public static final ImageIcon IMAGE_DOING = GUIPrism.getIconFromImage("smallClockAnim1.png");
	/** A property state constant image */
	public static final ImageIcon IMAGE_TICK = GUIPrism.getIconFromImage("smallTick.png");
	/** A property state constant image */
	public static final ImageIcon IMAGE_CROSS = GUIPrism.getIconFromImage("smallCross.png");
	/** A property state constant image */
	public static final ImageIcon IMAGE_ERROR = GUIPrism.getIconFromImage("smallError.png");
	/** A property state constant image */
	public static final ImageIcon IMAGE_INVALID = GUIPrism.getIconFromImage("smallWarning.png");
	/** A property state constant image */
	public static final ImageIcon IMAGE_NUMBER = GUIPrism.getIconFromImage("smallCompute.png");
	/** A property state constant image */
	public static final ImageIcon IMAGE_PARETO = GUIPrism.getIconFromImage("smallPareto.png");
	
	/** Property status */
	public static final int STATUS_NOT_DONE = 0;
	/** Property status */
	public static final int STATUS_DOING = 1;
	/** Property status */
	public static final int STATUS_PARSE_ERROR = 2;
	/** Property status */
	public static final int STATUS_RESULT_ERROR = 3;
	/** Property status */
	public static final int STATUS_RESULT_TRUE = 4;
	/** Property status */
	public static final int STATUS_RESULT_FALSE = 5;
	/** Property status */
	public static final int STATUS_RESULT_NUMBER = 6;
	/** Property status */
	public static final int STATUS_RESULT_PARETO = 7;

	//ATTRIBUTES

	private Prism prism; // Required for parsing

	private String id; // Unique ID
	private int status; // Status - see constants above
	private ImageIcon doingImage; // Image when in DOING state - can be modified externally for animations
	private boolean beingEdited; // Is this property currently being edited?

	private String propString; // String representing the property
	private Expression expr; // The parsed property (null if invalid)
	private String comment; // The property's comment

	private Result result; // Result of model checking etc. (if done, null if not)
	private int numberOfWarnings; // Result of model checking etc. (if done, null if not)
	private String parseError; // Parse error (if property is invalid)

	private String method; // Method used (verification, simulation)
	private String constantsString; // Constant values used
	private String name;
	private Vector<String> referencedNames;
	
	private GUIPropertiesList propList; // to be able to get named properties

	/** Creates a new instance of GUIProperty */
	public GUIProperty(Prism prism, GUIPropertiesList propList, String id, String propString, String name, String comment)
	{
		this.prism = prism;
		this.propList = propList;
		
		this.id = id;
		status = STATUS_NOT_DONE;
		doingImage = IMAGE_DOING;
		beingEdited = false;

		this.propString = propString;
		expr = null;
		this.comment = comment;
		this.name = name;

		result = null;
		parseError = "";
		method = "<none>";
		constantsString = "<none>";
	}

	//ACCESS METHODS

	public String getID()
	{
		return id;
	}

	public int getStatus()
	{
		return status;
	}

	public ImageIcon getImage()
	{
		switch (status) {
		case STATUS_NOT_DONE:
			return IMAGE_NOT_DONE;
		case STATUS_DOING:
			return doingImage;
		case STATUS_PARSE_ERROR:
			return IMAGE_INVALID;
		case STATUS_RESULT_ERROR:
			return IMAGE_ERROR;
		case STATUS_RESULT_TRUE:
			return IMAGE_TICK;
		case STATUS_RESULT_FALSE:
			return IMAGE_CROSS;
		case STATUS_RESULT_NUMBER:
			return IMAGE_NUMBER;
		case STATUS_RESULT_PARETO:
			return IMAGE_PARETO;
		default:
			return IMAGE_NOT_DONE;
		}
	}

	public boolean isBeingEdited()
	{
		return beingEdited;
	}

	public String getPropString()
	{
		return propString;
	}
	
	/**
	 * Returns the name of this property, or {@code null} if the property
	 * has no name.
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * If the property is valid (see {@link #isValid()}), returns a
	 * (potentialy empty) vector containing names of properties
	 * this property references.
	 * <p/>
	 * If the property is not valid, returns {@code null}.
	 */
	public Vector<String> getReferencedNames()
	{
		return this.referencedNames;
	}

	public Expression getProperty()
	{
		return expr;
	}

	public String getComment()
	{
		return comment;
	}

	/**
	 * Is this property valid? i.e. Did it parse OK last time it was parsed?
	 */
	public boolean isValid()
	{
		return expr != null;
	}

	/**
	 * Forgets the validity state of the property, i.e. {@link #isValid()} will
	 * be returning {@code false} until property is parsed OK again.
	 * @return
	 */
	public void makeInvalid() {
		this.expr = null;
		this.referencedNames = null;
	}
	
	/**
	 * Is this property both valid (i.e. parsed OK last time it was checked)
	 * and suitable approximate verification through simulation?
	 */
	public boolean isValidForSimulation()
	{
		return isValid() && prism.isPropertyOKForSimulation(expr);
	}

	public Result getResult()
	{
		return result;
	}
	
	public int getNumberOfWarnings()
	{
		return this.numberOfWarnings;
	}


	public String getResultString()
	{
		return result == null ? "Unknown" : result.getResultString();
	}

	public String getToolTipText()
	{
		switch (status) {
		case STATUS_DOING:
			return "In progress...";
		case STATUS_PARSE_ERROR:
			return "Invalid property: " + parseError;
		case STATUS_RESULT_ERROR:
			return getResultString();
		default:
			return "<html>Result: " + getResultString().replaceAll("\n", "<br/>") + "</html>";
		}
	}

	public String getConstantsString()
	{
		return constantsString;
	}

	public String getMethodString()
	{
		return method;
	}

	public String toString()
	{
		return ((this.name != null) ? ("\"" + this.name + "\" : ") : "") + propString;
	}

	//UPDATE METHODS

	public void setStatus(int status)
	{
		this.status = status;
	}

	public void setDoingImage(ImageIcon image)
	{
		doingImage = image;
	}

	public void setPropStringAndName(String propString, String name, ModulesFile m, String constantsString, String labelString)
	{
		this.propString = propString;
		this.name = name;
		setStatus(STATUS_NOT_DONE);
		propList.validateProperties();
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}

	public void setBeingEdited(boolean beingEdited)
	{
		this.beingEdited = beingEdited;
	}

	public void setResult(Result res)
	{
		result = res;
		if (result.getResult() instanceof Boolean) {
			if (((Boolean) result.getResult()).booleanValue()) {
				setStatus(STATUS_RESULT_TRUE);
			} else {
				setStatus(STATUS_RESULT_FALSE);
			}
		} else if (result.getResult() instanceof Integer) {
			setStatus(STATUS_RESULT_NUMBER);
		} else if (result.getResult() instanceof Double) {
			setStatus(STATUS_RESULT_NUMBER);
		} else if (result.getResult() instanceof BigRational) {
			setStatus(STATUS_RESULT_NUMBER);
		} else if (result.getResult() instanceof Interval) {
			setStatus(STATUS_RESULT_NUMBER);
		} else if (result.getResult() instanceof Exception) {
			setStatus(STATUS_RESULT_ERROR);
		} else if (result.getResult() instanceof TileList) {
			setStatus(STATUS_RESULT_PARETO);
		} else {
			setStatus(STATUS_NOT_DONE);
			result = null;
		}
	}
	
	public void setNumberOfWarnings(int n)
	{
		this.numberOfWarnings = n;
	}

	public void setMethodString(String method)
	{
		this.method = (method == null) ? "<none>" : method;
	}

	public void setConstants(Values mfConstants, Values pfConstants)
	{
		if (mfConstants != null && mfConstants.getNumValues() > 0) {
			constantsString = mfConstants.toString();
			if (pfConstants != null && pfConstants.getNumValues() > 0)
				constantsString += ", " + pfConstants.toString();
		} else if (pfConstants != null && pfConstants.getNumValues() > 0) {
			constantsString = pfConstants.toString();
		} else {
			constantsString = "<none>";
		}
	}

	public void parse(ModulesFile m, String constantsString, String labelString)
	{
		if (propString == null || constantsString == null || labelString == null) {
			expr = null;
			setStatus(STATUS_PARSE_ERROR);
			parseError = "(Unexpected) Properties, constants or labels are null";
			return;
		}
		try {
			//Parse constants and labels
			boolean couldBeNoConstantsOrLabels = false;
			PropertiesFile fConLab = null;
			try {
				fConLab = prism.parsePropertiesString(m, constantsString + "\n" + labelString);
			} catch (PrismException e) {
				couldBeNoConstantsOrLabels = true;
			}
			
			String namedString = "";
			int namedCount = 0;
			//Add named properties
			for (GUIProperty namedProp : this.propList.getAllNamedProperties()) {
				
				if (namedProp.isValid() &&
						(this.name == null || !this.name.equals(namedProp.getName()))) {
					namedCount++;
					namedString += "\"" + namedProp.getName() + "\" : " + namedProp.getPropString() + "\n";
				}
			}
			
			//Parse all together
			String withConsLabs = constantsString + "\n" + labelString + "\n" + namedString + propString;
			PropertiesFile ff = prism.parsePropertiesString(m, withConsLabs);
			
			//Validation of number of properties
			if (ff.getNumProperties() <= namedCount)
				throw new PrismException("Empty Property");
			else if (ff.getNumProperties() > namedCount + 1)
				throw new PrismException("Contains Multiple Properties");

			//Validation of constants and labels
			if (!couldBeNoConstantsOrLabels) {
				if (ff.getConstantList().size() != fConLab.getConstantList().size())
					throw new PrismException("Contains constants");
				if (ff.getLabelList().size() != fConLab.getLabelList().size())
					throw new PrismException("Contains labels");
			} else {
				if (ff.getConstantList().size() != 0)
					throw new PrismException("Contains constants");
				if (ff.getLabelList().size() != 0)
					throw new PrismException("Contains labels");
			}
			//Now set the property
			expr = ff.getProperty(namedCount);
			parseError = "(Unexpected) no error!";
			// if status was previously a parse error, reset status.
			// otherwise, don't set status - reparse doesn't mean existing results should be lost
			if (getStatus() == STATUS_PARSE_ERROR)
				setStatus(STATUS_NOT_DONE);
			
			// get the referenced names
			this.referencedNames = ff.getPropertyObject(namedCount).getAllPropRefsRecursively(ff); 
			
		} catch (PrismException ex) {
			this.expr = null;
			this.referencedNames = null;
			setStatus(STATUS_PARSE_ERROR);
			parseError = ex.getMessage();
		}
	}
	
	@Override
	public int hashCode()
	{
		return (this.propString != null) ? this.propString.length() : 0;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof GUIProperty))
			return false;
		
		return this.id.equals(((GUIProperty) obj).id);
	}
}
