//==============================================================================
//	
//	Copyright (c) 2002-2006 Andrew Hinton, Dave Parker
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

package chart;

/** This is a very simple class to store information about a point on the graph.
 * The idea is that this class can be overridden is needed to store more complex
 * data and have a more complex getDescription method which is used to display
 * tooltips for each point. This has no reference to the containing data structure
 * so any modifications must be dealt with externally.
 */
public class GraphPoint
{
    
    //Constants
    public static final double NULL_COORD = Double.NEGATIVE_INFINITY;
    
    //Attributes
    
    private double xCoord;
    private double yCoord;

	private MultiGraphModel theModel;
    
    //Constructor
    
    /** Creates a new GraphPoint with a given x and y co-ordinate.
     * @param x the x coordinate
     * @param y the y coordinate
     */    
    public GraphPoint(double x, double y, MultiGraphModel theModel)
    {
		this.theModel = theModel;
        xCoord = x;
        yCoord = y;
    }
    
    public GraphPoint(MultiGraphModel theModel)
    {
        this(NULL_COORD, NULL_COORD, theModel);
    }
    
    //Access Methods
    /** Access method to return this point's x-coordinate */    
    public double getXCoord()
    {
        return xCoord;
    }
    
    /** Access method to return this point's y-coordinate */    
    public double getYCoord()
    {
        return yCoord;
    }
    
    public void setXCoord(double d)
    {
        xCoord = d;
		//theModel.workOutAndSetXScales();
    }
    
    public void setYCoord(double d)
    {
        yCoord = d;
		//theModel.workOutAndSetYScales();
    }
    
	// Check that point is valid (x/y not +/- infnity or NaN)
	
	public boolean isValid()
	{
		// x = +/- infinity
		if (xCoord == Double.POSITIVE_INFINITY || xCoord == Double.NEGATIVE_INFINITY) return false;
		// x = NaN
		if (xCoord != xCoord) return false;
		// y = +/- infinity
		if (yCoord == Double.POSITIVE_INFINITY || yCoord == Double.NEGATIVE_INFINITY) return false;
		// y = NaN
		if (yCoord != yCoord) return false;
		// valid
		return true;
	}
	
    /** Returns a string describing this point.  This method is used to generate the
     * tooltips.
     */    
    public String getDescription()
    {
        return "("+(float)xCoord+", "+(float)yCoord+")";
    }
}
