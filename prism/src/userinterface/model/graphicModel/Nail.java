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

package userinterface.model.graphicModel;

//Java Packages

import java.awt.geom.*;
import userinterface.util.*;


/** This class is overrides State to gain access to its drag and drop facilities.
 * It is rendered by the transition it belongs to.  It is intended to represent
 * changes in direction of a transition line.  Normal Transitions can contain any
 * number of nails, however, branched transitions are limited to one per branch.
 * This class also contains the States(which could be other nails) which a
 * particular nail's transitions are connected to and from.
 */
public class Nail extends State
{
    
    //Attributes
    
    private State from;
    private State to;
    
    private Transition owner;
    
    //Constructor
    
    /** Creates a new Nail object with the given c and y co-ordinates along with the
     * state this nail's transition is coming from and going to.
     */    
    public Nail (double x, double y, State from, State to)
    {
        super(x,y);
        width = 2;
        height = 2;
        this.from = from;
        this.to = to;
    }
    
    //Access Methods
    
    /** Returns the State object this nail's transition is coming from. */    
    public State getFrom()
    {
        return from;
    }
    
    /** Returns the State object this nail's transition is going to. */    
    public State getTo()
    {
        return to;
    }
    
    public boolean intersects(Rectangle2D rect)
    {
        return new Ellipse2D.Double(getX()-2, getY()-2, 4, 4).intersects(rect);
    }
    
    //Update Methods
    
    public void delete()
    {
        if(owner != null)owner.deleteNail(this);
    }
    
    //Override super
    public void associateTransition(Transition t)
    {
        super.associateTransition(t);
        owner = t;
    }
    
    
    /** Sets the State this nail's transition is coming from. */    
    public void setFrom(State s)
    {
        from = s;
    }
    
    /** Sets the State this nail's transition is going to. */    
    public void setTo(State s)
    {
        to = s;
    }
    
    public String getClassDescriptor()
    {
        return "Point";
    }
    
    public String getDescriptor()
    {
        return "";//from.getDescriptor()+"-->"+to.getDescriptor();
    }
    
    public int getNumProperties()
    {
        return 2;
    }
    
    public SingleProperty getProperty(int index)
    {
        switch(index)
        {
            case 0: return x;
            default: return y;
        }
    }
    
    public int getUniquePropertyID()
    {
        return PropertyConstants.NAIL;
    }
    
    
    
}
