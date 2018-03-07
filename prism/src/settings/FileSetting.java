//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package settings;

import java.awt.*;
import java.io.*;
import javax.swing.*;

public class FileSetting extends Setting 
{
    private static FileRenderer renderer;
    private static FileEditor editor;
    
	private static FileSelector defaultSelector()
	{
		try {
			return new FileSelector()
			{
				JFileChooser choose = new JFileChooser();
				
				public File getFile(Frame parent, File defaultFile)
				{
					choose.setSelectedFile(defaultFile);
					int choice = choose.showOpenDialog(parent);
					if(choice == JFileChooser.CANCEL_OPTION) return null;
					else return choose.getSelectedFile();
					
				}
			};
		}
		// Catch any problems that occur when running in headless mode
		// (e.g. the command-line version of PRISM)
		// (in this case, you don't need this object anyway)
		catch (HeadlessException e) {
			return null;
		}
		catch (Error e) {
			return null;
		}
	}
    
	private boolean validFile;
	private FileSelector selector;
    
    /** Creates a new instance of FileSetting */
	public FileSetting(String name, File value, String comment, SettingOwner owner, boolean editableWhenMultiple)
    {
        super(name, value, comment, owner, editableWhenMultiple);
		if(value != null) validFile = value.isFile();
		else validFile = false;
    }
	
    public FileSetting(String name, File value, String comment, SettingOwner owner, boolean editableWhenMultiple, FontColorConstraint constraint)
    {
        super(name, value, comment, owner, editableWhenMultiple, constraint);
		if(value != null) validFile = value.isFile();
		else validFile = false;
    }
	
	public void checkObjectWithConstraints(Object obj) throws SettingException
	{
		super.checkObjectWithConstraints(obj);
		if(obj instanceof File)
		{
			File f = (File)obj;
			if(f != null) validFile = f.isFile();
			else validFile = false;
		}
		else 
			validFile = false;
	}
    
    public SettingEditor getSettingEditor()
    {
        if (editor == null) {
        	editor = new FileEditor();
        }
        return editor;
    }
    
    public SettingRenderer getSettingRenderer()
    {
        if (renderer == null) {
        	renderer = new FileRenderer();
        }
        return renderer;
    }
    
    public Class getValueClass()
    {
        return File.class;
    }
	
    public File getFileValue()
    {
		if(getValue() != null)
			return (File)getValue();
		else return null;
		
    }
	
	public boolean isValidFile()
	{
		return validFile;
	}
    
	public Object parseStringValue(String string) throws SettingException
	{
		return new File(string);
	}
	
	public String toString()
	{
		if(getFileValue() == null) return "";
		else
		{
			return getFileValue().getPath();
		}
	}
	
	public void setFileSelector(FileSelector selector)
	{
		this.selector = selector;
	}
	
	public FileSelector getFileSelector()
	{
		if (selector == null) {
			selector = defaultSelector();
		}
		return selector;
	}
	
}
