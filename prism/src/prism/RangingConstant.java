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

package prism;

public class RangingConstant
{
    private String name;
    private int type;
    private Object lo, hi, step;
    
    /** Creates a new instance of RangingConstant */
    public RangingConstant(String name, int type, Object lo, Object hi, Object step)
    {
	this.name = name;
	this.type = type;
	this.lo = lo;
	this.hi = hi;
	this.step = step;
    }
    
    //ACCESS METHODS
    
    public String getName()
    {
	return name;
    }
    
    public int getType()
    {
	return type;
    }
    
    public Object getLow()
    {
	return lo;
    }
    
    public Object getHi()
    {
	return hi;
    }
    
    public Object getStep()
    {
	return step;
    }
    
    public int getNumSteps()
    {
	//dummy dummy dummy
	return 2;
    }
    
    /*	Gets the value for the constant at the index i,
     *	eg. if we have 3..2..9, the values would be 
     *  [3, 5, 7, 9] and so getValue(1) would return 5.
     */
    public Object getValue(int i)
    {
	return new Integer(1); //dummy dummy dummy
    }
    
}
