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
import prism.*;

//Prism Packages

import userinterface.util.*;


public class ExpressionLabel extends ExpressionProperty
{
    
    //Attributes
    
    //Note: the following strings are now in SingleProperty
    //private String theString;
    //private String extension;
    private double offsetX;
    private double offsetY;
    private Color theColour;
    private Font theFont;
    private Object parent;
    private boolean selected;
    
    //Debugging
    private ArrayList intersects;
    //Constructors
    
    public static boolean lineLabels;
    
    
    
    public ExpressionLabel (String str, Object parent, Prism pr)
    {
	this(str, parent, "", pr);
    }
    
    
    public ExpressionLabel (String str, Object parent, String extension, Prism pr)
    {
        this(str, parent, extension, true, "", pr);
    }
    
    //New constructor for SingleProperty interface
    public ExpressionLabel (String str, Object parent, String extension, boolean multiline, String comment, Prism pr)
    {
        super((PropertyOwner)parent, extension, str, comment, pr);
	
        offsetX = 0;
        offsetY = 0;
        theColour = Color.black;
        theFont = new Font ("monospaced", Font.PLAIN, 10) ;
        
        selected = false;
        this.parent = parent;
    }
    
    
    
    //Access Methods
    
    /** Access method which states whether this label is currently selected.
     * @return flag to say whether this label is selected.
     */    
    public boolean isSelected ()
    {
        return selected;
    }
    
    /** Access method to return a string representation of this label.  The string
     * representation being simply the string supplied in the constructor or by the
     * setString method. REMAINS FOR BACKWARD COMPATIBILITY
     * @return the string supplied by the constructor or the setString method.
     * 
     */    
    public String getString ()
    {
        return (String)getProperty();
    }
    
    
    
    /** Access method to return the x-offset of this label from the calculated midpoint. */    
    public double getOffsetX ()
    {
        return offsetX;
    }
    
    /** Access method to return the y-offset of this label from the calculated midpoint. */    
    public double getOffsetY ()
    {
        return offsetY;
    }
    
    public Rectangle2D getBounds2D()
    {
        StringTokenizer tokens = new StringTokenizer(getString(), "\n");
        
        int noLines = tokens.countTokens();
        
        double height = (theFont.getSize2D () * noLines)+5;
        double width = 0;
        
        while(tokens.hasMoreTokens())
        {
            double l = theFont.getSize2D ()*tokens.nextToken().length ()*(5.0/8.0);
            if(l > width) width = l;
        }
        
        double parX;
            double parY;
            if(parent instanceof State)
            {
                parX = ((State)parent).getX ();
                parY = ((State)parent).getY ();
            }
            else if(parent instanceof Transition)
            {
                parX = ((Transition)parent).getMiddle ().getX();//dummy
                parY = ((Transition)parent).getMiddle ().getY();//dummy
            }
            else
            {
                parX = 0;
                parY = 0;
            }
            
        double mx = parX + offsetX;
        double my = parY + offsetY-10;
        
        double tx = parX + width +offsetX;
        double ty = parY + height + offsetY-10;
        
        return new Rectangle2D.Double(mx, my, tx-mx, ty-my);
    }
    
    double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, 
    minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
    public void workOutMinsAndMaxs()
    {
        StringTokenizer tokens = new StringTokenizer(getString(), "\n");
        
        int noLines = tokens.countTokens();
        
        double height = (theFont.getSize2D () * noLines)+5;
        double width = 0;
        
        while(tokens.hasMoreTokens())
        {
            double l = theFont.getSize2D ()*tokens.nextToken().length ()*(5.0/8.0);
            if(l > width) width = l;
        }
        
        double parX;
            double parY;
            if(parent instanceof State)
            {
                parX = ((State)parent).getX ();
                parY = ((State)parent).getY ();
            }
            else if(parent instanceof Transition)
            {
                parX = ((Transition)parent).getMiddle ().getX();//dummy
                parY = ((Transition)parent).getMiddle ().getY();//dummy
            }
            else
            {
                parX = 0;
                parY = 0;
            }
            
        minX = parX + offsetX - 5;
        minY = parY + offsetY - 25;
        
        maxX = parX + width +offsetX +5;
        maxY = parY + height + offsetY - 5;
    }
    
    //Always precede a call to the following methods with a call to workOutMinsandMaxs
    
    public double getMinX()
    {
        return minX;
    }
    
    public double getMinY()
    {
        return minY;
    }
    
    public double getMaxX()
    {
        return maxX;
    }
    
    public double getMaxY()
    {
        return maxY;
    }
    
    /** This method calculates whether a given "Hot area" rectangle intersects with this
     * label and returns the result.  The aim of this method is that it can be used to detect whether the
     * position of the mouse is colliding with the label.
     * @param rect A "Hot area" which we are looking for the collision to lie in.
     * @return the result of the collision.
     */    
    public boolean intersects (Rectangle2D rect)
    {
        intersects = new ArrayList();
        if(!getString().equals (""))
        {
            double parX;
            double parY;
            if(parent instanceof State)
            {
                parX = ((State)parent).getX ();
                parY = ((State)parent).getY ();
            }
            else if(parent instanceof Transition)
            {
                parX = ((Transition)parent).getMiddle ().getX();//dummy
                parY = ((Transition)parent).getMiddle ().getY();//dummy
            }
            else
            {
                parX = 0;
                parY = 0;
            }
            
            double x = parX+offsetX;
            double y = parY+offsetY-5;
            String pre = "";
            if(lineLabels)
            {
                pre = getName()+": ";
            }
            
            StringTokenizer tokens = new StringTokenizer(pre+getString(), "\n");
            int i = 0;
            boolean collides = false;
            while(tokens.hasMoreTokens () && !collides)
            {
                double height = theFont.getSize2D ();
                double width = theFont.getSize2D ()*tokens.nextToken().trim().length ()*(5.0/8.0);
                intersects.add(new Rectangle2D.Double (x, y+(i*(height+1.75)-2), width, height));
                collides = (new Rectangle2D.Double (x, y+(i*(height+1.75)-2), width, height)).intersects (rect);
                i++;
            }
            return collides;  
        }
        else return false;
    }
    
    //Update methods
    
    /** Sets the displayed string to the given string parameter. */    
    public void setString (String str) 
    {
        try
        {
        setProperty(str);
        }
        catch(PropertyException e)
        {
            //this should never happen
        }
    }
    
    /** Sets the x-offset to the new given parameter. */    
    public void setOffsetX (double x)
    {
        offsetX = x;
    }
    /** Moves this object relative to its current position by the given x and y
     * co-ordinates.
     * @param x amount to move in the x plain.
     * @param y amount to move in the y plain
     */    
    public void move (double x, double y)
    {
        if(getString().equals("")) return;
        offsetX+=x;
        offsetY+=y;
    }
   
    /** Sets the y-offset to the new given parameter. */    
    public void setOffsetY (double y)
    {
        offsetY = y;
    }
    /** Sets the colour of this label to the given Color object. */    
    public void setColour (Color c)
    {
        theColour = c;
    }
    /** Sets the font of this label to the given Font object. */    
    public void setFont (Font f)
    {
        theFont = f;
    }
    /** Sets the selected flag for this object to the given parameter. */    
    public void setSelected (boolean b)
    {
        selected = b;
    }
    
    // Rendering Methods
    
    /** A rendering method to draw this label to the given Graphics2D object.  Due to
     * the fact that the relative drawing point of a state is its x and y co-ordinates
     * and for a Transition there is a workOutMiddle() method, the relative x and y
     * values must be supplied to this method.  Usually this method will be called from
     * inside a Transition render method or a State render method.
     * @param g2 the Graphics2D component upon which to draw this label.
     * @param x the x position upon which to make relative co-ordinates exact.
     * @param y the y position upon which to make relative co-ordinates exact.
     */    
    public void render (Graphics2D g2, double x, double y)
    {
	render(g2, x, y,false);
    }
    
    /** A rendering method to draw this label to the given Graphics2D object, with the
     * additional option of allowing the "long" lines to be drawn.  Due to
     * the fact that the relative drawing point of a state is its x and y co-ordinates
     * and for a Transition there is a workOutMiddle() method, the relative x and y
     * values must be supplied to this method.  Usually this method will be called from
     * inside a Transition render method or a State render method.
     * @param g2 the Graphics2D component upon which to draw this label.
     * @param x the x position upon which to make relative co-ordinates exact.
     * @param y the y position upon which to make relative co-ordinates exact.
     * @param longLines flag to determine whether the long version of this label should be drawn.
     */    
    public void render (Graphics2D g2, double x, double y, boolean longLines)
    {
        intersects(new Rectangle2D.Double(0,0,1,1));
        StringTokenizer tokens = new StringTokenizer(getString(), "\n");
        if(selected)
        {
            g2.setColor (Color.green);
        }
        else
        {
            g2.setColor (theColour);
        }
        g2.setFont (theFont);
        int i = 0;
		boolean doneLong = false;
        while(tokens.hasMoreTokens())
        {
	    if(doneLong)
		g2.drawString (tokens.nextToken (), (float)(x+offsetX), (float)(y+offsetY+((i*(theFont.getSize()+2)))));
	    else
	    {
		if(!longLines)
                g2.drawString (tokens.nextToken ().trim(), (float)(x+offsetX), (float)(y+offsetY+((i*(theFont.getSize())))+2));
		else g2.drawString (getName()+": "+tokens.nextToken ().trim(), (float)(x+offsetX), (float)(y+offsetY+((i*(theFont.getSize())))+2));
	    }
            i++;
	    doneLong = true;
        }
                
         /*if(intersects != null)
         {
             g2.setColor(Color.magenta);
             for(int j = 0; j < intersects.size(); j++)
             {
                 Rectangle2D rect = (Rectangle2D)intersects.get(j);
                 g2.draw(rect);
             }
         }*/
                
               
    }
    
}
