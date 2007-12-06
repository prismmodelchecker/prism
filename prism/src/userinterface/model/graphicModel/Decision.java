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

import java.awt.geom.*;
import java.awt.*;
import userinterface.util.*;


/** This class overrides State, to take advantage of its drag and drop access
 * features.  Its only difference is the way it is identified of and instanceof
 * Decision and the fact that it is rendered as a small black dot.
 * <p>
 * This is intended to be used as the "joint" for branched transitions.  A normal
 * Transition should point to this Decision and any number of ProbTransitions can
 * come from this "joint".
 */
public class Decision extends State
{
    
    //Constructor
    
    /** Creates a new instance of Decision at the specified co-ordinates
     * @param x x co-ordinate for Decision node
     * @param y y co-ordinate for Decison node
     *
     */
    public Decision(double x, double y)
    {
        super(x,y,6,6);
    }
    
    //Rendering Methods
    
    /** This method draws this node to the given Graphics2D context.
     * @param g2 the graphics context on which to draw the node
     * @param i needed to override State's render method.  It is actually the State's external
     * ID number.
     */
    public void render(Graphics2D g2, int i)
    {
        double drawX = getX(), drawY = getY();
        //if moving and snapping, temporarily move a to a snap position
        if(movingSnap)
        {
            
            //drawX -= 3;
            drawX += 3;
            drawX /= (gridWidth/movingSubdivisions);
            drawX = Math.round(drawX);
            drawX *= (gridWidth/movingSubdivisions);
            drawX -= 3;
            
            
            //drawY -= 3;
            drawY += 3;
            drawY /= (gridWidth/movingSubdivisions);
            drawY = Math.round(drawY);
            drawY *= (gridWidth/movingSubdivisions);
            drawY -=3;
        }
        
        
        
        
        
        if(!selected)g2.setColor(Color.black);
        else g2.setColor(Color.green);
        Ellipse2D draw = new Ellipse2D.Double(drawX, drawY, getWidth(), getHeight());
        g2.fill(draw);
        
        g2.draw(draw);
        
        
        
    }
    /** This method draws this node to the given Graphics2D context.  There is also the
     * option of turning on "long-lines", but this is only required to override the
     * superclass render method.
     * @param g2 the graphics context on which to draw the node
     * @param i needed to override State's render method.  It is actually the State's external
     * ID number.
     * @param lines needed to override superclass render method.
     */
    public void render(Graphics2D g2, int i, boolean lines)
    {
        render(g2, i);
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
        return PropertyConstants.DECISION;
    }
    
    public void registerObserver(java.util.Observer obs)
    {
        x.addObserver(obs);
        y.addObserver(obs);
    }
    
    public String getClassDescriptor()
    {
        return "Probability Distribution";
    }
    
    public String getDescriptor()
    {
        return "";
    }
}
