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

import parser.Values;
import parser.ast.ModulesFile;
import userinterface.util.GUIEvent;

public class GUIModelEvent extends GUIEvent
{
	// CONSTANTS
	public static final int NEW_MODEL = 0; //when no parsed or built model
	public static final int LOAD_MODEL = 1; //when model is loaded into text editor
	public static final int IMPORT_MODEL = 2; //when model is imported into text editor
	public static final int MODEL_PARSED = 3; // when parsed model exists
	public static final int MODIFIED_SINCE_SAVE = 4; //when text is modified
	public static final int SAVE_MODEL = 5; //when text is saved to file
	public static final int MODEL_BUILT = 6; //when built model exists
	public static final int MODEL_PARSE_FAILED = 7; //when a failed model parse happens
	public static final int MODEL_BUILD_FAILED = 8; //when a failed model build happens
	public static final int NEW_LOAD_NOT_RELOAD_MODEL = 9;

	// DATA ATTRIBUTES
	private ModulesFile file;
	private Values buildValues;

	/** Creates a new instance of GUIModelEvent */
	public GUIModelEvent(int id, ModulesFile file)
	{
		super(id);
		this.file = file;
	}

	public GUIModelEvent(int id, Values buildValues)
	{
		super(id);
		this.file = null;
		this.buildValues = buildValues;
	}

	public GUIModelEvent(int id)
	{
		super(id);
		this.file = null;
	}

	public ModulesFile getModulesFile()
	{
		return file;
	}

	public Values getBuildValues()
	{
		return buildValues;
	}
}
