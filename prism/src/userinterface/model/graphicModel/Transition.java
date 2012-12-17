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
import java.util.*;
import java.awt.*;
import userinterface.util.*;
import prism.*;


/** This class stores information about simple transitions between States.  In its
 * simplest form it contains a from State and a to State, and it is simply rendered
 * as a line with an appropriate arrow between the two State's midpoint
 * co-ordinates.  If the from is pointed towards a Decision State it is rendered
 * differently and cannot contain nails.  Nails are used to provide breaks in the
 * transition line allowing transitions the ability to have nodes which stick to
 * the canvas.  These Nails can be moved around the screen.
 * <p>
 * Each transition contains data concerning its:
 * <ul>
 *    <li>Guard
 *    <li>Sync
 *    <li>Probability
 *    <li>Assignments
 * </ul>
 */
public class Transition implements PropertyOwner
{
    
    
    //Attributes
    /** This variable is the from State of this transition, it is protected <u>only</u>
     * to make it accessible to its subclasses.
     */
    protected State from;
    /** This variable is an ArrayList of nails.  It is protected to make it accessible
     * to its subclasses.  It is not designed to be accessed externally, so please use
     * the getNails() method to access this.
     */
    protected ArrayList nails;
    /** This variable is the to State of this transition, it is protected <u>only</u>
     * to make it accessible to its subclasses.
     */
    protected State to;
    
    protected Prism pr;
    
    private ExpressionLabel guard;
    private StringLabel sync;
    private StringLabel assignment;
    private ExpressionLabel probability;
    
    private boolean docked;
    private boolean selected;
    
    /** The current point which has been calculated as the "midpoint" of this
     * transition.
     */
    protected Point2D middle;
    
    
    //Debugging
    private ArrayList intersects;
    
    //Constructors
    
    /** Creates a new Transition object with the given to and from states. */
    public Transition(State from, State to, Prism pr)
    {
        this.from = from;
        this.to = to;
        this.pr = pr;
        guard = new ExpressionLabel("", this, "Guard", true, "A Boolean expression that describes the when the transition can occur.", pr);
        guard.setOffsetX(-10);
        guard.setOffsetY(-10);
        guard.setColour(Color.blue);
        sync = new StringLabel("", this, "Sync", false, "A string describing the name of the synchronisation action for this transition.  If this is left blank, the transition is performed asynchronously.");
        sync.setOffsetX(-10);
        sync.setOffsetY(0);
        sync.setColour(new Color(216,168,23));
        assignment = new StringLabel("", this, "Assignment", true, "A list of '&'-separated local variable updates.");
        assignment.setOffsetX(-10);
        assignment.setOffsetY(10);
        assignment.setColour(Color.red);
        probability = new ExpressionLabel("", this, "Probability", true, "A Boolean expression that describes the probability of this transition.", pr);
        probability.setOffsetX(-10);
        probability.setOffsetY(-20);
        probability.setColour(Color.black);
        nails = new ArrayList();
        docked = true;
        selected = false;
        
        from.associateTransition(this);
        to.associateTransition(this);
        workOutMiddle();
    }
    /** Creates a new Transition with the given to and from states and an ArrayList of
     * nails.
     */
    public Transition(State from, State to, ArrayList nails, Prism pr)
    {
        this(from, to, pr);
        this.nails = nails;
        
        for(int i = 0; i < getNumNails(); i++)
        {
            getNail(i).associateTransition(this);
        }
        workOutMiddle();
    }
    
    //Access Methods
    
    public Rectangle2D getBounds2D()
    {
        double minX = from.getX()+15;
        if(to.getX()+15 < minX) minX = to.getX()+15;
        Nail n;
        
        for(int i = 0; i < getNumNails(); i++)
        {
            n = getNail(i);
            if(n.getX() < minX) minX = n.getX();
        }
        
        double maxX = from.getX() + 15;
        if(to.getX()+15 > maxX) maxX = to.getX()+15;
        
        
        for(int i = 0; i < getNumNails(); i++)
        {
            n = getNail(i);
            if(n.getX() > maxX) maxX = n.getX();
        }
        
        double minY = from.getY() + 15;
        if(to.getY() + 15 < minY) minY = to.getY() +15;
        
        
        for(int i = 0; i < getNumNails(); i++)
        {
            n = getNail(i);
            if(n.getY() < minY) minY = n.getY();
        }
        
        double maxY = from.getY() + 15;
        if(to.getY() + 15> maxY) maxY = to.getY() + 15;
        
        
        for(int i = 0; i < getNumNails(); i++)
        {
            n = getNail(i);
            if(n.getY() > maxY) maxY = n.getY();
        }
        
        return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
    }
    
    /** Access method to return the ArrayList of nail objects for this transition. */
    public ArrayList getNails()
    {
        return nails;
    }
    
    /** Access method to return the from State object for this transition. */
    public State getFrom()
    {
        return from;
    }
    
    /** Access method to return the to State object for this transition. */
    public State getTo()
    {
        return to;
    }
    
    public int getNumNails()
    {
        return nails.size();
    }
    
    public Nail getNail(int i )
    {
        return (Nail)nails.get(i);
    }
    
    /** Access method to return the StringLabel object holding data concerning this
     * transition's guard String.
     */
    public ExpressionLabel getGuardLabel()
    {
        return guard;
    }
    
    /** Access method to return the StringLabel object holding data concerning this
     * transition's sync String.
     */
    public StringLabel getSyncLabel()
    {
        return sync;
    }
    
    /** Access method to return the StringLabel object holding data concerning this
     * transition's probability String.
     */
    public ExpressionLabel getProbabilityLabel()
    {
        return probability;
    }
    
    /** Access method to return the StringLabel object holding data concerning this
     * transition's assignment String.
     */
    public StringLabel getAssignmentLabel()
    {
        return assignment;
    }
    
    /** Access method to return a Point2D object representing the mid-point of this
     * transition which has been calculated previously by the workOutMiddle() method.
     */
    public Point2D getMiddle()
    {
        return middle;
    }
    
    public double getMinX()
    {
        double min = from.getX() - 10;
        if(to.getX() - 15 < min) min = to.getX() - 15 + 10;
        Nail n;
        
        for(int i = 0; i < getNumNails(); i++)
        {
            n = getNail(i);
            if(n.getX()-4 < min) min = n.getX()-4;
        }
        
        //Now do labels
        
        guard.workOutMinsAndMaxs();
        sync.workOutMinsAndMaxs();
        probability.workOutMinsAndMaxs();
        assignment.workOutMinsAndMaxs();
        
        if(guard.getMinX() < min) min = guard.getMinX();
        if(sync.getMinX() < min) min = sync.getMinX();
        if(probability.getMinX() < min) min = probability.getMinX();
        if(assignment.getMinX() < min) min = assignment.getMinX();
        
        return min;
    }
    
    public double getMaxX()
    {
        double max = from.getX() + 15 + 10;
        if(to.getX() + 15 + 10 > max) max = to.getX() + 15 + 10;
        Nail n;
        
        for(int i = 0; i < getNumNails(); i++)
        {
            n = getNail(i);
            if(n.getX()+4 > max) max = n.getX()+4;
        }
        
        //Now do labels
        
        guard.workOutMinsAndMaxs();
        sync.workOutMinsAndMaxs();
        probability.workOutMinsAndMaxs();
        assignment.workOutMinsAndMaxs();
        
        if(guard.getMaxX() > max) max = guard.getMaxX();
        if(sync.getMaxX() > max) max = sync.getMaxX();
        if(probability.getMaxX() > max) max = probability.getMaxX();
        if(assignment.getMaxX() > max) max = assignment.getMaxX();
        
        return max;
    }
    
    public double getMinY()
    {
        double min = from.getY() - 10;
        if(to.getY() - 15 < min) min = to.getY() - 15 + 10;
        Nail n;
        
        for(int i = 0; i < getNumNails(); i++)
        {
            n = getNail(i);
            if(n.getY()-4 < min) min = n.getY()-4;
        }
        
        //Now do labels
        
        guard.workOutMinsAndMaxs();
        sync.workOutMinsAndMaxs();
        probability.workOutMinsAndMaxs();
        assignment.workOutMinsAndMaxs();
        
        if(guard.getMinY() < min) min = guard.getMinY();
        if(sync.getMinY() < min) min = sync.getMinY();
        if(probability.getMinY() < min) min = probability.getMinY();
        if(assignment.getMinY() < min) min = assignment.getMinY();
        
        return min;
    }
    
    public double getMaxY()
    {
        double max = from.getY() + 15 + 10;
        if(to.getY() + 15 + 10 > max) max = to.getY() + 15 + 10;
        Nail n;
        
        for(int i = 0; i < getNumNails(); i++)
        {
            n = getNail(i);
            if(n.getY()+4 > max) max = n.getY()+4;
        }
        
        //Now do labels
        
        guard.workOutMinsAndMaxs();
        sync.workOutMinsAndMaxs();
        probability.workOutMinsAndMaxs();
        assignment.workOutMinsAndMaxs();
        
        if(guard.getMaxY() > max) max = guard.getMaxY();
        if(sync.getMaxY() > max) max = sync.getMaxY();
        if(probability.getMaxY() > max) max = probability.getMaxY();
        if(assignment.getMaxY() > max) max = assignment.getMaxY();
        
        return max;
    }
    
    /** Access method to state whether this Transition is docked. */
    public boolean isDocked()
    {
        return docked;
    }
    
    /** Calculates whether this transition collides with a given "Hot Area" rectangle.
     * This includes transitions with multiple nails, or no nails.
     */
    public boolean intersects(Rectangle2D rect)
    {
        intersects = new ArrayList();
        //rect.setRect(rect.getX()-2, rect.getY()-2, rect.getWidth()+4, rect.getHeight()+4);
        boolean doesIt = false;
        double fromX = from.getX()+(from.getWidth()/2);
        double fromY = from.getY()+(from.getWidth()/2);
        
        for(int i = 0; i < nails.size(); i++)
        {
            double toX = ((Nail)(nails.get(i))).getX();
            double toY = ((Nail)(nails.get(i))).getY();
            intersects.add((new Line2D.Double(fromX, fromY, toX, toY)));
            if((new Line2D.Double(fromX, fromY, toX, toY)).intersects(rect))
            { doesIt = true; break; }
            
            fromX = toX;
            fromY = toY;
        }
        intersects.add((new Line2D.Double(fromX, fromY, to.getX()+(to.getWidth()/2), to.getY()+(to.getWidth()/2))));
        if ((new Line2D.Double(fromX, fromY, to.getX()+(to.getWidth()/2), to.getY()+(to.getWidth()/2))).intersects(rect))
        { doesIt = true; }
        
        ////System.out.println("does it intersect? "+doesIt);
        return doesIt;
    }
    
    //Update Methods
    
    public void deleteNail(Nail n)
    {
        nails.remove(n);
        n.disassociateTransition(this);
    }
    
    /** Sets the guard String to the given parameter. */
    public void setGuard(String str)
    {
        guard.setString(str);
    }
    
    /** Sets the sync String to the given parameter. */
    public void setSync(String str)
    {
        sync.setString(str);
    }
    
    /** Sets the assignment String to the given parameter. */
    public void setAssignment(String str)
    {
        assignment.setString(str);
    }
    
    /** Sets the probability String to the given parameter. */
    public void setProbability(String str)
    {
        probability.setString(str);
    }
    
    /** Access method to state whether this transition is selected. */
    public boolean isSelected()
    {
        return selected;
    }
    
    /** Sets the state this Transition is pointing to to the given parameter. */
    public void setTo(State to)
    {
        this.to = to;
    }
    
    /** Sets this Transition as "docked" or not "docked".  This means it has been placed properly.  A
     * transition is only not "docked" when it is being drawn.  This is used for
     * temporary drawing Transitions.
     */
    public void setDocked(boolean b)
    {
        docked = b;
    }
    
    /** Sets whether this Transition is selected. */
    public void setSelected(boolean b)
    {
        selected = b;
        // for(int i = 0; i < nails.size(); i++)
        //     ((Nail)nails.get(i)).setSelected(b);
        
        //setSelectedButOnlyHighlightNails(b);
        
    }
    /*
    public void setSelectedButOnlyHighlightNails(boolean b)
    {
       // selected = b;
        //for(int i = 0; i < nails.size(); i++)
        //    ((Nail)nails.get(i)).setHighlighted(b);
    }
     */
    
    
    /** Sets the "middle" attribute according to the midpoint of this transition.  It is
     * calculated as the midpoint of the greatest distance between any two nails/States
     * in each plain.
     */
    
    public void workOutMiddle()
    {
        ArrayList poly = new ArrayList();
        double fromX = from.getX()+15;
        double fromY = from.getY()+15;
        double gridWidth = from.getGridWidth();
        double movingSubdivisions = from.getSubdivisions();
        double movingOffset = from.getMovingOffset();
        if(from.isMovingSnap())
        {
            if(from instanceof Decision)
            {
                fromX +=3;
                fromX /= (gridWidth/movingSubdivisions);
                fromX = Math.round(fromX);
                fromX *= (gridWidth/movingSubdivisions);
                fromX -= 3;
                //fromX -= 3;
                
                //drawY -= 3;
                fromY += 3;
                fromY /= (gridWidth/movingSubdivisions);
                fromY = Math.round(fromY);
                fromY *= (gridWidth/movingSubdivisions);
                fromY -= 3;
                //fromY -= 3;
            }
            else
            {
                fromX -= movingOffset;
                fromX /= (gridWidth/movingSubdivisions);
                fromX = Math.round(fromX);
                fromX *= (gridWidth/movingSubdivisions);
                fromX += movingOffset;
                
                fromY -= movingOffset;
                fromY /= (gridWidth/movingSubdivisions);
                fromY = Math.round(fromY);
                fromY *= (gridWidth/movingSubdivisions);
                fromY += movingOffset;
            }
        }
        
        
        poly.add( new java.awt.Point((int)fromX, (int)fromY));
        for(int i = 0; i < nails.size(); i++)
        {
            Nail toN = (Nail)(nails.get(i));
            double toX = toN.getX();
            double toY = toN.getY();
            gridWidth = toN.getGridWidth();
            movingSubdivisions = toN.getSubdivisions();
            movingOffset = toN.getMovingOffset();
            if(toN.isMovingSnap())
            {
                toX -= movingOffset;
                toX /= (gridWidth/movingSubdivisions);
                toX = Math.round(toX);
                toX *= (gridWidth/movingSubdivisions);
                toX += movingOffset;
                
                toY -= movingOffset;
                toY /= (gridWidth/movingSubdivisions);
                toY = Math.round(toY);
                toY *= (gridWidth/movingSubdivisions);
                toY += movingOffset;
            }
            
            poly.add(new java.awt.Point((int)toX, (int)toY));
            
            
        }
        double toX = to.getX()+15;
        double toY = to.getY()+15;
        gridWidth = to.getGridWidth();
        movingSubdivisions = to.getSubdivisions();
        movingOffset = to.getMovingOffset();
        if(to.isMovingSnap())
        {
            if(to instanceof Decision)
            {
                toX += 3;
                toX /= (gridWidth/movingSubdivisions);
                toX = Math.round(toX);
                toX *= (gridWidth/movingSubdivisions);
                toX -=3;
                //fromX -= 3;
                
                //drawY -= 3;
                toY += 3;
                toY /= (gridWidth/movingSubdivisions);
                toY = Math.round(toY);
                toY *= (gridWidth/movingSubdivisions);
                toY -= 3;
                //fromY -= 3;
            }
            else
            {
                toX -= movingOffset;
                toX /= (gridWidth/movingSubdivisions);
                toX = Math.round(toX);
                toX *= (gridWidth/movingSubdivisions);
                toX += movingOffset;
                
                toY -= movingOffset;
                toY /= (gridWidth/movingSubdivisions);
                toY = Math.round(toY);
                toY *= (gridWidth/movingSubdivisions);
                toY += movingOffset;
            }
        }
        poly.add( new java.awt.Point((int)toX, (int)toY));
        
        try
        {
            //All of the points are now in poly, find the midpoint of the polyline
            //Find the total length of the polyline
            java.awt.Point last = null;
            double length = 0.0;
            for(int i = 0; i < poly.size(); i++)
            {
                java.awt.Point p = (java.awt.Point)poly.get(i);
                
                if(last != null)
                {
                    length += Math.sqrt(Math.pow(p.x-last.x, 2) + Math.pow(p.y-last.y, 2));
                }
                last = p;
            }
            
            //Try to find out exactly where the midpoint is
            
            double midpoint = length /2.0;
            
            int i = 0;
            double sofar = 0;
            double lastAmount = 0;
            last = (java.awt.Point)poly.get(0);
            java.awt.Point p = (java.awt.Point)poly.get(0);
            
            while(sofar <= midpoint)
            {
                last = p;
                i++;
                p = (java.awt.Point)poly.get(i);
                lastAmount = Math.sqrt(Math.pow(p.x-last.x, 2) + Math.pow(p.y-last.y, 2));
                sofar += lastAmount;
                
            }
            
            double howmuch = (midpoint-(sofar-lastAmount))/lastAmount;
            
            ////System.out.println("howmuch = "+howmuch);
            
            double xPoint = last.x + ((p.x-last.x)*howmuch);
            double yPoint = last.y + ((p.y-last.y)*howmuch);
            
            
            
            //double xPoint = ((Point)poly.get(i-1)).x + ((((Point)poly.get(i)).x-((Point)poly.get(i-1)).x)*howmuch);
            //double yPoint = ((Point)poly.get(i-1)).y + ((((Point)poly.get(i)).y-((Point)poly.get(i-1)).y)*howmuch);
            
            middle =  new java.awt.Point((int)xPoint, (int)yPoint);
            
        }
        catch(Exception e)
        {
            Rectangle2D rect = getBounds2D();
            
            middle = new java.awt.Point((int)(rect.getX()+(rect.getWidth()/2)), (int)(rect.getY()+(rect.getHeight()/2)));
        }
        //Rectangle2D rect = poly.getBounds2D();
        
        //middle = new Point((int)(rect.getX()+(rect.getWidth()/2)), (int)(rect.getY()+(rect.getHeight()/2)));
    }
    
    
    
    
    
    
    /** Adds a nail to this Transition with the given co-ordinates.  According to these
     * co-ordinates this method must determine what position to place this nail so that
     * it lies between nails already placed.
     */
    public Nail addNail(double nx, double ny) //returns the nail so that the modulemodel can store it
    {
        //This must work out the position of the new nail according to the ones already there and then add it to the list
        ////System.out.println("Nail added to"+ nx+", "+ny);
        State one = from;
        State two = null;
        boolean found = false;
        int i = 0;
        for(i = 0; i < nails.size(); i++)
        {
            two = (Nail)nails.get(i);
            if(liesBetween(nx, ny, one, two))
            {
                found = true;
                break;
            }
            one = two;
        }
        if(!found)
        {
            two = to;
            if(liesBetween(nx,ny, one , to))
            {
                found = true;
            }
        }
        Nail returner =null;
        if(found)
        {
            returner = new Nail(nx, ny, one ,two);
            nails.add(i, returner);
        }
        else
        {
            //add it on to the end
            returner = new Nail(nx, ny, one, two);
            nails.add(returner);
        }
        returner.associateTransition(this);
        return returner;
        
        
    }
    
    //Rendering Methods
    
    /** Renders this Transition onto the given Graphics2D context. */
    public void render(Graphics2D g2)
    {
        render(g2, false);
    }
    /** Renders this Transition onto the given Graphics2D context, there is also the
     * additional option of stating whether long-lines should be used.
     */
    public void render(Graphics2D g2, boolean longLines)
    {
        intersects(new Rectangle2D.Double(0,0,1,1));
        if(docked)
        {
            
            double offsetFrom = from.getWidth()/2;
            double offset = to.getWidth()/2;
            
            
            //draw lines
            double fromX = from.getX()+offsetFrom;
            double fromY = from.getY()+offsetFrom;
            
            double gridWidth = from.getGridWidth();
            double movingSubdivisions = from.getSubdivisions();
            double movingOffset = from.getMovingOffset();
            
            //Used for curves
            double lastX = Double.NEGATIVE_INFINITY;
            double lastY = Double.NEGATIVE_INFINITY;
            double nextX = Double.POSITIVE_INFINITY;
            double nextY = Double.POSITIVE_INFINITY;
            
            //Storage of points before rendering
            PointList tempPoints = new PointList();
            
            
            
            if(from.isMovingSnap())
            {
                if(from instanceof Decision)
                {
                    fromX /= (gridWidth/movingSubdivisions);
                    fromX = Math.round(fromX);
                    fromX *= (gridWidth/movingSubdivisions);
                    //fromX -= 3;
                    
                    //drawY -= 3;
                    fromY /= (gridWidth/movingSubdivisions);
                    fromY = Math.round(fromY);
                    fromY *= (gridWidth/movingSubdivisions);
                    //fromY -= 3;
                }
                else
                {
                    fromX -= movingOffset;
                    fromX /= (gridWidth/movingSubdivisions);
                    fromX = Math.round(fromX);
                    fromX *= (gridWidth/movingSubdivisions);
                    fromX += movingOffset;
                    
                    fromY -= movingOffset;
                    fromY /= (gridWidth/movingSubdivisions);
                    fromY = Math.round(fromY);
                    fromY *= (gridWidth/movingSubdivisions);
                    fromY += movingOffset;
                }
            }
            
            tempPoints.add(new TempPoint(fromX, fromY));
            
            for(int i = 0; i < nails.size(); i++)
            {
                Nail toN = (Nail)(nails.get(i));
                double toX = toN.getX();
                double toY = toN.getY();
                movingSubdivisions = toN.getSubdivisions();
                movingOffset = toN.getMovingOffset();
                gridWidth = toN.getGridWidth();
                if(toN.isMovingSnap())
                {
                    //toX -= movingOffset;
                    toX /= (gridWidth/movingSubdivisions);
                    toX = Math.round(toX);
                    toX *= (gridWidth/movingSubdivisions);
                    //toX += movingOffset;
                    
                    //toY -= movingOffset;
                    toY /= (gridWidth/movingSubdivisions);
                    toY = Math.round(toY);
                    toY *= (gridWidth/movingSubdivisions);
                    //toY += movingOffset;
                }
                if(selected) g2.setColor(Color.green);
                else g2.setColor(Color.black);
                //g2.draw(new Line2D.Double(fromX, fromY, toX, toY));
                tempPoints.add(new TempPoint(toX, toY));
                
                fromX = toX;
                fromY = toY;
            }
            
            if(selected) g2.setColor(Color.green);
            else g2.setColor(Color.black);
            
            double toX = to.getX()+offset;
            double toY = to.getY()+offset;
            ////System.out.println("b4 toX = "+toX+" toY = "+toY);
            movingOffset = to.getMovingOffset();
            gridWidth = to.getGridWidth();
            movingSubdivisions = to.getSubdivisions();
            
            ////System.out.println("movingOffset = "+movingOffset+" gridWidth = "+gridWidth+" movingSubdivisions = "+movingSubdivisions);
            if(to.isMovingSnap())
            {
                if(to instanceof Decision)
                {
                    toX /= (gridWidth/movingSubdivisions);
                    toX = Math.round(toX);
                    toX *= (gridWidth/movingSubdivisions);
                    //toX -= 3;
                    
                    //drawY -= 3;
                    toY /= (gridWidth/movingSubdivisions);
                    toY = Math.round(toY);
                    toY *= (gridWidth/movingSubdivisions);
                    //toY -= 3;
                }
                else
                {
                    toX -= movingOffset;
                    toX /= (gridWidth/movingSubdivisions);
                    toX = Math.round(toX);
                    toX *= (gridWidth/movingSubdivisions);
                    toX += movingOffset;
                    
                    toY -= movingOffset;
                    toY /= (gridWidth/movingSubdivisions);
                    toY = Math.round(toY);
                    toY *= (gridWidth/movingSubdivisions);
                    toY += movingOffset;
                }
            }
            ////System.out.println("fromX = "+fromX+" fromY = "+fromY);
            ////System.out.println("toX = "+toX+" toY = "+toY);
            //g2.draw(new Line2D.Double(fromX, fromY, toX, toY));
            tempPoints.add(new TempPoint(toX,toY));
            
            //CURVE RENDERING
            
            TempPoint last = null, from = null, toP = null, next = null;
            
            double r = 8; //Curve radius
            double dy,dx,angle, e, f, g, j, midx, midy, x0,y0,x1,y1,xp,yp, s;
            
            GeneralPath path = new GeneralPath();
            
            for(int i = 0; i < tempPoints.size()+1; i++)
            {
                last = from;
                from = toP;
                toP = next;
                try
                {
                    next = tempPoints.getPoint(i);
                    while(toP != null && next.x == toP.x && next.y == toP.y)
                    {
                        i++;
                        next = tempPoints.getPoint(i);
                    }
                }
                catch(IndexOutOfBoundsException exc)
                {
                    next = null;
                }
                
                
                if(from == null) continue;
                else if(next == null && last == null)//no nails
                {
                    //Just one straight line
                    path.moveTo((float)from.x, (float)from.y);
                    path.lineTo((float)toP.x, (float)toP.y);
                }
                else if(next == null)//for the last point
                {
                    //Just need to finish with a straight line
                    path.lineTo((float)toP.x, (float)toP.y);
                }
                else if(last == null) //for the first point
                {
                    //Calculation of e and f(offset(from) of end of straight part)
                    dy = from.y - toP.y;
                    dx = from.x - toP.x;
                    angle = Math.atan(dy/dx);
                    e = r * Math.cos(angle);
                    f = r * Math.sin(angle);
                    if(dx >= 0 )
                    {
                        e*=-1;
                        f*=-1;
                    }
                    
                    //Calculation of g and j(offset(to) of end of curved part)
                    dy = toP.y - next.y;
                    dx = toP.x - next.x;
                    angle = Math.atan(dy/dx);
                    g = r * Math.cos(angle);
                    j = r * Math.sin(angle);
                    if(dx >= 0)
                    {
                        g*=-1;
                        j*=-1;
                    }
                    
                    //point 0 is the end of the line part
                    x0 = toP.x-e;
                    y0 = toP.y-f;
                    
                    //point 1 is the end of the curved part
                    x1 = toP.x+g;
                    y1 = toP.y+j;
                    
                    //Calculation of Bezier Parametric coordinate
                    //midx = (x0+x1)/2.0;
                    //midy = (y0+y1)/2.0;
                    xp = toP.x;//+(toP.x-midx);
                    yp = toP.y;//+(toP.y-midy);
                    
                    //Render
                    path.moveTo((float)from.x, (float)from.y);
                    path.lineTo((float)x0, (float)y0);
                    path.quadTo((float)xp, (float)yp, (float)x1, (float)y1);
                }
                else //for intermediary points
                {
                    //First find out whether the distance is large enough
                    dy = (from.y - toP.y);
                    dx = (from.x - toP.x);
                    s = Math.sqrt(Math.pow(dy,2) + Math.pow(dx,2));
                    if(s <= (2*r))
                    {
                        r = s/2.0;
                    }
                    
                    //Calculation of e and f(offset(from) of end of straight part)
                    dy = (from.y - toP.y);
                    dx = (from.x - toP.x);
                    angle = Math.atan(dy/dx);
                    e = r * Math.cos(angle);
                    f = r * Math.sin(angle);
                    if(dx >= 0 )
                    {
                        e*=-1;
                        f*=-1;
                    }
                    
                    //Calculation of g and j(offset(to) of end of curved part)
                    dy = (toP.y - next.y);
                    dx = (toP.x - next.x);
                    angle = Math.atan(dy/dx);
                    g = r * Math.cos(angle);
                    j = r * Math.sin(angle);
                    if(dx >= 0)
                    {
                        g*=-1;
                        j*=-1;
                    }
                    
                    //point 0 is the end of the line part
                    x0 = toP.x-e;
                    y0 = toP.y-f;
                    
                    //point 1 is the end of the curved part
                    x1 = toP.x+g;
                    y1 = toP.y+j;
                    
                    //Calculation of Bezier Parametric coordinate
                    //midx = (x0+x1)/2.0;
                    //midy = (y0+y1)/2.0;
                    xp = toP.x;//+(toP.x-midx);
                    yp = toP.y;//+(toP.y-midy);
                    
                    //Render
                    path.lineTo((float)x0, (float)y0);
                    //System.out.println("THIS LINE SOMETIMES CAUSES AN ERROR CALLING:");
                    //System.out.println("path.quadTo("+xp+", "+yp+", "+x1+", "+y1+")");
                    path.quadTo((float)xp, (float)yp, (float)x1, (float)y1);
                    
                    r = 8;
                }
            }
            
            
            g2.draw(path);
            
            
            
            
            //draw dots if selected
            
            for(int i = 0; i < nails.size(); i++)
            {
                Nail toN = (Nail)(nails.get(i));
                double toDX = toN.getX();
                double toDY = toN.getY();
                movingOffset = toN.getMovingOffset();
                gridWidth = toN.getGridWidth();
                movingSubdivisions = toN.getSubdivisions();
                if(toN.isMovingSnap())
                {
                    //toDX -= 3;
                    toDX /= (gridWidth/movingSubdivisions);
                    toDX = Math.round(toDX);
                    toDX *= (gridWidth/movingSubdivisions);
                    //toDX += 3;
                    
                    //toDY -= 3;
                    toDY /= (gridWidth/movingSubdivisions);
                    toDY = Math.round(toDY);
                    toDY *= (gridWidth/movingSubdivisions);
                    //toDY += 3;
                }
                if(toN.isSelected() || toN.isHighlighted())
                {
                    g2.setColor(Color.white);
                    g2.fill(new Ellipse2D.Double((toDX-2), (toDY-2), 4, 4));
                    if(toN.isHighlighted())g2.setColor(Color.black);
                    else g2.setColor(Color.green);
                    g2.draw(new Ellipse2D.Double((toDX-2), (toDY-2), 4, 4));
                    g2.setColor(Color.black);
                }
            }
            
            
            // draw arrow appropriately
            
            if(isSelected())
                g2.setColor(Color.green);
            else g2.setColor(Color.black);
            toX = to.getX();
            toY = to.getY();
            
            if(to.isMovingSnap())
            {
                
                toX /= (gridWidth/movingSubdivisions);
                toX = Math.round(toX);
                toX *= (gridWidth/movingSubdivisions);
                
                
                
                toY /= (gridWidth/movingSubdivisions);
                toY = Math.round(toY);
                toY *= (gridWidth/movingSubdivisions);
                
            }
            
            if(!(to instanceof Decision))
            {
                angle = 20;
                double length = 10;
                
                int xNegifier = 1;
                int yNegifier = 1;
                double xSwapper = Math.PI;
                double ySwapper = Math.PI;
                
                
                
                
                if(fromX<toX+offset)
                {xNegifier=-1; yNegifier = -1; xSwapper = 0; ySwapper = 0;}
                
                
                
                double theta = Math.atan((fromY-(toY+offset))/(fromX-(toX+offset)));
                x1 = ((Math.cos(theta)*offset)*xNegifier)+toX+offset;
                y1 = ((Math.sin(theta)*offset)*yNegifier)+toY+offset;
                double x2 = x1 - ((length*Math.sin(Math.toRadians(90-angle)-(theta+xSwapper))));//*xNegifier);
                double y2 = y1 - ((length*Math.cos(Math.toRadians(90-angle)-(theta+ySwapper))));//*yNegifier);
                double x3 = x1 + ((length*Math.sin(((theta+Math.toRadians(90-angle)+Math.abs(xSwapper-Math.PI))))));//*xNegifier);
                double y3 = y1 - ((length*Math.cos(((theta+Math.toRadians(90-angle)+Math.abs(ySwapper-Math.PI))))));//*yNegifier);
                int[] xpoints =
                {(int)x1, (int)x2, (int)x3};
                int[] ypoints =
                {(int)y1, (int)y2, (int)y3};
                g2.fill(new Polygon(xpoints, ypoints, 3));
                
            }
            
            //draw labels
            workOutMiddle();
            
            //g2.fill(new Ellipse2D.Double(middle.getX(), middle.getY(), 10, 10));
            probability.render(g2, middle.getX(), middle.getY(),longLines);
            sync.render(g2, middle.getX(), middle.getY(), longLines);
            guard.render(g2, middle.getX(), middle.getY(), longLines);
            assignment.render(g2, middle.getX(), middle.getY(), longLines);
            
            /*if(intersects != null)
            {
                for(int i = 0; i < intersects.size(); i++)
                {
                    Line2D line = (Line2D)intersects.get(i);
                    g2.setColor(Color.pink);
                    g2.draw(line);
                }
            }*/
        }
    }
    
    //Static Methods
    
    private static boolean liesBetween(double nx, double ny, State one, State two)
    {
        double sx1 = one.getX();
        double sy1 = one.getY();
        double sx2 = two.getX();
        double sy2 = two.getY();
        sx1+=(one.width/2);
        sy1+=(one.height/2);
        sx2+=(two.width/2);
        sy2+=(two.height/2);
        Line2D.Double aLine = new Line2D.Double(sx1,sy1,sx2,sy2);
        ////System.out.println("The result is "+aLine.intersects(nx-1, ny-1, 2, 2));
        return aLine.intersects(nx-4, ny-4, 8, 8);
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
        if(to instanceof Decision) return "Branched Transition";
        else
            return "Transition";
    }
    
    public String getDescriptor()
    {
        if(to instanceof Decision)
        {
            return "from "+from.getDescriptor()+" to many";
        }
        else
        {
            return "from "+from.getDescriptor()+" to "+to.getDescriptor();
        }
    }
    
    public int getNumProperties()
    {
        if(to instanceof Decision) return 2;
        else return 4;
    }
    
    public SingleProperty getProperty(int index)
    {
        switch(index)
        {
            case 0: return sync;
            case 1: return guard;
            case 2: return probability;
            default: return assignment;
        }
    }
    
    public int getUniquePropertyID()
    {
        if(to instanceof Decision) return prism.PropertyConstants.BRANCHTRANSITION;
        else
            return prism.PropertyConstants.TRANSITION;
    }
    
    public void registerObserver(Observer obs)
    {
        guard.addObserver(obs);
        sync.addObserver(obs);
        probability.addObserver(obs);
        assignment.addObserver(obs);
    }
    
    class TempPoint
    {
        double x, y;
        
        public TempPoint(double x, double y)
        {
            this.x = x;
            this.y = y;
        }
    }
    
    class PointList extends ArrayList
    {
        public PointList()
        {
            super();
        }
        
        public TempPoint getPoint(int i)
        {
            return (TempPoint)get(i);
        }
        
        public double getX(int i)
        {
            return getPoint(i).x;
        }
        
        public double getY(int i)
        {
            return getPoint(i).y;
        }
    }
    
}
