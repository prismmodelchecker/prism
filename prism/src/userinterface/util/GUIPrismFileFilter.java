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

package userinterface.util;

import java.util.Vector;
import java.io.File;
import javax.swing.filechooser.*;

public class GUIPrismFileFilter extends FileFilter
{
	private String name;
	private Vector<String> exts;
	
	public GUIPrismFileFilter()
	{
		name = "";
		exts = new Vector<String>();
	}

	public GUIPrismFileFilter(String s)
	{
		name = s;
		exts = new Vector<String>();
	}

	public void setName(String s)
	{
		name = s;
	}

	public void addExtension(String s)
	{
		exts.add(s);
	}

	public boolean accept(File f)
	{
		String ext;
		
		if (f.isDirectory()) {
			return true;
		}

		ext = getFileExtension(f);
		if (ext != null) {
			return exts.contains(ext);
		}

		return false;
	}
    
	public String getDescription() {
		return name;
	}

	public static String getFileExtension(File f)
	{
 		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i+1).toLowerCase();
		}
		
		return ext;
	}
}

//------------------------------------------------------------------------------
