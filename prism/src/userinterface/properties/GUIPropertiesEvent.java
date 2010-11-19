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

package userinterface.properties;

import userinterface.util.*;
import parser.*;

public class GUIPropertiesEvent extends GUIEvent
{
    public static final int REQUEST_MODEL_BUILD = 0;
    public static final int REQUEST_MODEL_PARSE = 1;
    public static final int EXPERIMENT_START =3;
    public static final int EXPERIMENT_END = 4;
    public static final int PROPERTIES_LIST_CHANGED = 5;
    public static final int VERIFY_END = 6;
    //private UndefinedConstants mfUndefined;
    private Values buildValues;
    
    /** Creates a new instance of GUIPropertiesEvent */
    public GUIPropertiesEvent(int id)
    {
        super(id);
    }
    
    public GUIPropertiesEvent(int id, Values buildValues)
    {
        super(id);
        this.buildValues = buildValues;
    }
    
    public Values getBuildValues()
    {
        return buildValues;
    }
    /*
    public GUIPropertiesEvent(int id, UndefinedConstants mfUndefined)
    {
        super(id);
        this.mfUndefined = mfUndefined;
    }
    
    public UndefinedConstants getMFUndefined()
    {
        return mfUndefined;
    }*/
    
}
