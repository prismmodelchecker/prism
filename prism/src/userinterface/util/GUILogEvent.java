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

public class GUILogEvent extends GUIEvent
{
    public static final int PRINTLN = 0;
    public static final int PRINT = 1;
    public static final int PRINTSEPARATOR = 2;
    public static final int PRINTWARNING = 3;
    
    /**
     * Constructs an instance of <code>GUILogEvent</code> with the specified detail message.
     * @param message the detail message.
     */
    public GUILogEvent(int type, Object message)
    {
        super(type, message);
    }
    
    public GUILogEvent(int type, int message)
    {
        super(type, new Integer(message));
    }
    
    public GUILogEvent(int type, double message)
    {
        super(type, new Double(message));
    }
    
    public GUILogEvent(int type, float message)
    {
        super(type, new Float(message));
    }
    
    public GUILogEvent(int type, long message)
    {
        super(type, new Long(message));
    }
    
    public GUILogEvent(int type, short message)
    {
        super(type, new Short(message));
    }
    
    public GUILogEvent(int type, byte message)
    {
        super(type, new Byte(message));
    }
    
    public GUILogEvent(int type, boolean message)
    {
        super(type, new Boolean(message));
    }
    
    public GUILogEvent(Object message)
    {
        this(PRINTLN, message);
    }
    
    public GUILogEvent(int message)
    {
        this(PRINTLN, new Integer(message));
    }
    
    public GUILogEvent(double message)
    {
        this(PRINTLN, new Double(message));
    }
    
    public GUILogEvent(float message)
    {
        this(PRINTLN, new Float(message));
    }
    
    public GUILogEvent(long message)
    {
        this(PRINTLN, new Long(message));
    }
    
    public GUILogEvent(short message)
    {
        this(PRINTLN, new Short(message));
    }
    
    public GUILogEvent(byte message)
    {
        this(PRINTLN, new Byte(message));
    }
    
    public GUILogEvent(boolean message)
    {
        this(PRINTLN, new Boolean(message));
    }
}
