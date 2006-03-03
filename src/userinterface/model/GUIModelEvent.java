//==============================================================================
//	
//	Copyright (c) 2002-2004, Andrew Hinton, Dave Parker
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
import userinterface.*;
import parser.ModulesFile;
import prism.Model;
import parser.Values;
import userinterface.util.*;
/**
 *
 * @author  ug60axh
 */
public class GUIModelEvent extends GUIEvent
{
    //CONSTANTS
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
    
    //DATA ATTRIBUTES
    private ModulesFile file;
    private Model model;
    private Values buildValues;
    
    /** Creates a new instance of GUIModelEvent */
    public GUIModelEvent(int id, ModulesFile file)
    {
        super(id);
        this.file = file;
        this.model = null;
    }
    
    public GUIModelEvent(int id, Model model)
    {
        super(id);
        this.model = model;
        this.file = null;
    }
    
    public GUIModelEvent(int id, Model model, Values buildValues)
    {
        super(id);
        this.model = model;
        this.file = null;
        this.buildValues = buildValues;
    }
    
    public GUIModelEvent(int id)
    {
        super(id);
        this.model = null;
        this.file = null;
    }
    
    public ModulesFile getModulesFile()
    {
        return file;
    }
    
    public Model getModel()
    {
        return model;
    }
    
    public Values getBuildValues()
    {
        return buildValues;
    }
    
}
