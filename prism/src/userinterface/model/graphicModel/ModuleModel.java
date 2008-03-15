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

import java.util.*;



import java.awt.geom.*;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import userinterface.model.*;
import userinterface.util.*;

/** This class is the model for a ModuleDrawingPane.  It contains all data /
 * processing methods required for the module diagrams to be drawn.  The processing
 * methods are called externally, when actions from the mouse / buttons are
 * detected.  This class changes the attributes of its data appropriately.  It
 * contains all of the states, transitions and branches as well as graphical
 * elements such as nails.  It also handles selection of items, the calling of
 * dialog boxes to edit them and the deletion of objects.
 */
public class ModuleModel extends SelectionModel implements Observer
{
    
    //Attributes
    
    //Parent Component, usually a GraphicModelEditor
    private GUIMultiModelHandler handler;
    
    //identification
    private GUIMultiModelTree.ModuleNode corresponding;
    private GraphicModuleContainer container;
    
    //Modified since last save
    private boolean modified;
    
    //Module Data
    private String moduleName;
    private ArrayList variables, theStates, transitions, theNails;
    private int numStates, numTransitions, numNails;
    
    //Drawing environment Data
    private double zoom, gridWidth, subdivisions;
    private boolean snap, showGrid, showLongLabels;
    
    private int currentWidth, currentHeight, viewingX, viewingY, scrOffsetX, scrOffsetY;
    
    
    //Popup menus
    private JPopupMenu transPopup;
    private JMenuItem addNailPop, startTransition;
    private JMenuItem addState;
    private JMenuItem addChoice;
    private JMenuItem cut, copy, paste, delete, selectAll;
    private JMenuItem zoomIn, zoomOut, restoreZoom;
    
    private State starterState; //for starting transitions from the menu
    
    private JPopupMenu modulePopup;
    private double popupX = Double.POSITIVE_INFINITY, popupY = Double.POSITIVE_INFINITY;
    
    //Current Drawing Mode
    private int mode;
    
    //Keys
    
    private boolean controlDown;
    
    /** Constant to describe a drawing mode. */
    public static final int EDIT = 0;
    
    public static final int ZOOM = 1;
    
    //Drag a selection attributes
    private boolean isSelecting;
    private double selStartX, selStartY, selEndX, selEndY;
    
    //Draw a zoom selection attributes
    private boolean isZoomSelecting;
    private double zoomStartX, zoomStartY, zoomEndX, zoomEndY;
    
    private boolean zoomAreaChanged =false;
    
    //Move selected objects attributes
    private boolean isMoving, movingAbsolutes = true;
    private double lastX, lastY, currX, currY;
    
    //Transition drawing attributes
    private boolean drawingTrans;
    private double tranDrawX1, tranDrawY1, tranDrawX2, tranDrawY2;
    private State tempFromState;
    private Nail lastNail;
    private ArrayList tempNails;
    
    //Branched Transition drawing attributes
    private boolean drawingProbTrans, tempProbNailDown, drawingExtraBranches;
    private double tempAddNailX, tempAddNailY;
    private State tempProbFrom;
    private Transition tempAddNailTrans;
    private Nail tempProbNail;
    private ArrayList tempProbNails;
    private Nail lastProbNail;
    private ArrayList tempProbTo;
    
    //Other
    private boolean isOverMovable = false;
    private boolean overTransition = false;
    
    
    
    //Debugging
    private Rectangle2D lastMouseBox = null;
    
    //Selection Listening
    
    //Constructor
    
    /** Constructs a new ModuleModel with the given parent JFrame.  This JFrame is
     * required to call dialog boxes who require the parent component to act modally.
     */
    public ModuleModel(GUIMultiModelHandler handler, GUIMultiModelTree.ModuleNode corresponding)
    {
        super();
        this.handler = handler;
        this.corresponding = corresponding;
        
        corresponding.setModel(this);
        modified = false;
        controlDown = false;
        //Set up Module attributes
        moduleName = corresponding.getName();
        variables = new ArrayList();
        theStates = new ArrayList();
        numStates = 0;
        transitions = new ArrayList();
        numTransitions = 0;
        theNails = new ArrayList();
        numNails = 0;
        
        //Set up drawing environment attributes
        zoom = 1.0;
        gridWidth = 30;
        subdivisions = 4;
        snap = true;
        showGrid = true;
        showLongLabels = false;
        
        //Set up drag selection attributes
        isSelecting = false;
        selStartX = 0;
        selStartY = 0;
        selEndX = 0;
        selEndY = 0;
        
        //Set up drag zoom selection attributes
        isZoomSelecting = false;
        zoomStartX = 0;
        zoomStartY = 0;
        zoomEndX = 0;
        zoomEndY = 0;
        
        //Set up move selected objects attributes
        isMoving = false;
        lastX = 0;
        lastY = 0;
        currX = 0;
        currY = 0;
        
        //Setup transition drawing attributes
        drawingTrans = false;
        tranDrawX1 = 0;
        tranDrawX2 = 0;
        tranDrawY1 = 0;
        tranDrawY2 = 0;
        tempFromState = null;
        lastNail = null;
        tempNails = new ArrayList();
        
        //Setup Branched Transition drawing attributes
        drawingProbTrans = false;
        tempProbNailDown = false;
        tempAddNailX = 0.0;
        tempAddNailY = 0.0;
        tempProbFrom = null;
        tempAddNailTrans = null;
        tempProbNail = null;
        tempProbTo = new ArrayList();
        tempProbNails = new ArrayList();
        lastProbNail = null;
        
        //Setup Popups
        transPopup = new JPopupMenu();
        
        modulePopup = new JPopupMenu();
        addNailPop = new JMenuItem("Add Point");
        addNailPop.setIcon(userinterface.GUIPrism.getIconFromImage("smallAddNail.png"));
        addNailPop.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                if(tempAddNailTrans != null)
                {
                    //System.out.println("Should be adding the nail");
                    Nail newNail;
                    if(isSnap())
                    {
                        newNail = tempAddNailTrans.addNail(snapIt(tempAddNailX), snapIt(tempAddNailY));
                    }
                    else
                    {
                        newNail = tempAddNailTrans.addNail(tempAddNailX, tempAddNailY);
                    }
                    if (newNail != null)
                    {theNails.add(newNail); numNails++;}
                    setChanged();
                    notifyObservers();
                }
            }
        });
        
        
        cut = new JMenuItem("Cut");
        cut.setIcon(userinterface.GUIPrism.getIconFromImage("smallCut.png"));
        cut.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                cut();
            }
        });
        
        copy = new JMenuItem("Copy");
        copy.setIcon(userinterface.GUIPrism.getIconFromImage("smallCopy.png"));
        copy.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                copy();
            }
        });
        
        paste = new JMenuItem("Paste");
        paste.setIcon(userinterface.GUIPrism.getIconFromImage("smallPaste.png"));
        paste.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                paste();
            }
        });
        
        delete = new JMenuItem("Delete");
        delete.setIcon(userinterface.GUIPrism.getIconFromImage("smallDelete.png"));
        delete.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                deleteSelected();
            }
        });
        
        zoomIn = new JMenuItem("Zoom In");
        zoomIn.setIcon(userinterface.GUIPrism.getIconFromImage("smallZoomIn.png"));
        zoomIn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                zoom*=1.1;
                centreX = (int)(popupX * zoom);
                centreY = (int)(popupY * zoom);
                zoomChanged = true;
                setChanged();
                notifyObservers(null);
            }
        });
        
        zoomOut = new JMenuItem("Zoom Out");
        zoomOut.setIcon(userinterface.GUIPrism.getIconFromImage("smallZoomOut.png"));
        zoomOut.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                zoom/=1.1;
                centreX = (int)(popupX * zoom);
                centreY = (int)(popupY * zoom);
                zoomChanged = true;
                setChanged();
                notifyObservers(null);
            }
        });
        
        restoreZoom = new JMenuItem("Restore");
        restoreZoom.setIcon(userinterface.GUIPrism.getIconFromImage("smallZoom.png"));
        restoreZoom.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                zoom=1;
                centreX = 0;
                centreY = 0;
                zoomChanged = true;
                setChanged();
                notifyObservers(null);
            }
        });
        
        startTransition = new JMenuItem("Start Transition");
        startTransition.setIcon(userinterface.GUIPrism.getIconFromImage("smallAddTransition.png"));
        startTransition.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                if(starterState != null)
                {
                    if(!drawingProbTrans && ! drawingExtraBranches)
                    {
                        isSelecting = false;
                        isMoving = false;
                        
                        tempProbFrom = starterState;
                        tempProbTo = new ArrayList();
                        
                        
                        drawingProbTrans = true;
                        lastNail = null;
                        tempNails = new ArrayList();
                        
                        deSelectAll();
                        setChanged();
                        notifyObservers(null);
                        
                        return;
                    }
                }
            }
        });
        
        addState = new JMenuItem("Add State");
        addState.setIcon(userinterface.GUIPrism.getIconFromImage("smallAddState.png"));
        addState.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                if(!drawingProbTrans)
                {
                    deSelectAll();
                    if(snap) addState(snapIt(popupX), snapIt(popupY));
                    else addState(popupX, popupY);
                    setChanged();
                    notifyObservers(null);
                }
            }
        });
        
        addChoice = new JMenuItem("Add Probabilistic Choice");
        addChoice.setIcon(userinterface.GUIPrism.getIconFromImage("smallAddChoice.png"));
        addChoice.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                if(!drawingProbTrans && ! drawingExtraBranches)
                    {
                        isSelecting = false;
                        isMoving = false;
                        
                        tempProbFrom = starterState;
                        tempProbTo = new ArrayList();
                        
                        drawingExtraBranches = true;
                        lastNail = new Nail(starterState.getX(), starterState.getY(), null, null);
                        
                        deSelectAll();
                        setChanged();
                        notifyObservers();
                    }
            }
        });
        
        
        
        
        
        
        transPopup.add(addNailPop);
        
        modulePopup.addSeparator();
        modulePopup.add(cut);
        modulePopup.add(copy);
        modulePopup.add(paste);
        modulePopup.add(delete);
        modulePopup.addSeparator();
        JMenu zoomer = new JMenu("Zoom");
        modulePopup.add(zoomer);
        zoomer.add(zoomIn);
        zoomer.add(zoomOut);
        zoomer.add(restoreZoom);
        
        
        //Set current drawing mode
        mode = EDIT;
    }
    
    
    
    
    public void addToAllYs(double val)
    {
        for(int i = 0; i < getNumStates(); i++)
        {
            State st = getState(i);
            st.move(0, val);
        }
    }
    
    public void addToAllXs(double val)
    {
        for(int i = 0; i < getNumStates(); i++)
        {
            State st = getState(i);
            st.move(val, 0);
        }
    }
    
    //Access Methods
    
    public GUIMultiModelTree.ModuleNode getCorrespondingModuleNode()
    {
        return corresponding;
    }
    
    /** Access method to state whether the diagram should display long labels or not. */
    public boolean isShowLongLabels()
    {
        return showLongLabels;
    }
    
    /** Access method to return a string representation of this ModuleModel.  It in fact
     * returns "module " + moduleName.
     */
    public String toString()
    {
        return "module " + moduleName;
    }
    
    /** Access method to state whether whilst drawing a branched transition, the nail
     * has been placed.  If this returns false, the location of this nail should be
     * worked out automatically.
     */
    public boolean isTempProbNailDown()
    {
        return tempProbNailDown;
    }
    
    /** Access method to state whether this module has been modified. */
    public boolean isModified()
    {
        return modified;
    }
    
    public boolean isZoomAreaChanged()
    {
        return zoomAreaChanged;
    }
    
    public boolean isZoomSelecting()
    {
        return isZoomSelecting;
    }
    
    /** Access method that if drawing a branched transition, returns the nail object
     * which has been placed.  If no nail has been placed, this method returns null.
     */
    public Nail getTempProbNail()
    {
        return tempProbNail;
    }
    
    public Nail getLastProbNail()
    {
        return lastProbNail;
    }
    
    /** Access method to state whether this diagram should display a grid. */
    public boolean isShowGrid()
    {
        return showGrid;
    }
    
    /** Access method to return the list of variables associated with this module.  An
     * array of Variable objects is stored in this ArrayList.
     */
    public ArrayList getVariables()
    {
        return variables;
    }
    
    /** Access method to return the name of this module. */
    public String getModuleName()
    {
        return moduleName;
    }
    
    /** Access method to state whether at the time a branched transition is being drawn. */
    public boolean isDrawingProbTrans()
    {
        return drawingProbTrans;
    }
    
    /** Access method which whilst drawing a branched transition, returns the State which the
     * transition comes from.
     */
    public State getTempProbFrom()
    {
        return tempProbFrom;
    }
    /** Access method which, when drawing a branched transition, returns the ArrayList
     * containing State objects which describe the branches already placed.
     */
    public ArrayList getTempProbTo()
    {
        return tempProbTo;
    }
    
    /** Access method which whilst drawing a transition, returns the State which the
     * transition comes from.
     */
    public State getTempFromState()
    {
        return this.tempFromState;
    }
    
    /** Access method which whilst drawing a transition returns the nail at the given
     * index in the temporary-drawing Nail array.
     */
    public Nail getTempNail(int i)
    {
        return ((Nail)tempNails.get(i));
    }
    
    public Nail getTempProbNail(int i)
    {
        return ((Nail)tempProbNails.get(i));
    }
    
    public int getNumTempProbNails()
    {
        return tempProbNails.size();
    }
    
    /** Access method which whilst drawing transitions returns the number of nails in
     * the temporary nail array.
     */
    public int getNumTempNails()
    {
        return tempNails.size();
    }
    
    /** Access method which returns the Transition at the given index. */
    public Transition getTransition(int i)
    {
        return ((Transition)(transitions.get(i)));
    }
    
    /** Access method which returns the number of Transitions in this module. */
    public int getNumTransitions()
    {
        return numTransitions;
    }
    
    /** Access method to return the zoom factor for this module diagram. */
    public double getZoom()
    {
        return zoom;
    }
    
    /** Access method to state whether objects should be snapped to the grid. */
    public boolean isSnap()
    {
        return snap;
    }
    
    /** Access method to return the width of the grid. */
    public double getGridWidth()
    {
        return gridWidth;
    }
    
    /** Access method to return the State object at the given index. */
    public State getState(int i)
    {
        return (State)(theStates.get(i));
    }
    
    /** Access method to return the number of States in this ModuleModel. */
    public int getNumStates()
    {
        return numStates;
    }
    
    /** Access method to return the index of the given State object. */
    public int getStateIndex(State st)
    {
        return theStates.indexOf(st);
    }
    
    /** Access method to state whether there is currently a selection box being drawn on
     * the diagram to select items.
     */
    public boolean isItSelecting()
    {
        return isSelecting;
    }
    
    public boolean isOverMovable()
    {
        return isOverMovable;
    }
    
    /** Access method to return the start x-coordinate of a selection box, if in
     * selection mode.
     */
    public double getSelStartX()
    {
        return selStartX;
    }
    
    /** Access method to return the current drawing mode.  The returned integer is an
     * integer described by the drawing mode constants.
     */
    public int getMode()
    {
        return mode;
    }
    
    /** Access method to return the start y-coordinate of a selection box, if in
     * selection mode.
     */
    public double getSelStartY()
    {
        return selStartY;
    }
    
    /** Access method to return the end x-coordinate of a selection box, if in
     * selection mode.
     */
    public double getSelEndX()
    {
        return selEndX;
    }
    
    /** Access method to return the end y-coordinate of a selection box, if in
     * selection mode.
     */
    public double getSelEndY()
    {
        return selEndY;
    }
    
    public double getZoomStartX()
    {
        return zoomStartX;
    }
    
    public double getZoomStartY()
    {
        return zoomStartY;
    }
    
    public double getZoomEndX()
    {
        return zoomEndX;
    }
    
    public double getZoomEndY()
    {
        return zoomEndY;
    }
    
    /** Access method to state whether at the current time, items are being dragged
     * around the screen.
     */
    public boolean isItMoving()
    {
        return isMoving;
    }
    
    /** Access method to state whether a transition is being drawn. */
    public boolean isDrawingTrans()
    {
        return drawingTrans;
    }
    
    /** Access method which whilst a transition is being drawn describes the start
     * x-coordinate of the last line of the transition.
     */
    public double getTranDrawX1()
    {
        return tranDrawX1;
    }
    
    /** Access method which whilst a transition is being drawn describes the end
     * x-coordinate of the last line of the transition.
     */
    public double getTranDrawX2()
    {
        return tranDrawX2;
    }
    
    /** Access method which whilst a transition is being drawn describes the start
     * y-coordinate of the last line of the transition.
     */
    public double getTranDrawY1()
    {
        return tranDrawY1;
    }
    
    /** Access method which whilst a transition is being drawn describes the end
     * y-coordinate of the last line of the transition.
     */
    public double getTranDrawY2()
    {
        return tranDrawY2;
    }
    
    
    //Update Methods
    
    /** Sets whether the module diagram should show labels in long mode. */
    public void setShowLongLabels(boolean b)
    {
        showLongLabels = b;
        StringLabel.lineLabels = b;
        ExpressionLabel.lineLabels =b;
        setChanged();
        notifyObservers(null);
    }
    
    /** Sets whether the grid should be shown. */
    public void setShowGrid(boolean b)
    {
        showGrid = b;
        setChanged();
        notifyObservers(null);
    }
    
    /** Adds the given Variable object to the module. */
        /*public void addVariable(Variable v)
         {
         variables.add(v);
         modified = true;
         }
         
         /** Removes the given Variable object from the module.
         public void removeVariable(Variable v)
         {
         variables.remove(v);
         modified = true;
         }*/
    
    /** Sets the module name to the given String. */
    public void setModuleName(String str)
    {
        moduleName = str;
        modified = true;
    }
    
    /** Adds a transition to the module coming from the given State "from" and going to
     * the given State "to".  The boolean value nails states whether the nails present
     * in the temporary nail array should be added to the transition.  An integer
     * describing the index of the transition is returned.
     * @param from The State object which this transition comes from.
     * @param to The State object which this transition goes to.
     * @param nails A flag to state whether the nails stored in the temporary nail array should be
     * used as nails for this transition.
     * @return the index of the added transition.
     */
    public int addTransition(State from, State to, boolean nails)
    {
        modified = true;
        if(nails)
        {
            ArrayList keepNails = (ArrayList)tempProbNails.clone();//(ArrayList)tempNails.clone();
            Transition t = new Transition(from, to, keepNails, handler.getGUIPlugin().getPrism());
            t.registerObserver(this);
            transitions.add(t);
            theNails.addAll(keepNails);
            for(int i = 0 ; i < keepNails.size(); i++)
            {
                Nail na = (Nail)keepNails.get(i);
                na.registerObserver(this);
            }
            numNails+=keepNails.size();
        }
        else
        {
            Transition t = new Transition(from, to,handler.getGUIPlugin().getPrism());
            t.registerObserver(this);
            transitions.add(t);
        }
        numTransitions++;
        handler.hasModified(true);
        //deSelectAll();
        return numTransitions-1;
    }
    
    /** Adds a transition to the module coming from the given State "from" and going to
     * the given State "to".  This addTransition method takes in an array of nails
     * which are added to the transition. An integer
     * describing the index of the transition is returned.
     * @param from The State object which this transition comes from.
     * @param to The State object which this transition goes to.
     * @param nails An ArrayList of nails to be added to the transition.
     * @return an integer describing the index of the transition added.
     */
    public int addTransition(State from, State to, ArrayList nails)
    {
        modified = true;
        tempNails = nails;
	tempProbNails = nails;
        return addTransition(from, to, true);
    }
    
    /** Adds a branch from the given State "from" to the given State "to".  The Nail
     * parameter describes if this branch should contain the one nail it is allowed.
     * If null is passed, no nail will be drawn for this branch.  An integer describing
     * the index of this branch is returned.
     */
    public int addProbTransition(State from, State to, Nail aNail, String probExpression)
    {
        modified = true;
        if(aNail != null)
        {
            ArrayList keepNails = new ArrayList();
            keepNails.add(aNail);
            ProbTransition prtr = new ProbTransition(from, to, keepNails,handler.getGUIPlugin().getPrism());
            prtr.registerObserver(this);
            transitions.add(prtr);
            if(probExpression != null)
            {
                prtr.setProbability(probExpression);
            }
            theNails.addAll(keepNails);
            for(int i = 0 ; i < keepNails.size(); i++)
            {
                Nail na = (Nail)keepNails.get(i);
                na.registerObserver(this);
            }
            numNails+=keepNails.size();
        }
        else
        {
            ProbTransition prtr = new ProbTransition(from, to,handler.getGUIPlugin().getPrism());
            prtr.registerObserver(this);
            transitions.add(prtr);
            if(probExpression != null)
            {
                prtr.setProbability(probExpression);
            }
        }
        numTransitions++;
        ////System.out.println("Prob Trans added to arraylist");
        handler.hasModified(true);
        return numTransitions-1;
    }
    
    public int addProbTransition(State from, State to, Nail aNail)
    {
        return addProbTransition(from, to, aNail, null);
    }
    
    /** Deletes the given State object. */
    public void deleteState(State st)
    {
        for(int i = 0; i < numTransitions; i++)
        {
            Transition tr = (Transition)transitions.get(i);
            if(tr.getFrom() == st || tr.getTo() == st) deleteTransition(tr);
        }
        theStates.remove(st);
        numStates--;
        orderStates();
        handler.hasModified(true);
    }
    
    /** Deletes the given Transition object. */
    public void deleteTransition(Transition tr)
    {
        for(int i = 0; i<numNails; i++)
        {
            for(int j = 0; j < tr.getNails().size(); j++)
            {
                if(theNails.get(i) == tr.getNails().get(j))
                {
                    theNails.remove(i);
                    numNails--;
                    break;
                }
            }
        }
        tr.getFrom().disassociateTransition(tr);
        tr.getTo().disassociateTransition(tr);
        transitions.remove(tr);
        numTransitions--;
        handler.hasModified(true);
    }
    
    public void deleteNail(Nail n)
    {
        n.delete();//removes it from its transition
        theNails.remove(n);
        numNails--;
    }
    
    /** Sets the current drawing mode to the mode index (see drawing mode constants). */
    public void setMode(int i)
    {
        ////System.out.println("setting the mode to "+i);
        modified = true;
        mode = i;
    }
    
    /** Sets the x-coordinate of the transition drawing line end to the given parameter. */
    
    
    private double lastTranDrawX, lastTranDrawY, lastSelX, lastSelY, lastZoomX, lastZoomY;
    
    public void setTranDrawX2(double d)
    {
        lastTranDrawX = tranDrawX2;
        tranDrawX2 = d/zoom;
    }
    
    /** Sets the y-coordinate of the transition drawing line end to the given parameter. */
    public void setTranDrawY2(double d)
    {
        lastTranDrawY = tranDrawY2;
        tranDrawY2 = d/zoom;
    }
    
    /** Sets the x-coordinate of the selection box end to the given parameter. */
    public void setSelEndX(double d)
    {
        lastSelX = selEndX;
        selEndX = d/zoom;
    }
    
    /** Sets the y-coordinate of the selection box end to the given parameter. */
    public void setSelEndY(double d)
    {
        lastSelY = selEndY;
        selEndY = d/zoom;
    }
    
    /** Sets the start x-coordinate of the selection box to the given parameter. */
    public void setSelStartX(double d)
    {
        selStartX = d/zoom;
    }
    
    /** Sets the start y-coordinate of the selection box to the given parameter. */
    public void setSelStartY(double d)
    {
        selStartY = d/zoom;
    }
    
    /** Sets the x co-ordinates to which the mouse is at during a moving operation. */
    public void setMoveX(double d)
    {
        currX = d/zoom;
    }
    
    /** Sets the y co-ordinates to which the mouse is at during a moving operation. */
    public void setMoveY(double d)
    {
        currY = d/zoom;
    }
    
    /** Sets this module diagram's zoom parameter. */
    public void setZoom(double z)
    {
        zoom = z;
        setChanged();
        notifyObservers(null);
    }
    
    /** Adds a Decision object, which is a node of a branched transition to the given x
     * and y co-ordinates.
     */
    public int addDecision( double x, double y)
    {
        modified = true;
        Decision dec = new Decision(x, y);
        dec.registerObserver(this);
        theStates.add(dec);
        numStates++;
        orderStates();
        handler.hasModified(true);
        return numStates-1;
    }
    
    /** Adds a State to the given x and y co-ordinates. */
    public void addState(double x, double y)
    {
        State s = new State(x, y);
        s.registerObserver(this);
        modified = true;
        theStates.add(s); numStates++; orderStates();
        handler.hasModified(true);
        //deSelectAll();
    }
    
    public void addState(State s)
    {
        s.registerObserver(this);
        ////System.out.println("adding state "+s.toString());
        modified = true;
        theStates.add(s); numStates++; orderStates();
        handler.hasModified(true);
        //deSelectAll();
        
    }
    
    /** Sets whether objects should be snapped to the grid. */
    public void setSnap(boolean b)
    {
        snap = b;
        setChanged();
        notifyObservers(null);
    }
    
    /** Sets the width of the grid for this module diagram. */
    public void setGridWidth(double d)
    {
        gridWidth = d;
    }
    
    public void notifyDimensions(int x, int y)
    {
        currentWidth = x;
        currentHeight = y;
    }
    
    public void notifyViewingArea(int x, int y)
    {
        viewingX = x;
        viewingY = y;
    }
    
    public boolean zoomChanged()
    {
        return zoomChanged;
    }
    
    public boolean zoomAreaChanged()
    {
        return zoomAreaChanged;
    }
    
    private boolean zoomChanged = false;
    private int centreX, centreY;
    private int cornerX, cornerY;
    
    public int getCentreX()
    {
        return centreX;
    }
    
    public int getCentreY()
    {
        return centreY;
    }
    
    public int getCornerX()
    {
        return cornerX;
    }
    
    public int getCornerY()
    {
        return cornerY;
    }
    
    public void doneZoom()
    {
        zoomChanged = false;
    }
    
    public void doneAreaZoom()
    {
        zoomAreaChanged = false;
    }
    
    private boolean rightClick;
    
    public void setRightClick()
    {
        rightClick = true;
    }
    
    public void setScrOffsetX(int scrOffsetX)
    {
        this.scrOffsetX = scrOffsetX;
    }
    
    public void setScrOffsetY(int scrOffsetY)
    {
        this.scrOffsetY = scrOffsetY;
    }
    
    public void setZoomEndX(double d)
    {
        lastZoomX = zoomEndX;
        zoomEndX = d/zoom;
    }
    
    public void setZoomEndY(double d)
    {
        lastZoomY = zoomEndY;
        zoomEndY = d/zoom;
    }
    
    //Processing events
    
    
    public void cut()
    {
        copy();
        deleteSelected();
    }
    
    public void copy()
    {
        //return new GraphicSelection(this);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        GraphicSelection gs = new GraphicSelection(this);
        clipboard.setContents(gs, null);
    }
    
    public void paste()
    {
        //System.out.println("pasting");
        deSelectAll();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if(contents == null) return;
        try
        {
            DataFlavor flavor = new DataFlavor("application/x-java-serialized-object;class=userinterface.model.graphicModel.GraphicSelection$SelectionPair");
            if(contents.isDataFlavorSupported(flavor))
            {
                GraphicSelection.SelectionPair selection = (GraphicSelection.SelectionPair)contents.getTransferData(flavor);
                
                int[] originalIndices = new int[selection.states.length];
                int[] newIndices = new int[selection.states.length];
                
                if(popupX == Double.POSITIVE_INFINITY || popupY == Double.POSITIVE_INFINITY)
                {
                    selection.offsetX += 30;
                    selection.offsetY += 30;
                }
                else
                {
                    selection.offsetX = (popupX - selection.topX);
                    selection.offsetY = (popupY - selection.topY);
                    
                    popupX = Double.POSITIVE_INFINITY;
                    popupY = Double.POSITIVE_INFINITY;
                }
                for(int i = 0; i < selection.states.length; i++)
                {
                    GraphicSelection.StateSelect stsel = selection.states[i];
                    originalIndices[i] = stsel.id;
                    //if(!stsel.decision)
                    {
                        
                        double mouseX = selection.topX;
                        double mouseY = selection.topY;
                        
                        if(!stsel.decision)
                        {
                            State newState = new State(mouseX+(stsel.x-mouseX)+selection.offsetX, mouseY+(stsel.y-mouseY)+selection.offsetY);
                            addState(newState);
                            newIndices[i] = theStates.indexOf(newState);
                            newState.setComment(stsel.comment);
                            newState.getCommentLabel().setOffsetX(stsel.commentX);
                            newState.getCommentLabel().setOffsetY(stsel.commentY);
                            newState.setInitial(stsel.initial);
                            newState.setSelected(true);
                            addToSelection(newState, false);
                            //System.out.println("should be successful");
                        }
                        else
                        {
                            newIndices[i] = addDecision(mouseX+(stsel.x-mouseX)+selection.offsetX, mouseY+(stsel.y-mouseY)+selection.offsetY);
                            getState(newIndices[i]).setSelected(true);
                            addToSelection(getState(newIndices[i]), false);
                        }
                        clipboard.setContents(new GraphicSelection(selection), null);
                    }
                }
                
                for(int i = 0; i < selection.transitions.length; i++)
                {
                    GraphicSelection.TransitionSelect trsel = selection.transitions[i];
                    //System.out.println("transition "+i);
                    //if(!trsel.prob)
                    {
                        double mouseX = selection.topX;
                        double mouseY = selection.topY;
                        //Find the indices of the from and to states
                        int indexFrom = -1;
                        int indexTo = -1;
                        for(int j = 0; j < originalIndices.length; j++)
                        {
                            if(originalIndices[j] == trsel.from.id) indexFrom = newIndices[j];
                            if(originalIndices[j] == trsel.to.id) indexTo = newIndices[j];
                        }
                        
                        if(indexFrom == -1 || indexTo == -1)
                            break;
                        
                        //System.out.println("Should be pasting a transition");
                        State from = getState(indexFrom);
                        State to = getState(indexTo);
                        
                        int tIndex = -1;
                        //Sort out the nails
                        if(trsel.nails.length == 0)
                        {
                            if(!trsel.prob)
                                tIndex = addTransition(from, to, false);
                            else
                                tIndex = addProbTransition(from, to, null);
                        }
                        else
                        {
                            ArrayList nails = new ArrayList();
                            
                            for(int j = 0; j < trsel.nails.length; j++)
                            {
                                GraphicSelection.NailSelect ns = trsel.nails[j];
                                Nail nn = new Nail(mouseX+(ns.x-mouseX)+selection.offsetX, mouseY+(ns.y-mouseY)+selection.offsetY, null, null);
                                nails.add(nn);
                            }
                            
                            Nail nn = (Nail)nails.get(0);
                            nn.setFrom(from);
                            nn.setSelected(true);
                            for(int j = 1; j < nails.size(); j++)
                            {
                                Nail curr = (Nail)nails.get(j);
                                curr.setSelected(true);
                                nn.setTo(curr);
                                curr.setFrom(nn);
                                nn = curr;
                            }
                            nn.setTo(to);
                            
                            if(!trsel.prob)
                                tIndex = addTransition(from, to, nails);
                            else
                                tIndex = addProbTransition(from, to, (Nail)nails.get(0));
                        }
                        
                        //Sort out labels
                        Transition nt = getTransition(tIndex);
                        nt.setAssignment(trsel.assignment);
                        nt.getAssignmentLabel().setOffsetX(trsel.assignmentX);
                        nt.getAssignmentLabel().setOffsetY(trsel.assignmentY);
                        nt.setSync(trsel.sync);
                        nt.getSyncLabel().setOffsetX(trsel.syncX);
                        nt.getSyncLabel().setOffsetY(trsel.syncY);
                        nt.setGuard(trsel.guard);
                        nt.getGuardLabel().setOffsetX(trsel.guardX);
                        nt.getGuardLabel().setOffsetY(trsel.guardY);
                        nt.setProbability(trsel.probability);
                        nt.getProbabilityLabel().setOffsetX(trsel.probabilityX);
                        nt.getProbabilityLabel().setOffsetY(trsel.probabilityY);
                        nt.setSelected(true);
                        addToSelection(nt, false);
                    }
                }
            }
            fireSelectionChanged();
        }
        catch(ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(UnsupportedFlavorException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    
    /** Processing method to delete all selected items.  When a State is deleted all
     * connecting transitions are also deleted.
     */
    public void deleteSelected()
    {
        modified = true;
        //First check to see whether any transitions
        ArrayList removeTr = new ArrayList();
        ArrayList removeSt = new ArrayList();
        for(int i = 0; i < numTransitions; i++)
        {
            Transition tra = (Transition)transitions.get(i);
            if(tra.isSelected())
            {
                
                if(tra.getTo() instanceof Decision) // is a start of a branched transition
                {
                    removeTr.add(tra);
                    for(int j = 0; j < numTransitions; j++)
                    {
                        Transition tra2 = (Transition)transitions.get(j);
                        
                        if(tra2 instanceof ProbTransition && tra2.getFrom() == tra.getTo())
                        {
                            removeTr.add(tra2);
                        }
                        
                    }
                    removeSt.add(tra.getTo());
                    
                }
                else // normal transition
                {
                    removeTr.add(tra);
                }
            }
        }
        
        //now delete all deleted states
        for(int i = 0; i < numStates; i++)
        {
            State st = (State)theStates.get(i);
            if(st.isSelected())
            {
                removeSt.add(st);
                for(int j = 0; j < numTransitions; j++)
                {
                    Transition tra = (Transition)transitions.get(j);
                    ////System.out.println("jloop"+tra);
                    if(tra.getFrom() == st || tra.getTo() == st)
                    {
                        if(tra.getFrom() == st)
                        {
                            if(tra.getTo() instanceof Decision)
                            {
                                removeSt.add(tra.getTo());
                                Decision de = (Decision)tra.getTo();
                                
                                for(int k = 0; k < numTransitions; k++)
                                {
                                    Transition tra2 = (Transition)transitions.get(k);
                                    if(tra2.getFrom() == de)
                                    {
                                        removeTr.add(tra2);
                                    }
                                }
                                
                            }
                            
                        }
                        removeTr.add(tra);
                    }
                }
            }
        }
        
        ArrayList removeNa = new ArrayList();
        for(int i = 0; i < numNails; i++)
        {
            Nail na = (Nail)theNails.get(i);
            if(na.isSelected())removeNa.add(na);
        }
        
        for(int i = 0 ; i < removeNa.size(); i++)
        {
            Nail na = (Nail)removeNa.get(i);
            deleteNail(na);
        }
        for(int i = 0 ; i < removeTr.size(); i++)
        {
            Transition t = (Transition)removeTr.get(i);
            t.getFrom().disassociateTransition(t);
            t.getTo().disassociateTransition(t);
        }
        transitions.removeAll(removeTr);
        theStates.removeAll(removeSt);
        numStates= theStates.size();
        numTransitions = transitions.size();
        orderStates();
        handler.hasModified(true);
        setChanged();
        notifyObservers(null);
    }
    
    /** Processing method to de select all items. */
    public void deSelectAll()
    {
        for(int i = 0; i < numStates; i++)
        {
            
            ((State)(theStates.get(i))).setSelected(false);
            //((State)(theStates.get(i))).getNameLabel().setSelected(false);
            //((State)(theStates.get(i))).getInvarientLabel().setSelected(false);
            ((State)(theStates.get(i))).getCommentLabel().setSelected(false);
        }
        for(int i = 0; i < numNails; i++)
            ((Nail)(theNails.get(i))).setSelected(false);
        for(int i = 0; i < numTransitions; i++)
        {
            ((Transition)(transitions.get(i))).setSelected(false);
            ((Transition)(transitions.get(i))).getGuardLabel().setSelected(false);
            ((Transition)(transitions.get(i))).getProbabilityLabel().setSelected(false);
            ((Transition)(transitions.get(i))).getSyncLabel().setSelected(false);
            ((Transition)(transitions.get(i))).getAssignmentLabel().setSelected(false);
            Transition trp = (Transition)transitions.get(i);
            if(trp instanceof ProbTransition)
            {
                ((ProbTransition)trp).getProbabilityLabel().setSelected(false);
            }
        }
        
        clearSelection(true);
    }
    
    /** When a branched transition is being drawn, its temporary information is stored
     * so this method can finalise it and add it to the diagram.
     */
    public void finaliseTempBranch()
    {
        if(tempProbTo.size() == 0)
        {
            drawingProbTrans = false;
            tempProbFrom = null;
            tempProbTo = new ArrayList();
            tempProbNailDown = false;
            tempProbNail = null;
            tempProbNails.clear();
            lastProbNail = null;
            handler.hasModified(true);
            return;
        }
        if(tempProbTo.size() == 1) //then we want a normal transition
        {
            if(tempProbNails.size() > 0)
                addTransition(tempProbFrom, (State)(tempProbTo.get(0)), tempProbNails);
            else
                addTransition(tempProbFrom, (State)(tempProbTo.get(0)), false);
            drawingProbTrans = false;
            tempProbFrom = null;
            tempProbTo = new ArrayList();
            tempProbNailDown = false;
            tempProbNail = null;
            tempProbNails.clear();
            lastProbNail = null;
            handler.hasModified(true);
            return;
        }
        
        int branchCount = tempProbTo.size();
        
        
        //Add all of the stuff
        int dec;
        if(!tempProbNailDown)
        {
            //if the nail hasn't been drawn then work out a position for the nail and draw it.
            double mix = tempProbFrom.getX()+15;
            double miy = tempProbFrom.getY()+15;
            double max = tempProbFrom.getX()+15;
            double may = tempProbFrom.getY()+15;
            for(int i = 0; i < tempProbTo.size(); i++)
            {
                mix = Math.min(((State)tempProbTo.get(i)).getX()+15, mix);
                max = Math.max(((State)tempProbTo.get(i)).getX()+15, max);
                miy = Math.min(((State)tempProbTo.get(i)).getY()+15, miy);
                may = Math.max(((State)tempProbTo.get(i)).getY()+15, may);
            }
            double probDecX = (mix+max)/2;
            double probDecY = (miy+may)/2;
            dec = addDecision(snapIt(probDecX+2.5)-2.5, snapIt(probDecY+2.5)-2.5);
            addTransition(tempProbFrom, getState(dec), false);
        }
        else
        {
            //what happens if the nail has been drawn
            dec = addDecision(lastProbNail.getX(), lastProbNail.getY());
            
            //tempProbNails.remove(lastProbNail);
            if(lastProbNail.getFrom() instanceof Nail)
            {
                ////System.out.println("This is where we want to look");
                Nail b4 = (Nail)lastProbNail.getFrom();
                b4.setTo(getState(dec));
                tempProbNails.remove(lastProbNail);
            }
            else
            {
                tempProbNails.remove(lastProbNail);
            }
            
            
            addTransition(tempProbFrom, getState(dec), tempProbNails);
            //else
            //addTransition(tempProbFrom, getState(dec), false);
        }
        boolean contin = tempProbTo.size()>0;
        while(contin)
        {
            State toState = (State)(tempProbTo.get(0));
            ArrayList sta = new ArrayList();
            sta.add(toState);
            int counter = 1;
            for(int i = 1; i < tempProbTo.size(); i++) // count all of the same
            {
                State otherState = (State)(tempProbTo.get(i));
                if(otherState == toState)
                {
                    counter++;
                    sta.add(otherState);
                }
            }
            
            if(counter == 1)
            {
                if(!(toState == tempProbFrom)) // if this state does not go back to the original state
                    addProbTransition(getState(dec), toState,null,"1/"+branchCount);
                else // if it does include a nail so they do not overlap
                    addProbTransition(getState(dec), toState, new Nail(((getState(dec).getX() + toState.getX())/2)+30, ((getState(dec).getY() + toState.getY())/2)+30, getState(dec), toState),"1/"+branchCount);
                
            }
            else
            {
                for(int i = 0; i < counter; i++)
                {
                    double offset = (-(((counter-1)*30)/2))+(i*30);
                    double x1 = getState(dec).getX()+3;
                    double y1 = getState(dec).getY()+3;
                    double x2 = toState.getX()+15;
                    double y2 = toState.getY()+15;
                    
                    double nailx = ((x1+x2)/2)+((Math.sin((Math.PI/2.0)-(Math.atan(((x1-x2)/2)/((y1-y2)/2)))))*((offset)));
                    double naily = ((y1+y2)/2)-((Math.cos((Math.PI/2.0)-(Math.atan(((x1-x2)/2)/((y1-y2)/2)))))*((offset)));
                    
                    addProbTransition(getState(dec), toState, new Nail(nailx, naily, getState(dec), toState),"1/"+branchCount);
                    
                }
            }
            for(int i = 0; i < sta.size(); i++)
            {
                tempProbTo.remove(sta.get(i));
            }
            contin = tempProbTo.size()>0;
        }
        drawingProbTrans = false;
        tempProbFrom = null;
        tempProbTo = new ArrayList();
        tempProbNailDown = false;
        tempProbNail = null;
        tempProbNails.clear();
        lastProbNail = null;
        handler.hasModified(true);
    }
    
    public void finaliseExtraBranches()
    {
        if(tempProbTo.size() == 0)
        {
            drawingExtraBranches = false;
            tempProbFrom = null;
            tempProbTo = new ArrayList();
            tempProbNailDown = false;
            tempProbNail = null;
            tempProbNails.clear();
            lastProbNail = null;
            handler.hasModified(true);
            return;
        }
        
        
        int branchCount = tempProbTo.size();
        
        
        //Add all of the stuff
        int dec = theStates.indexOf(tempProbFrom);
        
        
        
        
        
        boolean contin = tempProbTo.size()>0;
        while(contin)
        {
            State toState = (State)(tempProbTo.get(0));
            ArrayList sta = new ArrayList();
            sta.add(toState);
            int counter = 1;
            for(int i = 1; i < tempProbTo.size(); i++) // count all of the same
            {
                State otherState = (State)(tempProbTo.get(i));
                if(otherState == toState)
                {
                    counter++;
                    sta.add(otherState);
                }
            }
            
            if(counter == 1)
            {
                if(!(toState == tempProbFrom)) // if this state does not go back to the original state
                    addProbTransition(getState(dec), toState,null,"1/"+branchCount);
                else // if it does include a nail so they do not overlap
                    addProbTransition(getState(dec), toState, new Nail(((getState(dec).getX() + toState.getX())/2)+30, ((getState(dec).getY() + toState.getY())/2)+30, getState(dec), toState),"1/"+branchCount);
                
            }
            else
            {
                for(int i = 0; i < counter; i++)
                {
                    double offset = (-(((counter-1)*30)/2))+(i*30);
                    double x1 = getState(dec).getX()+3;
                    double y1 = getState(dec).getY()+3;
                    double x2 = toState.getX()+15;
                    double y2 = toState.getY()+15;
                    
                    double nailx = ((x1+x2)/2)+((Math.sin((Math.PI/2.0)-(Math.atan(((x1-x2)/2)/((y1-y2)/2)))))*((offset)));
                    double naily = ((y1+y2)/2)-((Math.cos((Math.PI/2.0)-(Math.atan(((x1-x2)/2)/((y1-y2)/2)))))*((offset)));
                    
                    addProbTransition(getState(dec), toState, new Nail(nailx, naily, getState(dec), toState),"1/"+branchCount);
                    
                }
            }
            for(int i = 0; i < sta.size(); i++)
            {
                tempProbTo.remove(sta.get(i));
            }
            contin = tempProbTo.size()>0;
        }
        drawingExtraBranches = false;
        tempProbFrom = null;
        tempProbTo = new ArrayList();
        tempProbNailDown = false;
        tempProbNail = null;
        tempProbNails.clear();
        lastProbNail = null;
        handler.hasModified(true);
    }
    
    public void zoomInOn(Rectangle2D area)
    {
        
        
        if(area.getWidth() < 30 || area.getHeight() < 30) return;
        double actualX = area.getX();
        double actualY = area.getY();
        actualX = snapIt(actualX);
        actualY = snapIt(actualY);
        
        //System.out.println("Should zoom");
        
        //System.out.println("actualX in zoomInOn is: "+actualX+" actualY = "+actualY);
        
        double zoomX = (currentWidth/zoom)/area.getWidth();
        double zoomY = (currentHeight/zoom)/area.getHeight();
        
        zoom = Math.min(zoomX, zoomY);
        //System.out.println("Zoom in zoomInOn is: "+zoom);
        
        cornerX = (int)(actualX * zoom);
        cornerY = (int)(actualY * zoom);
        zoomAreaChanged = true;
        setChanged();
        notifyObservers(null);
    }
    
    /** Called when a double click of the mouse occurs.  This method contains
     *	the processing necessary for a double click of the mouse.  In Select mode, a double click brings up a dialog box if on a State, Transition or Label.
     *	In Draw State mode, a double click attempts to place a state on the screen.
     *  This value assumes true and the following for loops check the placement of
     *	This state with other states and transitions on the screen.
     *  In PROBABILISTIC mode a double click on a state draws the final branch of a branched transition.
     **/
    public void processDoubleClick(double x, double y)
    {
        mouseDownWasReallySingleClick = false;
        ////System.out.println("double click");
        double actualX = x/zoom;
        double actualY = y/zoom;
        Rectangle2D mouseBox =
        new Rectangle2D.Double(actualX-1, actualY-1, 2, 2);
        setLastMouseBox(mouseBox);
        if(mode == EDIT)
        {
            //First check whether double click collides with a state, if so
            //and we are not already drawing, start prob transition mode
            
            
            for(int i = 0; i < numStates; i++)
            {
                State st = (State)theStates.get(i);
                if(st.intersects(mouseBox) && !(st instanceof Decision))
                {
                    //start a prob transition
                    if(!drawingProbTrans && ! drawingExtraBranches)
                    {
                        isSelecting = false;
                        isMoving = false;
                        
                        tempProbFrom = st;
                        tempProbTo = new ArrayList();
                        
                        
                        drawingProbTrans = true;
                        lastNail = null;
                        tempNails = new ArrayList();
                        
                        deSelectAll();
                        setChanged();
                        notifyObservers(null);
                        
                        return;
                    }
                    else if(drawingProbTrans)//end a prob transition
                    {
                        isSelecting = false;
                        isMoving = false;
                        
                        finaliseTempBranch();
                        
                        deSelectAll();
                        setChanged();
                        notifyObservers(null);
                        
                        return;
                    }
                    else if(drawingExtraBranches)//end drawing branches
                    {
                        isSelecting = false;
                        isMoving = false;
                        
                        finaliseExtraBranches();
                        
                        deSelectAll();
                        setChanged();
                        notifyObservers(null);
                        
                        return;
                    }
                    
                }
                if(st.intersects(mouseBox) && (st instanceof Decision))
                {
                    //start drawing extra branches
                    if(!drawingProbTrans && ! drawingExtraBranches)
                    {
                        isSelecting = false;
                        isMoving = false;
                        
                        tempProbFrom = st;
                        tempProbTo = new ArrayList();
                        
                        drawingExtraBranches = true;
                        lastNail = new Nail(st.getX(), st.getY(), null, null);
                        
                        deSelectAll();
                        setChanged();
                        notifyObservers();
                    }
                }
            }
            
            //Next Check whether double click collides with a transition
            for(int i = 0; i < numTransitions; i++)
            {
                Transition tr = (Transition)transitions.get(i);
                if(tr.intersects(mouseBox) ||
                tr.getGuardLabel().intersects(mouseBox) ||
                tr.getSyncLabel().intersects(mouseBox) ||
                tr.getAssignmentLabel().intersects(mouseBox) ||
                tr.getProbabilityLabel().intersects(mouseBox))
                {
                    //edit the transition properties
                    return;
                }
                if(tr instanceof ProbTransition)
                {
                    ProbTransition prtr = (ProbTransition)tr;
                    if(prtr.getProbabilityLabel().intersects(mouseBox))
                    {
                        //edit the prob transition properties
                        return;
                    }
                }
            }
            
            
            //a double click elsewhere, a double click attempts to
            //place a state on the screen.
            
            //If the location is valid then the state should be added
            
            if(!drawingProbTrans && !drawingExtraBranches)
            {
                deSelectAll();
                if(snap) addState(snapIt(actualX)-15, snapIt(actualY)-15);
                else addState(actualX-15, actualY-15);
            }
            
            
        }
        
        setChanged();
        notifyObservers(null);
    }
    
    public void processControlDown()
    {
        ////System.out.println("controldown");
        controlDown = true;
    }
    
    public void processControlUp()
    {
        ////System.out.println("controlup");
        controlDown = false;
    }
    
    public void processScrollWheel(double x, double y, boolean down)
    {
        if((mode == ZOOM && !zoomChanged) || (mode == EDIT && !zoomChanged && controlDown))
        {
            double actualX = x/zoom;
            double actualY = y/zoom;
            actualX = snapIt(actualX);
            actualY = snapIt(actualY);
            Rectangle2D mouseBox = new Rectangle2D.Double(actualX,  actualY, 1, 1);
            if(!down)
                zoom/=1.1;
            else
                zoom*=1.1;
            int screenCentreX = (viewingX/2);
            int screenCentreY = (viewingY/2);
            int screenMouseX  = (int)(x - scrOffsetX);
            int screenMouseY  = (int)(y - scrOffsetY);
            centreX = (int)(actualX * zoom)+(screenCentreX-screenMouseX);
            centreY = (int)(actualY * zoom)+(screenCentreY-screenMouseY);
            zoomChanged = true;
            setChanged();
            notifyObservers(null);
        }
        
        
        
    }
    
    /**	Called when there is a left mouse click on the mouse.  In Select mode, a single click selects whichever element is under the mouse.
     *	In Transition mode, a single click either completes the drawing of a transition(in on a state), or draws a nail(if anywhere else).
     *	In PROBABILISTIC mode a single click adds a state to the branches array
     */
    public void processSingleClick(double x, double y)
    {
        boolean doSelection = mouseDownWasReallySingleClick;
        mouseDownWasReallySingleClick = false;
        
        double actualX = x/zoom;
        double actualY = y/zoom;
        Rectangle2D mouseBox = new Rectangle2D.Double(actualX-1,  actualY-1, 2, 2);
        setLastMouseBox(mouseBox);
        if(mode == ZOOM)
        {
            //a single left button click will zoom in centered on the region in question
            zoom*=1.1;
            centreX = (int)(actualX * zoom);
            centreY = (int)(actualY * zoom);
            zoomChanged = true;
            setChanged();
            notifyObservers(null);
        }
        else if(mode == EDIT)
        {
            
            //In PROBABILISTIC mode a single click adds a state to the branches array
            if(drawingProbTrans)
            {
                State st = null;
                boolean foundAState = false;
                for(int i = 0; i < numStates; i++)
                {
                    st = (State)theStates.get(i);
                    
                    if(st.intersects(
                    (new Rectangle2D.Double(actualX, actualY, 1, 1))) &&
                    !(st instanceof Decision))
                    {
                        foundAState = true;
                        if(st == tempProbFrom)
                        {
                            if(this.tempProbTo.size() >0 || tempProbNailDown)
                                tempProbTo.add(st);
                        }
                        else
                        {
                            tempProbTo.add(st);
                        }
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    
                    
                }
                //If we get here, then the user wishes to place a nail...
                if(lastProbNail == null)
                {
		    //System.out.println("Should be adding the first nail");
                    //This must be the first nail put down
                    Nail theNail;
                    if(snap)
                        theNail =
                        new Nail(snapIt(actualX),
                        snapIt(actualY),
                        tempProbFrom,
                        null);
                    else
                        theNail =
                        new Nail(actualX, actualY, tempProbFrom, null);
                    lastProbNail = theNail;
                    tempProbNails.add(theNail);
                    tempProbNailDown = true;
                    //????
                    if(snap)
                    {
                        tranDrawX1 = snapIt(actualX);
                        tranDrawY1 = snapIt(actualY);
                    }
                    else
                    {
                        tranDrawX1 = actualX;
                        tranDrawY1 = actualY;
                    }
                    //????
                    
                }
                else
                {
		    //System.out.println("Adding a subsequent nail");
                    Nail theNail;
                    if(snap)
                        theNail =
                        new Nail(snapIt(actualX), snapIt(actualY), lastProbNail, null);
                    else
                        theNail =
                        new Nail(actualX, actualY, lastProbNail, null);
                    
                    lastProbNail.setTo(theNail);
                    lastProbNail = theNail;
                    tempProbNails.add(theNail);
                    if(snap)
                    {
                        tranDrawX1 = snapIt(actualX);
                        tranDrawY1 = snapIt(actualY);
                    }
                    else
                    {
                        tranDrawX1 = actualX;
                        tranDrawY1 = actualY;
                    }
                }
                                /* for now
                                 if(!foundAState)
                                 {
                                 //place the nail manually
                                 tempProbNail =
                                 new Nail(actualX, actualY, tempProbFrom, null);
                                 tempProbNailDown = true;
                                 }*/
                
                isMoving = false;
            }
            else if(drawingExtraBranches)
            {
                
                State st = null;
                for(int i = 0; i < numStates; i++)
                {
                    st = (State)theStates.get(i);
                    
                    if(st.intersects(
                    (new Rectangle2D.Double(actualX, actualY, 1, 1))) &&
                    !(st instanceof Decision))
                    {
                        tempProbTo.add(st);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    
                }
                
            }
            
            /*
            if(!doSelection)
            {
                //System.out.println("The click has already been processed");
                return;
            }
            else
            {
                //System.out.println("Processing as a click");
            }
            //Deselect everything first
            if(!controlDown)deSelectAll();
             
             
            //first check to see whether we are dragging any transitions
             
             
             
            //If not, a single click selects whichever element is under
            //the mouse
             
            for(int i = 0; i < numStates; i++)
            {
                State st = (State)theStates.get(i);
                if(st.intersects(mouseBox))
                {
                    if(controlDown)
                    {
                        st.setSelected(false);
                        st.getCommentLabel().setSelected(false);
                        removeFromSelection(st, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    else
                    {
                        st.setSelected(true);
                        st.getCommentLabel().setSelected(true);
                        addToSelection(st, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                }
                if(st.getCommentLabel().intersects(mouseBox))
                {
                    if(controlDown)
                    {
                        st.setSelected(false);
                        st.getCommentLabel().setSelected(false);
                        removeFromSelection(st, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    else
                    {
             
                        st.setSelected(true);
                        st.getCommentLabel().setSelected(true);
                        addToSelection(st, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                }
            }
            for(int i = 0; i < numNails; i++)
            {
                Nail na = (Nail)theNails.get(i);
                if(na.intersects(mouseBox))
                {
                    if(controlDown)
                    {
                        na.setSelected(false);
                        removeFromSelection(na, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
             
                    else
                    {
                        na.setSelected(true);
                        addToSelection(na, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                }
            }
            for(int i = 0; i < numTransitions; i++)
            {
                Transition tr = (Transition)transitions.get(i);
             
                if(tr.getGuardLabel().intersects(mouseBox))
                {
                    tr.getGuardLabel().setSelected(true);
                    tr.setSelectedButOnlyHighlightNails(true);
                    addToSelection(tr, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
                if(tr.getProbabilityLabel().intersects(mouseBox))
                {
                    tr.getProbabilityLabel().setSelected(true);
                    tr.setSelectedButOnlyHighlightNails(true);
                    addToSelection(tr, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
                if(tr.getSyncLabel().intersects(mouseBox))
                {
                    tr.getSyncLabel().setSelected(true);
                    tr.setSelectedButOnlyHighlightNails(true);
                    addToSelection(tr, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
                if(tr.getAssignmentLabel().intersects(mouseBox))
                {
                    tr.getAssignmentLabel().setSelected(true);
                    tr.setSelectedButOnlyHighlightNails(true);
                    addToSelection(tr, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
                if (tr instanceof ProbTransition)
                {
                    if(((ProbTransition)tr).getProbabilityLabel().intersects
                    (mouseBox))
                    {
                        ((ProbTransition)tr).getProbabilityLabel().setSelected
                        (true);
                        tr.setSelectedButOnlyHighlightNails(true);
                        addToSelection(tr, true);
                        setChanged();
                        notifyObservers(null);return;
                    }
                }
                if(tr.intersects(mouseBox))
                    //(new Rectangle2D.Double(actualX-2, actualY-2, 4, 4)))
                {
                    tr.setSelected(true);
                    tr.getAssignmentLabel().setSelected(true);
                    tr.getSyncLabel().setSelected(true);
                    tr.getProbabilityLabel().setSelected(true);
                    tr.getGuardLabel().setSelected(true);
                    addToSelection(tr, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
            }
             */
            setChanged();
            notifyObservers(null);
        }
    }
    
    /**	This method is called when a right mouse click event is produced.
     *	In transition mode, a right click of the mouse cancels the transition being drawn.
     *	In probabilistic mode, a right click cancels the last branch being drawn and draws the whole branched transition.
     *  In select mode, a right click can call the add nail dialog, if on a normal transition.
     */
    public void processRightClick(double x, double y, java.awt.event.MouseEvent e)
    {
        mouseDownWasReallySingleClick = false;
        double actualX = x/zoom;
        double actualY = y/zoom;
        Rectangle2D mouseBox = new Rectangle2D.Double(actualX-1,  actualY-1, 2, 2);
        setLastMouseBox(mouseBox);
        ////System.out.println("Processing right click");
        if(mode == ZOOM)
        {
            if(isZoomSelecting)
            {
                isZoomSelecting = false;
                return;
            }
            //a single left button click will zoom in centered on the region in question
            zoom/=1.1;
            centreX = (int)(actualX * zoom);
            centreY = (int)(actualY * zoom);
            zoomChanged = true;
            setChanged();
            notifyObservers(null);
        }
        else if(mode == EDIT)
        {
            if(drawingProbTrans)
            {
                if(tempProbTo.size()>0)
                {
                    finaliseTempBranch();
                }
                else
                {
                    drawingProbTrans = false;
                    tempProbFrom = null;
                    tempProbTo = new ArrayList();
                    tempProbNailDown = false;
                    tempProbNail = null;
		    lastProbNail = null;
		    //System.out.println("in rmclick stuff ere");
		    tempNails = new ArrayList();
		    tempProbNails = new ArrayList();
                }
                setChanged();
                notifyObservers(null);
            }
            else if(drawingExtraBranches)
            {
                if(tempProbTo.size()>0)
                {
                    finaliseExtraBranches();
                }
                else
                {
                    drawingExtraBranches = false;
                    tempProbFrom = null;
                    tempProbTo = new ArrayList();
                    tempProbNailDown = false;
                    tempProbNail = null;
                }
            }
            else // menus
            {
                //First clear the top of the menu
                Component comp = modulePopup.getComponent(0);
                while(!(comp instanceof JSeparator))
                {
                    if(modulePopup.getComponentCount() > 0)modulePopup.remove(0);
                    comp = modulePopup.getComponent(0);
                }
                boolean found = false;
                Transition nailThis = null;
                
                for(int i = 0; i < getNumTransitions(); i++)
                {
                    Transition tra = getTransition(i);
                    if(!found && tra.intersects(mouseBox) && tra.isSelected())
                    {
                        found = true;
                        nailThis = tra;
                    }
                    else if(found && tra.isSelected())
                    {
                        found = false;
                    }
                }
                if(found && nailThis != null)
                {
                    //transPopup.show(e.getComponent(), e.getX(), e.getY());
                    modulePopup.insert(addNailPop, 0);
                    modulePopup.show(e.getComponent(), e.getX(), e.getY());
                    tempAddNailTrans = nailThis;
                    tempAddNailX = actualX;
                    tempAddNailY = actualY;
                }
                else
                {
                    
                    //First look for intersections with states
                    for(int i = 0; i < getNumStates(); i++)
                    {
                        State sta = getState(i);
                        if(sta.intersects(mouseBox) && !(sta instanceof Decision))
                        {
                            modulePopup.insert(startTransition, 0);
                            modulePopup.show(e.getComponent(), e.getX(), e.getY());
                            starterState = sta;
                            popupX = actualX-15;
                            popupY = actualY-15;
                            return;
                        }
                        else if(sta.intersects(mouseBox) && (sta instanceof Decision))
                        {
                            modulePopup.insert(addChoice, 0);
                            modulePopup.show(e.getComponent(), e.getX(), e.getY());
                            starterState = sta;
                            popupX = actualX-15;
                            popupY = actualY-15;
                            return;
                        }
                    }
                    
                    
                    modulePopup.insert(addState, 0);
                    modulePopup.show(e.getComponent(), e.getX(), e.getY());
                    
                }
                popupX = actualX-15;
                popupY = actualY-15;
                
            }
        }
    }
    
    private boolean mouseDownWasReallySingleClick = false;
    
    /**	This method is called when a mouseDown event is produced somewhere.
     *	In select mode a mouse down starts dragging motions.  If the item is already selected then all selected items are dragged, if not, that one is selected and dragged.
     *
     **/
    public void processMouseDown(double x, double y)
    {
        popupX = Double.POSITIVE_INFINITY;
        popupY = Double.POSITIVE_INFINITY;
        double actualX = x/zoom;
        double actualY = y/zoom;
        Rectangle2D mouseBox = new Rectangle2D.Double(actualX-1,  actualY-1, 2, 2);
        setLastMouseBox(mouseBox);
        if(mode == ZOOM)
        {
            isZoomSelecting = true;
            zoomStartX = actualX;
            zoomStartY = actualY;
            zoomEndX = actualX;
            zoomEndY = actualY;
            
            setChanged();
            notifyObservers(null);
        }
        else if(mode == EDIT)
        {
            mouseDownWasReallySingleClick = true;
            //Deselect everything first if control is not pressed
            //if(!controlDown)deSelectAll();
            
            
            //first check to see whether we are dragging any transitions
            
            if(isDrawingProbTrans() || isDrawingExtraBranches())
            {
                return;
            }
            
            //If not, a single click selects whichever element is under
            //the mouse
            
            
            for(int i = 0; i < numStates; i++)
            {
                State st = (State)theStates.get(i);
                if(st.intersects(mouseBox))
                {
                    //if this state is not already selected then deselect all
                    if(!st.isSelected() && !controlDown)deSelectAll();
                    if(st.isSelected() && controlDown)
                    {
                        st.setSelected(false);
                        st.getCommentLabel().setSelected(false);
                        removeFromSelection(st, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    else
                    {
                        st.setSelected(true);
                        st.getCommentLabel().setSelected(true);
                        lastX = actualX;
                        lastY = actualY;
                        currX = actualX;
                        currY = actualY;
                        isMoving = true;
                        movingAbsolutes = true;
                        isSelecting = false;
                        addToSelection(st, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                }
                if(st.getCommentLabel().intersects(mouseBox))
                {
                    if(!st.getCommentLabel().isSelected() && !controlDown)deSelectAll();
                    if(st.getCommentLabel().isSelected() && controlDown)
                    {
                        st.setSelected(false);
                        st.getCommentLabel().setSelected(false);
                        removeFromSelection(st, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    else
                    {
                        st.getCommentLabel().setSelected(true);
                        st.setSelected(true);
                        lastX = actualX;
                        lastY = actualY;
                        currX = actualX;
                        currY = actualY;
                        isMoving = true;
                        movingAbsolutes = false;
                        isSelecting = false;
                        addToSelection(st, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                }
                
            }
            for(int i = 0; i < numNails; i++)
            {
                Nail na = (Nail)theNails.get(i);
                
                if(na.intersects(mouseBox))
                {
                    if(!na.isSelected() && !controlDown)deSelectAll();
                    if(na.isSelected() && controlDown)
                    {
                        na.setSelected(false);
                        removeFromSelection(na, true);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    na.setSelected(true);
                    lastX = actualX;
                    lastY = actualY;
                    currX = actualX;
                    currY = actualY;
                    isMoving = true;
                    movingAbsolutes = true;
                    isSelecting = false;
                    addToSelection(na, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
            }
            for(int i = 0; i < numTransitions; i++)
            {
                Transition tr = (Transition)transitions.get(i);
                
                if(tr.getGuardLabel().intersects(mouseBox))
                {
                    if(!tr.getGuardLabel().isSelected() && !controlDown)deSelectAll();
                    if(tr.getGuardLabel().isSelected() && controlDown)
                    {
                        tr.getGuardLabel().setSelected(false);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    tr.getGuardLabel().setSelected(true);
                    lastX = actualX;
                    lastY = actualY;
                    currX = actualX;
                    currY = actualY;
                    isMoving = true;
                    movingAbsolutes = false;
                    isSelecting = false;
                    tr.setSelected(true);
                    addToSelection(tr, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
                if(tr.getProbabilityLabel().intersects(mouseBox))
                {
                    if(!tr.getProbabilityLabel().isSelected() && !controlDown)deSelectAll();
                    if(tr.getProbabilityLabel().isSelected() && controlDown)
                    {
                        tr.getProbabilityLabel().setSelected(false);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    tr.getProbabilityLabel().setSelected(true);
                    lastX = actualX;
                    lastY = actualY;
                    currX = actualX;
                    currY = actualY;
                    isMoving = true;
                    movingAbsolutes = false;
                    isSelecting = false;
                    tr.setSelected(true);
                    addToSelection(tr, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
                if(tr.getSyncLabel().intersects(mouseBox))
                {
                    if(!tr.getSyncLabel().isSelected() && !controlDown)deSelectAll();
                    if(tr.getSyncLabel().isSelected() && controlDown)
                    {
                        tr.getSyncLabel().setSelected(false);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    tr.getSyncLabel().setSelected(true);
                    lastX = actualX;
                    lastY = actualY;
                    currX = actualX;
                    currY = actualY;
                    isMoving = true;
                    movingAbsolutes = false;
                    isSelecting = false;
                    tr.setSelected(true);
                    addToSelection(tr, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
                if(tr.getAssignmentLabel().intersects(mouseBox))
                {
                    if(!tr.getAssignmentLabel().isSelected() && !controlDown)deSelectAll();
                    if(tr.getAssignmentLabel().isSelected() && controlDown)
                    {
                        tr.getAssignmentLabel().setSelected(false);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    tr.getAssignmentLabel().setSelected(true);
                    lastX = actualX;
                    lastY = actualY;
                    currX = actualX;
                    currY = actualY;
                    isMoving = true;
                    movingAbsolutes = false;
                    isSelecting = false;
                    tr.setSelected(true);
                    addToSelection(tr, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
                if(tr.intersects(mouseBox))
                    //(new Rectangle2D.Double(actualX-2, actualY-2, 4, 4)))
                {
                    if(!tr.isSelected() && !controlDown)deSelectAll();
                    if(tr.isSelected() && controlDown)
                    {
                        tr.setSelected(false);
                        tr.getAssignmentLabel().setSelected(false);
                        tr.getSyncLabel().setSelected(false);
                        tr.getProbabilityLabel().setSelected(false);
                        tr.getGuardLabel().setSelected(false);
                        setChanged();
                        notifyObservers(null);
                        return;
                    }
                    tr.setSelected(true);lastX = actualX;
                    tr.getAssignmentLabel().setSelected(true);
                    tr.getSyncLabel().setSelected(true);
                    tr.getProbabilityLabel().setSelected(true);
                    tr.getGuardLabel().setSelected(true);
                    lastY = actualY;
                    currX = actualX;
                    currY = actualY;
                    isMoving = true;
                    movingAbsolutes = false;
                    isSelecting = false;
                    tr.setSelected(true);
                    addToSelection(tr, true);
                    setChanged();
                    notifyObservers(null);
                    return;
                }
                /*if (tr instanceof ProbTransition)
                {
                    if(((ProbTransition)tr).getProbabilityLabel().intersects
                    (mouseBox))
                    {
                        if(!((ProbTransition)tr).getProbabilityLabel().isSelected() && !controlDown)deSelectAll();
                        ((ProbTransition)tr).getProbabilityLabel().setSelected
                        (true);
                        lastX = actualX;
                        lastY = actualY;
                        currX = actualX;
                        currY = actualY;
                        isMoving = true;
                        movingAbsolutes = false;
                        isSelecting = false;
                        tr.setSelected(true);
                        addToSelection(tr, true);
                        setChanged();
                        notifyObservers(null);return;
                    }
                }*/
            }
            
            if(!controlDown)deSelectAll();
            //if no element is under the mouse then we start selecting
            isSelecting = true;
            selStartX = actualX;
            selStartY = actualY;
            selEndX = actualX;
            selEndY = actualY;
            
            
            setChanged();
            notifyObservers(null);
            
        }
        
        
    }
    /** This method is called when a mouseUp event is produced somewhere.
     *	In select mode a mouse up selects all items within a selection box.
     *	In transition mode, a transition is placed if there is a drag taking place and the release is over a state
     */
    public void processMouseUp(double x, double y)
    {
        double actualX = x/zoom;
        double actualY = y/zoom;
        //System.out.println("Process Mouse Up");
        if(mode == ZOOM)
        {
            if(isZoomSelecting)
            {
                //System.out.println("mode == zoom and isZoomselecting");
                Rectangle2D rect = new Rectangle2D.Double(zoomStartX, zoomStartY, zoomEndX-zoomStartX, zoomEndY-zoomStartY);
                zoomInOn(rect);
                isZoomSelecting = false;
                setChanged();
                notifyObservers(null);
            }
        }
        else if(mode == EDIT)
        {
            
            //in select mode a mouse up selects all items within a selection
            //box.
            if(drawingProbTrans) return;
            else if(isSelecting)
            {
                double selX;
                double selY;
                double selWidth;
                double selHeight;
                double modSelStartX = getSelStartX();
                double modSelStartY = getSelStartY();
                double modSelEndX = getSelEndX();
                double modSelEndY = getSelEndY();
                if(modSelStartX < modSelEndX)
                {
                    selX = modSelStartX;
                    selWidth = modSelEndX - modSelStartX;
                }
                else
                {
                    selX = modSelEndX;
                    selWidth = modSelStartX - modSelEndX;
                }
                if(modSelStartY < modSelEndY)
                {
                    selY = modSelStartY;
                    selHeight = modSelEndY - modSelStartY;
                }
                else
                {
                    selY = modSelEndY;
                    selHeight = modSelStartY - modSelEndY;
                }
                Rectangle2D.Double selection =
                new Rectangle2D.Double(selX, selY, selWidth, selHeight);
                
                //Clearing the selection if necessary
                if(!controlDown)
                {
                    for(int i = 0; i < getNumStates(); i++)
                    {
                        State st = ((State)(theStates.get(i)));
                        st.setSelected(false);
                        removeFromSelection(st, false);
                    }
                    for(int j = 0; j < numNails; j++)
                    {
                        Nail na = ((Nail)(theNails.get(j)));
                        na.setSelected(false);
                        removeFromSelection(na, false);
                    }
                    for(int i = 0; i < getNumTransitions(); i++)
                    {
                        Transition tr = getTransition(i);
                        tr.setSelected(false);
                        removeFromSelection(tr, false);
                    }
                }
                for(int i = 0; i < getNumStates(); i++)
                {
                    //System.out.println("looking in mouseup for state "+i);
                    State st = (State)theStates.get(i);
                    //if(st.intersects(selection))
                    if(selection.contains(st.getBounds2D()))
                    {
                        //System.out.println("intersects");
                        if(!st.isSelected())
                        {
                            st.setSelected(true);
                            st.getCommentLabel().setSelected(true);
                            
                            addToSelection(st, false);
                            continue;
                        }
                        else
                        {
                            st.setSelected(false);
                            st.getCommentLabel().setSelected(false);
                            removeFromSelection(st, false);
                            continue;
                        }
                    }
                    
                    //if(st.getCommentLabel().intersects(mouseBox))
                    if(selection.contains(st.getCommentLabel().getBounds2D()))
                    {
                        if(!st.isSelected())
                        {
                            st.setSelected(true);
                            st.getCommentLabel().setSelected(true);
                            addToSelection(st, false);
                            continue;
                        }
                        else
                        {
                            st.setSelected(false);
                            st.getCommentLabel().setSelected(false);
                            removeFromSelection(st ,false);
                            continue;
                        }
                    }
                }
                
                for(int i = 0; i < getNumTransitions(); i++)
                {
                    Transition tr = getTransition(i);
                    if(selection.contains(tr.getBounds2D()))
                    {
                        if(tr.isSelected())
                        {
                            tr.setSelected(false);
                            tr.getAssignmentLabel().setSelected(false);
                            tr.getSyncLabel().setSelected(false);
                            tr.getProbabilityLabel().setSelected(false);
                            tr.getGuardLabel().setSelected(false);
                            removeFromSelection(tr, true);
                            continue;
                        }
                        else
                        {
                            tr.setSelected(true);
                            tr.getAssignmentLabel().setSelected(true);
                            tr.getSyncLabel().setSelected(true);
                            tr.getProbabilityLabel().setSelected(true);
                            tr.getGuardLabel().setSelected(true);
                            addToSelection(tr, true);
                            continue;
                        }
                        
                    }
                    //if(tr.getGuardLabel().intersects(mouseBox))
                    if(selection.contains(tr.getGuardLabel().getBounds2D()))
                    {
                        if(tr.getGuardLabel().isSelected())
                        {
                            tr.getGuardLabel().setSelected(false);
                            continue;
                        }
                        else
                        {
                            tr.getGuardLabel().setSelected(true);
                            tr.setSelected(true);
                            addToSelection(tr, true);
                            continue;
                        }
                    }
                    //if(tr.getProbabilityLabel().intersects(mouseBox))
                    if(selection.contains(tr.getProbabilityLabel().getBounds2D()))
                    {
                        if(tr.getProbabilityLabel().isSelected())
                        {
                            tr.getProbabilityLabel().setSelected(false);
                            continue;
                        }
                        else
                        {
                            tr.getProbabilityLabel().setSelected(true);
                            tr.setSelected(true);
                            addToSelection(tr, true);
                            continue;
                        }
                    }
                    //if(tr.getSyncLabel().intersects(mouseBox))
                    if(selection.contains(tr.getSyncLabel().getBounds2D()))
                    {
                        if(tr.getSyncLabel().isSelected())
                        {
                            tr.getSyncLabel().setSelected(false);
                            continue;
                        }
                        else
                        {
                            tr.getSyncLabel().setSelected(true);
                            tr.setSelected(true);
                            addToSelection(tr, true);
                            continue;
                        }
                    }
                    //if(tr.getAssignmentLabel().intersects(mouseBox))
                    if(selection.contains(tr.getAssignmentLabel().getBounds2D()))
                    {
                        if(tr.getAssignmentLabel().isSelected())
                        {
                            tr.getAssignmentLabel().setSelected(false);
                            continue;
                        }
                        else
                        {
                            tr.getAssignmentLabel().setSelected(true);
                            tr.setSelected(true);
                            addToSelection(tr, true);
                            continue;
                        }
                    }
                    /*if (tr instanceof ProbTransition)
                    {
                        //if(((ProbTransition)tr).getProbabilityLabel().intersects
                        //(mouseBox))
                        if(selection.contains())
                        {
                            ((ProbTransition)tr).getProbabilityLabel().setSelected
                            (true);
                            tr.setSelectedButOnlyHighlightNails(true);
                            addToSelection(tr, true);
                        }
                    }*/
                    
                }
                for(int i = 0; i < numNails; i++)
                {
                    Nail na = (Nail)theNails.get(i);
                    //if(na.intersects((new Rectangle2D.Double(selX, selY, selWidth, selHeight))))
                    if((new Rectangle2D.Double(selX, selY, selWidth, selHeight)).contains(na.getBounds2D()))
                    {
                        if(!na.isSelected())
                        {
                            na.setSelected(true);
                            addToSelection(na, false);
                        }
                        else
                        {
                            na.setSelected(false);
                            removeFromSelection(na, false);
                        }
                        ////System.out.println("nail selected");
                    }
                }
                fireSelectionChanged();
            }
            
            else if(isMoving)
            {
                if(snap)
                {
                    for(int i = 0; i < numStates; i++)
                    {
                        State sta = (State)theStates.get(i);
                        if(sta.isSelected())
                        {
                            double offset = sta.getWidth()/2;
                            sta.setPosition
                            (snapIt
                            (sta.getX()+offset)-
                            offset, snapIt(sta.getY()+offset)-offset);
                            sta.stopMoving();
                        }
                    }
                    for(int i = 0; i < numNails; i++)
                    {
                        Nail na = (Nail)theNails.get(i);
                        if(na.isSelected())
                        {
                            na.setPosition
                            (snapIt(na.getX()), snapIt(na.getY()));
                            na.stopMoving();
                        }
                    }
                }
                
                
            }
            
        }
        isMoving = false;
        isSelecting = false;
        setChanged();
        notifyObservers();
    }
    /** This method is called following a drag event somewhere.  When in moving
     *	mode selected items are moves approriately.
     */
    public void processDrag()
    {
        mouseDownWasReallySingleClick = false;
        double
        minX=Double.POSITIVE_INFINITY,
        minY=Double.POSITIVE_INFINITY,
        maxX=0,
        maxY=0;
	boolean ignoreBounds = false;
        if(mode == ZOOM)
        {
            
        }
        else if(mode == EDIT)
        {
	    
            if(isMoving)
            {
                
                
                
                for(int i = 0; i < theStates.size(); i++)
                {
                    State sta = getState(i);
                    if(sta.isSelected() && movingAbsolutes)
                    {
                        double offset = sta.getWidth()/2;
                        if(sta.getMinX() < minX)minX = sta.getMinX();
                        if(sta.getMinY() < minY)minY = sta.getMinY();
                        if(sta.getMaxX() > maxX)maxX = sta.getMaxX();
                        if(sta.getMaxY() > maxY)maxY = sta.getMaxY();
                        sta.move(currX-lastX, currY-lastY);
                        if(sta.getMinX() < minX)minX = sta.getMinX();
                        if(sta.getMinY() < minY)minY = sta.getMinY();
                        if(sta.getMaxX() > maxX)maxX = sta.getMaxX();
                        if(sta.getMaxY() > maxY)maxY = sta.getMaxY();
                        //sta.setPosition(snapIt(sta.getX()+(currX-lastX)), snapIt(sta.getY()+(currY-lastY)));
                        if(snap)
                        {
                            sta.movingSnap(offset, gridWidth, subdivisions);
                        }
                    }
                    if(sta.getCommentLabel().isSelected()&& !movingAbsolutes)
                        sta.getCommentLabel().move(currX-lastX, currY-lastY);
                    
                    
                }
                for(int i = 0; i < theNails.size(); i++)
                {
                    
                    Nail na = (Nail)theNails.get(i);
                    if(na.isSelected()&& movingAbsolutes)
                    {
                        double offset = na.getWidth()/2;
                        if(na.getMinX() < minX)minX = na.getMinX();
                        if(na.getMinY() < minY)minY = na.getMinY();
                        if(na.getMaxX() > maxX)maxX = na.getMaxX();
                        if(na.getMaxY() > maxY)maxY = na.getMaxY();
                        na.move(currX-lastX, currY-lastY);
                        if(na.getMinX() < minX)minX = na.getMinX();
                        if(na.getMinY() < minY)minY = na.getMinY();
                        if(na.getMaxX() > maxX)maxX = na.getMaxX();
                        if(na.getMaxY() > maxY)maxY = na.getMaxY();
                        if(snap)
                        {
                            na.movingSnap(offset, gridWidth, subdivisions);
                        }
                    }
                }
		for(int i = 0; i < transitions.size(); i++)
		{
		    Transition tr = (Transition)transitions.get(i);
		    if(tr.getMaxX() > maxX) maxX = tr.getMaxX();
		    if(tr.getMinX() < minX) minX = tr.getMinX();
		    if(tr.getMaxY() > maxY) maxY = tr.getMaxY();
		    if(tr.getMinY() < minY) minY = tr.getMinY();
		}
                if(!movingAbsolutes)
                {
                    for(int i = 0; i < numTransitions; i++)
                    {
                        Transition tr = (Transition)transitions.get(i);
                        if(tr.getGuardLabel().isSelected())
                            tr.getGuardLabel().move(currX-lastX, currY-lastY);
                        if(tr.getProbabilityLabel().isSelected())
                            tr.getProbabilityLabel().move(currX-lastX, currY-lastY);
                        if(tr.getSyncLabel().isSelected())
                            tr.getSyncLabel().move(currX-lastX, currY-lastY);
                        if(tr.getAssignmentLabel().isSelected())
                            tr.getAssignmentLabel().move(currX-lastX, currY-lastY);
                    }
                }
                lastX = currX;
                lastY = currY;
            }
            
            
            if(isDrawingProbTrans())
            {
                ignoreBounds = true;
		/*
                State sta = getTempProbFrom();
                if(sta.getMinX() < minX)minX = sta.getMinX();
                if(sta.getMinY() < minY)minY = sta.getMinY();
                if(sta.getMaxX() > maxX)maxX = sta.getMaxX();
                if(sta.getMaxY() > maxY)maxY = sta.getMaxY();
                
                for(int i = 0; i < tempProbTo.size(); i++)
                {
                    sta = (State)getTempProbTo().get(i);
                    if(sta.getMinX() < minX)minX = sta.getMinX();
                    if(sta.getMinY() < minY)minY = sta.getMinY();
                    if(sta.getMaxX() > maxX)maxX = sta.getMaxX();
                    if(sta.getMaxY() > maxY)maxY = sta.getMaxY();
                }
                if(getNumTempProbNails() > 0)
                {
                    Nail na = getTempProbNail(getNumTempProbNails()-1);
                    if(na.getMinX() < minX)minX = na.getMinX();
                    if(na.getMinY() < minY)minY = na.getMinY();
                    if(na.getMaxX() > maxX)maxX = na.getMaxX();
                    if(na.getMaxY() > maxY)maxY = na.getMaxY();
                }
                else
                {
                    sta = getTempProbFrom();
                    if(sta.getMinX() < minX)minX = sta.getMinX();
                    if(sta.getMinY() < minY)minY = sta.getMinY();
                    if(sta.getMaxX() > maxX)maxX = sta.getMaxX();
                    if(sta.getMaxY() > maxY)maxY = sta.getMaxY();
                }
                
                if(lastTranDrawX < minX) minX = lastTranDrawX - 10;
                if(lastTranDrawX > maxX) maxX = lastTranDrawX + 10;
                if(lastTranDrawY < minY) minY = lastTranDrawY - 10;
                if(lastTranDrawY > maxY) maxY = lastTranDrawY + 10;
                
                if(tranDrawX2 < minX) minX = tranDrawX2 - 10;
                if(tranDrawX2 > maxX) maxX = tranDrawX2 + 10;
                if(tranDrawY2 < minY) minY = tranDrawY2 - 10;
                if(tranDrawY2 > maxY) maxY = tranDrawY2 + 10;*/
            }
            
            if(isDrawingExtraBranches())
            {
                
                State sta = getTempProbFrom();
                if(sta.getMinX() < minX)minX = sta.getMinX();
                if(sta.getMinY() < minY)minY = sta.getMinY();
                if(sta.getMaxX() > maxX)maxX = sta.getMaxX();
                if(sta.getMaxY() > maxY)maxY = sta.getMaxY();
                
                for(int i = 0; i < tempProbTo.size(); i++)
                {
                    sta = (State)getTempProbTo().get(i);
                    if(sta.getMinX() < minX)minX = sta.getMinX();
                    if(sta.getMinY() < minY)minY = sta.getMinY();
                    if(sta.getMaxX() > maxX)maxX = sta.getMaxX();
                    if(sta.getMaxY() > maxY)maxY = sta.getMaxY();
                }
                if(getNumTempProbNails() > 0)
                {
                    Nail na = getTempProbNail(getNumTempProbNails()-1);
                    if(na.getMinX() < minX)minX = na.getMinX();
                    if(na.getMinY() < minY)minY = na.getMinY();
                    if(na.getMaxX() > maxX)maxX = na.getMaxX();
                    if(na.getMaxY() > maxY)maxY = na.getMaxY();
                }
                else
                {
                    sta = getTempProbFrom();
                    if(sta.getMinX() < minX)minX = sta.getMinX();
                    if(sta.getMinY() < minY)minY = sta.getMinY();
                    if(sta.getMaxX() > maxX)maxX = sta.getMaxX();
                    if(sta.getMaxY() > maxY)maxY = sta.getMaxY();
                }
                
                if(lastTranDrawX < minX) minX = lastTranDrawX - 10;
                if(lastTranDrawX > maxX) maxX = lastTranDrawX + 10;
                if(lastTranDrawY < minY) minY = lastTranDrawY - 10;
                if(lastTranDrawY > maxY) maxY = lastTranDrawY + 10;
                
                if(tranDrawX2 < minX) minX = tranDrawX2 - 10;
                if(tranDrawX2 > maxX) maxX = tranDrawX2 + 10;
                if(tranDrawY2 < minY) minY = tranDrawY2 - 10;
                if(tranDrawY2 > maxY) maxY = tranDrawY2 + 10;
            }
            
            if(isSelecting)
            {
                if(selStartX < minX) minX = selStartX - 15;
                if(selStartX > maxX) maxX = selStartX + 15;
                if(selStartY < minY) minY = selStartY - 15;
                if(selStartY > maxY) maxY = selStartY + 15;
                
                if(selEndX < minX) minX = selEndX - 15;
                if(selEndX > maxX) maxX = selEndX + 15;
                if(selEndY < minY) minY = selEndY - 15;
                if(selEndY > maxY) maxY = selEndY + 15;
                
                if(lastSelX < minX) minX = lastSelX - 15;
                if(lastSelX > maxX) maxX = lastSelX + 15;
                if(lastSelY < minY) minY = lastSelY - 15;
                if(lastSelY > maxY) maxY = lastSelY + 15;
            }
            
            if(isZoomSelecting())
            {
                if(zoomStartX < minX) minX = zoomStartX - 15;
                if(zoomStartX > maxX) maxX = zoomStartX + 15;
                if(zoomStartY < minY) minY = zoomStartY - 15;
                if(zoomStartY > maxY) maxY = zoomStartY + 15;
                
                if(zoomEndX < minX) minX = zoomEndX - 15;
                if(zoomEndX > maxX) maxX = zoomEndX + 15;
                if(zoomEndY < minY) minY = zoomEndY - 15;
                if(zoomEndY > maxY) maxY = zoomEndY + 15;
                
                if(lastZoomX < minX) minX = lastZoomX - 15;
                if(lastZoomX > maxX) maxX = lastZoomX + 15;
                if(lastZoomY < minY) minY = lastZoomY - 15;
                if(lastZoomY > maxY) maxY = lastZoomY + 15;
            }
            
            
            
        }
        setChanged();
        
        if(!ignoreBounds && minX < Double.POSITIVE_INFINITY && minY < Double.POSITIVE_INFINITY)
        {
            Rectangle bounds = new Rectangle((int)(minX*zoom), (int)(minY*zoom), (int)((maxX-minX)*zoom), (int)((maxY-minY)*zoom));
            ////System.out.println("Should be notifying with: "+bounds);
            notifyObservers(bounds);
        }
        else
        {
            notifyObservers();
        }
    }
    
    public void processMouseMoved(double x, double y)
    {
        //popupX = Double.POSITIVE_INFINITY;
        //popupY = Double.POSITIVE_INFINITY;
        mouseDownWasReallySingleClick = false;
        if(mode == EDIT)
        {
            double actualX = x/zoom;
            double actualY = y/zoom;
            Rectangle2D mouseBox = new Rectangle2D.Double(actualX-1,  actualY-1, 2, 2);
            setLastMouseBox(mouseBox);
            
            
            if(!isMoving && !isDrawingProbTrans() && !isSelecting && !isZoomSelecting)
            {
                for(int i = 0; i < numStates; i++)
                {
                    State st = (State)theStates.get(i);
                    if(st.intersects(mouseBox))
                    {
                        isOverMovable = true;
                        setOverTransition(false);
                        return;
                    }
                    if(st.getCommentLabel().intersects(mouseBox))
                    {
                        
                        isOverMovable = true;
                        setOverTransition(false);
                        return;
                    }
                    
                }
                for(int i = 0; i < numNails; i++)
                {
                    Nail na = (Nail)theNails.get(i);
                    
                    if(na.intersects(mouseBox))
                    {
                        isOverMovable = true;
                        setOverTransition(false);
                        return;
                    }
                }
                for(int i = 0; i < numTransitions; i++)
                {
                    Transition tr = (Transition)transitions.get(i);
                    if(tr.getGuardLabel().intersects(mouseBox))
                    {
                        isOverMovable = true;
                        setOverTransition(false);
                        return;
                    }
                    if(tr.getProbabilityLabel().intersects(mouseBox))
                    {
                        isOverMovable = true;
                        setOverTransition(false);
                        return;
                    }
                    if(tr.getSyncLabel().intersects(mouseBox))
                    {
                        isOverMovable = true;
                        setOverTransition(false);
                        return;
                    }
                    if(tr.getAssignmentLabel().intersects(mouseBox))
                    {
                        isOverMovable = true;
                        setOverTransition(false);
                        return;
                    }
                    
                    if (tr instanceof ProbTransition)
                    {
                        if(((ProbTransition)tr).getProbabilityLabel().intersects
                        (mouseBox))
                        {
                            isOverMovable = true;
                            setOverTransition(false);
                            return;
                        }
                    }
                }
                isOverMovable = false;
                for(int i = 0; i < numTransitions; i++)
                {
                    Transition tr = (Transition)transitions.get(i);
                    
                    if(tr.intersects(mouseBox))
                    {
                        setOverTransition(true);
                        return;
                    }
                }
                setOverTransition(false);
            }
            else
            {
                isOverMovable = false;
                setOverTransition(false);
            }
        }
    }
    
    
    /** Deletes all selected States.
     * @deprecated Replaced by the deleteSelected() method.
     */
    public void processDeletePressed()
    {
        if(mode == EDIT)
        {
            for(int i = 0; i < numStates; i++)
            {
                State st = ((State)theStates.get(i));
                if(st.isSelected())
                {
                    deleteState(st);
                }
            }
        }
    }
    
    /** Method which according to the current grid width returns a snapped value for the
     * given input parameter.
     */
    public double snapIt(double in)
    {
        in /= (gridWidth/subdivisions);
        in = Math.round(in);
        in *= (gridWidth/subdivisions);
        return in;
    }
    
    /** Due to the fact that Decisions are stored in the State array, and State's are
     * indexed according to their location in this array, this method sorts out the
     * array of States to place normal States first, and Decisions last.
     */
    public void orderStates()
    {
        // This method has the job of sorting the state array so that normal states appear first and decisions last
        ArrayList normal = new ArrayList();
        ArrayList notNormal = new ArrayList();
        for(int i = 0; i < theStates.size(); i++)
        {
            Object st = theStates.get(i);
            if(st instanceof Decision) notNormal.add(st);
            else normal.add(st);
        }
        theStates = new ArrayList();
        theStates.addAll(normal);
        
        //notify each state of its new index
        for(int i = 0; i < normal.size(); i++)
        {
            getState(i).setDescriptor(i);
        }
        
        theStates.addAll(notNormal);
        
        int initial = 0;
        for(int i = 0; i < getNumStates(); i++)
        {
            State s = getState(i);
            if(s.isInitial()) initial = i;
        }
        if(normal.size() == 0)
            synchTreeStateVar(initial, 0);
        else
            synchTreeStateVar(initial, normal.size()-1);
    }
    
    
    public void synchTreeStateVar(int init, int high)
    {
        //System.out.println("setting init = "+init+" high = "+high);
        try
        {
            GUIMultiModelTree.StateVarNode stateNode = (GUIMultiModelTree.StateVarNode)corresponding.getChildAt(0);
            stateNode.setInitial(handler.getGUIPlugin().getPrism().parseSingleExpressionString(""+init));
            stateNode.setMax(handler.getGUIPlugin().getPrism().parseSingleExpressionString(""+high));
            
            corresponding.childrenChanged();
        }
        catch(Exception e)
        {
            ////System.out.println("UNEXOECTED ERROR");
            return;
        }
    }
    
    boolean mutual = true;
    public void update(Observable o, Object arg)
    {
        if(o instanceof BooleanProperty)
        {
            BooleanProperty bp = (BooleanProperty)o;
            
            if(mutual)
            {
                mutual = false;
                if(bp.getName().equals("initial") && bp.getBoolValue())
                {
                    //A value has been set to true, check for conflicts
                    for(int i = 0; i < getNumStates(); i++)
                    {
                        State st = getState(i);
                        if(bp.getOwner() != st)
                        {
                            st.setInitial(false);
                        }
                    }
                }
                mutual = true;
            }
        }
        int initial = 0;
        for(int i = 0; i < getNumStates(); i++)
        {
            State s = getState(i);
            if(s.isInitial()) initial = i;
        }
        orderStates();
        
        setChanged();
        notifyObservers(null);
    }
    
    /** Getter for property container.
     * @return Value of property container.
     *
     */
    public userinterface.model.graphicModel.GraphicModuleContainer getContainer()
    {
        return container;
    }
    
    /** Setter for property container.
     * @param container New value of property container.
     *
     */
    public void setContainer(userinterface.model.graphicModel.GraphicModuleContainer container)
    {
        this.container = container;
    }
    
    /**
     * Getter for property lastMouseBox.
     * @return Value of property lastMouseBox.
     */
    public java.awt.geom.Rectangle2D getLastMouseBox()
    {
        return lastMouseBox;
    }
    
    /**
     * Setter for property lastMouseBox.
     * @param lastMouseBox New value of property lastMouseBox.
     */
    public void setLastMouseBox(java.awt.geom.Rectangle2D lastMouseBox)
    {
        this.lastMouseBox = lastMouseBox;
    }
    
    /**
     * Getter for property overTransition.
     * @return Value of property overTransition.
     */
    public boolean isOverTransition()
    {
        return overTransition;
    }
    
    /**
     * Setter for property overTransition.
     * @param overTransition New value of property overTransition.
     */
    public void setOverTransition(boolean overTransition)
    {
        this.overTransition = overTransition;
    }
    
    /**
     * Getter for property drawingExtraBranches.
     * @return Value of property drawingExtraBranches.
     */
    public boolean isDrawingExtraBranches()
    {
        return drawingExtraBranches;
    }
    
    /**
     * Setter for property drawingExtraBranches.
     * @param drawingExtraBranches New value of property drawingExtraBranches.
     */
    public void setDrawingExtraBranches(boolean drawingExtraBranches)
    {
        this.drawingExtraBranches = drawingExtraBranches;
    }
    
}
