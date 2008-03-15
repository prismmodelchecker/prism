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

//Prism Package
import userinterface.util.*;

/** This class stores all of the data required to represent a state on a module
 * diagram.  It contains fields for a name, invariant and a flag to say whether it
 * is the initial value.  This class also provides the necessary methods to allow
 * selection of these states and movement around the screen.
 */
public class State implements PropertyOwner
{
    //Attributes
    
    //private StringLabel stateName;
    //private StringLabel invarient;
    private StringLabel comment;
    private BooleanProperty initial;
    
    protected DoubleProperty x, y;
    
    protected double width, height;
    
    private String descriptor;
    
    private Color theColour;
    /** This variable is protected to make it accessible to its sub-classes, for
     * <u>any</u> other use, use the isSelected() and setSelected() methods.
     */
    protected boolean selected;
    protected boolean highlighted;
    protected boolean movingSnap;
    protected double movingOffset;
    protected double movingSubdivisions;
    protected double gridWidth;
    
    private ArrayList associatedTransitions;
    
    //Constructors
    
    /** Creates a new State object with the given x and y co-ordinates, as well as the
     * additional option to state the size.  This size option is there because
     * sub-classes of this object such as Nail and Decision use a different size for
     * their display, but still require the use of State's methods.
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public State(double x, double y, double width, double height)
    {
        
        this.x = new DoubleProperty(this,"x", x, "The screen x-coordinate");
        this.y = new DoubleProperty(this,"y", y, "The screen y-coordinate");
        this.width = width;
        this.height = height;
        //super(x,y,width,height);
        /*stateName = new StringLabel("", this, "Name:");
        stateName.setOffsetX(5);
        stateName.setOffsetY(-5);
        invarient = new StringLabel("", this, "Invarient:");
        invarient.setOffsetX(5);
        invarient.setOffsetY(40);*/
        associatedTransitions = new ArrayList();
        
        descriptor = "0";
        
        comment = new StringLabel("", this, "Comment", true, "An optional description of this state");
        
        initial = new BooleanProperty(this,"Initial", false, "If true, the graphical state variable's initial value is set to the index of this state.");
        selected = true;
        theColour = new Color(175,238,238);
        movingSnap = false;
    }
    /** Creates a new State object with the given x and y co-ordinates, giving the State
     * a default size of 30x30pixels.
     */
    public State(double x, double y)
    {
        this(x,y,30,30);
    }
    
    //Access methods
    
    public Rectangle2D getBounds2D()
    {
        return new Rectangle2D.Double(x.getValue(),y.getValue(),width,height);
    }
    
    /** Access method which states whether this state is the initial state for module in
     * which it lies.  Methods to check whether this is the only initial state must be
     * provided externally.
     */
    public boolean isInitial()
    {
        return initial.getBoolValue();
    }
    
    /** Access method to state whether this State is selected. */
    public boolean isSelected()
    {
        return selected;
    }
    
    public BooleanProperty getInitialProperty()
    {
        return initial;
    }
    
    public String getComment()
    {
        return comment.getProperty().toString();
    }
    
    public double getX()
    {
        return x.getValue();
    }
    
    public double getY()
    {
        return y.getValue();
    }
    
    public double getWidth()
    {
        return width;
    }
    
    public double getHeight()
    {
        return height;
    }
    
    public StringLabel getCommentLabel()
    {
        return comment;
    }
    
    public boolean intersects(Rectangle2D rect)
    {
        return new Ellipse2D.Double(getX(), getY(), getWidth(), getHeight()).intersects(rect);
    }
    
    
    
    //Update Methods
    
    public void associateTransition(Transition t)
    {
        associatedTransitions.add(t);
    }
    
    public void disassociateTransition(Transition t)
    {
        associatedTransitions.remove(t);
    }
    
    /** This method sets the position of the state on the screen to the given x and y
     * co-ordinates.
     */
    public void setPosition(double newX, double newY)
    {
        x.setValue(newX);
        y.setValue(newY);
    }
    
    public void setComment(String str)
    {
        
            comment.setString(str);
        
    }
    
    /** Sets whether this state should be the initial state for this module.  Methods to check whether this is the only initial state must be
     * provided externally.
     */
    public void setInitial(boolean b)
    {
        initial.setBoolValue(b);
    }
    
    /** Sets whether this state should be selected. */
    public void setSelected(boolean b)
    {
     //System.out.println("setting state selected "+b);  
        selected = b;
        //highlighted = false;
    }
    
    public void movingSnap(double offset, double gridWidth, double subdivisions)
    {
        movingSnap = true;
        movingOffset = offset;
        movingSubdivisions = subdivisions;
        this.gridWidth = gridWidth;
    }
    
    public void stopMoving()
    {
        movingSnap = false;
    }
    
    /** Moves this state's co-ordinates by the given offset parameters x and y. */
    public void move(double dx, double dy)
    {
        x.setValue(getX()+dx, false);
        y.setValue(getY()+dy, false);
    }
    
    public boolean isMovingSnap()
    {
        return movingSnap;
    }
    
    public double getGridWidth()
    {
        return gridWidth;
    }
    
    public double getSubdivisions()
    {
        return movingSubdivisions;
    }
    
    public double getMovingOffset()
    {
        return movingOffset;
    }
    
    public double getMinX()
    {
        double min = x.getValue()-8;
        //more here for labels
        //more here for transitions
        
        for(int i = 0; i < associatedTransitions.size() ; i++)
        {
            ////System.out.println("checking for minX in transition "+i);
            Transition t = (Transition)associatedTransitions.get(i);
            if(t.getMinX() < min) min = t.getMinX();
        }
        comment.workOutMinsAndMaxs();
        if(comment.getMinX() < min) min = comment.getMinX();
        return min;
    }
    
    public double getMinY()
    {
        double min = y.getValue()-8;
        //more here for labels
        //more here for transitions
        for(int i = 0; i < associatedTransitions.size() ; i++)
        {
            Transition t = (Transition)associatedTransitions.get(i);
            if(t.getMinY() < min) min = t.getMinY();
        }
        
        comment.workOutMinsAndMaxs();
        if(comment.getMinY() < min) min = comment.getMinY();
        return min;
    }
    
    public double getMaxX()
    {
        double max = x.getValue()+width+8;
        //more here for labels
        //more here for transitions
        for(int i = 0; i < associatedTransitions.size() ; i++)
        {
            Transition t = (Transition)associatedTransitions.get(i);
            if(t.getMaxX() > max) max = t.getMaxX();
        }
        comment.workOutMinsAndMaxs();
        if(comment.getMaxX() > max) max = comment.getMaxX();
        return max;
    }
    
    public double getMaxY()
    {
        double max = y.getValue()+height+8;
        //more here for labels
        //more here for transitions
        for(int i = 0; i < associatedTransitions.size() ; i++)
        {
            Transition t = (Transition)associatedTransitions.get(i);
            if(t.getMaxY() > max) max = t.getMaxY();
        }
        comment.workOutMinsAndMaxs();
        if(comment.getMaxY() > max) max = comment.getMaxY();
        return max;
        
    }
    
    // Renderering Methods
    
    /** Renders this State to the given Graphics2D object with the external index
     * provided.
     * @param g2 the Graphics2D context on which to draw the State.
     * @param i every State should be stored externally in some sort of array.  Its index should
     * be supplied so it can be rendered to the screen.  This is mainly to get rid of
     * surprises in the ordering of exported model files.
     */
    public void render(Graphics2D g2, int i)
    {
        render(g2,  i, false);
    }
    
    /** Renders this State to the given Graphics2D object with the external index
     * provided with the additional choice of stating whether this State's labels
     * should be displayed in long or short format.
     * @param g2 the Graphics2D context on which to draw the State.
     * @param i every State should be stored externally in some sort of array.  Its index should
     * be supplied so it can be rendered to the screen.  This is mainly to get rid of
     * surprises in the ordering of exported model files.
     * @param longLabels parameter to state whether this state should render its labels in long(true) or
     * short(false) format.
     */
    public void render(Graphics2D g2, int i, boolean longLabels)
    {
        double drawX = getX(), drawY = getY();
        //if moving and snapping, temporarily move a to a snap position
        if(movingSnap)
        {
            
            drawX -= movingOffset;
            drawX /= (gridWidth/movingSubdivisions);
            drawX = Math.round(drawX);
            drawX *= (gridWidth/movingSubdivisions);
            drawX += movingOffset;
            
            drawY -= movingOffset;
            drawY /= (gridWidth/movingSubdivisions);
            drawY = Math.round(drawY);
            drawY *= (gridWidth/movingSubdivisions);
            drawY += movingOffset;
        }
        ////System.out.println("x = "+x+ ", y = "+y);
        
        g2.setColor(theColour);
        
        Ellipse2D shape = new Ellipse2D.Double(drawX, drawY, getWidth(), getHeight());
        //System.out.println("Drawing shape: ("+shape.getX()+", "+shape.getY()+", "+shape.getWidth()+", "+shape.getHeight()+")");
        g2.fill(shape);
        if(selected)g2.setColor(Color.green);
        else g2.setColor(Color.black);
        g2.draw(shape);
        String number = ""+i;
        float halfWidth =(g2.getFont().getSize2D()*number.length()*(4.5f/7.0f))/2;
        float halfHeight =((g2.getFont().getSize2D()*(6.0f/7.0f))/2);
        g2.setColor(Color.black);
        g2.drawString(number, ((float)drawX+15-halfWidth), ((float)drawY+15+halfHeight));
        if(isInitial())
        {
            if(selected)g2.setColor(Color.green);
        else g2.setColor(Color.black);
            g2.draw(new Ellipse2D.Double(drawX+5, drawY+5, getWidth()-10, getHeight()-10));
        }
        /*if(selected)
        {
            g2.setColor(Color.white);
            g2.fill(new Rectangle2D.Double(drawX-2, drawY-2, 4, 4));
            g2.fill(new Rectangle2D.Double((drawX+getWidth())-2, drawY-2,4,4));
            g2.fill(new Rectangle2D.Double(drawX-2, (drawY+getHeight())-2, 4, 4));
            g2.fill(new Rectangle2D.Double((drawX+getWidth())-2, (drawY+getHeight())-2, 4, 4));
            g2.setColor(Color.black);
            g2.draw(new Rectangle2D.Double(drawX-2, drawY-2, 4, 4));
            g2.draw(new Rectangle2D.Double((drawX+getWidth())-2, drawY-2,4,4));
            g2.draw(new Rectangle2D.Double(drawX-2, (drawY+getHeight())-2, 4, 4));
            g2.draw(new Rectangle2D.Double((drawX+getWidth())-2, (drawY+getHeight())-2, 4, 4));
        }*/
        
        //stateName.render(g2, x, y, longLabels);
        //invarient.render(g2, x, y, longLabels);
        comment.render(g2, drawX, drawY, longLabels);
        
        
        
        
    }
    
    
    public void setDescriptor(int i)
    {
        descriptor = ""+i;
    }
    
    public int compareTo(Object o)
    {
        if(o instanceof PropertyOwner)
        {
            PropertyOwner po = (PropertyOwner) o;
            if(getUniquePropertyID() < po.getUniquePropertyID() )return -1;
            else if(getUniquePropertyID() > po.getUniquePropertyID()) return 1;
            else return 0;
        }
        else return 0;
    }
    
    public String getClassDescriptor()
    {
        return "State";
    }
    
    public String getDescriptor()
    {
        return descriptor;
    }
    
    public int getNumProperties()
    {
        return 4;
    }
    
    public SingleProperty getProperty(int index)
    {
        switch(index)
        {
            case 0: return initial;
            case 1: return comment;
            case 2: return x;
            default: return y;
        }
    }
    
    public int getUniquePropertyID()
    {
        return PropertyConstants.STATE;
    }
    
    public void registerObserver(java.util.Observer obs)
    {
        comment.addObserver(obs);
        initial.addObserver(obs);
        x.addObserver(obs);
        y.addObserver(obs);
    }
    
    /** Getter for property highlighted.
     * @return Value of property highlighted.
     *
     */
    public boolean isHighlighted()
    {
        return highlighted;
    }
    
    /** Setter for property highlighted.
     * @param highlighted New value of property highlighted.
     *
     */
    /*public void setHighlighted(boolean highlighted)
    {
        this.highlighted = highlighted;
        if(highlighted && selected) selected = false;
    }*/
    
}
