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

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import userinterface.util.*;
import prism.*;

/** A ProbTransition is basically a Transition with an overridden probability
 * attribute.  It also has a different rendering method, allowing it to be drawn
 * with dotted lines.  It is used to represent the line between a Decision and a
 * State (a branch).
 */
public class ProbTransition extends Transition
{
    
    //Attributes
    
    private ExpressionLabel probability;
    
    //Constructors
    
    /** Constructs a ProbTransition with the given from State and to State.  In all
     * cases the from State should be an instanceof Decision.
     */    
    public ProbTransition(State from, State to, Prism pr)
    {
	super(from, to, pr);
	probability = new ExpressionLabel("", this, "Probability", true, "A Boolean expression describing the probability of this choice.", pr);
	probability.setOffsetX(-10);
	probability.setOffsetY(0);
    }
    
    /** Constructs a ProbTransition with the given from State and to State.  In all
     * cases the from State should be an instanceof Decision.  Although a branch can
     * only render one nail, an ArrayList is passed in to make to compatible with the
     * superclass, however, this should only contain <u>one</u> nail or no nails.
     */    
    public ProbTransition(State from, State to, ArrayList nails, Prism pr)
    {
	super(from, to, nails, pr);
	probability = new ExpressionLabel("", this, pr);
	probability.setOffsetX(-10);
	probability.setOffsetY(0);
    }
    
    //Access methods
    
    /** Access method to return the probability for this branch. */    
    public String getProbability()
    {
	return probability.getString();
    }
    
    /** Access method to return the StringLabel object for the probability for this
     * branch.
     */    
    public ExpressionLabel getProbabilityLabel()
    {
	return probability;
    }
    
    /** Access method which states whether a given "Hot-Area" rectangle intersects this
     * branch.
     */    
    public boolean intersects(Rectangle2D rect)
    {
	//rect.setRect(rect.getX()-3, rect.getY()-3, 6, 6);
	boolean doesIt = false;
	double fromX = from.getX()+2.5;
	double fromY = from.getY()+2.5;
	
	for(int i = 0; i < nails.size(); i++)
	{
	    double toX = ((Nail)(nails.get(i))).getX();
	    double toY = ((Nail)(nails.get(i))).getY();
	    
	    if((new Line2D.Double(fromX, fromY, toX, toY)).intersects(rect))
	    { doesIt = true; break; }
	    
	    fromX = toX;
	    fromY = toY;
	}
	if ((new Line2D.Double(fromX, fromY, to.getX()+15, to.getY()+15)).intersects(rect))
	{ doesIt = true; }
	
	return doesIt;
    }
    
    //Update methods
    
    /** Sets the probability to the given parameter. */    
    public void setProbability(String s)
    {
	probability.setString(s);
    }
    
    //Rendering methods
    
    /** Renders this branch the given Graphics2D context with normal style labels. */    
    public void render(Graphics2D g2)
    {
	render(g2, false);
    }
    
    /** Renders this branch the given Graphics2D context with the choice between long
     * and short labels.
     */    
    public void render(Graphics2D g2, boolean longLines)
    {
	float[] dashPattern =
	{ 5, 2 };
	g2.setStroke(new BasicStroke(1.0F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, dashPattern, 0));
	super.render(g2, longLines);
	g2.setStroke(new BasicStroke());
	probability.render(g2, middle.getX(), middle.getY(), longLines);
    }
    
     public String getClassDescriptor()
    {
        return "Probabilistic Choice";
    }
    
    public String getDescriptor()
    {
        return "to "+to.getDescriptor();
    }
    
    public int getNumProperties()
    {
        return 2;
    }
    
    public SingleProperty getProperty(int index)
    {
        switch(index)
        {
            case 0: return probability;
            default: return getAssignmentLabel();
        }
    }
    
    public void registerObserver(Observer obs)
    {
        super.registerObserver(obs);
        probability.addObserver(obs);
    }
    
    public int getUniquePropertyID()
    {
        return prism.PropertyConstants.PROBTRANSITION;
    }
    
    public double getMinX()
    {
        double min = super.getMinX();
	
	probability.workOutMinsAndMaxs();
        if(probability.getMinX() < min) min = probability.getMinX();
        
        return min;
    }
    
    public double getMinY()
    {
        double min = super.getMinY();
	
	probability.workOutMinsAndMaxs();
        if(probability.getMinY() < min) min = probability.getMinY();
        
        return min;
    }
    
    public double getMaxX()
    {
        
        double max = super.getMaxX();
	
	probability.workOutMinsAndMaxs();
        if(probability.getMaxX() > max) max = probability.getMaxX();
        
        return max;
    }
    
    public double getMaxY()
    {
        double max = super.getMaxY();
        if(probability.getMaxY() > max) max = probability.getMaxY();
        
        return max;
    }
    
}
