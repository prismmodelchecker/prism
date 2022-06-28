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
        super(type, Integer.valueOf(message));
    }
    
    public GUILogEvent(int type, double message)
    {
        super(type, Double.valueOf(message));
    }
    
    public GUILogEvent(int type, float message)
    {
        super(type, Float.valueOf(message));
    }
    
    public GUILogEvent(int type, long message)
    {
        super(type, Long.valueOf(message));
    }
    
    public GUILogEvent(int type, short message)
    {
        super(type, Short.valueOf(message));
    }
    
    public GUILogEvent(int type, byte message)
    {
        super(type, Byte.valueOf(message));
    }
    
    public GUILogEvent(int type, boolean message)
    {
        super(type, Boolean.valueOf(message));
    }
    
    public GUILogEvent(Object message)
    {
        this(PRINTLN, message);
    }
    
    public GUILogEvent(int message)
    {
        this(PRINTLN, Integer.valueOf(message));
    }
    
    public GUILogEvent(double message)
    {
        this(PRINTLN, Double.valueOf(message));
    }
    
    public GUILogEvent(float message)
    {
        this(PRINTLN, Float.valueOf(message));
    }
    
    public GUILogEvent(long message)
    {
        this(PRINTLN, Long.valueOf(message));
    }
    
    public GUILogEvent(short message)
    {
        this(PRINTLN, Short.valueOf(message));
    }
    
    public GUILogEvent(byte message)
    {
        this(PRINTLN, Byte.valueOf(message));
    }
    
    public GUILogEvent(boolean message)
    {
        this(PRINTLN, Boolean.valueOf(message));
    }
}
