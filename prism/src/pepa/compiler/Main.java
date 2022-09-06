//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Stephen Gilmore <stephen.gilmore@ed.ac.uk> (University of Edinburgh)
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

package pepa.compiler;

import java.lang.reflect.*;
public class Main {

    public static String compile(String fileName) throws InternalError {
	try {
	    Class c = Class.forName("pepa");
	    Method[] m = c.getMethods();
	    // Terminate here if no methods found
	    if (m.length == 0)
		throw new InternalError("Could not locate the methods for the PEPA compiler");

	    // Locate the entry point for the class
	    Method entryPoint = null;
	    for (int i = 0; i < m.length; i++) {
		if (m[i].getName().equals("compile")) {
		    entryPoint = m[i];
		}
	    }
	    // Terminate here if the entry point was not found
	    if (entryPoint == null)
		throw new InternalError("Could not locate the entry point for the PEPA compiler");

	    // Build the arguments for the method
	    Object[] oa = { fileName };
	    Object o = entryPoint.invoke(null, oa);
            if (!(o instanceof String))
		throw new InternalError("Non-string returned by PEPA compile method ");

	    String PRISM_result = (String)o;

            return PRISM_result;
	} catch (ClassNotFoundException cnfe) {
	    throw new InternalError("Could not load the PEPA compiler class from pepa.zip");
	} catch (SecurityException se) {
	    throw new InternalError("Could not secure the PEPA compiler class");
	} catch (IllegalAccessException iae) {
	    throw new InternalError("Could not access the PEPA compiler instance");
	} catch (IllegalArgumentException iarge) {
	    throw new InternalError("Attempted to start the PEPA compiler with the wrong arguments");
	} catch (InvocationTargetException ite) {
	    Throwable t = ite.getTargetException();	    
	    throw new InternalError(t.toString());
	}
    }

    public static void main(String[] args) throws InternalError {
	if (args == null)
	    throw new InternalError("Cannot invoke the PEPA compiler on a null filename");

	if (args.length == 0)
	    throw new InternalError("Must supply a file name to invoke the PEPA compiler");

	if (args.length != 1)
	    throw new InternalError("Cannot invoke the PEPA compiler on more than one file");

	System.out.println(compile(args[0]));
    }
}
